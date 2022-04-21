package io.smallrye.stork.servicediscovery.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ServiceRegistrarAttribute;
import io.smallrye.stork.api.config.ServiceRegistrarType;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.StorkInfrastructure;

@ServiceRegistrarAttribute(name = "k8s-host", description = "The Kubernetes API host.")
@ServiceRegistrarType(value = "kubernetes", metadataKey = KubernetesMetadataKey.class)
public class KubernetesServiceRegistrarProvider
        implements ServiceRegistrarProvider<KubernetesRegistrarConfiguration, KubernetesMetadataKey> {

    private static final Logger log = LoggerFactory.getLogger(KubernetesServiceRegistrarProvider.class);

    @Override
    public ServiceRegistrar<KubernetesMetadataKey> createServiceRegistrar(KubernetesRegistrarConfiguration config,
            String serviceRegistrarName, StorkInfrastructure infrastructure) {
        return new KubernetesServiceRegistrar(config, serviceRegistrarName, infrastructure);
    }

}
