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
package io.github.devers2.s2util.validation.spring;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;

import io.github.devers2.s2util.validation.S2Validator;

/**
 * Spring {@link SmartValidator} adapter for an {@link S2Validator}, so S2 rules run in Spring's standard validation flow
 * ({@code @Valid}/{@code @Validated} with {@code WebDataBinder}) next to Bean Validation, and both write to the same
 * {@code BindingResult}.
 *
 * <pre>{@code
 * private static final S2Validator<JoinForm> JOIN_RULES = S2Validator.<JoinForm>builder()
 *     .field("bizNo", "사업자번호").rule(S2RuleType.BIZRNO, true)
 *     .build();
 *
 * @InitBinder("joinForm")
 * void initBinder(WebDataBinder binder) {
 *     binder.addValidators(S2SpringValidator.of(JoinForm.class, JOIN_RULES)); // Bean Validation stays as is
 * }
 *
 * @PostMapping("/join")
 * String join(@Valid @ModelAttribute JoinForm joinForm, BindingResult result) { ... } // BV and S2 errors together
 * }</pre>
 * <p>
 * Messages use the request locale ({@link LocaleContextHolder}). Validation hints (groups) are ignored; use
 * {@code when(...)} conditions for role-specific rules. Custom lambda rules run here as well.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link S2Validator}를 Spring {@link SmartValidator}로 감싼 어댑터입니다. S2 규칙이 Spring 표준 검증 흐름({@code WebDataBinder}와
 * {@code @Valid}/{@code @Validated})에서 Bean Validation 과 함께 실행되고, 두 결과가 같은 {@code BindingResult}에 기록됩니다.
 * <p>
 * 메시지는 요청 로케일({@link LocaleContextHolder})을 씁니다. 검증 힌트(그룹)는 무시하며, 역할별 규칙은 {@code when(...)} 조건으로
 * 표현합니다. 커스텀 람다 규칙도 함께 실행됩니다.
 * </p>
 *
 * @param <T> The target type | 대상 타입
 * @since 2.2.0
 */
public final class S2SpringValidator<T> implements SmartValidator {

    private final Class<T> type;
    private final S2Validator<T> validator;

    private S2SpringValidator(Class<T> type, S2Validator<T> validator) {
        if (type == null || validator == null) {
            throw new IllegalArgumentException("type and validator cannot be null");
        }
        this.type = type;
        this.validator = validator;
    }

    /**
     * Creates an adapter that validates objects of {@code type} (and its subtypes) with {@code validator}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code type}(과 하위 타입) 객체를 {@code validator}로 검증하는 어댑터를 만듭니다.
     *
     * @param <T>       The target type | 대상 타입
     * @param type      The supported target class | 지원하는 대상 클래스
     * @param validator The S2 validator | S2 검증기
     * @return A new adapter | 새 어댑터
     */
    public static <T> S2SpringValidator<T> of(Class<T> type, S2Validator<T> validator) {
        return new S2SpringValidator<>(type, validator);
    }

    /**
     * Returns whether {@code clazz} is the target type or a subtype.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code clazz}가 대상 타입이거나 그 하위 타입인지 반환합니다.
     *
     * @param clazz The class to check | 확인할 클래스
     * @return {@code true} if supported | 지원하면 true
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return type.isAssignableFrom(clazz);
    }

    /**
     * Validates {@code target} and records failures in {@code errors} (unreadable paths become a {@code FieldError}).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@code target}을 검증하고 실패를 {@code errors}에 기록합니다(읽을 수 없는 경로는 {@code FieldError}로 기록).
     *
     * @param target The object to validate | 검증 대상
     * @param errors The Spring errors | Spring 오류 객체
     */
    @Override
    public void validate(Object target, Errors errors) {
        validator.validate(type.cast(target), error -> S2BindValidator.reject(errors, error),
                LocaleContextHolder.getLocale());
    }

    /**
     * Same as {@link #validate(Object, Errors)}; validation hints (groups) are ignored.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@link #validate(Object, Errors)}와 같습니다. 검증 힌트(그룹)는 무시합니다.
     *
     * @param target          The object to validate | 검증 대상
     * @param errors          The Spring errors | Spring 오류 객체
     * @param validationHints Ignored | 무시됨
     */
    @Override
    public void validate(Object target, Errors errors, Object... validationHints) {
        validate(target, errors);
    }

    /**
     * Returns the wrapped validator, e.g. to export rules JSON with {@code getRulesJson(locale)}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 감싼 검증기를 반환합니다. 예: {@code getRulesJson(locale)}로 규칙 JSON 내보내기.
     *
     * @return The S2 validator | S2 검증기
     */
    public S2Validator<T> getValidator() {
        return validator;
    }
}
