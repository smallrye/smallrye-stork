package io.smallrye.stork.servicediscovery.staticlist;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.InMemoryAddressesBackend;
import io.smallrye.stork.utils.ServiceInstanceIds;
import io.smallrye.stork.utils.StorkAddressUtils;

/**
 * An implementation of service discovery returning a static list of service instances.
 */
public final class StaticListServiceDiscovery implements ServiceDiscovery {

    private final List<ServiceInstance> instances;
    private final String serviceName;

    /**
     * Creates a new instance of StaticListServiceDiscovery.
     *
     * @param instances the list of instance
     */
    public StaticListServiceDiscovery(String serviceName, List<DefaultServiceInstance> instances) {
        this.serviceName = serviceName;
        this.instances = new ArrayList<>(instances);

    }

    @Override
    public Uni<List<ServiceInstance>> getServiceInstances() {
        List<String> addresses = InMemoryAddressesBackend.getAddresses(serviceName);
        if (addresses != null && !addresses.isEmpty()) {
            for (String address : addresses) {
                try {
                    HostAndPort hostAndPort = StorkAddressUtils.parseToHostAndPort(address, 80, "service");
                    DefaultServiceInstance serviceInstance = new DefaultServiceInstance(ServiceInstanceIds.next(),
                            hostAndPort.host,
                            hostAndPort.port,
                            hostAndPort.path, false);
                    if (!instances.contains(serviceInstance)) {
                        instances
                                .add(serviceInstance);
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Address not parseable to URL: " + address + " for service " + serviceName);
                }
            }
        }

        return Uni.createFrom().item(instances);
    }
}
