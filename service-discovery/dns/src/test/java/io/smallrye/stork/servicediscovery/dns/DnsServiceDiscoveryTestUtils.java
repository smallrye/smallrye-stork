package io.smallrye.stork.servicediscovery.dns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.StorkAddressUtils;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ServiceOptions;

public class DnsServiceDiscoveryTestUtils {

    private static long consulId = 0L;

    public static void shouldGetInstancesForMavenOrg(Stork stork, String serviceName) {
        List<ServiceInstance> serviceInstances = getServiceInstances(stork, serviceName, 20);
        assertThat(serviceInstances).isNotEmpty();
        for (ServiceInstance serviceInstance : serviceInstances) {
            assertThat(serviceInstance.getPort()).isEqualTo(8392);
        }
    }

    public static void shouldGetServiceInstanceIdsFromDns(Stork stork, ConsulClient client, String serviceName,
            Set<String> registeredConsulServices) throws InterruptedException {
        registerService(client, registeredConsulServices, serviceName, "127.0.0.5:8406");

        List<ServiceInstance> instances = getServiceInstances(stork, serviceName, 20);
        assertThat(instances).isNotEmpty();
        assertThat(instances.get(0).getHost()).isEqualTo("127.0.0.5");
    }

    public static void shouldGetServiceInstanceIdsFromDnsWithoutResolving(Stork stork, ConsulClient client, String serviceName,
            Set<String> registeredConsulServices) throws InterruptedException {
        registerService(client, registeredConsulServices, serviceName, "7f000005.addr.dc1.consul:8406");

        List<ServiceInstance> instances = getServiceInstances(stork, serviceName, 20);
        assertThat(instances).isNotEmpty();
        assertThat(instances.get(0).getHost()).isEqualTo("7f000005.addr.dc1.consul");
    }

    public static void shouldFetchA(Stork stork, ConsulClient client, String serviceName,
            Set<String> registeredConsulServices) throws InterruptedException {
        registerService(client, registeredConsulServices, serviceName, "127.0.0.5:8333", "127.0.0.6");

        List<ServiceInstance> serviceInstances = getServiceInstances(stork, serviceName, 5);

        assertThat(serviceInstances).hasSize(2);

        ServiceInstance five = serviceInstances.stream().filter(i -> i.getHost().endsWith("5")).findFirst().get();
        ServiceInstance six = serviceInstances.stream().filter(i -> i.getHost().endsWith("6")).findFirst().get();

        assertThat(five.getPort()).isEqualTo(8111);
        assertThat(six.getPort()).isEqualTo(8111);
    }

    public static void shouldFetchSRVWithAValues(Stork stork, ConsulClient client, String serviceName,
            Set<String> registeredConsulServices) throws InterruptedException {
        // 8333 port won't be included in A record, so the result should have 8111
        int port1 = 8333;
        String ip1 = "127.1.1.23";
        int port2 = 8334;
        String ip2 = "127.1.1.24";
        registerService(client, registeredConsulServices, serviceName, "[" + ip1 + "]:" + port1, "[" + ip2 + "]:" + port2);

        List<ServiceInstance> serviceInstances = getServiceInstances(stork, serviceName, 5);
        assertThat(serviceInstances).hasSize(2);

        ServiceInstance first = serviceInstances.stream().filter(i -> i.getHost().equals(ip1)).findFirst().get();
        ServiceInstance second = serviceInstances.stream().filter(i -> i.getHost().equals(ip2)).findFirst().get();

        assertThat(first.getPort()).isEqualTo(port1);
        assertThat(second.getPort()).isEqualTo(port2);
        assertThat(first.getMetadata().getMetadata().get(DnsMetadataKey.DNS_WEIGHT)).isNotNull();
        assertThat(second.getPort()).isEqualTo(port2);
    }

