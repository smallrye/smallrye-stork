package io.smallrye.stork.servicediscovery.dns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.utils.DurationUtils;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.ServiceInstanceIds;
import io.smallrye.stork.utils.ServiceInstanceUtils;
import io.smallrye.stork.utils.StorkAddressUtils;
import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.core.dns.SrvRecord;

/**
 * A service discovery implementation retrieving services from DNS.
 */
public class DnsServiceDiscovery extends CachingServiceDiscovery {

    private static final Logger log = LoggerFactory.getLogger(DnsServiceDiscovery.class);

    private final String serviceName;
    private final String hostname;
    private final DnsRecordType recordType;
    private final boolean secure;
    private final Integer port;
    private final boolean failOnError;
    private final long dnsTimeoutMs;
    private final boolean recursionDesired;
    private final boolean resolveSrv;

    // we'll use one resolver to resolve DNS server addresses and create another resolver backed up by them
    final Map<String, DnsClient> dnsClients = new HashMap<>();

    DnsServiceDiscovery(String serviceName, DnsConfiguration config, Vertx vertx) {
        super(config.getRefreshPeriod());
        this.serviceName = serviceName;
        this.secure = isSecure(config);
        this.recordType = recordType(config.getRecordType());
        this.failOnError = Boolean.parseBoolean(config.getFailOnError());
        this.recursionDesired = Boolean.parseBoolean(config.getRecursionDesired());
        this.resolveSrv = Boolean.parseBoolean(config.getResolveSrv());
        this.dnsTimeoutMs = DurationUtils.parseDuration(config.getDnsTimeout(), "DNS timeout")
                .toMillis();

        String dnsServersString = config.getDnsServers();

        if (dnsServersString != null && !dnsServersString.isBlank()
                && !"none".equalsIgnoreCase(dnsServersString)) {
            for (String dnsServer : dnsServersString.split(",")) {
                HostAndPort hostAndPort = StorkAddressUtils.parseToHostAndPort(dnsServer, 53,
                        "DNS server address for service " + serviceName);
                dnsClients.put(dnsServer, createClient(vertx, hostAndPort));
            }
        }
        if (dnsClients.isEmpty()) {
            dnsClients.put("Default DNS", createClient(vertx, null));
        }
        this.hostname = config.getHostname() == null ? serviceName : config.getHostname();

        try {
            this.port = config.getPort() == null ? null : Integer.parseInt(config.getPort());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port for service " + serviceName, e);
        }
        if (this.port == null && recordType != DnsRecordType.SRV) {
            throw new IllegalArgumentException(
                    "DNS service discovery for record types different than SRV require service instance port to be specified");
        }
    }

    private DnsRecordType recordType(String recordType) {
        String recordTypeString = recordType.toUpperCase(Locale.ROOT);
        try {
            return DnsRecordType.valueOf(recordTypeString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid DNS record type '" + recordType + "' for service " + serviceName +
                    ". The available types are " + Arrays.toString(DnsRecordType.values()), e);
        }
    }

    private DnsClient createClient(Vertx vertx, HostAndPort hostAndPort) {
        DnsClientOptions options = new DnsClientOptions()
                .setQueryTimeout(dnsTimeoutMs)
                .setRecursionDesired(recursionDesired);
        if (hostAndPort != null) {
            options.setHost(hostAndPort.host).setPort(hostAndPort.port);
        }
        return vertx.createDnsClient(options);
    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        AtomicInteger queriesLeft = new AtomicInteger(dnsClients.size());
        AtomicBoolean successRecorded = new AtomicBoolean();
        if (recordType == DnsRecordType.SRV) {
            return resolveSRV(previousInstances);
        } else {
            return resolveAorAAA(previousInstances, queriesLeft, successRecorded);
        }
    }

    private Uni<List<ServiceInstance>> resolveAorAAA(List<ServiceInstance> previousInstances, AtomicInteger queriesLeft,
            AtomicBoolean successRecorded) {
        Multi<ServiceInstance> serviceInstances = Multi.createFrom().emitter(
                em -> {
                    for (Map.Entry<String, DnsClient> dnsClient : dnsClients.entrySet()) {
                        DnsClient client = dnsClient.getValue();

                        switch (recordType) {
                            case A:
                                client.resolveA(hostname)
                                        .onFailure(error -> handleResolutionFailure(error, queriesLeft, em, dnsClient.getKey()))
                                        .onSuccess(lst -> handleStringResolution(lst, em, queriesLeft, previousInstances,
                                                successRecorded));
                                break;
                            case AAAA:
                                client.resolveAAAA(hostname)
                                        .onFailure(error -> handleResolutionFailure(error, queriesLeft, em, dnsClient.getKey()))
                                        .onSuccess(lst -> handleStringResolution(lst, em, queriesLeft, previousInstances,
                                                successRecorded));
                                break;
                            default:
                                em.fail(new IllegalStateException("Unsupported DNS record type " + recordType));
                                break;
                        }
                    }
                });
        return collectResults(successRecorded, serviceInstances);
    }

    private Uni<List<ServiceInstance>> collectResults(AtomicBoolean successRecorded, Multi<ServiceInstance> serviceInstances) {
        return serviceInstances.collect().asList()
                // we handle two kinds of results here:
                // 1. (at least one success && !failOnError) || all successful; incl all returning empty
                // 2. no success on either of queries
                .onItem().transformToUni(
                        result -> {
                            if (successRecorded.get()) {
                                return Uni.createFrom().item(result);
                            } else {
                                return Uni.createFrom().failure(
                                        new RuntimeException("No DNS server was able to resolve '" + hostname + '\''));
                            }
                        });
    }

