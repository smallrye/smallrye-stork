package io.smallrye.stork.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.FakeServiceConfig;
import io.smallrye.stork.Stork;
import io.smallrye.stork.WeldTestBase;
import io.smallrye.stork.api.NoServiceInstanceFoundException;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.config.ConfigWithType;
import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.config.ConfigProvider;

class RoundRobinLoadBalancerProviderCDITest extends WeldTestBase {

    public static final ConfigWithType LB_CONFIG = new ConfigWithType() {
        @Override
        public String type() {
            return RoundRobinLoadBalancerProvider.ROUND_ROBIN_TYPE;
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    private static final ConfigWithType FAKE_SERVICE_DISCOVERY_CONFIG = new ConfigWithType() {

        @Override
        public String type() {
            return "fake";
        }

        @Override
        public Map<String, String> parameters() {
            return Collections.emptyMap();
        }
    };

    public static final List<ServiceConfig> configurations = new ArrayList<>();

    private Stork stork;

    @BeforeEach
    public void init() {
        configurations.add(new FakeServiceConfig("a", FAKE_SERVICE_DISCOVERY_CONFIG, LB_CONFIG));
        weld.addBeanClass(MyLbConfigProvider.class);
        weld.addBeanClass(RoundRobinLoadBalancerProvider.class);
        run();

        Stork.initialize();
        stork = Stork.getInstance();
    }

    @AfterEach
    public void cleanup() {
        Stork.shutdown();
        close();
        configurations.clear();
    }

    @Test
    public void testWithoutServiceInstances() {
        assertThrows(NoServiceInstanceFoundException.class,
                () -> stork.getService("a").selectInstance(Collections.emptyList()));

    }

    @Test
    public void testWithASingleServiceInstance() {
        ServiceInstance instance = mock(ServiceInstance.class);
        Service service = stork.getService("a");

        // 3 selection in a row -> always the same instance
        ServiceInstance selected = service.selectInstance(List.of(instance));
        assertThat(selected).isEqualTo(instance);
        selected = service.selectInstance(List.of(instance));
        assertThat(selected).isEqualTo(instance);
        selected = service.selectInstance(List.of(instance));
        assertThat(selected).isEqualTo(instance);
    }

    @Test
    public void testWithMultiServiceInstances() {
        Service service = stork.getService("a");

        ServiceInstance instance1 = mock(ServiceInstance.class);
        ServiceInstance instance2 = mock(ServiceInstance.class);
        ServiceInstance instance3 = mock(ServiceInstance.class);
        when(instance1.getId()).thenReturn(1L);
        when(instance2.getId()).thenReturn(2L);
        when(instance3.getId()).thenReturn(3L);
        List<ServiceInstance> list = List.of(instance1, instance2);
        ServiceInstance selected = service.selectInstance(list);
        assertThat(selected).isEqualTo(instance1);
        selected = service.selectInstance(list);
        assertThat(selected).isEqualTo(instance2);
        selected = service.selectInstance(list);
        assertThat(selected).isEqualTo(instance1);
        selected = service.selectInstance(list);
        assertThat(selected).isEqualTo(instance2);
        selected = service.selectInstance(Collections.singletonList(instance2));
        assertThat(selected).isEqualTo(instance2);
        selected = service.selectInstance(Collections.singletonList(instance1));
        assertThat(selected).isEqualTo(instance1);
        selected = service.selectInstance(List.of(instance1, instance2, instance3));
        assertThat(selected).isEqualTo(instance1);
        selected = service.selectInstance(List.of(instance1, instance2, instance3));
        assertThat(selected).isEqualTo(instance2);
        selected = service.selectInstance(List.of(instance1, instance2, instance3));
        assertThat(selected).isEqualTo(instance3);
        selected = service.selectInstance(List.of(instance1, instance2, instance3));
        assertThat(selected).isEqualTo(instance1);
    }

    @Test
    public void testType() {

        assertTrue(stork.getService("a").getLoadBalancer() instanceof RoundRobinLoadBalancer);
    }

    @ApplicationScoped
    @Typed(ConfigProvider.class)
    public static class MyLbConfigProvider implements ConfigProvider {

        @Override
        public List<ServiceConfig> getConfigs() {
            return new ArrayList<>(configurations);
        }

        @Override
        public int priority() {
            return 5;
        }
    }
}
