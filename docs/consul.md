# Consul Service Discovery

[Consul](https://www.consul.io/) is a distributed, highly available, and data center aware solution to connect and configure applications across dynamic, distributed infrastructure.
It's often used as service discovery backend to register and locate the services composing your system.
Consul makes it simple for services to register themselves and to discover other services via a DNS or HTTP interface. 
External services can be registered as well.

This page explains how Stork can use Consul to handle the service discovery.

## Dependency

First, you need to add the Stork Consul Service Discovery provider:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>smallrye-stork-service-discovery-consul</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service expected to be registered in Consul, configure the lookup:

```properties
stork.my-service.service-discovery=consul
stork.my-service.service-discovery.consul-host=localhost
stork.my-service.service-discovery.consul-port=8500
```

Stork looks for the service with the given name (`my-service` in the previous example). 

Supported attributes are the following:

| Attribute            | Mandatory  | Default Value  | Description                  |
|----------------------|------------|----------------|------------------------------|
| `consul-host`        | No         | `localhost`    | The Consul host              |
| `consul-port`        | No         | `8500`         |  The Consul port             |
| `use-health-checks`  | No         | `false`        | Whether to use health check  |