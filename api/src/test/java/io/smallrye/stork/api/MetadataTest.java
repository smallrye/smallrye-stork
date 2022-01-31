package io.smallrye.stork.api;

import static io.smallrye.stork.api.Metadata.DefaultMetadataKey.GENERIC_METADATA_KEY;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MetadataTest {

    @Test
    void testEmpty() {
        Metadata<?> metadata = Metadata.empty();
        Assertions.assertNotNull(metadata);
        Assertions.assertNull(metadata.getMetadata().get(GENERIC_METADATA_KEY));
        Assertions.assertNotNull(GENERIC_METADATA_KEY.getName());
    }

    @Test
    void testOf() {
        Metadata<MyMetadataKey> metadata = Metadata.of(MyMetadataKey.class);
        Assertions.assertNotNull(metadata);
        metadata = metadata.with(MyMetadataKey.FOO, "hey!");
        Assertions.assertNotNull(metadata);
        Assertions.assertEquals("hey!", metadata.getMetadata().get(MyMetadataKey.FOO));
    }

    @Test
    void testOfWithMap() {
        Metadata<MyMetadataKey> metadata = Metadata.of(MyMetadataKey.class,
                Map.of(MyMetadataKey.FOO, "Hey!", MyMetadataKey.BAR, 23));
        Assertions.assertNotNull(metadata);
        Assertions.assertEquals("Hey!", metadata.getMetadata().get(MyMetadataKey.FOO));
        Assertions.assertEquals(23, metadata.getMetadata().get(MyMetadataKey.BAR));
    }

    @Test
    void testOfWithEmptyMap() {
        Metadata<MyMetadataKey> metadata = Metadata.of(MyMetadataKey.class, Collections.emptyMap());
        Assertions.assertNotNull(metadata);
        Assertions.assertNull(metadata.getMetadata().get(MyMetadataKey.FOO));
        Assertions.assertNull(metadata.getMetadata().get(MyMetadataKey.BAR));
    }

    @Test
    void shouldFailOnInvalidInputs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Metadata.of(MyMetadataKey.class, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Metadata.of(null));

        Metadata<MyMetadataKey> metadata = Metadata.of(MyMetadataKey.class);
        Assertions.assertThrows(IllegalArgumentException.class, () -> metadata.with(MyMetadataKey.FOO, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> metadata.with(null, "hello"));
    }

    public enum MyMetadataKey implements MetadataKey {
        FOO,
        BAR;

        @Override
        public String getName() {
            return "foo";
        }
    }

}
