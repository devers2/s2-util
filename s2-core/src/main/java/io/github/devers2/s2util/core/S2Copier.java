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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import io.github.devers2.s2util.exception.S2PropertyAccessException;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * High-performance object copy utility leveraging {@link S2Util}'s efficient data access mechanisms.
 * <p>
 * This class provides a fluent builder pattern for copying data between objects (typically DTOs/VOs)
 * with support for field mapping, field exclusion, and null-aware copying strategies. It uses
 * {@link java.lang.invoke.MethodHandle}-based field access under the hood for optimal performance.
 * </p>
 *
 * <p>
 * <b>Usage Example:</b>
 * </p>
 *
 * <pre>{@code
 * UserDto userDto = S2Copier.from(userEntity)
 *         .map("userId", "id")
 *         .exclude("password")
 *         .ignoreNulls()
 *         .to(UserDto.class);
 * }</pre>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util의 효율적인 데이터 접근 메커니즘을 활용한 고성능 객체 복사 유틸리티 클래스입니다.
 * <p>
 * 유연한 빌더 패턴을 제공하여 객체 간(주로 DTO/VO) 데이터 복사를 수행하며,
 * 필드 매핑, 필드 제외, null 처리 전략 등을 지원합니다.
 * 내부적으로는 {@link java.lang.invoke.MethodHandle} 기반의 필드 접근을 사용하여
 * 최적의 성능을 보장합니다.
 * </p>
 *
 * @author devers2
 * @version 1.0
 * @since 2026. 01. 24.
 * @see S2Util
 * @see S2Cache
 */
public class S2Copier<S> {

    private static final S2Logger logger = S2LogManager.getLogger(S2Copier.class);

    private final S source;
    private final Map<String, String> fieldMapping = new HashMap<>();
    private final Set<String> excludedFields = new HashSet<>();
    private final Set<String> deepCopyFields = new HashSet<>();
    private boolean ignoreNulls = false;
    private boolean deepCopyAll = false;
    private static final int MAX_DEPTH = 100; // 순환참조 방지를 위한 깊이 제한

    private S2Copier(S source) {
        this.source = source;
    }

    /**
     * Creates and initializes an S2Copier instance with the source object.
     * <p>
     * This is the entry point for the fluent copying workflow. The source object serves as the
     * data provider for the subsequent {@code to()} operations.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 복사할 원본 객체(Source)를 지정하여 S2Copier 인스턴스를 생성합니다.
     * <p>
     * 유연한 복사 워크플로우의 시작점이며, 원본 객체는 이후 {@code to()} 작업의
     * 데이터 제공자 역할을 합니다.
     * </p>
     *
     * @param <S>    Source object type | 원본 객체의 타입
     * @param source The source object to copy from (must not be null) | 복사할 원본 객체 (null이 아니어야 함)
     * @return A new S2Copier instance initialized with the source object | 초기화된 S2Copier 인스턴스
     * @throws IllegalArgumentException if source is null | source가 null인 경우
     */
    public static <S> S2Copier<S> from(S source) {
        if (source == null) {
            throw new IllegalArgumentException("Source object cannot be null.");
        }
        return new S2Copier<>(source);
    }

    /**
     * Adds a field mapping rule between source and target field names.
     * <p>
     * Use this method when the source object's field name differs from the target object's field name.
     * Multiple mappings can be chained together. If a target field is not explicitly mapped,
     * the method will attempt to use a source field with the same name.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 원본 객체의 필드명과 대상 객체의 필드명이 다를 때 매핑 규칙을 추가합니다.
     * <p>
     * 여러 매핑을 체이닝하여 추가할 수 있으며, 매핑되지 않은 대상 필드는
     * 같은 이름의 원본 필드를 자동으로 탐색합니다.
     * </p>
     *
     * @param sourceFieldName The field name in the source object | 원본 객체의 필드명
     * @param targetFieldName The field name in the target object | 대상 객체의 필드명
     * @return This S2Copier instance for method chaining | 메서드 체이닝을 위한 이 인스턴스
     */
    public S2Copier<S> map(String sourceFieldName, String targetFieldName) {
        this.fieldMapping.put(targetFieldName, sourceFieldName);
        return this;
    }

