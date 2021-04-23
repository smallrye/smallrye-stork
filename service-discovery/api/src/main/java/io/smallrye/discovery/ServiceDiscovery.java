package io.smallrye.discovery;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface ServiceDiscovery {
    CompletionStage<List<ServiceInstance>> getServiceInstances(); // TODO we probably need to take a name as a parameter

    List<ServiceInstance> getServiceInstancesBlocking(); // TODO we probably need to take a name as a parameter
}
