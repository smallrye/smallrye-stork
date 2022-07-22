# Static List Service Discovery

In some situations, such as demos, development, or testing, you may want to mock the service discovery by providing a predefined list of service instances.
For this purpose, Stork comes with a `static` service discovery type.

## Dependency

To use the `static` service discovery, first add the appropriate Service Discovery provider dependency to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-static-list</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service that should use the static list of service instances configure the service discovery `type`:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=static
    stork.my-service.service-discovery.address-list=localhost:8080,localhost:8081
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=static
    quarkus.stork.my-service.service-discovery.address-list=localhost:8080,localhost:8081
    ```

These are all the static service discovery parameters:

--8<-- "service-discovery/static-list/target/classes/META-INF/stork-docs/static-sd-attributes.txt"