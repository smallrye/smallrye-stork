package io.smallrye.loadbalancer;

import org.eclipse.microprofile.config.Config;

// mstodo introduce `type` so that the user doesn't have to specify
// mstodo fully qualified class names for target address provuder, load balancer producer, etc
public interface TargetAddressProviderProducer {

    TargetAddressProvider getTargetAddressProvider(Config loadBalancerConfig, String name);

}
