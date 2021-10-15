# Round-Robin Load Balancing

The round-robin is a straightforward load-balancing solution that just iterates over the set of service instances.
While being simple, this solution shares the load among the instances and may be sufficient in many cases.

## Dependency

First, you need to add the round-robin load-balancer to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>smallrye-stork-load-balancer-round-robin</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service expected to use a round-robin selection, configure the `load-balancer` to be `round-robin`:

```properties
stork.my-service.service-discovery=...
stork.my-service.service-discovery...=...
stork.my-service.load-balancer=round-robin
```
