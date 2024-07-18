# Consul Service Registration

[Consul](https://www.consul.io/) is a distributed, highly available, and data center aware solution to connect and configure applications across dynamic, distributed infrastructure.
It's often used as service discovery backend to register and locate the services composing your system.
Consul makes it simple for services to register themselves and to discover other services via a DNS or HTTP interface. 
External services can be registered as well.

This page explains how Stork can use Consul to handle the service registration.

## Dependency

First, you need to add the Stork Consul Service Registration provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-registration-consul</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Service registration configuration

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

--8<-- "../service-registration/consul/target/classes/META-INF/stork-docs/consul-sr-attributes.txt"