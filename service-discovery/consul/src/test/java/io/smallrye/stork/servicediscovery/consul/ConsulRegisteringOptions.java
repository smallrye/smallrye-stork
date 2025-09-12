package io.smallrye.stork.servicediscovery.consul;

import java.util.List;

public record ConsulRegisteringOptions(String serviceName, int port, List<String> tags, List<String> addresses) {
}
