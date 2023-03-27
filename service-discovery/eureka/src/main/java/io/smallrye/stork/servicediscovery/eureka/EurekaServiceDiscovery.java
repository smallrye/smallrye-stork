package io.smallrye.stork.servicediscovery.eureka;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.utils.ServiceInstanceIds;
import io.smallrye.stork.utils.ServiceInstanceUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

/**
 * Handle service discovery using a Eureka server.
 * <p>
 * It does not use the Eureka client which brings lots of dependencies on the classpath.
 * Eureka exposes a REST API, which can be used easily.
 * The REST API is described on https://github.com/Netflix/eureka/wiki/Eureka-REST-operations.
 */
public class EurekaServiceDiscovery extends CachingServiceDiscovery {

    private final WebClient client;
    private final String path;
    private final boolean secure;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<String> instance;

    /**
     * Creates a new EurekaServiceDiscovery
     *
     * @param config the configuration
     * @param serviceName the service name
     * @param infrastructure the infrastructure
     */
    public EurekaServiceDiscovery(EurekaConfiguration config, String serviceName,
            StorkInfrastructure infrastructure) {
        super(config.getRefreshPeriod());
        this.secure = isSecure(config);
        Vertx vertx = infrastructure.get(Vertx.class, Vertx::vertx);

        // Eureka instance
        String host = config.getEurekaHost();
        int port = Integer.parseInt(config.getEurekaPort());
        boolean trustAll = Boolean.parseBoolean(config.getEurekaTrustAll());
        boolean eurekaTls = Boolean.parseBoolean(config.getEurekaTls());

        // Service selection
        String app = config.getApplication() == null ? serviceName : config.getApplication();
        instance = Optional.ofNullable(config.getInstance()); // null is okay, me thinks

        client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(host).setDefaultPort(port).setSsl(eurekaTls).setTrustAll(trustAll));
        String contextPath = config.getEurekaContextPath();
        if (!contextPath.endsWith("/")) {
            contextPath += "/";
        }
        path = contextPath + "eureka/apps/" + app;
    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        Uni<HttpResponse<Buffer>> retrieval = client.get(path)
                .putHeader("Accept", "application/json;charset=UTF-8").send();

        return retrieval
                .map(this::getEurekaApplicationInstances)
                .map(this::selectAliveInstances)
                .map(this::selectSecureInstancesIfEnabled)
                .map(this::selectChosenInstanceIfEnabled)
                .map(appInstances -> toStorkServiceInstances(appInstances, previousInstances));
    }

    private Stream<ApplicationInstance> selectSecureInstancesIfEnabled(Stream<ApplicationInstance> stream) {
        if (!secure) {
            return stream;
        } else {
            return stream.filter(i -> i.securePort.enabled);
        }
    }

    private Stream<ApplicationInstance> selectChosenInstanceIfEnabled(Stream<ApplicationInstance> stream) {
        if (instance.isEmpty()) {
            return stream;
        } else {
            return stream.filter(i -> i.instanceId.equalsIgnoreCase(instance.get()));
        }
    }

    private List<ServiceInstance> toStorkServiceInstances(Stream<ApplicationInstance> instances,
            List<ServiceInstance> previousInstances) {
        return instances
                .map(instance -> {
                    String virtualAddress;
                    int port;

                    if (secure && instance.securePort.enabled) {
                        virtualAddress = instance.secureVipAddress;
                        if (virtualAddress == null) {
                            virtualAddress = instance.vipAddress;
                        }
                        port = instance.securePort.port;
                    } else {
                        virtualAddress = instance.vipAddress;
                        port = instance.port.port;
                    }
                    ServiceInstance matching = ServiceInstanceUtils.findMatching(previousInstances, virtualAddress, port);
                    return matching == null
                            ? new DefaultServiceInstance(ServiceInstanceIds.next(), virtualAddress, port, secure)
                            : matching;
                })
                .collect(Collectors.toList());
    }

    private Stream<ApplicationInstance> getEurekaApplicationInstances(HttpResponse<Buffer> response) {
        if (response.statusCode() == 404) {
            return Stream.empty();
        }

        // If it's not 404, it must be 200
        ensure200(response);

        JsonArray array = response.bodyAsJsonObject()
                .getJsonObject("application")
                .getJsonArray("instance");

        return array.stream().map(o -> ((JsonObject) o).mapTo(ApplicationInstance.class));
    }

    private Stream<ApplicationInstance> selectAliveInstances(Stream<ApplicationInstance> instances) {
        return instances
                .filter(ApplicationInstance::isUp);
    }

    private void ensure200(HttpResponse<Buffer> resp) {
        if (resp.statusCode() != 200) {
            throw new RuntimeException(
                    "Unable to retrieve services from Eureka, expected as 200-OK response, but got " + resp.statusCode()
                            + ", body is: " + resp.bodyAsString());
        }
    }

    private boolean isSecure(EurekaConfiguration config) {
        return config.getSecure() != null && Boolean.parseBoolean(config.getSecure());
    }
}
