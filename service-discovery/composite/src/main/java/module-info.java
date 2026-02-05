/**
 * SmallRye Stork Composite service discovery module
 */
module io.smallrye.stork.servicediscovery.composite {

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
    requires org.slf4j;

    exports io.smallrye.stork.servicediscovery.composite;

    provides io.smallrye.stork.spi.internal.ServiceDiscoveryLoader with io.smallrye.stork.servicediscovery.composite.CompositeServiceDiscoveryProviderLoader;

}