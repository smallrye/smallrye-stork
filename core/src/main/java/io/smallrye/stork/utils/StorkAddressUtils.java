package io.smallrye.stork.utils;

import static java.lang.String.format;

public final class StorkAddressUtils {

    public static HostAndPort parseToHostAndPort(String serviceAddress,
            int defaultPort,
            String serviceName) {
        if (serviceAddress == null || serviceAddress.isBlank()) {
            throw new IllegalArgumentException("Blank or null address: '" + serviceAddress + "'");
        }
        if (serviceAddress.charAt(0) == '[') {
            return parseIpV6AddressWithSquareBracket(serviceAddress, defaultPort, serviceName);
        } else if (countColons(serviceAddress) > 1) {
            return new HostAndPort(serviceAddress, defaultPort);
        } else {
            return parseNonIpv6Adress(serviceAddress, defaultPort, serviceName);
        }
    }

    private static HostAndPort parseNonIpv6Adress(String serviceAddress, int defaultPort, String serviceName) {
        String[] hostAndMaybePort = serviceAddress.split(":");

        switch (hostAndMaybePort.length) {
            case 1:
                return new HostAndPort(serviceAddress, defaultPort);
            case 2:
                try {
                    int port = Integer.parseInt(hostAndMaybePort[1]);
                    return new HostAndPort(hostAndMaybePort[0], port);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(format("Invalid port '%s' in address '%s' for service '%s'",
                            hostAndMaybePort[1], serviceAddress, serviceName), nfe);
                }
            default:
                throw new IllegalStateException(format("Unable to parse address '%s' to host and port for service '%s'",
                        serviceAddress, serviceName));
        }
    }

    private static HostAndPort parseIpV6AddressWithSquareBracket(String serviceAddress, int defaultPort, String serviceName) {
        StringBuilder host = new StringBuilder();
        // ipv6 address, either [some:add:ress]:port or [some:add:ress]
        int i = 1;
        boolean done = false;
        for (; i < serviceAddress.length(); i++) {
            if (serviceAddress.charAt(i) == ']') {
                done = true;
                break;
            } else {
                host.append(serviceAddress.charAt(i));
            }
        }

        if (!done) {
            throw new IllegalArgumentException(
                    format("IPv6 Address with a square bracket '[' does not have a matching closing square bracket ']' " +
                            "in address '%s' for service: '%s'", serviceAddress, serviceName));
        }

        if (++i == serviceAddress.length()) {
            return new HostAndPort(host.toString(), defaultPort);
        } else if (serviceAddress.charAt(i) != ':') {
            throw new IllegalArgumentException(
                    format("Unexpected character '%c' at character %d in address '%s' for service: '%s'",
                            serviceAddress.charAt(i), i, serviceAddress, serviceName));
        } else {
            int port = 0;
            i++; // current character was a colon that separated address and port
            for (; i < serviceAddress.length(); i++) {
                char c = serviceAddress.charAt(i);
                if (isDigit(c)) {
                    port = 10 * port + (c - '0');
                } else {
                    throw new IllegalArgumentException(
                            format("Unexpected character '%c' while parsing port number in " +
                                    "address '%s' for service '%s', at character %d, expected a digit", c, serviceName,
                                    serviceAddress, i));
                }
            }
            return new HostAndPort(host.toString(), port);
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static int countColons(String string) {
        int count = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == ':') {
                count++;
            }
        }
        return count;
    }
}
