# Least Requests Load Balancing

The `least-requests` load balancing strategy monitors the number of inflight calls and selects the less-used instance.

This strategy keeps track of the inflight calls made by the application and picks the service instance with the smallest number of inflight requests:

1. when the selection happens, the service instance with the smallest number of inflight requests is selected, and this number is incremented
2. when the operation completes, successfully or not, the number of inflight requests is decremented

## Dependency

First, you need to add the `least-requests` load-balancer to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-load-balancer-least-requests</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service expected to use a least-response-time selection, configure the `load-balancer` to be `least-requests`:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery=...
    stork.my-service.service-discovery...=...
    stork.my-service.load-balancer=least-requests
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery=...
    quarkus.stork.my-service.service-discovery...=...
    quarkus.stork.my-service.load-balancer=least-requests
    ```

[//]: # (Supported attributes are the following:)

[//]: # ()
[//]: # (--8<-- "load-balancer/least-requests/target/classes/META-INF/stork-docs/least-requests-lb-attributes.txt")