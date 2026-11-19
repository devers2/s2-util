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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import io.github.devers2.s2util.core.S2ThreadUtil;

/**
 * High-performance lightweight cache supporting optimistic creation and epoch-based approximate LRU.
 *
 * <h3>Design Philosophy and Advantages</h3>
 * <ul>
 * <li><b>Lock-free Read:</b> Achieves maximum performance by using {@link ConcurrentHashMap#get(Object)}
 * without any locking in 99% of cases.</li>
 * <li><b>Optimistic Creation:</b> Allows multiple threads to create values simultaneously
 * when a cache miss occurs, but ensures atomicity at the storage stage using {@code putIfAbsent}.
 * This eliminates wait time for other threads during value creation.</li>
 * <li><b>Epoch-based approximate LRU:</b> Each entry records the access <i>epoch</i>, a counter advanced by
 * insertions (not by reads). A hit only reads the current epoch and writes the entry only when the epoch has
 * changed since its last access, so reads perform no shared atomic update and scale with the number of cores.
 * Recency is tracked at epoch granularity, which is precise enough for eviction.</li>
 * <li><b>Asynchronous and Gradual Eviction:</b> When the cache reaches its maximum size,
 * eviction is performed on a dedicated maintenance thread (separate from application-facing
 * executors, see {@link MaintenanceExecutorHolder}) to prevent blocking the main thread and
 * to keep eviction timely even when the application saturates its own executors. Instead of
 * clearing the entire cache, it removes the least recently used entries down to 75% of the maximum to maintain
 * a high hit rate. If eviction cannot keep up (e.g. a burst of distinct keys), new values beyond
 * twice the maximum size are returned without being cached, so growth stays bounded and callers
 * never run eviction themselves.</li>
 * </ul>
 *
 * <h3>Philosophical Considerations and Guardrails</h3>
 * <ul>
 * <li><b>Epoch Overflow:</b> Uses a {@code long} counter advanced at most once per insertion. With 1 billion
 * increments per second, it takes approx. 292 years to overflow. For system stability, this is considered
 * "effectively infinite" and no reset logic is added to maintain zero-overhead.</li>
 * <li><b>Double Creation:</b> Optimistic creation is a deliberate trade-off. We prefer occasional
 * duplicate object creation over mandatory thread blocking (locking) during high concurrency.</li>
 * <li><b>Size and Eviction Accuracy:</b> The exactness of the size counter and strict serial consistency
 * during eviction are relaxed to ensure lock-free read/write performance. It is "eventually stable"
 * and sufficient for cache management.</li>
 * </ul>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 낙관적 생성과 세대(epoch) 기반 근사 LRU를 지원하는 고성능 경량 캐시 구현체입니다.
 *
 * <h3>설계 철학 및 장점</h3>
 * <ul>
 * <li><b>Lock-free 조회:</b> 99%의 케이스에서 별도의 락 없이 {@link ConcurrentHashMap#get(Object)}만으로
 * 값을 조회하여 극한의 성능을 제공합니다.</li>
 * <li><b>낙관적 생성:</b> 캐시 미스 발생 시 여러 스레드가 동시에 값을 생성하는 것을 허용하되,
 * 저장 시점에 {@code putIfAbsent}를 통해 원자성을 보장합니다. 이를 통해 값 생성 중 다른 스레드가
 * 대기하는 현상을 완벽히 제거합니다.</li>
 * <li><b>세대(epoch) 기반 근사 LRU:</b> 각 항목은 마지막 접근 <i>세대</i>를 기록하며, 세대는 조회가 아닌 삽입에 따라
 * 증가합니다. 조회는 현재 세대를 읽기만 하고 마지막 접근 이후 세대가 바뀌었을 때만 항목에 기록하므로, 공유 원자 연산이
 * 없어 코어 수에 비례해 확장됩니다. 접근 순서는 세대 단위로 추적되며 축출에는 충분히 정확합니다.</li>
 * <li><b>비동기 점진적 삭제:</b> 최대 크기에 도달하면 메인 스레드를 블로킹하지 않고, 애플리케이션이 쓰는
 * 실행기와 분리된 전용 유지보수 스레드({@link MaintenanceExecutorHolder})에서 삭제 작업을 수행하여
 * 애플리케이션 부하와 무관하게 축출이 지연되지 않도록 합니다. 전체를 비우는 대신 오래된 순으로 최대 크기의
 * 75%까지만 삭제하여 캐시 적중률을 안정적으로 유지하며, 축출이 따라가지 못하는 상황(서로 다른 키가 몰릴 때 등)에는
 * 최대 크기의 2배를 넘는 새 값을 캐싱하지 않고 반환만 하여 무한 증가를 막고 호출자가 축출을 떠안지 않게 합니다.</li>
 * </ul>
 *
 * <h3>기술적 고려사항 및 방어 기제</h3>
 * <ul>
 * <li><b>세대 오버플로우:</b> 삽입당 최대 1회 증가하는 {@code long} 카운터를 사용합니다. 초당 10억 번 증가해도
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
     * Lazily-initialized, dedicated single-thread executor for background eviction.
     * <p>
     * Kept separate from {@link S2ThreadUtil#getCommonExecutor()} so that application-submitted
     * work cannot delay eviction under load; only created on first use, since it stays unused
     * whenever Caffeine (which manages its own eviction) is available.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 백그라운드 축출 전용 지연 초기화 단일 스레드 실행기입니다.
     * <p>
     * 애플리케이션이 제출한 작업 때문에 축출이 밀리지 않도록 {@link S2ThreadUtil#getCommonExecutor()}와
     * 분리했습니다. Caffeine이 있으면(자체적으로 축출을 관리) 사용되지 않으므로 최초 사용 시점에만 생성됩니다.
     * </p>
     */
    private static final class MaintenanceExecutorHolder {
        private static final ExecutorService INSTANCE = Executors
                .newSingleThreadExecutor(S2ThreadUtil.getPlatformFactory());
    }

    /**
     * Wrapper class for cache entries.
     * Stores the value along with the epoch of its last access to track access order.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시 항목을 감싸는 래퍼 클래스입니다.
     * 값과 함께 접근 순서를 추적하기 위한 마지막 접근 세대(epoch)를 저장합니다.
     */
    private static class CacheEntry<V> {
        final V value;
        volatile long accessEpoch; // 마지막 접근 세대 (작을수록 오래됨)

        CacheEntry(V value, long accessEpoch) {
            this.value = value;
            this.accessEpoch = accessEpoch;
        }
    }

    /**
     * Sentinel object to support caching of null values.
     */
    private static final Object NULL_HOLDER = new Object();

    /**
     * Hard-limit multiplier: once the cache holds this many times {@code maxEntries} (eviction cannot
     * keep up, e.g. a burst of distinct keys), new values are returned without being cached, so the
     * cache never grows unbounded and callers never run eviction themselves. | 하드 리밋 배수: 축출이
     * 따라가지 못해(서로 다른 키가 몰릴 때 등) {@code maxEntries}의 이 배수에 이르면 새 값은 캐싱하지 않고
     * 반환만 하여, 캐시가 무한히 커지지 않고 호출자가 직접 축출을 떠안지도 않도록 합니다.
     */
    private static final int HARD_LIMIT_MULTIPLIER = 2;

    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final long hardLimit;
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicLong epoch = new AtomicLong(0);
    private final AtomicInteger insertTick = new AtomicInteger(0);
    private final int epochMask;
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
        this.hardLimit = Math.max(1L, (long) maxEntries * HARD_LIMIT_MULTIPLIER);
        // 세대는 삽입 N건마다 1 증가: 작은 캐시는 촘촘하게(정확도), 큰 캐시는 성기게(조회 쪽 캐시 라인 무효화 감소)
        this.epochMask = Integer.highestOneBit(Math.max(1, Math.min(64, maxEntries / 32))) - 1;
    }

    /**
     * Returns the cached value, or creates it if not present.
     * The access epoch is refreshed on access to provide LRU effects.
     * Supports null values through negative caching using a sentinel pattern.
     *
     * <h4>Concurrency and Double Creation</h4>
     * <p>
     * In high-concurrency scenarios where multiple threads simultaneously miss the same key,
     * {@code mappingFunction} may execute multiple times. This is a deliberate trade-off:
     * we prefer occasional duplicate creation over thread blocking (locking) during cache misses.
     * </p>
     * <p>
     * <b>Safe:</b> If your {@code mappingFunction} is idempotent or side-effect free,
     * double creation is harmless.<br>
     * <b>Caution:</b> If creation is expensive (e.g., DB queries, external API calls) and not idempotent,
     * consider using {@link CaffeineCacheAdapter} or adding external synchronization.
     * </p>
     *
     * <h4>Null Value Handling</h4>
     * <p>
     * Null values are cached using a sentinel object (negative caching).
     * Subsequent requests for the same key will return the cached null without calling {@code mappingFunction}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시된 값을 반환하며, 없을 경우 현재 스레드에서 생성하여 등록 후 반환합니다.
     * 조회 시 접근 세대를 갱신하여 LRU 효과를 제공합니다.
     * null 값도 센티넬 패턴을 통해 캐싱됩니다(음수 캐싱).
     *
     * <h4>동시성 및 이중 생성</h4>
     * <p>
     * 여러 스레드가 동시에 같은 키로 캐시 미스를 발생시킬 때, {@code mappingFunction}이 여러 번 실행될 수 있습니다.
     * 이는 의도된 트레이드오프입니다: 캐시 미스 중 스레드 블로킹(Lock)보다 아주 가끔 발생하는 이중 생성을 선택했습니다.
     * </p>
     * <p>
     * <b>안전함:</b> {@code mappingFunction}이 멱등성(idempotent)이거나 부작용이 없으면 이중 생성은 무해합니다.<br>
     * <b>주의:</b> 생성 비용이 큼(DB 쿼리, 외부 API 호출 등)이고 멱등성이 없으면
     * {@link CaffeineCacheAdapter} 사용 또는 외부 동기화 추가를 권장합니다.
     * </p>
     *
     * <h4>Null 값 처리</h4>
     * <p>
     * Null 값은 센티넬 객체를 사용하여 캐싱됩니다(음수 캐싱).
     * 같은 키에 대한 후속 요청은 {@code mappingFunction} 호출 없이 캐시된 null을 반환합니다.
     * </p>
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
            touch(entry);
            V value = entry.value;
            return (value == NULL_HOLDER) ? null : value;
        }

        // 2. Direct creation in the current thread (no virtual/dedicated thread needed)
        V newValue = mappingFunction.apply(key);

        // Use sentinel for null to support negative caching
        V valueToCache = (newValue == null) ? (V) NULL_HOLDER : newValue;

        // 하드 리밋: 축출이 따라가지 못해 상한의 배수에 이르면 캐싱하지 않고 값만 반환한다(무한 증가 방지)
        if (size.get() >= hardLimit) {
            scheduleEviction();
            return newValue;
        }

        // 3. Atomic registration (first-come, first-served)
        CacheEntry<V> newEntry = new CacheEntry<>(valueToCache, epoch.get());
        CacheEntry<V> existingEntry = cache.putIfAbsent(key, newEntry);

        if (existingEntry == null) {
            // 등록에 성공한 스레드만 크기를 세고, 상한을 넘으면 비동기 축출을 예약한다(호출 스레드는 막지 않음)
            if ((insertTick.incrementAndGet() & epochMask) == 0) {
                epoch.incrementAndGet();
            }
            if (size.incrementAndGet() > maxEntries) {
                scheduleEviction();
            }
            return newValue;
        }

        // If another thread registered first, return that value and update its access epoch
        touch(existingEntry);
        V existingValue = existingEntry.value;
        return (existingValue == NULL_HOLDER) ? null : existingValue;
    }

    /**
     * Retrieves the value from the cache if present, without creating it.
     * Updates the LRU sequence if found.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시에 값이 존재하면 반환하고, 없으면 null을 반환합니다.
     * 값을 생성하지 않으며, 조회 성공 시 LRU 순서를 갱신합니다.
     *
     * @param key Cache key | 조회할 키
     * @return Cached value or null if not present | 캐시된 값 또는 없으면 null
     */
    public V getIfPresent(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null) {
            touch(entry);
            V value = entry.value;
            return (value == NULL_HOLDER) ? null : value;
        }
        return null;
    }

    /**
     * Marks the entry as accessed in the current epoch. Writes only when the epoch has changed, so repeated
     * hits within an epoch are plain reads.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 항목을 현재 세대에 접근한 것으로 표시합니다. 세대가 바뀌었을 때만 기록하므로 같은 세대 안의 반복 조회는 읽기만 합니다.
     */
    private void touch(CacheEntry<V> entry) {
        long current = epoch.get();
        if (entry.accessEpoch != current) {
            entry.accessEpoch = current;
        }
    }

    /**
     * Checks whether an entry exists for the given key, including negatively-cached
     * ({@code null}-valued) entries.
     * <p>
     * Unlike {@link #getIfPresent(K)}, which unwraps the internal null sentinel and therefore
     * cannot distinguish "not cached" from "cached as null", this method reports raw entry
     * presence — useful for callers (e.g. hit/miss statistics) that need that distinction.
     * Does not affect the LRU sequence.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 음성 캐싱({@code null} 값)된 항목을 포함하여, 주어진 키에 대한 엔트리 존재 여부를 확인합니다.
     * <p>
     * 내부 null 센티널을 해제하여 반환하는 {@link #getIfPresent(K)}와 달리, "캐시에 없음"과
     * "null로 캐싱됨"을 구분할 수 없는 문제가 없어, 히트/미스 통계처럼 그 구분이 필요한 호출자에게
     * 유용합니다. LRU 순서에는 영향을 주지 않습니다.
     * </p>
     *
     * @param key Cache key | 조회할 키
     * @return {@code true} if an entry (including a negatively-cached one) exists | 엔트리가
     *         존재하면(음성 캐싱 포함) {@code true}
     */
    public boolean containsKey(K key) {
        return cache.get(key) != null;
    }

    /**
     * Schedules a background eviction on the maintenance thread unless one is already pending or running.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 이미 예약되었거나 실행 중인 축출이 없으면 유지보수 스레드에 백그라운드 축출을 예약합니다.
     */
    private void scheduleEviction() {
        if (!evicting.compareAndSet(false, true)) {
            return;
        }
        try {
            MaintenanceExecutorHolder.INSTANCE.execute(() -> {
                try {
                    // 축출 중에도 삽입이 계속되므로, 상한 아래로 내려갈 때까지(진척이 있는 동안) 반복한다
                    while (size.get() > maxEntries && evictOldEntries() > 0) {
                        // 반복
                    }
                } finally {
                    evicting.set(false);
                }
            });
        } catch (RejectedExecutionException e) {
            evicting.set(false);
        }
    }

    /**
     * Evicts the least recently accessed entries until the cache is back to 75% of {@code maxEntries}.
     * <p>
     * Access epochs are copied into a primitive snapshot before ordering, because they keep changing
     * under concurrent reads; ordering live values would break the sort contract. An entry is removed only
     * if it is still the same instance that was snapshotted.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 접근이 가장 오래된 항목부터 삭제하여 {@code maxEntries}의 75%까지 줄입니다.
     * <p>
     * 동시 조회 중에도 접근 순서 값이 계속 바뀌므로, 정렬 전에 원시 배열 스냅샷으로 복사합니다(살아있는 값으로
     * 정렬하면 정렬 계약이 깨짐). 스냅샷 시점과 같은 항목 인스턴스일 때만 삭제합니다.
     * </p>
     *
     * @return Number of entries removed | 삭제한 항목 수
     */
    @SuppressWarnings("unchecked")
    private int evictOldEntries() {
        if (size.get() <= maxEntries) {
            return 0; // Another thread might have already cleaned it
        }

        // 1. 키·항목·접근 순서를 스냅샷으로 복사(이후 값이 바뀌어도 정렬에 영향 없음)
        int capacity = cache.size() + 16;
        Object[] keys = new Object[capacity];
        CacheEntry<V>[] entries = new CacheEntry[capacity];
        long[] epochs = new long[capacity];
        int count = 0;
        for (Map.Entry<K, CacheEntry<V>> e : cache.entrySet()) {
            if (count == capacity) {
                break; // 복사 중 늘어난 항목은 다음 축출에서 처리
            }
            CacheEntry<V> entry = e.getValue();
            keys[count] = e.getKey();
            entries[count] = entry;
            epochs[count] = entry.accessEpoch;
            count++;
        }

        // 2. 지울 개수만큼의 경계값을 원시 배열 정렬로 구함
        int targetSize = (int) ((long) maxEntries * 3 / 4);
        int toRemove = count - targetSize;
        if (toRemove <= 0) {
            return 0;
        }
        long[] sorted = Arrays.copyOf(epochs, count);
        Arrays.sort(sorted);
        long cutoff = sorted[toRemove - 1];

        // 3. 경계값 이하(오래된) 항목을 지울 개수만큼 삭제 — 그사이 교체된 항목은 건드리지 않음
        int removedCount = 0;
        for (int i = 0; i < count && removedCount < toRemove; i++) {
            if (epochs[i] <= cutoff && cache.remove((K) keys[i], entries[i])) {
                size.decrementAndGet();
                removedCount++;
            }
        }
        return removedCount;
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
        // 동시 삽입과 겹쳐도 크기 카운터가 어긋나지 않도록, 실제로 지운 항목만큼만 줄인다
        for (K key : cache.keySet()) {
            if (cache.remove(key) != null) {
                size.decrementAndGet();
            }
        }
    }

    /**
     * Returns the estimated number of entries currently in the cache.
     *
     * <h4>Eventual Consistency</h4>
     * <p>
     * This method returns an <b>estimated</b> size due to the eventually-consistent design.
     * The actual size may vary slightly due to concurrent additions and evictions happening
     * without strict synchronization. Typical deviation is within ±5% for normal workloads.
     * </p>
     *
     * <h4>Usage Recommendations</h4>
     * <p>
     * <b>Safe:</b> Use for statistics, monitoring, or logging (e.g., "Cache size: 1,234").<br>
     * <b>Caution:</b> Do NOT use for strict logic decisions:
     * </p>
     *
     * <pre>{@code
     * // UNSAFE - relies on exact size
     * if (cache.estimatedSize() < 1000) {
     *     // Critical business decision - may be inaccurate!
     * }
     *
     * // SAFE - uses threshold with safety margin
     * if (cache.estimatedSize() < maxEntries * 0.8) {
     *     // Monitoring/logging with confidence interval
     * }
     * }</pre>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 캐시에 저장된 항목의 예상 개수를 반환합니다.
     *
     * <h4>최종적 일관성(Eventual Consistency)</h4>
     * <p>
     * 이 메서드는 최종적 일관성 설계로 인해 <b>추정값</b>을 반환합니다.
     * 엄격한 동기화 없이 동시에 발생하는 추가와 삭제로 인해 실제 크기는 약간 다를 수 있습니다.
     * 일반적인 작업 환경에서는 편차가 ±5% 범위입니다.
     * </p>
     *
     * <h4>사용 권장사항</h4>
     * <p>
     * <b>안전함:</b> 통계, 모니터링, 로깅에 사용하세요 (예: "Cache size: 1,234").<br>
     * <b>주의:</b> 엄격한 논리 결정에 사용하지 마세요:
     * </p>
     *
     * <pre>{@code
     * // 위험함 - 정확한 크기에 의존
     * if (cache.estimatedSize() < 1000) {
     *     // 중요한 비즈니스 결정 - 부정확할 수 있음!
     * }
     *
     * // 안전함 - 여유도를 포함한 임계값 사용
     * if (cache.estimatedSize() < maxEntries * 0.8) {
     *     // 신뢰도 있는 모니터링/로깅
     * }
     * }</pre>
     *
     * @return Estimated number of entries | 캐시 항목 개수(추정값)
     */
    public long estimatedSize() {
        return size.get();
    }
}
