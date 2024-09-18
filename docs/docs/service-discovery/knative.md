# Knative Service Discovery

Knative is a Kubernetes-based platform for serverless workloads. Knative provides a set of objects as Kubernetes Custom Resource Definitions (CRDs). 
These resources are used to define and control how your serverless workload behaves on the cluster. 
The Stork Knative service discovery implementation is very similar to the Kubernetes one. 
Stork will ask for [Knative services](https://knative.dev/docs/serving/reference/serving-api/#serving.knative.dev/v1.Service) to the cluster instead of vanilla [Kubernetes services](https://kubernetes.io/docs/concepts/services-networking/service/#service-resource) used by the Kubernetes implementation. 
To do so, Stork uses [Fabric 8 Knative Client](https://github.com/fabric8io/kubernetes-client/blob/master/extensions/knative/client/src/main/java/io/fabric8/knative/client/KnativeClient.java) which is just an extension of Fabric8 Kubernetes Client. 


## Dependency

First, you need to add the Stork Knative Service Discovery provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-knative</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

#### A few words about server authentication. 
Stork uses Fabric8 Knative Client which is a [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client#readme) extension to access the Kubernetes resources, concretely the `DefaultKnativeClient` implementation.

Since Knative Client is just an extension of Fabric8 Kubernetes Client, it’s also possible to get an instance of KnativeClient from KubernetesClient.

`DefaultKubernetesClient` will try to read the `~/.kube/config` file from your local machine and load the token for authenticating with the Kubernetes API server.

The level of access (Roles) depends on the configured `ServiceAccount`.

You can override this configuration if you want fine-grain control.


##### Role-based access control (RBAC)
If you're using a Kubernetes cluster with Role-Based Access Control (RBAC) enabled, the default permissions for a ServiceAccount don't allow it to list or modify any resources.
A `ServiceAccount`, a `Role` and a `RoleBinding` are needed in order to allow Stork to list the available service instances from the cluster or the namespace.

An example that allows listing all endpoints could look something like this:

```yaml
---
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
    stork.my-knservice.service-discovery.type=knative
    stork.my-knservice.service-discovery.knative-namespace=my-namespace
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-knservice.service-discovery.type=knative
    quarkus.stork.my-knservice.service-discovery.knative-namespace=my-namespace
    ```


Stork looks for the _Knative Service_ with the given name (`my-knservice` in the previous example) in the specified namespace.

Stork inspects the _Knative Service_ and retrieves the url of the service.

Supported attributes are the following:

--8<-- "target/attributes/META-INF/stork-docs/knative-sd-attributes.txt"


## Caching the service instances

Contacting the cluster too much frequently can result in performance problems. It's why Knative Service discovery extends `io.smallrye.stork.impl.CachingServiceDiscovery` to automatically _cache_ the service instances.
Moreover, the caching expiration has been also improved in order to only update the retrieved set of `ServiceInstance` if some of them changes and an event is emitted. 
This is done by creating an [Informer](https://www.javadoc.io/static/io.fabric8/kubernetes-client-api/6.1.1/io/fabric8/kubernetes/client/informers/SharedIndexInformer.html), similar to a [Watch](https://www.javadoc.io/static/io.fabric8/kubernetes-client-api/6.1.1/io/fabric8/kubernetes/client/Watch.html),  able to observe the events on the Knative Service instances resources. 

Note that: 
 - the cache is invalidated when an event is received. 
 - the cache is validated once the instances are retrieved from the cluster, in the `fetchNewServiceInstances` method.
 - the `cache` method is overrided to customize the expiration strategy. In this case the collection of service instances will be kept until an event occurs.


