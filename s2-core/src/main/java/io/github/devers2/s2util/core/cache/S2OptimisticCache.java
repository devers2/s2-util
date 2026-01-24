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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import io.github.devers2.s2util.core.S2ThreadUtil;

/**
 * High-performance lightweight cache supporting optimistic creation and sequence-based LRU.
 *
 * <h3>Design Philosophy & Advantages</h3>
 * <ul>
 * <li><b>Lock-free Read:</b> Achieves maximum performance by using {@link ConcurrentHashMap#get(Object)}
 * without any locking in 99% of cases.</li>
 * <li><b>Optimistic Creation:</b> Allows multiple threads to create values simultaneously
 * when a cache miss occurs, but ensures atomicity at the storage stage using {@code putIfAbsent}.
 * This eliminates wait time for other threads during value creation.</li>
 * <li><b>Sequence-based LRU:</b> Uses a simple {@link AtomicLong} counter instead of expensive
 * {@code System.nanoTime()} or {@code System.currentTimeMillis()} to track access order,
 * minimizing CPU overhead.</li>
 * <li><b>Asynchronous & Gradual Eviction:</b> When the cache reaches its maximum size,
 * eviction is performed on a background thread (via {@link S2ThreadUtil}) to prevent
 * blocking the main thread. Instead of clearing the entire cache, it gradually removes
 * only the oldest 50% of entries to maintain a high hit rate.</li>
 * </ul>
 *
 * <h3>Philosophical Considerations & Guardrails</h3>
 * <ul>
 * <li><b>Sequence Overflow:</b> Uses a {@code long} counter. With 1 billion increments per second,
 * it takes approx. 292 years to overflow. For system stability, this is considered
 * "effectively infinite" and no reset logic is added to maintain zero-overhead.</li>
 * <li><b>Double Creation:</b> Optimistic creation is a deliberate trade-off. We prefer occasional
 * duplicate object creation over mandatory thread blocking (locking) during high concurrency.</li>
 * <li><b>Size & Eviction Accuracy:</b> The exactness of the size counter and strict serial consistency
 * during eviction are relaxed to ensure lock-free read/write performance. It is "eventually stable"
 * and sufficient for cache management.</li>
 * </ul>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 낙관적 생성과 Sequence 기반 LRU를 지원하는 고성능 경량 캐시 구현체입니다.
 *
 * <h3>설계 철학 및 장점</h3>
 * <ul>
 * <li><b>Lock-free 조회:</b> 99%의 케이스에서 별도의 락 없이 {@link ConcurrentHashMap#get(Object)}만으로
 * 값을 조회하여 극한의 성능을 제공합니다.</li>
 * <li><b>낙관적 생성:</b> 캐시 미스 발생 시 여러 스레드가 동시에 값을 생성하는 것을 허용하되,
 * 저장 시점에 {@code putIfAbsent}를 통해 원자성을 보장합니다. 이를 통해 값 생성 중 다른 스레드가
 * 대기하는 현상을 완벽히 제거합니다.</li>
 * <li><b>Sequence 기반 LRU:</b> 값 비싼 {@code System.nanoTime()} 호출 대신 단순한 {@link AtomicLong}
 * 카운터를 사용하여 접근 순서를 추적함으로써 CPU 오버헤드를 최소화합니다.</li>
 * <li><b>비동기 점진적 삭제:</b> 최대 크기에 도달하면 메인 스레드를 블로킹하지 않고 별도의 백그라운드 스레드
 * ({@link S2ThreadUtil})에서 삭제 작업을 수행합니다. 전체를 비우는 대신 오래된 순으로 50%만 삭제하여
 * 캐시 적중률을 안정적으로 유지합니다.</li>
 * </ul>
 *
 * <h3>기술적 고려사항 및 방어 기제</h3>
 * <ul>
 * <li><b>Sequence 오버플로우:</b> {@code long} 카운터를 사용합니다. 초당 10억 번의 요청이 발생해도
 * 약 292년이 소요되므로, 실무적으로 무한한 수치로 간주하며 오버헤드 방지를 위해 별도의 리셋 로직을 두지 않습니다.</li>
 * <li><b>이중 생성(Double Creation):</b> 여러 스레드가 동시에 같은 값을 생성하는 것은 의도된 트레이드오프입니다.
 * 값 생성 중 모든 스레드를 블로킹(Lock)하는 비용보다, 아주 가끔 발생하는 중복 생성 비용이 훨씬 저렴합니다.</li>
 * <li><b>사이즈 및 삭제 정확성:</b> 캐시 사이즈 카운팅과 삭제 시점의 엄격한 일관성은 성능을 위해 의도적으로
 * 완화되었습니다. 이는 "최종적 일관성(Eventually Consistent)"을 따르며 캐시 관리 목적에는 충분히 정확합니다.</li>
 * </ul>
 *
 * @param <K> Type of cache key | 캐시 키의 타입
 * @param <V> Type of cache value | 캐시 값의 타입
 * @author devers2
 * @since 1.0.5
 */
