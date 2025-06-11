### Service Registration in SmallRye Stork

Service registration is the process by which services announce their availability to a central registry, allowing other services to discover and communicate with them. 
In SmallRye Stork, service registration is automated and integrated with supported registries like **Consul**. 
This ensures that services can dynamically join and leave the network.

#### Key Features:
- **Automatic Registration**: For Quarkus applications, SmallRye Stork automatically registers it with the configured service registry (e.g., Consul).

**IMPORTANT** Public IP address needs to be provided. Smallrye Stork will fail if the service IP address is not provided during registration.

#### Supported Registries:
Currently, Smallrye Stork provides seamless integration with **Consul**, Eureka and a Static registry.
This integration simplifies the management of dynamic environments where services are frequently added or removed.

#### Custom Registration:
In addition to the default mechanisms, SmallRye Stork allows you to implement custom service registration strategies, 
providing flexibility for different infrastructures or custom service registration needs.

In the following sections you can have more details about each specific implementation.

### Deregister Services

SmallRye Stork now supports deregistration of service instances from a central registry when explicitly requested. 
This feature complements the registration mechanism.

This operation is typically should be triggered when a service instance is shut down or no longer available, helping maintain a consistent and accurate service registry.
To enable service deregistration, you can invoke the `deregisterServiceInstance` method on the `ServiceRegistrar` implementation programmatically.

**Note**: When used in standalone mode, Stork does **not** automatically handle service instance registration or deregistration. 
However, when using the quarkus-stork-registration extension within a Quarkus application, 
service instances are automatically registered at application startup and deregistered upon shutdown, unless this behavior is explicitly disabled.

In the following sections you can have more details about each specific implementation.