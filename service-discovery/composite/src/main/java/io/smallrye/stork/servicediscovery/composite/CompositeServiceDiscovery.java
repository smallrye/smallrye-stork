package io.smallrye.stork.servicediscovery.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniJoin;
import io.smallrye.stork.ServiceDiscovery;
import io.smallrye.stork.ServiceInstance;
import io.smallrye.stork.Stork;
import io.smallrye.stork.servicediscovery.composite.util.CombiningList;

public class CompositeServiceDiscovery implements ServiceDiscovery {

    private final Collection<String> serviceNames;
    private final List<ServiceDiscovery> elements = new ArrayList<>();

    public CompositeServiceDiscovery(Collection<String> serviceNames) {
        this.serviceNames = Collections.unmodifiableCollection(serviceNames);
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
    public void initialize(Stork stork) {
        for (String service : serviceNames) {
            elements.add(stork.getService(service).getServiceDiscovery());
        }
    }
}
