package io.github.devers2.s2util.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        // 첫 작업이 인터럽트로 끝나며 허가를 반납하는 순간과 대기 작업의 인터럽트가 경쟁하므로 여러 번 반복해 확인한다
        for (int round = 0; round < 50; round++) {
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
            Thread.sleep(5); // 두 번째 작업이 허가를 기다리는 상태가 되도록

            executor.shutdownNow();

            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(executor.isShutdown() && executor.isTerminated());
            assertFalse(secondRan.get(), "task waiting for a permit ran after shutdownNow (round " + round + ")");
        }
    }

    @Test
    void pendingLimitBlocksSubmitterUntilTasksFinish() throws Exception {
        ExecutorService executor = S2ThreadUtil.newExecutor(2, 3);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();
        AtomicInteger done = new AtomicInteger();
        Runnable task = () -> {
            peak.accumulateAndGet(active.incrementAndGet(), Math::max);
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            active.decrementAndGet();
            done.incrementAndGet();
        };
        for (int i = 0; i < 5; i++) {
            executor.execute(task); // 실행 2 + 대기 3 = 한도까지는 바로 들어간다
        }
        Thread submitter = new Thread(() -> executor.execute(task));
        submitter.start();
        submitter.join(300);
        assertTrue(submitter.isAlive(), "6th submission should wait while 5 tasks are in flight");

        release.countDown();
        submitter.join(5000);
        assertFalse(submitter.isAlive());
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(6, done.get());
        assertTrue(peak.get() <= 2, "peak=" + peak.get());
    }

    @Test
    void blockedSubmitterIsRejectedWhenExecutorShutsDown() throws Exception {
        ExecutorService executor = S2ThreadUtil.newExecutor(1, 0);
        CountDownLatch started = new CountDownLatch(1);
        executor.execute(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        Thread submitter = new Thread(() -> {
            try {
                executor.execute(() -> { });
            } catch (Throwable e) {
                thrown.set(e);
            }
        });
        submitter.start();
        submitter.join(200);
        assertTrue(submitter.isAlive());

        executor.shutdownNow();
        submitter.join(5000);
        assertFalse(submitter.isAlive(), "blocked submitter was not released by shutdown");
        assertTrue(thrown.get() instanceof RejectedExecutionException, "thrown=" + thrown.get());
    }

    @Test
    void invalidLimitsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> S2ThreadUtil.newExecutor(0, 10));
        assertThrows(IllegalArgumentException.class, () -> S2ThreadUtil.newExecutor(2, -1));
    }

    /** 시스템 속성으로 시작한 별도 JVM 에서 실행되는 진입점. 결과를 한 줄로 출력한다. */
    public static class PropertiesMain {
        public static void main(String[] args) throws Exception {
            ExecutorService common = S2ThreadUtil.getCommonExecutor();
            AtomicInteger active = new AtomicInteger();
            AtomicInteger peak = new AtomicInteger();
            List<Callable<Object>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> {
                    peak.accumulateAndGet(active.incrementAndGet(), Math::max);
                    Thread.sleep(30);
                    active.decrementAndGet();
                    return null;
                });
            }
            common.invokeAll(tasks);
            System.out.println("RESULT virtual=" + S2ThreadUtil.isVirtualSupported() + " peak=" + peak.get());
            System.exit(0);
        }
    }

    private static String runWithProperty(String property) throws Exception {
        String classPath = Path.of(S2ThreadUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator
                + Path.of(S2ThreadUtilTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, property, "-cp", classPath, PropertiesMain.class.getName())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(30, TimeUnit.SECONDS));
        return output.lines().filter(l -> l.startsWith("RESULT ")).findFirst()
                .orElseThrow(() -> new AssertionError("no result: " + output));
    }

    @Test
    void virtualThreadsCanBeDisabledByProperty() throws Exception {
        assertEquals("RESULT virtual=false", runWithProperty("-Ds2.thread.virtual.enabled=false").replaceAll(" peak=.*", ""));
    }

    @Test
    void commonExecutorConcurrencyCanBeLimitedByProperty() throws Exception {
        assumeTrue(S2ThreadUtil.isVirtualSupported());
        String result = runWithProperty("-Ds2.thread.virtual.max_concurrency=2");
        assertTrue(result.startsWith("RESULT virtual=true"), result);
        int peak = Integer.parseInt(result.replaceAll(".* peak=", ""));
        assertTrue(peak >= 1 && peak <= 2, result);
    }
}

