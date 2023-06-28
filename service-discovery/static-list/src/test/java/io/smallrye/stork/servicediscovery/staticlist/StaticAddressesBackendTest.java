package io.smallrye.stork.servicediscovery.staticlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.stork.servicediscovery.staticlist.StaticListServiceRegistrar.StaticAddressesBackend;

public class StaticAddressesBackendTest {

    @BeforeEach
    public void clearAddresses() {
        StaticAddressesBackend.clearAll();
    }

    @Test
    public void shouldNotAddAddressAlreadyPresent() {
        StaticAddressesBackend.add("my-service", "localhost:8080");
        StaticAddressesBackend.add("my-service", "localhost:8080");

        List<String> addresses = StaticAddressesBackend.getAddresses("my-service");

        assertThat(addresses).isNotEmpty();
        assertThat(addresses.size()).isEqualTo(1);
        assertThat(addresses).contains("localhost:8080");

    }

    @Test
    public void shouldAddAFewAddress() {
        StaticAddressesBackend.add("my-service", "localhost:8080");
        StaticAddressesBackend.add("my-service", "localhost:8081");

        List<String> addresses = StaticAddressesBackend.getAddresses("my-service");

        assertThat(addresses).isNotEmpty();
        assertThat(addresses.size()).isEqualTo(2);
        assertThat(addresses).containsExactlyInAnyOrder("localhost:8080", "localhost:8081");

        StaticAddressesBackend.clear("my-service");
        addresses = StaticAddressesBackend.getAddresses("my-service");
        assertThat(addresses).isNull();
    }

    @Test
    public void shouldAddAFewAddressThenClearList() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            StaticAddressesBackend.add("", "localhost:8080");
        });

        String expectedMessage = "No service name provided for address localhost:8080";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));

    }

}
