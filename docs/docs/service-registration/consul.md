# Consul Service Registration

[Consul](https://www.consul.io/) is a distributed, highly available, and data center aware solution to connect and configure applications across dynamic, distributed infrastructure.
It's often used as service discovery backend to register and locate the services composing your system.
Consul makes it simple for services to register themselves and to discover other services via a DNS or HTTP interface. 
External services can be registered as well.

This page explains how Stork can use Consul to handle the service registration.

## Dependency

First, you need to add the Stork Consul Service Registration provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-registration-consul</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Service registration configuration

For each service that should register the service instances in Consul, configure the service registrar `type`:

=== "stork standalone"
```properties
stork.my-service.service-registrar.type=consul
```

=== "stork in quarkus"
```properties
quarkus.stork.my-service.service-registrar.type=consul
```

Consul service registrar is configured with the following parameters:

--8<-- "target/attributes/META-INF/stork-docs/consul-sr-attributes.txt"

## Service deregistration configuration

There is no specific configuration required to enable deregistration; however, you must ensure that a consul service registrar is configured for the service:

=== "stork standalone"
```properties
stork.my-service.service-registrar.type=consul
```

=== "stork in quarkus"
```properties
quarkus.stork.my-service.service-registrar.type=consul
```

As with registration, deregistration relies on the service name.

## Secure communication with Consul

When your Consul cluster uses TLS/SSL encryption, you can configure Stork to establish secure connections. Enable SSL and provide the necessary trust store configuration:

=== "stork standalone"
    ```properties
    stork.my-service.service-registrar.type=consul
    stork.my-service.service-registrar.consul-host=localhost
    stork.my-service.service-registrar.consul-port=8501
    stork.my-service.service-registrar.ssl=true
    stork.my-service.service-registrar.trust-store-path=/path/to/truststore.jks
    stork.my-service.service-registrar.trust-store-password=changeit
    stork.my-service.service-registrar.verify-host=true
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-registrar.type=consul
    quarkus.stork.my-service.service-registrar.consul-host=localhost
    quarkus.stork.my-service.service-registrar.consul-port=8501
    quarkus.stork.my-service.service-registrar.ssl=true
    quarkus.stork.my-service.service-registrar.trust-store-path=/path/to/truststore.jks
    quarkus.stork.my-service.service-registrar.trust-store-password=changeit
    quarkus.stork.my-service.service-registrar.verify-host=true
    ```

For mutual TLS (mTLS) authentication where the client must also present a certificate, provide both trust store and key store configuration:

=== "stork standalone"
    ```properties
    stork.my-service.service-registrar.type=consul
    stork.my-service.service-registrar.consul-host=localhost
    stork.my-service.service-registrar.consul-port=8501
    stork.my-service.service-registrar.ssl=true
    stork.my-service.service-registrar.trust-store-path=/path/to/truststore.jks
    stork.my-service.service-registrar.trust-store-password=changeit
    stork.my-service.service-registrar.key-store-path=/path/to/keystore.jks
    stork.my-service.service-registrar.key-store-password=changeit
    stork.my-service.service-registrar.verify-host=true
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-registrar.type=consul
    quarkus.stork.my-service.service-registrar.consul-host=localhost
    quarkus.stork.my-service.service-registrar.consul-port=8501
    quarkus.stork.my-service.service-registrar.ssl=true
    quarkus.stork.my-service.service-registrar.trust-store-path=/path/to/truststore.jks
    quarkus.stork.my-service.service-registrar.trust-store-password=changeit
    quarkus.stork.my-service.service-registrar.key-store-path=/path/to/keystore.jks
    quarkus.stork.my-service.service-registrar.key-store-password=changeit
    quarkus.stork.my-service.service-registrar.verify-host=true
    ```

## ACL Token Authentication

When your Consul cluster has ACL (Access Control List) enabled, you need to provide an ACL token for authentication:

=== "stork standalone"
    ```properties
    stork.my-service.service-registrar.type=consul
    stork.my-service.service-registrar.consul-host=localhost
    stork.my-service.service-registrar.consul-port=8500
    stork.my-service.service-registrar.acl-token=your-acl-token
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-registrar.type=consul
    quarkus.stork.my-service.service-registrar.consul-host=localhost
    quarkus.stork.my-service.service-registrar.consul-port=8500
    quarkus.stork.my-service.service-registrar.acl-token=your-acl-token
    ```

The ACL token can be combined with SSL/TLS configuration for secure, authenticated access to Consul.
