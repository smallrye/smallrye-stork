package io.smallrye.discovery;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface ServiceDiscovery {
    CompletionStage<List<ServiceInstance>> getServiceInstances(String name);

    List<ServiceInstance> getServiceInstancesBlocking(String name);
}
