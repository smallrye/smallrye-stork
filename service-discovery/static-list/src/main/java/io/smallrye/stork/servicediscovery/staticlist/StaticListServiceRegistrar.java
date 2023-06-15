package io.smallrye.stork.servicediscovery.staticlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.spi.StorkInfrastructure;
import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.StorkAddressUtils;

public class StaticListServiceRegistrar implements ServiceRegistrar<Metadata.DefaultMetadataKey> {
    private static final Logger log = LoggerFactory.getLogger(StaticListServiceRegistrar.class);
    private final StaticRegistrarConfiguration config;

    public StaticListServiceRegistrar(StaticRegistrarConfiguration config, String serviceName,
            StorkInfrastructure infrastructure) {
        this.config = config;
    }

    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata<Metadata.DefaultMetadataKey> metadata,
            String ipAddress,
            int port) {
        HostAndPort hostAndPortToAdd = StorkAddressUtils.parseToHostAndPort(ipAddress, port,
                "service '" + serviceName + "'");
        String hostAndPortToAddString = StorkAddressUtils.parseToString(hostAndPortToAdd);
        StaticAddressesBackend.add(serviceName, hostAndPortToAddString);
        return Uni.createFrom().voidItem();
    }

    public static final class StaticAddressesBackend {

        private static Map<String, List<String>> backend;

        public static List<String> getAddresses(String serviceName) {
            if (backend == null) {
                backend = new HashMap<>();
                backend.put(serviceName, Collections.emptyList());
            }
            return backend.get(serviceName);
        }

        public static void add(String serviceName, String address) {
            if (backend == null) {
                backend = new HashMap<>();
                backend.put(serviceName, new ArrayList<>());
            } else if (backend.get(serviceName) == null) {
                List<String> adresses = new ArrayList<>();
                adresses.add(address);
                backend.put(serviceName, adresses);
            } else if (!backend.get(serviceName).contains(address)) {
                backend.get(serviceName).add(address);

            }
        }

        public static void clear(String serviceName) {
            if (backend != null) {
                backend.remove(serviceName);
            }
        }
    }

}
