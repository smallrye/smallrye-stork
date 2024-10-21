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
In addition to the default mechanisms, SmallRye Stork allows you to implement custom service registration strategies, providing flexibility for different infrastructures or custom service discovery needs.

In the following sections you can have more details about each specific implementation.