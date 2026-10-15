package examples;

import io.smallrye.stork.Stork;

public class InitializationExample {

    public static void main(String[] args) {
        Stork.initialize();
        Stork stork = Stork.getInstance();
        // ...
    }
}
