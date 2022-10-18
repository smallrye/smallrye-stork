# Sticky Load Balancing

The sticky load balancer selects a single service instance and keeps using it until it fails.
Then, it selects another one.

It is possible to configure a backoff time to specify for how long a failing service instance should not be retried.

Precisely, the load balancer works as follows:

* if no service instance has been selected so far, select the first instance from the collection;
* else if the previously selected service instance has not failed, and is still available, return it;
* else return the first available service instance that has no recorded failure, if one exists;
* else, find the available instance for which the time since the last failure is the longest, and
    * if the backoff time since the failure passed, return it;
    * or, throw an `NoAcceptableServiceInstanceFoundException` as no acceptable instances are available.

## Configuration

To use the `sticky` load service selection strategy, set the load balancer type to `sticky`:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=...
    stork.my-service.service-discovery...=...
    stork.my-service.load-balancer.type=sticky
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=...
    quarkus.stork.my-service.service-discovery...=...
    quarkus.stork.my-service.load-balancer.type=sticky
    ```


The following attributes are supported:

--8<-- "../load-balancer/sticky/target/classes/META-INF/stork-docs/sticky-lb-attributes.txt"