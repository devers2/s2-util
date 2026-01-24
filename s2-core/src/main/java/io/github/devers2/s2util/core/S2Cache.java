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
package io.github.devers2.s2util.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import io.github.devers2.s2util.core.S2Cache.MethodHandleResolver.LookupType;
import io.github.devers2.s2util.core.S2Cache.MethodHandleResolver.MethodKey;
import io.github.devers2.s2util.core.cache.CacheAdapter;
import io.github.devers2.s2util.core.cache.CaffeineCacheAdapter;
import io.github.devers2.s2util.core.cache.SimpleCacheAdapter;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * Central cache management class for the S2Util library (Optimized for Java 17+).
 * <p>
 * This class serves as a high-performance registry for expensive objects such as regex patterns,
 * {@link java.lang.invoke.MethodHandle}s, and reflection metadata. It uses <b>Caffeine Cache</b>
 * when available, or falls back to a lightweight ConcurrentHashMap-based cache to ensure
 * scalability and stability in high-concurrency environments.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리 내부에서 사용하는 핵심 캐시 관리 클래스입니다 (Java 17+ 최적화).
 * <p>
 * 정규식 패턴(Pattern), MethodHandle, 리플렉션 메타데이터 등 생성 비용이 큰 객체들을 메모리에 캐싱하여 재사용합니다.
 * Caffeine이 클래스패스에 있으면 고성능 캐싱을 제공하고, 없으면 경량 캐시로 대체됩니다.
 * </p>
 *
 * <h3>Design Philosophy and Infrastructure (설계 철학 및 기반 기술)</h3>
 * <ul>
 * <li><b>Adaptive Caching:</b> Automatically selects Caffeine Cache (W-TinyLFU algorithm) when available,
 * or uses a lightweight ConcurrentHashMap-based cache for minimal dependencies.</li>
 * <li><b>Memory and Thread Safety:</b> Thread-safe by design using {@link java.util.concurrent.ConcurrentHashMap}
 * and optimized cache implementations.</li>
 * <li><b>MethodHandle Optimization:</b> Combines Java 17's {@link java.lang.invoke.MethodHandle} with
 * caching for near-native performance for dynamic operations.</li>
 * </ul>
 *
 * <h3>System Properties for Configuration (시스템 설정 프로퍼티)</h3>
 * <ul>
 * <li><b>{@code s2.cache.stats.enabled}</b> (boolean, default: {@code false}):
 * Enables cache performance statistics collection.</li>
 * <li><b>{@code s2.cache.listener.enabled}</b> (boolean, default: {@code false}):
 * Enables removal listeners for logging eviction events (Caffeine only).</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 * @see java.lang.invoke.MethodHandle
 */
public class S2Cache {

    private static final S2Logger logger = S2LogManager.getLogger(S2Cache.class);

    /**
     * Whether to enable cache statistics collection.
     * <p>
     * Configured via system property {@code s2.cache.stats.enabled}.
     * When enabled, metrics such as hit rate and eviction count are recorded,
     * which can be accessed via {@link com.github.benmanes.caffeine.cache.Cache#stats()}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시 성능 통계 수집 활성화 여부입니다.
     * <p>
     * 시스템 프로퍼티 {@code s2.cache.stats.enabled} 값을 통해 설정하며, 활성화 시 캐시 적중률(Hit Rate),
     * 배출 횟수(Eviction Count) 등의 지표를 기록하여 {@link com.github.benmanes.caffeine.cache.Cache#stats()}를
     * 통해 확인할 수 있게 합니다.
     * </p>
     */
    private static final boolean STATS_ENABLED = Boolean.getBoolean("s2.cache.stats.enabled");

    /**
     * Whether to enable cache removal listeners.
     * <p>
     * Configured via system property {@code s2.cache.listener.enabled}.
     * When enabled, removal events (Eviction, Replacement, etc.) are logged
     * for diagnostic purposes.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시 이벤트 리스너(제거 알림) 활성화 여부입니다.
     * <p>
     * 시스템 프로퍼티 {@code s2.cache.listener.enabled} 값을 통해 설정하며, 데이터가 캐시에서
     * 제거될 때(Eviction, Replacement 등) 원인과 함께 로그를 기록하여 진단 정보를 제공합니다.
     * </p>
     */
    private static final boolean LISTENER_ENABLED = Boolean.getBoolean("s2.cache.listener.enabled");

    /**
     * Map for converting Wrapper classes to Primitive classes.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Wrapper 클래스에서 Primitive 클래스로의 변환 맵입니다.
     */
    public static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE_MAP = new ConcurrentHashMap<>();

    /**
     * Map for converting Primitive classes to Wrapper classes.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Primitive 클래스에서 Wrapper 클래스로의 변환 맵입니다.
     */
    public static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_MAP = new ConcurrentHashMap<>();

    static {
        var w2p = Map.of(
                Integer.class, int.class,
                Long.class, long.class,
                Double.class, double.class,
                Float.class, float.class,
                Boolean.class, boolean.class,
                Byte.class, byte.class,
                Short.class, short.class,
                Character.class, char.class
        );

        WRAPPER_TO_PRIMITIVE_MAP.putAll(w2p);
        w2p.forEach((k, v) -> PRIMITIVE_TO_WRAPPER_MAP.put(v, k));
    }

    /**
     * Caffeine 사용 가능 여부를 캐싱하는 변수.
     * <p>
     * 클래스 로더를 통해 Caffeine 클래스 존재 여부를 확인하여 설정됩니다.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Caffeine이 클래스패스에 존재하는지 여부를 저장합니다.
     */
    private static final boolean CAFFEINE_AVAILABLE = isCaffeineAvailable();

    /**
     * Meta-registry storing dynamically created cache adapter instances.
     * <p>
     * Each cache uses either Caffeine (W-TinyLFU) or lightweight ConcurrentHashMap implementation
     * depending on availability.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 동적으로 생성된 캐시 어댑터 인스턴스들을 저장하는 메타 저장소입니다.
     * <p>
     * Caffeine 사용 가능 여부에 따라 적절한 캐시 구현체를 사용합니다.
     * </p>
     */
    private static final Map<CacheKey, CacheAdapter<?, ?>> DYNAMIC_CACHES = new ConcurrentHashMap<>();

    private static final String RESOURCE_BUNDLE_CACHE_NAME = "s2.cache.resource_bundle";
    private static final String FIELDS_CACHE_NAME = "s2.cache.fields";

    private S2Cache() {
        // Prevent instantiation
    }

