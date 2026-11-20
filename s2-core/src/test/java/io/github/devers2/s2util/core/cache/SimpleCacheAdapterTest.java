package io.github.devers2.s2util.core.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * SimpleCacheAdapter 통계 집계 시험. loader 가 실행되면 미스, 아니면 히트(null 음성 캐싱 포함)로 센다.
 */
class SimpleCacheAdapterTest {

    @Test
    void countsHitsAndMissesWithSingleLookup() {
        SimpleCacheAdapter<String, String> cache = new SimpleCacheAdapter<>(100, true);
        assertEquals("v", cache.get("a", k -> "v")); // 미스
        assertEquals("v", cache.get("a", k -> "x")); // 히트
        assertNull(cache.get("n", k -> null)); // 미스(null 음성 캐싱)
        assertNull(cache.get("n", k -> "x")); // 히트
        assertTrue(cache.getStats().contains("hits=2, misses=2"), cache.getStats());

        cache.clear();
        assertTrue(cache.getStats().contains("size=0, hits=0, misses=0"), cache.getStats());
    }

    @Test
    void statsDisabledStillCaches() {
        SimpleCacheAdapter<String, String> cache = new SimpleCacheAdapter<>(100, false);
        assertEquals("v", cache.get("a", k -> "v"));
        assertEquals("v", cache.get("a", k -> "x"));
        assertEquals("Stats disabled", cache.getStats());
    }

    @Test
    void entriesExpireAfterAccessLikeTheCaffeineBackend() throws InterruptedException {
        // 경량 캐시도 expiryMs 를 지킨다(이전에는 무시해 Caffeine 이 없으면 만료되지 않았다)
        SimpleCacheAdapter<String, Integer> cache = new SimpleCacheAdapter<>(100, 150, false);
        java.util.concurrent.atomic.AtomicInteger loads = new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Function<String, Integer> loader = k -> loads.incrementAndGet();
        assertEquals(1, cache.get("k", loader));
        // 만료 시간 안에서 계속 접근하면 남는다(마지막 접근 기준)
        for (int i = 0; i < 4; i++) {
            Thread.sleep(60);
            assertEquals(1, cache.get("k", loader));
        }
        // 접근 없이 만료 시간이 지나면 다시 만든다
        Thread.sleep(400);
        assertEquals(2, cache.get("k", loader));
        assertEquals(1, cache.estimatedSize());
        // 0 이면 만료 없음(기존 동작)
        SimpleCacheAdapter<String, Integer> forever = new SimpleCacheAdapter<>(100, false);
        forever.get("k", loader);
        Thread.sleep(200);
        assertEquals(3, forever.get("k", loader));
    }
}
