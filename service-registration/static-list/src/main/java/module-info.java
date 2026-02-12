/**
 * SmallRye Stork Static list service registration module
 */
module io.smallrye.stork.serviceregistration.staticlist {

    requires java.compiler;

    requires io.smallrye.stork.api;
    requires io.smallrye.mutiny;
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

    exports io.smallrye.stork.serviceregistration.staticlist;

    provides io.smallrye.stork.spi.internal.ServiceRegistrarLoader with io.smallrye.stork.serviceregistration.staticlist.StaticListServiceRegistrarProviderLoader;

}