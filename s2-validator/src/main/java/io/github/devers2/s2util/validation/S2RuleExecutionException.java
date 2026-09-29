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

import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.exception.S2RuntimeException;

/**
 * Thrown when a custom rule (lambda) itself throws while being evaluated.
 * <p>
 * This indicates a defect in the rule, not invalid user input, so it is kept distinct from
 * {@link S2ValidationException}. The message names only the field; the original exception is
 * available through {@link #getCause()} so that internal details are not exposed to end users.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 커스텀 규칙(람다) 평가 중 규칙 자체에서 예외가 발생했을 때 던져집니다.
 * <p>
 * 사용자 입력 오류가 아니라 규칙의 결함을 뜻하므로 {@link S2ValidationException}과 구분됩니다.
 * 메시지에는 필드 이름만 담고, 내부 정보가 최종 사용자에게 노출되지 않도록 원인 예외는 {@link #getCause()}로만 제공합니다.
 * </p>
 */
public class S2RuleExecutionException extends S2RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;

    /**
     * Creates an exception for a custom rule failure on the given field.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정한 필드의 커스텀 규칙 실행 오류 예외를 생성합니다.
     *
     * @param fieldName The field path (may be null) | 필드 경로 (null 가능)
     * @param cause     The exception thrown by the rule | 규칙이 던진 원인 예외
     */
    public S2RuleExecutionException(String fieldName, Throwable cause) {
        super(buildMessage(fieldName), cause);
        this.fieldName = fieldName;
    }

    /**
     * Returns the path of the field whose custom rule failed.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 커스텀 규칙 실행에 실패한 필드 경로를 반환합니다.
     *
     * @return The field path, or null if unknown | 필드 경로 (알 수 없으면 null)
     */
    public String getFieldName() {
        return fieldName;
    }

    private static String buildMessage(String fieldName) {
        boolean hasField = fieldName != null && !fieldName.isBlank();
        if (S2Util.isKorean()) {
            return "커스텀 람다 검증 실행 중 예외가 발생했습니다" + (hasField ? " (필드: '" + fieldName + "')" : "");
        }
        return "Custom validation logic threw an exception" + (hasField ? " on field '" + fieldName + "'" : "");
    }
}
