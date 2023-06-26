# Consul Service Discovery

[Consul](https://www.consul.io/) is a distributed, highly available, and data center aware solution to connect and configure applications across dynamic, distributed infrastructure.
It's often used as service discovery backend to register and locate the services composing your system.
Consul makes it simple for services to register themselves and to discover other services via a DNS or HTTP interface. 
External services can be registered as well.

This page explains how Stork can use Consul to handle the service discovery and service registration.

## Dependency

First, you need to add the Stork Consul Service Discovery provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-consul</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

### Service discovery configuration

For each service that should get the service instances from Consul, configure the service discovery `type`:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=consul
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=consul
    ```

Consul service discovery is configured with the following parameters:

--8<-- "../service-discovery/consul/target/classes/META-INF/stork-docs/consul-sd-attributes.txt"

## Service registration

Stork also provides the ability to register services using Consul as backend.

### Service registration configuration

For each service that should register the service instances in Consul, configure the service registrar `type`:

=== "stork standalone"
```properties
stork.my-service.service-registrar.type=consul
```

=== "stork in quarkus"
```properties
quarkus.stork.my-service.service-registrar.type=consul
```

Consul service registrar is configured with the following parameters:

--8<-- "../service-discovery/consul/target/classes/META-INF/stork-docs/consul-sr-attributes.txt"