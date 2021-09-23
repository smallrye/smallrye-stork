package io.smallrye.stork.servicediscovery.consul;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.DefaultServiceInstance;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceInstanceIds;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceEntry;
import io.vertx.ext.consul.ServiceEntryList;

public class ConsulServiceDiscovery implements ServiceDiscovery {

    public static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofMinutes(5);
    private static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");
    private final ConsulClient client;
    private final String serviceName;
    private final Duration refreshPeriod;
    private boolean passing = true; // default true?

    private volatile ServiceInstancesFetchResult fetchResult;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulServiceDiscovery.class);

    public ConsulServiceDiscovery(String serviceName, ServiceDiscoveryConfig config, Vertx vertx) {
        this.serviceName = serviceName;

        ConsulClientOptions options = new ConsulClientOptions();
        Map<String, String> parameters = config.parameters();
        String host = parameters.get("consul-host");
        if (host != null) {
            options.setHost(host);
        }
        String port = parameters.get("consul-port");
        if (port != null) {
            try {
                options.setPort(Integer.parseInt(port));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Port not parseable to int: " + port + " for service " + serviceName);
            }
        }
        String passingConfig = parameters.get("use-health-checks");
        if (passingConfig != null) {
            LOGGER.info("Processing Consul use-health-checks configured value: {}", passingConfig);
            passing = Boolean.parseBoolean(passingConfig);
        }
        client = ConsulClient.create(vertx, options);

        String refreshPeriod = config.parameters().get("refresh-period");
        try {
            // TODO: document it
            this.refreshPeriod = refreshPeriod != null
                    ? parseDuration(refreshPeriod)
                    : DEFAULT_REFRESH_INTERVAL;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("refresh-period for service discovery should be a number, got: " +
                    refreshPeriod,
                    e);
        }
    }

    /**
     * Converts a value representing the refresh period which start with a number by implicitly appending `PT` to it.
     * If the value consists only of a number, it implicitly treats the value as seconds.
     * Otherwise, tries to convert the value assuming that it is in the accepted ISO-8601 duration format.
     *
     * @param refreshPeriod duration as String
     * @return {@link Duration}
     */
    private Duration parseDuration(String refreshPeriod) {
        if (refreshPeriod.startsWith("-")) {
            throw new IllegalArgumentException("Negative refresh-period specified for service discovery: " + refreshPeriod);
        }
        if (DIGITS.asPredicate().test(refreshPeriod)) {
            return Duration.ofSeconds(Long.valueOf(refreshPeriod));
        }
        return Duration.parse("PT" + refreshPeriod);

    }

    public Uni<List<ServiceInstance>> getServiceInstances() {
        if (refreshNotNeed()) {
            return Uni.createFrom().item(fetchResult.serviceInstances);
        }

        return fetchNewServiceInstances();
    }

    private boolean refreshNotNeed() {
        return fetchResult != null
                && fetchResult.date.isAfter(LocalDateTime.now().minus(refreshPeriod));
    }

    private Uni<List<ServiceInstance>> fetchNewServiceInstances() {
        Uni<ServiceEntryList> serviceEntryList = Uni.createFrom().emitter(
                emitter -> client.healthServiceNodes(serviceName, passing)
                        .onComplete(result -> {
                            if (result.failed()) {
                                emitter.fail(result.cause());
                            } else {
                                emitter.complete(result.result());
                            }
                        }));
        return serviceEntryList.onItem().transform(this::map)
                .onItem().invoke(services -> this.fetchResult = new ServiceInstancesFetchResult(services, LocalDateTime.now())); // TODO: logging
    }

    private List<ServiceInstance> map(ServiceEntryList serviceEntryList) {
        List<ServiceEntry> list = serviceEntryList.getList();
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        for (ServiceEntry serviceEntry : list) {
            // TODO: reuse service instance IDs on refresh (so that they don't change)
            ServiceInstance serviceInstance = new DefaultServiceInstance(ServiceInstanceIds.next(),
                    serviceEntry.getService().getAddress(), serviceEntry.getService().getPort());
            serviceInstances.add(serviceInstance);
        }
        return serviceInstances;
    }

    public static class ServiceInstancesFetchResult {
        List<ServiceInstance> serviceInstances;
        LocalDateTime date;

        public ServiceInstancesFetchResult(List<ServiceInstance> serviceInstances, LocalDateTime date) {
            this.serviceInstances = serviceInstances;
            this.date = date;
        }
    }
}
