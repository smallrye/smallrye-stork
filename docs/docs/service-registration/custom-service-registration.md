# Implement your own service registration mechanism

Stork is extensible, and you can implement your own service registrar mechanism.

## Dependencies

To implement your _Service Registration Provider_, make sure your project depends on Core and Configuration Generator. 
The former brings classes necessary to implement custom registrar, the latter contains an [annotation processor](https://docs.oracle.com/en/java/javase/11/docs/api/java.compiler/javax/annotation/processing/Processor.html) that generates classes needed by Stork.

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-core</artifactId>
    <version>{{version.current}}</version>
</dependency>
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-configuration-generator</artifactId>
    <scope>provided</scope>
    <!-- provided scope is sufficient for the annotation processor -->
    <version>{{version.current}}</version>
</dependency>
```

## Implementing a service registrar provider

Service discovery implementation consists of three elements:

- `ServiceRegistrar` which is responsible for registering service instances for a single Stork service.
- `ServiceRegistrarProvider` which creates instances of `ServiceRegistrar` for a given service registrar _type_.
- `$typeConfiguration` which is a configuration for the registrar. This class is automatically generated during the compilation (using an annotation processor).

A _type_, for example, `acme`, identifies each provider.
This _type_ is used in the configuration to reference the provider:

=== "stork standalone"
    ```properties
    stork.my-service.service-registrar.type=acme
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-registrar.type=acme
    ```

A `ServiceRegistrarProvider` implementation needs to be annotated with `@ServiceRegistrarType` that defines the _type_.
Any configuration properties that the provider expects should be defined with `@ServiceRegistrarAttribute` annotations placed on the provider.
Optionally, you can also add `@ApplicationScoped` annotation in order to provide the service registrar implementation as CDI bean.

A service registrar provider class should look as follows:

```java linenums="1"
{{ insert('examples/AcmeServiceRegistrarProvider.java') }}
```

Note, that the `ServiceRegistrarProvider` interface takes a configuration class as a parameter. This configuration class 
is generated automatically by the _Configuration Generator_. 
Its name is created by appending `Configuration` to the service discovery type, such as `AcmeConfiguration`.

The next step is to implement the `ServiceRegistrar` interface:

```java linenums="1"
{{ insert('examples/AcmeServiceRegistrar.java') }}
```

This implementation is simplistic.
Typically, instead of creating a service instance with values from the configuration, you would connect to a service discovery backend, look for the service and build the list of service instance accordingly.
That's why the method returns a `Uni`.
Most of the time, the lookup is a remote operation.

As you can see, the `AcmeConfiguration` class gives access to the configuration attribute.

## Using your service registrar

In the project using it, don't forget to add the dependency on the module providing your implementation.
Then, in the configuration, just add:

=== "stork standalone"
    ```properties
    stork.my-service.service-registrar.type=acme
    stork.my-service.service-registrar.host=localhost
    stork.my-service.service-registrar.port=1234
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-registrar.type=acme
    quarkus.stork.my-service.service-registrar.host=localhost
    quarkus.stork.my-service.service-registrar.port=1234
    ```


Then, Stork will use your implementation to register the service instances using the `my-service` backend.
