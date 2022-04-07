# Composite Service Discovery

Some organizations use hybrid infrastructure. In such an infrastructure, different service instances may be discoverable via different service discovery providers. E.g. when _serviceA_ is being migrated from a VM to Kubernetes, its older versions can be discovered via Consul and newer via Kubernetes. 

`composite` service discovery addresses this problem by letting you define a service that consists of multiple services.

## Dependency

To use the `composite` service discovery, first add the appropriate Service Discovery provider dependency to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-composite</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service that should consist of multiple services, configure the service discovery `type`, and set the `services` property to a comma separated list of services:

```properties
stork.my-service.service-discovery.type=composite
stork.my-service.service-discovery.services=serviceA,serviceB

stork.serviceA.service-discovery.type=...
stork.serviceB.service-discovery.type=...
```

Remember to define the services that make up your composite service.

These are all the parameters of the composite service discovery:

--8<-- "service-discovery/composite/target/classes/META-INF/stork-docs/composite-sd-attributes.txt"