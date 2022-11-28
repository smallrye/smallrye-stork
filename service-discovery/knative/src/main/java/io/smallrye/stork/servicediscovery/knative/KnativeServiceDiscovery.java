package io.smallrye.stork.servicediscovery.knative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.utils.ServiceInstanceIds;
import io.smallrye.stork.utils.ServiceInstanceUtils;
import io.vertx.core.Vertx;

/**
 * An implementation of service discovery for Knative.
 * This implementation locates a Knative service and retrieves the <em>url</em>
 */
public class KnativeServiceDiscovery extends CachingServiceDiscovery {
    static final String METADATA_NAME = "metadata.name";
    private final KnativeClient kn;
    private final String application;
    private final boolean allNamespaces;
    private final String namespace;
    private final boolean secure;
    private final Vertx vertx;

    private static final Logger LOGGER = LoggerFactory.getLogger(KnativeServiceDiscovery.class);

    private AtomicBoolean invalidated = new AtomicBoolean();

    /**
     * Creates a new KubernetesServiceDiscovery.
     *
     * @param serviceName the service name
     * @param config the configuration
     * @param vertx the vert.x instance
     */
    public KnativeServiceDiscovery(String serviceName, KnativeConfiguration config, Vertx vertx) {
        super(config.getRefreshPeriod());
        Config base = Config.autoConfigure(null);
        String masterUrl = config.getKnativeHost() == null ? base.getMasterUrl() : config.getKnativeHost();
        this.application = config.getApplication() == null ? serviceName : config.getApplication();
        this.namespace = config.getKnativeNamespace() == null ? base.getNamespace() : config.getKnativeNamespace();

        allNamespaces = namespace != null && namespace.equalsIgnoreCase("all");

        if (namespace == null) {
            throw new IllegalArgumentException("Namespace is not configured for service '" + serviceName
                    + "'. Please provide a namespace. Use 'all' to discover services in all namespaces");
        }

        Config k8sConfig = new ConfigBuilder(base)
                .withMasterUrl(masterUrl)
                .withNamespace(namespace).build();
        this.kn = new DefaultKnativeClient(k8sConfig);
        this.vertx = vertx;
        this.secure = isSecure(config);
        kn.services().inform(new ResourceEventHandler<Service>() {
            @Override
            public void onAdd(Service obj) {
                LOGGER.info("Endpoint added: {}", obj.getMetadata().getName());
                //                invalidate();
            }

            @Override
            public void onUpdate(Service oldObj, Service newObj) {
                LOGGER.info("Endpoint updated : {}", newObj.getMetadata().getName());
                //                invalidate();
            }

            @Override
            public void onDelete(Service obj, boolean deletedFinalStateUnknown) {
                LOGGER.info("Endpoint deleted: {}", obj.getMetadata().getName());
                //                invalidate();
            }

        });

    }

    @Override
    public Uni<List<ServiceInstance>> cache(Uni<List<ServiceInstance>> uni) {
        return uni.memoize().until(() -> invalidated.get());
    }

    public void invalidate() {
        invalidated.set(true);
    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        Uni<List<Service>> knServicesUni = Uni.createFrom().emitter(
                emitter -> {
                    vertx.executeBlocking(future -> {
                        List<Service> items = new ArrayList<>();

                        if (allNamespaces) {
                            items.addAll(
                                    kn.services().inAnyNamespace().withField(METADATA_NAME, application).list().getItems());
                        } else {
                            Service e = kn.services().inNamespace(namespace).withName(application).get();
                            items.add(e);
                        }
                        future.complete(items);
                    }, result -> {
                        if (result.succeeded()) {
                            @SuppressWarnings("unchecked")
                            List<Service> knServices = (List<Service>) result.result();
                            emitter.complete(knServices);
                        } else {
                            LOGGER.error("Unable to retrieve the knative service from the {} service", application,
                                    result.cause());
                            emitter.fail(result.cause());
                        }
                    });
                });
        return knServicesUni.onItem().transform(knServices -> toStorkServiceInstances(knServices, previousInstances))
                .invoke(() -> invalidated.set(false));
    }

    private List<ServiceInstance> toStorkServiceInstances(List<Service> knServices, List<ServiceInstance> previousInstances) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        for (Service knService : knServices) {
            ServiceInstance matching = ServiceInstanceUtils.findMatching(previousInstances, knService.getStatus().getUrl(),
                    8080);
            if (matching != null) {
                serviceInstances.add(matching);
            } else {
                Map<String, String> labels = new HashMap<>(knService.getMetadata().getLabels() != null
                        ? knService.getMetadata().getLabels()
                        : Collections.emptyMap());

                Metadata<KnativeMetadataKey> knativeMetadata = Metadata.of(KnativeMetadataKey.class);

                serviceInstances
                        .add(new DefaultServiceInstance(ServiceInstanceIds.next(), knService.getStatus().getUrl(), 8080, secure,
                                labels,
                                knativeMetadata
                                        .with(KnativeMetadataKey.META_KNATIVE_SERVICE_ID, knService.getFullResourceName())
                                        .with(KnativeMetadataKey.META_KNATIVE_NAMESPACE, knService.getMetadata().getNamespace())
                                        .with(KnativeMetadataKey.META_KNATIVE_LATEST_REVISION,
                                                knService.getStatus().getLatestCreatedRevisionName())));
            }
        }
        return serviceInstances;

    }

    private static boolean isSecure(KnativeConfiguration config) {
        return config.getSecure() != null && Boolean.parseBoolean(config.getSecure());
    }

}
