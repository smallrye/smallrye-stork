package io.smallrye.loadbalancer.core;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import static io.smallrye.loadbalancer.core.Constants.LOAD_BALANCER_PREFIX;


// TODO: probably should be moved to some implementation module, not needed for "clients"
public final class ConfigUtils {

    public static Config getConfig(String name) {
        Config rootConfig = ConfigProvider.getConfig(Thread.currentThread().getContextClassLoader());

        return new ConfigView(rootConfig, name, LOAD_BALANCER_PREFIX);
    }

    private ConfigUtils() {
    }
}
