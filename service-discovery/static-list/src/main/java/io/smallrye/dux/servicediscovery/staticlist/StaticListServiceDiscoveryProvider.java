package io.smallrye.dux.servicediscovery.staticlist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.dux.config.ServiceDiscoveryConfig;
import io.smallrye.dux.spi.ServiceDiscoveryProvider;
import io.smallrye.dux.spi.ServiceInstanceIds;

public class StaticListServiceDiscoveryProvider implements ServiceDiscoveryProvider {

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config) {
        // we're configuring service discovery for
        // config prefix dux.<service-name>.discovery
        // URLs for static config should be listed as:
        // dux.<service-name>.discovery.1=...
        // dux.<service-name>.discovery.2=...
        // dux.<service-name>.discovery.3=...
        Map<String, String> parameters = config.parameters();
        Pattern number = Pattern.compile("\\d+");
        List<ServiceInstance> addressList = new ArrayList<>();

        parameters.keySet().stream()
                .filter(k -> number.matcher(k).matches())
                .sorted()
                .forEach(k -> addressList.add(new ServiceInstance(ServiceInstanceIds.next(), parameters.get(k))));

        return new StaticListServiceDiscovery(addressList);
    }

    @Override
    public String type() {
        return "static";
    }
}
