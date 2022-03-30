package io.smallrye.stork.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to define arrays of {@link ServiceDiscoveryAttribute}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ServiceDiscoveryAttributes {
    /**
     * @return the array of {@link ServiceDiscoveryAttribute}, must not contain {@code null}.
     */
    ServiceDiscoveryAttribute[] value();
}
