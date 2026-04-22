package io.smallrye.stork.servicediscovery.kubernetes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointAddressBuilder;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.EndpointSubsetBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.ObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.smallrye.common.constraint.Assert;
import io.smallrye.stork.api.ServiceInstance;

public class KubernetesTestUtils {

    KubernetesClient client;

    public KubernetesTestUtils(KubernetesClient client) {
        this.client = client;
    }

    public Endpoints registerKubernetesResources(String serviceName, String namespace, String... ips) {
        Assert.checkNotNullParam("ips", ips);
        Endpoints endpoints = buildAndRegisterKubernetesService(serviceName, namespace, true, ips);
        Arrays.stream(ips).forEach(ip -> buildAndRegisterBackendPod(serviceName, namespace, true, ip));
        return endpoints;
    }

    public static Map<String, Long> mapHostnameToIds(List<ServiceInstance> serviceInstances) {
        Map<String, Long> result = new HashMap<>();
        for (ServiceInstance serviceInstance : serviceInstances) {
            result.put(serviceInstance.getHost(), serviceInstance.getId());
        }
        return result;
    }

    public Endpoints buildAndRegisterKubernetesService(String applicationName, String namespace, boolean register,
            String... ipAdresses) {
        EndpointPort[] ports = new EndpointPort[] { new EndpointPortBuilder().withPort(8080).withProtocol("TCP").build() };
        return buildAndRegisterKubernetesService(applicationName, namespace, register, ports, ipAdresses);
    }

    public Endpoints buildAndRegisterKubernetesService(String applicationName, String namespace, boolean register,
            EndpointPort[] ports, String... ipAdresses) {

        Map<String, String> serviceLabels = new HashMap<>();
        serviceLabels.put("app.kubernetes.io/name", applicationName);
        serviceLabels.put("app.kubernetes.io/version", "1.0");

        List<EndpointAddress> endpointAddresses = Arrays.stream(ipAdresses)
                .map(ipAddress -> {
                    ObjectReference targetRef = new ObjectReference(null, null, "Pod",
                            applicationName + "-" + ipAsSuffix(ipAddress), namespace, null, UUID.randomUUID().toString());
                    EndpointAddress endpointAddress = new EndpointAddressBuilder().withIp(ipAddress).withTargetRef(targetRef)
                            .build();
                    return endpointAddress;
                }).collect(Collectors.toList());
        Endpoints endpoint = new EndpointsBuilder()
                .withNewMetadata().withName(applicationName).withLabels(serviceLabels).endMetadata()
                .addToSubsets(new EndpointSubsetBuilder().withAddresses(endpointAddresses)
                        .addToPorts(ports)
                        .build())
                .build();

        if (register) {
            if (namespace != null) {
                client.endpoints().inNamespace(namespace).resource(endpoint).create();
            } else {
                client.endpoints().resource(endpoint).create();
            }
        }
        return endpoint;

    }

    public void buildAndRegisterBackendPod(String name, String namespace, boolean register, String ip) {
        Pod backendPod = new PodBuilder().withNewMetadata()
                .withName(name + "-" + ipAsSuffix(ip))
                .addToLabels("app.kubernetes.io/name", name)
                .addToLabels("app.kubernetes.io/version", "1.0")
                .addToLabels("ui", "ui-" + ipAsSuffix(ip))
                .withNamespace(namespace)
                .endMetadata()
                .build();
        if (register) {
            if (namespace != null) {
                client.pods().inNamespace(namespace).resource(backendPod).create();
            } else {
                client.pods().resource(backendPod).create();
            }
        }
    }

    public static String ipAsSuffix(String ipAddress) {
        return ipAddress.replace(".", "");
    }

    public Service registerKubernetesServiceResource(String serviceName, String namespace,
            String clusterIp, ServicePort... ports) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/name", serviceName);
        labels.put("app.kubernetes.io/version", "1.0");

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceName)
                .withNamespace(namespace)
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withClusterIP(clusterIp)
                .withPorts(Arrays.asList(ports))
                .endSpec()
                .build();

        if (namespace != null) {
            client.services().inNamespace(namespace).resource(service).create();
        } else {
            client.services().resource(service).create();
        }
        return service;
    }

    public static ServicePort servicePort(String name, int port, String protocol) {
        return new ServicePortBuilder()
                .withName(name)
                .withPort(port)
                .withProtocol(protocol)
                .build();
    }
}
