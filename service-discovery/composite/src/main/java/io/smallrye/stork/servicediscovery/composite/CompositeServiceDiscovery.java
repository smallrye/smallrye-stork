package io.smallrye.stork.servicediscovery.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniJoin;
import io.smallrye.stork.api.NoSuchServiceDefinitionException;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.api.StorkServiceRegistry;
import io.smallrye.stork.servicediscovery.composite.util.CombiningList;

/**
 * A implementation of service discovery delegating to other services.
 */
public class CompositeServiceDiscovery implements ServiceDiscovery {

    private final Collection<String> serviceNames;
    private final List<ServiceDiscovery> elements = new ArrayList<>();
    private final String serviceName;
    private StorkServiceRegistry stork;
    private volatile boolean initialized;

    /**
     * Creates a new CompositeServiceDiscovery
     *
     * @param serviceName the service name
     * @param serviceNames the composed service names
     */
    public CompositeServiceDiscovery(String serviceName, Collection<String> serviceNames) {
        this.serviceNames = Collections.unmodifiableCollection(serviceNames);
        this.serviceName = serviceName;
    }

    @Override
    public Uni<List<ServiceInstance>> getServiceInstances() {
        if (!initialized) {
            init();
        }
        UniJoin.Builder<List<ServiceInstance>> builder = Uni.join().builder();
        for (ServiceDiscovery element : elements) {
            builder = builder.add(element.getServiceInstances());
        }

        return builder.joinAll().andFailFast().onItem().transform(CombiningList::new);
    }

    private void init() {
        List<ServiceDiscovery> list = new ArrayList<>();
        for (String service : serviceNames) {
            Service serviceElement = stork.getServiceOptional(service)
                    .orElseThrow(() -> new NoSuchServiceDefinitionException(
                            service + " (used in composite service discovery " + serviceName + ")"));
            list.add(serviceElement.getServiceDiscovery());
        }
        elements.addAll(list);
        initialized = true;
    }

    @Override
    public void initialize(StorkServiceRegistry stork) {
        this.stork = stork;

    }
}
