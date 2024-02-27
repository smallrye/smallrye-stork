# Stork MicroProfile Config 

Stork integrates with MicroProfile Configuration out of the box, enabling seamless access to configuration properties. 
This documentation explains how Stork can retrieve configuration details from the MicroProfile Config file present in the classpath.
Quarkus uses this approach for reading configuration details from the MicroProfile Config file located within the classpath.


## Dependency setup

To enable MicroProfile Config integration in Stork, you need to include the following dependency:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>smallrye-stork-microprofile</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Initializing Stork

If your framework lacks a pre-configured Stork instance, you'll need to perform initialization:

```java linenums="1"
{{ insert('examples/InitializationExample.java') }}
```
Upon initialization, Stork scans for the `io.smallrye.stork.config.MicroProfileConfigProvider` SPI provider and CDI beans (from version 2.x onwards). It then builds a comprehensive list of managed services by parsing the properties configuration files.

