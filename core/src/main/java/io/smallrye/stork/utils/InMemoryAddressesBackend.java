package io.smallrye.stork.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryAddressesBackend {

    private static Map<String, List<String>> backend = new HashMap<>();

    public static List<String> getAddresses(String serviceName) {
        return backend.get(serviceName);
    }

    public static void add(String serviceName, String address) {
        if (serviceName == null || serviceName.length() == 0) {
            throw new IllegalArgumentException("No service name provided for address " + address);
        }
        if (backend.get(serviceName) != null) {
            if (!backend.get(serviceName).contains(address)) {
                backend.get(serviceName).add(address);
            }
        } else {
            List<String> addresses = new ArrayList<>();
            addresses.add(address);
            backend.put(serviceName, addresses);
        }
    }

    public static void clear(String serviceName) {
        if (backend != null) {
            backend.remove(serviceName);
        }
    }

    public static void clearAll() {
        backend.clear();
    }
}
