# Implement your own load balancer mechanism

Stork is extensible, and you can implement your service selection (load-balancer) mechanism.

## Dependencies

To implement your _Load Balancer Provider_, make sure your project depends on Core and Configuration Generator. The former brings classes necessary to implement custom load balancer, the latter contains an annotation processor that generates classes needed by Stork.

```xml
<dependency>
    <groupI>io.smallrye.stork</groupI>
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

## Implementing a load balancer provider

Load balancer implementation consists of three elements:

- `LoadBalancer` which is responsible for selecting service instances for a single Stork service
- `LoadBalancerProvider` which creates instances of `LoadBalancer` for a given load balancer _type_
- `LoadBalancerProviderConfiguration` which is a configuration for the load balancer

A _type_, for example, `acme`, identifies each provider.
This _type_ is used in the configuration to reference the provider:

```properties
stork.my-service.load-balancer=acme
```

A `LoadBalancerProvider` implementation needs to be annotated with `@LoadBalancerType` that defines the _type_.
Any configuration properties that the provider expects should be defined with `@LoadBalancerAttribute` annotations placed on the provider.

A load balancer provider class should look as follows:
```java linenums="1"
--8<-- "docs/snippets/examples/AcmeLoadBalancerProvider.java"
```

Note, that the `LoadBalancerProvider` interface takes a configuration class as a parameter. This configuration class
is generated automatically by the _Configuration Generator_.
Its name is created by appending `Configuration` to the name of the provider class.

The next step is to implement the `LoadBalancer` interface:

```java linenums="1"
--8<-- "docs/snippets/examples/AcmeLoadBalancer.java"
```

This implementation is simplistic and just picks a random instance from the received list.

## Using your load balancer

In the project using it, don't forget to add the dependency on the module providing your implementation.
Then, in the configuration, just add:

```properties
stork.my-service.service-discovery=...
stork.my-service.load-balancer=acme
```

Then, Stork will use your implementation to select the `my-service` service instance.
