/**
 * SmallRye Stork Least requests load balancer module
 */
module io.smallrye.stork.loadbalancer.requests {

    requires java.compiler;

    requires io.smallrye.stork.api;
    requires io.smallrye.mutiny;
    requires io.smallrye.common.annotation;
    requires io.smallrye.stork.config.generator;
    requires io.smallrye.stork.core;
    requires jakarta.cdi;
    requires jakarta.cdi.lang.model;
    requires jakarta.annotation;
    requires jakarta.el;
    requires jakarta.interceptor;
    requires jakarta.inject;
    requires org.jctools.core;
    requires org.jboss.logging;

    exports io.smallrye.stork.loadbalancer.requests;

    provides io.smallrye.stork.spi.internal.LoadBalancerLoader with io.smallrye.stork.loadbalancer.requests.LeastRequestsLoadBalancerProviderLoader;

}