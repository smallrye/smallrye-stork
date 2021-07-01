package io.smallrye.stork.microprofile;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

public class TestMicroProfileConfigProvider extends ConfigProviderResolver {
    private final Config config;

    public TestMicroProfileConfigProvider(Config config) {
        this.config = config;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        return null;
    }

    @Override
    public ConfigBuilder getBuilder() {
        throw new IllegalStateException("method not supported");
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        throw new IllegalStateException("method not supported");
    }

    @Override
    public void releaseConfig(Config config) {
        throw new IllegalStateException("method not supported");
    }
}
