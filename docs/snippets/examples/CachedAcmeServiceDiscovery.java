package examples;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceDiscovery;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.utils.ServiceInstanceIds;

import java.util.Collections;
import java.util.List;

public class CachedAcmeServiceDiscovery extends CachingServiceDiscovery {

    private final String host;
    private final int port;

    public CachedAcmeServiceDiscovery(CachedAcmeConfiguration configuration) {
        super(configuration.getRefreshPeriod()); // (1)
        this.host = configuration.getHost();
        this.port = Integer.parseInt(configuration.getPort());
    }

    @Override  // (2)
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        // Retrieve services...
        DefaultServiceInstance instance =
                new DefaultServiceInstance(ServiceInstanceIds.next(), host, port, false);
        return Uni.createFrom().item(() -> Collections.singletonList(instance));
    }
}
