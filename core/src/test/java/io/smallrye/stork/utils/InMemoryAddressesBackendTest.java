package io.smallrye.stork.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InMemoryAddressesBackendTest {

    @BeforeEach
    public void clearAddresses() {
        InMemoryAddressesBackend.clearAll();
    }

    @Test
    public void shouldNotAddAddressAlreadyPresent() {
        InMemoryAddressesBackend.add("my-service", "localhost:8080");
        InMemoryAddressesBackend.add("my-service", "localhost:8080");

        List<String> addresses = InMemoryAddressesBackend.getAddresses("my-service");

        assertThat(addresses).isNotEmpty();
        assertThat(addresses.size()).isEqualTo(1);
        assertThat(addresses).contains("localhost:8080");

    }

    @Test
    public void shouldAddAFewAddress() {
        InMemoryAddressesBackend.add("my-service", "localhost:8080");
        InMemoryAddressesBackend.add("my-service", "localhost:8081");

        List<String> addresses = InMemoryAddressesBackend.getAddresses("my-service");

        assertThat(addresses).isNotEmpty();
        assertThat(addresses.size()).isEqualTo(2);
        assertThat(addresses).containsExactlyInAnyOrder("localhost:8080", "localhost:8081");

        InMemoryAddressesBackend.clear("my-service");
        addresses = InMemoryAddressesBackend.getAddresses("my-service");
        assertThat(addresses).isNull();
    }

    @Test
    public void shouldAddAFewAddressThenClearList() {

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            InMemoryAddressesBackend.add("", "localhost:8080");
        });

        String expectedMessage = "No service name provided for address localhost:8080";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));

    }

}
