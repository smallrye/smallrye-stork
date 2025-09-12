package io.smallrye.stork.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.ServiceInstance;
import io.vertx.core.Vertx;

class CachingServiceDiscoveryRefreshTest {

    private static final Vertx vertx = Vertx.vertx();

    @RepeatedTest(4)
    @Timeout(20)
    void shouldTriggerSupplierOnceAndCompleteAllConsumersWhenDone() throws InterruptedException {
        int amountOfGets = 300;

        List<List<ServiceInstance>> results = Collections.synchronizedList(new ArrayList<>());

        AtomicInteger refreshCount = new AtomicInteger(0);
        CountDownLatch awaitFinish = new CountDownLatch(amountOfGets);

        ExecutorService executor = Executors.newFixedThreadPool(amountOfGets);

        CachingServiceDiscovery discovery = new CachingServiceDiscovery("1M") {
            @Override
            public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
                refreshCount.incrementAndGet();
                return Uni.createFrom().emitter(e -> {
                    List<ServiceInstance> results = asList(
                            new DefaultServiceInstance(1, "localhost", 8406, Optional.empty(), false),
                            new DefaultServiceInstance(2, "localhost", 8407, Optional.empty(), false));
                    vertx.setTimer(2000, ignored -> e.complete(results));
                });
            }
        };
        for (int i = 0; i < amountOfGets; i++) {
            executor.execute(() -> {
                discovery.getServiceInstances()
                        .subscribe().with(instances -> {
                            results.add(instances);
                            awaitFinish.countDown();
                        });
            });

        }
        assertThat(awaitFinish.await(10, TimeUnit.SECONDS)).isTrue();

        await().atMost(5, TimeUnit.SECONDS)
                .until(refreshCount::get, Matchers.equalTo(1));

        assertThat(results).hasSize(amountOfGets);

        List<ServiceInstance> originalList = null;
        for (List<ServiceInstance> servicesList : results) {
            assertThat(servicesList).hasSize(2);

            // verify it's the exact same list that is returned:
            if (originalList == null) {
                originalList = servicesList;
            }

            assertThat(servicesList == originalList).isTrue();
        }
        executor.shutdown();

    }

    @Test
    @Timeout(10)
    void shouldInvalidateCacheAndTriggerRefreshAgain() throws InterruptedException {
        AtomicInteger refreshCount = new AtomicInteger(0);

        CachingServiceDiscovery discovery = new CachingServiceDiscovery("1M") {
            @Override
            public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
                refreshCount.incrementAndGet();
                return Uni.createFrom().item(List.of(
                        new DefaultServiceInstance(1, "localhost", 8406, Optional.empty(), false),
                        new DefaultServiceInstance(1, "localhost", 8407, Optional.empty(), false)));
            }
        };

        AtomicReference<List<ServiceInstance>> instances = new AtomicReference<>();

        discovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        // first call --> should call fetchNewInstances
        assertThat(instances.get()).hasSize(2);
        assertThat(refreshCount.get()).isEqualTo(1);

        instances.set(null);

        // Second call, no explicit validation happen --> should NOT call fetchNewInstances
        discovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(2);
        assertThat(refreshCount.get()).isEqualTo(1);

        instances.set(null);

        // Explicit invalidation
        discovery.invalidate();

        // New call --> should NOT call fetchNewInstances
        discovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances", th))
                .subscribe().with(instances::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> instances.get() != null);

        assertThat(instances.get()).hasSize(2);
        assertThat(refreshCount.get()).isEqualTo(2);
    }

    @RepeatedTest(4)
    @Timeout(20)
    void shouldCacheAndShareResultAcrossConsumers() throws InterruptedException {
        int amountOfGets = 300;

        List<List<ServiceInstance>> results = Collections.synchronizedList(new ArrayList<>());

        AtomicInteger refreshCount = new AtomicInteger(0);
        CountDownLatch awaitFinish = new CountDownLatch(amountOfGets);

        ExecutorService executor = Executors.newFixedThreadPool(amountOfGets);

        CachingServiceDiscovery discovery = new CachingServiceDiscovery("1M") {
            @Override
            public Uni<List<ServiceInstance>> fetchNewServiceInstances(List<ServiceInstance> previousInstances) {
                refreshCount.incrementAndGet();
                return Uni.createFrom().emitter(e -> {
                    List<ServiceInstance> results = asList(
                            new DefaultServiceInstance(1, "localhost", 8406, Optional.empty(), false),
                            new DefaultServiceInstance(2, "localhost", 8407, Optional.empty(), false));
                    vertx.setTimer(2000, ignored -> e.complete(results));
                });
            }
        };
        for (int i = 0; i < amountOfGets; i++) {
            executor.execute(() -> {
                discovery.getServiceInstances()
                        .subscribe().with(instances -> {
                            results.add(instances);
                            awaitFinish.countDown();
                        });
            });

        }
        assertThat(awaitFinish.await(10, TimeUnit.SECONDS)).isTrue();

        await().atMost(5, TimeUnit.SECONDS)
                .until(refreshCount::get, Matchers.equalTo(1));

        assertThat(results).hasSize(amountOfGets);

        List<ServiceInstance> beforeList = null;
        for (List<ServiceInstance> servicesList : results) {
            assertThat(servicesList).hasSize(2);

            // verify it's the exact same list that is returned:
            if (beforeList == null) {
                beforeList = servicesList;
            }

            assertThat(servicesList == beforeList).isTrue();
        }

        discovery.invalidate();

        AtomicReference<List<ServiceInstance>> afterList = new AtomicReference<>();

        discovery.getServiceInstances()
                .onFailure().invoke(th -> fail("Failed to get service instances", th))
                .subscribe().with(afterList::set);

        await().atMost(Duration.ofSeconds(5))
                .until(() -> afterList.get() != null);

        assertThat(afterList.get()).hasSize(2);
        assertThat(afterList.get()).isNotSameAs(beforeList);

        executor.shutdown();

    }

}
