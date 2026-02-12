package io.smallrye.stork.microprofile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.config.ConfigProvider;
import io.smallrye.stork.spi.config.SimpleServiceConfig;
import io.smallrye.stork.utils.StorkConfigUtils;

/**
 * Implementation of {@link ConfigProvider} using MicroProfile Config.
 */
public class MicroProfileConfigProvider implements ConfigProvider {

    private static final Logger log = Logger.getLogger(MicroProfileConfigProvider.class);

    private final List<ServiceConfig> serviceConfigs = new ArrayList<>();

    /**
     * Creates a new instance of MicroProfileConfigProvider.
     */
    public MicroProfileConfigProvider() {
        Config config = org.eclipse.microprofile.config.ConfigProvider.getConfig();

        Map<String, Map<String, String>> propertiesByServiceName = new HashMap<>();

        for (String propertyName : config.getPropertyNames()) {
            StorkConfigUtils.computeServiceProperty(propertiesByServiceName, propertyName,
                    config.getValue(propertyName, String.class));
        }

        for (Map.Entry<String, Map<String, String>> serviceEntry : propertiesByServiceName.entrySet()) {
            SimpleServiceConfig serviceConfig = StorkConfigUtils.buildServiceConfig(serviceEntry);
            serviceConfigs.add(serviceConfig);
        }
    }

    @Override
    public List<ServiceConfig> getConfigs() {
        return serviceConfigs;
    }

    @Override
    public int priority() {
        return 100;
    }

}
