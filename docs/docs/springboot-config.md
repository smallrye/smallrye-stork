# Stork Spring Boot Config 

Stork seamlessly supports Spring Boot configuration, facilitating access to configuration properties.
This documentation elaborates on how Spring Boot developers can use Stork in their Spring Boot applications and configure it using the application.properties file.


## Dependency setup

To enable Spring Boot configuration integration in Stork, you need to include the following dependency:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-spring-boot-config</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Initializing Stork

Since Spring Boot lacks a pre-configured Stork instance, you'll need create one. It can be done by providing a Spring bean performing Stork initialization:

```java linenums="1"
{{ insert('examples/SpringBootInitializationExample.java') }}
```
Upon initialization, Stork scans for the `io.smallrye.stork.springboot.SpringBootConfigProvider` SPI provider and CDI beans (from version 2.x onwards). 
It then builds a comprehensive list of managed services by parsing the properties configuration files.


Please note the importance of the `io.smallrye.stork.springboot.SpringBootApplicationContextProvider` bean in our setup. 
This bean has a critical role by granting Stork access to the current `org.springframework.context.ApplicationContext`.
It enables it to retrieve configuration details effectively. Consequently, it's imperative that this bean is instantiated prior to initiating the Stork initialization process.
In this case, we utilize the `@DependsOn` annotation for that.
It allows us controlling the bean creation order.

## Comprehensive Example

You can check our [Guitar Hero Application](https://github.com/aureamunoz/spring-stork-guitar-hero/) showcasing the seamless integration of Stork with Spring Boot Configuration. 
