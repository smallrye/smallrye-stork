package io.smallrye.stork.api;

import java.util.Map;
import java.util.Optional;

/**
 * The central API of Stork
 */
public interface StorkServiceRegistry {

    /**
     * Retrieves the {@link Service} associated with the given name.
     *
     * @param serviceName the service name, must not be {@code null}
     * @return the service
     * @throws NoSuchServiceDefinitionException if there is no service associated with the given name.
     */
    Service getService(String serviceName);

    /**
     * Retrieves the {@link Service} associated with the given name.
     * Unlike {@link #getService(String)} this method returns an {@link Optional} and so does not throw a
     * {@link NoSuchServiceDefinitionException} if there is no {@link Service} associated with the given name.
     *
     * @param serviceName the service name, must not be {@code null}
     * @return an {@link Optional} containing the {@link Service} if any, empty otherwise.
     */
    Optional<Service> getServiceOptional(String serviceName);

    /**
     * Adds a service to the list of services managed by Stork.
     * The service is only added if there is no service with that name already defined.
     * Otherwise, the service definition is ignored.
     *
     * @param name the service name, must not be {@code null} or blank
     * @param definition the definition, must not be {@code null}
     * @return the Stork instance.
     *         // TODO Define exception.
     */
    StorkServiceRegistry defineIfAbsent(String name, ServiceDefinition definition);

    /**
     * @return all the services managed by Stork. The returned map is immutable.
     */
    Map<String, Service> getServices();

}
