package io.smallrye.stork.sample.vertx;

import java.util.List;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceDefinition;
import io.smallrye.stork.api.ServiceInstance;
import io.smallrye.stork.impl.RoundRobinConfiguration;
import io.smallrye.stork.servicediscovery.staticlist.StaticConfiguration;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.net.Address;
import io.vertx.core.net.AddressResolver;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.endpoint.LoadBalancer;
import io.vertx.core.net.endpoint.ServerEndpoint;
import io.vertx.core.net.endpoint.ServerSelector;
import io.vertx.core.spi.endpoint.EndpointBuilder;
import io.vertx.core.spi.endpoint.EndpointResolver;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.ext.web.client.WebClient;

public class Main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Create 2 HTTP server on port 8086 and 8087
        vertx.createHttpServer().requestHandler(req -> req.response().endAndForget("Hello from server 1")).listen(8086).await()
                .indefinitely();
        vertx.createHttpServer().requestHandler(req -> req.response().endAndForget("Hello from server 2")).listen(8087).await()
                .indefinitely();

        // Create a Stork instance with the static list discovery configured to use the HTTP servers
        Stork.initialize();
        var stork = Stork.getInstance();
        stork.defineIfAbsent("my-service", ServiceDefinition.of(
                new StaticConfiguration().withAddressList("localhost:8086,localhost:8087"),
                new RoundRobinConfiguration()));

        // Now let's create a WebClient to invoke our service
        // TODO We cannot use the Mutiny API, the address resolver are not "code-generated"
        var builder = vertx.getDelegate().httpClientBuilder();
        var bridge = new StorkServiceBridge<>(stork.getService("my-service"));
        builder.withAddressResolver(bridge);
        builder.withLoadBalancer(bridge);
        var bare_client = builder.build();

        var client = HttpClient.newInstance(bare_client);
        var web = WebClient.wrap(client);

        for (int i = 0; i < 4; i++) {
            var resp = web.get("/").send().await().indefinitely();
            System.out.println(resp.statusCode() + ": " + resp.body());
        }

        vertx.closeAndAwait();

    }

    record StorkServiceInstance(ServiceInstance instance, int index) {

    }

    private static class StorkServiceBridge<L> implements AddressResolver<SocketAddress>, LoadBalancer {
        private final Service service;
        private final EndpointResolver<SocketAddress, StorkServiceInstance, L, L> resolver;

        public StorkServiceBridge(Service service) {
            this.service = service;
            this.resolver = new EndpointResolver<>() {
                @Override
                public SocketAddress tryCast(Address address) {
                    if (address instanceof SocketAddress) {
                        return (SocketAddress) address;
                    } else {
                        return null;
                    }
                }

                @Override
                public SocketAddress addressOf(StorkServiceInstance server) {
                    // Build a SocketAddress from the ServiceInstance.
                    return SocketAddress.inetSocketAddress(server.instance.getPort(), server.instance.getHost());
                }

                private static <T> Future<T> toFuture(Uni<T> uni) {
                    Promise<T> promise = Promise.promise();
                    uni.subscribeAsCompletionStage().whenComplete(promise::complete);
                    return promise.future();
                }

                @Override
                public Future<L> resolve(SocketAddress address, EndpointBuilder<L, StorkServiceInstance> builder) {
                    return toFuture(service.getInstances())
                            .map(i -> {
                                if (i == null || i.isEmpty()) {
                                    return builder.build();
                                } else {
                                    var b = builder;
                                    int index = 0;
                                    for (ServiceInstance serviceInstance : i) {
                                        b = b.addServer(new StorkServiceInstance(serviceInstance, index));
                                        index++;
                                    }
                                    return b.build();
                                }
                            });
                }

                @Override
                public L endpoint(L state) {
                    return state;
                }

                @Override
                public boolean isValid(L state) {
                    return state != null;
                }

                @Override
                public void dispose(L data) {
                    // TODO we could keep a track ot the pending resolution and cancel it.
                }

                @Override
                public void close() {

                }

            };

        }

        @Override
        public EndpointResolver<SocketAddress, StorkServiceInstance, L, L> endpointResolver(io.vertx.core.Vertx vertx) {
            return resolver;
        }

        @Override
        public ServerSelector selector(List<? extends ServerEndpoint> listOfServers) {
            return () -> {
                var list = listOfServers.stream().map(i -> (StorkServiceInstance) i.unwrap())
                        .map(StorkServiceInstance::instance).toList();
                var selected = service.selectInstance(list);
                return list.indexOf(selected);
            };
        }
    }
}
