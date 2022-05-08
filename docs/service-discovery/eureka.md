# Eureka Service Discovery

[Eureka](https://github.com/Netflix/eureka) is a RESTful service that is primarily used in the AWS cloud for the purpose of discovery, load balancing, and failover of middle-tier servers.

This page explains how Stork can use Eureka to handle the service discovery.

## Dependency

First, you need to add the Stork Consul Service Discovery provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-eureka</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each application instance expected to be registered in Eureka, configure the lookup:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=eureka
    stork.my-service.service-discovery.eureka-host=localhost
    stork.my-service.service-discovery.eureka-port=8761
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=eureka
    quarkus.stork.my-service.service-discovery.eureka-host=localhost
    quarkus.stork.my-service.service-discovery.eureka-port=8761
    ```


Stork looks for the service with the given name (`my-service` in the previous example).

Supported attributes are the following:

--8<-- "service-discovery/eureka/target/classes/META-INF/stork-docs/eureka-sd-attributes.txt"

The `application` attribute is optional.
It uses the Stork service name (`my-service` in the previous configuration) if not set.

The `instance` attribute allows selecting a specific instance.
Using this attribute prevents load-balancing as you will always select a single instance.

The `secure` attribute indicates if you want the _secure virtual address_ of the application instance.
If set to `true`, unsecured instances are filtered out from the available instances.
