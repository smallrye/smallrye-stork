# Concepts

This page presents the concepts used in Stork.
When using Stork in a _managed_ environment, such as Quarkus, all these concepts are hidden, as you only configure the lookup and selection.
However, when using the programmatic API, you will use these concepts directly.

![concepts](target/stork.png)

## Process overview

When using the programmatic API of Stork, you:

1. Retrieves the _singleton_ `Stork` instance. This instance is configured with the set of `Service` it manages.
2. Retrieves the `Service` you want to use. Each `Service` is associated with a name.
3. Retrieves the `ServiceInstance` which will provides the metadata to access the actual service.

![concepts](target/sequence.png)

Behind the scene, Stork will handle the service lookup and selection.

!!!note

    The service lookup and selection are asynchronous operations.
    Thus, the API returns instances of `Uni`. 

## Stork

`io.smallrye.stork.Stork` is the entry-point of the API.
The `Stork` instance is a _singleton_.
It needs to be initialized once (when the application starts) and shutdown when the application stops:

```java linenums="1"
--8<-- "docs/snippets/examples/StorkEntryPointExample.java"
```

During the initialization, Stork looks for `io.smallrye.stork.config.ConfigProvider` SPI provider and retrieves the list of managed services:

* A service is identified by a **name**. 
* A service has a service discovery configuration indicating how Stork will look for service instances
* A service can have a load-balancer configuration indicate how Stork can select the most appropriate instance.

## Service

A `io.smallrye.stork.Service` is the structure representing a _service_ used by the application.
Services are pre-configured with their name, service discovery, and optionally, their load-balancer.
You retrieve a `Service` using the `Stork#getService(String name)` method.

```java linenums="1"
--8<-- "docs/snippets/examples/StorkServiceExample.java"
```

The `Service` lets you retrieve the list of `ServiceInstance`, or select a single one, when a load-balancer is configured.

## Service Instance

The `io.smallrye.stork.ServiceInstance` represents an actual instance of the service. 
It provides the metadata to configure a _client_ to interact with that specific instance of service.

```java linenums="1"
--8<-- "docs/snippets/examples/StorkServiceLookupExample.java"
```

The service selection is a two-steps process:

1. Service lookup - using the service discovery
2. Service selection - using the load balancer

```java linenums="1"
--8<-- "docs/snippets/examples/StorkServiceSelectionExample.java"
```

## Service Discovery

The `io.smallrye.stork.ServiceDiscovery` represents a service discovery mechanism, such as DNS, Consul, or Eureka.

You can implement a custom service discovery for Stork by implementing the `ServiceDiscoveryProvider`
interface and register it with the Service Provider Interface (SPI) mechanism.

Please note that the `ServiceDiscovery` implementation must be non-blocking.

## Load Balancer

The `io.smallrye.stork.LoadBalancer` represents a load-balancer strategy, such as round-robin.

To implement a custom load balancer for Stork, implement the `LoadBalancerProvider`
interface and register it with the Service Provider Interface (SPI) mechanism.

Please note that the `LoadBalancer` implementation, similarly to `ServiceDiscovery`
must be non-blocking.