/**
 * SmallRye Stork Consul service registration module
 */
module io.smallrye.stork.serviceregistration.consul {

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
    requires io.vertx.web.client;
    requires io.vertx.uritemplate;
    requires io.vertx.web.common;
    requires io.vertx.auth.common;
    requires io.vertx.core;
    requires io.vertx.consul.client;
    requires io.netty.buffer;
    requires io.netty.transport;
    requires io.netty.handler;
    requires io.netty.transport.unix.common;
    requires io.netty.codec;
    requires io.netty.handler.proxy;
    requires io.netty.codec.socks;
    requires io.netty.codec.http;
    requires io.netty.codec.http2;
    requires io.netty.resolver;
    requires io.netty.resolver.dns;
    requires io.netty.codec.dns;
    requires com.fasterxml.jackson.core;

    exports io.smallrye.stork.serviceregistration.consul;

    provides io.smallrye.stork.spi.internal.ServiceRegistrarLoader with io.smallrye.stork.serviceregistration.consul.ConsulServiceRegistrarProviderLoader;
}