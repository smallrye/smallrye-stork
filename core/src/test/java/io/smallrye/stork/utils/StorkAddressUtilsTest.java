package io.smallrye.stork.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class StorkAddressUtilsTest {

    @Test
    void shouldParseIpv6WithoutPort() {
        String ipv6WithoutPort = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(ipv6WithoutPort, 8080,
                "serviceUsingDefaultPort");
        assertThat(result.host).isEqualTo(ipv6WithoutPort);
        assertThat(result.port).isEqualTo(8080);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseIpv6WithSquareBracketsWithoutPort() {
        String ipv6WithoutPort = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        String toParse = String.format("[%s]", ipv6WithoutPort);
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(toParse, 8080,
                "serviceWithSquareBracketsUsingDefaultPort");
        assertThat(result.host).isEqualTo(ipv6WithoutPort);
        assertThat(result.port).isEqualTo(8080);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseIpv6WithPort() {
        int port = 1382;
        String ipv6WithoutPort = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        String toParse = String.format("[%s]:%d", ipv6WithoutPort, port);

        HostAndPort result = StorkAddressUtils.parseToHostAndPort(toParse, 8080, "serviceUsingSpecifiedPort");
        assertThat(result.host).isEqualTo(ipv6WithoutPort);
        assertThat(result.port).isEqualTo(1382);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseIpV4WithoutPort() {
        String ipV4WithoutPort = "127.0.0.1";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(ipV4WithoutPort, 8383, "serviceUsingIPv4AdressWithoutPort");
        assertThat(result.host).isEqualTo(ipV4WithoutPort);
        assertThat(result.port).isEqualTo(8383);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseHostAndPort() {
        HostAndPort hostAndPort = new HostAndPort("localhost", 8080);
        String hostAndPortString = StorkAddressUtils.parseToString(hostAndPort);
        assertThat(hostAndPortString).isEqualTo("localhost:8080");

    }

    @Test
    void shouldParseHostPortAndPath() {
        HostAndPort hostAndPort = new HostAndPort("localhost", 8080, "hello");
        String hostAndPortString = StorkAddressUtils.parseToString(hostAndPort);
        assertThat(hostAndPortString).isEqualTo("localhost:8080/hello");

    }

    @Test
    void shouldParseIpV4WithoutPortButWithScheme() {
        String ipV4WithoutPort = "127.0.0.1";

        String ipV4WithoutPortWithScheme = "http://" + ipV4WithoutPort;
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(ipV4WithoutPortWithScheme, 8383,
                "serviceUsingIPv4AdressWithoutPortButScheme");
        assertThat(result.host).isEqualTo(ipV4WithoutPort);
        assertThat(result.port).isEqualTo(8383);
        assertThat(result.path).isEmpty();

        String ipV4WithoutPortWithSecureScheme = "https://" + ipV4WithoutPort;
        result = StorkAddressUtils.parseToHostAndPort(ipV4WithoutPortWithSecureScheme, 8383,
                "serviceUsingIPv4AdressWithoutPortButScheme");
        assertThat(result.host).isEqualTo(ipV4WithoutPort);
        assertThat(result.port).isEqualTo(8383);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseIpV4WithPort() {
        String address = "127.0.0.1";
        String addressWithPort = address + ":8787";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPort, 8383, "serviceUsingIPv4AddressWithPort");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8787);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseIpV4WithPortAndScheme() {
        String address = "127.0.0.1";
        String addressWithPortAndScheme = "http://" + address + ":8787";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPortAndScheme, 8383,
                "serviceUsingIPv4AddressWithPortAndSheme");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8787);
        assertThat(result.path).isEmpty();

        String addressWithPortAndSecureScheme = "https://" + address + ":8787";
        result = StorkAddressUtils.parseToHostAndPort(addressWithPortAndSecureScheme, 8383,
                "serviceUsingIPv4AddressWithPortAndSheme");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8787);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseIpV4WithPortAndPath() {
        String address = "127.0.0.1";
        String addressWithPortAndPath = address + ":8787/test";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPortAndPath, 8383,
                "serviceUsingIPv4AddressWithPort");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8787);
        assertThat(result.path).contains("/test");

    }

    @Test
    void shouldParseIpV4WithPortAndPathAndScheme() {
        String address = "127.0.0.1";
        String addressWithPortAndPathAndScheme = "http://" + address + ":8787/test";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPortAndPathAndScheme, 8383,
                "serviceUsingIPv4AddressWithPortAndPathAndScheme");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8787);
        assertThat(result.path).contains("/test");

        String addressWithPortAndPathAndSecureScheme = "https://" + address + ":8787/test";
        result = StorkAddressUtils.parseToHostAndPort(addressWithPortAndPathAndSecureScheme, 8383,
                "serviceUsingIPv4AddressWithPortAndPathAndScheme");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8787);
        assertThat(result.path).contains("/test");

    }

    @Test
    void shouldParseHostnameWithoutPort() {
        String address = "my-host.com.pl";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(address, 8383, "serviceUsingHostnameWithoutPort");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8383);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseHostnameWithoutPortButScheme() {
        String address = "my-host.com.pl";
        String addressWithScheme = "http://" + address;
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithScheme, 8383,
                "serviceUsingHostnameWithoutPortButScheme");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8383);
        assertThat(result.path).isEmpty();

        String addressWithSecureScheme = "https://" + address;
        result = StorkAddressUtils.parseToHostAndPort(addressWithSecureScheme, 8383,
                "serviceUsingHostnameWithoutPortButScheme");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8383);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseHostnameWithoutPortButWithPath() {
        String address = "my-host.com.pl/foo/bar";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(address, 8383, "serviceUsingHostnameWithoutPortButWithPath");
        assertThat(result.host).isEqualTo("my-host.com.pl");
        assertThat(result.port).isEqualTo(8383);
        assertThat(result.path).contains("/foo/bar");
    }

    @Test
    void shouldParseHostnameWithoutPortButWithPathAndSchema() {
        String address = "my-host.com.pl/foo/bar";
        String addressWithScheme = "http://" + address;
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithScheme, 8383,
                "serviceUsingHostnameWithoutPortButWithPathAndSchema");
        assertThat(result.host).isEqualTo("my-host.com.pl");
        assertThat(result.port).isEqualTo(8383);
        assertThat(result.path).contains("/foo/bar");

        String addressWithSecureScheme = "https://" + address;
        result = StorkAddressUtils.parseToHostAndPort(addressWithSecureScheme, 8383,
                "serviceUsingHostnameWithoutPortButWithPathAndSchema");
        assertThat(result.host).isEqualTo("my-host.com.pl");
        assertThat(result.port).isEqualTo(8383);
        assertThat(result.path).contains("/foo/bar");
    }

    @Test
    void shouldParseHostnameWithPort() {
        String address = "my-host.com.pl";
        String addressWithPort = address + ":8989";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPort, 8383, "serviceUsingHostnameWithPort");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8989);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseHostnameWithPortAndScheme() {
        String address = "my-host.com.pl";
        String addressWithPortAndScheme = "http://" + address + ":8989";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPortAndScheme, 8383,
                "serviceUsingHostnameWithPortAndScheme");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8989);
        assertThat(result.path).isEmpty();

        String addressWithPortAndSecureScheme = "https://" + address + ":8989";
        result = StorkAddressUtils.parseToHostAndPort(addressWithPortAndSecureScheme, 8383,
                "serviceUsingHostnameWithPortAndScheme");
        assertThat(result.host).isEqualTo(address);
        assertThat(result.port).isEqualTo(8989);
        assertThat(result.path).isEmpty();
    }

    @Test
    void shouldParseHostnameWithPortWithPath() {
        String addressWithPort = "my-host.com.pl:8989/test/123";
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPort, 8383, "serviceUsingHostnameWithPortAndPath");
        assertThat(result.host).isEqualTo("my-host.com.pl");
        assertThat(result.port).isEqualTo(8989);
        assertThat(result.path).contains("/test/123");
    }

    @Test
    void shouldParseHostnameWithPortWithPathAndScheme() {
        String addressWithPort = "my-host.com.pl:8989/test/123";
        String addressWithPortAndScheme = "http://" + addressWithPort;
        HostAndPort result = StorkAddressUtils.parseToHostAndPort(addressWithPortAndScheme, 8383,
                "serviceUsingHostnameWithPortAndPathAndScheme");
        assertThat(result.host).isEqualTo("my-host.com.pl");
        assertThat(result.port).isEqualTo(8989);
        assertThat(result.path).contains("/test/123");

        String addressWithPortAndSecureScheme = "https://" + addressWithPort;
        result = StorkAddressUtils.parseToHostAndPort(addressWithPortAndSecureScheme, 8383,
                "serviceUsingHostnameWithPortAndPathAndScheme");
        assertThat(result.host).isEqualTo("my-host.com.pl");
        assertThat(result.port).isEqualTo(8989);
        assertThat(result.path).contains("/test/123");
    }

}