    public static void shouldFetchSRVWithAAAAValues(Stork stork, ConsulClient client, String serviceName,
            Set<String> registeredConsulServices) throws InterruptedException {
        // 8333 port won't be included in A record, so the result should have 8111
        int port1 = 8333;
        String ip1 = "2001:db8:85a3:0:0:8a2e:370:7334";
        int port2 = 8334;
        String ip2 = "2001:db8:85a3:0:0:8a2e:370:7335";
        registerService(client, registeredConsulServices, serviceName, "[" + ip1 + "]:" + port1, "[" + ip2 + "]:" + port2);

        Service service = stork.getService(serviceName);
        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        List<ServiceInstance> serviceInstances = instances.get();
        assertThat(serviceInstances).hasSize(2);

        ServiceInstance first = serviceInstances.stream().filter(i -> i.getHost().equals(ip1)).findFirst().get();
        ServiceInstance second = serviceInstances.stream().filter(i -> i.getHost().equals(ip2)).findFirst().get();

        assertThat(first.getPort()).isEqualTo(port1);
        assertThat(second.getPort()).isEqualTo(port2);
    }

    public static void shouldFetchAAAA(Stork stork, ConsulClient client, String serviceName,
            Set<String> registeredConsulServices) throws InterruptedException {
        // 8333 port won't be included in A record, so the result should have 8111
        int port1 = 8333;
        String ip1 = "2001:db8:85a3:0:0:8a2e:370:7334";
        String ip2 = "2001:db8:85a3:0:0:8a2e:370:7335";
        registerService(client, registeredConsulServices, serviceName, "[" + ip1 + "]:" + port1, ip2);

        List<ServiceInstance> serviceInstances = getServiceInstances(stork, serviceName, 5);

        assertThat(serviceInstances).hasSize(2);

        ServiceInstance first = serviceInstances.stream().filter(i -> i.getHost().equals(ip1)).findFirst().get();
        ServiceInstance second = serviceInstances.stream().filter(i -> i.getHost().equals(ip2)).findFirst().get();

        assertThat(first.getPort()).isEqualTo(8111);
        assertThat(second.getPort()).isEqualTo(8111);
    }

    public static void shouldRefetchWhenRefreshPeriodReached(Stork stork, Logger log, ConsulClient client, String serviceName,
            Set<String> registeredConsulServices) throws InterruptedException {
        registerService(client, registeredConsulServices, serviceName, "127.0.0.5:8406");

        List<ServiceInstance> serviceInstances = getServiceInstances(stork, serviceName, 20);

        assertThat(serviceInstances).isNotEmpty();
        ServiceInstance firstInstance = serviceInstances.get(0);
        assertThat(firstInstance.getHost()).isEqualTo("127.0.0.5");
        long firstInstanceId = firstInstance.getId();

        deregisterServiceInstances(client, log, registeredConsulServices);
        //the service settings change in consul
        registerService(client, registeredConsulServices, serviceName, "127.0.0.6:8407", "127.0.0.5:8406");

        Service service = stork.getService(serviceName);
        // let's wait until the new services are populated to Stork (from DNS)
        await().atMost(Duration.ofSeconds(7))
                .until(() -> service.getServiceDiscovery().getServiceInstances().await().indefinitely().size() == 2);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        serviceInstances = instances.get();
        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder(
                "127.0.0.5", "127.0.0.6");
        long firstInstanceIdAfterRefetch = serviceInstances.stream()
                .filter(instance -> instance.getHost().equals("127.0.0.5"))
                .findFirst()
                .orElseThrow()
                .getId();
        assertThat(firstInstanceId).isEqualTo(firstInstanceIdAfterRefetch);

        ServiceInstance five = serviceInstances.stream().filter(i -> i.getHost().endsWith("5")).findFirst().get();
        ServiceInstance six = serviceInstances.stream().filter(i -> i.getHost().endsWith("6")).findFirst().get();

        assertThat(five.getPort()).isEqualTo(8406);
        assertThat(six.getPort()).isEqualTo(8407);
    }

