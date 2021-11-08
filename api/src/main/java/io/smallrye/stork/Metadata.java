package io.smallrye.stork;

import java.util.Collections;
import java.util.Map;

public class Metadata<T extends MetadataKey> {

    private Map<T, Object> metatada;

    public Metadata(Map<T, Object> metatada) {
        this.metatada = Collections.unmodifiableMap(metatada);

    }

    public Map<T, Object> getMetadata() {
        return metatada;
    }

}
