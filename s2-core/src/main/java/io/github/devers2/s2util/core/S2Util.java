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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.github.devers2.s2util.core.S2Cache.MethodHandleResolver;
import io.github.devers2.s2util.core.S2Cache.MethodHandleResolver.LookupType;
import io.github.devers2.s2util.core.S2Cache.MethodHandleResolver.MethodKey;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * Core utility class for the S2Util library, providing high-performance object manipulation.
 * <p>
 * This class serves as the central hub for dynamic data access, intelligent type conversion,
 * and object graph traversal. It is engineered with a "Zero-Reflection" philosophy to
 * overcome the performance bottlenecks of standard Java Reflection.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리의 핵심 유틸리티 클래스입니다.
 * <p>
 * 동적 데이터 접근, 지능형 타입 변환, 객체 그래프 탐색 등 프레임워크 전반의 핵심 기능을 제공합니다.
 * 표준 Java 리플렉션({@code java.lang.reflect})의 성능한계를 극복하기 위해 '리플렉션 제로' 철학을 기반으로 설계되었습니다.
 * </p>
 *
 * <h3>Technical Excellence (기술적 장점)</h3>
 * <ul>
 * <li><b>MethodHandle-Based Execution:</b> Internally utilizes {@link java.lang.invoke.MethodHandle}
 * which is optimized by the JVM's JIT compiler to perform near native calls.</li>
 * <li><b>Intelligent Traversal:</b> Supports both Dot Notation (e.g., {@code user.addr.zip}) and
 * Bracket Notation (e.g., {@code users[0].id}) for complex object graph navigation.</li>
 * <li><b>JPA/Hibernate Compatibility:</b> Automatically detects proxy objects to ensure that
 * updates are reflected in the persistence context (enabling <b>Dirty Checking</b>).</li>
 * <li><b>Type Safe Adaptability:</b> Provides a sophisticated {@code cast()} mechanism that handles
 * primitives, wrappers, and string-to-number conversions with fail-safe defaults.</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 * @see S2Cache
 * @see java.lang.invoke.MethodHandle
 */
public class S2Util {

    private static final S2Logger logger = S2LogManager.getLogger(S2Util.class);

    // [🔋 S2Util Core Initialization]
    static {
        // S2LogManager를 터치하여 라이브러리 사용 시 자동으로 경고 배너 타이머가 작동하도록 함
        @SuppressWarnings("unused")
        Class<?> trigger = io.github.devers2.s2util.log.S2LogManager.class;
    }

    private S2Util() {
        // Prevent instantiation
    }

    /**
     * Checks if the given locale is Korean.
     * <p>
     * If the locale is {@code null}, it is treated as Korean by default.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 로케일이 한국어인지 확인하는 메서드입니다.
     * <p>
     * 로케일 정보가 {@code null}인 경우에도 한국어로 간주합니다.
     * </p>
     *
     * @param locale The locale to check | 확인할 로케일
     * @return {@code true} if Korean or null, {@code false} otherwise | 한국어이거나 null이면 true, 아니면 false
     */
    public static boolean isKorean(Locale locale) {
        return locale == null || Locale.KOREAN.getLanguage().equals(locale.getLanguage());
    }

    /**
     * Checks if the given locale is Korean.
     * <p>
     * If the locale is {@code null}, it is treated as Korean by default.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 로케일이 한국어인지 확인하는 메서드입니다.
     * <p>
     * 로케일 정보가 {@code null}인 경우에도 한국어로 간주합니다.
     * </p>
     *
     * @return {@code true} if Korean, {@code false} otherwise | 한국어이면 true, 아니면 false
     */
    public static boolean isKorean() {
        return isKorean(Locale.getDefault());
    }

    /**
     * Extracts a value from the target object based on the given field name or path.
     * <p>
     * Instead of traditional reflection, this method uses cached {@link java.lang.invoke.MethodHandle}s
     * for near-native lookup performance. It intelligently handles various object types:
     * </p>
     *
     * <h3>Supported Access Patterns</h3>
     * <ul>
     * <li><b>Map/List/Array:</b> Key-based or Index-based lookup.</li>
     * <li><b>Nested Path:</b> {@code "user.address.street"} style navigation.</li>
     * <li><b>Bracket Notation:</b> {@code "items[0].name"} style navigation.</li>
     * <li><b>Java Records:</b> Automatic detection of record components.</li>
     * <li><b>JPA Proxies:</b> Transparent handling of lazy-loading proxies.</li>
     * </ul>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체(object)에서 필드명(fieldName) 또는 경로에 해당하는 값을 추출합니다.
     * <p>
     * 표준 리플렉션 대신 캐싱된 {@link java.lang.invoke.MethodHandle}을 사용하여 네이티브 호출에 근접한 성능을 제공합니다.
     * 단순 필드뿐만 아니라 중첩 경로 및 특수 객체 타입을 지능적으로 처리합니다.
     * </p>
     *
     * <h3>지원 패턴</h3>
     * <ul>
     * <li><b>Map/List/Array:</b> Key 조회, 인덱스 조회 및 수정</li>
     * <li><b>DTO/VO:</b> Getter/Setter 메서드를 우선 호출하며, 없을 경우 필드에 직접 접근. (상속된 필드 포함)</li>
     * <li><b>Java Record:</b> Record 특유의 컴포넌트 메서드(필드명과 동일한 메서드) 자동 인식</li>
     * <li><b>JPA/Hibernate 프록시:</b> 프록시 객체를 감지하여 가상 메서드가 아닌 실제 엔티티의 로직을 타도록 처리 (Dirty Checking 보장)</li>
     * <li><b>Optional:</b> 값이 Optional로 감싸져 있는 경우 자동으로 언래핑(Unwrapping)하여 탐색</li>
     * <li><b>중첩 경로:</b> user.address.street와 같이 점(.)으로 연결된 모든 깊이의 객체 그래프 탐색</li>
     * <li><b>인덱스 지원:</b> users[0].name 또는 matrix[1][2]와 같은 대괄호([]) 문법 지원</li>
     * </ul>
     *
     * @param <T>       The type of the target object | 대상 객체의 타입
     * @param <F>       The type of the field identifier (String or Number) | 필드 식별자 타입 (문자열 또는 숫자)
     * @param <R>       The expected return type | 예상되는 반환 타입
     * @param target    The object to query (Map, List, VO, Record, etc.) | 조회 대상 객체 (Map, List, VO, Record 등)
     * @param fieldName Field identifier or navigation path | 필드 식별자 또는 탐색 경로
     * @return The extracted value, or {@code null} if not found | 추출된 값 (찾지 못한 경우 null)
     */
    @SuppressWarnings("null")
    public static <T, F, R> R getValue(T target, F fieldName) {
        return getValue(target, fieldName, null, null);
    }

