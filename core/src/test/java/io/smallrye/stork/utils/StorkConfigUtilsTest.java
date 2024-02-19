package io.smallrye.stork.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class StorkConfigUtilsTest {

    @Mock
    private Logger log;

    @Test
    void computeServiceProperty_ValidProperty() {
        // Given
        Map<String, Map<String, String>> propertiesByServiceName = new HashMap<>();
        String propertyName = "stork.\"service-name\".load-balancer.property";
        String propertyValue = "value";

        // When
        StorkConfigUtils.computeServiceProperty(propertiesByServiceName, propertyName, propertyValue);

        // Then
        assertTrue(propertiesByServiceName.containsKey("service-name"));
        Map<String, String> serviceProperties = propertiesByServiceName.get("service-name");
        assertEquals("value", serviceProperties.get("load-balancer.property"));
    }

    @Test
    void computeServiceProperty_AnotherValidProperty() {
        // Given
        Map<String, Map<String, String>> propertiesByServiceName = new HashMap<>();
        String propertyName = "stork.service-name.load-balancer.property";
        String propertyValue = "value";

        // When
        StorkConfigUtils.computeServiceProperty(propertiesByServiceName, propertyName, propertyValue);

        // Then
        assertTrue(propertiesByServiceName.containsKey("service-name"));
        Map<String, String> serviceProperties = propertiesByServiceName.get("service-name");
        assertEquals("value", serviceProperties.get("load-balancer.property"));
    }

    @Test
    void computeServiceProperty_FewValidProperties() {
        // Given
        Map<String, Map<String, String>> propertiesByServiceName = new HashMap<>();
        String propertyName = "stork.service-name.load-balancer.property";
        String propertyValue = "value";

        // When
        StorkConfigUtils.computeServiceProperty(propertiesByServiceName, propertyName, propertyValue);

        String propertyName2 = "stork.service-name.load-balancer.property2";
        String propertyValue2 = "value2";

        StorkConfigUtils.computeServiceProperty(propertiesByServiceName, propertyName2, propertyValue2);

        // Then
        assertTrue(propertiesByServiceName.containsKey("service-name"));
        Map<String, String> serviceProperties = propertiesByServiceName.get("service-name");
        assertThat(serviceProperties).containsEntry("load-balancer.property", "value")
                .containsEntry("load-balancer.property2", "value2");
    }

    @Test
    void computeServiceProperty_InvalidProperty() {
        // Given
        Map<String, Map<String, String>> propertiesByServiceName = new HashMap<>();
        String propertyName = "invalid.property";
        String propertyValue = "value";

        // When
        StorkConfigUtils.computeServiceProperty(propertiesByServiceName, propertyName, propertyValue);

        // Then
        assertFalse(propertiesByServiceName.containsKey("service-name"));
    }

}
