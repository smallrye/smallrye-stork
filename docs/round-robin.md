# Round-Robin Load Balancing

The round-robin is a straightforward load-balancing solution that just iterates over the set of service instances.
While being simple, this solution shares the load among the instances and may be sufficient in many cases.

The round-robin strategy is the default load-balancing strategy.
It is provided by Stork itself, and so does not require an additional dependency.

## Configuration

There is no need to configure the load-balancing strategy to be `round-robin`.
Stork automatically uses this strategy when none are configured.

However, you can also configure it explicitly as follows:

```properties
stork.my-service.service-discovery=...
stork.my-service.service-discovery...=...
stork.my-service.load-balancer=round-robin
```