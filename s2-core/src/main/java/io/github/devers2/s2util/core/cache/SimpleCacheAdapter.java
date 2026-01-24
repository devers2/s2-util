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

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * S2OptimisticCache를 래핑하여 CacheAdapter 인터페이스를 구현하는 경량 캐시 어댑터입니다.
 * <p>
 * Caffeine이 클래스패스에 없을 때 사용되는 대체 캐시 구현체로,
 * ConcurrentHashMap 기반의 단순하지만 효율적인 캐싱을 제공합니다.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 경량 캐시 어댑터 구현체입니다.
 * <p>
 * S2OptimisticCache를 내부적으로 사용하며, 기본적인 통계 정보(히트/미스)를 수집합니다.
 * Caffeine의 상세한 기능은 제공하지 않지만, 대부분의 사용 사례에서 충분한 성능을 제공합니다.
 * </p>
 *
 * @param <K> 캐시 키의 타입
 * @param <V> 캐시 값의 타입
 * @author devers2
 * @since 1.0.5
 */
public class SimpleCacheAdapter<K, V> implements CacheAdapter<K, V> {

    private final S2OptimisticCache<K, V> cache;
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final boolean statsEnabled;

    /**
     * SimpleCacheAdapter 생성자.
     *
     * @param maxSize      최대 캐시 크기
     * @param statsEnabled 통계 수집 활성화 여부
     */
    public SimpleCacheAdapter(int maxSize, boolean statsEnabled) {
        this.cache = new S2OptimisticCache<>(maxSize);
        this.statsEnabled = statsEnabled;
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        // 먼저 캐시에서 조회
        V value = cache.get(key, k -> null);

        if (value != null) {
            // 캐시 히트
            if (statsEnabled) {
                hitCount.incrementAndGet();
            }
            return value;
        }

        // 캐시 미스 - loader를 통해 생성
        if (statsEnabled) {
            missCount.incrementAndGet();
        }

        return cache.get(key, loader);
    }

    @Override
    public String getStats() {
        if (!statsEnabled) {
            return "Stats disabled";
        }

        long hits = hitCount.get();
        long misses = missCount.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total : 0.0;

        return String.format(
                "SimpleCacheAdapter[size=%d, hits=%d, misses=%d, hitRate=%.2f%%]",
                cache.estimatedSize(),
                hits,
                misses,
                hitRate * 100
        );
    }

    @Override
    public void clear() {
        cache.clear();
        if (statsEnabled) {
            hitCount.set(0);
            missCount.set(0);
        }
    }

    @Override
    public long estimatedSize() {
        return cache.estimatedSize();
    }
}
