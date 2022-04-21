package io.smallrye.stork.servicediscovery.kubernetes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.vertx.core.Vertx;

public class KubernetesServiceRegistrar implements ServiceRegistrar<KubernetesMetadataKey> {
    private static final Logger log = LoggerFactory.getLogger(KubernetesServiceRegistrar.class);
    private final Vertx vertx;
    private final KubernetesRegistrarConfiguration config;

    public KubernetesServiceRegistrar(KubernetesRegistrarConfiguration config, String serviceRegistrarName,
            StorkInfrastructure infrastructure) {
        vertx = infrastructure.get(Vertx.class, Vertx::vertx);
        this.config = config;
    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata<KubernetesMetadataKey> metadata, String ipAddress) {
        return Uni.createFrom().emitter(em -> vertx.executeBlocking(future -> {
            registerKubernetesService(serviceName, metadata, ipAddress);
            future.complete();
        }, result -> {
            if (result.succeeded()) {
                log.info("Instances of service {} has been registered ", serviceName);
            } else {
                log.error("Unable to register instances of service {}", serviceName,
                        result.cause());
            }
        }));
    }

    private void registerKubernetesService(String application, Metadata<KubernetesMetadataKey> metadata, String ipAddress) {
        Config base = Config.autoConfigure(null);
        String masterUrl = config.getK8sHost() == null ? base.getMasterUrl() : config.getK8sHost();

        String namespace = (String) metadata.getMetadata().get(KubernetesMetadataKey.META_K8S_NAMESPACE);
        if (namespace == null) {
            namespace = base.getNamespace();
        }
        if (namespace == null) {
            throw new IllegalArgumentException("Namespace is not configured for service '" + application
                    + "'. Please provide a namespace. Use 'all' to discover services in all namespaces");
        }

        Config k8sConfig = new ConfigBuilder(base)
                .withMasterUrl(masterUrl)
                .withNamespace(namespace).build();
        try (KubernetesClient client = new DefaultKubernetesClient(k8sConfig)) {

            Map<String, String> serviceLabels = new HashMap<>();
            serviceLabels.put("app.kubernetes.io/name", "svc");
            serviceLabels.put("app.kubernetes.io/version", "1.0");

            registerBackendPods(application, namespace, serviceLabels, ipAddress, client);

            ObjectReference targetRef = new ObjectReference(null, null, "Pod",
                    application + "-" + ipAsSuffix(ipAddress), namespace, null, UUID.randomUUID().toString());
            EndpointAddress endpointAddress = new EndpointAddressBuilder().withIp(ipAddress).withTargetRef(targetRef)
                    .build();
            EndpointSubset endpointSubset = new EndpointSubsetBuilder().withAddresses(endpointAddress)
                    .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                    .build();
            // check if an endpoints already exists otherwise we will have a conflict error trying to create a new one with the same name
            Endpoints endpoints = client.endpoints().inNamespace(namespace).withName(application).get();
            if (endpoints != null) {
                Endpoints endpoint = new EndpointsBuilder(endpoints).addToSubsets(endpointSubset).build();
                client.endpoints().inNamespace(namespace).withName(application).patch(endpoint);
            } else {
                Endpoints newEndpoint = new EndpointsBuilder()
                        .withNewMetadata().withName(application).withLabels(serviceLabels).endMetadata()
                        .addToSubsets(new EndpointSubsetBuilder().withAddresses(endpointAddress)
                                .addToPorts(new EndpointPortBuilder().withPort(8080).build())
                                .build())
                        .build();
                client.endpoints().inNamespace(namespace).withName(application).create(newEndpoint);
            }
        }
    }

    private void registerBackendPods(String name, String namespace, Map<String, String> labels, String ipAdress,
            KubernetesClient client) {
        Map<String, String> podLabels = new HashMap<>(labels);
        podLabels.put("ui", "ui-" + ipAsSuffix(ipAdress));
        //TO DO create the pod with a container? Should we actually create the backendPod?
        Pod backendPod = new PodBuilder().withNewMetadata().withName(name + "-" + ipAsSuffix(ipAdress))
                .withLabels(podLabels)
                .endMetadata()
                .build();
        client.pods().inNamespace(namespace).create(backendPod);
    }

    private String ipAsSuffix(String ipAddress) {
        return ipAddress.replace(".", "");
    }
}
