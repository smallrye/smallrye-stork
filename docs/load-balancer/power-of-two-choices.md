# Power Of Two Choices Load Balancing

The `power-of-two-choices` load balancing selects two random service instances and then chooses the one with the least inflight requests.
It avoids the overhead of `least-requests` and the worst case for `random` where it selects a busy destination.

This strategy acts as follows:

1. when the selection happens, it picks two random instances from the list,
2. it returns the least loaded instance (based on the number of inflight requests),
3. when the operation completes, successfully or not, the number of inflight requests for the instance is decremented.

Check [The Power of Two Random Choices](http://www.eecs.harvard.edu/~michaelm/NEWWORK/postscripts/twosurvey.pdf) paper to learn more about this pattern and the benefits.

## Dependency

First, you need to add the random load-balancer to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-load-balancer-power-of-two-choices</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service expected to use a random service selection, configure the `load-balancer` to be `power-of-two-choices`:


=== "stork standalone"
    ```properties
    stork.my-service.service-discovery=...
    stork.my-service.service-discovery...=...
    stork.my-service.load-balancer=power-of-two-choices
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery=...
    quarkus.stork.my-service.service-discovery...=...
    quarkus.stork.my-service.load-balancer=power-of-two-choices
    ```

Supported attributes are the following:

--8<-- "load-balancer/power-of-two-choices/target/classes/META-INF/stork-docs/power-of-two-choices-lb-attributes.txt"