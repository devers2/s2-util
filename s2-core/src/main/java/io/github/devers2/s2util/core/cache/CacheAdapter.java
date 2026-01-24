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

import java.util.function.Function;

/**
 * Adapter interface for abstracting cache implementations.
 * <p>
 * Both Caffeine Cache and lightweight cache implementations implement this interface,
 * allowing the runtime to dynamically select the available cache implementation.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 캐시 구현체를 추상화하는 어댑터 인터페이스입니다.
 * <p>
 * Caffeine Cache와 경량 캐시 구현체 모두 이 인터페이스를 구현하여,
 * 런타임에 사용 가능한 캐시 구현체를 동적으로 선택할 수 있도록 합니다.
 * </p>
 *
 * @param <K> Type of cache key | 캐시 키의 타입
 * @param <V> Type of cache value | 캐시 값의 타입
 * @author devers2
 * @since 1.0.5
 */
public interface CacheAdapter<K, V> {

    /**
     * Retrieves a value from the cache, or creates it using the loader if not present.
     * <p>
     * If a value corresponding to the key exists in the cache, it is returned immediately.
     * If it does not exist, the loader function is executed to create the value,
     * which is then cached and returned.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시에서 값을 조회하거나, 없을 경우 loader를 통해 생성하여 반환합니다.
     * <p>
     * 키에 해당하는 값이 캐시에 존재하면 즉시 반환하고,
     * 존재하지 않으면 loader 함수를 실행하여 값을 생성한 후 캐싱하고 반환합니다.
     * </p>
     *
     * @param key    Key to look up | 조회할 키
     * @param loader Creation function to execute when value is missing | 값이 없을 때 실행할 생성 함수
     * @return Cached value or newly created value | 캐시된 값 또는 새로 생성된 값
     */
    V get(K key, Function<K, V> loader);

    /**
     * Returns cache statistics as a string.
     * <p>
     * The Caffeine adapter provides detailed statistics (hit rate, eviction count, etc.),
     * while the lightweight cache adapter provides only basic statistical information.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시 통계 정보를 문자열로 반환합니다.
     * <p>
     * Caffeine 어댑터는 상세한 통계(히트율, 배출 횟수 등)를 제공하며,
     * 경량 캐시 어댑터는 기본적인 통계 정보만 제공합니다.
     * </p>
     *
     * @return Cache statistics string | 캐시 통계 정보 문자열
     */
    String getStats();

    /**
     * Removes all entries from the cache.
     * <p>
     * This method completely clears the cache, and subsequent lookups will create new values.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시의 모든 항목을 제거합니다.
     * <p>
     * 이 메서드는 캐시를 완전히 비우며, 이후 조회 시 새로운 값을 생성합니다.
     * </p>
     */
    void clear();

    /**
     * Returns the estimated number of entries currently stored in the cache.
     * <p>
     * This value may not be exact and may return an approximation depending on the adapter implementation.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 캐시에 저장된 항목의 예상 개수를 반환합니다.
     * <p>
     * 정확한 값이 아닐 수 있으며, 어댑터 구현에 따라 근사치를 반환할 수 있습니다.
     * </p>
     *
     * @return Estimated number of entries | 캐시 항목 개수 (근사치 가능)
     */
    long estimatedSize();
}
