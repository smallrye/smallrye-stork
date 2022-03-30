package io.smallrye.stork.servicediscovery.eureka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Structure representing a Eureka Application instance.
 * Instances are created from the JSON responses returned by the Eureka service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationInstance {

    /**
     * The instance id.
     */
    public String instanceId;
    /**
     * The host name.
     */
    public String hostName;
    /**
     * The application name.
     */
    public String app;
    /**
     * The IP Address.
     */
    public String ipAddr;
    /**
     * The service status.
     */
    public Status status;
    /**
     * The overridden service status, replacing the actual status.
     */
    public Status overriddenStatus;
    /**
     * The port
     */
    public Port port;
    /**
     * The secure port, for secure services.
     */
    public Port securePort;
    /**
     * The country id.
     */
    public int countryId;
    /**
     * The virtual IP address.
     */
    public String vipAddress;
    /**
     * The secure virtual IP address, for service services.
     */
    public String secureVipAddress;
    /**
     * The service home page.
     */
    public String homePageUrl;
    /**
     * The service status page.
     */
    public String statusPageUrl;
    /**
     * The health check url.
     */
    public String healthCheckUrl;

    /**
     * @return {@code true} if the service should be considered as running.
     */
    public boolean isUp() {
        return overriddenStatus == Status.UP || status == Status.UP && overriddenStatus == Status.UNKNOWN;
    }

    /**
     * Structure representing a port.
     */
    public static class Port {
        /**
         * The port.
         */
        @JsonProperty("$")
        public int port;

        /**
         * Whether this port is enabled.
         */
        @JsonProperty("@enabled")
        public boolean enabled;
    }

    /**
     * The status (health) of a service
     */
    public enum Status {
        /**
         * The service is up and running.
         */
        UP,
        /**
         * The service is down.
         */
        DOWN,
        /**
         * The service is under maintenance.
         */
        OUT_OF_SERVICE,
        /**
         * The service is in an unknown state.
         */
        UNKNOWN,
        /**
         * The service is running, but not yet ready.
         */
        STARTING
    }
}
