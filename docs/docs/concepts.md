# Concepts

This page presents the concepts used in Stork.
When using Stork in a _managed_ environment, such as Quarkus, all these concepts are hidden, as you only configure the lookup and selection.
However, when using the programmatic API, you will use these concepts directly.


![concepts](target/stork.svg#only-light)
![concepts](target/stork_dark.svg#only-dark)

## Process overview

When using the programmatic API of Stork, you can:

1. Retrieve the _singleton_ `Stork` instance. This instance is configured with the set of `Service` it manages.
2. Retrieve the `Service` you want to use. Each `Service` is associated with a name.
3. Retrieve the `ServiceInstance` which will provide the metadata to access the actual service.

![service discovery and selection](target/sequence.svg#only-light)
![service discovery and selection](target/sequence_dark.svg#only-dark)

Behind the scenes, Stork will handle the service lookup and selection.

!!!note

    The service lookup and selection are asynchronous operations.
    Thus, the API returns instances of `Uni`. 

## Stork

`io.smallrye.stork.Stork` is the entry-point of the API.
The `Stork` instance is a _singleton_.
It needs to be initialized once (when the application starts) and shutdown when the application stops:

```java linenums="1"
--8<-- "snippets/examples/StorkEntryPointExample.java"
```

During the initialization, Stork looks for `io.smallrye.stork.config.ConfigProvider` SPI provider and CDI beans (from 2.x version) and retrieves the list of managed services:

* A service is identified by a **name**. 
* A service has a service discovery configuration indicating how Stork will look for service instances
* A service can have a load-balancer configuration indicating how Stork can select the most appropriate instance.

## Service

A `io.smallrye.stork.Service` is the structure representing a _service_ used by the application.
Services are pre-configured with their name, service discovery, and optionally, their load-balancer.
You retrieve a `Service` using the `Stork#getService(String name)` method.

```java linenums="1"
--8<-- "snippets/examples/StorkServiceExample.java"
```

The `Service` lets you retrieve the list of `ServiceInstance`, or select a single one, when a load-balancer is configured.

## Service Instance

The `io.smallrye.stork.api.ServiceInstance` represents an actual instance of the service. 
It provides the metadata to configure a _client_ to interact with that specific instance of service.

```java linenums="1"
--8<-- "snippets/examples/StorkServiceLookupExample.java"
```

The service selection is a two-steps process:

1. Service lookup - using the service discovery
2. Service selection - using the load balancer

```java linenums="1"
--8<-- "snippets/examples/StorkServiceSelectionExample.java"
```

## Service Discovery

The `io.smallrye.stork.api.ServiceDiscovery` represents a service discovery mechanism, such as DNS, Consul, or Eureka.

You can implement a custom service discovery for Stork by implementing the `ServiceDiscoveryProvider` interface.
The corresponding `ServiceRegistrarProviderLoader` and `RegistrarConfiguration` classes will be automatically generated during compilation time.

Please note that the `ServiceDiscovery` implementation must be non-blocking.

## Load Balancer

The `io.smallrye.stork.api.LoadBalancer` represents a load-balancer strategy, such as round-robin.

To implement a custom load balancer for Stork, implement the `LoadBalancerProvider` interface.
The corresponding `LoadBalancerProviderLoader` and `Configuration` classes will be automatically generated during compilation time.

Please note that the `LoadBalancer` implementation, similarly to `ServiceDiscovery`
must be non-blocking.

## Service registration

The `io.smallrye.stork.api.ServiceRegistrar` represents a service registration mechanism for Consul and Eureka.

You can implement a custom service registrar for Stork by implementing the `ServiceRegistrarProvider` interface.
The corresponding `ServiceRegistrarProviderLoader` and `RegistrarConfiguration` classes will be automatically generated during compilation time. 

Please note that the `ServiceRegistrar` implementation must be non-blocking.