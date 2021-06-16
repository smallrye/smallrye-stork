package io.smallrye.discovery.roundrobin;

import static io.smallrye.discovery.config.ConfigUtils.keySegment;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

import io.smallrye.discovery.ServiceDiscovery;
import io.smallrye.discovery.config.ConfigAccessor;

public class RoundRobinLoadBalancerInitializer {

    private static final String LOAD_BALANCER_TYPE = "round-robin";

    private static final String CONFIG_PREFIX = "load-balancer";

    private static final String TYPE_KEY_SEGMENT = "type";

    private static final int SERVICE_NAME_SEGMENT_POSITION = 1;

    private final ConfigAccessor configAccessor;

    public RoundRobinLoadBalancerInitializer() {
        this.configAccessor = new ConfigAccessor();
    }

    public RoundRobinLoadBalancerInitializer(Config config) {
        this.configAccessor = new ConfigAccessor(config);
    }

    public void init(ServiceDiscovery serviceDiscovery) {
        for (String serviceName : getServiceNames()) {
            serviceDiscovery.registerLoadBalancer(new RoundRobinLoadBalancer(serviceName));
        }
    }

    private Set<String> getServiceNames() {
        Set<String> serviceNames = new HashSet<>();

        for (String key : configAccessor.getKeys(CONFIG_PREFIX)) {
            String serviceName = keySegment(key, SERVICE_NAME_SEGMENT_POSITION);
            if (isRoundRobinLoadBalancer(serviceName)) {
                serviceNames.add(serviceName);
            }
        }

        return serviceNames;
    }

    private boolean isRoundRobinLoadBalancer(String serviceName) {
        return LOAD_BALANCER_TYPE.equals(
                configAccessor.getValue(String.format("%s.%s.%s", CONFIG_PREFIX, serviceName, TYPE_KEY_SEGMENT)));
    }
}
