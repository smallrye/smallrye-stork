/**
 * SmallRye Stork Configuration Generator Module
 * Generates configuration classes for Stork service discovery and load balancing
 */
module io.smallrye.stork.config.generator {

    requires java.compiler;

    requires io.smallrye.stork.api;
    requires io.smallrye.mutiny;
    requires io.smallrye.common.annotation;
    requires org.jctools.core;
    requires jakarta.cdi;
    requires jakarta.cdi.lang.model;
    requires jakarta.annotation;
    requires jakarta.el;
    requires jakarta.interceptor;
    requires jakarta.inject;

    exports io.smallrye.stork.config.generator;

    provides javax.annotation.processing.Processor with io.smallrye.stork.config.generator.ConfigurationGenerator;

}