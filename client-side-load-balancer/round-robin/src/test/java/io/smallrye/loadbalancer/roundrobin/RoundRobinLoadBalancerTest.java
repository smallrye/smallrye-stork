package io.smallrye.loadbalancer.roundrobin;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.smallrye.discovery.ServiceDiscovery;
import io.smallrye.discovery.ServiceInstance;
import io.smallrye.loadbalancer.LoadBalancer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoundRobinLoadBalancerTest {

    private final ServiceDiscovery serviceDiscovery = new TestServiceDiscovery(
            Arrays.asList(new ServiceInstance(1, "service-1-url"),
                    new ServiceInstance(2, "service-2-url")));

    @Test
    public void shouldGetServiceInstance() {
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer(serviceDiscovery);
        ServiceInstance oneInstance = loadBalancer.getServiceInstance().toCompletableFuture().join();
        ServiceInstance anotherInstance = loadBalancer.getServiceInstance().toCompletableFuture().join();

        assertThat(oneInstance.getId()).isEqualTo(1);
        assertThat(anotherInstance.getId()).isEqualTo(2);
    }

    @Test
    public void shouldGetBlockingServiceInstance() {
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer(serviceDiscovery);
        ServiceInstance oneInstance = loadBalancer.getServiceInstanceBlocking().get();
        ServiceInstance anotherInstance = loadBalancer.getServiceInstanceBlocking().get();

        assertThat(oneInstance.getId()).isEqualTo(1);
        assertThat(anotherInstance.getId()).isEqualTo(2);
    }

    private static class TestServiceDiscovery implements ServiceDiscovery {
        private final List<ServiceInstance> instances;

        TestServiceDiscovery(List<ServiceInstance> instances) {
            this.instances = instances;
        }

        @Override
        public CompletionStage<List<ServiceInstance>> getServiceInstances() {
            return CompletableFuture.completedStage(instances);
        }

        @Override
        public List<ServiceInstance> getServiceInstancesBlocking() {
            return instances;
        }
    }
}
