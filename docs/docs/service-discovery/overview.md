### Service Discovery in SmallRye Stork

As already introduced, service discovery is a crucial part of modern microservices architectures. 
It allows services to dynamically discover the location of other services at runtime, which is particularly useful in distributed systems where services may scale up or down,
or change their network addresses.

SmallRye Stork provides a flexible and extensible mechanism for service discovery.
It supports out of the box some service discovery such as Kubernetes or Consul but the main strength of it is customization so you can easily create your own implementation related on your business for example.
Stork allows services to communicate with each other without requiring hardcoded addresses, making it an ideal solution for microservices deployments.
SmallRye Stork brings this capability to clients for Quarkus applications but it's vendor agnostic so you easily use it with other solutions and even in standalone mode.


## Service discovery caching overview

Stork provides a built-in caching mechanism for service discovery results.
By default, each service discovery implementation may repeatedly contact its backend (e.g., Consul, Kubernetes, Eureka) to retrieve the current list of service instances.
Since these lookups can be expensive or frequent, Stork allows service discovery providers to cache the retrieved instances for a configurable period of time.

* Cached results are reused for subsequent lookups, avoiding unnecessary backend calls.
* The cache is automatically refreshed after the configured `refresh-period`.
* If a refresh fails, Stork keeps serving the previously retrieved list of instances until a new successful refresh occurs.

In addition to the time-based refresh mechanism, Stork also exposes an **cache invalidation API** .
This API can be used to explicitly clear the cached results, forcing the next lookup to perform a new service discovery call.
This is especially useful when the service discovery backend supports event-based notifications (for example, Consul blocking queries), allowing the cache to be invalidated as soon as a change is detected.

> For more details about how to enable caching in a custom service discovery implementation, see the [Caching the service instances](custom-service-discovery.md#caching-the-service-instances) section.


You can explore the different implementations and learn how to create your own in the following sections.