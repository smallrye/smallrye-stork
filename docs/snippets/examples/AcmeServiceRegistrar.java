package examples;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;

public class AcmeServiceRegistrar implements ServiceRegistrar {

    private final String backendHost;
    private final int backendPort;

    public AcmeServiceRegistrar(AcmeRegistrarConfiguration configuration) {
        this.backendHost = configuration.getHost();
        this.backendPort = Integer.parseInt(configuration.getPort());
    }


    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata metadata, String ipAddress, int defaultPort) {
        //do whatever is needed for registering service instance
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        //do whatever is needed for deregistering service instance
        return Uni.createFrom().voidItem();
    }
}
