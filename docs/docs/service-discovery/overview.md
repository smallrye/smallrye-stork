### Service Discovery in SmallRye Stork

As already introduced, service discovery is a crucial part of modern microservices architectures. 
It allows services to dynamically discover the location of other services at runtime, which is particularly useful in distributed systems where services may scale up or down,
or change their network addresses.

SmallRye Stork provides a flexible and extensible mechanism for service discovery.
It supports out of the box some service discovery such as Kubernetes or Consul but the main strength of it is customization so you can easily create your own implementation related on your business for example.
Stork allows services to communicate with each other without requiring hardcoded addresses, making it an ideal solution for microservices deployments.
SmallRye Stork brings this capability to clients for Quarkus applications but it's vendor agnostic so you easily use it with other solutions and even in standalone mode.

You can explore the different implementations and learn how to create your own in the following sections.