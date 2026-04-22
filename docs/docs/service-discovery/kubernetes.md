# Kubernetes Service Discovery

Kubernetes has a built-in support for service discovery and load-balancing.
However, you may need more flexibility to carefully select the service instance you want.

This page explains how Stork can use the Kubernetes API to handle the service discovery.

## Dependency

First, you need to add the Stork Kubernetes Service Discovery provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-kubernetes</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

#### A few words about server authentication. 
Stork uses [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client#readme) to access the Kubernetes resources, concretely the `DefaultKubernetesClient` implementation. 

It will try to read the `~/.kube/config` file from your local machine and load the token for authenticating with the Kubernetes API server.

If you are using the Stork Kubernetes discovery provider from inside a _Pod_, it loads `~/.kube/config` from the container file system.

This file is automatically mounted inside the Pod.

The level of access (Roles) depends on the configured `ServiceAccount`.

You can override this configuration if you want fine-grain control.

##### Role-based access control (RBAC)
If you're using a Kubernetes cluster with Role-Based Access Control (RBAC) enabled, the default permissions for a ServiceAccount don't allow it to list or modify any resources. 
A `ServiceAccount`, a `Role` and a `RoleBinding` are needed in order to allow Stork to list the available service instances from the cluster or the namespace. 

An example that allows listing all endpoints could look something like this:

```yaml
------
apiVersion: v1
kind: ServiceAccount
metadata:
  name: <appname>
  namespace: <namespace>
---
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: <appname>
  namespace: <namespace>
rules:
  - apiGroups: [""] # "" indicates the core API group
    resources: ["endpoints", "pods", "services"] # stork queries service endpoints, pods and services
    verbs: ["get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: RoleBinding
metadata:
  name: <appname>
  namespace: <namespace>
subjects:
  - kind: ServiceAccount
    # Reference to upper's `metadata.name`
    name: <appname>
    # Reference to upper's `metadata.namespace`
    namespace: <namespace>
roleRef:
  kind: Role
  name: <appname>
  apiGroup: rbac.authorization.k8s.io
```

## Configuration

For each service expected to be exposed as _Kubernetes Service_, configure the lookup:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=kubernetes
    stork.my-service.service-discovery.k8s-namespace=my-namespace
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=kubernetes
    quarkus.stork.my-service.service-discovery.k8s-namespace=my-namespace
    ```


Stork looks for the _Kubernetes Service_ with the given name (`my-service` in the previous example) in the specified namespace.

Instead of using the _Kubernetes Service_ IP directly, and let Kubernetes handle the selection and balancing, Stork inspects the service and retrieves the list of _pods_ providing the service.
Then, it can select the instance.


## EndpointSlice support (Experimental)

This feature is experimental. It is opt-in, and the API or behavior may change based on user feedback and Kubernetes evolution.
Recent Kubernetes versions expose service endpoints using **EndpointSlices** (`discovery.k8s.io/v1`), 
which are the recommended and scalable replacement for classic `Endpoints`.

Stork supports EndpointSlices and can use them transparently for service discovery.

### Why EndpointSlices

Compared to classic `Endpoints`, EndpointSlices:

* Scale better for services with many pods.
* Reduce load on the Kubernetes API server.
* Produce smaller and more frequent updates.
* Are the preferred API in modern Kubernetes clusters.

When available, EndpointSlices are the recommended source for service discovery.

## Configuration

By default, Stork uses **classic Endpoints** for compatibility.
EndpointSlices can be enabled explicitly or selected automatically.

### Explicit configuration

You can force the use of EndpointSlices per service:

```properties
quarkus.stork.my-service.service-discovery.use-endpoint-slices=true
```

Or explicitly disable them:

```properties
quarkus.stork.my-service.service-discovery.use-endpoint-slices=false
```

### Automatic detection (default behavior)

If `use-endpoint-slices` is not configured, Stork applies the following logic:

1. If the Kubernetes cluster exposes the `discovery.k8s.io/v1` API **and**
2. EndpointSlices exist for the target service

→ Stork uses EndpointSlices.

Otherwise, it falls back to classic Endpoints.

This ensures backward compatibility with older clusters and services while automatically benefiting from EndpointSlices when available.

## Service instance resolution with EndpointSlices

When using EndpointSlices, Stork does **not** resolve Pods.

Instead, service instances are derived directly from the EndpointSlice objects:

* `endpoint.addresses` → instance host
* `endpointSlice.ports` → instance port
* `endpointSlice.labels` → instance metadata

Each EndpointSlice represents a homogeneous group of endpoints.
All addresses in a slice share the same set of ports.
If different port combinations exist, Kubernetes exposes them as separate EndpointSlices.

This guarantees that all `(address, port)` combinations returned by Stork are valid.


## ClusterIP mode

By default, Stork resolves individual **pod IPs** from Kubernetes Endpoints (or EndpointSlices).
In ClusterIP mode, Stork resolves to the **ClusterIP and port** of the Kubernetes Service instead, without inspecting the backing pods.

The `use-cluster-ip` attribute enables this mode.

### When to use ClusterIP mode

ClusterIP mode is useful when:

* You have a **multi-port Service** and want Stork to resolve the port by name, without needing to know actual port numbers.
* You don't need client-side load balancing — for example, because `kube-proxy`, a service mesh, or another network-level mechanism already handles traffic distribution.
* You want to **control the resolution strategy through configuration**, without changing application code.

### Configuration

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=kubernetes
    stork.my-service.service-discovery.k8s-namespace=my-namespace
    stork.my-service.service-discovery.use-cluster-ip=true
    stork.my-service.service-discovery.port-name=http
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=kubernetes
    quarkus.stork.my-service.service-discovery.k8s-namespace=my-namespace
    quarkus.stork.my-service.service-discovery.use-cluster-ip=true
    quarkus.stork.my-service.service-discovery.port-name=http
    ```

In this mode, Stork queries the Kubernetes `Service` resource directly and returns a single `ServiceInstance` with:

* **Host** — the Service's ClusterIP (e.g., `10.96.0.15`)
* **Port** — resolved from the Service spec, matching `port-name` when multiple ports are defined

Since only one instance is returned, any configured Stork load balancer becomes a passthrough.

!!! note
    ClusterIP mode does not support headless Services (`clusterIP: None`). 
If the target Service is headless, Stork will fail with an error and fall back to the last known instances (or an empty list on first call).

### Multi-port Services

ClusterIP mode works well with multi-port Services.
Use the `port-name` attribute to select the desired port:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  ports:
    - name: http
      port: 8080
    - name: grpc
      port: 50051
```

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=kubernetes
    stork.my-service.service-discovery.use-cluster-ip=true
    stork.my-service.service-discovery.port-name=grpc
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=kubernetes
    quarkus.stork.my-service.service-discovery.use-cluster-ip=true
    quarkus.stork.my-service.service-discovery.port-name=grpc
    ```

This resolves to `10.96.x.x:50051` — the ClusterIP with the `grpc` port without the application needing to know the actual port number.
If `port-name` is not set, Stork uses the first port defined in the Service spec.

Supported attributes are the following:

--8<-- "target/attributes/META-INF/stork-docs/kubernetes-sd-attributes.txt"


## Caching the service instances

Contacting the cluster too much frequently can result in performance problems. It's why Kubernetes Service discovery extends `io.smallrye.stork.impl.CachingServiceDiscovery` to automatically _cache_ the service instances.
Moreover, the caching expiration has been also improved in order to only update the retrieved set of `ServiceInstance` if some of them changes and an event is emitted. 
This is done by creating an [Informer](https://www.javadoc.io/static/io.fabric8/kubernetes-client-api/6.1.1/io/fabric8/kubernetes/client/informers/SharedIndexInformer.html), similar to a [Watch](https://www.javadoc.io/static/io.fabric8/kubernetes-client-api/6.1.1/io/fabric8/kubernetes/client/Watch.html),  able to observe the events on the service instances resources. 

Note that: 
 - the cache is invalidated when an event is received. 
 - the cache is validated, if and only if, the instances are retrieved successfully from the cluster, in the `fetchNewServiceInstances` method.
 - In case of an error, the last successfully retrieved instances are returned, and a retry mechanism is triggered. 
The system will attempt to contact the cluster up to the number of times specified by request-retry-backoff-limit, waiting request-retry-backoff-interval milliseconds between each attempt.
By default, retries are disabled to prevent the system from entering an infinite loop of calls to an unresponsive cluster.
 - the `cache` method is overridden to customize the expiration strategy. In this case the collection of service instances will be kept until an event occurs.

When EndpointSlice are selected, Stork creates informers on the `EndpointSlice` resources instead of classic `Endpoints`. 
The caching behavior remains unchanged.

In ClusterIP mode, Stork creates an Informer on the Kubernetes `Service` resource (instead of Endpoints or EndpointSlices).
The cache is invalidated when the Service is added, updated, or deleted.
The same caching behavior described above applies.

