# Implement your own load balancer mechanism

Stork is extensible, and you can implement your service selection (load-balancer) mechanism.
Stork uses the SPI mechanism for loading implementations matching _Load Balancer Provider_ interface

## Dependency

To implement your _Load Balancer Provider_, make sure your project depends on:

```xml
<dependency>
    <groupI>io.smallrye.stork</groupI>
    <artifactId>smallrye-stork-api</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Implementing a load balancer provider

Stork uses the SPI mechanism for loading implementations matching _Load Balancer Provider_ interface during its initialization.
As a consequence, a load balancer provider implementation will contain:

![structure](target/load-balancer-provider-structure.png)

The _provider_ is a factory that creates an `io.smallrye.stork.LoadBalancer` instance for each configured service using this load balancer provider.
A _type_ identifies each provider.
You will use that _type_ in the configuration to reference the load-balancer provider you want for each service:

```properties
stork.my-service.load-balancer=acme
```

The first step consists of implementing the `io.smallrye.stork.spi.LoadBalancerProvider` interface:

```java linenums="1"
--8<-- "docs/snippets/examples/AcmeLoadBalancerProvider.java"
```

This implementation is straightforward.
The `type` method returns the load balancer provider identifier.
The `createLoadBalancer` method is the factory method.
It receives the instance configuration (a map constructed from all `stork.my-service.load-balancer.attr=value` properties)

Then, obviously, we need to implement the `LoadBalancer` interface:

```java linenums="1"
--8<-- "docs/snippets/examples/AcmeLoadBalancer.java"
```

Again, this implementation is simplistic and just picks a random instance from the received list.

The final step is to declare our `LoadBalancerProvider` in the `META-INF/services/io.smallrye.stork.spi.LoadBalancerProvider` file:

```text
examples.AcmeLoadBalancerProvider
```

## Using your load balancer

In the project using it, don't forget to add the dependency on the module providing your implementation.
Then, in the configuration, just add:

```properties
stork.my-service.service-discovery=...
stork.my-service.load-balancer=acme
```

Then, Stork will use your implementation to select the `my-service` service instance.
