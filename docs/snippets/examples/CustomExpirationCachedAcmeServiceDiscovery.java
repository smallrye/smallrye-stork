package examples;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.CachingServiceDiscovery;
import io.smallrye.stork.impl.DefaultServiceInstance;
import io.smallrye.stork.utils.ServiceInstanceIds;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomExpirationCachedAcmeServiceDiscovery extends CachingServiceDiscovery {

    private final String host;
    private final int port;

    private AtomicBoolean invalidated = new AtomicBoolean();

    public CustomExpirationCachedAcmeServiceDiscovery(CachedAcmeConfiguration configuration) {
        super(configuration.getRefreshPeriod());
        this.host = configuration.getHost();
        this.port = Integer.parseInt(configuration.getPort());
    }

    @Override
    public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
        // Retrieve services...
        DefaultServiceInstance instance =
                new DefaultServiceInstance(ServiceInstanceIds.next(), host, port, false);
        return Uni.createFrom().item(() -> Collections.singletonList(instance));
    }

    @Override
    public Uni<List<ServiceInstance>> cache(Uni<List<ServiceInstance>> uni) {
        return uni.memoize().until(() -> invalidated.get());
    }

    //command-based cache invalidation: user triggers the action to invalidate the cache.
    public void invalidate() {
        invalidated.set(true);
    }

}