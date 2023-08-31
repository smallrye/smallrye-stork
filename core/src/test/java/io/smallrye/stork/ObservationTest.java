package io.smallrye.stork;

import static io.smallrye.stork.FakeServiceConfig.FAKE_SERVICE_DISCOVERY_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.observability.ObservationPoints;
import io.smallrye.stork.integration.ObservableStorkInfrastructure;
import io.smallrye.stork.spi.config.ConfigProvider;

public class ObservationTest {

    //TODO Test with response time monitoring (both success and failure)

    @BeforeEach
    public void setUp() throws IOException {
        Stork.shutdown();
        AnchoredServiceDiscoveryProvider.services.clear();
        TestEnv.clearSPIs();
        TestEnv.configurations.clear();
    }

    @AfterEach
    public void cleanup() throws IOException {
        Stork.shutdown();
    }

    @Test
    void testEveryThingIsOk() {
        TestEnv.configurations.add(new FakeServiceConfig("my-service",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));

        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork stork = getNewObservableStork();

        Service service = stork.getService("my-service");
        assertThat(service.selectInstance().await().indefinitely()).isEqualTo(instance);
        assertThat(service.getObservations()).isNotNull();

        ObservationPoints.StorkResolutionEvent metrics = FakeObservationCollector.FAKE_STORK_EVENT;
        assertThat(metrics.getServiceName()).isEqualTo("my-service");
        assertThat(metrics.isDone()).isTrue();
        assertThat(metrics.failure()).isNull();
        assertThat(metrics.getOverallDuration()).isNotNull();
        assertThat(metrics.getDiscoveredInstancesCount()).isEqualTo(1);
        assertThat(metrics.getServiceDiscoveryType()).isEqualTo("fake");
        assertThat(metrics.getServiceSelectionType()).isEqualTo("round-robin");
        assertThat(metrics.getServiceDiscoveryDuration()).isNotNull();
        assertThat(metrics.getServiceSelectionDuration()).isNotNull();

    }

    @Test
    void testMetricsWhenServiceDiscoveryFailure() {
        FakeServiceConfig e = new FakeServiceConfig("my-service",
                new MockConfiguration(), null, null);
        TestEnv.configurations.add(e);

        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork stork = getNewObservableStork();

        Service service = stork.getService("my-service");

        when(service.getServiceDiscovery().getServiceInstances())
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Service Discovery induced failure")));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            service.selectInstance().await().indefinitely();
        });

        assertThat(exception.getMessage()).isEqualTo("Service Discovery induced failure");
        assertThat(service.getObservations()).isNotNull();

        ObservationPoints.StorkResolutionEvent metrics = FakeObservationCollector.FAKE_STORK_EVENT;
        assertThat(metrics.getServiceName()).isEqualTo("my-service");
        assertThat(metrics.isDone()).isTrue();
        assertThat(metrics.failure()).isEqualTo(exception);
        assertThat(metrics.getDiscoveredInstancesCount()).isEqualTo(-1);
        assertThat(metrics.getServiceDiscoveryType()).isEqualTo("mock");
        assertThat(metrics.getServiceSelectionType()).isEqualTo("round-robin");
        assertThat(metrics.getServiceDiscoveryDuration()).isNotNull();
        assertThat(metrics.getServiceSelectionDuration()).isNotNull();

    }

    @Test
    void testMetricsWhenLoadBalancerFailure() {
        TestEnv.configurations.add(new FakeServiceConfig("my-service",
                FAKE_SERVICE_DISCOVERY_CONFIG, new FakeSelectorConfiguration(), null));

        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork stork = getNewObservableStork();

        Service service = stork.getService("my-service");
        LoadBalancer loadBalancer = service.getLoadBalancer();

        when(loadBalancer.selectServiceInstance(any(Collection.class)))
                .thenThrow(new RuntimeException("Load Balancer induced failure"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            service.selectInstance().await().indefinitely();
        });

        assertThat(exception.getMessage()).isEqualTo("Load Balancer induced failure");
        assertThat(service.getObservations()).isNotNull();

        ObservationPoints.StorkResolutionEvent metrics = FakeObservationCollector.FAKE_STORK_EVENT;
        assertThat(metrics.getServiceName()).isEqualTo("my-service");
        assertThat(metrics.isDone()).isTrue();
        assertThat(metrics.failure()).isEqualTo(exception);
        assertThat(metrics.getDiscoveredInstancesCount()).isEqualTo(1);
        assertThat(metrics.getServiceDiscoveryType()).isEqualTo("fake");
        assertThat(metrics.getServiceSelectionType()).isEqualTo("fake-selector");
        assertThat(metrics.getServiceDiscoveryDuration()).isNotNull();
        assertThat(metrics.getServiceSelectionDuration()).isNotNull();

    }

    @Test
    void testWhenNoServicesDiscovered() {
        TestEnv.configurations.add(new FakeServiceConfig("my-service",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));

        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork stork = getNewObservableStork();

        Service service = stork.getService("my-service");
        Exception exception = assertThrows(NoServiceInstanceFoundException.class, () -> {
            service.selectInstance().await().indefinitely();
        });

        assertThat(service.getObservations()).isNotNull();

        ObservationPoints.StorkResolutionEvent metrics = FakeObservationCollector.FAKE_STORK_EVENT;
        assertThat(metrics.getServiceName()).isEqualTo("my-service");
        assertThat(metrics.isDone()).isTrue();
        assertThat(metrics.failure()).isNotNull();
        assertThat(metrics.failure()).isEqualTo(exception);
        assertThat(metrics.getDiscoveredInstancesCount()).isEqualTo(0);
        assertThat(metrics.getServiceDiscoveryType()).isEqualTo("fake");
        assertThat(metrics.getServiceSelectionType()).isEqualTo("round-robin");
        assertThat(metrics.getServiceDiscoveryDuration()).isNotNull();
        assertThat(metrics.getServiceSelectionDuration()).isNotNull();

    }

    private static Stork getNewObservableStork() {
        return new Stork(new ObservableStorkInfrastructure(new FakeObservationCollector()));
    }

}
