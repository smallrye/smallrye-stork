# Consul Service Discovery

[Consul](https://www.consul.io/) is a distributed, highly available, and data center aware solution to connect and configure applications across dynamic, distributed infrastructure.
It's often used as service discovery backend to register and locate the services composing your system.
Consul makes it simple for services to register themselves and to discover other services via a DNS or HTTP interface. 
External services can be registered as well.

As [specified](https://developer.hashicorp.com/consul/api-docs/agent/service#address) in the Consul documentation, if the host address is not provided, Stork will automatically use the Consul node address for the instance.

This page explains how Stork can use Consul to handle the service discovery and service registration.

## Dependency

First, you need to add the Stork Consul Service Discovery provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-consul</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

### Service discovery configuration

For each service that should get the service instances from Consul, configure the service discovery `type`:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=consul
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=consul
    ```

## Secure communication with Consul

Stork supports SSL/TLS encryption when connecting to a Consul server. This is useful when Consul is configured with HTTPS or when you need to authenticate using client certificates.

To enable SSL/TLS, set the `ssl` attribute to `true` and configure the trust store and optionally the key store:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=consul
    stork.my-service.service-discovery.consul-host=consul.example.com
    stork.my-service.service-discovery.consul-port=8501
    stork.my-service.service-discovery.ssl=true
    stork.my-service.service-discovery.trust-store-path=/path/to/truststore.jks
    stork.my-service.service-discovery.trust-store-password=changeit
    stork.my-service.service-discovery.verify-host=true
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=consul
    quarkus.stork.my-service.service-discovery.consul-host=consul.example.com
    quarkus.stork.my-service.service-discovery.consul-port=8501
    quarkus.stork.my-service.service-discovery.ssl=true
    quarkus.stork.my-service.service-discovery.trust-store-path=/path/to/truststore.jks
    quarkus.stork.my-service.service-discovery.trust-store-password=changeit
    quarkus.stork.my-service.service-discovery.verify-host=true
    ```

For mutual TLS (mTLS) authentication, also configure the key store containing the client certificate:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.key-store-path=/path/to/keystore.jks
    stork.my-service.service-discovery.key-store-password=changeit
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.key-store-path=/path/to/keystore.jks
    quarkus.stork.my-service.service-discovery.key-store-password=changeit
    ```

## ACL Token Authentication

If your Consul cluster has ACL enabled, you can provide an ACL token for authentication:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.acl-token=your-consul-acl-token
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.acl-token=your-consul-acl-token
    ```

Consul service discovery is configured with the following parameters:

--8<-- "target/attributes/META-INF/stork-docs/consul-sd-attributes.txt"