    /**
     * Specifies target fields to exclude from the copy operation.
     * <p>
     * This method is useful when certain fields (e.g., sensitive data like passwords or internal IDs)
     * should not be copied to the target object. Multiple exclusions can be added by chaining
     * multiple {@code exclude()} calls or by passing multiple field names as varargs.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 복사 작업에서 제외할 대상 필드명을 지정합니다.
     * <p>
     * 암호나 내부 ID와 같이 민감한 데이터를 대상 객체로 복사하지 않아야 할 때 유용합니다.
     * 여러 제외 필드를 추가할 수 있으며, 여러 번 체이닝하거나 가변 인자로 전달할 수 있습니다.
     * </p>
     *
     * @param fieldNames The target field names to exclude (varargs) | 제외할 필드명 목록 (가변 인자)
     * @return This S2Copier instance for method chaining | 메서드 체이닝을 위한 이 인스턴스
     */
    public S2Copier<S> exclude(String... fieldNames) {
        if (fieldNames != null) {
            for (var name : fieldNames) {
                this.excludedFields.add(name);
            }
        }
        return this;
    }

    /**
     * Configures the copier to skip fields with null values in the source object.
     * <p>
     * When enabled, any source field with a null value will not be copied to the target object,
     * allowing the target to retain its original values. This is particularly useful when performing
     * partial updates where null values should not overwrite existing target data.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 원본 객체의 값이 null인 필드를 복사하지 않도록 설정합니다.
     * <p>
     * 활성화되면 원본의 null 필드는 대상 객체로 복사되지 않으며,
     * 대상이 기존 값을 유지하게 됩니다. 부분 업데이트 시 null 값이 기존 데이터를
     * 덮어쓰지 않아야 할 때 유용합니다.
     * </p>
     *
     * @return This S2Copier instance for method chaining | 메서드 체이닝을 위한 이 인스턴스
     */
    public S2Copier<S> ignoreNulls() {
        this.ignoreNulls = true;
        return this;
    }

    /**
     * Enables deep copying for all object fields.
     * <p>
     * Deep copy creates independent copies of nested objects, collections, and maps,
     * preventing unintended modifications to shared reference objects.
     * Circular references are automatically detected and handled to prevent infinite loops.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 모든 객체 필드에 대해 깊은 복사를 활성화합니다.
     * <p>
     * 깊은 복사는 중첩 객체, 컬렉션, Map을 독립적으로 복사하여
     * 공유 참조 객체의 의도하지 않은 수정을 방지합니다.
     * 순환참조는 자동으로 감지되어 무한 루프를 방지합니다.
     * </p>
     *
     * @return This S2Copier instance for method chaining | 메서드 체이닝을 위한 이 인스턴스
     */
    public S2Copier<S> deep() {
        this.deepCopyAll = true;
        return this;
    }

    /**
     * Enables deep copying for specific fields only.
     * <p>
     * Only the specified fields will be deeply copied; all other fields are shallow copied.
     * This is useful when you want deep copies of collection fields while keeping reference copies
     * of simple object fields for performance reasons.
     * Circular references within specified fields are automatically detected and handled.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 필드에 대해서만 깊은 복사를 활성화합니다.
     * <p>
     * 지정된 필드만 깊은 복사되며, 나머지는 얕은 복사됩니다.
     * 컬렉션 필드는 깊게 복사하면서 단순 객체 필드는 참조 복사로 유지하고 싶을 때 유용합니다.
     * 지정된 필드 내의 순환참조는 자동으로 감지되고 처리됩니다.
     * </p>
     *
     * @param fieldNames The field names to deep copy (varargs) | 깊은 복사할 필드명 목록 (가변 인자)
     * @return This S2Copier instance for method chaining | 메서드 체이닝을 위한 이 인스턴스
     */
    public S2Copier<S> deepOnly(String... fieldNames) {
        if (fieldNames != null) {
            for (var name : fieldNames) {
                this.deepCopyFields.add(name);
            }
        }
        return this;
    }

