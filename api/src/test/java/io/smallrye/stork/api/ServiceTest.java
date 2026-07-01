package io.smallrye.stork.api;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.observability.NoopObservationCollector;

class ServiceTest {

    private static Service newServiceWithoutRegistrar() {
        return new Service("my-service", "round-robin", "test-discovery", new NoopObservationCollector(),
                serviceInstances -> {
                    throw new AssertionError("Load balancer should not be used in this test");
                },
                () -> Uni.createFrom().item(List.of()), null, false);
    }

    @Test
    void registerInstanceShouldFailWhenRegistrarIsMissing() {
        Service service = newServiceWithoutRegistrar();

        UnsupportedOperationException exception = Assertions.assertThrows(UnsupportedOperationException.class,
                () -> service.registerInstance("127.0.0.1", 8080).await().indefinitely());

        Assertions.assertEquals("This service has no service registrar configured.", exception.getMessage());
    }

    @Test
    void registerNamedInstanceShouldFailWhenRegistrarIsMissing() {
        Service service = newServiceWithoutRegistrar();

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> service.registerNamedInstance("instance-1", "127.0.0.1", 8080).await().indefinitely());
    }

    @Test
    void deregisterNamedInstanceShouldFailWhenRegistrarIsMissing() {
        Service service = newServiceWithoutRegistrar();

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> service.deregisterNamedInstance("instance-1").await().indefinitely());
    }

    @Test
    void deregisterByAddressShouldFailWhenRegistrarIsMissing() {
        Service service = newServiceWithoutRegistrar();

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> service.deregisterServiceInstance("127.0.0.1", 8080).await().indefinitely());
    }

    @Test
    void registerWithOptionsShouldFailWhenRegistrarIsMissing() {
        Service service = newServiceWithoutRegistrar();

        ServiceRegistrar.RegistrarOptions options = new ServiceRegistrar.RegistrarOptions(
                "my-service", "127.0.0.1", 8080, List.of("tag-1"), null);

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> service.registerInstance(options).await().indefinitely());
    }

    @Test
    void registerWithTagsShouldFailWhenRegistrarIsMissing() {
        Service service = newServiceWithoutRegistrar();

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> service.registerInstance(List.of("v1.0"), "127.0.0.1", 8080).await().indefinitely());
    }

    @Test
    void registerNamedInstanceWithTagsShouldFailWhenRegistrarIsMissing() {
        Service service = newServiceWithoutRegistrar();

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> service.registerNamedInstance("instance-1", List.of("v1.0"), "127.0.0.1", 8080).await()
                        .indefinitely());
    }

    @Test
    void deregisterAllShouldFailWhenRegistrarIsMissing() {
        Service service = newServiceWithoutRegistrar();

        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> service.deregisterServiceInstance().await().indefinitely());
    }
}
