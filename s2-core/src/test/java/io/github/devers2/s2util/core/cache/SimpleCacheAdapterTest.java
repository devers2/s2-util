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
}
