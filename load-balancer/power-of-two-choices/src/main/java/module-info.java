/**
 * SmallRye Stork Power of two choices load balancer module
 */
module io.smallrye.stork.loadbalancer.poweroftwochoices {

    requires java.compiler;

    requires io.smallrye.stork.api;
    requires io.smallrye.mutiny;
    requires io.smallrye.common.annotation;
    requires io.smallrye.stork.config.generator;
    requires io.smallrye.stork.core;
    requires io.smallrye.stork.loadbalancer.requests;
    requires jakarta.cdi;
    requires jakarta.cdi.lang.model;
    requires jakarta.annotation;
    requires jakarta.el;
    requires jakarta.interceptor;
    requires jakarta.inject;
    requires org.jctools.core;
    requires org.jboss.logging;

    exports io.smallrye.stork.loadbalancer.poweroftwochoices;

    provides io.smallrye.stork.spi.internal.LoadBalancerLoader with io.smallrye.stork.loadbalancer.poweroftwochoices.PowerOfTwoChoicesLoadBalancerProviderLoader;

}