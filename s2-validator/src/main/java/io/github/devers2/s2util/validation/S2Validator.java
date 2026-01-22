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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.exception.S2RuntimeException;
import io.github.devers2.s2util.validation.S2Field.S2CustomRule;

/**
 * Fluent Validation Engine for the S2Util library.
 * <p>
 * This class provides a type-safe, declarative way to perform data validation without
 * dependency on heavy frameworks like Bean Validation (JSR-303/380). It supports
 * complex object graphs, collection iteration, and conditional validation rules.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리의 유연한 선언적 유효성 검증 엔진입니다.
 * <p>
 * Bean Validation(JSR-303/380)과 같은 무거운 프레임워크에 의존하지 않고, 순수 자바의 타입 안정성을
 * 활용하여 데이터를 검증할 수 있는 인터페이스를 제공합니다. 복잡한 객체 그래프 탐색, 컬렉션 반복 검증,
 * 조건부 검증 등 엔터프라이즈 환경에서 필요한 다양한 시나리오를 지원합니다.
 * </p>
 *
 * <ul>
 * <li><b>플러그인 적용 (build.gradle):</b>
 *
 * <pre>{@code
 *  plugins {
 *      id 'io.github.devers2.validator' version '1.0.0' // 버전은 상황에 맞게 설정
 *  }
 *     }</pre>
 *
 * </li>
 * <li><b>기본 동작:</b> 플러그인이 적용된 프로젝트에서는 {@code ./gradlew build}, {@code compileJava} 등 Gradle 태스크 실행 시 자동으로 검증이 수행된다.</li>
 * <li><b>IDE 환경 설정 (필요시):</b> IDE의 'Run' 버튼으로 직접 실행 시 Gradle 빌드 사이클을 우회하는 경우가 있으며, 이 경우 검증이 누락될 수 있다. 이를 방지하기 위해 다음 설정을 권장한다.
 * <ul>
 * <li><b>VS Code:</b> {@code .vscode/launch.json}에 {@code "preLaunchTask": "classes"}를 추가하고,
 * {@code .vscode/tasks.json}에 에러 자동 노출 작업을 정의한다.
 *
 * <pre>{@code
 *  // 1. launch.json 예시
 *  {
 *    "configurations": [{
 *      "name": "www",
 *      "mainClass": "biz.placelink.redwit.lp.ApplicationBootstrap",
 *      "preLaunchTask": "classes" // 핵심 설정
 *    }]
 *  }
 *
 *  // 2. tasks.json 예시
 *  {
 *    "version": "2.0.0",
 *    "tasks": [{
 *      "label": "classes",
 *      "type": "shell",
 *      "command": "./gradlew",
 *      "args": ["classes"],
 *      "problemMatcher": ["$gradle"],
 *      "presentation": { "reveal": "always", "focus": true, "clear": true }
 *    }]
 *  }
 *       }</pre>
 *
 * </li>
 * <li><b>IntelliJ IDEA:</b> {@code Settings > Build, Execution, Deployment > Build Tools > Gradle} 메뉴에서 <b>Build and run using</b>을 <b>Gradle</b>로 변경한다.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h3>Key Features (주요 기능)</h3>
 * <ul>
 * <li><b>Fluent API:</b> Construct validation rules using a readable, chainable syntax
 * ({@code .field().rule().ko()}).</li>
 * <li><b>Static Analysis Support:</b> Integrates with {@code s2-validator-plugin} to verify
 * field names at compile-time, preventing runtime {@code NoSuchFieldException}.</li>
 * <li><b>Thread Safety:</b> Completed {@link S2Validator} instances are thread-safe and can
 * be cached and reused across multiple threads.</li>
 * <li><b>Multi-language Messages:</b> Deep integration with {@link java.util.ResourceBundle}
 * for automated error message translation and localization.</li>
 * </ul>
 *
 * <h3>Usage Modes (사용 모드)</h3>
 * <ul>
 * <li><b>Immediate ({@link #of(Object)}):</b> Quick validation of a specific object instance.</li>
 * <li><b>Blueprint ({@link #builder()}):</b> Define reusable validation logic to be stored
 * in {@link S2ValidatorFactory}.</li>
 * <li><b>Single ({@link #check(Object)}):</b> Ad-hoc validation for individual variables.</li>
 * </ul>
 *
 * @param <T> The type of the object to be validated
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class S2Validator<T> implements Serializable {

    private static final long serialVersionUID = -7182934058123948572L;

    /**
     * Default base name for the resource bundle used globally for validation messages.
     * <p>
     * When configured, this bundle is prioritized over {@link S2RuleType}'s internal messages
     * and individual messages defined in {@code S2CustomRule}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 시스템 전역에서 공통으로 참조할 메시지 번들(ResourceBundle)의 기본 이름입니다.
     * <p>
     * 설정 시 {@link S2RuleType}의 내장 메시지는 물론, {@code S2CustomRule}에 설정된
     * 개별 메시지보다도 우선하여 참조됩니다.
     * </p>
     */
    private static String validationBundle;
    /** Default system-wide locale (defaults to {@link Locale#getDefault()}) */
    private static Locale defaultLocale = Locale.getDefault();

    /** List of field validation configurations */
    private final List<S2Field<T>> fields = new ArrayList<>();
    /** Current user locale for the validation session */
    private Locale currentLocale = defaultLocale;
    /** Whether to throw {@link S2RuntimeException} immediately on first failure */
    private boolean failFastWithException = true;

    private S2Validator() {
        this(true);
    }

    /**
     * Private constructor for internal builder use.
     *
     * @param failFastWithException Whether validation should throw exceptions on failure
     */
    private S2Validator(boolean failFastWithException) {
        this.failFastWithException = failFastWithException;
    }

    /**
     * Starts an immediate validation chain for the given target object.
     * <p>
     * Use this mode when you want to execute validation once for a specific instance
     * without creating a reusable validator.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 대상 객체에 대해 즉각적인 검증 체인을 시작합니다.
     * <p>
     * 재사용 가능한 검증기 생성 없이, 특정 인스턴스에 대해 1회성 검증을 수행할 때 사용합니다.
     * </p>
     *
     * @param <T>    The type of the target object | 대상 객체의 타입
     * @param target The object to validate | 검증 대상 객체
     * @return A fluent step to define fields and rules | 필드와 규칙을 정의할 수 있는 Fluent API 단계
     * @apiNote
     *
     *          <pre>{@code
     * // Example 1: Basic validation with error handler
     * Map<String, Object> userData = new HashMap<>();
     * userData.put("userId", "admin");
     * userData.put("age", 20);
     * userData.put("email", "test@s2.kr");
     *
     * List<S2ValidationError> errors = new ArrayList<>();
     *
     * S2Validator.of(userData)
     *     .field("userId", "아이디").rule(S2RuleType.REQUIRED)
     *     .field("age", "나이").rule(S2RuleType.MIN_VALUE, 19)
     *     .field("email", "이메일").rule(S2RuleType.EMAIL)
     *     .validate(errors::add, Locale.KOREAN);
     *
     * if (!errors.isEmpty()) {
     *     errors.forEach(err -> System.out.println(err.defaultMessage()));
     * }
     *
     * // Example 2: Throw exception on failure
     * try {
     *     S2Validator.of(userData)
     *         .field("userId", "아이디").rule(S2RuleType.REQUIRED)
     *         .field("email", "이메일").rule(S2RuleType.EMAIL)
     *         .validate();  // Throws S2RuntimeException if validation fails
     * } catch (S2RuntimeException e) {
     *     System.err.println("검증 실패: " + e.getMessage());
     * }
     *
     * // Example 3: Cross-field validation
     * Map<String, Object> passwordData = new HashMap<>();
     * passwordData.put("password", "secret123");
     * passwordData.put("confirmPw", "secret123");
     *
     * S2Validator.of(passwordData)
     *     .field("confirmPw", "비밀번호 확인")
     *     .rule((value, target) -> {
     *         String password = S2Util.getValue(target, "password", "");
     *         return password.equals(value);
     *     })
     *     .ko("{0|은/는} 원본 비밀번호와 일치해야 합니다.")
     *     .validate();
     * }</pre>
     */
    public static <T> S2FieldStep.ValidateStartStep<T> of(T target) {
        return new S2ValidateChain<>(new S2Validator<>(), target);
    }

    /**
     * Starts an immediate validation chain for the given target object with custom fail-fast behavior.
     * <p>
     * <b>Sequence:</b> {@code field() -> rule() -> ko()/en()}
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * [즉시 검증 모드] 검증 대상 객체(target)를 지정하여 검증 체인을 시작합니다.
     * <br>
     * <b>순서:</b> {@code field() -> rule() -> ko()/en()}
     *
     * @param <T>                   The type of the target object
     * @param target                The object to validate
     * @param failFastWithException If true, failure throws an exception; if false, returns boolean
     * @return A fluent step to define fields and rules
     */
    public static <T> S2FieldStep.ValidateStartStep<T> of(T target, boolean failFastWithException) {
        return new S2ValidateChain<>(new S2Validator<>(failFastWithException), target);
    }

    /**
     * Starts a builder chain to define a reusable {@link S2Validator}.
     * <p>
     * Use this mode to define a "Blueprint" of validation logic. The resulting
     * validator can be registered in {@link S2ValidatorFactory} for high-performance
     * reuse across different service layers.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 재사용 가능한 {@link S2Validator}를 정의하기 위한 빌더 체인을 시작합니다.
     * <p>
     * 검증 로직의 '설계도(Blueprint)'를 정의할 때 사용하며, 생성된 인스턴스는 {@link S2ValidatorFactory}에
     * 등록하여 시스템 전반에서 성능 효율적으로 재사용할 수 있습니다.
     * </p>
     *
     * @param <T> The type of the object this validator will handle | 이 검증기가 처리할 객체의 타입
     * @return A fluent step to construct the validator blueprint | 검증기 설계도를 구성할 Fluent API 단계
     * @apiNote
     *
     *          <pre>{@code
     * // Example 1: Define a reusable validator blueprint
     * S2Validator<UserDTO> userValidator = S2Validator.<UserDTO>builder()
     *     .field("userId", "아이디").rule(S2RuleType.REQUIRED)
     *         .rule(S2RuleType.MIN_LENGTH, 4).ko("{0|은/는} 최소 {1}자 이상이어야 합니다.")
     *     .field("userPw", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
     *     .field("email", "이메일").rule(S2RuleType.EMAIL)
     *     .build();
     *
     * // Later: Reuse the validator for multiple instances
     * UserDTO user1 = new UserDTO();
     * user1.setUserId("admin");
     * user1.setUserPw("password123");
     * user1.setEmail("admin@s2.kr");
     *
     * userValidator.validate(user1);  // Validates user1
     *
     * UserDTO user2 = new UserDTO();
     * user2.setUserId("test");
     * userValidator.validate(user2);  // Reuses the same validator for user2
     *
     * // Example 2: Complex rules with conditional validation
     * S2Validator<Map<String, Object>> complexValidator = S2Validator.builder()
     *     .field("userType", "사용자 타입").rule(S2RuleType.REQUIRED)
     *     .field("adminId", "관리자 ID")
     *         .when("userType", "ADMIN")  // Only validate when userType is ADMIN
     *         .rule(S2RuleType.REQUIRED)
     *         .rule((String v) -> v.startsWith("ADM-"))
     *         .ko("{0|은/는} 'ADM-'로 시작해야 합니다.")
     *     .build();
     *
     * // Example 3: Register in factory for system-wide reuse
     * S2ValidatorFactory.getOrRegister("USER_JOIN", () ->
     *     S2Validator.builder()
     *         .field("userId", "아이디").rule(S2RuleType.REQUIRED)
     *         .field("email", "이메일").rule(S2RuleType.EMAIL)
     *         .build()
     * );
     * }</pre>
     */
    public static <T> S2FieldStep.BuilderStartStep<T> builder() {
        return new S2BuilderChain<>(new S2Validator<>());
    }

    /**
     * Validates a single value without throwing an exception.
     * <p>
     * Primarily used for simple boolean checks where a simple {@code true/false}
     * result is sufficient.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 개별 변수나 값을 예외 발생 없이 검증하며, 결과를 {@code boolean}으로 반환합니다.
     * <p>
     * 단순 논리 체크 등 결과의 참/거짓 여부만 필요할 때 유용합니다.
     * </p>
     *
     * @param <T>   The type of the value | 값의 타입
     * @param value The value to check | 검증할 값
     * @return A rule definition step | 규칙 정의 단계
     * @apiNote
     *
     *          <pre>{@code
     * // Example 1: Basic value check (returns boolean)
     * String email = "test@s2.kr";
     * boolean isValidEmail = S2Validator.check(email)
     *     .rule(S2RuleType.EMAIL)
     *     .validate();
     *
     * if (isValidEmail) {
     *     System.out.println("유효한 이메일");
     * } else {
     *     System.out.println("유효하지 않은 이메일");
     * }
     *
     * // Example 2: Chaining multiple rules
     * String password = "pass123";
     * boolean isValidPassword = S2Validator.check(password)
     *     .rule(S2RuleType.MIN_LENGTH, 8)
     *     .rule(S2RuleType.PATTERN, "^[a-zA-Z0-9]+$")
     *     .validate();
     *
     * // Example 3: Custom predicate rule
     * Integer age = 25;
     * boolean isAdult = S2Validator.check(age)
     *     .rule((Integer value) -> value >= 19)
     *     .validate();
     *
     * // Example 4: Quick inline validation
     * if (S2Validator.check(userId).rule(S2RuleType.REQUIRED).validate()) {
     *     // Process with valid userId
     * }
     * }</pre>
     */
    public static <T> S2RuleStep.SimpleCheckRuleStep<T> check(T value) {
        // failFastWithException = false (Boolean Mode)
        S2Validator<java.util.Map<String, Object>> validator = new S2Validator<>(false);
        java.util.Map<String, Object> wrapper = new java.util.HashMap<>();
        wrapper.put("value", value);

        // 라벨 없이 "value" 키로 초기화
        return new S2ValueChain<>(validator, wrapper, "value", null);
    }

    /**
     * [단일 값 검증 모드 - 예외 발생]
     * <p>
     * 객체나 Map이 아닌 단일 값을 검증하며, 실패 시 지정된 라벨을 사용하여 S2RuntimeException을 던진다.
     * 라벨이 있으므로 에러 메시지 커스터마이징(ko/en)이 가능하다.
     * </p>
     * <br>
     * <b>순서:</b> check(value, label) -> rule() -> ko()/en() -> validate() (throws Exception)
     *
     * @param value 검증 대상 값
     * @param label 에러 메시지에 사용할 라벨
     * @return LabeledCheckRuleStep (메시지 설정 가능)
     */
    public static <T> S2RuleStep.LabeledCheckRuleStep<T> check(T value, String label) {
        // failFastWithException = true (Exception Mode)
        S2Validator<java.util.Map<String, Object>> validator = new S2Validator<>(true);
        java.util.Map<String, Object> wrapper = new java.util.HashMap<>();
        wrapper.put("value", value);

        // 라벨 지정하여 초기화
        return new S2ValueChain<>(validator, wrapper, "value", label);
    }

    /**
     * Implementation of {@link S2RuleStep.LabeledCheckRuleStep} for <b>Labeled Check Mode</b>.
     * <p>
     * Wraps a single value into a temporary Map to leverage the field-based validation logic.
     * This allow developers to use the same fluent API for individual variables or
     * map entries without creating a full domain model.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * [단일 값 검증 모드 - 라벨 포함]에 대한 구현체입니다.
     * <p>
     * 단일 값을 임시 Map 객체로 감싸 필드 기반 검증 엔진을 동일하게 활용할 수 있도록 설계되었습니다.
     * 이를 통해 전체 도메인 모델 생성 없이 개별 변수나 맵 엔트리에 대해 동일한 Fluent API를 사용할 수 있습니다.
     * </p>
     *
     * @param <T> The value type | 검증 대상 값의 타입
     */
    public static class S2ValueChain<T> extends AbstractChain<java.util.Map<String, Object>> implements S2RuleStep.LabeledCheckRuleStep<T> {

        private final java.util.Map<String, Object> targetWithWrapper;

        private S2ValueChain(S2Validator<java.util.Map<String, Object>> validator, java.util.Map<String, Object> targetWithWrapper, String valueKey, String label) {
            super(validator);
            this.targetWithWrapper = targetWithWrapper;
            // 생성 즉시 필드(값 자체)를 선택한 상태로 만듦
            doField(valueKey, label);
        }

        // --- Rule Step ---
        @Override
        public S2ValueChain<T> rule(S2RuleType type) {
            return (S2ValueChain<T>) rule(type, null, null);
        }

        @Override
        public S2ValueChain<T> rule(S2RuleType type, Object value) {
            return (S2ValueChain<T>) rule(type, value, null);
        }

        @Override
        public S2ValueChain<T> rule(S2RuleType type, Object value, String errorMessageKey) {
            doRule(type, value, errorMessageKey);
            return this;
        }

        @Override
        public <V> S2ValueChain<T> rule(Predicate<V> logic) {
            return (S2ValueChain<T>) rule(logic, null);
        }

        @Override
        public <V> S2ValueChain<T> rule(Predicate<V> logic, String errorMessageKey) {
            doCustomRule(logic, errorMessageKey);
            return this;
        }

        // --- Message Step ---
        @Override
        public S2ValueChain<T> storeMessage(String lang, String template) {
            ensureField();
            currentField.storeMessage(lang, template);
            return this;
        }

        @Override
        public S2ValueChain<T> ko(String msg) {
            doMessage(Locale.KOREAN, msg);
            return this;
        }

        @Override
        public S2ValueChain<T> en(String msg) {
            doMessage(Locale.ENGLISH, msg);
            return this;
        }

        @Override
        public S2ValueChain<T> message(Locale loc, String msg) {
            doMessage(loc, msg);
            return this;
        }

        // --- Terminal ---
        @Override
        public boolean validate() {
            return validate(null);
        }

        @Override
        public boolean validate(Locale locale) {
            return new Runner<>(validator).run(targetWithWrapper, null, locale);
        }
    }

    /**
     * Common chain logic using the Adapter Pattern.
     * <p>
     * Connects {@link S2Validator} and {@link S2Field} operations while managing the
     * currently selected field state for fluent chaining.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 어댑터 패턴(Adapter Pattern)을 활용한 공용 체인 로직입니다.
     * <p>
     * S2Validator와 S2Field의 유기적인 기능 연결을 담당하며, 현재 선택된 필드(currentField) 상태를
     * 관리하여 Fluent API 체이닝을 지원합니다.
     * </p>
     *
     * @param <T> The target object type
     */
    abstract static class AbstractChain<T> {
        protected final S2Validator<T> validator;
        protected S2Field<T> currentField;

        protected AbstractChain(S2Validator<T> validator) {
            this.validator = validator;
        }

        /**
         * Selects or initializes a field to be validated.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 검증 대상을 선택하거나 필드를 초기화합니다.
         *
         * @param name  Field name or logical key | 필드명 또는 논리적 키
         * @param label Human-readable label for error messages | 에러 메시지에 사용될 필드 라벨
         */
        protected void doField(Object name, String label) {
            this.currentField = new S2Field<>(validator, name, label);
            validator.fields.add(this.currentField);
        }

        protected void doRule(S2RuleType type, Object value, String errorMessageKey) {
            ensureField();
            currentField.rule(type, value, errorMessageKey);
        }

        protected void doCustomRule(Predicate<?> logic, String errorMessageKey) {
            ensureField();
            currentField.rule(logic, errorMessageKey);
        }

        protected void doCustomCrossRule(BiPredicate<?, T> logic, String errorMessageKey) {
            ensureField();
            currentField.rule(logic, errorMessageKey);
        }

        protected void doMessage(Locale locale, String template) {
            ensureField();
            currentField.message(locale, template);
        }

        protected void doWhen(Object fieldName, Object value) {
            ensureField();
            currentField.when(fieldName, value);
        }

        protected void doAnd(Object fieldName, Object value) {
            ensureField();
            currentField.and(fieldName, value);
        }

        /**
         * Ensures that a field context is active before defining rules.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 규칙을 정의하기 전에 필드 컨텍스트가 활성화되어 있는지 확인합니다.
         *
         * @throws IllegalStateException If called without a prior {@code field()} call | field() 호출 없이 실행된 경우 발생
         */
        protected void ensureField() {
            if (currentField == null)
                throw new IllegalStateException("field() must be called first. (field()를 먼저 호출해야 합니다.)");
        }
    }

    /**
     * Implementation of start, field, and rule steps for <b>Validation Mode</b>.
     * <p>
     * Coordinates the immediate execution of validation logic for a specific object instance.
     * It manages the target object throughout the fluent chain until a terminal {@code validate()}
     * method is invoked.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * [즉시 검증 모드]에 대한 통합 구현체입니다.
     * <p>
     * 특정 객체 인스턴스에 대한 검증 로직의 즉각적인 실행을 조율합니다.
     * Fluent 체인 전체에서 대상 객체를 관리하며, 최종 단계인 {@code validate()} 메서드가 호출될 때
     * 실제 검증을 수행합니다.
     * </p>
     *
     * @param <T> The target object type | 검증 대상 객체의 타입
     */
    public static class S2ValidateChain<T> extends AbstractChain<T> implements S2FieldStep.ValidateStartStep<T>, S2RuleStep.ValidateRuleStep<T>, S2ConditionStep.ValidateConditionStep<T> {

        private final T target;

        private S2ValidateChain(S2Validator<T> validator, T target) {
            super(validator);
            this.target = target;
        }

        // --- Field Step ---
        @Override
        public S2RuleStep.ValidateRuleStep<T> field(Object name, String label) {
            doField(name, label);
            return this;
        }

        // --- Rule Step ---
        @Override
        public S2RuleStep.ValidateRuleStep<T> rule(S2RuleType type) {
            return rule(type, null, null);
        }

        @Override
        public S2RuleStep.ValidateRuleStep<T> rule(S2RuleType type, Object value) {
            return rule(type, value, null);
        }

        @Override
        public S2RuleStep.ValidateRuleStep<T> rule(S2RuleType type, Object value, String errorMessageKey) {
            doRule(type, value, errorMessageKey);
            return this;
        }

        @Override
        public <V> S2RuleStep.ValidateRuleStep<T> rule(Predicate<V> logic) {
            return rule(logic, null);
        }

        @Override
        public <V> S2RuleStep.ValidateRuleStep<T> rule(Predicate<V> logic, String errorMessageKey) {
            doCustomRule(logic, errorMessageKey);
            return this;
        }

        @Override
        public <V> S2RuleStep.ValidateRuleStep<T> rule(BiPredicate<V, T> logic) {
            return rule(logic, null);
        }

        @Override
        public <V> S2RuleStep.ValidateRuleStep<T> rule(BiPredicate<V, T> logic, String errorMessageKey) {
            doCustomCrossRule(logic, errorMessageKey);
            return this;
        }

        @Override
        public S2ConditionStep.ValidateConditionStep<T> when(Object fieldName, Object value) {
            doWhen(fieldName, value);
            return this;
        }

        @Override
        public S2ConditionStep.ValidateConditionStep<T> and(Object fieldName, Object value) {
            doAnd(fieldName, value);
            return this;
        }

        @Override
        public S2RuleStep.ValidateRuleStep<T> storeMessage(String lang, String template) {
            doMessage(null, null); // Dummy or ignored as we use ko/en
            // doMessage expects Locale, but storeMessage expects String lang.
            // Let's refine doMessage or currentField.storeMessage directly.
            ensureField();
            currentField.storeMessage(lang, template);
            return this;
        }

        // --- Message Step ---
        @Override
        public S2RuleStep.ValidateRuleStep<T> ko(String msg) {
            doMessage(Locale.KOREAN, msg);
            return this;
        }

        @Override
        public S2RuleStep.ValidateRuleStep<T> en(String msg) {
            doMessage(Locale.ENGLISH, msg);
            return this;
        }

        @Override
        public S2RuleStep.ValidateRuleStep<T> message(Locale loc, String msg) {
            doMessage(loc, msg);
            return this;
        }

        @Override
        public S2FieldStep.ValidateFieldStep<T> field(Object name) {
            return field(name, null);
        }

        // --- Terminal ---
        @Override
        public boolean validate() {
            return validate(null, null);
        }

        @Override
        public boolean validate(Consumer<S2ValidationError> handler) {
            return new Runner<>(validator).run(target, handler, null);
        }

        @Override
        public boolean validate(Locale locale) {
            return new Runner<>(validator).run(target, null, locale);
        }

        @Override
        public boolean validate(Consumer<S2ValidationError> handler, Locale locale) {
            return new Runner<>(validator).run(target, handler, locale);
        }
    }

    /**
     * Implementation of start, field, and rule steps for <b>Builder Mode</b>.
     * <p>
     * Defines a reusable template (blueprint) for validation logic. The resulting
     * {@link S2Validator} instance can be applied to different object instances later,
     * providing high performance for high-concurrency environments.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * [빌더 모드]에 대한 통합 구현체입니다.
     * <p>
     * 유효성 검증 로직을 위한 재사용 가능한 템플릿(설계도)을 정의합니다. 결과물인
     * {@link S2Validator} 인스턴스는 이후 서로 다른 객체들에 반복 적용될 수 있으며,
     * 고동시성(High Concurrency) 환경에서 우수한 성능을 제공합니다.
     * </p>
     *
     * @param <T> The target object type | 검증 대상 객체의 타입
     */
    public static class S2BuilderChain<T> extends AbstractChain<T> implements S2FieldStep.BuilderStartStep<T>, S2RuleStep.BuilderRuleStep<T>, S2ConditionStep.BuilderConditionStep<T> {

        private S2BuilderChain(S2Validator<T> validator) {
            super(validator);
        }

        // --- Field Step ---
        @Override
        public S2RuleStep.BuilderRuleStep<T> field(Object name, String label) {
            doField(name, label);
            return this;
        }

        // --- Rule Step ---
        @Override
        public S2RuleStep.BuilderRuleStep<T> rule(S2RuleType type) {
            return rule(type, null, null);
        }

        @Override
        public S2RuleStep.BuilderRuleStep<T> rule(S2RuleType type, Object value) {
            return rule(type, value, null);
        }

        @Override
        public S2RuleStep.BuilderRuleStep<T> rule(S2RuleType type, Object value, String errorMessageKey) {
            doRule(type, value, errorMessageKey);
            return this;
        }

        @Override
        public <V> S2RuleStep.BuilderRuleStep<T> rule(Predicate<V> logic) {
            return rule(logic, null);
        }

        @Override
        public <V> S2RuleStep.BuilderRuleStep<T> rule(Predicate<V> logic, String errorMessageKey) {
            doCustomRule(logic, errorMessageKey);
            return this;
        }

        @Override
        public <V> S2RuleStep.BuilderRuleStep<T> rule(BiPredicate<V, T> logic) {
            return rule(logic, null);
        }

        @Override
        public <V> S2RuleStep.BuilderRuleStep<T> rule(BiPredicate<V, T> logic, String errorMessageKey) {
            doCustomCrossRule(logic, errorMessageKey);
            return this;
        }

        @Override
        public S2ConditionStep.BuilderConditionStep<T> when(Object fieldName, Object value) {
            doWhen(fieldName, value);
            return this;
        }

        @Override
        public S2ConditionStep.BuilderConditionStep<T> and(Object fieldName, Object value) {
            doAnd(fieldName, value);
            return this;
        }

        @Override
        public S2RuleStep.BuilderRuleStep<T> storeMessage(String lang, String template) {
            ensureField();
            currentField.storeMessage(lang, template);
            return this;
        }

        // --- Message Step ---
        @Override
        public S2RuleStep.BuilderRuleStep<T> ko(String msg) {
            doMessage(Locale.KOREAN, msg);
            return this;
        }

        @Override
        public S2RuleStep.BuilderRuleStep<T> en(String msg) {
            doMessage(Locale.ENGLISH, msg);
            return this;
        }

        @Override
        public S2RuleStep.BuilderRuleStep<T> message(Locale loc, String msg) {
            doMessage(loc, msg);
            return this;
        }

        @Override
        public S2FieldStep.BuilderFieldStep<T> field(Object name) {
            return field(name, null);
        }

        // --- Terminal ---
        @Override
        public S2Validator<T> build() {
            return validator;
        }
    }

    /**
     * Internal validation execution engine (supports recursive calls).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 내부 검증 실행 엔진입니다 (재귀 호출 및 중첩 검증 지원).
     *
     * @param target       The object to validate
     * @param errorHandler Consumer to handle validation errors
     * @param locale       Target locale for error messages
     * @param visited      Set of already visited objects to prevent circular references
     * @param depth        Current recursion depth
     * @return True if validation passes, false otherwise
     */
    protected boolean run(T target, Consumer<S2ValidationError> errorHandler, Locale locale, java.util.Set<Object> visited, int depth) {
        return new Runner<>(this).run(target, errorHandler, locale, visited, depth);
    }

    // =========================================================================
    // Validation Runner (Logic Preserved)
    // =========================================================================

    /**
     * Encapsulates the runtime execution logic for validation rules.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증 규칙의 런타임 실행 로직을 캡슐화한 실행기 클래스입니다.
     *
     * @param <T> The target object type
     */
    public static class Runner<T> {
        private final S2Validator<T> config;

        protected Runner(S2Validator<T> config) {
            this.config = config;
        }

        public boolean run(T target, Consumer<S2ValidationError> errorHandler, Locale locale) {
            return run(target, errorHandler, locale, java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()), 0);
        }

        protected boolean run(T target, Consumer<S2ValidationError> errorHandler, Locale locale, java.util.Set<Object> visited, int depth) {
            if (target == null)
                return true;

            // [순환 참조 방지 - IdentityHashMap 기반 Set 추적]
            // 동일한 객체 인스턴스가 이미 검증 경로에 있는지 확인
            if (!visited.add(target)) {
                return true;
            }

            try {
                var isAllValid = true;
                var currentLocale = locale != null ? locale : config.currentLocale;

                // 1단계: [] 와일드카드가 포함된 필드들을 prefix별로 그룹화
                Map<String, List<S2Field<?>>> wildcardGroups = new java.util.LinkedHashMap<>();
                Set<String> processedFields = new java.util.HashSet<>();

                for (var field : config.fields) {
                    if (field == null)
                        continue;
                    String fieldName = String.valueOf(field.getName());

                    // [] 패턴 감지
                    if (fieldName.contains("[]")) {
                        // prefix 추출 (예: "items[].name" -> "items")
                        int bracketIndex = fieldName.indexOf("[]");
                        String prefix = fieldName.substring(0, bracketIndex);

                        wildcardGroups.computeIfAbsent(prefix, k -> new java.util.ArrayList<>()).add(field);
                    }
                }

                // 2단계: 그룹화된 와일드카드 필드들을 처리
                for (Map.Entry<String, List<S2Field<?>>> entry : wildcardGroups.entrySet()) {
                    String collectionPrefix = entry.getKey();
                    List<S2Field<?>> groupFields = entry.getValue();

                    // 컬렉션 값 가져오기
                    Object collectionValue = S2Util.getValue(target, collectionPrefix);

                    if (collectionValue instanceof Iterable<?> it) {
                        int idx = 0;
                        for (Object item : it) {
                            isAllValid &= processWildcardItem(item, collectionPrefix, idx, groupFields, target, errorHandler, currentLocale, visited, depth);
                            idx++;
                        }
                    } else if (collectionValue != null && collectionValue.getClass().isArray()) {
                        int len = java.lang.reflect.Array.getLength(collectionValue);
                        for (int i = 0; i < len; i++) {
                            Object item = java.lang.reflect.Array.get(collectionValue, i);
                            isAllValid &= processWildcardItem(item, collectionPrefix, i, groupFields, target, errorHandler, currentLocale, visited, depth);
                        }
                    }

                    // 처리한 필드들을 기록
                    for (S2Field<?> field : groupFields) {
                        processedFields.add(String.valueOf(field.getName()));
                    }
                }

                // 3단계: 일반 필드 처리 (와일드카드가 아닌 필드 or 이미 처리되지 않은 필드)
                for (var field : config.fields) {
                    if (field == null || !field.shouldValidate(target))
                        continue;

                    String fieldName = String.valueOf(field.getName());

                    // 이미 와일드카드로 처리된 필드는 스킵
                    if (processedFields.contains(fieldName))
                        continue;

                    var fieldValue = S2Util.getValue(target, fieldName);
                    var fieldLabel = field.getLabel();
                    var rules = field.getRules();
                    var customRules = field.getCustomRules();

                    if (rules.isEmpty() && customRules.isEmpty()) {
                        var requiredCheck = S2Rule.required();
                        if (requiredCheck.isInvalid(fieldValue, target)) {
                            isAllValid = false;
                            if (!reportError(
                                    errorHandler, new S2ValidationError(
                                            fieldName,
                                            requiredCheck.getErrorMessageKey(),
                                            new Object[] { fieldLabel },
                                            field.getErrorMessage(requiredCheck, currentLocale)
                                    ), config.failFastWithException
                            ))
                                return false;
                        }
                    }
                    for (var rule : rules) {
                        if (rule.getRuleType() == S2RuleType.NESTED || rule.getRuleType() == S2RuleType.EACH) {
                            if (rule.getCheckValue() instanceof S2Validator<?> sub) {
                                @SuppressWarnings("unchecked")
                                S2Validator<Object> subObj = (S2Validator<Object>) sub;

                                // [순환 참조 체크 - Set 기반 객체 인스턴스 추적]
                                // 동일한 객체가 이미 검증 경로에 있으면 순환 참조로 판단
                                if (fieldValue != null && visited.contains(fieldValue)) {
                                    isAllValid = false;
                                    if (!reportError(
                                            errorHandler, new S2ValidationError(
                                                    fieldName,
                                                    "ERR_CIRCULAR_REFERENCE",
                                                    new Object[] { fieldLabel },
                                                    "순환 참조가 감지되었습니다."
                                            ), config.failFastWithException
                                    ))
                                        return false;
                                    continue;
                                }

                                if (rule.getRuleType() == S2RuleType.NESTED) {
                                    if (fieldValue != null) {
                                        boolean subOk = subObj.run(fieldValue, (S2ValidationError err) -> {
                                            reportError(
                                                    errorHandler, new S2ValidationError(
                                                            fieldName + (S2Util.isEmpty(err.fieldName()) ? "" : "." + err.fieldName()),
                                                            err.errorCode(),
                                                            err.errorArgs(),
                                                            err.defaultMessage()
                                                    ), config.failFastWithException
                                            );
                                        }, currentLocale, visited, depth + 1);
                                        if (!subOk)
                                            isAllValid = false;
                                    }
                                } else { // EACH
                                    if (fieldValue instanceof Iterable<?> it) {
                                        int idx = 0;
                                        for (Object item : it) {
                                            final int finalIdx = idx;
                                            boolean subOk = subObj.run(item, (S2ValidationError err) -> {
                                                reportError(
                                                        errorHandler, new S2ValidationError(
                                                                fieldName + "[" + finalIdx + "]" + (S2Util.isEmpty(err.fieldName()) ? "" : "." + err.fieldName()),
                                                                err.errorCode(),
                                                                err.errorArgs(),
                                                                err.defaultMessage()
                                                        ), config.failFastWithException
                                                );
                                            }, currentLocale, visited, depth + 1);
                                            if (!subOk)
                                                isAllValid = false;
                                            idx++;
                                        }
                                    } else if (fieldValue != null && fieldValue.getClass().isArray()) {
                                        int len = java.lang.reflect.Array.getLength(fieldValue);
                                        for (int i = 0; i < len; i++) {
                                            final int finalIdx = i;
                                            Object item = java.lang.reflect.Array.get(fieldValue, i);
                                            boolean subOk = subObj.run(item, (S2ValidationError err) -> {
                                                reportError(
                                                        errorHandler, new S2ValidationError(
                                                                fieldName + "[" + finalIdx + "]" + (S2Util.isEmpty(err.fieldName()) ? "" : "." + err.fieldName()),
                                                                err.errorCode(),
                                                                err.errorArgs(),
                                                                err.defaultMessage()
                                                        ), config.failFastWithException
                                                );
                                            }, currentLocale, visited, depth + 1);
                                            if (!subOk)
                                                isAllValid = false;
                                        }
                                    }
                                }
                            }
                            continue;
                        }

                        if (rule.isInvalid(fieldValue, target)) {
                            isAllValid = false;
                            var args = S2Util.isNotEmpty(rule.getCheckValue()) ? new Object[] { fieldLabel, rule.getCheckValue() } : new Object[] { fieldLabel };
                            if (!reportError(
                                    errorHandler, new S2ValidationError(
                                            fieldName,
                                            rule.getErrorMessageKey(),
                                            args,
                                            field.getErrorMessage(rule, currentLocale)
                                    ), config.failFastWithException
                            ))
                                return false;
                        }
                    }
                    for (var customCheck : customRules) {
                        if (customCheck.isInvalid(fieldValue, target)) {
                            isAllValid = false;
                            if (!reportError(
                                    errorHandler, new S2ValidationError(
                                            fieldName,
                                            customCheck.getErrorMessageKey(),
                                            null,
                                            field.getErrorMessage(customCheck, currentLocale)
                                    ), config.failFastWithException
                            ))
                                return false;
                        }
                    }
                }
                return isAllValid;
            } finally {
                visited.remove(target);
            }
        }

        /**
         * 와일드카드 필드 그룹의 각 아이템을 검증한다.
         *
         * @param item             검증할 아이템 (컬렉션의 요소)
         * @param collectionPrefix 컬렉션 prefix (예: "items")
         * @param index            현재 인덱스
         * @param groupFields      같은 prefix를 공유하는 필드들
         * @param target           원본 검증 대상 객체
         * @param errorHandler     에러 핸들러
         * @param locale           로케일
         * @return 모든 필드가 유효하면 true
         */
        private boolean processWildcardItem(
                Object item,
                String collectionPrefix,
                int index,
                List<S2Field<?>> groupFields,
                T target,
                Consumer<S2ValidationError> errorHandler,
                Locale locale,
                java.util.Set<Object> visited,
                int depth) {
            boolean isValid = true;

            for (S2Field<?> field : groupFields) {
                String fullFieldName = String.valueOf(field.getName());

                // "items[].name" -> "name" 추출
                int bracketIndex = fullFieldName.indexOf("[]");
                String suffix = fullFieldName.substring(bracketIndex + 2);
                if (suffix.startsWith(".")) {
                    suffix = suffix.substring(1);
                }

                // item에서 실제 값 추출
                Object fieldValue = S2Util.isEmpty(suffix) ? item : S2Util.getValue(item, suffix);
                String fieldLabel = field.getLabel();
                List<S2Rule> rules = field.getRules();
                var customRules = field.getCustomRules(); // Use var for type inference

                // 에러 경로: items[0].name 형식
                String errorPath = collectionPrefix + "[" + index + "]" + (S2Util.isEmpty(suffix) ? "" : "." + suffix);

                // 기본 REQUIRED 검증
                if (rules.isEmpty() && customRules.isEmpty()) {
                    var requiredCheck = S2Rule.required();
                    if (requiredCheck.isInvalid(fieldValue, item)) {
                        isValid = false;
                        reportError(
                                errorHandler, new S2ValidationError(
                                        errorPath,
                                        requiredCheck.getErrorMessageKey(),
                                        new Object[] { fieldLabel },
                                        field.getErrorMessage(requiredCheck, locale)
                                ), config.failFastWithException
                        );
                    }
                }

                // 일반 규칙 검증
                for (S2Rule rule : rules) {
                    // NESTED와 EACH는 와일드카드 문법에서 지원하지 않음 (이미 sub-validator 방식으로 처리 가능)
                    if (rule.getRuleType() == S2RuleType.NESTED || rule.getRuleType() == S2RuleType.EACH) {
                        continue;
                    }

                    if (rule.isInvalid(fieldValue, item)) {
                        isValid = false;
                        var args = S2Util.isNotEmpty(rule.getCheckValue())
                                ? new Object[] { fieldLabel, rule.getCheckValue() }
                                : new Object[] { fieldLabel };
                        reportError(
                                errorHandler, new S2ValidationError(
                                        errorPath,
                                        rule.getErrorMessageKey(),
                                        args,
                                        field.getErrorMessage(rule, locale)
                                ), config.failFastWithException
                        );
                    }
                }

                // 커스텀 규칙 검증
                for (S2CustomRule<?, ?> customCheck : customRules) {
                    @SuppressWarnings("unchecked")
                    S2CustomRule<Object, Object> typedCheck = (S2CustomRule<Object, Object>) customCheck;
                    if (typedCheck.isInvalid(fieldValue, item)) {
                        isValid = false;
                        reportError(
                                errorHandler, new S2ValidationError(
                                        errorPath,
                                        customCheck.getErrorMessageKey(),
                                        null,
                                        field.getErrorMessage(customCheck, locale)
                                ), config.failFastWithException
                        );
                    }
                }
            }

            return isValid;
        }
    }

    /**
     * Executes validation on the target object using default settings.
     * <p>
     * This method uses the default locale and no custom error handler.
     * If validation fails and {@code failFastWithException} is true, throws {@link S2RuntimeException}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 기본 설정을 사용하여 대상 객체에 대한 검증을 실행합니다.
     *
     * @param target The object to validate | 검증 대상 객체
     * @return {@code true} if validation passes | 검증 성공 시 true
     * @throws S2RuntimeException If validation fails and exception mode is enabled | 검증 실패 및 예외 모드인 경우
     */
    public boolean validate(T target) {
        return validate(target, null, null);
    }

    /**
     * Executes validation with a custom error handler.
     * <p>
     * The handler will be invoked for each validation error found.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 커스텀 에러 핸들러를 사용하여 검증을 실행합니다.
     *
     * @param target  The object to validate | 검증 대상 객체
     * @param handler Consumer to handle validation errors | 검증 에러를 처리할 핸들러
     * @return {@code true} if validation passes | 검증 성공 시 true
     */
    public boolean validate(T target, Consumer<S2ValidationError> handler) {
        return validate(target, handler, null);
    }

    /**
     * Executes validation using a specific locale for error messages.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 특정 로케일을 사용하여 검증을 실행합니다.
     *
     * @param target The object to validate | 검증 대상 객체
     * @param locale The locale for error message resolution | 에러 메시지 해석에 사용할 로케일
     * @return {@code true} if validation passes | 검증 성공 시 true
     */
    public boolean validate(T target, Locale locale) {
        return validate(target, null, locale);
    }

    /**
     * Executes validation with full control over error handling and localization.
     * <p>
     * This is the most flexible validation method, allowing both custom error handling
     * and specific locale selection.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 에러 처리와 로케일을 완전히 제어하여 검증을 실행합니다.
     * 가장 유연한 검증 메서드로, 커스텀 에러 핸들링과 특정 로케일 선택을 모두 지원합니다.
     *
     * @param target  The object to validate | 검증 대상 객체
     * @param handler Consumer to handle validation errors | 검증 에러를 처리할 핸들러
     * @param locale  The locale for error message resolution | 에러 메시지 해석에 사용할 로케일
     * @return {@code true} if all validations pass | 모든 검증을 통과한 경우 true
     */
    public boolean validate(T target, Consumer<S2ValidationError> handler, Locale locale) {
        return new Runner<>(this).run(target, handler, locale);
    }

    /**
     * Reports a validation error either by invoking the handler or throwing an exception.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 에러가 발생했을 때 설정에 따라 예외를 던지거나 핸들러에 전달합니다.
     *
     * @param errorHandler Consumer to handle the error | 에러 핸들러
     * @param error        The validation error information | 에러 정보
     * @param throwEx      Whether to throw an exception if no handler is present | 예외 발생 여부
     * @return {@code true} if processing should continue | 처리를 계속할 경우 true
     */
    private static boolean reportError(Consumer<S2ValidationError> errorHandler, S2ValidationError error, boolean throwEx) {
        if (errorHandler == null) {
            if (throwEx)
                throw new S2RuntimeException(error.defaultMessage());
            return false;
        }
        errorHandler.accept(error);
        return true;
    }

    /**
     * Sets the global message bundle base name for the validation system.
     * <p>
     * Internally delegates to {@link S2Validator#validationBundle}. This bundle takes
     * priority over {@link S2RuleType}'s built-in messages and individual
     * messages defined in custom rules.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증 시스템에서 공통으로 참조할 메시지 번들(ResourceBundle)의 기본 이름을 설정합니다.
     * <p>
     * 입력값은 클래스패스 상의 경로를 포함한 이름(예: {@code messages/validation})입니다.
     * 설정 시 해당 경로에서 로케일별 {@code .properties} 파일을 검색하여 다국어 메시지를 조회합니다.
     * </p>
     *
     * <p>
     * <b>우선순위 및 적용 조건:</b>
     * </p>
     * <ul>
     * <li>본 설정은 모든 내장/커스텀 메시지보다 최우선순위를 갖습니다.</li>
     * <li>단, {@code validationBundle}이 설정되어 있어야 하고, 검증 규칙에 에러 메시지 키가 정의되어 있어야 하며,
     * 실제 해당 번들 파일 내에 해당 키에 매핑된 메시지가 존재해야만 적용됩니다.</li>
     * <li>위 조건 중 하나라도 만족하지 않으면 내장된 기본 메시지나 커스텀 메시지를 사용합니다.</li>
     * </ul>
     *
     * @param bundleName Base name of the resource bundle (including path) | 리소스 번들의 BaseName (경로 포함)
     */
    public static void setValidationBundle(String bundleName) {
        S2Validator.validationBundle = bundleName;
    }

    /**
     * Gets the currently configured global validation message bundle name.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 설정된 전역 검증 메시지 번들의 이름을 반환합니다.
     *
     * @return The validation bundle base name | 현재 설정된 전역 검증 메시지 번들의 이름
     */
    static String getValidationBundle() {
        return S2Validator.validationBundle;
    }

    /**
     * 검증 시스템의 기본 로케일을 설정한다.
     * <p>
     * 메시지 조회 시 요청된 로케일에 해당하는 메시지가 없을 경우
     * 이 설정값을 기준으로 메시지를 재조회한다.
     * </p>
     */
    public static void setDefaultLocale(Locale locale) {
        if (locale != null) {
            S2Validator.defaultLocale = locale;
        }
    }

    /**
     * 검증 시스템의 기본 로케일을 반환한다.
     *
     * @return 현재 설정된 기본 로케일
     */
    static Locale getDefaultLocale() {
        return S2Validator.defaultLocale;
    }

    public List<S2Field<T>> getFields() {
        return fields;
    }

    protected void startField(Object name, String label) {
        S2Field<T> field = new S2Field<>(this, name, label);
        this.fields.add(field);
    }

    /**
     * Data record representing a single validation failure.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 유효성 검사 실패 정보를 담는 불변 레코드 객체입니다.
     *
     * @param fieldName      The logical path to the field (e.g., "items[0].name")
     * @param errorCode      Internal error code or translation key
     * @param errorArgs      Arguments for parameterized error messages
     * @param defaultMessage Human-readable error message in the requested locale
     */
    public record S2ValidationError(
            String fieldName,
            String errorCode,
            Object[] errorArgs,
            String defaultMessage
    ) {}

}
