# Using Stork with Quarkus

[Quarkus](https://quarkus.io) is a Kubernetes Native Java stack tailored for OpenJDK HotSpot and GraalVM.

Quarkus REST Client Reactive and gRPC extensions come with built-in integration with SmallRye Stork.

This page describes how to use REST Client Reactive with Stork. Using gRPC with Stork is similar.

We will use the Consul service discovery and the round-robin load balancer as examples.

## The project

You can create a Quarkus project with the REST Client Reactive extension using [code.quarkus.io](https://code.quarkus.io).

The corresponding [Quarkus guide](https://quarkus.io/guides/rest-client-reactive) describes the extension in more detail.

## The client

To use the REST client to communicate with a remote endpoint, you need to create an interface that describes how the communication should work.
The client requires `baseUri` (or `baseUrl`) pointing to the address of the remote endpoint.

To use Stork to determine the actual address, set the scheme of the URI to `stork` and the hostname of the URI to the name of the Stork service.


For example, the `HelloClient` below will use the Stork service called `hello-service` to determine the address of the destination, and `/hello` as the base path for queries:
```java linenums="1"
--8<-- "docs/snippets/examples/HelloClient.java"
```

## The service
In Stork, a `Service` consists of service discovery and a load balancer. 
The Service discovery is responsible for determining the `ServiceInstance`s, that is, available addresses for a service. 
The load balancer picks a single `ServiceInstance` for a call.

### Dependencies
To use the service discovery and the load balancer of your choosing, you need to add the appropriate dependencies to your application. 
For example, if you wish to use Consul and load-balance the calls with round-robin, add the following to your `pom.xml`:

```xml
    <dependency>
        <groupId>io.smallrye.stork</groupId>
        <artifactId>stork-service-discovery-consul</artifactId>
    </dependency>
```

!!! note
    No need to add a dependency for the round-robin load-balancer. This strategy is built-in.
    Also, no need to specify the stork version, Quarkus imports the Stork _BOM_.


When using Eureka, Kubernetes, or any other service discovery mechanism, or a different load balancer, replace the dependencies above with the ones you need.
Based on the defined dependencies, Stork automatically registers providers for service discovery mechanisms and load balancers.

### The config
The last piece of the puzzle is the actual service configuration. 
If your Consul instance is running on `localhost` on port `8500`, service discovery configuration should look as follows:

```properties
quarkus.stork.hello-service.service-discovery.type=consul
quarkus.stork.hello-service.service-discovery.consul-host=localhost
quarkus.stork.hello-service.service-discovery.consul-port=8500
```

!!! note
When no load-balancing is configured, Stork uses a round-robin.
