package io.smallrye.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.stork.utils.HostAndPort;
import io.smallrye.stork.utils.StorkAddressUtils;

public class StorkAddressUtilsTest {

    @Test
    void shouldParseIpv6WithoutPort() {
        String ipv6WithoutPort = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(ipv6WithoutPort, 8080,
                "serviceUsingDefaultPort");
        assertThat(result.host).isEqualTo(ipv6WithoutPort);
        assertThat(result.port).isEqualTo(8080);
    }

    @Test
    void shouldParseIpv6WithSquareBracketsWithoutPort() {
        String ipv6WithoutPort = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        String toParse = String.format("[%s]", ipv6WithoutPort);
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(toParse, 8080,
                "serviceWithSquareBracketsUsingDefaultPort");
        assertThat(result.host).isEqualTo(ipv6WithoutPort);
        assertThat(result.port).isEqualTo(8080);
    }

    @Test
    void shouldParseIpv6WithPort() {
        int port = 1382;
        String ipv6WithoutPort = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        String toParse = String.format("[%s]:%d", ipv6WithoutPort, port);

        HostAndPort result = StorkAddressUtils.parseToHostAndPort(toParse, 8080, "serviceUsingSpecifiedPort");
        assertThat(result.host).isEqualTo(ipv6WithoutPort);
        assertThat(result.port).isEqualTo(1382);
    }

    @Test
    void shouldParseIpV4WithoutPort() {
        String ipV4WithoutPort = "127.0.0.1";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(ipV4WithoutPort, 8383, "serviceUsingIPv4AdressWithoutPort");
        assertThat(result.host).isEqualTo(ipV4WithoutPort);
        assertThat(result.port).isEqualTo(8383);
    }

    @Test
    void shouldParseIpV4WithPort() {
        String address = "127.0.0.1";
        String addressWithPort = address + ":8787";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPort, 8383, "serviceUsingIPv4AdressWithPort");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8787);
    }

    @Test
    void shouldParseHostnameWithoutPort() {
        String address = "my-host.com.pl";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(address, 8383, "serviceUsingHostnameWithoutPort");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8383);
    }

    @Test
    void shoyuldParseHostnameWithPort() {
        String address = "my-host.com.pl";
        String addressWithPort = address + ":8989";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPort, 8383, "serviceUsingHostnameWithPort");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8989);
    }

}
