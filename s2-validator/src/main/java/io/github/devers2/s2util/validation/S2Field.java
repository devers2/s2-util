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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import io.github.devers2.s2util.core.S2StringUtil;
import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;
import io.github.devers2.s2util.message.S2ResourceBundle;

/**
 * Metadata and rule container for an individual field.
 * <p>
 * This class stores the identity (name) and descriptive label of a field, along with
 * a collection of validation rules ({@link S2Rule}) and conditional logic
 * ({@link S2Condition}) that determines when the validation should be executed.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 개별 필드에 대한 검증 메타데이터 및 규칙 컨테이너입니다.
 * <p>
 * 필드의 식별자(이름)와 사용자에게 보여줄 논리적 명칭(라벨)을 관리하며, 해당 필드에 적용될
 * 여러 검증 규칙({@link S2Rule}) 및 특정 상황에서만 검증을 수행하도록 제어하는 조건부 로직
 * ({@link S2Condition})을 포함합니다.
 * </p>
 *
 * <h3>Conditional Validation Logic (조건부 검증 엔진)</h3>
 * Rules are evaluated only if {@link #shouldValidate(Object)} returns {@code true}.
 * The conditions are organized into groups:
 * <ul>
 * <li><b>Internal Groups (AND):</b> All conditions within a group must be satisfied.</li>
 * <li><b>External Groups (OR):</b> Validation proceeds if at least one group is satisfied.</li>
 * </ul>
 *
 * @param <T> The type of the root target object
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class S2Field<T> implements Serializable {

    private static final long serialVersionUID = -7438229703800777199L;

    private static final S2Logger logger = S2LogManager.getLogger(S2Field.class);

    /** The parent validator instance this field belongs to */
    private final S2Validator<T> validator;
    /** Unique identifier for data extraction (field name, map key, etc.) */
    private Object name;
    /** Descriptive label shown in error messages (defaults to name) */
    private String label;
    /** List of standard validation rules assigned to this field */
    private List<S2Rule> rules = new ArrayList<>();
    /** List of custom lambda-based validation rules */
    private final List<S2CustomRule<?, T>> customRules = new ArrayList<>();
    /**
     * Grouped conditional statements.
     * <p>
     * - Inner list (List&lt;S2Condition&gt;) represents <b>AND</b> relationships.
     * - Outer list represents <b>OR</b> relationships between these groups.
     * </p>
     */
    /** List of grouped conditions that determine whether this field should be validated */
    private final List<List<S2Condition>> conditionGroups = new ArrayList<>();
    /** Pointer to the most recently added rule for subsequent message configuration */
    private S2RuleMessageStep currentRule;

    /**
     * Internal constructor for field initialization.
     * <p>
     * Defaults to {@link S2RuleType#REQUIRED} if no other rules are added (not explicitly
     * coded here, but managed via builders).
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 필드 초기화를 위한 내부 생성자입니다.
     *
     * @param validator Parent validator instance | 부모 검증기 인스턴스
     * @param name      Data identifier (field name, Map key, etc.) | 데이터 식별자 (필드명, 맵 키 등)
     * @param label     Logical display name (label) | 논리적 명칭 (라벨)
     */
    protected S2Field(S2Validator<T> validator, Object name, String label) {
        if (S2Util.isEmpty(name)) {
            throw new IllegalArgumentException("[S2Field] name cannot be null or empty.");
        }
        this.validator = validator;
        this.name = name;
        this.label = label != null && !label.isBlank() ? label : (String) name;
    }

    /**
     * Adds a standard rule with optional criterion and custom message key.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 표준 검증 규칙(S2RuleType)을 추가합니다. 기준값과 메시지 키를 선택적으로 지정할 수 있습니다.
     *
     * @param type            The rule type | 검증 규칙 타입
     * @param value           Criterion value (optional) | 검증 기준값 (선택 사항)
     * @param errorMessageKey Custom property key (optional) | 커스텀 메시지 키 (선택 사항)
     * @return Current field instance | 현재 필드 인스턴스
     */
    public S2Field<T> rule(S2RuleType type, Object value, String errorMessageKey) {
        S2Rule rule = new S2Rule(type, value, errorMessageKey);
        this.rules.add(rule);
        this.currentRule = rule; // Update tracking pointer
        return this;
    }

    public S2Field<T> rule(S2RuleType type) {
        return rule(type, null, null);
    }

    public S2Field<T> rule(S2RuleType type, Object value) {
        return rule(type, value, null);
    }

    /**
     * Adds a custom Predicate-based validation rule.
     *
     * @param <V>   The field value type | 필드 값의 타입
     * @param logic Validation logic | 검증 로직 (Predicate)
     * @return Current field instance | 현재 필드 인스턴스
     */
    public <V> S2Field<T> rule(Predicate<V> logic) {
        return rule(logic, null);
    }

    public <V> S2Field<T> rule(Predicate<V> logic, String errorMessageKey) {
        return rule((v, t) -> {
            @SuppressWarnings("unchecked")
            V castValue = (V) v;
            return logic.test(castValue);
        }, errorMessageKey);
    }

    public <V> S2Field<T> rule(BiPredicate<V, T> logic) {
        return rule(logic, null);
    }

    /**
     * Adds a custom lambda-based validation rule.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 람다식을 이용한 커스텀 검증 규칙을 추가합니다.
     *
     * @param <V>             The type of the field value | 필드 값의 타입
     * @param logic           The validation logic (accepts value and root object) | 검증 로직 (필드 값과 루트 객체를 인자로 받음)
     * @param errorMessageKey Custom property key (optional) | 커스텀 메시지 키 (선택 사항)
     * @return Current field instance | 현재 필드 인스턴스
     */
    public <V> S2Field<T> rule(BiPredicate<V, T> logic, String errorMessageKey) {
        S2CustomRule<V, T> custom = new S2CustomRule<>(logic, errorMessageKey);
        this.customRules.add(custom);
        this.currentRule = custom; // Update tracking pointer
        return this;
    }

    /**
     * Sets a message template for the specific locale.
     *
     * @param locale  Target locale | 대상 로케일
     * @param message Message template | 에러 메시지 템플릿
     * @return Current field instance | 현재 필드 인스턴스
     */
    public S2Field<T> message(Locale locale, String message) {
        return storeMessage(locale.getLanguage(), message);
    }

    /**
     * Manually stores an error message template for a specific language.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 특정 언어에 대한 에러 메시지 템플릿을 수동으로 저장합니다.
     *
     * @param lang     Language code ("ko", "en") | 언어 코드 ("ko", "en")
     * @param template Message template | 에러 메시지 템플릿
     * @return Current field instance | 현재 필드 인스턴스
     */
    public S2Field<T> storeMessage(String lang, String template) {
        if (this.currentRule == null) {
            throw new IllegalStateException("Missing rule definition. Call rule() before setting a message.");
        }
        this.currentRule.storeMessage(lang, template);
        return this;
    }

    /**
     * Starts a new conditional group (OR branch).
     * <p>
     * Validation for this field will only occur if the specified field in the target
     * matches the given value. Calling this method creates a new group.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 새로운 검증 조건 그룹(OR 분기)을 시작합니다.
     * <p>
     * 대상 객체의 특정 필드 값이 지정된 값과 일치할 때만 이 필드의 검증을 수행합니다.
     * 이 메서드를 호출할 때마다 새로운 독립적인 조건 그룹이 생성됩니다.
     * </p>
     *
     * @param fieldName The name of the field to check in the target object | 대상 객체 내의 확인 대상 필드 이름
     * @param value     The value required to trigger validation | 검증을 실행하기 위한 기준 값
     * @return Current S2Field instance for chaining | 체이닝을 위한 현재 S2Field 인스턴스
     */
    public S2Field<T> when(Object fieldName, Object value) {
        List<S2Condition> group = new ArrayList<>();
        group.add(new S2Condition(fieldName, value));
        this.conditionGroups.add(group);
        return this;
    }

    /**
     * Appends an additional condition to the current group (AND relationship).
     *
     * @param fieldName The name of the field to check | 확인 대상 필드 이름
     * @param value     The expected value | 기대되는 기준 값
     * @return Current field instance | 현재 필드 인스턴스
     */
    public S2Field<T> and(Object fieldName, Object value) {
        if (this.conditionGroups.isEmpty())
            throw new IllegalStateException("and() must be called after when().");
        this.conditionGroups.get(this.conditionGroups.size() - 1).add(new S2Condition(fieldName, value));
        return this;
    }

    /**
     * Returns the data identifier (field name, map key, etc.).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 데이터의 식별자(필드명, Map의 Key 등)를 반환합니다.
     *
     * @return The data identifier | 데이터의 식별자
     */
    public Object getName() {
        return name;
    }

    /**
     * Returns the logical display name (label).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 필드의 논리적 명칭(라벨)을 반환합니다.
     *
     * @return The logical label | 필드의 논리적 명칭
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the list of standard validation rules.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 설정된 표준 검증 규칙 목록을 반환합니다.
     *
     * @return List of validation rules | 검증 규칙 리스트
     */
    public List<S2Rule> getRules() {
        return rules;
    }

    /**
     * Returns the list of custom validation logic.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 설정된 커스텀 검증 로직 목록을 반환합니다.
     *
     * @return List of custom validation rules | 커스텀 검증 로직 리스트
     */
    public List<S2CustomRule<?, T>> getCustomRules() {
        return customRules;
    }

    /**
     * Returns the list of condition groups.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 설정된 조건 그룹 목록을 반환합니다.
     *
     * @return List of condition groups | 조건 그룹 리스트
     */
    public List<List<S2Condition>> getConditionGroups() {
        return conditionGroups;
    }

    /**
     * Determines whether validation should be performed for the given target.
     * <p>
     * Evaluation Logic: If any condition group (OR) has all its internal
     * conditions (AND) satisfied, returns {@code true}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 주어진 객체 상태가 설정된 조건을 충족하여 검증을 수행해야 하는지 여부를 판단합니다.
     * <p>
     * 평가 로직: 등록된 조건 그룹(OR) 중 하나라도 내부 조건(AND)을 모두 만족하면 {@code true}를 반환합니다.
     * </p>
     *
     * @param target The target object to evaluate against | 평가 대상이 되는 객체 인스턴스
     * @return {@code true} if validation should proceed | 검증을 수행해야 하는 경우 true
     */
    public boolean shouldValidate(Object target) {
        if (conditionGroups.isEmpty())
            return true;
        // OR: 하나라도 만족하는 그룹이 있으면 검증 진행함
        return conditionGroups.stream().anyMatch(group -> {
            // AND: 그룹 내 모든 조건이 만족되어야 함
            return group.stream().allMatch(cond -> cond.isSatisfied(target));
        });
    }

    /**
     * Returns the error message for the given rule.
     * <p>
     * If a message is defined for the {@code errorMessageKey} in an external property file,
     * it takes priority. This requires the base resource path to be pre-configured via
     * {@link io.github.devers2.s2util.validation.S2Validator#setValidationBundle(String)}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 에러 메시지를 반환합니다.
     * <p>
     * 외부 메시지 설정 파일(Property)에 {@code errorMessageKey}에 해당하는 메시지가 정의되어 있다면
     * 이를 최우선으로 사용합니다. 단, 이 기능을 활성화하려면
     * {@link io.github.devers2.s2util.validation.S2Validator#setValidationBundle(String)}을
     * 통해 기본 리소스 경로가 사전에 설정되어 있어야 합니다.
     * </p>
     *
     * @param rule   Validation rule | 검증 규칙
     * @param locale Current user locale | 현재 사용자 로케일
     * @return Error message | 에러 메시지
     */
    public String getErrorMessage(S2Rule rule, Locale locale) {
        return getErrorMessage(rule.getErrorMessageTemplate(locale), rule.getCheckValue(), locale);
    }

    /**
     * Returns the error message for the given custom check.
     * <p>
     * If a message is defined for the {@code errorMessageKey} in an external property file,
     * it takes priority. This requires the base resource path to be pre-configured via
     * {@link io.github.devers2.s2util.validation.S2Validator#setValidationBundle(String)}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 에러 메시지를 반환합니다.
     * <p>
     * 외부 메시지 설정 파일(Property)에 {@code errorMessageKey}에 해당하는 메시지가 정의되어 있다면
     * 이를 최우선으로 사용합니다. 단, 이 기능을 활성화하려면
     * {@link io.github.devers2.s2util.validation.S2Validator#setValidationBundle(String)}을
     * 통해 기본 리소스 경로가 사전에 설정되어 있어야 합니다.
     * </p>
     *
     * @param customCheck Custom validation logic | 커스텀 검증 로직
     * @param locale      Current user locale | 현재 사용자 로케일
     * @return Error message | 에러 메시지
     */
    public String getErrorMessage(S2CustomRule<?, ?> customCheck, Locale locale) {
        return getErrorMessage(customCheck.getErrorMessageTemplate(locale), null, locale);
    }

    /**
     * Substitutes field label and criterion value to return the complete error message.
     * <p>
     * If a message is defined for the {@code errorMessageKey} in an external property file,
     * it takes priority. This requires the base resource path to be pre-configured via
     * {@link io.github.devers2.s2util.validation.S2Validator#setValidationBundle(String)}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 필드 설명과 기준값을 치환하여 완성된 에러 메시지를 반환합니다.
     * <p>
     * 외부 메시지 설정 파일(Property)에 {@code errorMessageKey}에 해당하는 메시지가 정의되어 있다면
     * 이를 최우선으로 사용합니다. 단, 이 기능을 활성화하려면
     * {@link io.github.devers2.s2util.validation.S2Validator#setValidationBundle(String)}을
     * 통해 기본 리소스 경로가 사전에 설정되어 있어야 합니다.
     * </p>
     *
     * @param errorMessageTemplate Error message template (e.g., "{0|is/are} required") | 에러 메시지 템플릿 (예: "{0|은/는} 필수 입력 항목입니다.")
     * @param checkValue           Validation criterion value | 검증 기준값
     * @param locale               Current user locale | 현재 사용자 로케일
     * @return Substituted error message | 치환 완료된 에러 메시지 (예: "이름은(는) 필수 입력 항목입니다.")
     */
    private String getErrorMessage(String errorMessageTemplate, Object checkValue, Locale locale) {
        var fieldLabel = label != null && !label.isBlank() ? label : name;
        var template = (errorMessageTemplate == null || errorMessageTemplate.isBlank())
                ? (S2Util.isKorean(locale) ? "{0|은/는} 올바르지 않은 값입니다." : "Invalid value: {0}")
                : errorMessageTemplate;

        return S2StringUtil.formatMessage(template, fieldLabel, checkValue);
    }

    /**
     * Performs validation based on the current configuration.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재까지 구성된 내용을 바탕으로 검증을 수행합니다.
     *
     * @param target Target object to validate | 검증 대상 객체
     * @return True if all rules pass, false otherwise | 모든 규칙을 통과하면 true, 아니면 false
     */
    public boolean validate(T target) {
        return validator.validate(target);
    }

    /**
     * Performs validation and passes errors to a handler.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재까지 구성된 내용을 바탕으로 검증을 수행하고, 발생한 에러를 핸들러로 전달합니다.
     *
     * @param target Target object to validate | 검증 대상 객체
     * @param h      Error handler to be invoked | 에러 발생 시 호출될 핸들러
     * @return True if all rules pass, false otherwise | 모든 규칙을 통과하면 true, 아니면 false
     */
    public boolean validate(T target, java.util.function.Consumer<S2Validator.S2ValidationError> h) {
        return validator.validate(target, h);
    }

    /**
     * Completes field configuration and returns to the validator builder.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 필드 설정을 마치고 다시 검증기 빌더로 돌아갑니다.
     *
     * @return The parent S2Validator object | S2Validator 객체
     */
    public S2Validator<T> build() {
        return validator;
    }

    /**
     * Wrapper for custom validation logic provided via lambdas.
     * <p>
     * Uses {@link BiPredicate} to allow inspection of both the individual field value
     * and the root object (for cross-field dependency checks).
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 람다식으로 제공된 사용자 정의 검증 로직을 관리하는 래퍼 클래스입니다.
     * <p>
     * {@link BiPredicate}를 사용하여 개별 필드 값뿐만 아니라 전체 루트 객체를 인자로 받아
     * 필드 간 상관관계 검증(Cross-field validation)을 수행할 수 있습니다.
     * </p>
     */
    public static class S2CustomRule<V, T> implements S2RuleMessageStep, Serializable {

        private static final long serialVersionUID = 2120694816429761625L;

        private final BiPredicate<V, T> logic;
        /** * 로케일 언어별 에러 메시지 템플릿 맵 */
        private final Map<String, String> messageTemplates = new HashMap<>();
        /** 에러 메시지 프로퍼티 키 */
        private String errorMessageKey;

        /**
         * Custom check constructor.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 커스텀 체크 생성자입니다.
         *
         * @param logic Validation logic | 검증 로직
         */
        public S2CustomRule(BiPredicate<V, T> logic) {
            this.logic = logic;
        }

        /**
         * Custom check constructor with message key.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 커스텀 체크 생성자입니다.
         *
         * @param logic           Validation logic | 검증 로직
         * @param errorMessageKey Error message property key | 에러 메시지 프로퍼티 키
         */
        public S2CustomRule(BiPredicate<V, T> logic, String errorMessageKey) {
            this.logic = logic;
            this.errorMessageKey = errorMessageKey;
        }

        /**
         * Implementation of {@link S2RuleMessageStep} to record templates.
         *
         * @param language Targeted language code
         * @param template Template string (e.g., "{0} is invalid")
         * @return This rule instance
         */
        @Override
        public S2RuleMessageStep storeMessage(String language, String template) {
            if (language != null && !language.isBlank() && template != null) {
                this.messageTemplates.put(language, template);
            }
            return this;
        }

        /**
         * Executes the custom validation logic.
         * <p>
         * Implementation detail: Performs dynamic casting with <b>type erasure</b> safety.
         * Since generic info is lost at runtime, this method catches {@link ClassCastException}
         * to handle type mismatches gracefully by returning {@code false}.
         * </p>
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 제공된 람다 로직을 사용하여 유효성 검증을 수행합니다.
         * <p>
         * 구현 상세: Type Erasure 특성상 런타임에 제네릭 정보가 소멸되므로, 런타임에 비정상적인 타입이
         * 인입될 경우를 대비해 {@link ClassCastException}을 처리하여 안전하게 {@code false}를 반환합니다.
         * </p>
         *
         * @param value  The individual field value | 개별 필드 값
         * @param target The root target object | 루트 대상 객체
         * @return {@code true} if valid | 유효한 경우 true
         */
        @SuppressWarnings("unchecked")
        public boolean isValid(Object value, Object target) {
            try {
                V castValue = (V) value;
                T castTarget = (T) target;
                return logic.test(castValue, castTarget);
            } catch (ClassCastException e) {
                if (S2Util.isKorean()) {
                    logger.warn(
                            "커스텀 검증 중 타입 캐스팅 실패. valueType: {}, targetType: {}",
                            value != null ? value.getClass().getName() : "null",
                            target != null ? target.getClass().getName() : "null"
                    );
                } else {
                    logger.warn(
                            "Type casting failure during custom validation. valueType: {}, targetType: {}",
                            value != null ? value.getClass().getName() : "null",
                            target != null ? target.getClass().getName() : "null"
                    );
                }
                return false;
            }
        }

        /**
         * Checks if the value is invalid according to this custom rule.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 이 커스텀 규칙에 따라 값이 유효하지 않은지 확인합니다.
         *
         * @param value  The individual field value | 개별 필드 값
         * @param target The root target object | 루트 대상 객체
         * @return True if invalid | 유효하지 않으면 true
         */
        public boolean isInvalid(Object value, Object target) {
            return !isValid(value, target);
        }

        /**
         * Resolves the localized error message template for this custom rule.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 이 커스텀 규칙에 대한 현지화된 에러 메시지 템플릿을 해석합니다.
         *
         * @param locale Current user locale | 현재 사용자 로케일
         * @return Resolved message template | 해석된 메시지 템플릿
         */
        public String getErrorMessageTemplate(Locale locale) {
            return S2ResourceBundle.getMessage(S2Validator.getValidationBundle(), errorMessageKey, locale).orElseGet(() -> {
                String template = messageTemplates.get(locale.getLanguage());
                if (template == null || template.isBlank()) {
                    template = messageTemplates.get(S2Validator.getDefaultLocale().getLanguage());
                }
                return template;
            });
        }

        /**
         * Returns the error message property key.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 에러 메시지 프로퍼티 키를 반환합니다.
         *
         * @return Error message property key | 에러 메시지 프로퍼티 키
         */
        public String getErrorMessageKey() {
            return errorMessageKey;
        }

    }

}
