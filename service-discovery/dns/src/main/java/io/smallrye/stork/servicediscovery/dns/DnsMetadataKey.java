package io.smallrye.stork.servicediscovery.dns;

import io.smallrye.stork.api.MetadataKey;

/**
 * The DNS metadata keys.
 */
public enum DnsMetadataKey implements MetadataKey {

    /**
     * DNS hostname.
     */
    DNS_NAME("dns-name"),

    /**
     * In case of SRV queries, weight for the target. Otherwise, 1.
     */
    DNS_WEIGHT("dns-weight");

    private final String name;

    /**
     * Creates a new DnsMetadataKey
     *
     * @param name the name
     */
    DnsMetadataKey(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
