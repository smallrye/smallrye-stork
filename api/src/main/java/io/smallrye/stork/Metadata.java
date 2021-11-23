package io.smallrye.stork;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Metadata<T extends Enum<T>> {

    private EnumMap<T, Object> metatada;
    Class<T> enumType;

    public Metadata(Class<T> key) {
        this.metatada = new EnumMap<T, Object>(key);
        this.enumType = key;
    }

    public Metadata() {

    }

    public Map<T, Object> getMetadata() {
        return metatada;
    }

    public Optional<Object> get(T key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be `null`");
        }
        return Optional.ofNullable(metatada.get(key));
    }

    public void put(T key, Object o) {
        metatada.put(key, o);
    }

    public Set<T> keySet() {
        return Set.of(enumType.getEnumConstants());
    }

}