    public static void shouldRefetchWhenCacheInvalidated(Stork stork, Logger log, ConsulClient client, String serviceName,
            Set<String> registeredConsulServices) throws InterruptedException {

        Service service = stork.getService(serviceName);
        registerService(client, registeredConsulServices, serviceName, "127.0.0.5:8406");

        List<ServiceInstance> serviceInstances = getServiceInstances(stork, serviceName, 20);

        assertThat(serviceInstances).isNotEmpty();
        ServiceInstance firstInstance = serviceInstances.get(0);
        assertThat(firstInstance.getHost()).isEqualTo("127.0.0.5");
        assertThat(firstInstance.getPort()).isEqualTo(8406);

        deregisterServiceInstances(client, log, registeredConsulServices);

        //the service settings change
        registerService(client, registeredConsulServices, serviceName, "127.0.0.6:8407", "127.0.0.5:8406");

        //refresh period not yet reached, we don't get the new settings just registered
        serviceInstances = getServiceInstances(stork, serviceName, 20);

        assertThat(serviceInstances).isNotEmpty();
        firstInstance = serviceInstances.get(0);
        assertThat(firstInstance.getHost()).isEqualTo("127.0.0.5");
        assertThat(firstInstance.getPort()).isEqualTo(8406);

        //Force cache invalidation
        DnsServiceDiscovery dnsServiceDiscovery = (DnsServiceDiscovery) service.getServiceDiscovery();
        dnsServiceDiscovery.invalidate();

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(10))
                .until(() -> instances.get() != null);

        serviceInstances = instances.get();
        assertThat(serviceInstances.stream().map(ServiceInstance::getHost)).containsExactlyInAnyOrder(
                "127.0.0.5", "127.0.0.6");
        ServiceInstance five = serviceInstances.stream().filter(i -> i.getHost().endsWith("5")).findFirst().get();
        ServiceInstance six = serviceInstances.stream().filter(i -> i.getHost().endsWith("6")).findFirst().get();

        assertThat(five.getPort()).isEqualTo(8406);
        assertThat(six.getPort()).isEqualTo(8407);
    }

    public static String getDnsIp(GenericContainer<?> consul) {
        @SuppressWarnings("deprecation")
        String dnsIp = consul.getContainerIpAddress();

        if ("localhost".equals(dnsIp)) {
            dnsIp = "127.0.0.1";
        }
        return dnsIp;
    }

    public static List<ServiceInstance> getServiceInstances(Stork stork, String serviceName, int timeout) {
        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(timeout))
                .until(() -> instances.get() != null);
        return instances.get();
    }

    public static void registerService(ConsulClient client, Set<String> registeredConsulServices, String application,
            String... addresses) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(addresses.length);
        Set<String> consulServiceIds = new HashSet<>();
        for (String addressString : addresses) {
            HostAndPort address = StorkAddressUtils.parseToHostAndPort(addressString, 0, "");
            String consulServiceId = "" + (consulId++);
            client.registerService(
                    new ServiceOptions().setId(consulServiceId).setName(application)
                            .setAddress(address.host).setPort(address.port))
                    .onComplete(result -> {
                        if (result.failed()) {
                            fail("Failed to register service in Consul " + address, result.cause());
                        } else {
                            consulServiceIds.add(consulServiceId);
                            latch.countDown();
                        }
                    });
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Failed to register service in consul in time");
        }
        registeredConsulServices.addAll(consulServiceIds);
    }

    public static void deregisterServiceInstances(ConsulClient client, Logger log, Set<String> registeredConsulServices)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(registeredConsulServices.size());
        for (String id : registeredConsulServices) {

            log.info("unregistering service {}", id);
            client.deregisterService(id, res -> {
                if (res.succeeded()) {
                    log.info("unregistered service {}", id);
                    latch.countDown();
                } else {
                    fail("Failed to deregister service in consul", res.cause());
                }
            });
        }
        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Failed to deregister service in consul in time");
        }
        registeredConsulServices.clear();
    }

    public static boolean clientCanTalkToConsul(ConsulClient client) {
        var done = new CompletableFuture<>();
        client.agentInfo().onFailure(done::completeExceptionally)
                .onSuccess(done::complete);
        try {
            done.get();
            return true;
        } catch (Exception any) {
            return false;
        }
    }

}
