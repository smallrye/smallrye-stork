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

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=composite
    stork.my-service.service-discovery.services=serviceA,serviceB
    
    stork.serviceA.service-discovery.type=...
    stork.serviceB.service-discovery.type=...
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=composite
    quarkus.stork.my-service.service-discovery.services=serviceA,serviceB
    
    quarkus.stork.serviceA.service-discovery.type=...
    quarkus.stork.serviceB.service-discovery.type=...
    ```

Remember to define the services that make up your composite service.

Be aware that Stork doesn't work as a standalone service discovery cluster. 
Instead, it processes composite configurations—meaning it handles multiple configurations that may use different service discovery implementation. 
For each specific configuration, Stork delegates the service discovery task to the appropriate service discovery provider. 
If one of these providers doesn't respond or fails, it affects Stork's ability to resolve that specific configuration, but it doesn't mean Stork itself is faulty; 
it relies on the performance of the service discovery systems it’s configured to work with.

These are all the parameters of the composite service discovery:

--8<-- "target/attributes/META-INF/stork-docs/composite-sd-attributes.txt"