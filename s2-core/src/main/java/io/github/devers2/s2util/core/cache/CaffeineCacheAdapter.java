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
 * Caffeine Cache를 래핑하여 CacheAdapter 인터페이스를 구현하는 고성능 캐시 어댑터입니다.
 * <p>
 * Caffeine의 W-TinyLFU 알고리즘을 활용하여 최적의 캐시 히트율을 제공하며,
 * 상세한 통계 정보와 이벤트 리스너를 지원합니다.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * Caffeine 기반 캐시 어댑터 구현체입니다.
 * <p>
 * Caffeine이 클래스패스에 존재할 때 사용되며, 고성능 캐싱과 상세한 통계 정보를 제공합니다.
 * W-TinyLFU 알고리즘을 통해 뛰어난 캐시 적중률을 보장합니다.
 * </p>
 *
 * @param <K> 캐시 키의 타입
 * @param <V> 캐시 값의 타입
 * @author devers2
 * @since 1.0.5
 */
public class CaffeineCacheAdapter<K, V> implements CacheAdapter<K, V> {

    private final Cache<K, V> cache;

    /**
     * CaffeineCacheAdapter 생성자.
     * <p>
     * Caffeine Cache 인스턴스를 직접 받아서 래핑합니다.
     * </p>
     *
     * @param cache Caffeine Cache 인스턴스
     */
    public CaffeineCacheAdapter(Cache<K, V> cache) {
        this.cache = Objects.requireNonNull(cache, "Caffeine cache must not be null");
    }

    @SuppressWarnings("null")
    @Override
    public V get(K key, Function<K, V> loader) {
        return cache.get(key, loader);
    }

    @Override
    public String getStats() {
        return cache.stats().toString();
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public long estimatedSize() {
        return cache.estimatedSize();
    }

    /**
     * Caffeine Cache 빌더를 생성하는 팩토리 메서드입니다.
     * <p>
     * S2Cache의 기존 설정을 유지하면서 Caffeine Cache를 생성합니다.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Caffeine Cache 빌더를 생성합니다.
     * <p>
     * 최대 크기, 만료 시간, 통계 수집, 리스너 등을 설정할 수 있습니다.
     * </p>
     *
     * @param <K>      캐시 키의 타입
     * @param <V>      캐시 값의 타입
     * @param maxSize  최대 캐시 크기
     * @param expiryMs 만료 시간 (밀리초, 0이면 만료 없음)
     * @return 설정된 Caffeine 빌더
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
     * Caffeine Cache를 생성하는 팩토리 메서드입니다.
     * <p>
     * 통계 수집과 제거 리스너를 선택적으로 활성화할 수 있습니다.
     * </p>
     *
     * @param <K>             캐시 키의 타입
     * @param <V>             캐시 값의 타입
     * @param maxSize         최대 캐시 크기
     * @param expiryMs        만료 시간 (밀리초, 0이면 만료 없음)
     * @param statsEnabled    통계 수집 활성화 여부
     * @param removalListener 제거 리스너 (null 가능)
     * @return 생성된 Caffeine Cache 인스턴스
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
     * 내부 Caffeine Cache 인스턴스를 반환합니다.
     * <p>
     * 고급 기능이 필요할 때 직접 Caffeine API에 접근할 수 있도록 합니다.
     * </p>
     *
     * @return Caffeine Cache 인스턴스
     */
    public Cache<K, V> getUnderlyingCache() {
        return cache;
    }
}
