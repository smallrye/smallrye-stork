package io.smallrye.stork.api.config;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The type of the load balancer. Use this annotation on your {@link io.smallrye.stork.spi.LoadBalancerProvider}
 *
 * The type is used to determine load balancer for services.
 *
 * Use {@code stork.<my-service>.load-balancer.type=my-load-balancer} to use a
 * {@link io.smallrye.stork.spi.LoadBalancerProvider} annotated with {@code @LoadBalancerType("my-load-balancer")}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
public @interface LoadBalancerType {
    /**
     *
     * @return the type of the load balance
     */
    String value();
}
