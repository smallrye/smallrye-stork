package io.smallrye.stork.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verify the default implementation for {@link ServiceInstance}.
 */
class ServiceInstanceTest {

    ServiceInstance instance = new ServiceInstance() {
        @Override
        public long getId() {
            return 0;
        }

        @Override
        public String getHost() {
            return null;
        }

        @Override
        public int getPort() {
            return 0;
        }

        @Override
        public boolean isSecure() {
            return false;
        }
    };

    @Test
    void defaultMetadataShouldBeEmpty() {
        Metadata<?> metadata = instance.getMetadata();
        Assertions.assertNotNull(metadata);
        Assertions.assertNull(metadata.getMetadata().get("foo"));
    }

    @Test
    void defaultLabelsShouldBeEmpty() {
        Map<String, String> labels = instance.getLabels();
        Assertions.assertNotNull(labels);
        Assertions.assertTrue(labels.isEmpty());
    }

    @Test
    void defaultStatisticsAreDisabled() {
        Assertions.assertFalse(instance.gatherStatistics());
        Assertions.assertDoesNotThrow(() -> instance.recordStart(true));
        Assertions.assertDoesNotThrow(() -> instance.recordReply());
        Assertions.assertDoesNotThrow(() -> instance.recordEnd(null));
        Assertions.assertDoesNotThrow(() -> instance.recordStart(false));
        Assertions.assertDoesNotThrow(() -> instance.recordEnd(new Exception("boom")));
    }

}
