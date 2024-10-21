### Load Balancer / Service Selection in SmallRye Stork

Once services are registered and discovered, the next critical step is selecting which service instance will handle a given request. 
SmallRye Stork provides flexible load balancing strategies to efficiently distribute requests across multiple instances of a service. 
This ensures optimal resource usage, improved performance, and high availability.

#### Key Features:
- **Multiple Load Balancing Strategies**: SmallRye Stork supports several built-in strategies for selecting service instances. 
Check them out in the following dedicated sections.
- **Customizable Strategies**: You can define custom service selection strategies based on your unique use case or performance requirements, ensuring that the load balancer can adapt to specific needs.

#### How it Works:
Once a service has been registered and discovered, the load balancer comes into play when a client makes a request to that service. 
Stork applies the configured load balancing strategy to select an instance from the available pool of discovered services.

This feature ensures that your services remain responsive, scalable, and resilient, providing a smooth experience for both users and developers.
