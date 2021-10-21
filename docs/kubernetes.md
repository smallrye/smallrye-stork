# Kubernetes Service Discovery

Kubernetes has a built-in support for service discovery and load-balancing.
However, you may need more flexibility to carefully select the service instance you want.

This page explains how Stork can use the Kubernetes API to handle the service discovery.

## Dependency

First, you need to add the Stork Kubernetes Service Discovery provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>smallrye-stork-service-discovery-kubernetes</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service expected to be exposed as _Kubernetes Service_, configure the lookup:

```properties
stork.my-service.service-discovery=kubernetes
stork.my-service.service-discovery.k8s-namespace=my-namespace
```

Stork looks for the _Kubernetes Service_ with the given name (`my-service` in the previous example) in the specified namespace.

Instead of using the _Kubernetes Service_ IP directly, and let Kubernetes handle the selection and balancing, Stork inspects the service and retrieves the list of _pods_ providing the service.
Then, it can select the instance.


Supported attributes are the following:

| Attribute              | Mandatory    | Default Value     | Description                                                         |
|-- -------------------- | ------------ | ----------------- | ------------------------------------------------------------------- |
| `k8s-host`             | No           | _master url_      | The Kubernetes API host                                             |
| `k8s-namespace`        | No           |                   | The namespace of the service. Use `all` to discover all namespaces. |