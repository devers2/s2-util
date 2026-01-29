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
import java.util.Collection;

import io.github.devers2.s2util.core.S2Util;

/**
 * Metadata representing a single conditional requirement for validation.
 * <p>
 * This record stores a field name (or Map key) and its expected value.
 * It is used by {@link S2Field} to determine if its rules should be executed
 * based on the current state of the target object.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 특정 필드 값에 따라 검증 수행 여부를 결정하는 조건 메타데이터입니다.
 * <p>
 * {@link S2Field}에서 사용되며, 대상 객체의 특정 필드 상태가 기대값과 일치하는지 확인하여
 * 해당 필드의 유효성 검사 규칙을 실행할지 여부를 결정합니다.
 * </p>
 *
 * @param fieldName The name of the field or Map key to inspect | 검사 대상 필드 이름 또는 Map 키
 * @param value     The expected value to satisfy the condition | 조건을 만족하기 위한 기대값
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public record S2Condition(Object fieldName, Object value) implements Serializable {

    /**
     * Evaluates whether the condition is met by the given target object.
     * <p>
     * <b>Comparison Logic:</b>
     * <ul>
     * <li>If the actual value is a {@link Collection}, it checks if the
     * expected value exists within the collection (useful for checkboxes).</li>
     * <li>Otherwise, it performs a string-based equality check.</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 대상 객체의 현재 상태가 이 조건을 만족하는지 평가합니다.
     * <p>
     * <b>상세 비교 로직:</b>
     * <ul>
     * <li>실제 값이 {@link Collection}인 경우, 기대값이 컬렉션 내에 포함되어 있는지 확인합니다 (체크박스 그룹 등).</li>
     * <li>그 외의 경우에는 문자열로 변환하여 동등 여부를 비교합니다.</li>
     * </ul>
     * </p>
     *
     * @param target The object to inspect | 검사 대상 객체 인스턴스
     * @return {@code true} if satisfied | 조건이 만족된 경우 true
     */
    public boolean isSatisfied(Object target) {
        Object actualValue = S2Util.getValue(target, fieldName);
        if (actualValue == null)
            return value == null;
        if (value == null)
            return false;

        // 기대값을 미리 정규화 (한 번만 수행)
        String normalizedValue = normalizeValue(value);

        // 실제 값이 컬렉션(체크박스 등)인 경우 포함 여부 확인
        if (actualValue instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::normalizeValue)
                    .anyMatch(v -> v.equals(normalizedValue));
        }

        // 단일 값인 경우 정규화된 값 비교
        return normalizeValue(actualValue).equals(normalizedValue);
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

}
