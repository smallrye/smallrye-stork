# Least Response Time Load Balancing

The `least-response-time` load balancing strategy collects response times of the calls made with service instances and picks an instance based on this information.

Erroneous responses are treated as responses with a long response time, by default 60 seconds. This can be controlled with the `error-penalty` attribute.

The algorithm for service instance selection is as follows:

- if there is a service instance that wasn't used before - use it, otherwise:
- if there are any service instances with collected response times - select the one for which *score* is the lowest, otherwise:
- select a random instance

The *score* for an instance decreases in time if an instance is not used. This way we ensure that instances that haven't been used in a long time, are retried.

For the details on the *score* calculation, see [Score calculation](#score-calculation)

## Dependency

To use this load balancer, start with adding the least-response-time load-balancer dependency to your project:

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-load-balancer-least-response-time</artifactId>
    <version>{{version.current}}</version>
</dependency>
```

## Configuration

For each service expected to use a least-response-time selection, configure the `load-balancer` to be `least-response-time`:

```properties
stork.my-service.service-discovery=...
stork.my-service.service-discovery...=...
stork.my-service.load-balancer=least-response-time
```

The following attributes are supported:

--8<-- "load-balancer/response-time/target/classes/META-INF/stork-docs/least-response-time-lb-attributes.txt"

## Score calculation

The *score* of a service instance is calculated by dividing a weighted sum of response times by sum of the weighs. The result is additionally adjusted to account for instances that haven't been used for a long time.

Let:

- $n$ denote how many instance selections were made so far
- $t_i$ denote the response time for call $i$
- $n_i$ denote the number of instance selections done until the moment of recording the response time for call $i$
- $n_{max}$ denote the number of instance selections at the moment of last call recorded with this instance
- $\delta$ denote a configurable `declining-factor`

The idea for the weight is to decrease the importance of the data collected long time (many calls) ago. For call $i$, the weight is calculated as follows:
$$
w_i = \delta ^ {(n - n_i)}
$$

The score of a service instance is calculated as:
$$
score(n) = \delta^{n - n_{max}} * \frac{\sum_i t_i * w_i}{\sum_i w_i} =
\delta^{n - n_{max}} * \frac{\sum_i t_i * \delta^{n - n_i}}{\sum_i \delta^{n - n_i}}
$$

The `declining-factor` should be in $(0, 1]$ , the default is $0.9$. Using a lower value makes the older response times less important.