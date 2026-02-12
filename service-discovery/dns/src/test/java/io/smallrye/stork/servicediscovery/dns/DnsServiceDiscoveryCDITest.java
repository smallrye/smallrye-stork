package io.smallrye.stork.servicediscovery.dns;

import static io.smallrye.stork.servicediscovery.dns.DnsServiceDiscoveryTestUtils.getDnsIp;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;

import io.smallrye.stork.Stork;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProviderBean;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
@ExtendWith(WeldJunit5Extension.class)
public class DnsServiceDiscoveryCDITest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(TestConfigProviderBean.class,
            DnsServiceDiscoveryProviderLoader.class);

    @Inject
    TestConfigProviderBean config;

    private static final Logger log = Logger.getLogger(DnsServiceDiscoveryCDITest.class);

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
        config.clear();
        // Starting and wiring the container can take a bit of time.
        // So, let's be defensive.
        await().until(() -> consul.getContainerInfo() != null && consul.getContainerInfo().getNetworkSettings() != null
                && consul.getContainerInfo().getNetworkSettings().getPorts() != null
                && consul.getContainerInfo().getNetworkSettings().getPorts().getBindings() != null);
        consulPort = await()
                .until(() -> {
                    Map<ExposedPort, Ports.Binding[]> bindings = consul.getContainerInfo().getNetworkSettings().getPorts()
                            .getBindings();
                    if (bindings.get(TCP_8500) == null || bindings.get(TCP_8500)[0] == null) {
                        return null;
                    }
                    return Integer.parseInt(bindings.get(TCP_8500)[0].getHostPortSpec());
                }, notNullValue());
        dnsPort = await()
                .until(() -> {
                    Map<ExposedPort, Ports.Binding[]> bindings = consul.getContainerInfo().getNetworkSettings().getPorts()
                            .getBindings();
                    if (bindings.get(UDP_8600) == null || bindings.get(UDP_8600)[0] == null) {
                        return null;
                    }
                    return Integer.parseInt(bindings.get(UDP_8600)[0].getHostPortSpec());
                }, notNullValue());

        client = ConsulClient.create(Vertx.vertx(),
                new ConsulClientOptions().setHost("localhost").setPort(consulPort));
        stork = StorkTestUtils.getNewStorkInstance();

        // without waiting we're sometimes in a state where container is not ready yet for second test
        await().atMost(10, TimeUnit.SECONDS).until(() -> DnsServiceDiscoveryTestUtils.clientCanTalkToConsul(client));
    }

    @AfterEach
    void cleanUp() throws InterruptedException {
        DnsServiceDiscoveryTestUtils.deregisterServiceInstances(client, log, registeredConsulServices);
    }

    @Test
    void shouldGetInstancesForMavenOrg() {
        config.addServiceConfig("maven", null, "dns", null,
                null, Map.of("hostname", "maven.org", "record-type", "A", "port", "8392", "refresh-period", "5M"),
                null);
        String serviceName = "maven";
        stork = StorkTestUtils.getNewStorkInstance();

        DnsServiceDiscoveryTestUtils.shouldGetInstancesForMavenOrg(stork, serviceName);

    }

    @Test
    void shouldGetServiceInstanceIdsFromDns() throws InterruptedException {
        //Given a service `my-service` registered in consul (available via DNS) and a refresh-period of 5 minutes
        String serviceName = "my-service";

        config.addServiceConfig("my-service", null, "dns", null,
                null,
                Map.of("hostname", "my-service.service.dc1.consul", "dns-servers", getDnsIp(consul) + ":" + dnsPort, "port",
                        "8111", "refresh-period", "5M"),
                null);

        stork = StorkTestUtils.getNewStorkInstance();
        DnsServiceDiscoveryTestUtils.shouldGetServiceInstanceIdsFromDns(stork, client, serviceName, registeredConsulServices);

    }

    @Test
    void shouldGetServiceInstanceIdsFromDnsWithoutResolving() throws InterruptedException {
        //Given a service `my-service` registered in consul (available via DNS) and a refresh-period of 5 minutes
        String serviceName = "my-service";

        config.addServiceConfig("my-service", null, "dns", null,
                null,
                Map.of("hostname", "my-service.service.dc1.consul", "dns-servers", getDnsIp(consul) + ":" + dnsPort, "port",
                        "8111", "resolve-srv", "false", "refresh-period", "5M"),
                null);

        stork = StorkTestUtils.getNewStorkInstance();

        DnsServiceDiscoveryTestUtils.shouldGetServiceInstanceIdsFromDnsWithoutResolving(stork, client, serviceName,
                registeredConsulServices);
    }

    @Test
    void shouldFailWithoutPortForA() throws InterruptedException {
        //Given a service `my-service-3` registered in consul and a refresh-period of 5 minutes
        config.addServiceConfig("my-service", null, "dns", null,
                null,
                Map.of("hostname", "my-service.service.dc1.consul", "dns-servers", getDnsIp(consul) + ":" + dnsPort,
                        "record-type",
                        "A"),
                null);

        assertThatThrownBy(() -> StorkTestUtils.getNewStorkInstance())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFailWithoutPortForAAAA() throws InterruptedException {
        config.addServiceConfig("my-service", null, "dns", null,
                null,
                Map.of("hostname", "my-service.service.dc1.consul", "dns-servers", getDnsIp(consul) + ":" + dnsPort,
                        "record-type",
                        "AAAA"),
                null);

        assertThatThrownBy(() -> StorkTestUtils.getNewStorkInstance())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldFetchA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        config.addServiceConfig(serviceName, null, "dns", null,
                null,
                Map.of("hostname", "my-service-3.service.dc1.consul", "dns-servers", getDnsIp(consul) + ":" + dnsPort, "port",
                        "8111", "record-type", "A"),
                null);

        stork = StorkTestUtils.getNewStorkInstance();

        DnsServiceDiscoveryTestUtils.shouldFetchA(stork, client, serviceName, registeredConsulServices);
    }

    @Test
    void shouldFetchSRVWithAValues() throws InterruptedException {
        //Given a service `my-service-x` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-x";

        config.addServiceConfig(serviceName, null, "dns", null,
                null,
                Map.of("hostname", "my-service-x.service.dc1.consul", "dns-servers", getDnsIp(consul) + ":" + dnsPort),
                null);

        stork = StorkTestUtils.getNewStorkInstance();

        DnsServiceDiscoveryTestUtils.shouldFetchSRVWithAValues(stork, client, serviceName,
                registeredConsulServices);

    }

    @Test
    void shouldFetchSRVWithAAAAValues() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-x";

        config.addServiceConfig(serviceName, null, "dns", null,
                null,
                Map.of("hostname", "my-service-x.service.dc1.consul", "dns-servers", getDnsIp(consul) + ":" + dnsPort),
                null);

        stork = StorkTestUtils.getNewStorkInstance();

        DnsServiceDiscoveryTestUtils.shouldFetchSRVWithAAAAValues(stork, client, serviceName,
                registeredConsulServices);
    }

    @Test
    void shouldFetchAAAA() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-3";

        config.addServiceConfig(serviceName, null, "dns", null,
                null,
                Map.of("hostname", serviceName + ".service.dc1.consul", "record-type", "AAAA", "port", "8111", "dns-servers",
                        getDnsIp(consul) + ":" + dnsPort, "refresh-period", "5M"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();

        DnsServiceDiscoveryTestUtils.shouldFetchAAAA(stork, client, serviceName,
                registeredConsulServices);

    }

    @Test
    void shouldRefetchWhenRefreshPeriodReached() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-2";

        config.addServiceConfig(serviceName, null, "dns", null,
                null,
                Map.of("hostname", "my-service-2.service.dc1.consul", "record-type", "SRV", "dns-servers",
                        getDnsIp(consul) + ":" + dnsPort, "refresh-period", "5s"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();

        DnsServiceDiscoveryTestUtils.shouldRefetchWhenRefreshPeriodReached(stork, log, client, serviceName,
                registeredConsulServices);
    }

    @Test
    void shouldRefetchWhenCacheInvalidated() throws InterruptedException {
        //Given a service `my-service-2` registered in consul and a refresh-period of 5 minutes
        String serviceName = "my-service-2";

        config.addServiceConfig(serviceName, null, "dns", null,
                null,
                Map.of("hostname", "my-service-2.service.dc1.consul", "record-type", "SRV", "dns-servers",
                        getDnsIp(consul) + ":" + dnsPort, "refresh-period", "5M"),
                null);
        stork = StorkTestUtils.getNewStorkInstance();

        DnsServiceDiscoveryTestUtils.shouldRefetchWhenCacheInvalidated(stork, log, client, serviceName,
                registeredConsulServices);

    }

}
