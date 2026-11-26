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

## Lease renewal

Eureka evicts an instance whose lease is not renewed within the lease duration.
Stork renews the lease of each registered instance every `lease-renewal-interval` (default: 30 seconds) and registers the instance again if Eureka does not know it, e.g. after a restart of the Eureka server or if the initial registration failed.
Renewals stop when the instance is deregistered.
The registrar sends `lease-renewal-interval` and `lease-duration` (default: 90 seconds) to Eureka as lease information.

`lease-renewal-interval` must be greater than 0 and lower than `lease-duration`.
Keep it well below `lease-duration`, so that several renewals fit into one lease duration and a single failed renewal does not get the instance evicted.
Eureka's default ratio of 1:3 (30 and 90 seconds) is a good reference.

If you set `lease-renewal-enabled=false`, Eureka evicts the instance once `lease-duration` has expired.

!!! note
    Previous versions did not renew the lease and sent a fixed lease duration of 10000 seconds to Eureka instead.
    If you set `lease-renewal-enabled=false` or renew the lease with your own heartbeat, set a `lease-duration` that is long enough for your use case; otherwise, Eureka evicts the instance after 90 seconds.

With Stork standalone, deregister the instances before calling `Stork.shutdown()`; otherwise, their leases keep being renewed.
When Quarkus registers the instances, it deregisters them on shutdown.

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
