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
package io.github.devers2.s2util.json;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Lightweight, dependency-free JSON utility: writes and parses JSON and maps it to and from plain Java types.
 * <p>
 * It is deliberately small. Anything outside the supported types below fails with {@link S2JsonException} instead of
 * being guessed or skipped; for annotations, polymorphism, custom serializers or streaming, use Jackson.
 * </p>
 * <ul>
 * <li><b>Strict by default:</b> standard JSON (RFC 8259) only; relaxed syntax is opt-in through {@link Feature}. Trailing
 * content, nesting deeper than {@link #MAX_DEPTH}, invalid escapes and malformed or overlong ({@link #MAX_NUMBER_LENGTH})
 * numbers are rejected with the character position. Failures inside collections or proxies being written are wrapped in
 * {@link S2JsonException} with the path.</li>
 * <li><b>Parsed values:</b> objects → {@code LinkedHashMap<String, Object>} (a duplicate key keeps the last value),
 * arrays → {@code ArrayList<Object>}, strings → {@code String}, integers → {@code Long} ({@code BigInteger} beyond
 * {@code long}), decimals → {@code Double} ({@code BigDecimal} with {@link Feature#USE_BIG_DECIMAL_FOR_FLOATS}),
 * {@code true}/{@code false} → {@code Boolean}, {@code null} → {@code null}.</li>
 * <li><b>Supported types</b> (writing and mapping): {@code null}, {@code String}/{@code CharSequence},
 * {@code Character}, {@code Boolean}, numbers (primitives, wrappers, {@code BigInteger}, {@code BigDecimal}), enums (by
 * {@code name()}), {@code java.time} types and {@code Date} (ISO-8601 strings), {@code UUID}, {@code URI},
 * {@code Locale} (language tag), {@code Optional}, arrays, {@code Collection}s, {@code Map}s (keys written as strings),
 * records (components) and POJOs/DTOs/VOs (non-static, non-transient fields including inherited ones). Mapping creates a
 * POJO with its no-arg constructor, or else an immutable value object through its constructor by parameter name
 * (requires compiling with {@code -parameters}). Unknown JSON properties are ignored when mapping.</li>
 * <li><b>Proxies:</b> Hibernate and Spring AOP proxies are written from their real object (a lazy entity is initialized,
 * so the session must be open); fields added by Hibernate bytecode enhancement are skipped. Other proxies, lambdas and
 * hidden classes fail instead of being written from their empty proxy fields.</li>
 * <li><b>Errors:</b> never returns {@code null} or partial output for bad input; circular references, NaN/Infinity
 * (unless {@link Feature#ALLOW_NON_NUMERIC_NUMBERS}), lossy number conversions (3.7 → {@code int}) and unsupported JDK
 * types throw.</li>
 * </ul>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 의존성 없는 경량 JSON 유틸리티입니다. JSON 을 생성·파싱하고, 일반 Java 타입과 서로 매핑합니다.
 * <p>
 * 의도적으로 작게 유지합니다. 아래 지원 타입을 벗어나면 추측하거나 건너뛰지 않고 {@link S2JsonException}을 던집니다. 어노테이션,
 * 다형성, 커스텀 직렬화기, 스트리밍이 필요하면 Jackson 을 사용하십시오.
 * </p>
 * <ul>
 * <li><b>기본은 엄격:</b> 표준 JSON(RFC 8259)만 받으며, 느슨한 문법은 {@link Feature}로 켭니다. 뒤따르는 문자, {@link #MAX_DEPTH}보다 깊은
 * 중첩, 잘못된 이스케이프, 잘못되거나 너무 긴({@link #MAX_NUMBER_LENGTH}) 숫자는 문자 위치와 함께 거부합니다. 쓰는 중 컬렉션·프록시에서 난
 * 오류는 경로를 담은 {@link S2JsonException}으로 감쌉니다.</li>
 * <li><b>파싱 결과:</b> 객체 → {@code LinkedHashMap<String, Object>}(중복 키는 마지막 값), 배열 → {@code ArrayList<Object>}, 문자열 →
 * {@code String}, 정수 → {@code Long}({@code long} 범위를 넘으면 {@code BigInteger}), 소수 → {@code Double}
 * ({@link Feature#USE_BIG_DECIMAL_FOR_FLOATS}면 {@code BigDecimal}), 참/거짓 → {@code Boolean}, {@code null} → {@code null}.</li>
 * <li><b>지원 타입</b>(생성·매핑): {@code null}, {@code String}/{@code CharSequence}, {@code Character}, {@code Boolean}, 숫자(기본형,
 * 래퍼, {@code BigInteger}, {@code BigDecimal}), 열거형({@code name()}), {@code java.time} 타입과 {@code Date}(ISO-8601 문자열),
 * {@code UUID}, {@code URI}, {@code Locale}(언어 태그), {@code Optional}, 배열, {@code Collection}, {@code Map}(키는 문자열로 기록),
 * record(컴포넌트), POJO/DTO/VO(상속 포함, static·transient 제외 필드). 매핑할 때 POJO 는 인자 없는 생성자로, 없으면 불변 VO 로 보고 생성자
 * 파라미터 이름으로 만듭니다({@code -parameters} 컴파일 필요). 매핑할 때 모르는 JSON 속성은 무시합니다.</li>
 * <li><b>프록시:</b> Hibernate·Spring AOP 프록시는 실제 객체로 씁니다(지연 로딩 엔티티는 초기화되므로 세션이 열려 있어야 함). Hibernate
 * 바이트코드 강화로 추가된 필드는 제외합니다. 그 밖의 프록시, 람다, 숨은 클래스는 빈 프록시 필드로 쓰지 않고 예외를 던집니다.</li>
 * <li><b>오류:</b> 잘못된 입력에 {@code null}이나 일부만 만든 결과를 돌려주지 않습니다. 순환 참조, NaN/Infinity
 * ({@link Feature#ALLOW_NON_NUMERIC_NUMBERS} 없이), 손실되는 숫자 변환(3.7 → {@code int}), 지원하지 않는 JDK 타입은 예외입니다.</li>
 * </ul>
 *
 * @author devers2
 * @since 2.0.0
 */
public final class S2JsonUtil {

    /**
     * Maximum nesting depth of objects/arrays when parsing, writing or mapping; deeper input fails instead of
     * overflowing the stack.
     * <p>
     * <b>[한국어 설명]</b> 파싱·생성·매핑 시 객체/배열의 최대 중첩 깊이입니다. 더 깊으면 스택 넘침 대신 예외를 던집니다.
     * </p>
     */
    public static final int MAX_DEPTH = 512;

    /**
     * Maximum length of a number in JSON text, and of the digits of an integer created from it; longer numbers fail
     * instead of spending quadratic time building a huge {@code BigInteger} (the same default as Jackson).
     * <p>
     * <b>[한국어 설명]</b> JSON 숫자 표기와, 그로부터 만드는 정수 자릿수의 최대 길이입니다. 더 길면 거대한 {@code BigInteger}를 만드느라
     * 제곱 시간을 쓰는 대신 예외를 던집니다(Jackson 기본값과 같음).
     * </p>
     */
    public static final int MAX_NUMBER_LENGTH = 1000;

    /**
     * Opt-in relaxations of standard JSON.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 표준 JSON 을 느슨하게 받는 선택 기능입니다.
     */
    public enum Feature {
        /** Allow raw control characters (U+0000–U+001F) in strings | 문자열 안의 이스케이프 안 된 제어 문자 허용 */
        ALLOW_UNESCAPED_CONTROL_CHARS,
        /** Allow a backslash before any character ({@code \x} → {@code x}) | 모든 문자 앞의 백슬래시 허용 */
        ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER,
        /** Parse and write {@code NaN}, {@code Infinity}, {@code -Infinity} | {@code NaN}, {@code Infinity} 읽기·쓰기 허용 */
        ALLOW_NON_NUMERIC_NUMBERS,
        /** Allow missing array values ({@code [1,,3]} → {@code [1,null,3]}) | 배열의 빈 값 허용 */
        ALLOW_MISSING_VALUES,
        /** Allow a trailing comma in objects and arrays | 객체·배열 끝의 쉼표 허용 */
        ALLOW_TRAILING_COMMA,
        /** Allow {@code //} and {@code /* *}{@code /} comments | {@code //}, {@code /* *}{@code /} 주석 허용 */
        ALLOW_JAVA_COMMENTS,
        /** Allow {@code #} comments | {@code #} 주석 허용 */
        ALLOW_YAML_COMMENTS,
        /** Allow single-quoted strings and keys | 작은따옴표 문자열·키 허용 */
        ALLOW_SINGLE_QUOTES,
        /** Allow unquoted identifier keys ({@code {name: 1}}) | 따옴표 없는 식별자 키 허용 */
        ALLOW_UNQUOTED_FIELD_NAMES,
        /** Allow leading zeros ({@code 007}) | 숫자 앞의 0 허용 */
        ALLOW_LEADING_ZEROS_FOR_NUMBERS,
        /** Allow a leading plus sign ({@code +1}) | 숫자 앞의 + 허용 */
        ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS,
        /** Allow a leading decimal point ({@code .5}) | 소수점으로 시작하는 숫자 허용 */
        ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS,
        /** Allow a trailing decimal point ({@code 1.}) | 소수점으로 끝나는 숫자 허용 */
        ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS,
        /** Parse decimals as {@code BigDecimal} instead of {@code Double} (no rounding) | 소수를 {@code Double} 대신 {@code BigDecimal}로 파싱 (반올림 없음) */
        USE_BIG_DECIMAL_FOR_FLOATS;

        private static int flags(Feature[] features) {
            int flags = 0;
            if (features != null) {
                for (Feature feature : features) {
                    if (feature != null) {
                        flags |= 1 << feature.ordinal();
                    }
                }
            }
            return flags;
        }

        private boolean in(int flags) {
            return (flags & (1 << ordinal())) != 0;
        }
    }

    private S2JsonUtil() {
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Writes a value as JSON.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 값을 JSON 으로 씁니다.
     *
     * @param value    The value (supported types in the class documentation) | 값 (지원 타입은 클래스 설명 참고)
     * @param features Optional features ({@link Feature#ALLOW_NON_NUMERIC_NUMBERS}) | 선택 기능
     * @return The JSON text | JSON 문자열
     * @throws S2JsonException If the value contains an unsupported type, a circular reference or NaN/Infinity | 지원하지
     *                         않는 타입, 순환 참조, NaN/Infinity 가 있는 경우
     * @apiNote
     *
     *          <pre>{@code
     * S2JsonUtil.toJson(Map.of("name", "홍길동", "age", 30));   // {"name":"홍길동","age":30}
     * S2JsonUtil.toJson(new Order(LocalDate.of(2026, 9, 30))); // {"day":"2026-09-30"}
     * }</pre>
     */
    public static String toJson(Object value, Feature... features) {
        StringBuilder sb = new StringBuilder();
        new Writer(Feature.flags(features)).write(sb, value, 0);
        return sb.toString();
    }

    /**
     * Parses JSON text into maps, lists and scalar values.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * JSON 문자열을 Map, List, 스칼라 값으로 파싱합니다.
     *
     * @param json     The JSON text | JSON 문자열
     * @param features Optional relaxations | 선택 기능
     * @return The parsed value (see the class documentation) | 파싱한 값 (클래스 설명 참고)
     * @throws IllegalArgumentException If {@code json} is null | {@code json}이 null 인 경우
     * @throws S2JsonException          If the text is not valid JSON | 올바른 JSON 이 아닌 경우
     */
    public static Object parse(String json, Feature... features) {
        if (json == null) {
            throw new IllegalArgumentException("json must not be null");
        }
        return new Parser(json, Feature.flags(features)).parseDocument();
    }

    /**
     * Parses JSON text whose top level must be an object.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 최상위가 객체여야 하는 JSON 문자열을 파싱합니다.
     *
     * @param json     The JSON text | JSON 문자열
     * @param features Optional relaxations | 선택 기능
     * @return The object as an ordered map | 순서가 유지되는 Map
     * @throws S2JsonException If the text is not valid JSON or not an object | 올바른 JSON 객체가 아닌 경우
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json, Feature... features) {
        Object value = parse(json, features);
        if (!(value instanceof Map)) {
            throw new S2JsonException("Expected a JSON object but was " + describe(value));
        }
        return (Map<String, Object>) value;
    }

    /**
     * Parses JSON text whose top level must be an array.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 최상위가 배열이어야 하는 JSON 문자열을 파싱합니다.
     *
     * @param json     The JSON text | JSON 문자열
     * @param features Optional relaxations | 선택 기능
     * @return The array as a list | 배열 List
     * @throws S2JsonException If the text is not valid JSON or not an array | 올바른 JSON 배열이 아닌 경우
     */
    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json, Feature... features) {
        Object value = parse(json, features);
        if (!(value instanceof List)) {
            throw new S2JsonException("Expected a JSON array but was " + describe(value));
        }
        return (List<Object>) value;
    }

    /**
     * Parses JSON text and maps it to {@code type}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * JSON 문자열을 파싱해 {@code type}으로 매핑합니다.
     *
     * @param <T>      Target type | 대상 타입
     * @param json     The JSON text | JSON 문자열
     * @param type     The target class (supported types in the class documentation) | 대상 클래스
     * @param features Optional relaxations | 선택 기능
     * @return The mapped value | 매핑한 값
     * @throws S2JsonException If parsing or mapping fails; the message names the property path | 파싱·매핑 실패 시 (메시지에
     *                         속성 경로 포함)
     * @apiNote
     *
     *          <pre>{@code
     * record User(String name, int age, List<String> tags) {}
     * User user = S2JsonUtil.fromJson("{\"name\":\"홍길동\",\"age\":30,\"tags\":[\"a\"]}", User.class);
     * }</pre>
     */
    public static <T> T fromJson(String json, Class<T> type, Feature... features) {
        return convert(parse(json, features), type);
    }

    /**
     * Parses a JSON array and maps each element to {@code elementType}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * JSON 배열을 파싱해 각 요소를 {@code elementType}으로 매핑합니다.
     *
     * @param <T>         Element type | 요소 타입
     * @param json        The JSON text | JSON 문자열
     * @param elementType The element class | 요소 클래스
     * @param features    Optional relaxations | 선택 기능
     * @return The mapped list | 매핑한 List
     * @throws S2JsonException If parsing or mapping fails | 파싱·매핑 실패 시
     */
    public static <T> List<T> fromJsonList(String json, Class<T> elementType, Feature... features) {
        List<Object> array = parseArray(json, features);
        List<T> result = new ArrayList<>(array.size());
        Mapper mapper = new Mapper();
        for (int i = 0; i < array.size(); i++) {
            mapper.path.push(i);
            @SuppressWarnings("unchecked")
            T element = (T) mapper.convert(array.get(i), elementType, 1);
            result.add(element);
            mapper.path.pop();
        }
        return result;
    }

    /**
     * Maps an already parsed value (maps, lists and scalars, as returned by {@link #parse}) to {@code type}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 이미 파싱한 값({@link #parse}가 돌려주는 Map, List, 스칼라)을 {@code type}으로 매핑합니다.
     *
     * @param <T>   Target type | 대상 타입
     * @param value The parsed value | 파싱한 값
     * @param type  The target class | 대상 클래스
     * @return The mapped value | 매핑한 값
     * @throws S2JsonException If mapping fails | 매핑 실패 시
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        try {
            return (T) new Mapper().convert(value, type, 0);
        } catch (S2JsonException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new S2JsonException("Failed to map to " + type.getName(), e);
        }
    }

    // =========================================================================
    // Writer
    // =========================================================================

    /** JSON number grammar, used to check {@code toString()} of unusual Number subclasses | JSON 숫자 문법 */
    private static final Pattern JSON_NUMBER = Pattern.compile("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

    private static final class Writer {
        private final int flags;
        private final Set<Object> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Deque<Object> path = new ArrayDeque<>();

        Writer(int flags) {
            this.flags = flags;
        }

        private S2JsonException error(String message) {
            return new S2JsonException(message + " at " + pathOf(path));
        }

        void write(StringBuilder sb, Object value, int depth) {
            if (depth > MAX_DEPTH) {
                throw error("Nesting deeper than " + MAX_DEPTH);
            }
            if (value == null) {
                sb.append("null");
            } else if (value instanceof CharSequence || value instanceof Character) {
                writeString(sb, value.toString());
            } else if (value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Number number) {
                writeNumber(sb, number);
            } else if (value instanceof Enum<?> e) {
                writeString(sb, e.name());
            } else if (value instanceof Optional<?> optional) {
                write(sb, optional.orElse(null), depth);
            } else if (value instanceof OptionalInt o) {
                sb.append(o.isPresent() ? String.valueOf(o.getAsInt()) : "null");
            } else if (value instanceof OptionalLong o) {
                sb.append(o.isPresent() ? String.valueOf(o.getAsLong()) : "null");
            } else if (value instanceof OptionalDouble o) {
                if (o.isPresent()) {
                    writeNumber(sb, o.getAsDouble());
                } else {
                    sb.append("null");
                }
            } else if (value instanceof Date date) {
                writeString(sb, Instant.ofEpochMilli(date.getTime()).toString());
            } else if (value instanceof Locale locale) {
                writeString(sb, locale.toLanguageTag());
            } else if (value instanceof UUID || value instanceof URI || isJavaTime(value.getClass())) {
                writeString(sb, value.toString());
            } else {
                writeContainer(sb, value, depth);
            }
        }

        private void writeContainer(StringBuilder sb, Object value, int depth) {
            value = unwrapProxy(value);
            Class<?> type = value.getClass();
            boolean known = value instanceof Map || value instanceof Collection || type.isArray() || type.isRecord();
            if (!known && (isJdkType(type) || type.isHidden() || type.isSynthetic())) {
                throw error("Unsupported type " + type.getName());
            }
            if (!visiting.add(value)) {
                throw error("Circular reference to " + type.getName());
            }
            try {
                writeKnownContainer(sb, value, type, depth);
            } catch (S2JsonException e) {
                throw e;
            } catch (RuntimeException e) {
                // e.g. a lazy JPA collection iterated after its session closed | 예: 세션이 닫힌 뒤 순회한 JPA 지연 컬렉션
                throw new S2JsonException("Failed to write " + type.getName() + " at " + pathOf(path), e);
            } finally {
                visiting.remove(value);
            }
        }

        private void writeKnownContainer(StringBuilder sb, Object value, Class<?> type, int depth) {
            if (value instanceof Map<?, ?> map) {
                writeMap(sb, map, depth);
            } else if (value instanceof Collection<?> collection) {
                writeArray(sb, collection.iterator(), depth);
            } else if (type.isArray()) {
                writeArray(sb, arrayIterator(value), depth);
            } else if (type.isRecord()) {
                writeRecord(sb, value, depth);
            } else {
                writePojo(sb, value, depth);
            }
        }

        /**
         * Returns the real object behind a Hibernate or Spring AOP proxy. The proxy's own fields are empty (the state
         * lives in the target) and hold interceptor internals, so writing it directly would produce wrong JSON.
         * Unwrapping initializes a lazy Hibernate proxy, which needs an open session.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * Hibernate·Spring AOP 프록시 뒤의 실제 객체를 반환합니다. 프록시 자체의 필드는 비어 있고(상태는 대상 객체에 있음) 인터셉터
         * 내부값을 담고 있어, 그대로 쓰면 틀린 JSON 이 나옵니다. 지연 로딩 Hibernate 프록시는 꺼낼 때 초기화되므로 세션이 열려 있어야 합니다.
         */
        private Object unwrapProxy(Object value) {
            for (int i = 0; i < 4 && isProxy(value.getClass()); i++) {
                Object target = invokeNoArg(value, "getHibernateLazyInitializer", "getImplementation");
                if (target == null) {
                    target = invokeNoArg(value, "getTargetSource", "getTarget");
                }
                if (target == null) {
                    throw error("Cannot unwrap proxy " + value.getClass().getName()
                            + " (supported: Hibernate and Spring AOP proxies); convert it to a DTO first");
                }
                value = target;
            }
            return value;
        }

        /** Calls {@code first()} then {@code second()} on its result; null when {@code first} does not exist. | first() 결과에 second() 호출. first 가 없으면 null */
        private Object invokeNoArg(Object target, String first, String second) {
            java.lang.reflect.Method method;
            try {
                method = target.getClass().getMethod(first);
            } catch (NoSuchMethodException e) {
                return null;
            }
            try {
                method.setAccessible(true);
                Object holder = method.invoke(target);
                java.lang.reflect.Method next = holder.getClass().getMethod(second);
                next.setAccessible(true);
                return next.invoke(holder);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new S2JsonException("Cannot initialize proxy " + target.getClass().getName() + " at " + pathOf(path)
                        + " (a lazy entity needs an open session)", e.getCause());
            } catch (ReflectiveOperationException | RuntimeException e) {
                throw new S2JsonException("Cannot unwrap proxy " + target.getClass().getName() + " at " + pathOf(path), e);
            }
        }

        private void writeMap(StringBuilder sb, Map<?, ?> map, int depth) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = mapKey(entry.getKey());
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, key);
                sb.append(':');
                path.push(key);
                write(sb, entry.getValue(), depth + 1);
                path.pop();
            }
            sb.append('}');
        }

        private String mapKey(Object key) {
            if (key instanceof CharSequence || key instanceof Character || key instanceof Number || key instanceof Boolean
                    || key instanceof UUID) {
                return key.toString();
            }
            if (key instanceof Enum<?> e) {
                return e.name();
            }
            throw error("Unsupported map key type " + (key == null ? "null" : key.getClass().getName()));
        }

        private void writeArray(StringBuilder sb, Iterator<?> items, int depth) {
            sb.append('[');
            int index = 0;
            while (items.hasNext()) {
                if (index > 0) {
                    sb.append(',');
                }
                path.push(index);
                write(sb, items.next(), depth + 1);
                path.pop();
                index++;
            }
            sb.append(']');
        }

        private void writeRecord(StringBuilder sb, Object record, int depth) {
            sb.append('{');
            boolean first = true;
            RecordInfo info = recordInfo(record.getClass(), path);
            for (int i = 0; i < info.names().length; i++) {
                Object componentValue;
                try {
                    componentValue = info.accessors()[i].invoke(record);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    throw new S2JsonException("Cannot read record component " + info.names()[i] + " at " + pathOf(path), e);
                }
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, info.names()[i]);
                sb.append(':');
                path.push(info.names()[i]);
                write(sb, componentValue, depth + 1);
                path.pop();
            }
            sb.append('}');
        }

        private void writePojo(StringBuilder sb, Object pojo, int depth) {
            sb.append('{');
            boolean first = true;
            for (Field field : propertyFields(pojo.getClass())) {
                Object fieldValue;
                try {
                    fieldValue = field.get(pojo);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    throw new S2JsonException("Cannot read field " + field.getName() + " at " + pathOf(path), e);
                }
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, field.getName());
                sb.append(':');
                path.push(field.getName());
                write(sb, fieldValue, depth + 1);
                path.pop();
            }
            sb.append('}');
        }

        private void writeNumber(StringBuilder sb, Number number) {
            if (number instanceof Double || number instanceof Float) {
                double d = number.doubleValue();
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    if (!Feature.ALLOW_NON_NUMERIC_NUMBERS.in(flags)) {
                        throw error("NaN/Infinity is not valid JSON (enable ALLOW_NON_NUMERIC_NUMBERS)");
                    }
                    sb.append(Double.isNaN(d) ? "NaN" : d > 0 ? "Infinity" : "-Infinity");
                    return;
                }
                sb.append(number);
                return;
            }
            String text = number.toString();
            if (!(number instanceof BigDecimal) && !JSON_NUMBER.matcher(text).matches()) {
                throw error("Number " + text + " of " + number.getClass().getName() + " is not valid JSON");
            }
            sb.append(text);
        }
    }

    /** Writes a JSON string literal; also escapes U+2028/U+2029 so the output can be embedded in a script. | JSON 문자열 리터럴 기록. 스크립트에 넣을 수 있도록 U+2028/U+2029 도 이스케이프 */
    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20 || c == '\u2028' || c == '\u2029') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static Iterator<Object> arrayIterator(Object array) {
        int length = Array.getLength(array);
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < length;
            }

            @Override
            public Object next() {
                return Array.get(array, index++);
            }
        };
    }

    // =========================================================================
    // Parser
    // =========================================================================

    private static final class Parser {
        private final String json;
        private final int flags;
        private int pos;
        private int depth;

        Parser(String json, int flags) {
            this.json = json;
            this.flags = flags;
        }

        private S2JsonException error(String message) {
            return new S2JsonException(message, pos, null);
        }

        Object parseDocument() {
            skipWhitespace();
            if (pos >= json.length()) {
                throw error("Empty JSON");
            }
            Object value = parseValue();
            skipWhitespace();
            if (pos < json.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) {
                throw error("Unexpected end of JSON");
            }
            char c = json.charAt(pos);
            switch (c) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseString('"');
                case '\'':
                    if (Feature.ALLOW_SINGLE_QUOTES.in(flags)) {
                        return parseString('\'');
                    }
                    throw error("Single quotes are not allowed");
                case 't':
                    return literal("true", Boolean.TRUE);
                case 'f':
                    return literal("false", Boolean.FALSE);
                case 'n':
                    return literal("null", null);
                case 'N':
                    return nonNumeric("NaN", Double.NaN);
                case 'I':
                    return nonNumeric("Infinity", Double.POSITIVE_INFINITY);
                default:
                    if (c == '-' && json.startsWith("-Infinity", pos)) {
                        return nonNumeric("-Infinity", Double.NEGATIVE_INFINITY);
                    }
                    if (c == '-' || c == '+' || c == '.' || (c >= '0' && c <= '9')) {
                        return parseNumber();
                    }
                    throw error("Unexpected character '" + c + "'");
            }
        }

        private Object literal(String word, Object value) {
            if (!json.startsWith(word, pos)) {
                throw error("Invalid literal");
            }
            pos += word.length();
            return value;
        }

        private Object nonNumeric(String word, double value) {
            if (!Feature.ALLOW_NON_NUMERIC_NUMBERS.in(flags)) {
                throw error(word + " is not allowed (enable ALLOW_NON_NUMERIC_NUMBERS)");
            }
            return literal(word, value);
        }

        private void enter() {
            if (++depth > MAX_DEPTH) {
                throw error("Nesting deeper than " + MAX_DEPTH);
            }
        }

        private Map<String, Object> parseObject() {
            enter();
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // '{'
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                depth--;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseKey();
                skipWhitespace();
                if (peek() != ':') {
                    throw error("Expected ':' after key");
                }
                pos++;
                map.put(key, parseValue());
                skipWhitespace();
                char c = peek();
                if (c == '}') {
                    pos++;
                    break;
                }
                if (c != ',') {
                    throw error(c == 0 ? "Unexpected end of JSON" : "Expected ',' or '}'");
                }
                pos++;
                skipWhitespace();
                if (peek() == '}' && Feature.ALLOW_TRAILING_COMMA.in(flags)) {
                    pos++;
                    break;
                }
            }
            depth--;
            return map;
        }

        private String parseKey() {
            char c = peek();
            if (c == '"') {
                return parseString('"');
            }
            if (c == '\'' && Feature.ALLOW_SINGLE_QUOTES.in(flags)) {
                return parseString('\'');
            }
            if (Feature.ALLOW_UNQUOTED_FIELD_NAMES.in(flags) && (Character.isLetter(c) || c == '_' || c == '$')) {
                int start = pos;
                while (pos < json.length()
                        && (Character.isLetterOrDigit(json.charAt(pos)) || json.charAt(pos) == '_' || json.charAt(pos) == '$')) {
                    pos++;
                }
                return json.substring(start, pos);
            }
            throw error(c == 0 ? "Unexpected end of JSON" : "Expected a quoted key");
        }

        private List<Object> parseArray() {
            enter();
            List<Object> list = new ArrayList<>();
            pos++; // '['
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                depth--;
                return list;
            }
            while (true) {
                skipWhitespace();
                char c = peek();
                if ((c == ',' || c == ']') && Feature.ALLOW_MISSING_VALUES.in(flags)) {
                    list.add(null);
                } else {
                    list.add(parseValue());
                    skipWhitespace();
                    c = peek();
                }
                if (c == ']') {
                    pos++;
                    break;
                }
                if (c != ',') {
                    throw error(c == 0 ? "Unexpected end of JSON" : "Expected ',' or ']'");
                }
                pos++;
                skipWhitespace();
                if (peek() == ']' && Feature.ALLOW_TRAILING_COMMA.in(flags)) {
                    pos++;
                    break;
                }
            }
            depth--;
            return list;
        }

        private String parseString(char quote) {
            pos++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (c == quote) {
                    pos++;
                    return sb.toString();
                }
                if (c == '\\') {
                    pos++;
                    if (pos >= json.length()) {
                        break;
                    }
                    char e = json.charAt(pos);
                    switch (e) {
                        case '"', '\\', '/' -> sb.append(e);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            int code = 0;
                            for (int i = 1; i <= 4; i++) {
                                int digit = pos + i < json.length() ? Character.digit(json.charAt(pos + i), 16) : -1;
                                if (digit < 0) {
                                    throw error("Invalid unicode escape");
                                }
                                code = code * 16 + digit;
                            }
                            sb.append((char) code);
                            pos += 4;
                        }
                        default -> {
                            if ((e == '\'' && Feature.ALLOW_SINGLE_QUOTES.in(flags))
                                    || Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.in(flags)) {
                                sb.append(e);
                            } else {
                                throw error("Invalid escape sequence \\" + e);
                            }
                        }
                    }
                    pos++;
                } else {
                    if (c < 0x20 && !Feature.ALLOW_UNESCAPED_CONTROL_CHARS.in(flags)) {
                        throw error("Unescaped control character");
                    }
                    sb.append(c);
                    pos++;
                }
            }
            throw error("Unterminated string");
        }

        private Number parseNumber() {
            int start = pos;
            if (peek() == '+') {
                if (!Feature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS.in(flags)) {
                    throw error("Leading '+' is not allowed");
                }
                pos++;
            } else if (peek() == '-') {
                pos++;
            }
            int intStart = pos;
            int intDigits = digits();
            if (intDigits == 0 && peek() != '.') {
                throw error("Invalid number");
            }
            if (intDigits > 1 && json.charAt(intStart) == '0' && !Feature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.in(flags)) {
                throw error("Leading zeros are not allowed");
            }
            boolean decimal = false;
            if (peek() == '.') {
                decimal = true;
                pos++;
                int fractionDigits = digits();
                if (intDigits == 0 && !Feature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS.in(flags)) {
                    throw error("Leading decimal point is not allowed");
                }
                if (fractionDigits == 0 && (intDigits == 0 || !Feature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS.in(flags))) {
                    throw error("Trailing decimal point is not allowed");
                }
            }
            if (peek() == 'e' || peek() == 'E') {
                decimal = true;
                pos++;
                if (peek() == '+' || peek() == '-') {
                    pos++;
                }
                if (digits() == 0) {
                    throw error("Invalid exponent");
                }
            }
            if (pos - start > MAX_NUMBER_LENGTH) {
                throw new S2JsonException("Number longer than " + MAX_NUMBER_LENGTH + " characters", start, null);
            }
            String text = json.substring(start, pos);
            if (text.startsWith("+")) {
                text = text.substring(1);
            }
            if (text.endsWith(".")) {
                text = text + "0";
            }
            try {
                if (!decimal) {
                    BigInteger big = new BigInteger(text);
                    return big.bitLength() < 64 ? (Number) big.longValue() : big;
                }
                if (Feature.USE_BIG_DECIMAL_FOR_FLOATS.in(flags)) {
                    return new BigDecimal(text);
                }
                double d = Double.parseDouble(text);
                if (Double.isInfinite(d)) {
                    throw error("Number out of double range (use USE_BIG_DECIMAL_FOR_FLOATS)");
                }
                return d;
            } catch (NumberFormatException e) {
                throw new S2JsonException("Invalid number", start, e);
            }
        }

        private int digits() {
            int start = pos;
            while (pos < json.length() && json.charAt(pos) >= '0' && json.charAt(pos) <= '9') {
                pos++;
            }
            return pos - start;
        }

        private char peek() {
            return pos < json.length() ? json.charAt(pos) : 0;
        }

        private void skipWhitespace() {
            while (pos < json.length()) {
                char c = json.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else if (c == '/' && Feature.ALLOW_JAVA_COMMENTS.in(flags) && pos + 1 < json.length()
                        && json.charAt(pos + 1) == '/') {
                    skipLine();
                } else if (c == '/' && Feature.ALLOW_JAVA_COMMENTS.in(flags) && pos + 1 < json.length()
                        && json.charAt(pos + 1) == '*') {
                    int end = json.indexOf("*/", pos + 2);
                    if (end < 0) {
                        throw error("Unterminated comment");
                    }
                    pos = end + 2;
                } else if (c == '#' && Feature.ALLOW_YAML_COMMENTS.in(flags)) {
                    skipLine();
                } else {
                    return;
                }
            }
        }

        private void skipLine() {
            while (pos < json.length() && json.charAt(pos) != '\n') {
                pos++;
            }
        }
    }

    // =========================================================================
    // Mapper
    // =========================================================================

    /** Parsers for string-valued JDK types | 문자열로 표현하는 JDK 타입 파서 */
    private static final Map<Class<?>, Function<String, Object>> STRING_TYPES = new LinkedHashMap<>();

    static {
        STRING_TYPES.put(LocalDate.class, LocalDate::parse);
        STRING_TYPES.put(LocalDateTime.class, LocalDateTime::parse);
        STRING_TYPES.put(LocalTime.class, LocalTime::parse);
        STRING_TYPES.put(OffsetDateTime.class, OffsetDateTime::parse);
        STRING_TYPES.put(OffsetTime.class, OffsetTime::parse);
        STRING_TYPES.put(ZonedDateTime.class, ZonedDateTime::parse);
        STRING_TYPES.put(Instant.class, Instant::parse);
        STRING_TYPES.put(YearMonth.class, YearMonth::parse);
        STRING_TYPES.put(Year.class, Year::parse);
        STRING_TYPES.put(MonthDay.class, MonthDay::parse);
        STRING_TYPES.put(Duration.class, Duration::parse);
        STRING_TYPES.put(Period.class, Period::parse);
        STRING_TYPES.put(ZoneId.class, ZoneId::of);
        STRING_TYPES.put(ZoneOffset.class, ZoneOffset::of);
        STRING_TYPES.put(UUID.class, UUID::fromString);
        STRING_TYPES.put(URI.class, URI::create);
        STRING_TYPES.put(Locale.class, Locale::forLanguageTag);
    }

    private static final class Mapper {
        private final Deque<Object> path = new ArrayDeque<>();

        private S2JsonException error(String message) {
            return new S2JsonException(message + " at " + pathOf(path));
        }

        Object convert(Object node, Type type, int depth) {
            if (depth > MAX_DEPTH) {
                throw error("Nesting deeper than " + MAX_DEPTH);
            }
            Class<?> raw = rawClass(type);
            if (node == null) {
                if (raw.isPrimitive()) {
                    throw error("null cannot be mapped to " + raw.getName());
                }
                if (raw == Optional.class) {
                    return Optional.empty();
                }
                return raw == OptionalInt.class ? OptionalInt.empty()
                        : raw == OptionalLong.class ? OptionalLong.empty()
                                : raw == OptionalDouble.class ? OptionalDouble.empty() : null;
            }
            if (raw == Object.class) {
                return node;
            }
            if (raw == String.class || raw == CharSequence.class) {
                return expect(node, String.class, raw);
            }
            if (raw == Character.class || raw == char.class) {
                String s = expect(node, String.class, raw);
                if (s.length() != 1) {
                    throw error("Expected a single character but was \"" + s + "\"");
                }
                return s.charAt(0);
            }
            if (raw == Boolean.class || raw == boolean.class) {
                return expect(node, Boolean.class, raw);
            }
            if (Number.class.isAssignableFrom(box(raw))) {
                return toNumber(expect(node, Number.class, raw), box(raw));
            }
            if (raw.isEnum()) {
                return toEnum(expect(node, String.class, raw), raw);
            }
            if (raw == Optional.class) {
                return Optional.ofNullable(convert(node, typeArgument(type, 0), depth));
            }
            if (raw == OptionalInt.class) {
                return OptionalInt.of((Integer) toNumber(expect(node, Number.class, raw), Integer.class));
            }
            if (raw == OptionalLong.class) {
                return OptionalLong.of((Long) toNumber(expect(node, Number.class, raw), Long.class));
            }
            if (raw == OptionalDouble.class) {
                return OptionalDouble.of((Double) toNumber(expect(node, Number.class, raw), Double.class));
            }
            if (raw == Date.class) {
                if (node instanceof Number n) {
                    return new Date(n.longValue());
                }
                return Date.from((Instant) parseString(expect(node, String.class, raw), Instant.class, Instant::parse));
            }
            Function<String, Object> stringType = STRING_TYPES.get(raw);
            if (stringType != null) {
                return parseString(expect(node, String.class, raw), raw, stringType);
            }
            if (raw.isArray()) {
                List<?> list = expect(node, List.class, raw);
                Type componentType = type instanceof GenericArrayType g ? g.getGenericComponentType() : raw.getComponentType();
                Object array = Array.newInstance(raw.getComponentType(), list.size());
                for (int i = 0; i < list.size(); i++) {
                    path.push(i);
                    Array.set(array, i, convert(list.get(i), componentType, depth + 1));
                    path.pop();
                }
                return array;
            }
            if (Collection.class.isAssignableFrom(raw) || raw == Iterable.class) {
                return toCollection(expect(node, List.class, raw), raw, typeArgument(type, 0), depth);
            }
            if (Map.class.isAssignableFrom(raw)) {
                return toMap(expect(node, Map.class, raw), raw, typeArgument(type, 0), typeArgument(type, 1), depth);
            }
            if (raw.isRecord()) {
                return toRecord(expect(node, Map.class, raw), raw, depth);
            }
            if (isJdkType(raw) || raw.isInterface() || Modifier.isAbstract(raw.getModifiers())) {
                throw error("Unsupported target type " + raw.getName());
            }
            return toPojo(expect(node, Map.class, raw), raw, depth);
        }

        private <V> V expect(Object node, Class<V> jsonType, Class<?> target) {
            if (!jsonType.isInstance(node)) {
                throw error("Expected " + describeJsonType(jsonType) + " for " + target.getSimpleName() + " but was "
                        + describe(node));
            }
            return jsonType.cast(node);
        }

        private Object parseString(String text, Class<?> target, Function<String, ?> parser) {
            try {
                return parser.apply(text);
            } catch (RuntimeException e) {
                throw new S2JsonException("Cannot parse \"" + text + "\" as " + target.getSimpleName() + " at " + pathOf(path), e);
            }
        }

        private Object toNumber(Number number, Class<?> target) {
            if (target == Number.class) {
                return number;
            }
            if (target == Double.class || target == Float.class) {
                double d = number.doubleValue();
                boolean sourceFinite = !(number instanceof Double || number instanceof Float) || Double.isFinite(d);
                if (sourceFinite && !Double.isFinite(d)) {
                    throw error("Number " + number + " is out of double range");
                }
                if (target == Float.class && Double.isFinite(d) && Math.abs(d) > Float.MAX_VALUE) {
                    throw error("Number " + number + " is out of float range");
                }
                return target == Double.class ? (Object) d : (Object) (float) d;
            }
            BigDecimal decimal;
            if (number instanceof Double || number instanceof Float) {
                if (!Double.isFinite(number.doubleValue())) {
                    throw error("Number " + number + " cannot be mapped to " + target.getSimpleName());
                }
                decimal = BigDecimal.valueOf(number.doubleValue());
            } else {
                decimal = new BigDecimal(number.toString());
            }
            if (target == BigDecimal.class) {
                return decimal;
            }
            if (decimal.signum() != 0 && (long) decimal.precision() - decimal.scale() > MAX_NUMBER_LENGTH) {
                throw error("Number " + number + " has more than " + MAX_NUMBER_LENGTH + " integer digits");
            }
            try {
                if (target == BigInteger.class) {
                    return decimal.toBigIntegerExact();
                }
                if (target == Long.class) {
                    return decimal.longValueExact();
                }
                if (target == Integer.class) {
                    return decimal.intValueExact();
                }
                if (target == Short.class) {
                    return decimal.shortValueExact();
                }
                if (target == Byte.class) {
                    return decimal.byteValueExact();
                }
            } catch (ArithmeticException e) {
                throw error("Number " + number + " does not fit " + target.getSimpleName() + " without loss");
            }
            throw error("Unsupported number type " + target.getName());
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private Object toEnum(String name, Class<?> enumType) {
            try {
                return Enum.valueOf((Class<? extends Enum>) enumType, name);
            } catch (IllegalArgumentException e) {
                throw error("\"" + name + "\" is not a constant of " + enumType.getSimpleName());
            }
        }

        @SuppressWarnings("unchecked")
        private Object toCollection(List<?> list, Class<?> raw, Type elementType, int depth) {
            Collection<Object> result;
            if (raw.isInterface() || Modifier.isAbstract(raw.getModifiers())) {
                if (raw.isAssignableFrom(ArrayList.class)) {
                    result = new ArrayList<>(list.size());
                } else if (raw.isAssignableFrom(LinkedHashSet.class)) {
                    result = new LinkedHashSet<>();
                } else if (raw.isAssignableFrom(TreeSet.class)) {
                    result = new TreeSet<>();
                } else {
                    throw error("Unsupported collection type " + raw.getName());
                }
            } else {
                result = (Collection<Object>) instantiate(raw);
            }
            for (int i = 0; i < list.size(); i++) {
                path.push(i);
                result.add(convert(list.get(i), elementType, depth + 1));
                path.pop();
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        private Object toMap(Map<?, ?> node, Class<?> raw, Type keyType, Type valueType, int depth) {
            Map<Object, Object> result;
            if (raw.isInterface() || Modifier.isAbstract(raw.getModifiers())) {
                if (raw.isAssignableFrom(LinkedHashMap.class)) {
                    result = new LinkedHashMap<>();
                } else if (raw == SortedMap.class || raw == NavigableMap.class) {
                    result = new TreeMap<>();
                } else {
                    throw error("Unsupported map type " + raw.getName());
                }
            } else {
                result = (Map<Object, Object>) instantiate(raw);
            }
            Class<?> keyClass = rawClass(keyType);
            for (Map.Entry<?, ?> entry : node.entrySet()) {
                String key = String.valueOf(entry.getKey());
                path.push(key);
                Object mappedKey = keyClass == Object.class || keyClass == String.class ? key
                        : keyClass.isEnum() ? toEnum(key, keyClass)
                                : STRING_TYPES.containsKey(keyClass) ? parseString(key, keyClass, STRING_TYPES.get(keyClass))
                                        : Number.class.isAssignableFrom(box(keyClass)) ? toNumber(numberKey(key), box(keyClass))
                                                : null;
                if (mappedKey == null) {
                    throw error("Unsupported map key type " + keyClass.getName());
                }
                result.put(mappedKey, convert(entry.getValue(), valueType, depth + 1));
                path.pop();
            }
            return result;
        }

        private Number numberKey(String key) {
            try {
                return new BigDecimal(key);
            } catch (NumberFormatException e) {
                throw error("Map key \"" + key + "\" is not a number");
            }
        }

        private Object toRecord(Map<?, ?> node, Class<?> type, int depth) {
            RecordInfo info = recordInfo(type, path);
            Object[] args = new Object[info.names().length];
            for (int i = 0; i < args.length; i++) {
                String name = info.names()[i];
                path.push(name);
                args[i] = node.containsKey(name) ? convert(node.get(name), info.types()[i], depth + 1)
                        : defaultValue(info.accessors()[i].getReturnType());
                path.pop();
            }
            try {
                return info.constructor().newInstance(args);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e;
                throw new S2JsonException("Cannot create record " + type.getName() + " at " + pathOf(path), cause);
            }
        }

        private Object toPojo(Map<?, ?> node, Class<?> type, int depth) {
            // Without a no-arg constructor it is an immutable value object, created through its constructor | 인자 없는 생성자가 없으면 불변 VO 로 보고 생성자로 생성
            Set<String> assigned = new java.util.HashSet<>();
            Object instance = NO_ARG_CONSTRUCTOR.get(type).isPresent() ? instantiate(type) : construct(node, type, assigned, depth);
            for (Field field : propertyFields(type)) {
                if (!node.containsKey(field.getName()) || assigned.contains(field.getName())) {
                    continue;
                }
                path.push(field.getName());
                Object value = convert(node.get(field.getName()), field.getGenericType(), depth + 1);
                try {
                    field.set(instance, value);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    throw new S2JsonException("Cannot set field " + field.getName() + " at " + pathOf(path), e);
                }
                path.pop();
            }
            return instance;
        }

        /**
         * Creates an object without a no-arg constructor by matching JSON keys to constructor parameter names. Uses the
         * only constructor, or else the one whose parameters are exactly the property fields. Parameter names require
         * compiling with {@code -parameters} (on by default in Spring Boot and s2-build-support).
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 인자 없는 생성자가 없는 객체를 JSON 키와 생성자 파라미터 이름을 맞춰 만듭니다. 생성자가 하나면 그것을, 여럿이면 파라미터가 속성 필드와
         * 정확히 같은 생성자를 씁니다. 파라미터 이름은 {@code -parameters}로 컴파일해야 알 수 있습니다(Spring Boot, s2-build-support 기본값).
         */
        private Object construct(Map<?, ?> node, Class<?> type, Set<String> assigned, int depth) {
            Constructor<?> constructor = selectConstructor(type);
            java.lang.reflect.Parameter[] params = constructor.getParameters();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                if (!params[i].isNamePresent()) {
                    throw error(type.getName() + " has no no-arg constructor and its constructor parameter names are not "
                            + "available; compile with -parameters, add a no-arg constructor, or use a record");
                }
                String name = params[i].getName();
                path.push(name);
                args[i] = node.containsKey(name) ? convert(node.get(name), params[i].getParameterizedType(), depth + 1)
                        : defaultValue(params[i].getType());
                path.pop();
                assigned.add(name);
            }
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(args);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e;
                throw new S2JsonException("Cannot create " + type.getName() + " at " + pathOf(path), cause);
            }
        }

        private Constructor<?> selectConstructor(Class<?> type) {
            List<Constructor<?>> candidates = new ArrayList<>();
            for (Constructor<?> c : type.getDeclaredConstructors()) {
                if (!c.isSynthetic()) {
                    candidates.add(c);
                }
            }
            if (candidates.size() == 1) {
                return candidates.get(0);
            }
            List<String> fieldNames = propertyFields(type).stream().map(Field::getName).toList();
            for (Constructor<?> c : candidates) {
                List<String> paramNames = java.util.Arrays.stream(c.getParameters())
                        .map(p -> p.isNamePresent() ? p.getName() : "").toList();
                if (paramNames.equals(fieldNames)) {
                    return c;
                }
            }
            throw error(type.getName() + " has no no-arg constructor and several constructors; add a no-arg constructor "
                    + "or one whose parameters are all fields in declaration order");
        }

        private Object instantiate(Class<?> type) {
            Constructor<?> constructor = NO_ARG_CONSTRUCTOR.get(type).orElse(null);
            if (constructor == null) {
                throw error(type.getName() + " needs a no-arg constructor");
            }
            try {
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (ReflectiveOperationException | RuntimeException e) {
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e;
                throw new S2JsonException("Cannot create " + type.getName() + " at " + pathOf(path), cause);
            }
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Record metadata resolved once per class | 클래스별로 한 번 구하는 record 메타데이터 */
    private record RecordInfo(String[] names, java.lang.reflect.Method[] accessors, Type[] types, Constructor<?> constructor) {
    }

    private static final ClassValue<RecordInfo> RECORD_INFO = new ClassValue<>() {
        @Override
        protected RecordInfo computeValue(Class<?> type) {
            RecordComponent[] components = type.getRecordComponents();
            String[] names = new String[components.length];
            java.lang.reflect.Method[] accessors = new java.lang.reflect.Method[components.length];
            Type[] types = new Type[components.length];
            Class<?>[] rawTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                names[i] = components[i].getName();
                accessors[i] = components[i].getAccessor();
                accessors[i].setAccessible(true);
                types[i] = components[i].getGenericType();
                rawTypes[i] = components[i].getType();
            }
            try {
                Constructor<?> constructor = type.getDeclaredConstructor(rawTypes);
                constructor.setAccessible(true);
                return new RecordInfo(names, accessors, types, constructor);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("No canonical constructor in " + type.getName(), e);
            }
        }
    };

    private static RecordInfo recordInfo(Class<?> type, Deque<Object> path) {
        try {
            return RECORD_INFO.get(type);
        } catch (RuntimeException e) {
            throw new S2JsonException("Cannot access record " + type.getName() + " at " + pathOf(path), e);
        }
    }

    /** The declared no-arg constructor per class, if any | 클래스별 선언된 인자 없는 생성자 (없으면 빈 값) */
    private static final ClassValue<Optional<Constructor<?>>> NO_ARG_CONSTRUCTOR = new ClassValue<>() {
        @Override
        protected Optional<Constructor<?>> computeValue(Class<?> type) {
            try {
                return Optional.of(type.getDeclaredConstructor());
            } catch (NoSuchMethodException e) {
                return Optional.empty();
            }
        }
    };

    /** Property fields per class; ClassValue does not keep classes from being unloaded | 클래스별 속성 필드. ClassValue 는 클래스 언로드를 막지 않음 */
    private static final ClassValue<List<Field>> PROPERTY_FIELDS = new ClassValue<>() {
        @Override
        protected List<Field> computeValue(Class<?> type) {
            return List.copyOf(collectPropertyFields(type));
        }
    };

    private static List<Field> propertyFields(Class<?> type) {
        return PROPERTY_FIELDS.get(type);
    }

    /** Non-static, non-transient, non-synthetic fields of a class and its non-JDK superclasses, superclass first | 클래스와 JDK 가 아닌 상위 클래스의 속성 필드 (상위 먼저) */
    private static List<Field> collectPropertyFields(Class<?> type) {
        Deque<Class<?>> hierarchy = new ArrayDeque<>();
        for (Class<?> c = type; c != null && !isJdkType(c); c = c.getSuperclass()) {
            hierarchy.push(c);
        }
        List<Field> fields = new ArrayList<>();
        for (Class<?> c : hierarchy) {
            for (Field field : c.getDeclaredFields()) {
                int mod = field.getModifiers();
                // $$_hibernate_* fields are added by Hibernate bytecode enhancement, not entity state | $$_hibernate_* 필드는 Hibernate 바이트코드 강화가 추가한 것으로 엔티티 상태가 아님
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || field.isSynthetic()
                        || field.getName().startsWith("$$_hibernate_")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                } catch (RuntimeException e) {
                    throw new S2JsonException("Cannot access field " + c.getName() + "." + field.getName()
                            + " (open the package to io.github.devers2.s2util)", e);
                }
                fields.add(field);
            }
        }
        return fields;
    }

    private static boolean isJdkType(Class<?> type) {
        String name = type.getName();
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.") || name.startsWith("sun.")
                || name.startsWith("com.sun.");
    }

    /** Proxy class per the s2-core convention ({@link io.github.devers2.s2util.core.S2Cache#getRealClass(Object)}), excluding lambdas | s2-core 규칙상 프록시 클래스 (람다 제외) */
    private static boolean isProxy(Class<?> type) {
        String name = type.getName();
        return !type.isHidden() && (name.contains("$$") || name.contains("CGLIB") || name.contains("HibernateProxy"));
    }

    private static boolean isJavaTime(Class<?> type) {
        return type.getName().startsWith("java.time.");
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType p) {
            return (Class<?>) p.getRawType();
        }
        if (type instanceof GenericArrayType g) {
            return Array.newInstance(rawClass(g.getGenericComponentType()), 0).getClass();
        }
        if (type instanceof WildcardType w) {
            return rawClass(w.getUpperBounds()[0]);
        }
        if (type instanceof TypeVariable<?> v) {
            return rawClass(v.getBounds()[0]);
        }
        return Object.class;
    }

    private static Type typeArgument(Type type, int index) {
        if (type instanceof ParameterizedType p && p.getActualTypeArguments().length > index) {
            return p.getActualTypeArguments()[index];
        }
        return Object.class;
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class)
            return Integer.class;
        if (type == long.class)
            return Long.class;
        if (type == double.class)
            return Double.class;
        if (type == float.class)
            return Float.class;
        if (type == boolean.class)
            return Boolean.class;
        if (type == char.class)
            return Character.class;
        if (type == short.class)
            return Short.class;
        if (type == byte.class)
            return Byte.class;
        return Void.class;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return Array.get(Array.newInstance(type, 1), 0);
    }

    /** Formats path segments (property names and array indexes, innermost first) only when an error needs them | 경로 조각(속성 이름, 배열 번호)은 오류가 날 때만 문자열로 조립 */
    private static String pathOf(Deque<Object> path) {
        StringBuilder sb = new StringBuilder("$");
        Iterator<Object> it = path.descendingIterator();
        while (it.hasNext()) {
            Object segment = it.next();
            if (segment instanceof Integer index) {
                sb.append('[').append(index).append(']');
            } else {
                sb.append('.').append(segment);
            }
        }
        return sb.toString();
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map) {
            return "an object";
        }
        if (value instanceof List) {
            return "an array";
        }
        if (value instanceof String) {
            return "a string";
        }
        if (value instanceof Number) {
            return "a number";
        }
        if (value instanceof Boolean) {
            return "a boolean";
        }
        return value.getClass().getSimpleName();
    }

    private static String describeJsonType(Class<?> jsonType) {
        if (jsonType == Map.class) {
            return "an object";
        }
        if (jsonType == List.class) {
            return "an array";
        }
        if (jsonType == String.class) {
            return "a string";
        }
        if (jsonType == Number.class) {
            return "a number";
        }
        return "a boolean";
    }
}
