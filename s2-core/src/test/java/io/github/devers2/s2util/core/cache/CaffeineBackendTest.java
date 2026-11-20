package io.github.devers2.s2util.core.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.benmanes.caffeine.cache.Cache;

import io.github.devers2.s2util.core.S2Cache;
import io.github.devers2.s2util.core.S2Util;

/**
 * Caffeine 백엔드 시험. {@code caffeineTest} 태스크(Caffeine 이 클래스패스에 있음)에서만 돌고, 기본 {@code test} 에서는 건너뛴다.
 */
class CaffeineBackendTest {

    private static final boolean CAFFEINE_RUN = "caffeine".equals(System.getProperty("s2.test.cacheBackend"));

    @BeforeAll
    static void onlyInCaffeineRun() {
        Assumptions.assumeTrue(CAFFEINE_RUN, "caffeineTest 태스크에서만 실행");
    }

    private static <K, V> CaffeineCacheAdapter<K, V> adapter(int maxSize) {
        Cache<K, V> cache = CaffeineCacheAdapter.createCache(maxSize, 0, false, null);
        return new CaffeineCacheAdapter<>(cache);
    }

    @Test
    void s2CacheReallyUsesCaffeineInThisRun() {
        // 이 태스크가 실제로 Caffeine 백엔드로 도는지(클래스패스 구성이 틀리면 조용히 경량 캐시로 돌 수 있음)
        assertTrue(S2Cache.isCaffeineEnabled());
    }

    @Test
    void loaderRunsOutsideTheCacheLock() {
        // loader 안에서 같은 키를 다시 조회해도 된다 — computeIfAbsent 버킷 락 안에서 돌면 "Recursive update" IllegalStateException
        CaffeineCacheAdapter<Integer, String> cache = adapter(100);
        String value = cache.get(1, k -> cache.get(1, inner -> "inner") + "-outer");
        // 먼저 저장된 값(안쪽 조회의 "inner")이 남고 바깥 호출도 그 값을 돌려준다 — 경량 캐시와 같은 의미
        assertEquals("inner", value);
        assertEquals("inner", cache.get(1, k -> "unused"));
    }

    @Test
    void slowLoadersOfDifferentKeysDoNotWaitForEachOther() throws Exception {
        // 64개 키의 loader 가 모두 동시에 들어와야 풀리는 장벽 — loader 가 버킷 락 안에서 돌면 같은 버킷 키끼리 막혀 시간 초과
        CaffeineCacheAdapter<Integer, Integer> cache = adapter(1000);
        int keys = 64;
        CyclicBarrier barrier = new CyclicBarrier(keys);
        ExecutorService pool = Executors.newFixedThreadPool(keys);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < keys; i++) {
                int key = i;
                futures.add(pool.submit(() -> cache.get(key, k -> {
                    try {
                        barrier.await(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new IllegalStateException("loaders did not run concurrently", e);
                    }
                    return k * 10;
                })));
            }
            for (int i = 0; i < keys; i++) {
                assertEquals(i * 10, futures.get(i).get(20, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void nullResultIsReturnedButNotCached() {
        CaffeineCacheAdapter<String, String> cache = adapter(10);
        AtomicInteger calls = new AtomicInteger();
        assertNull(cache.get("k", k -> {
            calls.incrementAndGet();
            return null;
        }));
        assertNull(cache.get("k", k -> {
            calls.incrementAndGet();
            return null;
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void sizeStaysBounded() {
        CaffeineCacheAdapter<Integer, Integer> cache = adapter(100);
        for (int i = 0; i < 10_000; i++) {
            cache.get(i, k -> k);
        }
        cache.getUnderlyingCache().cleanUp();
        assertTrue(cache.estimatedSize() <= 100, "size " + cache.estimatedSize());
    }

    @Test
    void s2CacheResolveAndGetValueWorkOnCaffeine() {
        // 동적 캐시(resolve)·메서드 핸들 공용 캐시(JDK 클래스)·정규식 캐시가 Caffeine 백엔드로 동작
        assertEquals(Optional.of("v1"), S2Cache.resolve("s2.test.caffeine", 1, Integer.class, String.class, 10,
                k -> Optional.of("v" + k)));
        assertEquals(3, (int) S2Util.getValue("abc", "length"));
        assertTrue(S2Cache.getPattern("^a+$").isPresent());
    }
}
