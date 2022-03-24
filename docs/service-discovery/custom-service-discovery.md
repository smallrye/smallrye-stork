# Implement your own service discovery mechanism

Stork is extensible, and you can implement your own service discovery mechanism.

## Dependencies

To implement your _Service Discovery Provider_, make sure your project depends on Core and Configuration Generator. The former brings classes necessary to implement custom discovery, the latter contains an annotation processor that generates classes needed by Stork.

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

## Implementing a service discovery provider

Service discovery implementation consists of three elements:

- `ServiceDiscovery` which is responsible for locating service instances for a single Stork service
- `ServiceDiscoveryProvider` which creates instances of `ServiceDiscovery` for a given service discovery _type_.
- `ServiceDiscoveryProviderConfiguration` which is a configuration for the discovery

A _type_, for example, `acme`, identifies each provider.
This _type_ is used in the configuration to reference the provider:

```properties
stork.my-service.service-discovery=acme
```

A `ServiceDiscoveryProvider` implementation needs to be annotated with `@ServiceDiscoveryType` that defines the _type_.
Any configuration properties that the provider expects should be defined with `@ServiceDiscoveryAttribute` annotations placed on the provider.

A service discovery provider class should look as follows:

```java linenums="1"
--8<-- "docs/snippets/examples/AcmeServiceDiscoveryProvider.java"
```

Note, that the `ServiceDiscoveryProvider` interface takes a configuration class as a parameter. This configuration class 
is generated automatically by the _Configuration Generator_. 
Its name is created by appending `Configuration` to the name of the provider class.

The next step is to implement the `ServiceDiscovery` interface:

```java linenums="1"
--8<-- "docs/snippets/examples/AcmeServiceDiscovery.java"
```

This implementation is simplistic.
Typically, instead of creating a service instance with values from the configuration, you would connect to a service discovery backend, look for the service and build the list of service instance accordingly.
That's why the method returns a `Uni`.
Most of the time, the lookup is a remote operation.

## Using your service discovery

In the project using it, don't forget to add the dependency on the module providing your implementation.
Then, in the configuration, just add:

```properties
stork.my-service.service-discovery=acme
stork.my-service.service-discovery.host=localhost
stork.my-service.service-discovery.port=1234
```

Then, Stork will use your implementation to locate the `my-service` service.