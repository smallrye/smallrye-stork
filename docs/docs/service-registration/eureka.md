# Eureka Service Registration

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

The `ipAddress` passed to `registerServiceInstance()` is used as both the IP address (`ipAddr`) and the hostname (`hostName`) registered in Eureka.
If the instance has a distinct DNS hostname, set `host-name` explicitly to override this default.

## Secure port registration

By default, only the non-secure port is registered. Clients using the Eureka service discovery with `secure=true` only select instances with an enabled secure port, so they do not see such an instance.
To register a secure port, set `secure-port`; `secure-vip-address` and `non-secure-port-enabled` are optional:

=== "stork standalone"
```properties
stork.my-service.service-registrar.type=eureka
stork.my-service.service-registrar.host-name=my-service.example.com
stork.my-service.service-registrar.secure-port=443
# Optional, defaults to the virtual IP address (the service name)
stork.my-service.service-registrar.secure-vip-address=my-service-secure
# Optional, only for services not reachable over plain HTTP
stork.my-service.service-registrar.non-secure-port-enabled=false
```

=== "stork in quarkus"
```properties
quarkus.stork.my-service.service-registrar.type=eureka
quarkus.stork.my-service.service-registrar.host-name=my-service.example.com
quarkus.stork.my-service.service-registrar.secure-port=443
# Optional, defaults to the virtual IP address (the service name)
quarkus.stork.my-service.service-registrar.secure-vip-address=my-service-secure
# Optional, only for services not reachable over plain HTTP
quarkus.stork.my-service.service-registrar.non-secure-port-enabled=false
```

The secure virtual IP address defaults to the virtual IP address (the service name); set `secure-vip-address` to use another one.
For services only reachable over HTTPS, set `non-secure-port-enabled=false` to register the non-secure port as disabled; this requires `secure-port` to be set.
Secure clients connect to the registered host name, so set `host-name` to the name the TLS certificate is issued for.

## Service deregistration configuration

There is no specific configuration required to enable deregistration; however, you must ensure that a consul service registrar is configured for the service:

=== "stork standalone"
```properties
stork.my-service.service-registrar.type=eureka
```

=== "stork in quarkus"
```properties
quarkus.stork.my-service.service-registrar.type=eureka
```

As with registration, deregistration relies on the service name. 
