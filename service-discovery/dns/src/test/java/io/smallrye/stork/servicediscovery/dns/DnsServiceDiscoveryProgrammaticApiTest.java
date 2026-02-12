package io.smallrye.stork.servicediscovery.dns;

import static io.smallrye.stork.servicediscovery.dns.DnsServiceDiscoveryTestUtils.deregisterServiceInstances;
import static io.smallrye.stork.servicediscovery.dns.DnsServiceDiscoveryTestUtils.getDnsIp;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProvider;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class DnsServiceDiscoveryProgrammaticApiTest {
    private static final Logger log = Logger.getLogger(DnsServiceDiscoveryProgrammaticApiTest.class);

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
        await().atMost(10, TimeUnit.SECONDS).until(() -> DnsServiceDiscoveryTestUtils.clientCanTalkToConsul(client));
    }

    @AfterEach
    void cleanUp() throws InterruptedException {
        deregisterServiceInstances(client, log, registeredConsulServices);
    }

    @Test
    void shouldGetInstancesForMavenOrg() {
        String serviceName = "maven";

        DnsConfiguration config = new DnsConfiguration().withHostname("maven.org").withRecordType("A")
                .withPort("8392");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        DnsServiceDiscoveryTestUtils.shouldGetInstancesForMavenOrg(stork, serviceName);

    }

    @Test
    void shouldGetServiceInstanceIdsFromDns() throws InterruptedException {
        //Given a service `my-service` registered in consul (available via DNS) and a refresh-period of 5 minutes
        String serviceName = "my-service";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname("my-service.service.dc1.consul").withRefreshPeriod("5M")
                .withPort("8111");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        DnsServiceDiscoveryTestUtils.shouldGetServiceInstanceIdsFromDns(stork, client, serviceName, registeredConsulServices);

    }

    @Test
    void shouldGetServiceInstanceIdsFromDnsWithoutResolving() throws InterruptedException {
        String serviceName = "my-service";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname("my-service.service.dc1.consul").withRefreshPeriod("5M")
                .withPort("8111")
                .withResolveSrv("false");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        DnsServiceDiscoveryTestUtils.shouldGetServiceInstanceIdsFromDnsWithoutResolving(stork, client, serviceName,
                registeredConsulServices);
    }

    @Test
    void shouldFailWithoutPortForA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul")
                .withRecordType("A");
        assertThatThrownBy(() -> stork.defineIfAbsent(serviceName, ServiceDefinition.of(config)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailWithoutPortForAAAA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul")
                .withRecordType("AAAA");
        assertThatThrownBy(() -> stork.defineIfAbsent(serviceName, ServiceDefinition.of(config)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFetchA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul")
                .withRecordType("A")
                .withPort("8111");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        DnsServiceDiscoveryTestUtils.shouldFetchA(stork, client, serviceName,
                registeredConsulServices);
    }

    @Test
    void shouldFetchSRVWithAValues() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-x";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        DnsServiceDiscoveryTestUtils.shouldFetchSRVWithAValues(stork, client, serviceName,
                registeredConsulServices);
    }

    @Test
    void shouldFetchSRVWithAAAAValues() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-x";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        DnsServiceDiscoveryTestUtils.shouldFetchSRVWithAAAAValues(stork, client, serviceName,
                registeredConsulServices);
    }

    @Test
    void shouldFetchAAAA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname(serviceName + ".service.dc1.consul")
                .withRecordType("AAAA")
                .withPort("8111");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        DnsServiceDiscoveryTestUtils.shouldFetchAAAA(stork, client, serviceName,
                registeredConsulServices);

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-2";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname("my-service-2.service.dc1.consul").withRefreshPeriod("5s")
                .withRecordType("SRV");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));
        DnsServiceDiscoveryTestUtils.shouldRefetchWhenRefreshPeriodReached(stork, log, client, serviceName,
                registeredConsulServices);
    }

    @Test
    void shouldRefetchWhenCacheInvalidated() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-2";

        DnsConfiguration config = new DnsConfiguration().withDnsServers(getDnsIp(consul) + ":" + dnsPort)
                .withHostname("my-service-2.service.dc1.consul").withRefreshPeriod("5M")
                .withRecordType("SRV");
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(config));

        DnsServiceDiscoveryTestUtils.shouldRefetchWhenCacheInvalidated(stork, log, client, serviceName,
                registeredConsulServices);

    }

}
