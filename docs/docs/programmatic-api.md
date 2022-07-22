# Stork Programmatic API

Stork proposes a programmatic API that lets you register new service Definitions and do manual lookup and selection.
When using the programmatic API of Stork, you can:
Retrieve the singleton Stork instance. This instance is configured with the set of Services it manages.
Register new service definition.
Retrieve the Service you want to use. Each Service is associated with a name.
Retrieve the ServiceInstance, which will provide the metadata to access the actual instance.


## Initializing Stork

If your framework does not already provide a configured `Stork` instance, you need to do:

```java linenums="1"
--8<-- "snippets/examples/InitializationExample.java"
```

## Adding service dynamically

To register a new `ServiceDefinition`, use the `defineIfAbsent` method:

```java linenums="1"
--8<-- "snippets/examples/DefinitionExample.java"
```

The `ServiceDefinition` instances can be created from:

- A service discovery configuration - these classes are provided by the service discovery implementations,
- An optional load balancer configuration - these classes are provided by the load balancer implementations

Attributes from the service discovery and load balancer can be configured from the `Configuration` classes.

## Looking for service instances

To list the service instances for a given service, or to select an instance according to the load balancer strategy, use the following code:

```java linenums="1"
--8<-- "snippets/examples/LookupExample.java"
```

The lookup and selection methods are returning Uni as these processes are asynchronous.

## All in one example

The following snippet provides an _all in one_ example of the Stork programmatic API:

```java linenums="1"
--8<-- "snippets/examples/StorkApiExample.java"
```
