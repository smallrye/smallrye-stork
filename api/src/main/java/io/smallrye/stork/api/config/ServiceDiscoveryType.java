package io.smallrye.stork.api.config;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The type of the service discovery. Use this annotation on your {@link io.smallrye.stork.spi.ServiceDiscoveryProvider}
 *
 * The type is used to determine service discovery for services.
 *
 * Use {@code stork.<my-service>.service-discovery.type=my-service-discovery} to use a
 * {@link io.smallrye.stork.spi.ServiceDiscoveryProvider} annotated with {@code @ServiceDiscoveryType("my-service-discovery")}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
public @interface ServiceDiscoveryType {
    /**
     *
     * @return the type of the service discovery
     */
    String value();
}
