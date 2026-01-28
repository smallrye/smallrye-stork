
/**
 * SmallRye Stork Configuration Generator Module
 * Generates configuration classes for Stork service discovery and load balancing
 */
module io.smallrye.stork.test {

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
    requires org.slf4j.simple;
    requires org.assertj.core;
    requires net.bytebuddy;

    exports io.smallrye.stork.test;

    provides io.smallrye.stork.spi.config.ConfigProvider with io.smallrye.stork.test.TestConfigProvider;

}
