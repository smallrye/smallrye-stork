# DNS Service Discovery

DNS is a name resolution protocol used to determine IP addresses for hostnames.
That makes it a natural fit for service discovery.
Consul and AWS Cloud Map provide DNS resolutions for service discovery.

This page explains how Stork can use DNS to handle the service discovery.

## DNS records

DNS supports a [variety of record types](https://en.wikipedia.org/wiki/List_of_DNS_record_types). Stork can resolve hostnames to addresses based on [_SRV_](https://en.wikipedia.org/wiki/SRV_record), A and AAAA records. All these types of records may return multiple addresses for a single hostname. You may choose to not resolve target hostnames.

While _A_ and _AAAA_ records are quite similar, they just carry an IP (_v4_ for _A_ and _v6_ for _AAAA_), the _SRV_ records are different.
They contain a _weight_, a _target_ and a _port_ for a service instance.
The _target_ returned in an _SRV_ record needs to be resolved further by an _A_ or an _AAAA_ record.

In short, it works as follows:

![DNS service discovery](../target/srv_sequence.svg#only-light)
![DNS service discovery](../target/srv_sequence_dark.svg#only-dark)

## Dependency

To use the DNS service discovery, you need to add the Stork DNS Service Discovery provider dependency to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-dns</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

Next, set the service discovery `type` to `dns`.
Additionally, you would usually specify the DNS server, or servers, to use for the discovery. 
All in all, your configuration could look as follows:

=== "stork standalone"
    ```properties
    stork.my-service.service-discovery.type=dns
    # optional dns servers:
    stork.my-service.service-discovery.dns-servers=my-dns-server:8221,my-dns-server2
    ```

=== "stork in quarkus"
    ```properties
    quarkus.stork.my-service.service-discovery.type=dns

    # optional dns servers:
    quarkus.stork.my-service.service-discovery.dns-servers=my-dns-server:8221,my-dns-server2
    ```

All the available parameters are as follows:

--8<-- "../service-discovery/dns/target/classes/META-INF/stork-docs/dns-sd-attributes.txt"