public class S2OptimisticCache<K, V> {

    /**
     * Wrapper class for cache entries.
     * Stores the value along with a sequence number to track access order.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시 항목을 감싸는 래퍼 클래스입니다.
     * 값과 함께 접근 순서를 추적하기 위한 sequence를 저장합니다.
     */
    private static class CacheEntry<V> {
        final V value;
        volatile long sequence; // Access sequence (smaller is older) | 접근 순서 추적 (작을수록 오래됨)

        CacheEntry(V value, long sequence) {
            this.value = value;
            this.sequence = sequence;
        }
    }

    /**
     * Sentinel object to support caching of null values.
     */
    private static final Object NULL_HOLDER = new Object();

    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicLong sequenceGenerator = new AtomicLong(0);
    private final AtomicBoolean evicting = new AtomicBoolean(false);

    /**
     * Constructs a new S2OptimisticCache.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2OptimisticCache 생성자입니다.
     *
     * @param maxEntries Maximum number of entries allowed | 캐시의 최대 허용 개수
     */
    public S2OptimisticCache(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * Returns the cached value, or creates it if not present.
     * The sequence is updated on every access to provide LRU effects.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시된 값을 반환하며, 없을 경우 현재 스레드에서 생성하여 등록 후 반환합니다.
     * 조회 시마다 sequence를 갱신하여 LRU 효과를 제공합니다.
     *
     * @param key             Cache key | 캐시 키
     * @param mappingFunction Creation function to execute when value is missing | 값이 없을 때 실행할 생성 함수
     * @return Cached value or newly created value | 캐시된 값 또는 새로 생성된 값
     */
    @SuppressWarnings({ "null", "unchecked" })
    public V get(K key, Function<? super K, ? extends V> mappingFunction) {
        // 1. Non-blocking lookup (99% of cases)
        CacheEntry<V> entry = cache.get(key);
        if (entry != null) {
            // Update sequence - AtomicLong.incrementAndGet() is extremely fast!
            entry.sequence = sequenceGenerator.incrementAndGet();
            V value = entry.value;
            return (value == NULL_HOLDER) ? null : value;
        }

        // 2. Direct creation in the current thread (no virtual/dedicated thread needed)
        V newValue = mappingFunction.apply(key);

        // Use sentinel for null to support negative caching
        V valueToCache = (newValue == null) ? (V) NULL_HOLDER : newValue;

        // 3. Atomic registration (first-come, first-served)
        CacheEntry<V> newEntry = new CacheEntry<>(valueToCache, sequenceGenerator.incrementAndGet());
        CacheEntry<V> existingEntry = cache.putIfAbsent(key, newEntry);

        if (existingEntry == null) {
            // Check and manage size for the successful registerer
            if (size.incrementAndGet() > maxEntries) {
                // Start asynchronous eviction (doesn't block the main thread!)
                if (evicting.compareAndSet(false, true)) {
                    S2ThreadUtil.getCommonExecutor().execute(() -> {
                        try {
                            evictOldEntries();
                        } finally {
                            evicting.set(false);
                        }
                    });
                }
                // Skip if eviction is already in progress (will be cleaned later)
            }
            return newValue;
        }

        // If another thread registered first, return that value and update its sequence
        existingEntry.sequence = sequenceGenerator.incrementAndGet();
        V existingValue = existingEntry.value;
        return (existingValue == NULL_HOLDER) ? null : existingValue;
    }

    /**
     * Synchronized method to evict old entries starting from the lowest sequence.
     * Removes 50% of the oldest entries to provide LRU effects.
     * Runs on a background thread to avoid blocking the caller.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 설정된 임계치를 넘었을 경우 오래된 항목부터 절반을 삭제합니다.
     * Sequence가 낮은(오래된) 항목부터 우선 삭제하여 LRU 효과를 제공합니다.
     * 이 메서드는 백그라운드 스레드에서 실행되므로 메인 호출자를 블로킹하지 않습니다.
     */
    private void evictOldEntries() {
        if (size.get() <= maxEntries) {
            return; // Another thread might have already cleaned it
        }

        // 1. Copy entries to a list
        List<Map.Entry<K, CacheEntry<V>>> entries = new ArrayList<>(cache.entrySet());

        // 2. Sort by sequence ascending (older first)
        entries.sort(Comparator.comparingLong(e -> e.getValue().sequence));

        // 3. Keep half, remove the rest
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
     * Completely clears the cache.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시를 완전히 비웁니다.
     */
    public void clear() {
        cache.clear();
        size.set(0);
    }

    /**
     * Returns the estimated number of entries currently in the cache.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 캐시에 저장된 항목의 예상 개수를 반환합니다.
     *
     * @return Number of entries | 캐시 항목 개수
     */
    public long estimatedSize() {
        return size.get();
    }
}
