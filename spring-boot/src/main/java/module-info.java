/**
 * SmallRye Stork Spring Boot module
 */
module io.smallrye.stork.springboot {

    requires java.compiler;

    requires io.smallrye.stork.api;
    requires io.smallrye.mutiny;
    requires io.smallrye.stork.core;
    requires jakarta.cdi;
    requires jakarta.cdi.lang.model;
    requires jakarta.annotation;
    requires jakarta.el;
    requires jakarta.interceptor;
    requires jakarta.inject;
    requires org.jctools.core;
    requires org.slf4j;

    requires spring.boot;
    requires spring.core;
    requires org.apache.commons.logging;
    requires org.jspecify;
    requires spring.context;
    requires spring.aop;
    requires spring.beans;
    requires spring.expression;
    requires micrometer.observation;
    requires micrometer.commons;
    requires spring.boot.autoconfigure;
    requires org.jboss.logging;
    requires io.smallrye.common.annotation;

    provides io.smallrye.stork.spi.config.ConfigProvider with io.smallrye.stork.springboot.SpringBootConfigProvider;

}