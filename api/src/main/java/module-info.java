/**
 * SmallRye Stork API Module
 */
module io.smallrye.stork.api {

    requires java.compiler;

    requires io.smallrye.mutiny;
    requires io.smallrye.common.annotation;
    requires org.jctools.core;

    exports io.smallrye.stork.api;
    exports io.smallrye.stork.api.config;
    exports io.smallrye.stork.api.observability;
    exports io.smallrye.stork.spi;
    exports io.smallrye.stork.spi.config;
    exports io.smallrye.stork.spi.internal;

}