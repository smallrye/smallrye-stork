package io.smallrye.stork.loadbalancer.sticky;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.LoadBalancer;
import io.smallrye.stork.api.NoAcceptableServiceInstanceFoundException;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.test.StorkTestUtils;
import io.smallrye.stork.test.TestConfigProviderBean;

@ExtendWith(WeldJunit5Extension.class)
class StickyLoadBalancerCDITest {
    public static final long _70_s = Duration.ofSeconds(70).toNanos();
    public static final long _80_s = Duration.ofSeconds(80).toNanos();

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(TestConfigProviderBean.class,
            StickyLoadBalancerProviderLoader.class);

    @Inject
    TestConfigProviderBean config;

    @Inject
    StickyLoadBalancerProviderLoader loader;

    private Stork stork;

    @BeforeEach
    void setUp() {
        config.clear();
        config.addServiceConfig("without-instances", "sticky",
                "empty-services", Collections.emptyMap(), Collections.emptyMap());
        config.addServiceConfig("failover-backoff", "sticky",
                "empty-services", Map.of(StickyLoadBalancerProvider.FAILURE_BACKOFF_TIME, "60s"), Collections.emptyMap());

        stork = StorkTestUtils.getNewStorkInstance();
    }

    @Test
    void shouldFailIfNoInstances() {
        Service serviceWithoutInstances = stork.getService("without-instances");
        Service serviceWithFailover = stork.getService("failover-backoff");

        assertThrows(NoServiceInstanceFoundException.class,
                () -> serviceWithoutInstances.selectInstance(Collections.emptyList()));
        assertThrows(NoServiceInstanceFoundException.class,
                () -> serviceWithFailover.selectInstance(Collections.emptyList()));
    }

    @Test
    void shouldReturnConsistentlyIfOnlyOneServiceInstance() {

        ServiceInstance instance = mockServiceInstance();
        Service serviceWithoutInstances = stork.getService("without-instances");
        assertSelectedFrom(serviceWithoutInstances, List.of(instance), instance);
        ServiceInstance selected;
        selected = serviceWithoutInstances.selectInstance(List.of(instance));
        assertThat(selected.getId()).isEqualTo(instance.getId());

        selected.recordEnd(new Exception("induced failure"));
        selected = serviceWithoutInstances.selectInstance(List.of(instance));
        assertThat(selected.getId()).isEqualTo(instance.getId());
    }

    @Test
    void shouldFailOnSingleFailingInstanceIfBackoffTimeSet() {
        ServiceInstance instance = mockServiceInstance();
        Service serviceWithoutInstances = stork.getService("failover-backoff");
        var selected = assertSelectedFrom(serviceWithoutInstances, List.of(instance), instance);
        assertThat(selected.getId()).isEqualTo(instance.getId());

        selected.recordEnd(new Exception("induced failure"));
        assertThatThrownBy(() -> serviceWithoutInstances.selectInstance(List.of(instance)))
                .isInstanceOf(NoAcceptableServiceInstanceFoundException.class);
        assertThatThrownBy(() -> serviceWithoutInstances.selectInstance(List.of(instance)))
                .isInstanceOf(NoAcceptableServiceInstanceFoundException.class);
    }

    @Test
    void shouldContinueOnSingleInstanceIfBackoffTimePassed() {
        Service service = stork.getService("failover-backoff");
        ServiceInstance instance = mockServiceInstance();
        var selected = assertSelectedFrom(service, List.of(instance), instance);

        LoadBalancer lb = service.getLoadBalancer();

        ((StickyLoadBalancer) lb).recordEndAtTime(selected.getId(), new Exception("induced failure"),
                System.nanoTime() - _70_s);
        assertSelectedFrom(service, List.of(instance), instance);
    }

    @Test
    void shouldSwitchToNextInstanceOnFailure() {
        Service service = stork.getService("without-instances");
        ServiceInstance instance1 = mockServiceInstance();
        ServiceInstance instance2 = mockServiceInstance();
        ServiceInstance instance3 = mockServiceInstance();

        List<ServiceInstance> list = List.of(instance1, instance2);
        assertSelectedFrom(service, list, instance1);
        assertSelectedFrom(service, list, instance1);
        assertSelectedFrom(service, list, instance1);
        var lastSelected = assertSelectedFrom(service, list, instance1);

        lastSelected.recordEnd(new Exception("failure"));
        assertSelectedFrom(service, list, instance2);
        assertSelectedFrom(service, list, instance2);
        assertSelectedFrom(service, list, instance2);

        // check if adding an instance to the list doesn't change the result value:
        list = List.of(instance1, instance2, instance3);
        assertSelectedFrom(service, list, instance2);
        assertSelectedFrom(service, list, instance2);
        lastSelected = assertSelectedFrom(service, list, instance2);

        lastSelected.recordEnd(new Exception("exception thrown by service 2"));
        list = List.of(instance1, instance2, instance3);
        assertSelectedFrom(service, list, instance3);
        assertSelectedFrom(service, list, instance3);
        lastSelected = assertSelectedFrom(service, list, instance3);

        // when 3 is marked as failed, 1 should be used once again as the one that failed the furthest in time
        lastSelected.recordEnd(new Exception("exception thrown by service 3"));
        assertSelectedFrom(service, list, instance1);
        assertSelectedFrom(service, list, instance1);
    }

