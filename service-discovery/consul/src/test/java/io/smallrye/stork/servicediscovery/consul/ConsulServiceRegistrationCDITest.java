package io.smallrye.stork.servicediscovery.consul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProviderBean;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
@ExtendWith(WeldJunit5Extension.class)
public class ConsulServiceRegistrationCDITest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(TestConfigProviderBean.class,
            ConsulServiceDiscoveryProviderLoader.class);

    @Inject
    TestConfigProviderBean config;

    @Container
    public GenericContainer<?> consul = new GenericContainer<>(DockerImageName.parse("consul:1.9"))
            .withExposedPorts(8500);

    Stork stork;
    int consulPort;
    ConsulClient client;
    long consulId;

    @BeforeEach
    void setUp() {
        config.clear();
        consulPort = consul.getMappedPort(8500);
        consulId = 0L;
        client = ConsulClient.create(Vertx.vertx(),
                new ConsulClientOptions().setHost("localhost").setPort(consulPort));
    }

    @Test
    void shouldRegisterServiceInstancesInConsul() throws InterruptedException {
        config.addServiceConfig("my-service", null, "consul",
                null, Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort), "refresh-period", "5"));
        config.addServiceRegistrarConfig("my-consul-registrar", "consul",
                Map.of("consul-host", "localhost", "consul-port", String.valueOf(consulPort)));
        Stork stork = StorkTestUtils.getNewStorkInstance();

        String serviceName = "my-service";

        ServiceRegistrar<ConsulMetadataKey> consulRegistrar = stork.getServiceRegistrar("my-consul-registrar");

        CountDownLatch registrationLatch = new CountDownLatch(1);
        consulRegistrar.registerServiceInstance(serviceName, Metadata.of(ConsulMetadataKey.class)
                .with(ConsulMetadataKey.META_CONSUL_SERVICE_ID, serviceName), "10.96.96.231", 8406).subscribe()
                .with(success -> registrationLatch.countDown(), failure -> fail(""));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> registrationLatch.getCount() == 0L);

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        Service service = stork.getService(serviceName);

        service.getServiceDiscovery().getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances from Kubernetes", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(20))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(1);
    }
}
