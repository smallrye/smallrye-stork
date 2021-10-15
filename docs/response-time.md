# Least Response Time Load Balancing

The `least-response-time` load balancing strategy monitors the operations and selects the _fastest_ instance.
This load balancer collects statistics and determines which instance will provide the _shorter_ response time.

## Dependency

First, you need to add the least-response-time load-balancer to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>smallrye-stork-load-balancer-response-time</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service expected to use a least-response-time selection, configure the `load-balancer` to be `least-response-time`:

```properties
stork.my-service.service-discovery=...
stork.my-service.service-discovery...=...
stork.my-service.load-balancer=least-response-time
```
