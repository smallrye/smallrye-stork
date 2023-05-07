package io.smallrye.stork.servicediscovery.dns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.StorkAddressUtils;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.ServiceOptions;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class DnsServiceDiscoveryTest {
    private static final Logger log = LoggerFactory.getLogger(DnsServiceDiscoveryTest.class);

    public static final ExposedPort TCP_8500 = ExposedPort.tcp(8500);
    public static final ExposedPort UDP_8600 = ExposedPort.udp(8600);

    private final Set<String> registeredConsulServices = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Container
    public GenericContainer<?> consul = new GenericContainer<>(DockerImageName.parse("consul:1.9"))
            .withCreateContainerCmdModifier(cmd -> {
                HostConfig hostConfig = cmd.getHostConfig();
                Ports portBindings = hostConfig.getPortBindings();
                cmd.withExposedPorts(TCP_8500, UDP_8600);
                portBindings.bind(UDP_8600, Ports.Binding.empty());
                portBindings.bind(TCP_8500, Ports.Binding.empty());
                hostConfig.withPortBindings(portBindings);
                cmd.withHostConfig(hostConfig);
            })
            .waitingFor(Wait.forListeningPort());

    Stork stork;
    int consulPort;
    int dnsPort;
    ConsulClient client;
    long consulId;

    @BeforeEach
    void setUp() {
        TestConfigProvider.clear();
        Map<ExposedPort, Ports.Binding[]> portBindings = consul.getContainerInfo().getNetworkSettings().getPorts()
                .getBindings();
        consulPort = Integer.parseInt(portBindings.get(TCP_8500)[0].getHostPortSpec());
        dnsPort = Integer.parseInt(portBindings.get(UDP_8600)[0].getHostPortSpec());
        client = ConsulClient.create(Vertx.vertx(),
                new ConsulClientOptions().setHost("localhost").setPort(consulPort));
        stork = StorkTestUtils.getNewStorkInstance();

        // without waiting we're sometimes in a state where container is not ready yet for second test
        await().atMost(10, TimeUnit.SECONDS).until(this::clientCanTalkToConsul);
    }

    @AfterEach
    void cleanUp() throws InterruptedException {
        deregisterServiceInstances();
    }

    @Test
    void shouldGetInstancesForMavenOrg() {
        String serviceName = "maven";

        DnsConfiguration config = new DnsConfiguration().withHostname("maven.org").withRecordType("A")
                .withPort("8392");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        List<ServiceInstance> serviceInstances = getServiceInstances(serviceName, 20);
        assertThat(serviceInstances).isNotEmpty();
        for (ServiceInstance serviceInstance : serviceInstances) {
            assertThat(serviceInstance.getPort()).isEqualTo(8392);
        }

    }

    @Test
    void shouldGetServiceInstanceIdsFromDns() throws InterruptedException {
        //Given a service `my-service` registered in consul (available via DNS) and a refresh-period of 5 minutes
        String serviceName = "my-service";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp() + ":" + dnsPort)
                .withHostname("my-service.service.dc1.consul").withRefreshPeriod("5M")
                .withPort("8111");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        registerService(serviceName, "127.0.0.5:8406");

        List<ServiceInstance> instances = getServiceInstances(serviceName, 20);
        assertThat(instances).isNotEmpty();
        assertThat(instances.get(0).getHost()).isEqualTo("127.0.0.5");
    }

    @Test
    void shouldGetServiceInstanceIdsFromDnsWithoutResolving() throws InterruptedException {
        String serviceName = "my-service";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp() + ":" + dnsPort)
                .withHostname("my-service.service.dc1.consul").withRefreshPeriod("5M")
                .withPort("8111")
                .withResolveSrv("false");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        registerService(serviceName, "7f000005.addr.dc1.consul:8406");

        List<ServiceInstance> instances = getServiceInstances(serviceName, 20);
        assertThat(instances).isNotEmpty();
        assertThat(instances.get(0).getHost()).isEqualTo("7f000005.addr.dc1.consul");
    }

    @Test
    void shouldFailWithoutPortForA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp() + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul")
                .withRecordType("A");
        assertThatThrownBy(() -> stork.defineIfAbsent(serviceName, ServiceDefinition.of(config)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailWithoutPortForAAAA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp() + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul")
                .withRecordType("AAAA");
        assertThatThrownBy(() -> stork.defineIfAbsent(serviceName, ServiceDefinition.of(config)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFetchA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp() + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul")
                .withRecordType("A")
                .withPort("8111");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        // 8333 port won't be included in A record, so the result should have 8111
        registerService(serviceName, "127.0.0.5:8333", "127.0.0.6");

        List<ServiceInstance> serviceInstances = getServiceInstances(serviceName, 5);

        assertThat(serviceInstances).hasSize(2);

        ServiceInstance five = serviceInstances.stream().filter(i -> i.getHost().endsWith("5")).findFirst().get();
        ServiceInstance six = serviceInstances.stream().filter(i -> i.getHost().endsWith("6")).findFirst().get();

        assertThat(five.getPort()).isEqualTo(8111);
        assertThat(six.getPort()).isEqualTo(8111);
    }

    @Test
    void shouldFetchSRVWithAValues() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-x";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp() + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        // 8333 port won't be included in A record, so the result should have 8111
        int port1 = 8333;
        String ip1 = "127.1.1.23";
        int port2 = 8334;
        String ip2 = "127.1.1.24";
        registerService(serviceName, "[" + ip1 + "]:" + port1, "[" + ip2 + "]:" + port2);

        List<ServiceInstance> serviceInstances = getServiceInstances(serviceName, 5);
        assertThat(serviceInstances).hasSize(2);

        ServiceInstance first = serviceInstances.stream().filter(i -> i.getHost().equals(ip1)).findFirst().get();
        ServiceInstance second = serviceInstances.stream().filter(i -> i.getHost().equals(ip2)).findFirst().get();

        assertThat(first.getPort()).isEqualTo(port1);
        assertThat(second.getPort()).isEqualTo(port2);
        assertThat(first.getMetadata().getMetadata().get(DnsMetadataKey.DNS_WEIGHT)).isNotNull();
        assertThat(second.getPort()).isEqualTo(port2);
    }

    @Test
    void shouldFetchSRVWithAAAAValues() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-x";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp() + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        // 8333 port won't be included in A record, so the result should have 8111
        int port1 = 8333;
        String ip1 = "2001:db8:85a3:0:0:8a2e:370:7334";
        int port2 = 8334;
        String ip2 = "2001:db8:85a3:0:0:8a2e:370:7335";
        registerService(serviceName, "[" + ip1 + "]:" + port1, "[" + ip2 + "]:" + port2);

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

    @Test
    void shouldFetchAAAA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp() + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul")
                .withRecordType("AAAA")
                .withPort("8111");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        // 8333 port won't be included in A record, so the result should have 8111
        int port1 = 8333;
        String ip1 = "2001:db8:85a3:0:0:8a2e:370:7334";
        String ip2 = "2001:db8:85a3:0:0:8a2e:370:7335";
        registerService(serviceName, "[" + ip1 + "]:" + port1, ip2);

        List<ServiceInstance> serviceInstances = getServiceInstances(serviceName, 5);

        assertThat(serviceInstances).hasSize(2);

        ServiceInstance first = serviceInstances.stream().filter(i -> i.getHost().equals(ip1)).findFirst().get();
        ServiceInstance second = serviceInstances.stream().filter(i -> i.getHost().equals(ip2)).findFirst().get();

        assertThat(first.getPort()).isEqualTo(8111);
        assertThat(second.getPort()).isEqualTo(8111);
    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-2";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp() + ":" + dnsPort)
                .withHostname("my-service-2.service.dc1.consul").withRefreshPeriod("5s")
                .withRecordType("SRV");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        registerService(serviceName, "127.0.0.5:8406");

        List<ServiceInstance> serviceInstances = getServiceInstances(serviceName, 20);

        assertThat(serviceInstances).isNotEmpty();
        ServiceInstance firstInstance = serviceInstances.get(0);
        assertThat(firstInstance.getHost()).isEqualTo("127.0.0.5");
        long firstInstanceId = firstInstance.getId();

        deregisterServiceInstances();
        //the service settings change in consul
        registerService(serviceName, "127.0.0.6:8407", "127.0.0.5:8406");

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

    private String getDnsIp() {
        @SuppressWarnings("deprecation")
        String dnsIp = consul.getContainerIpAddress();

        if ("localhost".equals(dnsIp)) {
            dnsIp = "127.0.0.1";
        }
        return dnsIp;
    }

    private List<ServiceInstance> getServiceInstances(String serviceName, int seconds) {
        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);
        // call stork service discovery and gather service instances in the cache
        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Consul", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(seconds))
                .until(() -> instances.get() != null);
        return instances.get();
    }

    private void registerService(String application,
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

    private void deregisterServiceInstances() throws InterruptedException {
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

    private boolean clientCanTalkToConsul() {
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
