package io.smallrye.stork.api;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;

import io.smallrye.mutiny.Uni;

/**
 * Represents a <em>Service</em>.
 * <p>
 * This container gives you access to the service instances and to the selected service instance.
 */
public class Service {

    private final Semaphore instanceSelectionLock;
    private final LoadBalancer loadBalancer;
    private final ServiceDiscovery serviceDiscovery;
    private final ServiceRegistrar serviceRegistrar;
    private final String serviceName;

    /**
     * Creates a new Service.
     *
     * @param serviceName the name, must not be {@code null}, must not be blank
     * @param loadBalancer the load balancer, can be {@code null}
     * @param serviceDiscovery the service discovery, must not be {@code null}
     * @param serviceRegistrar the service registrar, can be {@code null}
     * @param requiresStrictRecording whether strict recording must be enabled
     */
    public Service(String serviceName, LoadBalancer loadBalancer, ServiceDiscovery serviceDiscovery,
            ServiceRegistrar serviceRegistrar, boolean requiresStrictRecording) {
        this.loadBalancer = loadBalancer;
        this.serviceDiscovery = serviceDiscovery;
        this.serviceRegistrar = serviceRegistrar;
        this.serviceName = serviceName;
        this.instanceSelectionLock = requiresStrictRecording ? new Semaphore(1) : null;
    }

    /**
     * Selects a service instance.
     * <p>
     * The selection looks for the service instances and select the one to use using the load balancer.
     *
     * <b>Note:</b> this method doesn't record a start of an operation using this load balancer and does not
     * synchronize load balancer invocations even if the load balancer is not thread safe
     *
     * @return a Uni with a ServiceInstance, or with {@link NoServiceInstanceFoundException} if the load balancer failed to find
     *         a service instance capable of handling a call
     */
    public Uni<ServiceInstance> selectInstance() {
        return serviceDiscovery.getServiceInstances()
                .map(this::selectInstance);
    }

    /**
     * Using the underlying load balancer, select a service instance from the collection of service instances.
     *
     * <b>Note:</b> this method doesn't record a start of an operation using this load balancer and does not
     * synchronize load balancer invocations even if the load balancer is not thread safe
     *
     * @param instances collection of instances
     * @return a ServiceInstance, or with {@link NoServiceInstanceFoundException} if the load balancer failed to find
     *         a service instance capable of handling a call
     * @throws NoServiceInstanceFoundException if all the instances are nto
     */
    public ServiceInstance selectInstance(Collection<ServiceInstance> instances) {
        return loadBalancer.selectServiceInstance(instances);
    }

    /**
     * Selects a service instance and records a start of an operation using the instance
     * <p>
     * The selection looks for the service instances and select the one to use using the load balancer.
     *
     * @param measureTime whether the operation for which the operation is chosen records time (will call
     *        {@link ServiceInstance#recordReply()})
     * @return a Uni with a ServiceInstance, or with {@link NoServiceInstanceFoundException} if the load balancer failed to find
     *         a service instance capable of handling a call
     * @see LoadBalancer#requiresStrictRecording()
     */
    public Uni<ServiceInstance> selectInstanceAndRecordStart(boolean measureTime) {
        return serviceDiscovery.getServiceInstances()
                .map(list -> selectInstanceAndRecordStart(list, measureTime));
    }

    /**
     * Select a ServiceInstance for this service from a collection and record a start of an operation using the instance.
     * <p>
     * Access to the underlying LoadBalancer method is serialized.
     *
     * @param instances instances to choose from
     * @param measureTime whether the operation for which the operation is chosen records time (will call
     *        {@link ServiceInstance#recordReply()})
     * @return the selected instance
     * @see #selectInstanceAndRecordStart(boolean)
     * @see LoadBalancer#requiresStrictRecording()
     */
    public ServiceInstance selectInstanceAndRecordStart(Collection<ServiceInstance> instances, boolean measureTime) {
        if (instanceSelectionLock == null) {
            ServiceInstance result = loadBalancer.selectServiceInstance(instances);
            if (result.gatherStatistics()) {
                result.recordStart(measureTime);
            }
            return result;
        } else {
            try {
                instanceSelectionLock.acquire();
                try {
                    ServiceInstance result = loadBalancer.selectServiceInstance(instances);
                    if (result.gatherStatistics()) {
                        result.recordStart(measureTime);
                    }
                    return result;
                } finally {
                    instanceSelectionLock.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to lock for ServiceInstance selection", e);
            }
        }
    }

    /**
     * Provide a collection of available {@link ServiceInstance}s
     *
     * @return a Uni producing the list of {@link ServiceInstance ServiceInstances}.
     */
    public Uni<List<ServiceInstance>> getInstances() {
        return serviceDiscovery.getServiceInstances();
    }

    /**
     * Get the underlying load balancer instance.
     *
     * @return load balancer
     */
    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    /**
     * Get the underlying service discovery
     *
     * @return service discovery
     */
    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    /**
     * Get the underlying service registrar
     *
     * @return service registrar
     */
    public ServiceRegistrar getServiceRegistrar() {
        return serviceRegistrar;
    }

    /**
     * @return the service name.
     */
    public String getServiceName() {
        return serviceName;
    }
}
