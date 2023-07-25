# Overview

SmallRye Stork `{{version.current}}` is a service discovery and client-side load-balancing framework.

The essence of distributed systems resides in the interaction between _services_.
In modern architecture, you often have multiple instances of your service to share the load or improve the resilience by redundancy.
But, how do you select the _best_ instance of your service?
That's where Stork helps.
Stork is going to select the most appropriate instance.
It offers:

* Extensible service discovery mechanisms
* Built-in support for Consul and Kubernetes
* Customizable client load-balancing strategies
* A programmatic API and a _managed_ approach
* A Quarkus integration, but Stork can be used in any environment

## The problem

In distributed systems, applications typically need to call one another.
In a monolithic application, components invoke one another through language-level method or procedure calls.
In a traditional distributed system deployment, services run at fixed, well-known locations (schemes, hosts, and ports) and can call one another using HTTP/REST or some RPC mechanism. The service locations are often hardcoded in the application configuration.
Nevertheless, a modern distributed system or microservice-based application typically runs in virtualized or containerized environments where the number of instances of a service and their locations change dynamically.
IPs get randomly assigned, and instances can be created or destroyed at any time.
With such dynamics, hard-coded locations are a dead-end.


![the problem](images/problem-light.png#only-light)
![the problem](images/problem-dark.png#only-dark)

## The solution

Stork handles the service lookup and selection.
It proposes an extensible set of service discovery mechanisms and load-balancing strategies.

![the solution](images/solution-light.png#only-light)
![the solution](images/solution-dark.png#only-dark)

## What if the infrastructure provides such a feature?

Some infrastructure, such as Kubernetes, provides service discoveries and load-balancing features.
However, these mechanisms often lack flexibility. You cannot influence the service instance selection, and the load-balancing strategy is generally a simple _round-robin_.

Stork provides more flexibility in the service instance selection. For example, it can select the fastest instance (based on the previous calls) to improve the response time.
However, if you don't need that flexibility, just use the infrastructure layer.


