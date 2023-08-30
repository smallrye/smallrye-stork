# Stork Observability API

Stork proposes an observability API that automatically observes some parameters to show how the Stork service discovery and selection are behaving.

For any _observation_ to happen, you need to provide your own implementation of an `ObservationCollector.` By default, Stork provides a no-op implementation.

The `ObservationCollector` is responsible for instantiating the `StorkObservation`.

The `StorkObservation` reacts to Stork events thanks to a `StorkEventHandler`. 

You can extend the metrics collection by extending the `StorkEventHandler` interface.

The following sequence diagram shows how the observability is initialized : 


![observability initialization](target/observability_sequence.svg#only-light)
![observability initialization](target/observability_sequence_dark.svg#only-dark)



The `StorkObservation` registers times, number of discovered instances, the selected instance and failures by reacting to the lifecycle of a Stork event such as:

- start : Observation has been started. 
The beginning time is registered. 
It happens when the `ObservationCollector#create()` method gets called.
- service discovery success: a collection of instances has been successfully discovered for a service. 
The end discovery time and number of instances are recorded. 
It happens when the `StorkObservation#onServiceDiscoverySuccess` gets called.
- service discovery error: an error occurs when discovering a service. 
The end discovery time and failure cause are captured. 
It happens when the `StorkObservation#onServiceDiscoveryFailure` gets called.
- service selection success: an instance has been successfully selected from the collection.
The end selection time and selected instance ID are registered.
It happens when the `StorkObservation#onServiceSelectionSuccess` gets called.
- service selection error: an error occurred during selecting the instance. 
End selection time and failure cause are registered. 
It happens when the `StorkObservation#onServiceSelectionFailure` gets called.
- end: Observation has finished. Overall duration is registered. 
It happens when the `StorkObservation#onServiceSelectionSuccess` gets called.

The following sequence diagram represents the described observation process above:


  ![observation_process](target/observation_sequence.svg#only-light)
  ![observation_process](target/observation_sequence_dark.svg#only-dark)



## Implementing an observation collector

An `ObservationCollector` implementation must override the `create` method to provide an instance of StorkObservation.
In addition, the user can access and enrich the observation data through the `StorkEventHandler`.

A custom observation collector class should look as follows:

```java linenums="1"
{{ insert('examples/AcmeObservationCollector.java') }}
```

The next step is to initialize Stork with an `ObservableStorkInfrastructure`, taking an instance of your `ObservationCollector` as parameter.

```java linenums="1"
{{ insert('examples/ObservableInitializationExample.java') }}
```

Then, Stork uses your implementation to register metrics.


## Observing service discovery and selection behaviours

To access metrics registered by `StorkObservation`, use the following code:

```java linenums="1"
{{ insert('examples/ObservationExample.java') }}
```

# Stork Observability with Quarkus

Stork metrics are automatically enabled when using Stork together with the Micrometer extension in a Quarkus application. 

Micrometer collects the metrics of the rest and grpc client using Stork, as well as when using the Stork API.

As an example, if you export the metrics to Prometheus, you will get:

````text
# HELP stork_load_balancer_failures_total The number of failures during service selection.
# TYPE stork_load_balancer_failures_total counter
stork_load_balancer_failures_total{service_name="hello-service",} 0.0
# HELP stork_service_selection_duration_seconds The duration of the selection operation 
# TYPE stork_service_selection_duration_seconds summary
stork_service_selection_duration_seconds_count{service_name="hello-service",} 13.0
stork_service_selection_duration_seconds_sum{service_name="hello-service",} 0.001049291
# HELP stork_service_selection_duration_seconds_max The duration of the selection operation 
# TYPE stork_service_selection_duration_seconds_max gauge
stork_service_selection_duration_seconds_max{service_name="hello-service",} 0.0
# HELP stork_overall_duration_seconds_max The total duration of the Stork service discovery and selection operations
# TYPE stork_overall_duration_seconds_max gauge
stork_overall_duration_seconds_max{service_name="hello-service",} 0.0
# HELP stork_overall_duration_seconds The total duration of the Stork service discovery and selection operations
# TYPE stork_overall_duration_seconds summary
stork_overall_duration_seconds_count{service_name="hello-service",} 13.0
stork_overall_duration_seconds_sum{service_name="hello-service",} 0.001049291
# HELP stork_service_discovery_failures_total The number of failures during service discovery
# TYPE stork_service_discovery_failures_total counter
stork_service_discovery_failures_total{service_name="hello-service",} 0.0
# HELP stork_service_discovery_duration_seconds_max The duration of the discovery operation
# TYPE stork_service_discovery_duration_seconds_max gauge
stork_service_discovery_duration_seconds_max{service_name="hello-service",} 0.0
# HELP stork_service_discovery_duration_seconds The duration of the discovery operation
# TYPE stork_service_discovery_duration_seconds summary
stork_service_discovery_duration_seconds_count{service_name="hello-service",} 13.0
stork_service_discovery_duration_seconds_sum{service_name="hello-service",} 6.585046209
# HELP stork_instances_count_total The number of service instances discovered
# TYPE stork_instances_count_total counter
stork_instances_count_total{service_name="hello-service",} 26.0
````






