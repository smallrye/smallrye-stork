package io.smallrye.stork.servicediscovery.eureka;

import static io.smallrye.stork.servicediscovery.eureka.EurekaDiscoveryTest.registerApplicationInstance;
import static io.smallrye.stork.servicediscovery.eureka.EurekaDiscoveryTest.unregisterAll;
import static io.smallrye.stork.servicediscovery.eureka.EurekaDiscoveryTest.updateApplicationInstanceStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.weld.junit5.ExplicitParamInjection;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProviderBean;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@Testcontainers
@DisabledOnOs(OS.WINDOWS)
@ExtendWith(WeldJunit5Extension.class)
@ExplicitParamInjection
public class EurekaDiscoveryCDITest {

    private static final Logger log = LoggerFactory.getLogger(EurekaDiscoveryCDITest.class);
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(TestConfigProviderBean.class,
            EurekaServiceDiscoveryProviderLoader.class);

    public static final int EUREKA_PORT = 8761;

    @Container
    public GenericContainer<?> eureka = new GenericContainer<>(DockerImageName.parse("quay.io/amunozhe/eureka-server:0.3"))
            .withExposedPorts(EUREKA_PORT);

    private static Vertx vertx = Vertx.vertx();;
    private WebClient client;

    public int port;
    public String host;