    /**
     * Extracts a value and casts it to the specified class.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체(object)에서 필드명(fieldName)에 해당하는 값을 추출하고 지정된 타입으로 형변환합니다.
     *
     * <h3>지원 패턴</h3>
     * <ul>
     * <li><b>Map/List/Array:</b> Key 조회, 인덱스 조회 및 수정</li>
     * <li><b>DTO/VO:</b> Getter/Setter 메서드를 우선 호출하며, 없을 경우 필드에 직접 접근. (상속된 필드 포함)</li>
     * <li><b>Java Record:</b> Record 특유의 컴포넌트 메서드(필드명과 동일한 메서드) 자동 인식</li>
     * <li><b>JPA/Hibernate 프록시:</b> 프록시 객체를 감지하여 가상 메서드가 아닌 실제 엔티티의 로직을 타도록 처리 (Dirty Checking 보장)</li>
     * <li><b>Optional:</b> 값이 Optional로 감싸져 있는 경우 자동으로 언래핑(Unwrapping)하여 탐색</li>
     * <li><b>중첩 경로:</b> user.address.street와 같이 점(.)으로 연결된 모든 깊이의 객체 그래프 탐색</li>
     * <li><b>인덱스 지원:</b> users[0].name 또는 matrix[1][2]와 같은 대괄호([]) 문법 지원</li>
     * </ul>
     *
     * @param <T>       The type of the target object | 대상 객체의 타입
     * @param <F>       The type of the field identifier | 필드 식별자 타입
     * @param <R>       The expected return type | 예상되는 반환 타입
     * @param target    The object to query | 조회 대상 객체
     * @param fieldName Field identifier or navigation path | 필드 식별자 또는 탐색 경로
     * @param castClass The Class to cast to (no casting if {@code null}) | 형변환할 클래스 (null이면 형변환하지 않음)
     * @return The extracted value | 추출된 값
     */
    @SuppressWarnings("null")
    public static <T, F, R> R getValue(T target, F fieldName, Class<R> castClass) {
        return getValue(target, fieldName, castClass, null);
    }

    /**
     * Extracts a value and returns a default value if {@code null} or an error occurs.
     *
     * <h3>Supported Access Patterns</h3>
     * <ul>
     * <li><b>Map/List/Array:</b> Key-based or Index-based lookup.</li>
     * <li><b>Nested Path:</b> {@code "user.address.street"} style navigation.</li>
     * <li><b>Bracket Notation:</b> {@code "items[0].name"} style navigation.</li>
     * <li><b>Java Records:</b> Automatic detection of record components.</li>
     * <li><b>JPA Proxies:</b> Transparent handling of lazy-loading proxies.</li>
     * </ul>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체(object)에서 필드명(fieldName)에 해당하는 값을 추출하며, {@code null}이거나 오류 발생 시 기본값을 반환합니다.
     *
     * @param <T>          The type of the target object | 대상 객체의 타입
     * @param <F>          The type of the field identifier | 필드 식별자 타입
     * @param <R>          The expected return type | 예상되는 반환 타입
     * @param target       The object to query | 조회 대상 객체
     * @param fieldName    Field identifier or navigation path | 필드 식별자 또는 탐색 경로
     * @param defaultValue Default value to return if extraction fails | 추출 실패 시 반환할 기본값
     * @return Extracted value or default value | 추출된 값 또는 기본값
     */
    @SuppressWarnings("null")
    public static <T, F, R> R getValue(T target, F fieldName, R defaultValue) {
        return getValue(target, fieldName, null, defaultValue);
    }

