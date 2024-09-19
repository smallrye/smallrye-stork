# Eureka Service Discovery

[Eureka](https://github.com/Netflix/eureka) is a RESTful service that is primarily used in the AWS cloud for the purpose of discovery, load balancing, and failover of middle-tier servers.

This page explains how Stork can use Eureka to handle the service registration.

## Dependency

First, you need to add the Stork Eureka Service Registration provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-registration-eureka</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Service registration configuration

For each service that should register the service instances in Eureka, configure the service registrar `type`:

=== "stork standalone"
```properties
stork.my-service.service-registrar.type=eureka
stork.my-service.service-registrar.eureka-host=localhost
stork.my-service.service-registrar.eureka-port=8761
```

=== "stork in quarkus"
```properties
quarkus.stork.my-service.service-registrar.type=eureka
quarkus.stork.my-service.service-registrar.eureka-host=localhost
quarkus.stork.my-service.service-registrar.eureka-port=8761
```

Eureka service registrar is configured with the following parameters:

--8<-- "target/attributes/META-INF/stork-docs/eureka-sr-attributes.txt"
