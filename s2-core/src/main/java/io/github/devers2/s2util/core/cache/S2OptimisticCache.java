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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 낙관적 생성을 지원하는 고성능 경량 캐시.
 * <p>
 * Lock-free 조회를 지향하며, 값이 없을 경우 여러 스레드가 동시에 생성하는 것을 허용하되
 * 저장 시점에 원자성을 보장하여 성능 경합을 최소화한다.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * ConcurrentHashMap 기반 경량 캐시 구현체입니다.
 * <p>
 * 조회 시 락을 사용하지 않아 고성능을 제공하며, 여러 스레드가 동시에 값을 생성할 수 있지만
 * putIfAbsent를 통해 저장 시점에만 원자성을 보장합니다.
 * 최대치 도달 시 전체 캐시를 삭제하는 단순한 전략을 사용합니다.
 * </p>
 *
 * @param <K> 캐시 키의 타입
 * @param <V> 캐시 값의 타입
 * @author devers2
 * @since 1.0.5
 */
public class S2OptimisticCache<K, V> {

    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final AtomicInteger size = new AtomicInteger(0);

    /**
     * S2OptimisticCache 생성자.
     *
     * @param maxEntries 캐시의 최대 허용 개수
     */
    public S2OptimisticCache(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * 캐시된 값을 반환하며, 없을 경우 현재 스레드에서 생성하여 등록 후 반환한다.
     *
     * @param key             캐시 키
     * @param mappingFunction 값이 없을 때 실행할 생성 함수
     * @return 캐시된 값 또는 새로 생성된 값
     */
    @SuppressWarnings("null")
    public V get(K key, Function<? super K, ? extends V> mappingFunction) {
        // 1. Non-blocking 조회 (99%의 케이스)
        V value = cache.get(key);
        if (value != null) {
            return value;
        }

        // 2. 현재 스레드에서 직접 생성 (가상 스레드/별도 스레드 불필요)
        V newValue = mappingFunction.apply(key);
        if (newValue == null) {
            return null;
        }

        // 3. 원자적 등록 (먼저 생성한 스레드의 값만 인정)
        V existingValue = cache.putIfAbsent(key, newValue);

        if (existingValue == null) {
            // 내가 첫 등록자라면 사이즈 체크 및 관리
            if (size.incrementAndGet() > maxEntries) {
                clearIfFull();
            }
            return newValue;
        }

        // 찰나의 차이로 다른 스레드가 먼저 등록했다면 그 값을 반환
        return existingValue;
    }

    /**
     * 설정된 임계치를 넘었을 경우 캐시를 비운다.
     */
    private synchronized void clearIfFull() {
        if (size.get() > maxEntries) {
            cache.clear();
            size.set(0);
        }
    }

    /**
     * 캐시를 완전히 비웁니다.
     */
    public void clear() {
        cache.clear();
        size.set(0);
    }

    /**
     * 현재 캐시에 저장된 항목의 예상 개수를 반환합니다.
     *
     * @return 캐시 항목 개수
     */
    public long estimatedSize() {
        return size.get();
    }
}
