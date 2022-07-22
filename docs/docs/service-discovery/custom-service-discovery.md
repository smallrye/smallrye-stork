# Implement your own service discovery mechanism

Stork is extensible, and you can implement your own service discovery mechanism.

## Dependencies

To implement your _Service Discovery Provider_, make sure your project depends on Core and Configuration Generator. 
The former brings classes necessary to implement custom discovery, the latter contains an [annotation processor](https://docs.oracle.com/en/java/javase/11/docs/api/java.compiler/javax/annotation/processing/Processor.html) that generates classes needed by Stork.

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

- `ServiceDiscovery` which is responsible for locating service instances for a single Stork service.
- `ServiceDiscoveryProvider` which creates instances of `ServiceDiscovery` for a given service discovery _type_.
- `$typeConfiguration` which is a configuration for the discovery. This class is automatically generated during the compilation (using an annotation processor).

A _type_, for example, `acme`, identifies each provider.
This _type_ is used in the configuration to reference the provider:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=acme
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=acme
    ```

A `ServiceDiscoveryProvider` implementation needs to be annotated with `@ServiceDiscoveryType` that defines the _type_.
Any configuration properties that the provider expects should be defined with `@ServiceDiscoveryAttribute` annotations placed on the provider.

A service discovery provider class should look as follows:

```java linenums="1"
--8<-- "snippets/examples/AcmeServiceDiscoveryProvider.java"
```

Note, that the `ServiceDiscoveryProvider` interface takes a configuration class as a parameter. This configuration class 
is generated automatically by the _Configuration Generator_. 
Its name is created by appending `Configuration` to the service discovery type, such as `AcmeConfiguration`.

The next step is to implement the `ServiceDiscovery` interface:

```java linenums="1"
--8<-- "snippets/examples/AcmeServiceDiscovery.java"
```

This implementation is simplistic.
Typically, instead of creating a service instance with values from the configuration, you would connect to a service discovery backend, look for the service and build the list of service instance accordingly.
That's why the method returns a `Uni`.
Most of the time, the lookup is a remote operation.

As you can see, the `AcmeConfiguration` class gives access to the configuration attribute.

## Using your service discovery

In the project using it, don't forget to add the dependency on the module providing your implementation.
Then, in the configuration, just add:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=acme
    stork.my-service.service-discovery.host=localhost
    stork.my-service.service-discovery.port=1234
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=acme
    quarkus.stork.my-service.service-discovery.host=localhost
    quarkus.stork.my-service.service-discovery.port=1234
    ```


Then, Stork will use your implementation to locate the `my-service` service.

## Using your service discovery using the programmatic API

When building your service discovery project, the configuration generator creates a configuration class.
This class can be used to configure your service discovery using the Stork programmatic API.

```java
```java linenums="1"
--8<-- "snippets/examples/AcmeDiscoveryApiUsage.java"
```

Remember that attributes, like `host`, are declared using the `@ServiceDiscoveryAttribute` annotation on the `ServiceDiscoveryProvider` implementation.

## Caching the service instances

Your `ServiceDiscovery` implementation can extend `io.smallrye.stork.impl.CachingServiceDiscovery` to automatically _cache_ the service instance.
In this case, the retrieved set of `ServiceInstance` is cached and only updated after some time.
This duration is an additional configuration attribute.
For homogeneity, we recommend the following attribute:

```java
@ServiceDiscoveryAttribute(name = "refresh-period", description = "Service discovery cache refresh period.", 
        defaultValue = CachingServiceDiscovery.DEFAULT_REFRESH_INTERVAL)
```

The following snippet extends the _acme_ service discovery with the `refresh-period` attribute:

```java linenums="1"
--8<-- "docs/snippets/examples/CachedAcmeServiceDiscoveryProvider.java"
```

Extending `io.smallrye.stork.impl.CachingServiceDiscovery` changes the structure of the service discovery implementation:

```java linenums="1"
--8<-- "docs/snippets/examples/CachedAcmeServiceDiscovery.java"
```

1. Call the `super` constructor with the `refresh-period` value
2. Implement `fetchNewServiceInstances` instead of `getServiceInstances`.
   The method is called periodically, and the retrieved instances are cached.
   This implementation is simplistic.

If the retrieval fails, the error is reported, and Stork keeps the previously retrieved list of instances.


