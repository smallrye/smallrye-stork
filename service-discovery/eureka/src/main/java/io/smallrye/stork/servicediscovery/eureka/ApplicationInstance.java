package io.smallrye.stork.servicediscovery.eureka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationInstance {

    public String instanceId;
    public String hostName;
    public String app;
    public String ipAddr;
    public Status status;
    public Status overriddenStatus;
    public Port port;
    public Port securePort;
    public int countryId;
    public String vipAddress;
    public String secureVipAddress;
    public String homePageUrl;
    public String statusPageUrl;
    public String healthCheckUrl;

    public boolean isUp() {
        return overriddenStatus == Status.UP || status == Status.UP && overriddenStatus == Status.UNKNOWN;
    }

    public static class Port {
        @JsonProperty("$")
        public int port;

        @JsonProperty("@enabled")
        public boolean enabled;
    }

    public enum Status {
        UP,
        DOWN,
        OUT_OF_SERVICE,
        UNKNOWN,
        STARTING
    }
}