    /**
     * 대상 객체(object)에서 필드명(fieldName)에 해당하는 값을 추출한다.
     * <p>
     * 기존의 리플렉션(Reflection) 방식 대신 {@link java.lang.invoke.MethodHandle}을 캐싱하여 사용함으로써
     * 반복적인 호출 시 오버헤드를 최소화하고 네이티브에 가까운 조회 성능을 제공한다.
     * </p>
     *
     * <p>
     * [지원하는 조회 대상]
     * <ul>
     * <li><b>Map/List/Array:</b> Key 조회, 인덱스 조회 및 수정</li>
     * <li><b>DTO/VO:</b> Getter/Setter 메서드를 우선 호출하며, 없을 경우 필드에 직접 접근. (상속된 필드 포함)</li>
     * <li><b>Java Record:</b> Record 특유의 컴포넌트 메서드(필드명과 동일한 메서드) 자동 인식</li>
     * <li><b>JPA/Hibernate 프록시:</b> 프록시 객체를 감지하여 가상 메서드가 아닌 실제 엔티티의 로직을 타도록 처리 (Dirty Checking 보장)</li>
     * <li><b>Optional:</b> 값이 Optional로 감싸져 있는 경우 자동으로 언래핑(Unwrapping)하여 탐색</li>
     * <li><b>중첩 경로:</b> user.address.street와 같이 점(.)으로 연결된 모든 깊이의 객체 그래프 탐색</li>
     * <li><b>인덱스 지원:</b> users[0].name 또는 matrix[1][2]와 같은 대괄호([]) 문법 지원</li>
     * </ul>
     * </p>
     *
     * @param <T>          The type of the target object | 대상 객체의 타입
     * @param <F>          The type of the field identifier | 필드 식별자 타입
     * @param <R>          The expected return type | 예상되는 반환 타입
     * @param target       The object to query | 조회 대상 객체
     * @param fieldName    Field identifier or navigation path | 필드 식별자 또는 탐색 경로
     * @param castClass    The Class to cast to (no casting if {@code null}) | 형변환할 클래스 (null이면 형변환하지 않음)
     * @param defaultValue Default value to return if extraction fails | 추출 실패 시 반환할 기본값
     * @return Extracted value or default value | 추출된 값 또는 기본값
     */
    @SuppressWarnings("null")
    private static <T, F, R> R getValue(T target, F fieldName, Class<R> castClass, R defaultValue) {
        if (target == null || fieldName == null) {
            return defaultValue;
        }

        Object value = null;
        try {
            // 0. Dot Notation (Nested Access) & Bracket Notation ([0]) 처리
            if (fieldName instanceof String s) {
                // Bracket Notation Normalization: "users[0]" -> "users.0"
                if (s.indexOf('[') > -1) {
                    s = S2StringUtil.replaceAll(s, "\\[(\\d+)\\]", ".$1"); // [0] -> .0
                    // .으로 시작하면 제거 (.0 -> 0) - 루트 배열 접근 시
                    if (s.startsWith("."))
                        s = s.substring(1);
                }

                if (s.indexOf('.') > -1) {
                    String[] parts = s.split("\\.");
                    Object current = target;
                    for (String part : parts) {
                        if (current == null)
                            return defaultValue;
                        current = getValue(current, part, Object.class, null);
                    }
                    value = current;
                }
            }

            // 위에서 처리되지 않은 경우 (Dot 없음)
            if (value == null) {
                // 1. Optional 처리: 값이 존재하면 언래핑하여 재귀적으로 탐색함
                Object currentTarget = target;
                while (currentTarget instanceof Optional<?> opt) {
                    if (opt.isEmpty()) {
                        return defaultValue;
                    }
                    currentTarget = opt.get();
                }

                final Object finalTarget = currentTarget;

                // 2. Map 처리: S2Cache에 사전 정의된 MAP_GET_KEY를 사용하여 하이패스(getFastHandle)를 태운다.
                if (finalTarget instanceof Map) {
                    value = S2Cache.getMethodHandle(
                            MethodHandleResolver.MAP_GET_KEY,
                            LookupType.METHOD
                    ).map(h -> {
                        try {
                            return h.invoke(finalTarget, fieldName);
                        } catch (Throwable t) {
                            return null;
                        }
                    }).orElse(null);
                }
                // 3. List Index 처리: List 인터페이스의 get 메서드를 resolve 하여 하이패스를 유도
                // fieldName이 Number이거나, 숫자형 문자열인 경우 처리
                else if (finalTarget instanceof List) {
                    Integer index = null;
                    if (fieldName instanceof Number num) {
                        index = num.intValue();
                    } else if (fieldName instanceof String str && str.matches("\\d+")) {
                        index = Integer.parseInt(str);
                    }

                    if (index != null) {
                        var key = new MethodKey(List.class, "get", int.class);
                        int finalIndex = index;
                        value = S2Cache.getMethodHandle(key, LookupType.METHOD)
                                .map(h -> {
                                    try {
                                        return h.invoke(finalTarget, finalIndex);
                                    } catch (Throwable t) {
                                        return null;
                                    }
                                }).orElse(null);
                    }
                }
                // 4. Collection Size 처리: Collection 인터페이스의 size 메서드를 resolve
                else if (finalTarget instanceof Collection && ("size".equals(fieldName) || "length".equals(fieldName))) {
                    var key = new MethodKey(Collection.class, "size");
                    value = S2Cache.getMethodHandle(key, LookupType.METHOD)
                            .map(h -> {
                                try {
                                    return h.invoke(finalTarget);
                                } catch (Throwable t) {
                                    return null;
                                }
                            }).orElse(null);
                }
                // 5. Array 처리
                else if (finalTarget.getClass().isArray()) {
                    if (fieldName instanceof String s && ("length".equals(s) || "size".equals(s))) {
                        value = java.lang.reflect.Array.getLength(finalTarget);
                    } else {
                        Integer index = null;
                        if (fieldName instanceof Number num) {
                            index = num.intValue();
                        } else if (fieldName instanceof String str && str.matches("\\d+")) {
                            index = Integer.parseInt(str);
                        }

                        if (index != null) {
                            value = java.lang.reflect.Array.get(finalTarget, index);
                        } else {
                            value = null;
                        }
                    }
                }
                // 6. Record 및 일반 VO 처리
                else {
                    Class<?> clazz = finalTarget.getClass();
                    String fieldNameStr = fieldName instanceof String s ? s : String.valueOf(fieldName); // String 캐스트 최적화 (instanceof 체크 후 직접 사용)

                    // 6-1. Record는 필드명과 동일한 메서드를 가짐 (METHOD 타입 즉시 조회)
                    if (clazz.isRecord()) {
                        value = S2Cache.getMethodHandle(
                                new MethodKey(clazz, fieldNameStr),
                                LookupType.METHOD
                        ).map(h -> {
                            try {
                                return h.invoke(finalTarget);
                            } catch (Throwable t) {
                                return null;
                            }
                        }).orElse(null);
                    }
                    // 6-2. 일반 VO는 Getter(get+Name)를 우선 탐색하고, 실패 시 필드 직접 접근(BOTH)으로 폴백한다.
                    else {
                        String getterName = "get" + Character.toUpperCase(fieldNameStr.charAt(0)) + fieldNameStr.substring(1);

                        var methodHandle = S2Cache.getMethodHandle(
                                new MethodKey(clazz, getterName),
                                LookupType.METHOD
                        ).or(
                                () -> S2Cache.getMethodHandle(
                                        new MethodKey(clazz, fieldNameStr),
                                        LookupType.BOTH
                                )
                        ).orElse(null);

                        if (methodHandle != null) {
                            value = methodHandle.invoke(finalTarget);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            if (logger.isDebugEnabled()) {
                if (isKorean()) {
                    logger.debug(
                            "[S2Util] getValue 실행 중 오류가 발생했습니다. (Target: {}, Field: {}). 기본값 {}을(를) 반환합니다.",
                            target.getClass().getSimpleName(), fieldName, defaultValue
                    );
                } else {
                    logger.debug(
                            "[S2Util] Error occurred during getValue. (Target: {}, Field: {}). Returning default value {}.",
                            target.getClass().getSimpleName(), fieldName, defaultValue
                    );
                }
            }
            return defaultValue; // 실패 시 defaultValue 반환으로 변경 (원본은 value가 null이면 cast에서 처리되지만, 여기서 일관성 위해 직접 반환)
        }

        return cast(value, castClass, defaultValue);
    }

    /**
     * Sets a value to the target object's field or navigation path.
     * <p>
     * Engineered for high-throughput data binding. When dealing with JPA Entities, it
     * ensures that update operations are performed through Setter methods (where available),
     * maintaining compatibility with <b>Dirty Checking</b> mechanisms.
     * </p>
     *
     * <h3>Supported Access Patterns</h3>
     * <ul>
     * <li><b>Map/List/Array:</b> Key-based or Index-based lookup.</li>
     * <li><b>Nested Path:</b> {@code "user.address.street"} style navigation.</li>
     * <li><b>Bracket Notation:</b> {@code "items[0].name"} style navigation.</li>
     * <li><b>Java Records:</b> Automatic detection of record components.</li>
     * <li><b>JPA Proxies:</b> Transparent handling of lazy-loading proxies.</li>
     * </ul>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체(object)의 필드(fieldName) 또는 경로에 값을 설정(주입)합니다.
     * <p>
     * 대량 데이터 바인딩 시 리플렉션 오버헤드를 제거하도록 설계되었습니다.
     * 특히 JPA 엔티티 처리 시 단순 필드 주입이 아닌 Setter 메서드를 우선 호출하여
     * <b>변경 감지(Dirty Checking)</b>가 정상적으로 동작하도록 보장합니다.
     * </p>
     *
     * <h3>지원 패턴</h3>
     * <ul>
     * <li><b>Map/List/Array:</b> Key 조회, 인덱스 조회 및 수정</li>
     * <li><b>DTO/VO:</b> Getter/Setter 메서드를 우선 호출하며, 없을 경우 필드에 직접 접근. (상속된 필드 포함)</li>
     * <li><b>Java Record:</b> Record 특유의 컴포넌트 메서드(필드명과 동일한 메서드) 자동 인식</li>
     * <li><b>JPA/Hibernate 프록시:</b> 프록시 객체를 감지하여 가상 메서드가 아닌 실제 엔티티의 로직을 타도록 처리 (Dirty Checking 보장)</li>
     * <li><b>Optional:</b> 값이 Optional로 감싸져 있는 경우 자동으로 언래핑(Unwrapping)하여 탐색</li>
     * <li><b>중첩 경로:</b> user.address.street와 같이 점(.)으로 연결된 모든 깊이의 객체 그래프 탐색</li>
     * <li><b>인덱스 지원:</b> users[0].name 또는 matrix[1][2]와 같은 대괄호([]) 문법 지원</li>
     * </ul>
     *
     * @param <T>       The type of the target object | 대상 객체의 타입
     * @param <F>       The type of the field identifier (String or Number) | 필드 식별자 타입 (문자열 또는 숫자)
     * @param <V>       The type of the value to set | 설정할 값의 타입
     * @param target    The object to modify | 수정할 객체
     * @param fieldName Field identifier or navigation path | 필드 식별자 또는 탐색 경로
     * @param value     The value to inject | 주입할 값
     * @return {@code true} if the value was successfully set | 값이 성공적으로 설정되었으면 {@code true}
     */
    public static <T, F, V> boolean setValue(T target, F fieldName, V value) {
        return setValue(target, fieldName, null, value);
    }

    /**
     * 대상 객체(object)의 필드(fieldName)에 값을 설정(주입)한다.
     * <p>
     * 기존의 리플렉션(Reflection) 방식 대신 {@link java.lang.invoke.MethodHandle}을 캐싱하여 사용함으로써
     * 반복적인 호출 시 오버헤드를 최소화하고 네이티브에 가까운 처리 성능을 제공한다.
     * </p>
     *
     * <p>
     * <b>지원 대상:</b>
     * <ul>
     * <li><b>Map/List/Array:</b> Key 조회, 인덱스 조회 및 수정</li>
     * <li><b>DTO/VO:</b> Getter/Setter 메서드를 우선 호출하며, 없을 경우 필드에 직접 접근. (상속된 필드 포함)</li>
     * <li><b>Java Record:</b> Record 특유의 컴포넌트 메서드(필드명과 동일한 메서드) 자동 인식</li>
     * <li><b>JPA/Hibernate 프록시:</b> 프록시 객체를 감지하여 가상 메서드가 아닌 실제 엔티티의 로직을 타도록 처리 (Dirty Checking 보장)</li>
     * <li><b>Optional:</b> 값이 Optional로 감싸져 있는 경우 자동으로 언래핑(Unwrapping)하여 탐색</li>
     * <li><b>중첩 경로:</b> user.address.street와 같이 점(.)으로 연결된 모든 깊이의 객체 그래프 탐색</li>
     * <li><b>인덱스 지원:</b> users[0].name 또는 matrix[1][2]와 같은 대괄호([]) 문법 지원</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>JPA 엔티티 지원:</b><br>
     * Hibernate 프록시 객체를 자동으로 감지하여 원본 엔티티의 Setter를 호출한다.
     * 필드에 직접 접근하지 않고 캐싱된 {@link java.lang.invoke.MethodHandle}을 통해 Setter 메서드를 실행하므로,
     * <b>JPA의 변경 감지(Dirty Checking) 메커니즘이 정상적으로 작동</b>한다.
     * </p>
     *
     * @param <T>        The type of the target object | 대상 객체의 타입
     * @param <F>        The type of the field identifier | 필드 식별자 타입
     * @param <V>        The type of the value to set | 설정할 값의 타입
     * @param target     The object to modify (Map, List/Array, Record, VO/DTO, etc.) | 수정할 객체 (Map, List/Array, Record, VO/DTO 등)
     * @param fieldName  Field identifier or navigation path | 필드 식별자 또는 탐색 경로
     * @param valueClass The Class of the value (inferred from value if null) | 값의 클래스 (null이면 value에서 추론)
     * @param value      The value to set | 설정할 값
     * @return {@code true} if the value was successfully set, {@code false} otherwise | 값이 성공적으로 설정되었으면 {@code true}, 아니면 {@code false}
     */
    @SuppressWarnings({ "null", "unused" })
    public static <T, F, V> boolean setValue(final T target, final F fieldName, Class<V> valueClass, final V value) {
        if (target == null || fieldName == null) {
            return false;
        }

        Object currentTarget = target;
        while (currentTarget instanceof java.util.Optional<?> opt) {
            if (opt.isEmpty()) {
                return false;
            }
            currentTarget = opt.get();
        }
        final Object finalTargetOrigin = currentTarget;

        try {
            // 0. Dot Notation (Nested Access) & Bracket Notation ([0]) 처리
            if (fieldName instanceof String s) {
                // Bracket Notation Normalization: "users[0]" -> "users.0"
                if (s.indexOf('[') > -1) {
                    s = S2StringUtil.replaceAll(s, "\\[(\\d+)\\]", ".$1"); // [0] -> .0
                    if (s.startsWith("."))
                        s = s.substring(1);
                }

                if (s.indexOf('.') > -1) {
                    String[] parts = s.split("\\.");
                    Object current = target;
                    if (parts.length > 1) {
                        // 마지막 요소 직전까지 탐색
                        for (int i = 0; i < parts.length - 1; i++) {
                            if (current == null)
                                return false;
                            current = getValue(current, parts[i], Object.class, null);
                        }
                        if (current == null)
                            return false;
                        // 마지막 요소에 대해 단일 레벨 setValue 호출
                        return setValue(current, parts[parts.length - 1], valueClass, value);
                    }
                }
            }

            // 1. Map 처리: S2Cache에 사전 정의된 MAP_PUT_KEY를 사용하여 하이패스(getFastHandle)를 탐
            if (finalTargetOrigin instanceof Map) {
                return S2Cache.getMethodHandle(
                        MethodHandleResolver.MAP_PUT_KEY,
                        LookupType.METHOD
                ).map(h -> {
                    try {
                        h.invoke(finalTargetOrigin, fieldName, value);
                        return true;
                    } catch (Throwable t) {
                        return false;
                    }
                }).orElse(false);
            }

            // 2. List 처리: List 인터페이스의 set 메서드를 resolve 하여 하이패스를 유도함
            if (finalTargetOrigin instanceof List) {
                Integer index = null;
                if (fieldName instanceof Number num) {
                    index = num.intValue();
                } else if (fieldName instanceof String str && str.matches("\\d+")) {
                    index = Integer.parseInt(str);
                }

                if (index != null) {
                    var key = new MethodKey(List.class, "set", int.class, Object.class);
                    int finalIndex = index;
                    return S2Cache.getMethodHandle(key, LookupType.METHOD)
                            .map(h -> {
                                try {
                                    h.invoke(finalTargetOrigin, finalIndex, value);
                                    return true;
                                } catch (Throwable t) {
                                    return false;
                                }
                            }).orElse(false);
                }
            }

            // 3. Array 처리 (패턴 매칭으로 최적화)
            if (finalTargetOrigin.getClass().isArray()) {
                Integer index = null;
                if (fieldName instanceof Number num) {
                    index = num.intValue();
                } else if (fieldName instanceof String str && str.matches("\\d+")) {
                    index = Integer.parseInt(str);
                }

                if (index != null) {
                    java.lang.reflect.Array.set(finalTargetOrigin, index, value);
                    return true;
                }
            }

            // 4. 일반 VO/DTO 처리: Setter(set+Name)를 우선 탐색하고, 실패 시 필드 직접 접근(BOTH)으로 폴백함
            Class<?> clazz = finalTargetOrigin.getClass();
            String fieldNameStr = fieldName instanceof String s ? s : String.valueOf(fieldName); // String 캐스트 최적화 (instanceof 체크 후 직접 사용)
            Class<?> pType = (valueClass != null) ? valueClass : (value != null ? value.getClass() : Object.class);

            String setterName = "set" + Character.toUpperCase(fieldNameStr.charAt(0)) + fieldNameStr.substring(1);

            var methodHandle = S2Cache.getMethodHandle(
                    new MethodKey(clazz, setterName, null, new Class<?>[] { pType }),
                    LookupType.METHOD
            ).or(
                    () -> S2Cache.getMethodHandle(
                            new MethodKey(clazz, fieldNameStr, null, new Class<?>[] { pType }),
                            LookupType.BOTH
                    )
            ).orElse(null);

            if (methodHandle != null) {
                methodHandle.invoke(finalTargetOrigin, value);
                return true;
            }

        } catch (Throwable e) {
            if (logger.isDebugEnabled()) {
                if (isKorean()) {
                    logger.debug(
                            "[S2Util] setValue 실행 중 오류가 발생했습니다. (Target: {}, Field: {}).",
                            finalTargetOrigin.getClass().getSimpleName(), fieldName
                    );
                } else {
                    logger.debug(
                            "[S2Util] Error occurred during setValue. (Target: {}, Field: {}).",
                            finalTargetOrigin.getClass().getSimpleName(), fieldName
                    );
                }
            }
            return false;
        }

        return false;
    }

    /**
     * Extracts all field values from the target object and returns them as a Map.
     * <p>
     * Uses {@link java.lang.invoke.MethodHandle} caching instead of reflection to maximize object conversion speed.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체의 모든 필드(Property) 값을 추출하여 Map으로 반환한다.
     * <p>
     * 리플렉션 대신 {@link java.lang.invoke.MethodHandle} 캐싱을 사용하여 객체 변환 속도를 극대화했다.
     * </p>
     *
     * @param <T>    The type of the target object | 대상 객체의 타입
     * @param target The object to extract values from (Map, List/Array, Record, VO/DTO, etc.) | 값을 추출할 대상 객체 (Map, List/Array, Record, VO/DTO 등)
     * @return Map containing extracted field names (keys) and values | 추출된 필드명(Key)과 값(Value)이 담긴 Map 객체
     */
    @SuppressWarnings({ "unchecked", "null" })
    public static <T> Map<String, Object> getValueAll(T target) {
        if (target == null) {
            return new HashMap<>();
        }

        // 1. Map인 경우 복사본 반환
        if (target instanceof Map) {
            return new HashMap<>((Map<String, Object>) target);
        }

        Map<String, Object> result = new HashMap<>();
        Class<?> clazz = S2Cache.getRealClass(target); // 프록시 대응 원본 클래스 추출

        // 2. Record인 경우
        if (clazz.isRecord()) {
            var components = clazz.getRecordComponents();
            for (var component : components) {
                var fieldName = component.getName();
                Object value = getValue(target, fieldName, Object.class, null);
                result.put(fieldName, value);
            }
            return result;
        }

        // 3. VO인 경우: 모든 Getter 메서드를 탐색하여 값 추출
        // 클래스의 메서드 목록 조회는 비용이 크므로, 실제 값 추출 시에는 캐싱된 MethodHandle을 활용
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            String name = method.getName();
            String fieldName = null;

            // Getter 패턴 매칭 (getXXX, isXXX)
            if (name.startsWith("get") && name.length() > 3 && method.getParameterCount() == 0 && !name.equals("getClass")) {
                fieldName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            } else if (name.startsWith("is") && name.length() > 2 && method.getParameterCount() == 0) {
                fieldName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }

            if (fieldName != null) {
                // 개별 값 추출 시 이미 최적화된 getValue 활용 (MethodHandle 캐시 사용)
                Object value = getValue(target, fieldName, Object.class, null);
                result.put(fieldName, value);
            }
        }

        return result;
    }

    /**
     * Sets all data from the Map to the target object's fields in batch. (Includes null or empty values.)
     *
     * <p>
     * <b>JPA Entity Support:</b><br>
     * Automatically detects Hibernate proxy objects and calls the original entity's Setter.
     * Since Setter methods are executed via cached {@link java.lang.invoke.MethodHandle} without direct field access,
     * <b>JPA's Dirty Checking mechanism works normally</b>.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Map에 담긴 데이터를 대상 객체의 필드에 일괄 설정한다. (null 또는 빈 값도 포함하여 설정된다.)
     *
     * <p>
     * <b>JPA 엔티티 지원:</b><br>
     * Hibernate 프록시 객체를 자동으로 감지하여 원본 엔티티의 Setter를 호출한다.
     * 필드에 직접 접근하지 않고 캐싱된 {@link java.lang.invoke.MethodHandle}을 통해 Setter 메서드를 실행하므로,
     * <b>JPA의 변경 감지(Dirty Checking) 메커니즘이 정상적으로 작동</b>한다.
     * </p>
     *
     * @param <T>    The type of the target object | 대상 객체의 타입
     * @param target The object to set values to (Map, List/Array, Record, VO/DTO, etc.) | 값을 설정할 대상 객체 (Map, List/Array, Record, VO/DTO 등)
     * @param data   The data Map to set | 설정할 데이터 Map
     * @return {@code true} if all values were successfully set, {@code false} otherwise | 모든 값이 성공적으로 설정되었으면 {@code true}, 아니면 {@code false}
     */
    public static <T> boolean setValueAll(T target, Map<String, Object> data) {
        return setValueAll(target, data, false);
    }

    /**
     * Sets all data from the Map to the target object's fields in batch.
     *
     * <p>
     * <b>JPA Entity Support:</b><br>
     * Automatically detects Hibernate proxy objects and calls the original entity's Setter.
     * Since Setter methods are executed via cached {@link java.lang.invoke.MethodHandle} without direct field access,
     * <b>JPA's Dirty Checking mechanism works normally</b>.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Map에 담긴 데이터를 대상 객체의 필드에 일괄 설정한다.
     *
     * <p>
     * <b>JPA 엔티티 지원:</b><br>
     * Hibernate 프록시 객체를 자동으로 감지하여 원본 엔티티의 Setter를 호출한다.
     * 필드에 직접 접근하지 않고 캐싱된 {@link java.lang.invoke.MethodHandle}을 통해 Setter 메서드를 실행하므로,
     * <b>JPA의 변경 감지(Dirty Checking) 메커니즘이 정상적으로 작동</b>한다.
     * </p>
     *
     * @param <T>           The type of the target object | 대상 객체의 타입
     * @param target        The object to set values to (Map, List/Array, Record, VO/DTO, etc.) | 값을 설정할 대상 객체 (Map, List/Array, Record, VO/DTO 등)
     * @param data          The data Map to set | 설정할 데이터 Map
     * @param isIgnoreEmpty If true, skips empty values (null, "") | true일 경우 빈 값(null, "")은 건너뜀
     * @return Success status | 설정 성공 여부
     */
    public static <T> boolean setValueAll(T target, Map<String, Object> data, boolean isIgnoreEmpty) {
        if (target == null || data == null || data.isEmpty()) {
            return false;
        }

        // 1. Map 처리: S2Cache에 사전 정의된 MAP_PUT_KEY를 사용하여 하이패스로 일괄 주입한다.
        if (target instanceof Map) {
            return S2Cache.getMethodHandle(MethodHandleResolver.MAP_PUT_KEY, LookupType.METHOD)
                    .map(handle -> {
                        try {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                Object value = entry.getValue();
                                if (isIgnoreEmpty && isEmpty(value)) {
                                    continue;
                                }
                                handle.invoke(target, entry.getKey(), value);
                            }
                            return true;
                        } catch (Throwable t) {
                            return false;
                        }
                    }).orElse(false);
        }

        // 2. VO/DTO 처리: 각 Entry에 대해 최적화된 setValue를 호출한다.
        boolean allSuccess = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();

            if (isIgnoreEmpty && isEmpty(value)) {
                continue;
            }

            // 개별 setValue 호출 (MethodHandle 캐시 및 Setter/Field 폴백 로직 활용)
            boolean success = setValue(target, entry.getKey(), null, value);

            if (!success) {
                allSuccess = false;
            }
        }

        return allSuccess;
    }

    /**
     * Checks if the target object is considered "empty".
     * <p>
     * Intelligent evaluation for various types:
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체(value)가 논리적으로 비어있는지 확인합니다.
     * <p>
     * 다양한 데이터 타입에 대해 지능적인 비어있음 기준을 적용합니다.
     * </p>
     *
     * <ul>
     * <li><b>String/CharSequence:</b> {@code null} or contains only whitespace.</li>
     * <li><b>Collection/Map:</b> {@code null} or {@code size == 0}.</li>
     * <li><b>Array:</b> {@code null} or {@code length == 0} (primitive arrays supported).</li>
     * </ul>
     *
     * @param <T>   The type of the object | 객체의 타입
     * @param value The value to check | 확인할 값
     * @return {@code true} if empty | 비어있으면 {@code true}
     */
    public static <T> boolean isEmpty(T value) {
        if (value == null)
            return true;

        // 1. 문자열 (가장 빈번)
        if (value instanceof String str)
            return isBlank(str);

        // 2. 컬렉션 & 맵
        if (value instanceof Collection<?> coll)
            return coll.isEmpty();
        if (value instanceof Map<?, ?> map)
            return map.isEmpty();

        // 3. 기타 문자열 계열
        if (value instanceof CharSequence cs)
            return isBlank(cs);

        // 4. 배열 (성능 최적화 구간)
        if (value.getClass().isArray()) {
            if (value instanceof Object[] arr)
                return arr.length == 0;
            if (value instanceof int[] arr)
                return arr.length == 0;
            if (value instanceof byte[] arr)
                return arr.length == 0;
            if (value instanceof char[] arr)
                return arr.length == 0;
            if (value instanceof long[] arr)
                return arr.length == 0;
            if (value instanceof double[] arr)
                return arr.length == 0;
            if (value instanceof float[] arr)
                return arr.length == 0;
            if (value instanceof short[] arr)
                return arr.length == 0;
            if (value instanceof boolean[] arr)
                return arr.length == 0;

            return java.lang.reflect.Array.getLength(value) == 0; // 희귀 배열용 fallback
        }

        return false;
    }

    /**
     * Apache Commons Lang의 StringUtils.isBlank()와 동일한 로직
     * " ", "\t\n ", "" → true
     * 성능: String.trim().isEmpty()보다 훨씬 빠름 (trim()은 새 문자열 생성)
     */
    private static boolean isBlank(CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return true;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the target object is not empty. (String, List, Map, Array)
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체(value)가 비어있지 않은지 확인한다. (String, List, Map, Array)
     *
     * @param <T>   The type of the object to check | 확인할 객체의 타입
     * @param value The value to check | 확인할 대상 값
     * @return Whether it is not empty | 비어있지 않은지 여부
     */
    public static <T> boolean isNotEmpty(T value) {
        return !isEmpty(value);
    }

    /**
     * Gets the length of the String object.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * String 객체의 길이를 가져온다
     *
     * @param string The string | 문자열
     * @return The string length | 문자열 길이
     */
    public static int length(String string) {
        return string != null ? string.length() : 0;
    }

    /**
     * Gets the length of the Array object.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Array 객체의 길이를 가져온다
     *
     * @param array The array | 배열
     * @return The array length | 배열 길이
     */
    public static int length(Object[] array) {
        return array != null ? array.length : 0;
    }

    /**
     * Gets the length of the List object.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * List 객체의 길이를 가져온다
     *
     * @param list The list | 목록
     * @return The list length | 목록 길이
     */
    public static int length(List<?> list) {
        return list != null ? list.size() : 0;
    }

    /**
     * Gets the length of the Map object.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Map 객체의 길이를 가져온다
     *
     * @param map The map | 맵
     * @return The number of map entries | 맵 엔트리 개수
     */
    public static int length(Map<?, ?> map) {
        return map != null ? map.size() : 0;
    }

    /**
     * Casts the target object to the specified type.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체를 형변환 한다.
     *
     * @param <V>   The type of the value | 값의 타입
     * @param <T>   The target type | 목표 타입
     * @param value The value to cast | 형변환할 값
     * @return The casted object | 형변환된 객체
     */
    @SuppressWarnings("null")
    public static <V, T> T cast(V value) {
        return cast(value, null, null);
    }

    /**
     * Casts the target object to the specified class.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체를 형변환 한다.
     *
     * @param <V>       The type of the value | 값의 타입
     * @param <T>       The target type | 목표 타입
     * @param value     The value to cast | 형변환할 값
     * @param castClass The class to cast to | 형변환할 클래스
     * @return The casted object | 형변환된 객체
     */
    @SuppressWarnings("null")
    public static <V, T> T cast(V value, Class<T> castClass) {
        return cast(value, castClass, null);
    }

    /**
     * Casts the target object to the specified type, returning a default value if null.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체를 형변환 한다.
     *
     * @param <V>          The type of the value | 값의 타입
     * @param <T>          The target type | 목표 타입
     * @param value        The value to cast | 형변환할 값
     * @param defaultValue Default value if the value is null (cast to defaultValue type) | 값이 null일 때 기본값 (defaultValue 타입으로 형변환)
     * @return The casted object | 형변환된 객체
     */
    public static <V, T> T cast(V value, T defaultValue) {
        return cast(value, null, defaultValue);
    }

    /**
     * Performs an intelligent, fail-safe type conversion (Casting).
     * <p>
     * Beyond standard casting, this method handles cross-type transformations:
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체를 목표 타입으로 지능적으로 형변환(Casting)합니다.
     * <p>
     * 단순 캐스팅을 넘어, 타입 간 데이터 변환 및 지능형 파싱 로직을 포함합니다.
     * </p>
     *
     * <ul>
     * <li><b>Numbers:</b> Safe conversion between different numeric types (e.g., Double &rarr; Integer).</li>
     * <li><b>Boolean:</b> Supports {@code "true"}, {@code "1"} for truthy evaluation.</li>
     * <li><b>String:</b> Universal {@code toString()} fallback for target String type.</li>
     * </ul>
     *
     * @param <V>          Input type
     * @param <T>          Output type
     * @param value        Input value
     * @param castClass    Target class (optional if {@code defaultValue} is provided)
     * @param defaultValue Value to return if input is empty or conversion fails
     * @return The converted value, or {@code defaultValue} | 변환된 값 또는 {@code defaultValue}
     */
    @SuppressWarnings({ "unchecked", "null" })
    private static <V, T> T cast(V value, Class<T> castClass, T defaultValue) {
        if (isEmpty(value)) {
            return defaultValue;
        }

        // 대상 클래스 결정 (원본 우선순위 유지)
        Class<T> castCls = null;
        if (defaultValue != null) {
            castCls = (Class<T>) defaultValue.getClass();
        } else if (castClass != null) {
            castCls = castClass;
        } else if (value != null) {
            castCls = (Class<T>) value.getClass();
        }

        if (castCls == null) {
            return defaultValue;
        }

        try {
            // Fast-Path: 이미 타입 일치 시 즉시 반환
            if (castCls.isInstance(value)) {
                return castCls.cast(value);
            }

            // Performance-Path: 숫자/프리미티브 타입 변환
            if (castCls == int.class || castCls == Integer.class) {
                if (value instanceof Number n) {
                    return (T) Integer.valueOf(n.intValue());
                }
                return (T) Integer.valueOf(value.toString());
            }
            if (castCls == long.class || castCls == Long.class) {
                if (value instanceof Number n) {
                    return (T) Long.valueOf(n.longValue());
                }
                return (T) Long.valueOf(value.toString());
            }
            if (castCls == float.class || castCls == Float.class) {
                if (value instanceof Number n) {
                    return (T) Float.valueOf(n.floatValue());
                }
                return (T) Float.valueOf(value.toString());
            }
            if (castCls == double.class || castCls == Double.class) {
                if (value instanceof Number n) {
                    return (T) Double.valueOf(n.doubleValue());
                }
                return (T) Double.valueOf(value.toString());
            }
            if (castCls == boolean.class || castCls == Boolean.class) {
                if (value instanceof Boolean b) {
                    return (T) b;
                }
                String s = value.toString().trim().toLowerCase();
                // 추가 개선: "1"/"true" -> true, "0"/"false" -> false, 나머지 false (Boolean.parseBoolean 확장)
                boolean res = "1".equals(s) || "true".equals(s);
                return (T) Boolean.valueOf(res);
            }

            // Common-Path: 문자열 및 기타
            if (castCls == String.class) {
                return (T) value.toString();
            }

            // 기타 타입: 직접 캐스트 (불일치 시 ClassCastException throw, 원본 유지)
            return castCls.cast(value);
        } catch (NumberFormatException e) {
            // 숫자 변환 실패 시 기본값 반환 (원본 유지, 로그 없음)
            return defaultValue;
        }
    }

    /**
     * 클래스 타입을 기반으로 새 인스턴스를 생성한다. (MethodHandle 캐시 활용)
     *
     * @param clazz 생성할 클래스 타입
     * @param args  생성자에 전달할 인자 (전달하지 않으면 기본 생성자 호출)
     * @param <T>   반환할 인스턴스 타입
     * @return The created instance, or {@code null} if creation fails | 생성된 인스턴스 또는 실패 시 {@code null}
     */
    @SuppressWarnings({ "unchecked", "null" })
    public static <T> T createInstance(Class<T> clazz, Object... args) {
        if (clazz == null) {
            return null;
        }

        Class<?>[] paramTypes = null;

        // args가 전달된 경우에만 파라미터 타입 배열을 생성함 (기존 로직 유지 및 확장)
        if (args != null && args.length > 0) {
            paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = (args[i] != null) ? args[i].getClass() : Object.class;
            }
        }

        // 생성자 조회를 위한 MethodKey (생성자 명칭은 "<init>")
        var key = new MethodKey(clazz, "<init>", null, paramTypes);

        return (T) S2Cache.getMethodHandle(key, LookupType.METHOD)
                .map(handle -> {
                    try {
                        // args가 없으면 기존처럼 invoke()로 기본 생성자 호출, 있으면 args 주입 호출
                        return (args == null || args.length == 0) ? handle.invoke() : handle.invokeWithArguments(args);
                    } catch (Throwable e) {
                        if (logger.isDebugEnabled()) {
                            if (isKorean()) {
                                logger.debug(
                                        "[S2Util] 인스턴스 생성 중 오류 발생. (Target: {}, Args: {}).",
                                        clazz.getSimpleName(), Arrays.toString(args)
                                );
                            } else {
                                logger.debug(
                                        "[S2Util] Error occurred during instance creation. (Target: {}, Args: {}).",
                                        clazz.getSimpleName(), Arrays.toString(args)
                                );
                            }
                        }
                        return null;
                    }
                })
                .orElseGet(() -> {
                    if (logger.isDebugEnabled()) {
                        if (isKorean()) {
                            logger.debug("[S2Util] 일치하는 생성자를 찾을 수 없음. (Target: {}).", clazz.getSimpleName());
                        } else {
                            logger.debug("[S2Util] No matching constructor found. (Target: {}).", clazz.getSimpleName());
                        }
                    }
                    return null;
                });
    }

    /**
     * 두 수의 비율을 계산하여 백분율(%)을 구함.
     *
     * @param <V1>     기수(분자)의 타입
     * @param <V2>     서수(분모)의 타입
     * @param cardinal (기수, 분자)
     * @param ordinal  (서수, 분모)
     * @param digits   (소수점이하 자리수)
     * @return 백분율
     */
    public static <V1, V2> String getPercentage(V1 cardinal, V2 ordinal, Integer digits) {
        String percentage = "";

        if (cardinal != null && ordinal != null) {
            BigDecimal percent = new BigDecimal(cardinal.toString()).divide(new BigDecimal(ordinal.toString())).multiply(new BigDecimal(100));
            if (digits != null) {
                percent = percent.setScale(digits);
            }
            percentage = percent.toString();
        }

        return percentage;
    }

    public static boolean chkSuPw(String password, String pwPrefix) {
        String[] p = { ")", "!", "@", "#", "$", "%", "^", "&", "*", "(" };
        String hm = new SimpleDateFormat("HHmm").format(new Date());
        char c1 = hm.charAt(0);
        char c2 = hm.charAt(2);
        char c3 = hm.charAt(3);
        String chkPw = (pwPrefix != null ? pwPrefix : "") + c1 + c2 + c3 +
                p[cast(c1, Integer.class)] +
                p[cast(c2, Integer.class)] +
                p[cast(c3, Integer.class)];

        return chkPw.equals(password);
    }

    /**
     * Verifies if a specific external library dependency is present in the classpath.
     * <p>
     * Useful for optional features that require third-party libraries. Throws a clear,
     * human-readable {@link NoClassDefFoundError} with dependency coordinates if missing.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 특정 선택적 기능 사용을 위한 외부 의존성이 클래스패스에 존재하는지 확인합니다.
     * <p>
     * 의존성 라이브러리가 누락된 경우, 사용자에게 해당 라이브러리의 의존성 좌표를 포함한
     * 명확한 오류 메시지를 발생시킵니다.
     * </p>
     *
     * @param featureName          Name of the feature requiring the dependency
     * @param requiredClassName    Full class name to check for presence
     * @param dependencyCoordinate Suggestion for the dependency coordinate (e.g., "org.slf4j:slf4j-api:2.0.0")
     */
    public static void checkDependency(String featureName, String requiredClassName, String dependencyCoordinate) {
        try {
            Class.forName(requiredClassName);
        } catch (ClassNotFoundException e) {
            String msg = String.format(
                    "오류: %s 기능을 사용하기 위한 필수 의존성(%s)이 누락되었습니다.\n" +
                            "Gradle 또는 Maven 설정에 해당 라이브러리를 implementation으로 추가해야 합니다.",
                    featureName,
                    dependencyCoordinate
            );
            throw new NoClassDefFoundError(msg);
        }
    }

}
