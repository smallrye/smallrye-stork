# Implement your own load balancer mechanism

Stork is extensible, and you can implement your service selection (load-balancer) mechanism.

## Dependencies

To implement your _Load Balancer Provider_, make sure your project depends on Core and Configuration Generator. 
The former brings classes necessary to implement custom load balancer, the latter contains an annotation processor that generates classes needed by Stork.

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

## Implementing a load balancer provider

Load balancer implementation consists of three elements:

- `LoadBalancer` which is responsible for selecting service instances for a single Stork service,
- `LoadBalancerProvider` which creates instances of `LoadBalancer` for a given load balancer _type_,
- `$typeConfiguration` which is a configuration for the load balancer. This class is automatically generated.

A _type_, for example `acme-load-balancer`, identifies each provider.
This _type_ is used in the configuration to reference the provider:

=== "stork standalone"
    ```properties
    stork.my-service.load-balancer.type=acme-load-balancer
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.load-balancer.type=acme-load-balancer 
    ```


A `LoadBalancerProvider` implementation needs to be annotated with `@LoadBalancerType` that defines the _type_.
Any configuration properties that the provider expects should be defined with `@LoadBalancerAttribute` annotations placed on the provider.

A load balancer provider class should look as follows:
```java linenums="1"
--8<-- "snippets/examples/AcmeLoadBalancerProvider.java"
```

Note, that the `LoadBalancerProvider` interface takes a configuration class as a parameter. 
This configuration class  is generated automatically by the _Configuration Generator_.
Its name is created by appending `Configuration` to the load balancer type, like `AcmeLoadBalancerConfiguration`.

The next step is to implement the `LoadBalancer` interface.

The essence of load balancers' work happens in the `selectServiceInstance` method. The method returns a single `ServiceInstance` from a collection. 

```java linenums="1"
--8<-- "snippets/examples/AcmeLoadBalancer.java"
```

This implementation is simplistic and just picks a random instance from the received list.

Some load balancers make the pick based on statistics such as calls in progress or response times, or amount of errors of a service instance. 
To collect this information in your load balancer, you can wrap the selected service instance into `ServiceInstanceWithStatGathering`.

Load balancers based on statistics often expect that an operation using a selected service instance is marked as started before the next selection. 
By default, Stork assumes that a `LoadBalancer` requires this and guards the calls accordingly. 
If this is not the case for your implementation, override the `requiresStrictRecording()` method to return `false`.

## Using your load balancer

In the project using it, don't forget to add the dependency on the module providing your implementation.
Then, in the configuration, just add:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=...
    stork.my-service.load-balancer.type=acme-load-balancer\
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=...
    quarkus.stork.my-service.load-balancer.type=acme-load-balancer
    ```

Then, Stork will use your implementation to select the `my-service` service instance.

## Using your load balancer using the programmatic API

When building your load balancer project, the configuration generator creates a configuration class.
This class can be used to configure your load balancer using the Stork programmatic API. 

```java linenums="1"
--8<-- "snippets/examples/AcmeSelectorApiUsage.java"
```

Remember that attributes, like `my-attribute`, are declared using the `@LoadBalancerAttribute` annotation on the `LoadBalancerProvider` implementation.