    @BeforeEach
    public void init() {
        client = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost(eureka.getHost())
                .setDefaultPort(eureka.getMappedPort(EUREKA_PORT)));
        port = eureka.getMappedPort(EUREKA_PORT);
        host = eureka.getHost();
    }

    @AfterEach
    public void cleanup() {
        unregisterAll(client);
        client.close();
    }

    @Test
    public void testWithoutApplicationInstancesThenOne(TestInfo info) {
        String serviceName = info.getDisplayName() + "-my-service";
        String secondService = info.getDisplayName() + "-my-second-service";
        registerApplicationInstance(client, secondService, "id1", "acme.com", 1234, null, -1, "UP", "");

        Stork stork = configureAndGetStork(serviceName);
        Service service = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 0);
        List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).isEmpty();

        registerApplicationInstance(client, serviceName, "id0", "com.example", 1111, null, -1, "STARTING", "");

        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 0);
        instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).isEmpty();

        updateApplicationInstanceStatus(client, serviceName, "id0", "UP", "");

        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 1);
        instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(1).allSatisfy(instance -> {
            assertThat(instance.getHost()).isEqualTo("com.example");
            assertThat(instance.getPort()).isEqualTo(1111);
        });
    }

    @Test
    public void testWithTwoUpApplicationInstances(TestInfo info) {
        String serviceName = info.getDisplayName() + "-my-service";
        String secondService = info.getDisplayName() + "-my-second-service";
        registerApplicationInstance(client, serviceName, "id1", "acme.com", 1234, null, -1, "UP", "");
        registerApplicationInstance(client, serviceName, "id2", "acme2.com", 1235, null, -1, "UP", "");
        registerApplicationInstance(client, secondService, "second", "acme.com", 1236, null, -1, "UP", "");

        Stork stork = configureAndGetStork(serviceName);
        Service service = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await()
                .atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> {
                    List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
                    Assertions.assertEquals(2, instances.size(),
                            () -> "Unable to get the expected number of instances while expecting 2 - " + instances
                                    + " (" + instances.stream().map(ServiceInstance::getHost).collect(Collectors.toList())
                                    + ") " + client.get("/eureka/apps").sendAndAwait().bodyAsJsonObject());
                });
        List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(2)
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme.com");
                    assertThat(instance.getPort()).isEqualTo(1234);
                })
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme2.com");
                    assertThat(instance.getPort()).isEqualTo(1235);
                });
    }

    @Test
    public void testWithTwoUpAndSecuredApplicationInstances(TestInfo info) {
        String serviceName = info.getDisplayName() + "-my-service";
        String secondService = info.getDisplayName() + "-my-second-service";
        registerApplicationInstance(client, serviceName, "id1", "acme.com", 1234, "secure.acme.com", 433, "UP", "");
        registerApplicationInstance(client, serviceName, "id2", "acme2.com", 1235, null, 8433, "UP", "");
        registerApplicationInstance(client, secondService, "second", "acme.com", 1236, null, -1, "UP", "");

        Stork stork = configureAndGetStork(serviceName, true, null);
        Service service = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 2);
        List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(2)
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("secure.acme.com");
                    assertThat(instance.getPort()).isEqualTo(433);
                    assertThat(instance.isSecure()).isTrue();
                })
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme2.com");
                    assertThat(instance.getPort()).isEqualTo(8433);
                    assertThat(instance.isSecure()).isTrue();
                });
    }

    @Test
    public void testWithTwoUpApplicationInstancesButOnlyOneUp(TestInfo info) {
        String serviceName = info.getDisplayName() + "-my-service";
        String secondService = info.getDisplayName() + "-my-second-service";
        registerApplicationInstance(client, serviceName, "id1", "acme.com", 1234, null, -1, "DOWN", "");
        registerApplicationInstance(client, serviceName, "id2", "acme2.com", 1235, null, -1, "UP", "");
        registerApplicationInstance(client, secondService, "second", "acme.com", 1236, null, -1, "UP", "");

        Stork stork = configureAndGetStork(serviceName);
        Service service = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 1);
        List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(1)
                .allSatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme2.com");
                    assertThat(instance.getPort()).isEqualTo(1235);
                });

        updateApplicationInstanceStatus(client, serviceName, "id1", "UP", "");
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 2);
        instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(2)
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme.com");
                    assertThat(instance.getPort()).isEqualTo(1234);
                })
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme2.com");
                    assertThat(instance.getPort()).isEqualTo(1235);
                });
    }

    @Test
    public void testWithTwoOOSUpApplicationInstancesThenUp(TestInfo info) {
        String serviceName = info.getDisplayName() + "-my-service";
        String secondService = info.getDisplayName() + "-my-second-service";
        registerApplicationInstance(client, serviceName, "id1", "acme.com", 1234, null, -1, "DOWN", "");
        registerApplicationInstance(client, serviceName, "id2", "acme2.com", 1235, null, -1, "DOWN", "");
        registerApplicationInstance(client, secondService, "id1", "acme.com", 1236, null, -1, "UP", "");

        Stork stork = configureAndGetStork(serviceName);
        Service service = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 0);
        List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).isEmpty();

        updateApplicationInstanceStatus(client, serviceName, "id1", "STARTING", "");
        updateApplicationInstanceStatus(client, serviceName, "id2", "STARTING", "");

        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 0);
        instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).isEmpty();

        updateApplicationInstanceStatus(client, serviceName, "id1", "UP", "");
        await().until(() -> {
            List<ServiceInstance> serviceInstances = service.getInstances().await().atMost(Duration.ofSeconds(5));
            return serviceInstances.size() == 1;
        });

        instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(1)
                .allSatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme.com");
                    assertThat(instance.getPort()).isEqualTo(1234);
                });

        updateApplicationInstanceStatus(client, serviceName, "id2", "UP", "");
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 2);
        instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(2)
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme.com");
                    assertThat(instance.getPort()).isEqualTo(1234);
                })
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme2.com");
                    assertThat(instance.getPort()).isEqualTo(1235);
                });
    }

    @Test
    public void testWithOneUpApplicationInstanceThenDown(TestInfo info) {
        String serviceName = info.getDisplayName() + "-my-service";
        String secondService = info.getDisplayName() + "-my-second-service";
        registerApplicationInstance(client, serviceName, "id1", "acme.com", 1234, null, -1, "UP", "");
        registerApplicationInstance(client, secondService, "second", "acme.com", 1236, null, -1, "UP", "");

        Stork stork = configureAndGetStork(serviceName);
        Service service = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 1);
        List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(1)
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme.com");
                    assertThat(instance.getPort()).isEqualTo(1234);
                });

        updateApplicationInstanceStatus(client, serviceName, "id1", "DOWN", "");

        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 0);
        instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).isEmpty();
    }

    @Test
    public void testWithTwoUpApplicationInstancesButOnlyOneSecure(TestInfo info) {
        String serviceName = info.getDisplayName() + "-my-service";
        String secondService = info.getDisplayName() + "-my-second-service";
        registerApplicationInstance(client, serviceName, "id1", "acme.com", 1234, null, -1, "UP", "");
        registerApplicationInstance(client, serviceName, "id2", "acme2.com", 1235, "ssl.acme.com", 433, "UP", "");
        registerApplicationInstance(client, secondService, "second", "acme.com", 1236, null, -1, "UP", "");

        Stork stork = configureAndGetStork(serviceName, true, null);
        Service service = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 1);
        List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(1)
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("ssl.acme.com");
                    assertThat(instance.getPort()).isEqualTo(433);
                });
    }

    @Test
    public void testInstanceSelection(TestInfo info) {
        String serviceName = info.getDisplayName() + "-my-service";
        String secondService = info.getDisplayName() + "-my-second-service";
        registerApplicationInstance(client, serviceName, "id1", "acme.com", 1234, null, -1, "UP", "");
        registerApplicationInstance(client, serviceName, "id2", "acme2.com", 1235, "ssl.acme.com", 433, "UP", "");
        registerApplicationInstance(client, secondService, "second", "acme.com", 1236, null, -1, "UP", "");

        Stork stork = configureAndGetStork(serviceName, false, "id2");
        Service service = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 1);
        List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(1)
                .anySatisfy(instance -> {
                    assertThat(instance.getHost()).isEqualTo("acme2.com");
                    assertThat(instance.getPort()).isEqualTo(1235);
                });

        stork = configureAndGetStork(serviceName, false, "missing");
        Service missing = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await().until(() -> missing.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 0);
    }

    @Disabled(value = "Enable if you want to test a contextualized server, it will require adding a server.servlet.context-path=/myserviceregistry property to application.properties file")
    @Test
    public void testContextualizedServer(TestInfo info) {
        String serviceName = info.getDisplayName() + "-my-service";
        String secondService = info.getDisplayName() + "-my-second-service";
        registerApplicationInstance(client, secondService, "id1", "acme.com", 1234, null, -1, "UP", "/myserviceregistry");

        Stork stork = configureAndGetStork(serviceName);
        Service service = stork.getService(serviceName);
        Assertions.assertNotNull(service);
        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 0);
        List<ServiceInstance> instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).isEmpty();

        registerApplicationInstance(client, serviceName, "id0", "com.example", 1111, null, -1, "STARTING",
                "/myserviceregistry");

        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 0);
        instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).isEmpty();

        updateApplicationInstanceStatus(client, serviceName, "id0", "UP", "/myserviceregistry");

        await().until(() -> service.getInstances().await().atMost(Duration.ofSeconds(5)).size() == 1);
        instances = service.getInstances().await().atMost(Duration.ofSeconds(5));
        assertThat(instances).hasSize(1).allSatisfy(instance -> {
            assertThat(instance.getHost()).isEqualTo("com.example");
            assertThat(instance.getPort()).isEqualTo(1111);
        });
    }

    protected Stork configureAndGetStork(String serviceName) {
        return configureAndGetStork(serviceName, false, null);
    }

    private Stork configureAndGetStork(String serviceName, boolean secure, String instance) {
        Stork stork = StorkTestUtils.getNewStorkInstance();
        EurekaConfiguration configuration = new EurekaConfiguration()
                .withEurekaHost(eureka.getHost())
                .withEurekaPort(Integer.toString(port))
                .withRefreshPeriod("1S")
                .withSecure(Boolean.toString(secure));
        if (instance != null) {
            configuration = configuration.withInstance(instance);
        }
        stork.defineIfAbsent(serviceName, ServiceDefinition.of(configuration));
        return stork;
    }

}
