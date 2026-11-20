package io.github.devers2.s2util.core.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/**
 * S2OptimisticCache 동시성·축출 시험.
 */
class S2OptimisticCacheTest {

    /** 여러 스레드를 정해진 시간 동안 돌리고, 던져진 예외를 모은다. */
    private static List<Throwable> runConcurrently(int threads, long millis, Runnable[] bodies) throws InterruptedException {
        AtomicBoolean stop = new AtomicBoolean();
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        List<Thread> started = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            Runnable body = bodies[t % bodies.length];
            Thread th = new Thread(() -> {
                try {
                    while (!stop.get()) {
                        body.run();
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            th.start();
            started.add(th);
        }
        Thread.sleep(millis);
        stop.set(true);
        for (Thread th : started) {
            th.join();
        }
        return new ArrayList<>(errors);
    }

    private static void awaitSizeAtMost(S2OptimisticCache<?, ?> cache, long bound) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (cache.estimatedSize() > bound && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
    }

    @Test
    void evictionUnderConcurrentReadsNeverThrows() throws InterruptedException {
        // 축출 정렬 중 다른 스레드가 접근 순서를 바꿔도 get() 이 예외를 던지면 안 된다
        int max = 1000;
        S2OptimisticCache<Integer, Integer> cache = new S2OptimisticCache<>(max);
        AtomicInteger next = new AtomicInteger();
        Runnable writer = () -> cache.get(next.incrementAndGet(), k -> k);
        Runnable reader = () -> cache.get(ThreadLocalRandom.current().nextInt(next.get() + 1), k -> k);

        List<Throwable> errors = runConcurrently(8, 1500, new Runnable[] { writer, reader, reader, reader });

        assertTrue(errors.isEmpty(), () -> "get() threw: " + errors.get(0));
        awaitSizeAtMost(cache, max);
        assertTrue(cache.estimatedSize() <= max, "size=" + cache.estimatedSize());
    }

    @Test
    void distinctKeyBurstStaysBounded() throws InterruptedException {
        // 서로 다른 키만 몰려도(전부 miss) 크기가 상한의 2배(+스레드 경쟁분)를 넘지 않는다
        int max = 1000;
        int threads = 8;
        S2OptimisticCache<Long, Long> cache = new S2OptimisticCache<>(max);
        AtomicLong next = new AtomicLong();
        AtomicLong peak = new AtomicLong();
        Runnable inserter = () -> {
            cache.get(next.incrementAndGet(), k -> k);
            long size = cache.estimatedSize();
            peak.accumulateAndGet(size, Math::max);
        };

        List<Throwable> errors = runConcurrently(threads, 1500, new Runnable[] { inserter });

        assertTrue(errors.isEmpty(), () -> "get() threw: " + errors.get(0));
        assertTrue(peak.get() <= 2L * max + threads, "peak=" + peak.get());
        awaitSizeAtMost(cache, max);
        assertTrue(cache.estimatedSize() <= max, "size=" + cache.estimatedSize());
    }

    @Test
    void valuesAreStillReturnedWhenNotCached() throws InterruptedException {
        // 하드 리밋으로 캐싱하지 않는 경우에도 계산한 값은 그대로 돌려준다
        S2OptimisticCache<Long, Long> cache = new S2OptimisticCache<>(10);
        AtomicLong next = new AtomicLong();
        AtomicBoolean wrong = new AtomicBoolean();
        Runnable inserter = () -> {
            long key = next.incrementAndGet();
            if (cache.get(key, k -> k * 2) != key * 2) {
                wrong.set(true);
            }
        };

        List<Throwable> errors = runConcurrently(8, 500, new Runnable[] { inserter });

        assertTrue(errors.isEmpty(), () -> "get() threw: " + errors.get(0));
        assertFalse(wrong.get());
    }

    @Test
    void evictsLeastRecentlyUsedFirst() throws InterruptedException {
        int max = 100;
        S2OptimisticCache<Integer, Integer> cache = new S2OptimisticCache<>(max);
        for (int i = 0; i < max; i++) {
            cache.get(i, k -> k);
        }
        // 0~39 를 최근에 다시 사용
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 40; i++) {
                cache.get(i, k -> -1);
            }
        }
        cache.get(max, k -> k); // 상한 초과 → 축출

        awaitSizeAtMost(cache, max - 1);
        for (int i = 0; i < 40; i++) {
            assertTrue(cache.containsKey(i), "recently used key evicted: " + i);
            assertEquals(i, cache.getIfPresent(i));
        }
        assertTrue(cache.containsKey(max), "newest key evicted");
    }

    @Test
    void clearKeepsSizeCounterConsistent() throws InterruptedException {
        // clear() 가 동시 삽입과 겹쳐도 크기 카운터가 실제 항목 수와 같아야 한다(어긋나면 상한이 무너짐)
        // 상한은 넉넉히: 부하가 크면 삽입이 100만을 넘어 비동기 축출이 돌고, 축출이 끝나기 전에 세면 값이 어긋난다(이 시험의 대상이 아님)
        S2OptimisticCache<Integer, Integer> cache = new S2OptimisticCache<>(100_000_000);
        AtomicInteger next = new AtomicInteger();
        Runnable inserter = () -> cache.get(next.incrementAndGet(), k -> k);
        Runnable clearer = cache::clear;

        List<Throwable> errors = runConcurrently(8, 500, new Runnable[] { clearer, inserter, inserter, inserter });

        assertTrue(errors.isEmpty(), () -> "threw: " + errors.get(0));
        int actual = 0;
        for (int key = 1; key <= next.get(); key++) {
            if (cache.containsKey(key)) {
                actual++;
            }
        }
        assertEquals(actual, cache.estimatedSize());
    }

    @Test
    void cachesNullValues() {
        S2OptimisticCache<String, String> cache = new S2OptimisticCache<>(10);
        AtomicInteger calls = new AtomicInteger();
        assertNull(cache.get("a", k -> {
            calls.incrementAndGet();
            return null;
        }));
        assertNull(cache.get("a", k -> {
            calls.incrementAndGet();
            return "x";
        }));
        assertEquals(1, calls.get());
        assertTrue(cache.containsKey("a"));
    }
}
