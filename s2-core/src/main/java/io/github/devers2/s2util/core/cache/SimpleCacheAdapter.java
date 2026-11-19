/**
 * S2Util Library
 *
 * Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)
 * Contact: eseungsu.dev@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For more information, please see the LICENSE file in the root directory.
 */
package io.github.devers2.s2util.core.cache;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

/**
 * Lightweight cache adapter that implements the CacheAdapter interface by wrapping S2OptimisticCache.
 * <p>
 * This is an alternative cache implementation used when Caffeine is not on the classpath,
 * providing simple but efficient caching based on ConcurrentHashMap.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2OptimisticCache를 래핑하여 CacheAdapter 인터페이스를 구현하는 경량 캐시 어댑터입니다.
 * <p>
 * Caffeine이 클래스패스에 없을 때 사용되는 대체 캐시 구현체로,
 * ConcurrentHashMap 기반의 단순하지만 효율적인 캐싱을 제공합니다.
 * </p>
 *
 * @param <K> Type of cache key | 캐시 키의 타입
 * @param <V> Type of cache value | 캐시 값의 타입
 * @author devers2
 * @since 1.0.5
 */
public class SimpleCacheAdapter<K, V> implements CacheAdapter<K, V> {

    private final S2OptimisticCache<K, V> cache;
    // 여러 스레드가 동시에 올려도 경합이 적은 LongAdder 사용
    private final LongAdder requestCount = new LongAdder();
    private final LongAdder missCount = new LongAdder();
    private final boolean statsEnabled;

    /**
     * Constructs a new SimpleCacheAdapter.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * SimpleCacheAdapter 생성자입니다.
     *
     * @param maxSize      Maximum cache size | 최대 캐시 크기
     * @param statsEnabled Whether to enable statistics collection | 통계 수집 활성화 여부
     */
    public SimpleCacheAdapter(int maxSize, boolean statsEnabled) {
        this.cache = new S2OptimisticCache<>(maxSize);
        this.statsEnabled = statsEnabled;
    }

    /**
     * Retrieves a value from the cache, or creates it using the loader if not present.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시에서 값을 조회하거나, 없을 경우 loader를 통해 생성하여 반환합니다.
     *
     * @param key    Key to look up | 조회할 키
     * @param loader Creation function to execute when value is missing | 값이 없을 때 실행할 생성 함수
     * @return Cached value or newly created value | 캐시된 값 또는 새로 생성된 값
     */
    @Override
    public V get(K key, Function<K, V> loader) {
        if (!statsEnabled) {
            // Null-safe: result can be null from Optional loaders (or a negatively-cached entry)
            return cache.get(key, loader);
        }

        // 조회는 한 번만 한다. loader 가 실행되면 미스, 아니면 히트(null 로 음성 캐싱된 항목도 loader 를 부르지 않으므로 히트)
        requestCount.increment();
        return cache.get(key, k -> {
            missCount.increment();
            return loader.apply(k);
        });
    }

    /**
     * Returns cache statistics as a string.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시 통계 정보를 문자열로 반환합니다.
     *
     * @return Cache statistics string | 캐시 통계 정보 문자열
     */
    @Override
    public String getStats() {
        if (!statsEnabled) {
            return "Stats disabled";
        }

        long total = requestCount.sum();
        long misses = Math.min(missCount.sum(), total);
        long hits = total - misses;
        double hitRate = total > 0 ? (double) hits / total : 0.0;

        return String.format(
                "SimpleCacheAdapter[size=%d, hits=%d, misses=%d, hitRate=%.2f%%]",
                cache.estimatedSize(),
                hits,
                misses,
                hitRate * 100);
    }

    /**
     * Removes all entries from the cache.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시의 모든 항목을 제거합니다.
     */
    @Override
    public void clear() {
        cache.clear();
        if (statsEnabled) {
            requestCount.reset();
            missCount.reset();
        }
    }

    /**
     * Returns the estimated number of entries currently stored in the cache.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 캐시에 저장된 항목의 예상 개수를 반환합니다.
     *
     * @return Estimated number of entries | 캐시 항목 개수
     */
    @Override
    public long estimatedSize() {
        return cache.estimatedSize();
    }
}
