package io.smallrye.stork;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Service Instance metadata.
 * <p>
 * This class stores service instance metadata that can be used by the load balancer to select the service instance to use.
 * <p>
 * Instances of this class are <strong>immutable</strong>. Modification operations return new instances.
 * <p>
 * You can creates new instances using the {@link #of(Class)}, {@link #of(Class, Map)} and {@link #with(Enum, Object)}methods.
 * <p>
 */
public class Metadata<T extends Enum<T>> {

    private final EnumMap<T, Object> metatada;
    private final Class<T> clazz;
    private static final Metadata EMPTY = new Metadata(DefaultMetadataKey.class, Collections.emptyMap());

    private Metadata(Class<T> key, Map<T, Object> metatada) {
        if (metatada.isEmpty()) {
            this.metatada = new EnumMap<T, Object>(key);
            this.clazz = key;
        } else {
            this.metatada = new EnumMap<T, Object>(metatada);
            this.clazz = key;
        }
    }

    /**
     * Returns an empty set of metadata.
     *
     * @return the empty instance
     */
    public static Metadata empty() {
        return EMPTY;
    }

    /**
     * Returns an instance of {@link Metadata} containing metadata values.
     *
     * @param metadata the metadata, must not be {@code null}, must not contain {@code null},
     *        must not contain multiple objects of the same class
     * @return the new metadata
     */
    public static Metadata of(Class<?> key, Map<?, Object> metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("`metadata` must not be `null`");
        }
        return new Metadata(key, metadata);
    }

    /**
     * Returns an instance of {@link Metadata} containing an empty set of values.
     *
     * @param key the type of metadata, must not be {@code null}
     * @return the new metadata
     */
    public static Metadata of(Class<?> key) {
        if (key == null) {
            throw new IllegalArgumentException("`key` must not be `null`");
        }
        return new Metadata(key, Collections.emptyMap());
    }

    /**
     * Creates a new instance of {@link Metadata} with the current entries, plus {@code item}.
     * If the current set of metadata contains already an instance of the class of {@code item}, the value is replaced
     * in the returned {@link Metadata}.
     *
     * @param item the metadata to be added, must not be {@code null}.
     * @return the new instance of {@link Metadata}
     */
    public Metadata with(T key, Object item) {
        if (key == null) {
            throw new IllegalArgumentException("`key` must not be `null`");
        }
        if (item == null) {
            throw new IllegalArgumentException(key.name() + " should not be `null`");
        }
        EnumMap<T, Object> copy = new EnumMap<>(metatada);
        copy.put(key, item);
        return new Metadata(this.clazz, copy);
    }

    public EnumMap<T, Object> getMetadata() {
        return metatada;
    }

    public enum DefaultMetadataKey implements MetadataKey {
        GENERIC_METADATA_KEY("generic");

        private String name;

        DefaultMetadataKey(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

}