    @Test
    void shouldFailOnMultipleInstancesIfAllFailedWithinBackoffTime() {
        Service service = stork.getService("failover-backoff");
        ServiceInstance instance1 = mockServiceInstance();
        ServiceInstance instance2 = mockServiceInstance();
        ServiceInstance instance3 = mockServiceInstance();

        List<ServiceInstance> list = List.of(instance1, instance2);
        assertSelectedFrom(service, list, instance1);
        assertSelectedFrom(service, list, instance1);
        assertSelectedFrom(service, list, instance1);
        var lastSelected = assertSelectedFrom(service, list, instance1);

        lastSelected.recordEnd(new Exception("failure"));
        assertSelectedFrom(service, list, instance2);
        assertSelectedFrom(service, list, instance2);
        assertSelectedFrom(service, list, instance2);

        // check if adding an instance to the list doesn't change the result value:
        list = List.of(instance1, instance2, instance3);
        assertSelectedFrom(service, list, instance2);
        assertSelectedFrom(service, list, instance2);
        lastSelected = assertSelectedFrom(service, list, instance2);

        lastSelected.recordEnd(new Exception("exception thrown by service 2"));
        assertSelectedFrom(service, list, instance3);
        assertSelectedFrom(service, list, instance3);
        lastSelected = assertSelectedFrom(service, list, instance3);

        // when 3 is marked as failed, 1 should be used once again as the one that failed the furthest in time
        lastSelected.recordEnd(new Exception("exception thrown by service 3"));

        assertThatThrownBy(() -> service.selectInstance(List.of(instance1, instance2, instance3)))
                .isInstanceOf(NoAcceptableServiceInstanceFoundException.class);
    }

    /**
     * 1 fails now - 80s, 2 fails at now - 70s, 3 fails now
     *
     * we select next instance from (2,3), i.e. 1 is not available currently. To avoid memory leaks,
     * 1 should be removed from the collection of available service instances at this point (because it's an older failure
     * then the new selection made)
     */
    @Test
    @SuppressWarnings("deprecation")
    void shouldForgetAboutFailureIfHappenedBeforeNewlySelectedOne() {
        long now = System.nanoTime();
        Service service = stork.getService("without-instances");
        ServiceInstance instance1 = mockServiceInstance();
        ServiceInstance instance2 = mockServiceInstance();
        ServiceInstance instance3 = mockServiceInstance();

        List<ServiceInstance> list = List.of(instance1, instance2, instance3);
        var selected = assertSelectedFrom(service, list, instance1);
        LoadBalancer lb = service.getLoadBalancer();
        ((StickyLoadBalancer) lb).recordEndAtTime(selected.getId(), new Exception("failure!"), now - _80_s);

        selected = assertSelectedFrom(lb, list, instance2);
        ((StickyLoadBalancer) lb).recordEndAtTime(selected.getId(), new Exception("failure!"), now - _70_s);

        selected = assertSelectedFrom(lb, list, instance3);
        ((StickyLoadBalancer) lb).recordEndAtTime(selected.getId(), new Exception("failure!"), now);

        assertSelectedFrom(lb, List.of(instance2, instance3), instance2);
        assertThat(((StickyLoadBalancer) lb).getFailedInstances()).hasSize(1); // only instance3 here
        assertThat(((StickyLoadBalancer) lb).getFailedInstances()).containsKey(instance3.getId());
    }

    @Test
    void shouldRejectFailedInstanceWithinBackoffTime() {
        Service service = stork.getService("failover-backoff");
        ServiceInstance instance1 = mockServiceInstance();
        ServiceInstance instance2 = mockServiceInstance();
        var selected = assertSelectedFrom(service, List.of(instance1), instance1);
        selected.recordEnd(new Exception("induced failure"));
        assertThatThrownBy(() -> service.selectInstance(List.of(instance1)))
                .isInstanceOf(NoAcceptableServiceInstanceFoundException.class);

        assertSelectedFrom(service, List.of(instance2), instance2);
        assertThatThrownBy(() -> service.selectInstance(List.of(instance1)))
                .isInstanceOf(NoAcceptableServiceInstanceFoundException.class);
    }

    private ServiceInstance assertSelectedFrom(LoadBalancer lb, List<ServiceInstance> list, ServiceInstance instance1) {
        ServiceInstance selected = lb.selectServiceInstance(list);
        assertThat(selected.getId()).isEqualTo(instance1.getId());
        return selected;
    }

    private ServiceInstance assertSelectedFrom(Service storkService, List<ServiceInstance> list, ServiceInstance instance1) {
        ServiceInstance selected = storkService.selectInstance(list);
        assertThat(selected.getId()).isEqualTo(instance1.getId());
        return selected;
    }

    private static final AtomicLong serviceInstanceIds = new AtomicLong(1);

    private ServiceInstance mockServiceInstance() {
        ServiceInstance result = mock(ServiceInstance.class);
        long nextId = serviceInstanceIds.getAndIncrement();
        when(result.getId()).thenReturn(nextId);
        return result;
    }

    @Test
    public void testType() {
        assertThat(loader.type()).isEqualTo(StickyLoadBalancerProvider.TYPE);
    }

}
