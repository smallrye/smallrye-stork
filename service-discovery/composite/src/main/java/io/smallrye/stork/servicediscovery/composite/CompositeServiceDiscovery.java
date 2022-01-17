package io.smallrye.stork.servicediscovery.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniJoin;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.servicediscovery.composite.util.CombiningList;

public class CompositeServiceDiscovery implements ServiceDiscovery {

    private final Collection<String> serviceNames;
    private final List<ServiceDiscovery> elements = new ArrayList<>();
    private final String serviceName;

    public CompositeServiceDiscovery(String serviceName, Collection<String> serviceNames) {
        this.serviceNames = Collections.unmodifiableCollection(serviceNames);
        this.serviceName = serviceName;
    }

    @Override
    public Uni<List<ServiceInstance>> getServiceInstances() {
        UniJoin.Builder<List<ServiceInstance>> builder = Uni.join().builder();
        for (ServiceDiscovery element : elements) {
            builder.add(element.getServiceInstances());
        }

        return builder.joinAll().andFailFast().onItem().transform(CombiningList::new);
    }

    @Override
    public void initialize(Map<String, Service> services) {
        for (String service : serviceNames) {
            Service serviceElement = services.get(service);
            if (serviceElement == null) {
                throw new IllegalArgumentException("Service '" + service + "' used in the composite service discovery '"
                        + serviceName + "' not found");
            }
            elements.add(serviceElement.getServiceDiscovery());
        }
    }
}
