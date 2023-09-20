package io.smallrye.stork;

import static io.smallrye.stork.FakeServiceConfig.FAKE_SERVICE_DISCOVERY_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
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
    void shouldGetMetricsWhenSelectingInstance() {
        //Given a configuration service using a SD and default LB
        TestEnv.configurations.add(new FakeServiceConfig("my-service",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));

        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork stork = getNewObservableStork();

        Service service = stork.getService("my-service");

        //When we try to get service instances
        assertThat(service.selectInstance().await().indefinitely()).isEqualTo(instance);

        //One instance is found and metrics are also gathered accordingly
        assertThat(service.getObservations()).isNotNull();
        ObservationPoints.StorkResolutionEvent metrics = FakeObservationCollector.FAKE_STORK_EVENT;
        assertThat(metrics.getServiceName()).isEqualTo("my-service");
        assertThat(metrics.isDone()).isTrue();
        assertThat(metrics.failure()).isNull();
        assertThat(metrics.getDiscoveredInstancesCount()).isEqualTo(1);
        assertThat(metrics.getServiceDiscoveryType()).isEqualTo("fake");
        assertThat(metrics.getServiceSelectionType()).isEqualTo("round-robin");

        assertDurations(metrics);

    }

    private static void assertDurations(ObservationPoints.StorkResolutionEvent metrics) {
        Duration overallDuration = metrics.getOverallDuration();
        Duration serviceDiscoveryDuration = metrics.getServiceDiscoveryDuration();
        Duration serviceSelectionDuration = metrics.getServiceSelectionDuration();
        assertThat(overallDuration).isNotNull();
        assertThat(serviceDiscoveryDuration).isNotNull();
        assertThat(serviceSelectionDuration).isNotNull();
        assertThat(overallDuration).isGreaterThanOrEqualTo(serviceDiscoveryDuration.plus(serviceSelectionDuration));
    }

    @Test
    void shouldGetMetricsAfterSelectingInstanceWhenServiceDiscoveryFails() {
        //Given a configuration service using a failing SD and default LB
        FakeServiceConfig e = new FakeServiceConfig("my-service",
                new MockConfiguration(), null, null);
        TestEnv.configurations.add(e);

        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork stork = getNewObservableStork();

        //When we try to get service instances
        Service service = stork.getService("my-service");

        when(service.getServiceDiscovery().getServiceInstances())
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Service Discovery induced failure")));

        //An error is thrown and metrics are also gathered accordingly
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
        assertDurations(metrics);

    }

    @Test
    void shouldGetMetricsWhenSelectingInstanceFails() {
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
        assertDurations(metrics);

    }

    @Test
    void shouldGetMetricsAfterSelectingInstanceWhenWhenNoServicesDiscovered() {
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
        assertDurations(metrics);
    }

    // From here, same tests but using the selectInstanceAndRecordStart method

    @Test
    void shouldGetMetricsWhenSelectingInstanceWithRecordAndStart() {
        TestEnv.configurations.add(new FakeServiceConfig("my-service",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));

        ServiceInstance instance = mock(ServiceInstance.class);
        AnchoredServiceDiscoveryProvider.services.add(instance);
        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork stork = getNewObservableStork();

        Service service = stork.getService("my-service");
        assertThat(service.selectInstanceAndRecordStart(true).await().indefinitely()).isEqualTo(instance);
        assertThat(service.getObservations()).isNotNull();

        ObservationPoints.StorkResolutionEvent metrics = FakeObservationCollector.FAKE_STORK_EVENT;
        assertThat(metrics.getServiceName()).isEqualTo("my-service");
        assertThat(metrics.isDone()).isTrue();
        assertThat(metrics.failure()).isNull();
        assertThat(metrics.getOverallDuration()).isNotNull();
        assertThat(metrics.getDiscoveredInstancesCount()).isEqualTo(1);
        assertThat(metrics.getServiceDiscoveryType()).isEqualTo("fake");
        assertThat(metrics.getServiceSelectionType()).isEqualTo("round-robin");
        assertDurations(metrics);

    }

    @Test
    void shouldGetMetricsAfterSelectingInstanceWithMonitoringWhenServiceDiscoveryFails() {
        FakeServiceConfig e = new FakeServiceConfig("my-service",
                new MockConfiguration(), null, null);
        TestEnv.configurations.add(e);

        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork stork = getNewObservableStork();

        Service service = stork.getService("my-service");

        when(service.getServiceDiscovery().getServiceInstances())
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Service Discovery induced failure")));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            service.selectInstanceAndRecordStart(true).await().indefinitely();
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
        assertDurations(metrics);

    }

    @Test
    void shouldGetMetricsWhenSelectingInstanceWithMonitoringFails() {
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
            service.selectInstanceAndRecordStart(true).await().indefinitely();
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
        assertDurations(metrics);

    }

    @Test
    void shouldGetMetricsAfterSelectingInstanceWithMonitoringWhenWhenNoServicesDiscovered() {
        TestEnv.configurations.add(new FakeServiceConfig("my-service",
                FAKE_SERVICE_DISCOVERY_CONFIG, null, null));

        TestEnv.install(ConfigProvider.class, TestEnv.AnchoredConfigProvider.class);
        Stork stork = getNewObservableStork();

        Service service = stork.getService("my-service");
        Exception exception = assertThrows(NoServiceInstanceFoundException.class, () -> {
            service.selectInstanceAndRecordStart(true).await().indefinitely();
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
        assertDurations(metrics);

    }

    private static Stork getNewObservableStork() {
        return new Stork(new ObservableStorkInfrastructure(new FakeObservationCollector()));
    }

}
