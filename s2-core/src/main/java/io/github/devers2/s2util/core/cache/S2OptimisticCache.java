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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 낙관적 생성과 Sequence 기반 LRU를 지원하는 고성능 경량 캐시.
 * <p>
 * Lock-free 조회를 지향하며, 값이 없을 경우 여러 스레드가 동시에 생성하는 것을 허용하되
 * 저장 시점에 원자성을 보장하여 성능 경합을 최소화한다.
 * 최대치 도달 시 sequence가 낮은(오래된) 항목부터 점진적으로 삭제한다.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * ConcurrentHashMap 기반 경량 캐시 구현체입니다.
 * <p>
 * 조회 시 락을 사용하지 않아 고성능을 제공하며, 여러 스레드가 동시에 값을 생성할 수 있지만
 * putIfAbsent를 통해 저장 시점에만 원자성을 보장합니다.
 * Sequence 기반 LRU를 통해 자주 사용되는 항목을 우선 보호하며,
 * 최대치 도달 시 오래된 항목부터 절반을 삭제하는 점진적 전략을 사용합니다.
 * </p>
 *
 * @param <K> 캐시 키의 타입
 * @param <V> 캐시 값의 타입
 * @author devers2
 * @since 1.0.5
 */
public class S2OptimisticCache<K, V> {

    /**
     * 캐시 항목을 감싸는 래퍼 클래스.
     * 값과 함께 접근 순서를 추적하기 위한 sequence를 저장합니다.
     */
    private static class CacheEntry<V> {
        final V value;
        volatile long sequence; // 접근 순서 추적 (작을수록 오래됨)

        CacheEntry(V value, long sequence) {
            this.value = value;
            this.sequence = sequence;
        }
    }

    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicLong sequenceGenerator = new AtomicLong(0); // 전역 순서 카운터

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
     * 조회 시마다 sequence를 갱신하여 LRU 효과를 얻는다.
     *
     * @param key             캐시 키
     * @param mappingFunction 값이 없을 때 실행할 생성 함수
     * @return 캐시된 값 또는 새로 생성된 값
     */
    @SuppressWarnings("null")
    public V get(K key, Function<? super K, ? extends V> mappingFunction) {
        // 1. Non-blocking 조회 (99%의 케이스)
        CacheEntry<V> entry = cache.get(key);
        if (entry != null) {
            // 접근 순서 갱신 - AtomicLong.incrementAndGet()은 매우 빠름!
            entry.sequence = sequenceGenerator.incrementAndGet();
            return entry.value;
        }

        // 2. 현재 스레드에서 직접 생성 (가상 스레드/별도 스레드 불필요)
        V newValue = mappingFunction.apply(key);
        if (newValue == null) {
            return null;
        }

        // 3. 원자적 등록 (먼저 생성한 스레드의 값만 인정)
        CacheEntry<V> newEntry = new CacheEntry<>(newValue, sequenceGenerator.incrementAndGet());
        CacheEntry<V> existingEntry = cache.putIfAbsent(key, newEntry);

        if (existingEntry == null) {
            // 내가 첫 등록자라면 사이즈 체크 및 관리
            if (size.incrementAndGet() > maxEntries) {
                evictOldEntries();
            }
            return newValue;
        }

        // 찰나의 차이로 다른 스레드가 먼저 등록했다면 그 값을 반환
        // 그리고 그 항목의 sequence도 갱신
        existingEntry.sequence = sequenceGenerator.incrementAndGet();
        return existingEntry.value;
    }

    /**
     * 설정된 임계치를 넘었을 경우 오래된 항목부터 절반을 삭제한다.
     * Sequence가 낮은(오래된) 항목부터 우선 삭제하여 LRU 효과를 얻는다.
     */
    private synchronized void evictOldEntries() {
        if (size.get() <= maxEntries) {
            return; // 다른 스레드가 이미 정리했을 수 있음
        }

        // 1. 현재 캐시 항목들을 리스트로 복사
        List<Map.Entry<K, CacheEntry<V>>> entries = new ArrayList<>(cache.entrySet());

        // 2. sequence 기준 오름차순 정렬 (작을수록 오래됨)
        entries.sort(Comparator.comparingLong(e -> e.getValue().sequence));

        // 3. 절반만 남기고 나머지 삭제
        int targetSize = maxEntries / 2;
        int toRemove = entries.size() - targetSize;

        for (int i = 0; i < toRemove && i < entries.size(); i++) {
            K keyToRemove = entries.get(i).getKey();
            if (cache.remove(keyToRemove) != null) {
                size.decrementAndGet();
            }
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
