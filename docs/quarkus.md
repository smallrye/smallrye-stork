# Using Stork with Quarkus

[Quarkus](https://quarkus.io) is a Kubernetes Native Java stack tailored for OpenJDK HotSpot and GraalVM.

SmallRye Stork can be smoothly used with Quarkus REST Client Reactive and gRPC extensions. 

This page describes how to use REST Client Reactive with Stork and Consul and the round-robin load balancer. The other permutations of the components are similar.

## The project
Quarkus REST Client Reactive comes with a built-in integration with SmallRye Stork.
You can find instructions on how to create a project using REST Client Reactive in the [Quarkus guide](https://quarkus.io/guides/rest-client-reactive).

## The client
The way the REST client works, it has to have a `baseUri`, or `baseUrl` defined for requests it makes.
Stork's integration uses URI's schema to pass information that Stork should be used for a client.

Given a client interface as follows:
```java linenums="1"
--8<-- "docs/snippets/examples/HelloClient.java"
```

The following configuration says the client should use a Stork service called `hello-service` to determine the address of the destination and to use `/hello` as the base path for queries:
```properties
quarkus.rest-client.hello-client.uri=stork://hello-service/hello
```

## The service
Stork's `Service` consists of service discovery and load balancer. 
The REST Client Reactive extension uses `Service` to determine a single `ServiceInstance` for every outgoing call. `ServiceInstance` carries address of the remote endpoint.

### Dependencies
To use service discovery and load balancer of your choosing, you need to add appropriate dependencies to your application. E.g. if you wish to use Consul and load-balance the calls with round-robin, you should add:
```xml
    <dependency>
        <groupId>io.smallrye.stork</groupId>
        <artifactId>smallrye-stork-service-discovery-consul</artifactId>
        <version>{{version.current}}</version>
    </dependency>
    <dependency>
        <groupId>io.smallrye.stork</groupId>
        <artifactId>smallrye-stork-load-balancer-round-robin</artifactId>
        <version>{{version.current}}</version>
    </dependency>
```

When using Eureka, Kubernetes, or any other service discovery mechanism, or a different load balancer, replace the dependencies above with the ones you need.
Providers for both, service discovery and load balancer, will be automatically registered when the dependencies are present.

### The config
The last piece of the puzzle is the actual service configuration. If your Consul instance is running on `localhost` on port `8500`, service discovery configuration should look as follows:

```properties
stork.hello-service.service-discovery=consul
stork.hello-service.service-discovery.consul-host=localhost
stork.hello-service.service-discovery.consul-port=8500
```

For round-robin, the config should simply define the load balancer type:
```properties
stork.hello-service.load-balancer=round-robin
```