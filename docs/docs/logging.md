# Logging Configuration

Stork uses [JBoss Logging](https://github.com/jboss-logging/jboss-logging) for all its logging needs. JBoss Logging is a logging facade that can delegate to various logging frameworks such as Log4j, SLF4J, or JDK logging.

## Logger Categories

Stork uses the following main logger categories:

### Core Loggers

- `io.smallrye.stork.Stork` - Main Stork class, logs service initialization and configuration
- `io.smallrye.stork.impl.CachingServiceDiscovery` - Service discovery caching operations
- `io.smallrye.stork.utils.StorkConfigUtils` - Configuration parsing and validation

### Service Discovery Loggers

- `io.smallrye.stork.servicediscovery.kubernetes.KubernetesServiceDiscovery` - Kubernetes service discovery events
- `io.smallrye.stork.servicediscovery.dns.DnsServiceDiscovery` - DNS-based service discovery
- `io.smallrye.stork.servicediscovery.knative.KnativeServiceDiscovery` - KNative service discovery events

### Load Balancer Loggers

- `io.smallrye.stork.loadbalancer.leastresponsetime.LeastResponseTimeLoadBalancer` - Least response time load balancer selection logic

### Service Registration Loggers

- `io.smallrye.stork.serviceregistration.consul.ConsulServiceRegistrar` - Consul service registration/deregistration
- `io.smallrye.stork.serviceregistration.consul.ConsulServiceRegistrarProvider` - Consul registrar provider initialization
- `io.smallrye.stork.serviceregistration.staticlist.StaticListServiceRegistrar` - Static list service registration
- `io.smallrye.stork.serviceregistration.staticlist.StaticListServiceRegistrarProvider` - Static list registrar provider
- `io.smallrye.stork.serviceregistration.eureka.EurekaServiceRegistrar` - Eureka service registration/deregistration

### Configuration Provider Loggers

- `io.smallrye.stork.microprofile.MicroProfileConfigProvider` - MicroProfile configuration loading

## Configuring Log Levels with Quarkus

In your `application.properties`:

```properties
# Set global Stork log level
quarkus.log.category."io.smallrye.stork".level=DEBUG

# Service discovery logging
quarkus.log.category."io.smallrye.stork.servicediscovery".level=DEBUG

# Specific implementations
quarkus.log.category."io.smallrye.stork.servicediscovery.kubernetes.KubernetesServiceDiscovery".level=TRACE
quarkus.log.category."io.smallrye.stork.servicediscovery.dns.DnsServiceDiscovery".level=DEBUG

# Load balancer logging
quarkus.log.category."io.smallrye.stork.loadbalancer".level=DEBUG

# Service registration logging
quarkus.log.category."io.smallrye.stork.serviceregistration".level=INFO
quarkus.log.category."io.smallrye.stork.serviceregistration.consul.ConsulServiceRegistrar".level=DEBUG

# Console output format
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
```

## Troubleshooting

### Too Many Logs

If you're getting too many debug logs in production:

1. Set the root Stork logger to INFO or WARN:
   ```properties
   quarkus.log.category."io.smallrye.stork".level=INFO
   ```

2. Selectively enable DEBUG only for components you're troubleshooting
