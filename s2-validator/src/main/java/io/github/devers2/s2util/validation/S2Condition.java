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
package io.github.devers2.s2util.validation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.github.devers2.s2util.core.S2Util;

/**
 * Metadata representing a single conditional requirement for validation.
 * <p>
 * This record stores a field name (or Map key), a comparison {@link S2Operator} and the value to compare with. It is
 * used by {@link S2Field} to determine if its rules should be executed based on the current state of the target
 * object. {@code s2.validator.js} judges conditions the same way.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 특정 필드 값에 따라 검증 수행 여부를 결정하는 조건 메타데이터입니다.
 * <p>
 * 필드 이름(또는 Map 키), 비교 연산자({@link S2Operator}), 비교 값을 담으며, {@link S2Field}가 대상 객체의 상태로 이 필드의 규칙을
 * 실행할지 결정할 때 사용합니다. {@code s2.validator.js}도 같은 방식으로 판정합니다.
 * </p>
 *
 * @param fieldName The name of the field or Map key to inspect | 검사 대상 필드 이름 또는 Map 키
 * @param operator  The comparison operator | 비교 연산자
 * @param value     The value to compare with (a list for IN/NOT_IN, null for EMPTY/NOT_EMPTY) | 비교 값
 *                  (IN/NOT_IN 은 목록, EMPTY/NOT_EMPTY 는 null)
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public record S2Condition(Object fieldName, S2Operator operator, Object value) implements Serializable {

    /**
     * Validates the operator and value at creation, so a condition that can never be judged consistently fails fast.
     * Arrays for IN/NOT_IN are stored as an unmodifiable list.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 일관되게 판정할 수 없는 조건이 바로 실패하도록 생성 시점에 연산자와 비교 값을 검사합니다. IN/NOT_IN 의 배열은 수정할 수 없는
     * 목록으로 저장합니다.
     *
     * @throws IllegalArgumentException If the value does not fit the operator | 비교 값이 연산자에 맞지 않는 경우
     */
    public S2Condition {
        Objects.requireNonNull(operator, "operator");
        if (operator.isUnary() && value != null) {
            throw new IllegalArgumentException("[S2Condition] " + operator + " takes no value, but was: " + value);
        }
        if (operator.isNumeric()) {
            Double number = S2Rule.toDouble(value);
            if (number == null || number.isNaN() || number.isInfinite()) {
                throw new IllegalArgumentException(
                        "[S2Condition] " + operator + " requires a finite number, but was: " + value);
            }
        }
        if (operator.isList()) {
            if (value instanceof Object[] array) {
                value = List.of(array);
            } else if (value instanceof Collection<?> collection) {
                value = List.copyOf(collection);
            } else {
                throw new IllegalArgumentException(
                        "[S2Condition] " + operator + " requires a collection or array, but was: " + value);
            }
        }
    }

    /**
     * Creates an equality ({@link S2Operator#EQ}) condition; {@code null} means the field must be empty.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 같음({@link S2Operator#EQ}) 조건을 만듭니다. {@code null}은 필드가 비어 있어야 함을 뜻합니다.
     *
     * @param fieldName The field to inspect | 검사 대상 필드
     * @param value     The expected value | 기대값
     */
    public S2Condition(Object fieldName, Object value) {
        this(fieldName, S2Operator.EQ, value);
    }

    /**
     * Evaluates whether the condition is met by the given target object.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체의 현재 상태가 이 조건을 만족하는지 평가합니다.
     *
     * @param target The object to inspect | 검사 대상 객체 인스턴스
     * @return {@code true} if satisfied | 조건이 만족된 경우 true
     * @see S2Operator
     */
    public boolean isSatisfied(Object target) {
        return isSatisfied(target, List.of());
    }

    /**
     * Evaluates the condition, looking the field up in {@code target} first and then in each outer object (nearest
     * first, root last) while the value is empty. This lets a condition inside a NESTED/EACH sub-validator refer to a
     * field of an outer object, in the same order the client uses. A blank string or an empty collection counts as
     * empty, like an empty form field on the client.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 값이 비어 있으면 {@code target}에서 시작해 바깥 객체들(가까운 순서, 루트 마지막)에서 조건 필드를 찾아 평가합니다. NESTED/EACH
     * 하위 검증기의 조건이 바깥 객체의 필드를 가리킬 수 있으며, 클라이언트와 같은 순서입니다. 공백 문자열과 빈 컬렉션은 클라이언트의 빈 폼
     * 필드처럼 빈 값으로 봅니다.
     *
     * @param target       The object to inspect | 검사 대상 객체
     * @param outerTargets Outer objects, nearest first | 가까운 순서의 바깥 객체들
     * @return {@code true} if satisfied | 조건이 만족된 경우 true
     */
    boolean isSatisfied(Object target, List<Object> outerTargets) {
        Object actualValue = presentValue(target);
        for (int i = 0; actualValue == null && i < outerTargets.size(); i++) {
            actualValue = presentValue(outerTargets.get(i));
        }
        return switch (operator) {
            case EQ -> isEqual(actualValue);
            case NE -> !isEqual(actualValue);
            case IN -> isIn(actualValue);
            case NOT_IN -> !isIn(actualValue);
            case EMPTY -> actualValue == null;
            case NOT_EMPTY -> actualValue != null;
            case GT, GTE, LT, LTE -> compareNumber(actualValue);
        };
    }

    /** EQ: empty matches only {@code null}; a collection matches when it contains the value. | EQ: 빈 값은 null 과만 일치, 컬렉션은 값을 포함하면 일치 */
    private boolean isEqual(Object actualValue) {
        if (actualValue == null)
            return value == null;
        if (value == null)
            return false;
        String expected = normalizeValue(value);
        return valuesOf(actualValue).stream().map(this::normalizeValue).anyMatch(expected::equals);
    }

    /** IN: any of the actual values is one of the listed values; empty never matches. | IN: 실제 값 중 하나가 목록에 있으면 일치, 빈 값은 불일치 */
    private boolean isIn(Object actualValue) {
        if (actualValue == null)
            return false;
        List<String> expected = ((List<?>) value).stream().map(this::normalizeValue).toList();
        return valuesOf(actualValue).stream().map(this::normalizeValue).anyMatch(expected::contains);
    }

    /** GT/GTE/LT/LTE on plain numbers; empty, non-numeric and collection values do not match. | 일반 숫자의 크기 비교. 빈 값·숫자 아님·컬렉션은 불일치 */
    private boolean compareNumber(Object actualValue) {
        if (actualValue == null || actualValue instanceof Collection<?> || actualValue instanceof Object[])
            return false;
        Double actual = S2Rule.toDouble(actualValue);
        if (actual == null || actual.isNaN())
            return false;
        int cmp = Double.compare(actual, S2Rule.toDouble(value));
        return switch (operator) {
            case GT -> cmp > 0;
            case GTE -> cmp >= 0;
            case LT -> cmp < 0;
            default -> cmp <= 0;
        };
    }

    /** The actual value as a list: collection/array elements, or the single value. | 실제 값을 목록으로: 컬렉션/배열 요소 또는 단일 값 */
    private static List<?> valuesOf(Object actualValue) {
        if (actualValue instanceof Collection<?> collection)
            return List.copyOf(collection);
        if (actualValue instanceof Object[] array)
            return Arrays.asList(array);
        return List.of(actualValue);
    }

    /**
     * Returns the condition field's value in {@code source}, treating {@code null} sources, blank strings and empty
     * collections as absent.
     *
     * @param source The object to read from | 값을 읽을 객체
     * @return The value, or {@code null} if absent | 값 (없으면 null)
     */
    private Object presentValue(Object source) {
        if (source == null) {
            return null;
        }
        Object v = S2Util.getValue(source, fieldName);
        if (v instanceof String str && str.isBlank())
            return null;
        if (v instanceof Collection<?> collection && collection.isEmpty())
            return null;
        return v instanceof Object[] array && array.length == 0 ? null : v;
    }

    /**
     * 모든 값을 정규화하여 일관된 형태의 문자열로 변환합니다.
     *
     * <ul>
     * <li>Boolean: "true"/"false"로 정규화</li>
     * <li>Number: toString() 후 정규화 (1과 1.0은 구분)</li>
     * <li>Enum: name()으로 정규화</li>
     * <li>String: 양쪽 공백 제거 (필요시 Boolean 문자열 소문자 정규화)</li>
     * </ul>
     *
     * @param val 정규화할 값
     * @return 정규화된 문자열 값
     */
    private String normalizeValue(Object val) {
        if (val == null)
            return null;

        // instanceof 순서 최적화: 자주 나타나는 타입부터 체크
        if (val instanceof String str) {
            // String: 양쪽 공백 제거, Boolean 문자열 정규화
            String trimmed = str.trim();
            if (trimmed.equalsIgnoreCase("true")) {
                return "true";
            }
            if (trimmed.equalsIgnoreCase("false")) {
                return "false";
            }
            return trimmed;
        }

        if (val instanceof Boolean bool) {
            // Boolean: 직접 비교로 toString() 호출 최소화
            return bool ? "true" : "false";
        }

        if (val instanceof Enum<?> enumVal) {
            // Enum: name() 사용
            return enumVal.name();
        }

        // Number: toString() 사용 (Integer, Long, Double 등)
        if (val instanceof Number) {
            return val.toString();
        }

        // 기타 타입: toString()
        return val.toString();
    }

    /**
     * Resolves this condition against a concrete wildcard item index.
     * <p>
     * If {@link #fieldName} contains the {@code "[]"} wildcard marker (e.g. {@code "items[].type"}),
     * it is substituted with the concrete index (e.g. {@code "items[3].type"}) so the condition can be
     * evaluated against a specific array element while validating a {@code field("collection[].x")}
     * group. Non-wildcard field names (referring to an outer/root-level field) are returned unchanged.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 와일드카드 그룹 검증 중 특정 아이템 인덱스를 기준으로 조건을 재해석합니다.
     * <p>
     * {@link #fieldName}에 {@code "[]"} 와일드카드 표기가 포함된 경우(예: {@code "items[].type"}),
     * 이를 실제 인덱스로 치환하여(예: {@code "items[3].type"}) 배열의 특정 요소를 기준으로 조건을 평가할 수
     * 있게 합니다. 와일드카드 표기가 없는 필드명(상위/루트 레벨 필드를 가리킴)은 그대로 반환됩니다.
     * </p>
     *
     * @param index The concrete array index of the current wildcard item | 현재 와일드카드 아이템의 실제 인덱스
     * @return A resolved {@link S2Condition} usable against the root target | 루트 대상 객체에 사용 가능한 재해석된 조건
     */
    S2Condition resolveForWildcardIndex(int index) {
        if (fieldName instanceof String path && path.contains("[]")) {
            int bracketIndex = path.indexOf("[]");
            String resolved = path.substring(0, bracketIndex) + "[" + index + "]" + path.substring(bracketIndex + 2);
            return new S2Condition(resolved, operator, value);
        }
        return this;
    }

    /**
     * Static factory method for creating a new S2Condition instance.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 새로운 S2Condition 인스턴스 생성을 위한 정적 팩토리 메서드입니다.
     *
     * @param fieldName Name of the criteria field | 기준 필드 이름
     * @param value     Expected value | 기대값
     * @return A new S2Condition instance | 새로운 S2Condition 인스턴스
     */
    public static S2Condition of(Object fieldName, Object value) {
        return new S2Condition(fieldName, value);
    }

    /**
     * Static factory method for a condition with an operator.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 연산자가 있는 조건을 만드는 정적 팩토리 메서드입니다.
     *
     * @param fieldName Name of the criteria field | 기준 필드 이름
     * @param operator  Comparison operator | 비교 연산자
     * @param value     Value to compare with | 비교 값
     * @return A new S2Condition instance | 새로운 S2Condition 인스턴스
     */
    public static S2Condition of(Object fieldName, S2Operator operator, Object value) {
        return new S2Condition(fieldName, operator, value);
    }

}
