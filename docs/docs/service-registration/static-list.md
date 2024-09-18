# Static List Service Registration

Stork provides the ability to register services using Static list as backend.


## Dependency

To use the `static` service registrar, first add the appropriate Service Registration provider dependency to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-registration-static-list</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Service registration configuration

For each service that should register the service instances in a static list, configure the service registrar `type`:

=== "stork standalone"
```properties
stork.my-service.service-registrar.type=static
```

=== "stork in quarkus"
```properties
quarkus.stork.my-service.service-registrar.type=static
```

Static service registrar is configured with the following parameters:

--8<-- "target/attributes/META-INF/stork-docs/static-sr-attributes.txt"