    private Uni<List<ServiceInstance>> resolveSRV(List<ServiceInstance> previousInstances) {
        AtomicInteger queriesLeft = new AtomicInteger(dnsClients.size());
        AtomicBoolean successRecorded = new AtomicBoolean();
        // it may be okay to have a failure when resolving the SRV
        Multi<Tuple2<DnsClient, SrvRecord>> records = Multi.createFrom().emitter(
                em -> {
                    for (Map.Entry<String, DnsClient> clientEntry : dnsClients.entrySet()) {
                        DnsClient client = clientEntry.getValue();

                        client.resolveSRV(hostname)
                                .onFailure(error -> handleResolutionFailure(error, queriesLeft, em, clientEntry.getKey()))
                                .onSuccess(lst -> {
                                    successRecorded.set(true);
                                    for (SrvRecord record : lst) {
                                        em.emit(Tuple2.of(client, record));
                                    }
                                    if (queriesLeft.decrementAndGet() == 0) {
                                        em.complete();
                                    }
                                });
                    }
                });

        if (!resolveSrv) {
            Multi<ServiceInstance> targets = records.onItem().transformToUni(
                    record -> {
                        String target = record.getItem2().target();
                        return Uni.createFrom().item(toStorkServiceInstance(target, record.getItem2().port(),
                                record.getItem2().weight(), previousInstances));
                    }).concatenate();
            return collectResults(successRecorded, targets);
        }

        Multi<ServiceInstance> instances = records.onItem().transformToUni(
                record -> {
                    String target = record.getItem2().target();
                    DnsClient client = record.getItem1();
                    // TODO : an option to specify that one of these queries could be skipped
                    Uni<List<String>> aInstances = Uni.createFrom().emitter(em -> client.resolveA(target, addresses -> {
                        if (addresses.failed()) {
                            log.warn("Failed to lookup the address retrieved from DNS: " + target, addresses.cause());
                            em.complete(Collections.emptyList());
                        } else {
                            em.complete(addresses.result());
                        }
                    }));
                    Uni<List<String>> aaaaInstances = Uni.createFrom().emitter(em -> client.resolveAAAA(target, addresses -> {
                        if (addresses.failed()) {
                            log.warn("Failed to lookup the address retrieved from DNS: " + target, addresses.cause());
                            em.complete(Collections.emptyList());
                        } else {
                            em.complete(addresses.result());
                        }
                    }));
                    return Uni.combine().all().unis(aInstances, aaaaInstances)
                            .combinedWith((strings, strings2) -> {
                                List<String> result = new ArrayList<>(strings);
                                result.addAll(strings2);
                                if (result.isEmpty()) {
                                    log.warn("Failed to resolve ip address for target from SRV request: " + target);
                                }
                                return result;
                            }).onItem().transform(
                                    addresses -> addresses.stream()
                                            .map(address -> toStorkServiceInstance(address, record.getItem2().port(),
                                                    record.getItem2().weight(), previousInstances))
                                            .collect(Collectors.toList()));
                }).concatenate()
                .onItem().transformToMulti(l -> Multi.createFrom().iterable(l))
                .concatenate();

        return collectResults(successRecorded, instances);
    }

    private void handleStringResolution(List<String> lst, MultiEmitter<? super ServiceInstance> em, AtomicInteger queriesLeft,
            List<ServiceInstance> previousInstances, AtomicBoolean successRecorded) {
        for (String target : lst) {
            em.emit(toStorkServiceInstance(target, port, 1, previousInstances));
        }
        successRecorded.set(true);
        if (queriesLeft.decrementAndGet() == 0) {
            em.complete();
        }
    }

    private void handleResolutionFailure(Throwable error, AtomicInteger queriesLeft, MultiEmitter<?> em, String dnsServer) {
        String message = "Failure resolving name " + hostname + " with " + dnsServer;
        log.warn(message, error);
        if (failOnError) {
            em.fail(new RuntimeException(message, error));
        }
        if (queriesLeft.decrementAndGet() == 0) {
            em.complete();
        }
    }

    private ServiceInstance toStorkServiceInstance(String target, int port, int weight,
            List<ServiceInstance> previousInstances) {
        if (this.port != null) {
            port = this.port;
        }
        Metadata<DnsMetadataKey> dnsMetadata = createDnsMetadata(hostname, weight);

        ServiceInstance matching = ServiceInstanceUtils.findMatching(previousInstances, target, port);
        if (matching == null) {
            return new DefaultServiceInstance(ServiceInstanceIds.next(),
                    target, port, secure, dnsMetadata);
        } else {
            return matching;
        }
    }

    private Metadata<DnsMetadataKey> createDnsMetadata(String hostname, int weight) {
        Metadata<DnsMetadataKey> dnsMetadata = Metadata.of(DnsMetadataKey.class);
        dnsMetadata = dnsMetadata.with(DnsMetadataKey.DNS_NAME, hostname);
        dnsMetadata = dnsMetadata.with(DnsMetadataKey.DNS_WEIGHT, weight);
        return dnsMetadata;
    }

    private boolean isSecure(DnsConfiguration config) {
        return config.getSecure() != null && Boolean.parseBoolean(config.getSecure());
    }
}
