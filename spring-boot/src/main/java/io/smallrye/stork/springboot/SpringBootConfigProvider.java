package io.smallrye.stork.springboot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import io.smallrye.stork.api.config.ServiceConfig;
import io.smallrye.stork.spi.config.ConfigProvider;
import io.smallrye.stork.spi.config.SimpleServiceConfig;
import io.smallrye.stork.utils.StorkConfigUtils;

public class SpringBootConfigProvider implements ConfigProvider {

    private final List<ServiceConfig> serviceConfigs = new ArrayList<>();

    public SpringBootConfigProvider() {
        ApplicationContext context = SpringBootApplicationContextProvider.getApplicationContext();
        ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getEnvironment();

        Map<String, Map<String, String>> propertiesByServiceName = new HashMap<>();

        for (String propertyName : getPropertyNames(environment)) {

            StorkConfigUtils.computeServiceProperty(propertiesByServiceName, propertyName,
                    environment.getProperty(propertyName));

        }

        for (Map.Entry<String, Map<String, String>> serviceEntry : propertiesByServiceName.entrySet()) {
            SimpleServiceConfig serviceConfig = StorkConfigUtils.buildServiceConfig(serviceEntry);
            serviceConfigs.add(serviceConfig);
        }
    }

    private List<String> getPropertyNames(ConfigurableEnvironment environment) {
        List<String> propertyNames = new ArrayList<>();
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource) {
                String[] names = ((EnumerablePropertySource<?>) propertySource)
                        .getPropertyNames();
                if (names != null) {
                    for (String name : names) {
                        propertyNames.add(name);
                    }
                }
            }
        }
        return propertyNames;
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
