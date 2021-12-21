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

####A few words about server authentication. 
Stork uses Fabric8 Kubernetes Client to access the Kubernetes resources, concretely the `DefaultKubernetesClient` implementation. It will try to read the ~/.kube/config file in your home directory and load information required for authenticating with the Kubernetes API server. If you are using DefaultKubernetesClient from inside a Pod, it will load ~/.kube/config from the ServiceAccount volume mounted inside the Pod. You can override this configuration if you want a more complex configuration.


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

| Attribute              | Mandatory    | Default Value                                                                               | Description                                                         |
|-- -------------------- | ------------ | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| `k8s-host`             | No           | _master url_                                                                                | The Kubernetes API host                                             |
| `k8s-namespace`        | No           | Current namespace from the `.kube/config` file in your home directory or the mounted volume | The namespace of the service. Use `all` to discover all namespaces. |