    /**
     * Executes the copy operation, transferring data from the source to the specified target object.
     * <p>
     * This method respects all previously configured rules (field mappings, exclusions, and null-handling).
     * Fields are accessed using {@link S2Util#getValue(Object, Object)} and {@link S2Util#setValue(Object, Object, Class, Object)},
     * ensuring optimal performance through MethodHandle caching. A target field is copied only when the source has that
     * property; a value of a different type is converted with {@link S2Util#cast(Object, Class)} when possible. Other
     * field-level copy errors (no setter, unconvertible value) skip the field and are logged at DEBUG, while getter/setter
     * failures and an exceeded deep-copy depth are thrown.
     * </p>
     *
     * <p>
     * <b>Behavior by Target Type:</b>
     * <ul>
     * <li><b>Map Target:</b> Uses source object's fields as the basis. Iterates through source fields and copies values to Map keys.
     * If a field mapping exists for a source field, the mapped key name is used; otherwise, the source field name is used as the Map key.</li>
     * <li><b>Typed Object Target (DTO/VO/Entity):</b> Uses target object's fields as the basis. Iterates through target fields and copies values from source.
     * If a field mapping exists for a target field, the mapped source field name is used to fetch the value.</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 원본에서 지정된 대상 객체로 데이터를 복사합니다.
     * <p>
     * 이전에 설정한 모든 규칙(필드 매핑, 제외 목록, null 처리)을 준수합니다. 대상 필드는 원본에 그 속성이 있을 때만 복사하며,
     * 타입이 다르면 가능할 때 {@link S2Util#cast(Object, Class)} 로 변환합니다. 그 밖의 필드 단위 오류(쓸 곳이 없음, 변환 불가)는
     * 그 필드를 건너뛰고 DEBUG 로그를 남기며, getter·setter 자체의 실패와 깊은 복사 깊이 초과는 예외로 알립니다.
     * </p>
     * <p>
     * <b>대상 타입별 동작:</b>
     * <ul>
     * <li><b>Map 대상:</b> 원본 객체의 필드를 기준으로 사용합니다. 원본의 각 필드를 읽어 Map 키에 저장합니다.
     * 필드 매핑이 있으면 매핑된 키 이름을 사용하고, 없으면 원본 필드명을 Map 키로 사용합니다.</li>
     * <li><b>일반 객체 대상 (DTO/VO/Entity):</b> 대상 객체의 필드를 기준으로 사용합니다. 대상의 각 필드에 대해 원본에서 값을 읽습니다.
     * 필드 매핑이 있으면 매핑된 원본 필드명을 사용하여 값을 가져옵니다.</li>
     * </ul>
     * </p>
     *
     * @param <T>    Target object type | 대상 객체의 타입
     * @param target The target object to populate with copied values (must not be null) | 값을 채워넣을 대상 객체 (null이 아니어야 함)
     * @return The target object with copied values | 복사된 값으로 채워진 대상 객체
     * @throws IllegalArgumentException if target is null or a record instance (use {@link #to(Class)}) | target이 null 이거나 레코드 인스턴스인 경우 ({@link #to(Class)} 사용)
     * @throws io.github.devers2.s2util.exception.S2PropertyAccessException if a source getter throws (exclude such fields with {@link #exclude(String...)}) | 원본 getter 가 예외를 던질 때 ({@link #exclude(String...)} 로 제외 가능)
     */
    @SuppressWarnings("unchecked")
    public <T> T to(T target) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null.");
        }

        // Map 타겟의 경우: source의 필드를 기준으로 복사
        if (target instanceof Map) {
            return toMap((Map<String, Object>) target);
        }
        if (target.getClass().isRecord()) {
            throw new IllegalArgumentException("A record cannot be modified; use to(" + target.getClass().getSimpleName()
                    + ".class) to create a copy (한국어: 레코드는 바꿀 수 없으니 to(클래스)로 새로 만드십시오)");
        }

        // 일반 객체 타겟: target의 필드(상속 필드 포함)를 기준으로 복사
        var fields = copyFields(S2Cache.getRealClass(target));
        // Create a new visited map for deep copy tracking
        var visited = new IdentityHashMap<Object, Object>();

        for (var field : fields) {
            var targetFieldName = field.getName();

            // 1. 제외 목록에 포함된 필드인지 확인
            if (excludedFields.contains(targetFieldName)) {
                continue;
            }

            // 2. 매핑 설정 확인 (없으면 자기 이름 사용)
            var sourceFieldName = fieldMapping.getOrDefault(targetFieldName, targetFieldName);

            // 2-1. 원본에 없는 속성은 대상 값을 그대로 둔다(일부 필드만 가진 DTO 로 엔티티를 갱신할 때 id 등이 null 로 지워지지 않도록)
            if (!sourceHasProperty(sourceFieldName)) {
                continue;
            }

            try {
                // 3. Source에서 값을 그대로 가져온다(빈 문자열·빈 컬렉션을 null 로 바꾸지 않음).
                var value = S2Util.getValue(source, sourceFieldName);
                // 4. Null 무시 설정 체크
                if (value == null && ignoreNulls) {
                    continue;
                }

                // 5. Deep copy 필요 여부 확인
                if (value != null && (deepCopyAll || deepCopyFields.contains(sourceFieldName))) {
                    value = deepCopyValue(value, field.getType(), visited, 0);
                }

                // 6. Target에 값을 설정한다. 타입이 달라 쓰지 못하면 S2Util.cast 로 변환해 다시 시도한다("12" → int 등)
                if (!S2Util.setValue(target, targetFieldName, null, value)
                        && !setConverted(target, targetFieldName, field.getType(), value)) {
                    logSkipped(targetFieldName, null);
                }
            } catch (S2PropertyAccessException | DepthExceededException e) {
                // getter 의 실패·깊이 초과는 빠진 값으로 넘기지 않는다(읽지 못하는 필드는 exclude 로 제외)
                throw e;
            } catch (Exception e) {
                // 오류는 인지만 할 수 있도록 하며, 복사 과정이 중단되지 않게 함
                logSkipped(targetFieldName, e);
                continue;
            }
        }
        return target;
    }

    /**
     * Internal helper method for copying to Map targets.
     * <p>
     * Iterates through source object's fields and copies values to the target Map.
     * If a field mapping exists that has this source field as the value, the mapped key is used.
     * Otherwise, the source field name is used as the Map key.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Map 대상으로 복사하기 위한 내부 헬퍼 메서드입니다.
     * <p>
     * 원본 객체의 필드를 반복하면서 값을 대상 Map에 복사합니다.
     * 이 원본 필드를 값으로 가지는 필드 매핑이 존재하면 매핑된 키를 사용하고,
     * 그렇지 않으면 원본 필드명을 Map 키로 사용합니다.
     * </p>
     *
     * @param targetMap The target Map to populate | 채워넣을 대상 Map
     * @return The target Map with copied values | 복사된 값으로 채워진 Map
     */
    @SuppressWarnings("unchecked")
    private <T> T toMap(Map<String, Object> targetMap) {
        // Create a new visited map for deep copy tracking
        var visited = new IdentityHashMap<Object, Object>();

        // 원본이 Map 이면 항목을, 아니면 원본의 필드(상속 필드 포함)를 기준으로 복사
        List<String> sourceNames = new ArrayList<>();
        Map<String, Class<?>> sourceTypes = new HashMap<>();
        if (source instanceof Map<?, ?> sourceMap) {
            for (var key : sourceMap.keySet()) {
                sourceNames.add(String.valueOf(key));
            }
        } else {
            for (var field : copyFields(S2Cache.getRealClass(source))) {
                sourceNames.add(field.getName());
                sourceTypes.put(field.getName(), field.getType());
            }
        }

        for (var sourceFieldName : sourceNames) {

            // 1. 제외 목록에 포함된 필드인지 확인
            if (excludedFields.contains(sourceFieldName)) {
                continue;
            }

            // 2. 매핑 설정에서 이 source 필드와 매칭되는 target 키 찾기
            // fieldMapping: {targetKey -> sourceFieldName}
            // 역방향으로 찾아서 targetKey를 결정
            String mapKey = sourceFieldName;
            for (var entry : fieldMapping.entrySet()) {
                if (entry.getValue().equals(sourceFieldName)) {
                    mapKey = entry.getKey();
                    break;
                }
            }

            try {
                // 3. Source에서 값을 그대로 가져온다(빈 문자열·빈 컬렉션을 null 로 바꾸지 않음).
                var value = S2Util.getValue(source, sourceFieldName);
                // 4. Null 무시 설정 체크
                if (value == null && ignoreNulls) {
                    continue;
                }

                // 5. Deep copy 필요 여부 확인
                if (value != null && (deepCopyAll || deepCopyFields.contains(sourceFieldName))) {
                    value = deepCopyValue(value, sourceTypes.getOrDefault(sourceFieldName, value.getClass()), visited, 0);
                }

                // 6. Map에 값을 저장한다.
                targetMap.put(mapKey, value);
            } catch (S2PropertyAccessException | DepthExceededException e) {
                // getter 의 실패·깊이 초과는 빠진 값으로 넘기지 않는다(읽지 못하는 필드는 exclude 로 제외)
                throw e;
            } catch (Exception e) {
                // 오류는 인지만 할 수 있도록 하며, 복사 과정이 중단되지 않게 함
                logSkipped(sourceFieldName, e);
                continue;
            }
        }
        return (T) targetMap;
    }

    /**
     * Creates a new instance of the target class and copies data from the source to it.
     * <p>
     * This method is a convenience overload that combines instantiation and copying in a single operation.
     * The target class must have a no-argument constructor (any access level), or be a record, which is created with its
     * canonical constructor. After instantiation, all configured copy rules are applied via the {@link #to(Object)} method.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 클래스의 새 인스턴스를 생성한 후 원본에서 데이터를 복사합니다.
     * <p>
     * 인스턴스 생성과 복사를 한 번의 작업으로 수행하는 편의 메서드입니다.
     * 대상 클래스는 no-argument 생성자(접근 수준 무관)를 가지거나 레코드여야 하며(레코드는 정식 생성자로 생성),
     * 인스턴스 생성 후 설정된 모든 복사 규칙이 {@link #to(Object)} 메서드를 통해 적용됩니다.
     * </p>
     *
     * @param <T>         Target object type | 대상 객체의 타입
     * @param targetClass The target class to instantiate (must have a no-argument constructor) | 인스턴스를 생성할 대상 클래스 (no-argument 생성자 필요)
     * @return A new instance of targetClass populated with copied values | 복사된 값으로 채워진 새 인스턴스
     * @throws RuntimeException if instantiation fails | 인스턴스 생성 실패 시
     * @throws io.github.devers2.s2util.exception.S2PropertyAccessException if a source getter throws (exclude such fields with {@link #exclude(String...)}) | 원본 getter 가 예외를 던질 때 ({@link #exclude(String...)} 로 제외 가능)
     */
    public <T> T to(Class<T> targetClass) {
        if (targetClass == null) {
            throw new IllegalArgumentException("Target class cannot be null.");
        }
        if (targetClass.isRecord()) {
            return toRecord(targetClass);
        }
        T instance;
        try {
            instance = newInstance(targetClass);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to instantiate target class: " + targetClass.getName() + " (한국어: 대상 클래스 인스턴스 생성 실패)",
                    e);
        }
        // 복사 중의 예외(getter 실패 등)는 인스턴스 생성 실패로 감싸지 않고 그대로 알린다
        return to(instance);
    }

    /**
     * Internal helper method for deep copying object values.
     * <p>
     * Recursively copies nested objects, collections, and maps while detecting and preventing
     * circular references using IdentityHashMap. Primitive wrappers are returned as-is.
     * Collections (List, Set) and Maps are recursively copied with deep copies of their elements.
     * Custom objects are copied by recursively copying all their fields.
     * </p>
     *
     * <p>
     * <b>Circular Reference Handling:</b>
     * When a circular reference is detected (an object that has already been visited),
     * the previously copied instance is returned instead of attempting to copy again.
     * This prevents infinite loops and stack overflow. The MAX_DEPTH limit provides
     * a secondary safeguard against unexpectedly deep object graphs.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 객체 값을 깊게 복사하기 위한 내부 헬퍼 메서드입니다.
     * <p>
     * 중첩 객체, 컬렉션, Map을 재귀적으로 복사하면서 IdentityHashMap으로
     * 순환참조를 감지하고 방지합니다. 원시 래퍼는 그대로 반환됩니다.
     * 컬렉션(List, Set)과 Map은 요소에 대한 깊은 복사와 함께 재귀적으로 복사됩니다.
     * 사용자 정의 객체는 모든 필드를 재귀적으로 복사하여 복사됩니다.
     * </p>
     *
     * <p>
     * <b>순환참조 처리:</b>
     * 순환참조가 감지되면(이미 방문한 객체), 다시 복사하는 대신
     * 이전에 복사한 인스턴스를 반환합니다. 이는 무한 루프와 스택 오버플로우를 방지합니다.
     * MAX_DEPTH 제한은 예상치 못하게 깊은 객체 그래프에 대한 2차 보호를 제공합니다.
     * </p>
     *
     * @param source     The object to be deep copied | 깊게 복사할 객체
     * @param targetType The target type for the copied object | 복사된 객체의 대상 타입
     * @param visited    Map tracking already-copied objects (IdentityHashMap) | 이미 복사된 객체 추적 맵
     * @param depth      Current recursion depth for circular reference safeguarding | 순환참조 보호를 위한 현재 재귀 깊이
     * @return The deep copy of the source object | 원본 객체의 깊은 복사본
     * @throws Exception if the maximum recursion depth is exceeded | 최대 재귀 깊이 초과 시
     */
    private Object deepCopyValue(Object source, Class<?> targetType, Map<Object, Object> visited, int depth)
            throws Exception {
        if (source == null) {
            return null;
        }

        // Check depth limit to prevent infinite recursion
        if (depth > MAX_DEPTH) {
            throw new DepthExceededException(
                    "Maximum deep copy depth exceeded (MAX_DEPTH=" + MAX_DEPTH + ")" +
                            " (한국어: 최대 깊은 복사 깊이 초과)");
        }

        // Check if this object has already been copied (circular reference detection)
        if (visited.containsKey(source)) {
            return visited.get(source);
        }

        // Resolve proxies (e.g. lazily-loaded Hibernate associations) to the real entity class;
        // otherwise getDeclaredConstructor() below would try to instantiate the proxy itself.
        // 프록시(예: 지연 로딩된 Hibernate 연관 엔티티)를 실제 엔티티 클래스로 해석함 — 그렇지 않으면
        // 아래 getDeclaredConstructor()가 프록시 자체를 인스턴스화하려고 시도하게 됨.
        var sourceClass = S2Cache.getRealClass(source);

        // Primitive wrappers and immutable objects - return as-is
        if (isPrimitiveWrapper(sourceClass) || sourceClass == String.class) {
            return source;
        }

        // 배열: 새 배열에 요소를 복사(원시 타입 배열은 그대로 복제, 객체 배열은 요소도 깊은 복사)
        if (sourceClass.isArray()) {
            int length = java.lang.reflect.Array.getLength(source);
            var componentType = sourceClass.getComponentType();
            var copiedArray = java.lang.reflect.Array.newInstance(componentType, length);
            visited.put(source, copiedArray);
            if (componentType.isPrimitive()) {
                System.arraycopy(source, 0, copiedArray, 0, length);
            } else {
                for (int i = 0; i < length; i++) {
                    var element = java.lang.reflect.Array.get(source, i);
                    java.lang.reflect.Array.set(copiedArray, i,
                            element != null ? deepCopyValue(element, element.getClass(), visited, depth + 1) : null);
                }
            }
            return copiedArray;
        }

        // java.util.Date 계열(가변): 복제
        if (source instanceof java.util.Date date) {
            var copiedDate = (java.util.Date) date.clone();
            visited.put(source, copiedDate);
            return copiedDate;
        }

        // 컬렉션: 원본과 같은 종류로 복사한다(ConcurrentHashMap·CopyOnWriteArrayList·EnumMap·ArrayDeque 등 —
        // 일반 ArrayList·LinkedHashMap 으로 바꾸면 대상 필드 타입과 맞지 않아 조용히 빠졌다). 요소는 깊은 복사
        if (source instanceof java.util.EnumSet<?> enumSet) {
            // 열거형 요소는 불변이라 복제만 하면 된다
            var copiedSet = enumSet.clone();
            visited.put(source, copiedSet);
            return copiedSet;
        }
        if (source instanceof java.util.Collection<?> sourceCollection) {
            java.util.Collection<Object> copiedCollection = newCollectionLike(sourceCollection);
            visited.put(source, copiedCollection);
            for (var element : sourceCollection) {
                copiedCollection.add(element != null ? deepCopyValue(element, element.getClass(), visited, depth + 1) : null);
            }
            return copiedCollection;
        }
        if (source instanceof Map<?, ?> sourceMap) {
            Map<Object, Object> copiedMap = newMapLike(sourceMap);
            visited.put(source, copiedMap);
            for (var entry : sourceMap.entrySet()) {
                var value = entry.getValue();
                copiedMap.put(entry.getKey(), value != null ? deepCopyValue(value, value.getClass(), visited, depth + 1) : null);
            }
            return copiedMap;
        }

        // JDK 값 타입(LocalDate·BigDecimal·UUID·enum 등): 불변이므로 그대로 공유
        // (내부 필드에 접근할 수 없어 복사를 시도하면 값이 통째로 빠지던 문제 방지)
        if (sourceClass.isEnum() || sourceClass.getClassLoader() == null) {
            return source;
        }

        // Handle custom objects (상속 필드 포함)
        try {
            // Try to create a new instance (protected·private 기본 생성자 포함, 예: JPA 엔티티)
            var copiedObject = newInstance(sourceClass);
            visited.put(source, copiedObject);

            for (var field : copyFields(sourceClass)) {
                var fieldName = field.getName();
                try {
                    var fieldValue = S2Util.getValue(source, fieldName);
                    var copiedFieldValue = fieldValue != null
                            ? deepCopyValue(fieldValue, field.getType(), visited, depth + 1)
                            : null;
                    S2Util.setValue(copiedObject, fieldName, null, copiedFieldValue);
                } catch (S2PropertyAccessException | DepthExceededException e) {
                    throw e;
                } catch (Exception e) {
                    // Skip problematic fields
                    logSkipped(fieldName, e);
                    continue;
                }
            }
            return copiedObject;
        } catch (S2PropertyAccessException | DepthExceededException e) {
            visited.remove(source);
            throw e;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // If instantiation fails, return the original object
            visited.remove(source);
            return source;
        }
    }

    /**
     * Fields to copy for a class: instance fields of the class and its superclasses (subclass first, a
     * subclass field hides a superclass field of the same name), excluding static and synthetic fields.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 복사할 필드 목록입니다. 클래스와 상위 클래스의 인스턴스 필드(하위 클래스 먼저, 같은 이름이면 하위 클래스 필드가
     * 가림)이며 static·합성 필드는 제외합니다.
     */
    private static final ClassValue<Field[]> COPY_FIELDS = new ClassValue<>() {
        @Override
        protected Field[] computeValue(Class<?> type) {
            Map<String, Field> fields = new LinkedHashMap<>();
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) {
                        fields.putIfAbsent(f.getName(), f);
                    }
                }
            }
            return fields.values().toArray(new Field[0]);
        }
    };

    private static Field[] copyFields(Class<?> type) {
        return COPY_FIELDS.get(type);
    }

    /** 기본 생성자로 인스턴스를 만든다. public 이 아닌 생성자(예: JPA 엔티티의 protected 생성자)도 허용한다. */
    /**
     * 원본과 같은 종류의 빈 컬렉션. 정렬 기준(TreeSet·PriorityQueue)은 유지하고, 그 밖은 원본 클래스의 기본 생성자,
     * 만들 수 없으면(List.of 같은 불변 컬렉션 등) List → ArrayList, Set → LinkedHashSet, 그 밖 → LinkedList.
     */
    @SuppressWarnings("unchecked")
    private static java.util.Collection<Object> newCollectionLike(java.util.Collection<?> source) {
        if (source instanceof SortedSet<?> sorted) {
            return new TreeSet<>(castComparator(sorted.comparator()));
        }
        if (source instanceof java.util.PriorityQueue<?> queue) {
            return new java.util.PriorityQueue<>(Math.max(1, queue.size()), castComparator(queue.comparator()));
        }
        if (source instanceof java.util.concurrent.PriorityBlockingQueue<?> queue) {
            return new java.util.concurrent.PriorityBlockingQueue<>(Math.max(1, queue.size()), castComparator(queue.comparator()));
        }
        java.util.Collection<Object> same = tryNewInstance(source.getClass(), java.util.Collection.class);
        if (same != null) {
            return same;
        }
        if (source instanceof List) {
            return new ArrayList<>(source.size());
        }
        if (source instanceof Set) {
            return new LinkedHashSet<>(Math.max(16, (int) (source.size() / 0.75f) + 1));
        }
        return new LinkedList<>();
    }

    /**
     * 원본과 같은 종류의 빈 Map. 정렬 기준(TreeMap)·키 타입(EnumMap)은 유지하고, 그 밖은 원본 클래스의 기본 생성자,
     * 만들 수 없으면 LinkedHashMap(순서 유지).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Map<Object, Object> newMapLike(Map<?, ?> source) {
        if (source instanceof SortedMap<?, ?> sorted) {
            return new TreeMap<>(castComparator(sorted.comparator()));
        }
        if (source instanceof java.util.EnumMap enumMap) {
            Map<Object, Object> copied = new java.util.EnumMap(enumMap);
            copied.clear();
            return copied;
        }
        Map<Object, Object> same = tryNewInstance(source.getClass(), Map.class);
        return same != null ? same : new LinkedHashMap<>(Math.max(16, (int) (source.size() / 0.75f) + 1));
    }

    /** 공개 기본 생성자가 있으면 새 인스턴스, 없거나 실패하면 null (JDK 내부 불변 컬렉션 등) */
    @SuppressWarnings("unchecked")
    private static <T> T tryNewInstance(Class<?> type, Class<?> expected) {
        try {
            Constructor<?> constructor = type.getConstructor();
            if (!Modifier.isPublic(type.getModifiers()) || Modifier.isAbstract(type.getModifiers())) {
                return null;
            }
            Object instance = constructor.newInstance();
            return expected.isInstance(instance) ? (T) instance : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /**
     * 원본이 이 속성을 가졌는지. Map 은 키(경로 "a.b" 는 getValue 에 맡김), 객체는 필드·getter(get/is)·같은 이름의 메서드·레코드 구성요소.
     */
    private boolean sourceHasProperty(String name) {
        if (name.indexOf('.') >= 0 || name.indexOf('[') >= 0) {
            return true; // 경로는 getValue 가 판단
        }
        if (source instanceof Map<?, ?> map) {
            return map.containsKey(name) || S2Util.getValue(map, name) != null;
        }
        return SOURCE_PROPERTIES.get(S2Cache.getRealClass(source)).contains(name);
    }

    /** 클래스가 읽게 해 주는 속성 이름: 인스턴스 필드, 공개 무인자 메서드(getX·isX 는 x 로, 그 밖은 이름 그대로) */
    private static final ClassValue<Set<String>> SOURCE_PROPERTIES = new ClassValue<>() {
        @Override
        protected Set<String> computeValue(Class<?> type) {
            Set<String> names = new HashSet<>();
            for (Field f : copyFields(type)) {
                names.add(f.getName());
            }
            for (java.lang.reflect.Method m : type.getMethods()) {
                if (m.getParameterCount() != 0 || m.getReturnType() == void.class || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                String n = m.getName();
                names.add(n);
                if (n.length() > 3 && n.startsWith("get") && !Character.isLowerCase(n.charAt(3))) {
                    names.add(Character.toLowerCase(n.charAt(3)) + n.substring(4));
                } else if (n.length() > 2 && n.startsWith("is") && !Character.isLowerCase(n.charAt(2))) {
                    names.add(Character.toLowerCase(n.charAt(2)) + n.substring(3));
                }
            }
            return Set.copyOf(names);
        }
    };

    /** 값 타입이 필드와 달라 쓰지 못했을 때 S2Util.cast 로 바꿔 다시 쓴다(변환할 수 없으면 false) */
    private static boolean setConverted(Object target, String fieldName, Class<?> fieldType, Object value) {
        if (value == null || fieldType.isInstance(value)) {
            return false;
        }
        Object converted;
        try {
            converted = S2Util.cast(value, fieldType);
        } catch (ClassCastException e) {
            return false;
        }
        return converted != null && S2Util.setValue(target, fieldName, null, converted);
    }

    /**
     * 레코드는 정식 생성자로 만든다: 구성요소마다 원본 값(매핑·제외·깊은 복사 적용), 없거나 변환할 수 없으면 null(기본형은 0·false).
     */
    @SuppressWarnings("unchecked")
    private <T> T toRecord(Class<T> recordClass) {
        var components = recordClass.getRecordComponents();
        Class<?>[] types = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        var visited = new IdentityHashMap<Object, Object>();
        for (int i = 0; i < components.length; i++) {
            String name = components[i].getName();
            Class<?> type = components[i].getType();
            types[i] = type;
            Object value = null;
            String sourceName = fieldMapping.getOrDefault(name, name);
            if (!excludedFields.contains(name) && sourceHasProperty(sourceName)) {
                value = S2Util.getValue(source, sourceName);
                try {
                    if (value != null && (deepCopyAll || deepCopyFields.contains(sourceName))) {
                        value = deepCopyValue(value, type, visited, 0);
                    }
                } catch (S2PropertyAccessException | DepthExceededException e) {
                    throw e;
                } catch (Exception e) {
                    logSkipped(name, e);
                }
                if (value != null && !wrap(type).isInstance(value)) {
                    try {
                        value = S2Util.cast(value, type);
                    } catch (ClassCastException e) {
                        value = null;
                    }
                }
            }
            args[i] = value != null ? value : defaultValue(type);
        }
        try {
            Constructor<T> constructor = recordClass.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException("Record constructor of " + recordClass.getName() + " threw an exception"
                    + " (한국어: 레코드 생성자가 예외를 던짐)", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate record " + recordClass.getName()
                    + " (한국어: 레코드 인스턴스 생성 실패)", e);
        }
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        return Character.class;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == long.class) return 0L;
        if (type == double.class) return 0d;
        if (type == float.class) return 0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        return 0;
    }

    /** 깊은 복사 깊이 초과 — 필드 단위 오류처럼 건너뛰지 않고 호출자에게 알린다(가이드 3-5) */
    private static final class DepthExceededException extends io.github.devers2.s2util.exception.S2RuntimeException {
        private static final long serialVersionUID = 1L;

        DepthExceededException(String message) {
            super(message);
        }
    }

    private static <T> T newInstance(Class<T> type) throws ReflectiveOperationException {
        Constructor<T> constructor = type.getDeclaredConstructor();
        if (!Modifier.isPublic(constructor.getModifiers()) || !Modifier.isPublic(type.getModifiers())) {
            constructor.setAccessible(true);
        }
        return constructor.newInstance();
    }

    @SuppressWarnings("unchecked")
    private static Comparator<Object> castComparator(Comparator<?> comparator) {
        return (Comparator<Object>) comparator;
    }

    /** 복사하지 못한 필드를 디버그 로그로 남긴다(복사는 계속 진행). */
    private static void logSkipped(String fieldName, Exception e) {
        if (logger.isDebugEnabled()) {
            logger.debug("[S2Copier] Field '{}' was not copied: {}", fieldName, e != null ? e.toString() : "no setter or field to write");
        }
    }

    /**
     * Internal helper method to determine if a class is a primitive wrapper.
     * <p>
     * Primitive wrappers (Integer, Long, Double, etc.) are immutable and don't need
     * to be deep copied; they can be safely returned as-is.
     * </p>
     *
     * @param cls The class to check | 확인할 클래스
     * @return true if the class is a primitive wrapper | 클래스가 원시 래퍼인 경우 true
     */
    private boolean isPrimitiveWrapper(Class<?> cls) {
        return cls == Integer.class ||
                cls == Long.class ||
                cls == Double.class ||
                cls == Float.class ||
                cls == Boolean.class ||
                cls == Byte.class ||
                cls == Short.class ||
                cls == Character.class;
    }
}