    /**
     * Caffeine 클래스가 클래스패스에 존재하는지 확인합니다.
     * <p>
     * 클래스 로더를 통해 Caffeine 클래스를 로드하여 존재 여부를 판단합니다.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Caffeine이 사용 가능한지 확인합니다.
     *
     * @return Caffeine 사용 가능 여부
     */
    private static boolean isCaffeineAvailable() {
        try {
            Class.forName("com.github.benmanes.caffeine.cache.Caffeine");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Creates a cache adapter instance based on Caffeine availability.
     * <p>
     * If Caffeine is available, creates a CaffeineCacheAdapter with advanced features.
     * Otherwise, creates a SimpleCacheAdapter with basic caching functionality.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Caffeine 사용 가능 여부에 따라 적절한 캐시 어댑터를 생성합니다.
     * <p>
     * Caffeine이 있으면 고성능 어댑터를, 없으면 경량 어댑터를 생성합니다.
     * </p>
     *
     * @param <K>       캐시 키의 타입
     * @param <V>       캐시 값의 타입
     * @param maxSize   최대 캐시 크기
     * @param expiryMs  만료 시간 (밀리초, 0이면 만료 없음)
     * @param cacheName 캐시 이름 (로깅용)
     * @return 생성된 캐시 어댑터
     */
    @SuppressWarnings("unchecked")
    private static <K, V> CacheAdapter<K, V> createCacheAdapter(int maxSize, long expiryMs, String cacheName) {
        if (CAFFEINE_AVAILABLE) {
            try {
                // Caffeine 어댑터 생성
                var caffeineBuilder = CaffeineCacheAdapter.createBuilder(maxSize, expiryMs);

                if (STATS_ENABLED) {
                    caffeineBuilder.recordStats();
                }

                // Executor 설정 (S2ThreadUtil 사용)
                caffeineBuilder.executor(Objects.requireNonNull(S2ThreadUtil.getCommonExecutor()));

                // Removal Listener 설정
                if (LISTENER_ENABLED) {
                    caffeineBuilder.removalListener((key, value, cause) -> {
                        if (logger.isDebugEnabled()) {
                            logRemovalEvent(cacheName, key, cause);
                        }
                    });
                }

                var caffeineCache = caffeineBuilder.build();
                var adapter = new CaffeineCacheAdapter<K, V>((com.github.benmanes.caffeine.cache.Cache<K, V>) caffeineCache);

                if (logger.isDebugEnabled()) {
                    logger.debug("[S2Cache] Caffeine adapter created for cache: {}", cacheName);
                }

                return adapter;
            } catch (Exception e) {
                logger.warn("[S2Cache] Failed to create Caffeine adapter, falling back to simple cache: {}", e.getMessage());
                // Caffeine 생성 실패 시 경량 캐시로 폴백
            }
        }

        // 경량 캐시 어댑터 생성
        if (logger.isDebugEnabled()) {
            logger.debug("[S2Cache] Simple cache adapter created for cache: {} (Caffeine not available)", cacheName);
        }
        return new SimpleCacheAdapter<>(maxSize, STATS_ENABLED);
    }

    /**
     * Logs a cache removal event.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시 제거 이벤트를 로깅합니다.
     *
     * @param cacheName Name of the cache | 캐시 이름
     * @param key       Removed key | 제거된 키
     * @param cause     Reason for removal | 제거 사유
     */
    private static void logRemovalEvent(String cacheName, Object key, Object cause) {
        if (S2Util.isKorean(Locale.getDefault())) {
            logger.debug("캐시 항목 제거됨. 이름: {}, 키: {}, 사유: {}", cacheName, key, cause);
        } else {
            logger.debug("Cache entry removed. name: {}, key: {}, cause: {}", cacheName, key, cause);
        }
    }

    /**
     * Returns whether Caffeine Cache is currently being used.
     * <p>
     * Returns true if high-performance caching via Caffeine is enabled,
     * or false if lightweight caching via ConcurrentHashMap is being used.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Caffeine 캐시 사용 여부를 반환합니다.
     * <p>
     * true이면 Caffeine 기반 고성능 캐싱을, false이면 ConcurrentHashMap 기반 경량 캐싱을 사용 중임을 의미합니다.
     * </p>
     *
     * @return True if Caffeine is enabled | Caffeine 사용 가능 여부
     */
    public static boolean isCaffeineEnabled() {
        return CAFFEINE_AVAILABLE;
    }

    /**
     * A unique key record for identifying cache instances.
     * <p>
     * By including type information for both keys and values, it isolates caches
     * with the same name but different types, preventing {@link ClassCastException}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시 인스턴스를 식별하기 위한 고유 키 레코드입니다.
     * <p>
     * 이름뿐만 아니라 키와 값의 타입 정보를 포함함으로써, 동일한 이름을 사용하더라도
     * 타입이 다를 경우 서로 다른 캐시로 격리하여 {@link ClassCastException}을 방지합니다.
     * </p>
     *
     * @param name      Unique name of the cache | 캐시 식별 이름
     * @param keyType   Class type of the cache key | 캐시 키의 클래스 타입
     * @param valueType Class type of the cache value | 캐시 값의 클래스 타입
     */
    private record CacheKey(String name, Class<?> keyType, Class<?> valueType) {}

    /**
     * Retrieves a value from the cache or loads it using the provided loader if not present.
     * <p>
     * This method provides a default expiration of 0 (no expiration).
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시에서 값을 조회하거나, 없을 경우 생성 로직을 실행하여 결과를 캐싱하고 반환합니다.
     * <p>
     * 별도의 만료 시간 설정 없이 영구 보관(메모리 부족 전까지)하는 기본 처리 방식입니다.
     * </p>
     *
     * @param <K>       The type of the cache key | 캐시 키의 타입
     * @param <V>       The type of the cache value | 캐시 값의 타입
     * @param cacheName Unique name of the cache instance | 캐시 인스턴스의 고유 이름
     * @param key       The key to look up | 조회할 키
     * @param keyType   Class of the key (used for registry isolation) | 키 클래스 (메타 저장소 격리용)
     * @param valueType Class of the value (used for registry isolation) | 값 클래스 (메타 저장소 격리용)
     * @param maxSize   Maximum size if a new cache instance needs to be created | 새 캐시 인스턴스 생성 시 최대 크기
     * @param loader    Function to generate the value if not in cache | 캐시에 없을 경우 값을 생성할 함수
     * @return An Optional containing the value, or empty if generation fails | 값을 포함한 Optional, 생성 실패 시 빈 Optional
     */
    public static <K, V> Optional<V> resolve(String cacheName, K key, Class<K> keyType, Class<V> valueType, int maxSize, Function<K, Optional<V>> loader) {
        return resolve(cacheName, key, keyType, valueType, maxSize, 0, loader);
    }

    /**
     * Retrieves a value from the cache or loads it using the provided loader if not present.
     * <p>
     * This method combines cache instance management (Get-or-Create) and value retrieval
     * into a single atomic operation point.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시에서 값을 조회하거나, 없을 경우 생성 로직을 실행하여 캐싱 후 반환합니다.
     * <p>
     * 캐시 인스턴스의 생성 관리와 값 조회를 하나의 진입점으로 통합한 핵심 메서드입니다.
     * </p>
     *
     * @param <K>       The type of the cache key | 캐시 키의 타입
     * @param <V>       The type of the cache value | 캐시 값의 타입
     * @param cacheName Unique name of the cache instance | 캐시 인스턴스의 고유 이름
     * @param key       The key to look up | 조회할 키
     * @param keyType   Class of the key (used for registry isolation) | 키 클래스 (메타 저장소 격리용)
     * @param valueType Class of the value (used for registry isolation) | 값 클래스 (메타 저장소 격리용)
     * @param maxSize   Maximum size if a new cache instance needs to be created | 새 캐시 인스턴스 생성 시 최대 크기
     * @param expiryMs  Expiration time in milliseconds (0 for no expiration) | 만료 시간 (ms, 0은 만료 없음)
     * @param loader    Function to generate the value if not in cache | 캐시에 없을 경우 값을 생성할 함수
     * @return An Optional containing the value, or empty if generation fails | 값을 포함한 Optional, 생성 실패 시 빈 Optional
     */
    public static <K, V> Optional<V> resolve(String cacheName, K key, Class<K> keyType, Class<V> valueType, int maxSize, long expiryMs, Function<K, Optional<V>> loader) {
        if (key == null) {
            return Optional.empty();
        }

        // 1. 레코드 기반 고유 식별자 생성함
        CacheKey cacheKey = new CacheKey(cacheName, keyType, valueType);

        // 2. 해당 식별자에 맞는 캐시 어댑터를 가져오거나 생성함
        @SuppressWarnings("unchecked")
        CacheAdapter<K, Optional<V>> cache = (CacheAdapter<K, Optional<V>>) DYNAMIC_CACHES.computeIfAbsent(cacheKey, ck -> {
            return createCacheAdapter(maxSize, expiryMs, ck.name());
        });

        // 3. 입력 데이터 타입 검증함 (O(1))
        if (!keyType.isInstance(key)) {
            throw new IllegalArgumentException(
                    String.format(
                            "[S2Cache] 타입 불일치: 캐시(%s)는 %s 타입을 요구하지만 %s 가 입력됨",
                            cacheName, keyType.getSimpleName(), key.getClass().getSimpleName()
                    )
            );
        }

        // 4. 로딩 수행
        Optional<V> value = cache.get(key, Objects.requireNonNull(loader));

        // 5. 반환 값 타입 검증 (Optional 내부 데이터 확인, 성능 최적화를 위해 디버그 모드에서만 검증)
        if (logger.isDebugEnabled()) {
            if (value.isPresent() && !valueType.isInstance(value.get())) {
                DYNAMIC_CACHES.remove(cacheKey);
                throw new IllegalStateException("[S2Cache] 캐시 데이터 오염됨: 예상 타입 " + valueType.getSimpleName());
            }
        }

        return value;
    }

    /**
     * Returns a consolidated string representation of all active cache statistics.
     * <p>
     * Requires {@code s2.cache.stats.enabled=true} to provide meaningful metrics.
     * Useful for health monitoring and performance tuning.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 활성화된 모든 동적 캐시의 현재 통계 정보를 문자열로 반환합니다.
     * <p>
     * 시스템 프로퍼티 {@code s2.cache.stats.enabled}가 true인 경우에만 유의미한 지표가 출력됩니다.
     * 자가 진단 및 튜닝 시 활용 가능합니다.
     * </p>
     *
     * @return A multi-line summary of cache hit rates, eviction counts, etc.
     */
    public static String getCacheStats() {
        if (!STATS_ENABLED) {
            return "Cache statistics are disabled. (Set -Ds2.cache.stats.enabled=true)";
        }

        StringBuilder sb = new StringBuilder("\n--- S2Cache Global Statistics ---\n");

        sb.append(
                """
                [S2Cache Statistics]
                - MethodCache: %s
                - PatternCache: %s
                """.formatted(
                        MethodHandleResolver.CACHE.getStats(),
                        PatternResolver.CACHE.getStats()
                )
        );

        DYNAMIC_CACHES.forEach((ck, cache) -> {
            sb.append(
                    String.format(
                            "[%s <%s>] %s%n",
                            ck.name(),
                            ck.valueType().getSimpleName(),
                            cache.getStats()
                    )
            );
        });
        return sb.toString();
    }

    /**
     * Resolves the actual class type from an object instance, stripping away proxies.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 객체 인스턴스로부터 프록시가 제거된 실제 클래스 타입을 추출합니다.
     *
     * @param object The target object instance | 대상 객체 인스턴스
     * @return The underlying target class, or null if object is null | 실제 대상 클래스 (객체가 null이면 null)
     */
    public static Class<?> getRealClass(Object object) {
        if (object == null)
            return null;
        return getRealClass(object.getClass());
    }

    /**
     * Resolves the actual class type from a given class, stripping away proxies.
     * <p>
     * Detects and unwraps common proxy patterns such as Hibernate Proxies ({@code $$HibernateProxy})
     * or Spring CGLIB proxies. This ensures that reflection and caching lookups target the
     * correct underlying entity and not the temporary proxy wrapper.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 클래스 타입으로부터 프록시가 제거된 실제 클래스 타입을 추출합니다.
     * <p>
     * Hibernate 프록시나 Spring CGLIB 프록시가 전달될 경우, 해당 프록시의 부모인 실제 엔티티 클래스를 반환합니다.
     * 이를 통해 정확한 메서드 탐색 및 캐싱 키 생성을 보장합니다.
     * </p>
     *
     * @param clazz The class to inspect | 조사할 클래스
     * @return The underlying target class, or the original class if it's not a proxy | 실제 대상 클래스 (프록시가 아니면 원본 클래스)
     */
    private static Class<?> getRealClass(Class<?> clazz) {
        if (clazz != null && (clazz.getName().contains("$$") || clazz.getName().contains("CGLIB"))) {
            return clazz.getSuperclass();
        }
        return clazz;
    }

    /**
     * Retrieves a {@link MethodHandle} for a method or field from the cache based on {@link MethodKey}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@link MethodKey}를 기반으로 캐시에서 메서드 또는 필드에 대한 {@link MethodHandle}을 조회합니다.
     *
     * @param key  Identification key containing target class and member info | 대상 클래스 및 멤버 정보를 포함하는 식별 키
     * @param type Search scope (METHOD, FIELD, or BOTH) | 검색 범위 (METHOD, FIELD, 또는 BOTH)
     * @return An Optional containing the MethodHandle if found, or empty if resolution fails | 발견된 경우 MethodHandle을 포함하는 Optional, 실패 시 빈 Optional
     */
    public static Optional<MethodHandle> getMethodHandle(MethodKey key, LookupType type) {
        return MethodHandleResolver.resolve(key, type);
    }

    /**
     * Retrieves a compiled and cached {@link Pattern} for the given regex string.
     * <p>
     * Compiling regular expressions is expensive; this method ensures that identical regex
     * patterns are reused across the system.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 정규식 문자열에 대해 컴파일 및 캐싱된 {@link Pattern} 객체를 반환합니다.
     * <p>
     * 정규식 컴파일은 비용이 많이 드는 작업이므로, 동일한 패턴이 시스템 전반에서 재사용되도록 보장합니다.
     * </p>
     *
     * @param regex The regular expression string | 정규식 문자열
     * @return The compiled Pattern object wrapped in an Optional, or empty if regex is invalid | Optional로 감싸진 컴파일된 Pattern 객체, 유효하지 않은 정규식인 경우 빈 Optional
     */
    public static Optional<Pattern> getPattern(String regex) {
        return PatternResolver.resolve(regex);
    }

    /**
     * Retrieves all declared fields for a class, leveraging high-performance caching.
     * <p>
     * Fields are pre-processed with {@link java.lang.reflect.Field#setAccessible(boolean)} set to true.
     * Returns a cloned array to prevent external modification of the cache origin.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐싱을 적용하여 클래스의 모든 선언된 필드(Declared Fields)를 반환합니다.
     * <p>
     * 조회된 필드들은 즉시 접근 가능하도록 설정되어 있으며, 캐시 수정을 방지하기 위해 복사본을 반환합니다.
     * </p>
     *
     * @param clazz Target class | 대상 클래스
     * @return An Optional containing the array of fields | 필드 배열을 포함하는 Optional
     */
    public static Optional<Field[]> getFields(Class<?> clazz) {
        if (clazz == null)
            return Optional.empty();

        return resolve(
                FIELDS_CACHE_NAME,
                clazz, Class.class,
                Field[].class,
                1000,
                key -> {
                    Field[] fieldArray = key.getDeclaredFields();

                    // 모든 필드에 대해 접근 권한 부여함
                    for (Field field : fieldArray) {
                        field.setAccessible(true);
                    }

                    // 방어적 복사를 통해 외부 수정으로부터 캐시 원본 보호함
                    return Optional.of(fieldArray.clone());
                }
        );
    }

    /**
     * Retrieves a cached {@link ResourceBundle} for localization.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 다국어 처리를 위한 {@link ResourceBundle}을 조회합니다 (캐싱 적용).
     *
     * @param basename Base path of the resource bundle (e.g., "messages/validation") | 리소스 번들 기본 경로
     * @param locale   Target locale (defaults to {@link Locale#getDefault()} if null) | 대상 로케일 (null인 경우 기본값 사용)
     * @return An Optional containing the ResourceBundle | ResourceBundle을 포함하는 Optional
     */
    public static Optional<ResourceBundle> getResourceBundle(String basename, Locale locale) {
        return ResourceBundleResolver.resolve(basename, locale);
    }

    /**
     * Resolver for {@link java.lang.invoke.MethodHandle}s.
     * <p>
     * Automatically discovers, creates, and caches MethodHandles based on {@link MethodKey}.
     * It follows a prioritized search strategy (public methods first, then declared methods)
     * and implements "Negative Caching" for non-existent members to prevent repetitive,
     * expensive reflection attempts.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * MethodHandle 리졸버입니다.
     * <p>
     * MethodKey로 요청된 메서드 핸들을 자동으로 탐색·생성·캐싱합니다.
     * Public 메서드 → Declared 메서드 순으로 탐색하며, 존재하지 않는 멤버에 대해서도
     * 결과(Optional.empty())를 캐싱하여 무분별한 리플렉션 시도를 방지합니다.
     * </p>
     */
    public static class MethodHandleResolver {

        /**
         * Central cache for {@link MethodHandle} objects discovered via reflection.
         * <p>
         * Uses an optimized cache implementation (Caffeine or lightweight) to manage memory efficiently.
         * </p>
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 리플렉션을 통해 조회된 {@link MethodHandle} 객체들의 중앙 캐시입니다.
         * <p>
         * 사용 가능한 캐시 구현체를 사용하여 메모리를 효율적으로 관리합니다.
         * </p>
         */
        private static final CacheAdapter<MethodKey, Optional<MethodHandle>> CACHE = createMethodCache();

        /**
         * Creates a cache adapter for MethodHandles.
         *
         * @return CacheAdapter instance for MethodHandles | MethodHandle 전용 캐시 어댑터 인스턴스
         */
        private static CacheAdapter<MethodKey, Optional<MethodHandle>> createMethodCache() {
            return createCacheAdapter(10000, 0, "MethodHandleResolver");
        }

        /**
         * Target lookup scope for member discovery.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 멤버 탐색 시 대상이 되는 탐색 범위 타입입니다.
         */
        public enum LookupType {
            /** 메서드만 검색 */
            METHOD,
            /** 필드만 검색 */
            FIELD,
            /** 메서드와 필드 모두 검색 */
            BOTH
        }

        /** Map GET MethodHandle 키 */
        public static final MethodKey MAP_GET_KEY = new MethodKey(Map.class, "get", Object.class);
        /** Map PUT MethodHandle 키 */
        public static final MethodKey MAP_PUT_KEY = new MethodKey(Map.class, "put", Object.class, Object.class);

        // 하이패스용 메서드 핸들 사전 정의 (자주 사용되는 메서드 선언)
        private static final MethodHandle MAP_GET;
        private static final MethodHandle MAP_PUT;
        private static final MethodHandle MAP_CONTAINS;
        private static final MethodHandle LIST_GET;
        private static final MethodHandle COLLECTION_SIZE;
        private static final MethodHandle OBJECT_TO_STRING;
        private static final MethodHandle OPTIONAL_GET;
        private static final MethodHandle OPTIONAL_IS_PRESENT;

        static {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();

                // 빈번하게 사용되는 표준 메서드들을 정적 초기화 시점에 미리 로드한다.
                MAP_GET = lookup.findVirtual(Map.class, "get", MethodType.methodType(Object.class, Object.class));
                MAP_PUT = lookup.findVirtual(Map.class, "put", MethodType.methodType(Object.class, Object.class, Object.class));
                MAP_CONTAINS = lookup.findVirtual(Map.class, "containsKey", MethodType.methodType(boolean.class, Object.class));
                LIST_GET = lookup.findVirtual(List.class, "get", MethodType.methodType(Object.class, int.class));
                COLLECTION_SIZE = lookup.findVirtual(Collection.class, "size", MethodType.methodType(int.class));
                OBJECT_TO_STRING = lookup.findVirtual(Object.class, "toString", MethodType.methodType(String.class));
                OPTIONAL_GET = lookup.findVirtual(Optional.class, "get", MethodType.methodType(Object.class));
                OPTIONAL_IS_PRESENT = lookup.findVirtual(Optional.class, "isPresent", MethodType.methodType(boolean.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new ExceptionInInitializerError("[MethodHandleResolver] 정적 초기화 실패: " + e.getMessage());
            }
        }

        private static final Class<?>[] EMPTY_TYPES = new Class<?>[0];

        /**
         * Immutable key used to uniquely identify a method or field within a specific class.
         * <p>
         * Designed for high-frequency lookup scenarios, leveraging Java 17's {@code record}
         * for built-in immutability and memory efficiency.
         * </p>
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 특정 클래스의 메서드나 필드를 고유하게 식별하기 위한 불변 키 클래스입니다.
         * <p>
         * 빈번한 캐시 조회 성능을 최적화하기 위해 Java 17의 {@code record}를 사용하여 설계되었습니다.
         * </p>
         *
         * @param targetClass The class containing the member | 멤버를 포함하는 클래스
         * @param methodName  Name of the method (use {@code "<init>"} for constructors) | 메서드 이름 (생성자의 경우 {@code "<init>"} 사용)
         * @param fieldName   Optional field name for setter/getter type inference | 세터/게터 타입 추론을 위한 선택적 필드 이름
         * @param paramTypes  Array of parameter types (empty for fields) | 파라미터 타입 배열 (필드의 경우 빈 배열)
         */
        public record MethodKey(Class<?> targetClass, String methodName, String fieldName, Class<?>[] paramTypes) {

            /* 가변인자 최적화 - paramType 3개 까지는 일반 매개변수로 받고 4개 이상은 가변인자로 받는다. */

            /**
             * Constructor without fieldName.
             * <p>
             * <b>CAUTION:</b> Keys created without a fieldName are distinct from those with one.
             * This may bypass field-type based setter inference; use only for explicit method calls.
             * </p>
             *
             * <p>
             * <b>[한국어 설명]</b>
             * </p>
             * fieldName이 없는 공통 생성자입니다.
             * <p>
             * <b>주의:</b> fieldName이 누락된 경우 다른 키로 인식되며, 필드 타입 기반의 Setter 추론 기능이
             * 작동하지 않으므로 명확한 메서드 호출 시에만 사용하십시오.
             * </p>
             *
             * @param targetClass Target class | 대상 클래스
             * @param methodName  Method name (e.g., "getName") | 메서드 이름 (예: "getName")
             */
            public MethodKey(Class<?> targetClass, String methodName) {
                this(targetClass, methodName, null, EMPTY_TYPES);
            }

            /**
             * Constructor without fieldName (for single parameter).
             * <p>
             * <b>CAUTION:</b> Keys created without a fieldName are distinct from those with one.
             * </p>
             *
             * <p>
             * <b>[한국어 설명]</b>
             * </p>
             * fieldName 없이 단일 파라미터 타입을 받는 생성자입니다.
             * <p>
             * <b>주의:</b> fieldName이 누락된 경우, fieldName이 있는 키와 서로 다른 키로 인식됩니다.
             * </p>
             *
             * @param targetClass Target class | 대상 클래스
             * @param methodName  Method name (e.g., "getName", "setName") | 메서드 이름 (예: "getName", "setName")
             * @param paramType   Parameter type | 파라미터 타입
             */
            public MethodKey(Class<?> targetClass, String methodName, Class<?> paramType) {
                this(targetClass, methodName, null, new Class<?>[] { paramType });
            }

            /**
             * Creates a MethodKey with two parameter types.
             *
             * <p>
             * <b>[한국어 설명]</b>
             * </p>
             * fieldName 없이 두 개의 파라미터 타입을 받는 생성자입니다.
             *
             * @param targetClass Target class | 대상 클래스
             * @param methodName  Method name | 메서드 이름
             * @param paramType1  First parameter type | 첫 번째 파라미터 타입
             * @param paramType2  Second parameter type | 두 번째 파라미터 타입
             */
            public MethodKey(Class<?> targetClass, String methodName, Class<?> paramType1, Class<?> paramType2) {
                this(targetClass, methodName, null, new Class<?>[] { paramType1, paramType2 });
            }

            /**
             * 생성자 (fieldName 없이 호출 가능)
             * <p>
             * <b>❗주의:</b> fieldName이 누락된 경우, fieldName이 있는 키와 서로 다른 키로 인식됩니다.
             * Setter 추론(Inference) 기능이 작동하지 않으므로, Map.get/put 등 명확한 메서드 호출 시에만 사용하십시오.
             * </p>
             *
             * @param targetClass 대상 클래스
             * @param methodName  메서드 이름(예: "getName", "setName")
             * @param paramType1  파라미터 타입1
             * @param paramType2  파라미터 타입2
             * @param paramType3  파라미터 타입3
             */
            public MethodKey(Class<?> targetClass, String methodName, Class<?> paramType1, Class<?> paramType2, Class<?> paramType3) {
                this(targetClass, methodName, null, new Class<?>[] { paramType1, paramType2, paramType3 });
            }

            /**
             * 생성자 (fieldName 없이 호출 가능)
             * <p>
             * <b>❗주의:</b> fieldName이 누락된 경우, fieldName이 있는 키와 서로 다른 키로 인식됩니다.
             * Setter 추론(Inference) 기능이 작동하지 않으므로, Map.get/put 등 명확한 메서드 호출 시에만 사용하십시오.
             * </p>
             *
             * @param targetClass 대상 클래스
             * @param methodName  메서드 이름(예: "getName", "setName")
             * @param paramType1  파라미터 타입1
             * @param paramType2  파라미터 타입2
             * @param paramType3  파라미터 타입3
             * @param paramType4  파라미터 타입4
             * @param paramTypes  파라미터 가변 인자(없을 경우 생략)
             */
            public MethodKey(Class<?> targetClass, String methodName, Class<?> paramType1, Class<?> paramType2, Class<?> paramType3, Class<?> paramType4, Class<?>... paramTypes) {
                this(targetClass, methodName, null, combine(paramType1, paramType2, paramType3, paramType4, paramTypes));
            }

            /**
             * 생성자
             *
             * @param targetClass 대상 클래스
             * @param methodName  메서드 이름(예: "getName", "setName")
             * @param fieldName   필드 이름 (Setter 검색 실패 시 필드 타입 추론용, null 가능)
             */
            public MethodKey(Class<?> targetClass, String methodName, String fieldName) {
                this(targetClass, methodName, fieldName, EMPTY_TYPES);
            }

            /**
             * 생성자
             *
             * @param targetClass 대상 클래스
             * @param methodName  메서드 이름(예: "getName", "setName")
             * @param fieldName   필드 이름 (Setter 검색 실패 시 필드 타입 추론용, null 가능)
             * @param paramType   파라미터 타입
             */
            public MethodKey(Class<?> targetClass, String methodName, String fieldName, Class<?> paramType) {
                this(targetClass, methodName, fieldName, new Class<?>[] { paramType });
            }

            /**
             * 생성자
             *
             * @param targetClass 대상 클래스
             * @param methodName  메서드 이름(예: "getName", "setName")
             * @param fieldName   필드 이름 (Setter 검색 실패 시 필드 타입 추론용, null 가능)
             * @param paramType1  파라미터 타입1
             * @param paramType2  파라미터 타입2
             */
            public MethodKey(Class<?> targetClass, String methodName, String fieldName, Class<?> paramType1, Class<?> paramType2) {
                this(targetClass, methodName, fieldName, new Class<?>[] { paramType1, paramType2 });
            }

            /**
             * 생성자
             *
             * @param targetClass 대상 클래스
             * @param methodName  메서드 이름(예: "getName", "setName")
             * @param fieldName   필드 이름 (Setter 검색 실패 시 필드 타입 추론용, null 가능)
             * @param paramType1  파라미터 타입1
             * @param paramType2  파라미터 타입2
             * @param paramType3  파라미터 타입3
             */
            public MethodKey(Class<?> targetClass, String methodName, String fieldName, Class<?> paramType1, Class<?> paramType2, Class<?> paramType3) {
                this(targetClass, methodName, fieldName, new Class<?>[] { paramType1, paramType2, paramType3 });
            }

            /**
             * 생성자
             *
             * @param targetClass 대상 클래스
             * @param methodName  메서드 이름(예: "getName", "setName")
             * @param fieldName   필드 이름 (Setter 검색 실패 시 필드 타입 추론용, null 가능)
             * @param paramType1  파라미터 타입1
             * @param paramType2  파라미터 타입2
             * @param paramType3  파라미터 타입3
             * @param paramType4  파라미터 타입4
             * @param paramTypes  파라미터 가변 인자(없을 경우 생략)
             */
            public MethodKey(Class<?> targetClass, String methodName, String fieldName, Class<?> paramType1, Class<?> paramType2, Class<?> paramType3, Class<?> paramType4, Class<?>... paramTypes) {
                this(targetClass, methodName, fieldName, combine(paramType1, paramType2, paramType3, paramType4, paramTypes));
            }

            /**
             * Combines multiple individual parameter types and a variadic array into a single array.
             *
             * <p>
             * <b>[한국어 설명]</b>
             * </p>
             * 개별적으로 전달된 파라미터 타입들과 가변 인자 배열을 하나의 배열로 통합합니다.
             *
             * @param p1   First parameter type
             * @param p2   Second parameter type
             * @param p3   Third parameter type
             * @param p4   Fourth parameter type
             * @param rest Additional variadic parameter types
             * @return A consolidated array of parameter types
             */
            private static Class<?>[] combine(Class<?> p1, Class<?> p2, Class<?> p3, Class<?> p4, Class<?>... rest) {
                var combined = new Class<?>[4 + (rest != null ? rest.length : 0)];
                combined[0] = p1;
                combined[1] = p2;
                combined[2] = p3;
                combined[3] = p4;
                if (rest != null && rest.length > 0) {
                    System.arraycopy(rest, 0, combined, 4, rest.length);
                }
                return combined;
            }

            /**
             * 콤팩트 생성자를 통한 유효성 검사 및 방어적 복사
             */
            public MethodKey {
                Objects.requireNonNull(targetClass, "[MethodKey] targetClass는 필수 입력 값입니다.");
                if (methodName == null || methodName.isBlank()) {
                    throw new IllegalArgumentException("[MethodKey] methodName은 필수 입력 값입니다.");
                }
                paramTypes = (paramTypes == null) ? EMPTY_TYPES : paramTypes.clone();
            }

            // record에서 배열 필드는 기본 equals/hashCode가 참조 비교를 수행하므로,
            // 내용 기반 비교를 위해 명시적으로 재정의한다. (성능 최적화된 기존 로직 계승)

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (!(o instanceof MethodKey that)) {
                    return false;
                }
                // Class 객체는 JVM 내에서 싱글톤이므로 '==' 비교가 가장 빠름
                return targetClass == that.targetClass &&
                        methodName.equals(that.methodName) &&
                        Objects.equals(fieldName, that.fieldName) &&
                        Arrays.equals(paramTypes, that.paramTypes);
            }

            @Override
            public int hashCode() {
                // Objects.hash() 대신 오버헤드가 적은 직접 계산 방식 유지
                int result = targetClass.hashCode();
                result = 31 * result + methodName.hashCode();
                result = 31 * result + (fieldName != null ? fieldName.hashCode() : 0);
                result = 31 * result + Arrays.hashCode(paramTypes);
                return result;
            }

        }

        /**
         * Resolves a MethodHandle based on the key and lookup type.
         * <p>
         * First checks for fast-pass targets, and scans for the appropriate member if not cached.
         * Managed via {@link S2Cache#resolve} for centralized cache storage.
         * </p>
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 지정된 키와 조회 타입을 기반으로 MethodHandle을 조회합니다. (캐싱 적용)
         * <p>
         * 먼저 하이패스 대상을 조회하며, 캐시에 없을 경우 적절한 대상을 스캔하여 결과를 반환합니다.
         * 내부적으로 {@link S2Cache#resolve}를 사용하여 공통 캐시 저장소에서 관리합니다.
         * </p>
         *
         * @param key  Identification key for method or field | 메서드 또는 필드 식별 정보
         * @param type Search scope (METHOD, FIELD, or BOTH) | 검색 대상 범위 (METHOD, FIELD, BOTH 중 하나)
         * @return An Optional containing the resolved MethodHandle | 검색된 MethodHandle을 담은 Optional 객체
         */
        private static Optional<MethodHandle> resolve(MethodKey key, LookupType type) {
            if (key == null)
                return Optional.empty();

            // 캐시 접근 전 즉시 반환 가능한 핸들이 있는지 확인
            MethodHandle fastHandle = getFastHandle(key);
            if (fastHandle != null)
                return Optional.of(fastHandle);

            // 프록시 체크 및 키 최적화
            Class<?> realClass = getRealClass(key.targetClass());
            MethodKey refinedKey = (realClass != null && realClass != key.targetClass())
                    ? new MethodKey(realClass, key.methodName(), key.fieldName(), key.paramTypes())
                    : key;

            // 람다 외부에서 미리 필요한 값을 확정(final)
            final boolean isNotField = (type != LookupType.FIELD);
            final boolean isNotMethod = (type != LookupType.METHOD);
            final boolean allowPrivate = isNotMethod; // METHOD 타입이 아니면 private 허용

            return CACHE.get(refinedKey, k -> {
                // 캡처된 변수(isNotField, isNotMethod, allowPrivate)를 직접 사용하여 호출 스택을 줄임
                if (isNotField) {
                    MethodHandle methodHandle = scanMethod(k, allowPrivate);
                    if (methodHandle != null)
                        return Optional.of(methodHandle);
                }

                if (isNotMethod) {
                    MethodHandle fieldHandle = scanField(k);
                    if (fieldHandle != null)
                        return Optional.of(fieldHandle);
                }

                return Optional.empty();
            });
        }

        /**
         * Quickly retrieves fast-pass targets (standard methods of common classes).
         * Returns handles for common methods in Map, List, Collection, and Object directly.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 하이패스 대상(자주 사용되는 표준 클래스의 메서드)을 캐시 접근 없이 빠르게 조회합니다.
         * Map, List, Collection 및 Object의 기본 메서드들에 대한 핸들을 즉시 반환합니다.
         *
         * @param key Member identification info | 메서드 식별 정보
         * @return Pre-defined MethodHandle, or null if not applicable | 미리 정의된 MethodHandle, 해당 사항이 없으면 null 반환
         */
        private static MethodHandle getFastHandle(MethodKey key) {
            Class<?> clazz = key.targetClass();
            String methodName = key.methodName();
            int paramTypeLength = key.paramTypes().length;

            // 컬렉션 프레임워크 관련 인터페이스 및 구현체인지 확인함
            if (Map.class.isAssignableFrom(clazz)) {
                if ("get".equals(methodName) && paramTypeLength == 1)
                    return MAP_GET;
                if ("put".equals(methodName) && paramTypeLength == 2)
                    return MAP_PUT;
                if ("containsKey".equals(methodName) && paramTypeLength == 1)
                    return MAP_CONTAINS;
            } else if (List.class.isAssignableFrom(clazz) && "get".equals(methodName) && paramTypeLength == 1) {
                Class<?> paramType = key.paramTypes()[0];
                if (paramType == int.class || paramType == Integer.class) {
                    return LIST_GET;
                }
            } else if (Collection.class.isAssignableFrom(clazz) && "size".equals(methodName) && paramTypeLength == 0) {
                return COLLECTION_SIZE;
            }

            // 기본 Object 메서드 및 Optional 관련 처리를 수행함
            if ("toString".equals(methodName) && paramTypeLength == 0)
                return OBJECT_TO_STRING;
            if (clazz == Optional.class) {
                if ("get".equals(methodName))
                    return OPTIONAL_GET;
                if ("isPresent".equals(methodName))
                    return OPTIONAL_IS_PRESENT;
            }

            return null;
        }

        /**
         * Scans for a method based on the key.
         * Performs sequential searches: normal search, type-conversion-based search, and field-type-inference-based search.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 키 정보를 기반으로 메서드를 스캔합니다.
         * 일반 검색, 타입 변환 기반 검색, 필드 타입 추론 기반 검색을 순차적으로 수행합니다.
         *
         * @param key          Member identification info | 메서드 식별 정보
         * @param allowPrivate Whether to allow private access | private 메서드 접근 허용 여부
         * @return Resolved MethodHandle, or null if not found | 검색된 MethodHandle, 찾지 못한 경우 null 반환
         */
        private static MethodHandle scanMethod(MethodKey key, boolean allowPrivate) {
            Class<?> clazz = key.targetClass();

            // 1. 일반적인 방식으로 메서드를 검색
            MethodHandle methodHandle = findMethodRecursive(key, allowPrivate);
            if (methodHandle != null)
                return methodHandle;

            // 2. 파라미터가 있는 경우 래퍼/기본 타입 변환 후 재시도
            if (key.paramTypes().length > 0) {
                methodHandle = findWithConvertedTypes(key, allowPrivate);
                if (methodHandle != null)
                    return methodHandle;
            }

            // 3. 세터(Setter)의 경우 필드 타입을 추론하여 검색을 시도
            if (key.fieldName() != null && key.paramTypes().length == 1) {
                Class<?> fieldType = findFieldTypeRecursive(clazz, key.fieldName());
                if (fieldType != null && fieldType != key.paramTypes()[0]) {
                    MethodKey inferredKey = new MethodKey(clazz, key.methodName(), key.fieldName(), new Class<?>[] { fieldType });
                    methodHandle = findMethodRecursive(inferredKey, allowPrivate);
                    if (methodHandle != null)
                        return methodHandle;
                }
            }

            return null;
        }

        /**
         * Recursively searches for a method up the class hierarchy.
         * Handles constructor requests if methodName is {@code "<init>"}, otherwise searches for matching declared methods.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 클래스 계층 구조를 올라가며 재귀적으로 메서드를 조회합니다.
         * 생성자 요청 시 생성자를 생성하며, 일반 메서드는 선언된 메서드에서 매칭되는 항목을 찾습니다.
         *
         * @param key          Member identification info | 메서드 식별 정보
         * @param allowPrivate Whether to allow private access | 프라이빗 접근 허용 여부
         * @return Resolved MethodHandle, or null if lookup fails | 검색된 MethodHandle, 검색 실패 시 null 반환
         */
        private static MethodHandle findMethodRecursive(MethodKey key, boolean allowPrivate) {
            Class<?> current = key.targetClass();

            if ("<init>".equals(key.methodName()))
                // 생성자 요청 처리
                return createConstructor(current, key.paramTypes(), allowPrivate);

            if (allowPrivate) {
                /*
                 * [Private Access Mode] Private/Protected 를 포함한 모든 메서드를 찾는다.
                 * getDeclaredMethod(): 해당 클래스에만 선언된 메서드만 탐색하므로 상위 클래스까지 검색한다.
                 *
                 * private/protected 메서드는 상속 계층에서 자동으로 검색되지 않으므로
                 * getDeclaredMethod를 사용하여 부모 클래스 방향으로 수동 스캔을 수행한다.
                 * 성공 시 즉시 반환하여 검색 효율을 높인다.
                 */
                while (current != null && current != Object.class) {
                    try {
                        Method method = current.getDeclaredMethod(key.methodName(), key.paramTypes());
                        return createHandle(method, current, allowPrivate);
                    } catch (NoSuchMethodException e) {
                        // 해당 클래스에 없으면 상위 클래스로 이동하여 재시도함
                        current = current.getSuperclass();
                    }
                }
                return null;
            } else {
                /*
                 * [Public Only Mode - false] Public 메서드만 찾는다.
                 * getMethod(): 해당 클래스 및 모든 상위 클래스와 인터페이스를 탐색한다.
                 *
                 * 실무 환경에서는 존재하는 메서드를 호출할 확률이 압도적으로 높다.
                 * 따라서 루프를 통한 수동 검색보다 JVM 네이티브 레벨에서 최적화된 getMethod()를 호출하는 것이
                 * 성공 케이스(Fast-Path)에서 훨씬 유리하다.
                 * 실패 시 발생하는 예외 비용은 Optional.empty() 캐싱을 통해 최초 1회로 제한한다.
                 * ※ 실패할 확률이 높은 상황에서는 getMethods() 를 사용하는 것이 좋을 수 있다.
                 */
                try {
                    Method method = current.getMethod(key.methodName(), key.paramTypes());
                    return createHandle(method, current, allowPrivate);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            }
        }

        /**
         * Searches for a method considering autoboxing/unboxing between primitive and wrapper types.
         * Retries the search by converting types based on mapping rules.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 기본형(Primitive)과 래퍼(Wrapper) 타입 간의 오토박싱/언박싱을 고려하여 메서드를 검색합니다.
         * 오토박싱/언박싱 상황에 대응하기 위해 각 맵을 기준으로 타입을 변환하여 재검색합니다.
         *
         * @param key          Member identification info | 메서드 식별 정보
         * @param allowPrivate Whether to allow private access | 프라이빗 접근 허용 여부
         * @return Resolved MethodHandle, or null if lookup fails | 변환 후 검색된 MethodHandle, 실패 시 null 반환
         */
        private static MethodHandle findWithConvertedTypes(MethodKey key, boolean allowPrivate) {
            // 래퍼 타입을 기본 타입으로 변환하여 조회를 시도함
            Class<?>[] convertType = convertTypes(key.paramTypes(), WRAPPER_TO_PRIMITIVE_MAP);
            MethodHandle methodHandle = findMethodRecursive(new MethodKey(key.targetClass(), key.methodName(), key.fieldName(), convertType), allowPrivate);
            if (methodHandle != null)
                return methodHandle;

            // 기본 타입을 래퍼 타입으로 변환하여 조회를 시도함
            convertType = convertTypes(key.paramTypes(), PRIMITIVE_TO_WRAPPER_MAP);
            return findMethodRecursive(new MethodKey(key.targetClass(), key.methodName(), key.fieldName(), convertType), allowPrivate);
        }

        /**
         * Converts an array of types to their corresponding counterparts defined in the map.
         * Keeps the original type if no mapping is found.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 주어진 타입 배열을 맵에 정의된 대응 타입으로 변환합니다.
         * 대응하는 타입이 맵에 없는 경우 원본 타입을 유지합니다.
         *
         * @param original Original array of class types | 원본 클래스 타입 배열
         * @param map      Mapping rules | 변환 규칙을 담은 맵
         * @return Converted array of class types | 변환이 완료된 클래스 타입 배열
         */
        private static Class<?>[] convertTypes(Class<?>[] original, Map<Class<?>, Class<?>> map) {
            Class<?>[] converted = new Class<?>[original.length];
            for (int i = 0; i < original.length; i++) {
                // 맵에서 변환 대상을 찾고 없으면 기존 타입 그대로 유지함
                converted[i] = map.getOrDefault(original[i], original[i]);
            }
            return converted;
        }

        /**
         * Traversing the class hierarchy to find the type of a specific field.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 클래스 계층 구조를 순회하며 특정 필드의 타입을 찾습니다.
         *
         * @param clazz     Target class | 대상 클래스
         * @param fieldName Field name | 필드 이름
         * @return Field type, or null if not found | 찾은 필드의 Class 타입, 없으면 null 반환
         */
        private static Class<?> findFieldTypeRecursive(Class<?> clazz, String fieldName) {
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                try {
                    return current.getDeclaredField(fieldName).getType();
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }
            return null;
        }

        /**
         * Scans for a field by traversing the class hierarchy.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 클래스 계층 구조를 순회하며 조건에 맞는 필드를 스캔합니다.
         * 이름이 일치하는 필드를 찾을 때까지 상위 클래스로 올라가며 검색을 수행합니다.
         *
         * @param key Field identification info | 필드 식별 정보
         * @return MethodHandle for the field (getter), or null if not found | 검색된 MethodHandle(Getter), 찾지 못한 경우 null 반환
         */
        private static MethodHandle scanField(MethodKey key) {
            Class<?> current = key.targetClass();

            // Object 클래스에 도달할 때까지 필드 조회를 반복
            while (current != null && current != Object.class) {
                try {
                    Field f = current.getDeclaredField(key.methodName());
                    return createFieldHandle(f, current);
                } catch (NoSuchFieldException e) {
                    // 현재 클래스에 없으면 부모 클래스로 이동
                    current = current.getSuperclass();
                }
            }
            return null;
        }

        /**
         * Creates a MethodHandle from a Method object.
         * Enables accessibility and performs lookup based on privacy settings.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * Method 객체를 이용해 MethodHandle을 생성합니다.
         * 접근 권한을 활성화한 후, 설정에 따라 일반 또는 프라이빗 Lookup을 수행합니다.
         *
         * @param method       Target method object | 대상 메서드 객체
         * @param clazz        Target class | 대상 클래스
         * @param allowPrivate Whether to allow private access | 프라이빗 접근 허용 여부
         * @return Created MethodHandle, or null if creation fails | 생성된 MethodHandle, 실패 시 null 반환
         */
        private static MethodHandle createHandle(Method method, Class<?> clazz, boolean allowPrivate) {
            try {
                // 리플렉션 접근 권한을 강제로 허용함
                method.setAccessible(true);
                var lookup = allowPrivate ? MethodHandles.privateLookupIn(clazz, MethodHandles.lookup()) : MethodHandles.lookup();
                return lookup.unreflect(method);
            } catch (Exception e) {
                logger.error("메서드 핸들 생성을 실패하였습니다: {}", method.getName());
                return null;
            }
        }

        /**
         * Creates a MethodHandle for a getter from a Field object.
         * Returns a handle that can read the field's value even with private access.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * Field 객체를 이용해 Getter 목적의 MethodHandle을 생성합니다.
         * 프라이빗 조회를 통해 대상 필드의 값을 읽을 수 있는 핸들을 반환합니다.
         *
         * @param f     Target field object | 대상 필드 객체
         * @param clazz Target class | 대상 클래스
         * @return Getter MethodHandle, or null if creation fails | 필드 Getter 핸들, 실패 시 null 반환
         */
        private static MethodHandle createFieldHandle(Field f, Class<?> clazz) {
            try {
                // 필드 접근 권한을 강제로 허용함
                f.setAccessible(true);
                return MethodHandles.privateLookupIn(clazz, MethodHandles.lookup()).unreflectGetter(f);
            } catch (Exception e) {
                logger.error("필드 핸들 생성을 실패하였습니다: {}", f.getName());
                return null;
            }
        }

        /**
         * Creates a MethodHandle for a class constructor.
         * Constructs the method type based on parameter types and performs lookup.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 클래스 생성자에 접근하는 MethodHandle을 생성합니다.
         * 파라미터 타입을 기반으로 생성자 시그니처를 구성하고 적절한 Lookup을 시도합니다.
         *
         * @param clazz Target class | 대상 클래스
         * @param pts   Constructor parameter types | 생성자 파라미터 타입 배열
         * @param priv  Whether to allow private access | 프라이빗 접근 허용 여부
         * @return Constructor MethodHandle, or null if creation fails | 생성자 MethodHandle, 실패 시 null 반환
         */
        private static MethodHandle createConstructor(Class<?> clazz, Class<?>[] pts, boolean priv) {
            try {
                // 생성자는 반환 타입을 void로 지정하여 타입을 구성함
                var type = MethodType.methodType(void.class, pts);
                var lookup = priv ? MethodHandles.privateLookupIn(clazz, MethodHandles.lookup()) : MethodHandles.lookup();
                return lookup.findConstructor(clazz, type);
            } catch (Exception e) {
                // 생성자를 찾지 못한 경우 null을 반환함
                return null;
            }
        }
    }

    /**
     * Resolver for compiled {@link java.util.regex.Pattern}s.
     * <p>
     * Regex compilation is a CPU-intensive operation. This resolver ensures that
     * frequently used patterns are compiled once and reused across the system.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 정규표현식 Pattern 리졸버입니다.
     * <p>
     * 정규식 컴파일은 비용이 많이 드는 작업이므로, 빈번하게 사용되는 패턴을 캐싱하여 시스템 부하를 줄입니다.
     * </p>
     */
    public static class PatternResolver {
        private static final CacheAdapter<String, Optional<Pattern>> CACHE = createCacheAdapter(1000, 0, "PatternResolver");

        /**
         * Retrieves a cached Pattern object for the given regex string.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 정규식 문자열을 통해 캐시된 Pattern 객체를 가져옵니다.
         *
         * @param regex Regular expression string | 정규식 문자열
         * @return Compiled Pattern object wrapped in an Optional | 컴파일된 Pattern 객체를 포함한 Optional
         */
        private static Optional<Pattern> resolve(String regex) {
            if (regex == null || regex.isBlank()) {
                return Optional.empty();
            }
            return CACHE.get(regex, k -> {
                try {
                    return Optional.of(Pattern.compile(k));
                } catch (PatternSyntaxException e) {
                    logger.warn("Invalid pattern: {}", k);
                    return Optional.empty();
                }
            });
        }
    }

    /**
     * Resolver for {@link java.util.ResourceBundle}s.
     * <p>
     * Automatically loads and caches ResourceBundles based on basename and locale.
     * Implements negative caching for missing resources to prevent repetitive lookups.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * ResourceBundle 리졸버입니다.
     * <p>
     * basename과 locale을 기반으로 ResourceBundle을 자동으로 로드 및 캐싱합니다.
     * 존재하지 않는 리소스에 대해서도 결과를 캐싱(Negative Caching)하여 반복적인 탐색 부하를 방지합니다.
     * </p>
     */
    public static class ResourceBundleResolver {

        /**
         * Immutable key record for identifying resource bundles.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 리소스 번들 식별을 위한 고유 키 레코드입니다.
         *
         * @param basename Bundle path | 번들 경로
         * @param locale   Locale information | 로케일 정보
         */
        private record BundleKey(String basename, Locale locale) {}

        /**
         * Retrieves a cached {@link java.util.ResourceBundle}.
         * <p>
         * While {@link java.util.ResourceBundle#getBundle} provides internal caching, it does not
         * cache "missing" resources. This resolver implements <b>Negative Caching</b> by storing
         * {@link Optional#empty()} when a bundle is not found, significantly reducing file system
         * or classpath traversal overhead for repeated missing bundle checks.
         * </p>
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * {@link java.util.ResourceBundle}을 캐싱하여 반환합니다.
         * <p>
         * 표준 API는 '존재하지 않는 번들'에 대한 캐싱을 제공하지 않아 반복적인 예외 및 탐색 부하가 발생합니다.
         * 본 리졸버는 네거티브 캐싱(Negative Caching)을 통해 부하를 최소화합니다.
         * </p>
         *
         * @param basename Base name of the resource bundle (e.g. "messages/validation") | 리소스 번들 기본 경로
         * @param locale   Target locale (defaults to {@link Locale#getDefault()} if null) | 대상 로케일 (null 인 경우 기본값 사용)
         * @return An Optional containing the bundle if it exists | 번들이 존재할 경우 이를 포함한 Optional
         */
        @SuppressWarnings("null")
        public static Optional<ResourceBundle> resolve(String basename, Locale locale) {
            if (basename == null || basename.isBlank()) {
                return Optional.empty();
            }
            Locale targetLocale = locale != null ? locale : Locale.getDefault();

            return S2Cache.resolve(
                    RESOURCE_BUNDLE_CACHE_NAME,
                    new BundleKey(basename, targetLocale),
                    BundleKey.class,
                    ResourceBundle.class,
                    100,
                    key -> {
                        try {
                            return Optional.of(ResourceBundle.getBundle(key.basename(), key.locale()));
                        } catch (MissingResourceException e) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                        "[S2Cache] ResourceBundle을 찾을 수 없음: {} (locale: {}) - empty 결과 캐싱함",
                                        key.basename(), key.locale()
                                );
                            }
                            return Optional.empty();
                        }
                    }
            );
        }
    }

}
