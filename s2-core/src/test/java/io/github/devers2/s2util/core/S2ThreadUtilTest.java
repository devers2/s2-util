package io.github.devers2.s2util.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * S2ThreadUtil.newExecutor 의 동시 실행 수 제한 시험. 가상 스레드(Java 21+)와 플랫폼 풀(Java 17) 모두
 * 양수 maxThreads 를 넘지 않아야 한다.
 */
class S2ThreadUtilTest {

    @Test
    void positiveMaxThreadsLimitsConcurrency() throws Exception {
        ExecutorService executor = S2ThreadUtil.newExecutor(3);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            final int n = i;
            tasks.add(() -> {
                peak.accumulateAndGet(active.incrementAndGet(), Math::max);
                Thread.sleep(10);
                active.decrementAndGet();
                return n;
            });
        }
        try {
            List<Future<Integer>> results = executor.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) {
                assertEquals(i, results.get(i).get());
            }
        } finally {
            executor.shutdown();
        }
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(peak.get() <= 3, "peak=" + peak.get());
        assertTrue(peak.get() >= 2, "tasks did not run concurrently, peak=" + peak.get());
    }

    @Test
    void nonPositiveMaxThreadsIsUnboundedOnVirtualThreads() throws Exception {
        assumeTrue(S2ThreadUtil.isVirtualSupported());
        int tasks = 200;
        ExecutorService executor = S2ThreadUtil.newExecutor(0);
        CountDownLatch allRunning = new CountDownLatch(tasks);
        CountDownLatch release = new CountDownLatch(1);
        try {
            for (int i = 0; i < tasks; i++) {
                executor.execute(() -> {
                    allRunning.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            assertTrue(allRunning.await(5, TimeUnit.SECONDS), "not all tasks ran at once");
        } finally {
            release.countDown();
            executor.shutdown();
        }
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void shutdownNowStopsTasksWaitingForPermit() throws Exception {
        ExecutorService executor = S2ThreadUtil.newExecutor(1);
        CountDownLatch firstStarted = new CountDownLatch(1);
        AtomicBoolean secondRan = new AtomicBoolean();
        executor.execute(() -> {
            firstStarted.countDown();
            try {
                new CountDownLatch(1).await(); // 인터럽트될 때까지 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
        executor.execute(() -> secondRan.set(true));
        Thread.sleep(50); // 두 번째 작업이 허가를 기다리는 상태가 되도록

        executor.shutdownNow();

        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(executor.isShutdown() && executor.isTerminated());
        assertFalse(secondRan.get());
    }
}
