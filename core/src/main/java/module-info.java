/**
 * SmallRye Stork Configuration Generator Module
 * Generates configuration classes for Stork service discovery and load balancing
 */
module io.smallrye.stork.core {

    requires java.compiler;

    requires io.smallrye.stork.api;
    requires io.smallrye.mutiny;
    requires io.smallrye.common.annotation;
    requires io.smallrye.stork.config.generator;
    requires jakarta.cdi;
    requires jakarta.cdi.lang.model;
    requires jakarta.annotation;
    requires jakarta.el;
    requires jakarta.interceptor;
    requires jakarta.inject;
    requires org.jctools.core;
    requires org.jboss.logging;

    exports io.smallrye.stork;
    exports io.smallrye.stork.impl;
    exports io.smallrye.stork.utils;
    exports io.smallrye.stork.integration;

    uses io.smallrye.stork.spi.config.ConfigProvider;
    uses io.smallrye.stork.spi.internal.ServiceDiscoveryLoader;
    uses io.smallrye.stork.spi.internal.LoadBalancerLoader;
    uses io.smallrye.stork.spi.internal.ServiceRegistrarLoader;

}