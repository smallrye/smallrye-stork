package io.smallrye.stork;

import static io.smallrye.stork.TestEnv.SPI_ROOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;

public class ProgrammaticApiTest {

    @BeforeEach
    public void init() {
        SPI_ROOT.mkdirs();
        AnchoredServiceDiscoveryProvider.services.clear();
        TestEnv.configurations.clear();
    }

    @AfterEach
    public void cleanup() throws IOException {
        Stork.shutdown();
        TestEnv.clearSPIs();
        AnchoredServiceDiscoveryProvider.services.clear();
        TestEnv.configurations.clear();
    }

    @Test
    void testWithoutLoadBalancing() {
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);
        Stork.initialize();

        Stork.getInstance().defineIfAbsent("my-service",
                ServiceDefinition.of(new FakeConfiguration().withSecure("true")));

        Service service = Stork.getInstance().getService("my-service");
        assertThat(service).isNotNull();
        assertThat(service.getInstances().await().indefinitely()).hasSize(2);
        ServiceInstance i1 = service.selectInstance().await().indefinitely();
        ServiceInstance i2 = service.selectInstance().await().indefinitely();
        assertThat(i1.isSecure()).isTrue();
        assertThat(i2.isSecure()).isTrue();
        assertThat(i1).isNotSameAs(i2);

        assertThat(Stork.getInstance().getServices()).hasSize(1);
    }

    @Test
    void testWithLoadBalancing() {
        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance1);
        AnchoredServiceDiscoveryProvider.services.add(instance2);
        Stork.initialize();

        Stork.getInstance().defineIfAbsent("my-service",
                ServiceDefinition.of(new FakeConfiguration().withSecure("true"), new SingleConfiguration()));

        Service service = Stork.getInstance().getService("my-service");
        assertThat(service).isNotNull();
        assertThat(service.getInstances().await().indefinitely()).hasSize(2);
        ServiceInstance i1 = service.selectInstance().await().indefinitely();
        ServiceInstance i2 = service.selectInstance().await().indefinitely();
        assertThat(i1.isSecure()).isTrue();
        assertThat(i2.isSecure()).isTrue();
        assertThat(i1).isSameAs(i2);

        assertThat(Stork.getInstance().getServices()).hasSize(1);
    }

    @Test
    void testInvalidConfig() {
        Stork.initialize();
        assertThatThrownBy(() -> Stork.getInstance().defineIfAbsent(null,
                ServiceDefinition.of(new FakeConfiguration().withSecure("true"), new SingleConfiguration())))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Stork.getInstance().defineIfAbsent("acme",
                ServiceDefinition.of(null, new SingleConfiguration()))).isInstanceOf(IllegalArgumentException.class);

    }

}
