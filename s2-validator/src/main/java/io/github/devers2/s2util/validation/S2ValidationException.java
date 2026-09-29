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

import io.github.devers2.s2util.exception.S2RuntimeException;

/**
 * Thrown in exception mode when an input value fails a validation rule.
 * <p>
 * Carries the field path and error code of the failure so that callers can map it to a
 * field-level response (e.g. HTTP 400) without parsing the message. Distinct from
 * {@link S2RuleExecutionException}, which signals a bug inside a custom rule.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 예외 모드에서 입력값이 검증 규칙을 통과하지 못했을 때 던져집니다.
 * <p>
 * 메시지를 해석하지 않고도 필드 단위 응답(예: HTTP 400)으로 변환할 수 있도록 실패한 필드 경로와 오류 코드를 함께 담습니다.
 * 커스텀 규칙 내부 버그를 뜻하는 {@link S2RuleExecutionException}과 구분됩니다.
 * </p>
 */
public class S2ValidationException extends S2RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String fieldName;
    private final String errorCode;

    /**
     * Creates an exception from a validation error.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증 오류 정보로 예외를 생성합니다.
     *
     * @param error The validation error | 검증 오류 정보
     */
    public S2ValidationException(S2Validator.S2ValidationError error) {
        super(error.defaultMessage());
        this.fieldName = error.fieldName();
        this.errorCode = error.errorCode();
    }

    /**
     * Creates an exception with an explicit field name, error code, and message.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 필드 이름, 오류 코드, 메시지를 직접 지정해 예외를 생성합니다.
     *
     * @param fieldName The field path or label | 필드 경로 또는 라벨
     * @param errorCode The error code | 오류 코드
     * @param message   The error message | 오류 메시지
     */
    public S2ValidationException(String fieldName, String errorCode, String message) {
        super(message);
        this.fieldName = fieldName;
        this.errorCode = errorCode;
    }

    /**
     * Returns the logical path of the failed field (e.g. {@code items[0].name}).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 실패한 필드의 논리 경로(예: {@code items[0].name})를 반환합니다.
     *
     * @return The field path | 필드 경로
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the error code (message key) of the failed rule.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 실패한 규칙의 오류 코드(메시지 키)를 반환합니다.
     *
     * @return The error code | 오류 코드
     */
    public String getErrorCode() {
        return errorCode;
    }
}
