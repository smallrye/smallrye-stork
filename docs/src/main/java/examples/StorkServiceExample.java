package examples;

import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;

public class StorkServiceExample {

    public static void main(String[] args) {
        Stork.initialize();
        Stork stork = Stork.getInstance();

        Service service = stork.getService("my-service");

        // ...
        Stork.shutdown();
    }

}
