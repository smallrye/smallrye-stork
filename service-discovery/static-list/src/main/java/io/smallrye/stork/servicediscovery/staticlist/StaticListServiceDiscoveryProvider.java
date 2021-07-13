package io.smallrye.stork.servicediscovery.staticlist;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.config.ServiceDiscoveryConfig;
import io.smallrye.stork.spi.ServiceDiscoveryProvider;
import io.smallrye.stork.spi.ServiceInstanceIds;

public class StaticListServiceDiscoveryProvider implements ServiceDiscoveryProvider {

    @Override
    public ServiceDiscovery createServiceDiscovery(ServiceDiscoveryConfig config, String serviceName) {
        // we're configuring service discovery for
        // config prefix stork.<service-name>.discovery
        // URLs for static config should be listed as:
        // stork.<service-name>.discovery.1=...
        // stork.<service-name>.discovery.2=...
        // stork.<service-name>.discovery.3=...
        Map<String, String> parameters = config.parameters();
        Pattern number = Pattern.compile("\\d+");
        List<ServiceInstance> addressList = new ArrayList<>();

        parameters.keySet().stream()
                .filter(k -> number.matcher(k).matches())
                .sorted(Comparator.comparing(Integer::valueOf))
                .forEach(k -> {//
                    URL url = null;
                    try {
                        url = new URL(parameters.get(k));
                        String host = url.getHost();
                        int port = url.getPort();
                        addressList.add(new ServiceInstance(ServiceInstanceIds.next(), host, port));
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException(
                                "Address not parseable to URL: " + url + " for service " + serviceName);
                    }
                });
        return new StaticListServiceDiscovery(addressList);
    }

    @Override
    public String type() {
        return "static";
    }
}
