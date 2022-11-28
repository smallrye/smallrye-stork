# Knative Service Discovery



## Dependency

First, you need to add the Stork Kubernetes Service Discovery provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-knative</artifactId>
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
    resources: ["endpoints", "pods"] # stork queries service endpoints and pods
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
    stork.my-service.service-discovery.type=knative
    stork.my-service.service-discovery.knative-namespace=my-namespace
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=knative
    quarkus.stork.my-service.service-discovery.knative-namespace=my-namespace
    ```


Stork looks for the _Knative Service_ with the given name (`my-service` in the previous example) in the specified namespace.

Instead of using the _Knative Service_ IP directly, and let Kubernetes handle the selection and balancing, Stork inspects the service and retrieves the list of _pods_ providing the service.
Then, it can select the instance.

Supported attributes are the following:

--8<-- "../service-discovery/knative/target/classes/META-INF/stork-docs/knative-sd-attributes.txt"


## Caching the service instances

Contacting the cluster too much frequently can result in performance problems. It's why Kubernetes Service discovery extends `io.smallrye.stork.impl.CachingServiceDiscovery` to automatically _cache_ the service instances.
Moreover, the caching expiration has been also improved in order to only update the retrieved set of `ServiceInstance` if some of them changes and an event is emitted. 
This is done by creating an [Informer](https://www.javadoc.io/static/io.fabric8/kubernetes-client-api/6.1.1/io/fabric8/kubernetes/client/informers/SharedIndexInformer.html), similar to a [Watch](https://www.javadoc.io/static/io.fabric8/kubernetes-client-api/6.1.1/io/fabric8/kubernetes/client/Watch.html),  able to observe the events on the service instances resources. 

--8<-- "../src/main/java/io/smallrye/stork/servicediscovery/knative/KnativeServiceDiscovery.java"

Note that: 
 - the cache is invalidated when an event is received. 
 - the cache is validated once the instances are retrieved from the cluster, in the `fetchNewServiceInstances` method.
 - the `cache` method is overrided to customize the expiration strategy. In this case the collection of service instances will be kept until an event occurs.


