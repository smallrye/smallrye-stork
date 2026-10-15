package examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import io.smallrye.stork.Stork;
import io.smallrye.stork.springboot.SpringBootApplicationContextProvider;

@SpringBootApplication
public class SpringBootInitializationExample {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootInitializationExample.class, args);
    }

    @Bean
    @DependsOn("springBootApplicationContextProvider")
    public Stork stork() {
        Stork.initialize();
        Stork stork = Stork.getInstance();
        return stork;
    }

    @Bean
    public SpringBootApplicationContextProvider springBootApplicationContextProvider() {
        return new SpringBootApplicationContextProvider();
    }
}
