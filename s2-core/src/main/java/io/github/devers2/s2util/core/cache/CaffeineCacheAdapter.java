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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;

/**
 * High-performance cache adapter that implements the CacheAdapter interface by wrapping Caffeine Cache.
 * <p>
 * Leverages Caffeine's W-TinyLFU algorithm to provide optimal cache hit rates,
 * supporting detailed statistical information and event listeners.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * Caffeine Cache를 래핑하여 CacheAdapter 인터페이스를 구현하는 고성능 캐시 어댑터입니다.
 * <p>
 * Caffeine의 W-TinyLFU 알고리즘을 활용하여 최적의 캐시 히트율을 제공하며,
 * 상세한 통계 정보와 이벤트 리스너를 지원합니다.
 * </p>
 *
 * @param <K> Type of cache key | 캐시 키의 타입
 * @param <V> Type of cache value | 캐시 값의 타입
 * @author devers2
 * @since 1.0.5
 */
public class CaffeineCacheAdapter<K, V> implements CacheAdapter<K, V> {

    private final Cache<K, V> cache;

    /**
     * Constructs a new CaffeineCacheAdapter.
     * <p>
     * Wraps a Caffeine Cache instance directly.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * CaffeineCacheAdapter 생성자입니다.
     * <p>
     * Caffeine Cache 인스턴스를 직접 받아서 래핑합니다.
     * </p>
     *
     * @param cache Caffeine Cache instance | Caffeine Cache 인스턴스
     */
    public CaffeineCacheAdapter(Cache<K, V> cache) {
        this.cache = Objects.requireNonNull(cache, "Caffeine cache must not be null");
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
    @SuppressWarnings("null")
    @Override
    public V get(K key, Function<K, V> loader) {
        return cache.get(key, loader);
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
        return cache.stats().toString();
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
        cache.invalidateAll();
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

    /**
     * Factory method for creating a Caffeine Cache builder.
     * <p>
     * Creates a Caffeine Cache while maintaining the existing configuration of S2Cache.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Caffeine Cache 빌더를 생성하는 팩토리 메서드입니다.
     * <p>
     * S2Cache의 기존 설정을 유지하면서 Caffeine Cache를 생성합니다.
     * </p>
     *
     * @param <K>      Type of cache key | 캐시 키의 타입
     * @param <V>      Type of cache value | 캐시 값의 타입
     * @param maxSize  Maximum cache size | 최대 캐시 크기
     * @param expiryMs Expiry time (ms, 0 for no expiration) | 만료 시간 (밀리초, 0이면 만료 없음)
     * @return Configured Caffeine builder | 설정된 Caffeine 빌더
     */
    public static <K, V> Caffeine<Object, Object> createBuilder(int maxSize, long expiryMs) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder().maximumSize(maxSize);

        if (expiryMs > 0) {
            // Add jitter (0~2000ms) to prevent thundering herd problem
            long jitter = (long) (Math.random() * 2000);
            builder.expireAfterAccess(expiryMs + jitter, TimeUnit.MILLISECONDS);
        }

        return builder;
    }

    /**
     * Factory method for creating a Caffeine Cache instance.
     * <p>
     * Statistics collection and removal listeners can be optionally enabled.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Caffeine Cache를 생성하는 팩토리 메서드입니다.
     * <p>
     * 통계 수집과 제거 리스너를 선택적으로 활성화할 수 있습니다.
     * </p>
     *
     * @param <K>             Type of cache key | 캐시 키의 타입
     * @param <V>             Type of cache value | 캐시 값의 타입
     * @param maxSize         Maximum cache size | 최대 캐시 크기
     * @param expiryMs        Expiry time (ms, 0 for no expiration) | 만료 시간 (밀리초, 0이면 만료 없음)
     * @param statsEnabled    Whether to enable statistics collection | 통계 수집 활성화 여부
     * @param removalListener Removal listener (can be null) | 제거 리스너 (null 가능)
     * @return Created Caffeine Cache instance | 생성된 Caffeine Cache 인스턴스
     */
    public static <K, V> Cache<K, V> createCache(
            int maxSize,
            long expiryMs,
            boolean statsEnabled,
            RemovalListener<K, V> removalListener) {
        Caffeine<Object, Object> builder = createBuilder(maxSize, expiryMs);

        if (statsEnabled) {
            builder.recordStats();
        }

        if (removalListener != null) {
            builder.removalListener(removalListener);
        }

        @SuppressWarnings("unchecked")
        Cache<K, V> cache = (Cache<K, V>) builder.build();

        return cache;
    }

    /**
     * Returns the underlying Caffeine Cache instance.
     * <p>
     * Allows direct access to the Caffeine API when advanced functionality is required.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 내부 Caffeine Cache 인스턴스를 반환합니다.
     * <p>
     * 고급 기능이 필요할 때 직접 Caffeine API에 접근할 수 있도록 합니다.
     * </p>
     *
     * @return Caffeine Cache instance | Caffeine Cache 인스턴스
     */
    public Cache<K, V> getUnderlyingCache() {
        return cache;
    }
}
