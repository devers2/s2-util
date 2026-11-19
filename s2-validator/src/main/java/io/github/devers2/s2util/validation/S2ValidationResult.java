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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.devers2.s2util.validation.S2Validator.S2ValidationError;

/**
 * All errors of one validation run, returned by {@link S2Validator#validateAll(Object)}. Unlike {@code validate(target)},
 * it never throws for invalid values, so it fits REST/JSON responses.
 *
 * <pre>{@code
 * S2ValidationResult result = USER_RULES.validateAll(user, locale);
 * if (!result.isValid()) {
 *     return ResponseEntity.badRequest().body(result.toMap()); // {"email": ["..."], ...} — same shape as s2.validator.js
 * }
 * }</pre>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 검증 한 번의 모든 오류이며 {@link S2Validator#validateAll(Object)}가 반환합니다. {@code validate(target)}와 달리 값이 잘못돼도 예외를
 * 던지지 않아 REST/JSON 응답에 알맞습니다. {@link #toMap()}은 {@code s2.validator.js}의 {@code validate()}·{@code check()}와 같은
 * {@code {필드: [메시지]}} 형식입니다.
 *
 * @since 2.2.0
 */
public final class S2ValidationResult {

    private final List<S2ValidationError> errors;

    S2ValidationResult(List<S2ValidationError> errors) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Returns whether there are no errors.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 오류가 없는지 반환합니다.
     *
     * @return {@code true} if valid | 유효하면 true
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Returns all errors in validation order.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 모든 오류를 검증 순서대로 반환합니다(수정 불가 목록).
     *
     * @return The errors | 오류 목록
     */
    public List<S2ValidationError> getErrors() {
        return errors;
    }

    /**
     * Returns the errors of one field path (e.g. {@code "items[0].qty"}).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 한 필드 경로(예: {@code "items[0].qty"})의 오류를 반환합니다.
     *
     * @param fieldName The field path | 필드 경로
     * @return The errors of the field | 그 필드의 오류
     */
    public List<S2ValidationError> getErrors(String fieldName) {
        return errors.stream().filter(e -> e.fieldName() != null && e.fieldName().equals(fieldName)).toList();
    }

    /**
     * Returns the messages by field path in validation order, the same shape as {@code s2.validator.js} returns.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 필드 경로별 메시지를 검증 순서대로 반환합니다. {@code s2.validator.js}가 반환하는 것과 같은 형식입니다.
     *
     * @return Field path → messages | 필드 경로 → 메시지 목록
     */
    public Map<String, List<String>> toMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (S2ValidationError error : errors) {
            map.computeIfAbsent(error.fieldName(), k -> new ArrayList<>()).add(error.defaultMessage());
        }
        return map;
    }

    /**
     * Throws {@link S2ValidationException} for the first error if invalid.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 오류가 있으면 첫 오류로 {@link S2ValidationException}을 던집니다.
     *
     * @throws S2ValidationException If invalid | 유효하지 않은 경우
     */
    public void throwIfInvalid() {
        if (!errors.isEmpty()) {
            throw new S2ValidationException(errors.get(0));
        }
    }

    @Override
    public String toString() {
        return "S2ValidationResult" + toMap();
    }
}
