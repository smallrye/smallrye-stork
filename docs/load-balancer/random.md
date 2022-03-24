# Random Load Balancing

The `random` load balancing is a straightforward service instance selection solution that picks a random instance every time.

## Dependency

First, you need to add the random load-balancer to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-load-balancer-random</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service expected to use a random service selection, configure the `load-balancer` to be `random`:

```properties
stork.my-service.service-discovery=...
stork.my-service.service-discovery...=...
stork.my-service.load-balancer=random
```

Supported attributes are the following:

--8<-- "load-balancer/random/target/classes/META-INF/stork-docs/random-lb-attributes.txt"
