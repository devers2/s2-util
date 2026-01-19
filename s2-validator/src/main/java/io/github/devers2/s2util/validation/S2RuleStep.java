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

import java.util.Locale;

/**
 * Fluent API interface representing the state after a rule has been assigned to a field.
 * <p>
 * This interface bridges the rule definition with message customization and continuation
 * steps. It provides specialized sub-interfaces for different execution modes
 * (Validation, Building, and Ad-hoc Checking).
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 필드에 검증 규칙이 할당된 직후의 상태를 나타내는 Fluent API 인터페이스입니다.
 * <p>
 * 규칙 정의와 메시지 커스터마이징, 그리고 다른 필드로의 전환 과정을 연결합니다.
 * 실행 모드(즉시 검증, 빌더, 단일 값 체크)에 따라 최적화된 하위 인터페이스를 제공합니다.
 * </p>
 *
 * @param <T> The type of the root target object
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public interface S2RuleStep<T> extends S2RuleMessageStep {

    @Override
    S2RuleStep<T> storeMessage(String lang, String template);

    /**
     * Sets the Korean (ko) message template.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 한국어(ko) 메시지 템플릿을 설정합니다.
     *
     * @param message Korean message template | 한국어 메시지 템플릿
     * @return Current step instance | 현재 단계 인스턴스
     */
    S2RuleStep<T> ko(String message);

    /**
     * Sets the English (en) message template.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 영어(en) 메시지 템플릿을 설정합니다.
     *
     * @param message English message template | 영어 메시지 템플릿
     * @return Current step instance | 현재 단계 인스턴스
     */
    S2RuleStep<T> en(String message);

    /**
     * Sets the message template for a specific locale.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 특정 로케일에 대한 메시지 템플릿을 설정합니다.
     *
     * @param locale  Target locale | 대상 로케일
     * @param message Message template for the locale | 로케일용 메시지 템플릿
     * @return Current step instance | 현재 단계 인스턴스
     */
    S2RuleStep<T> message(Locale locale, String message);

    // =================================================================
    // Mode-Specific Rule Steps
    // =================================================================

    /**
     * Rule step interface specialized for <b>Validation Mode</b> (started by {@code of()}).
     * <p>
     * Allows setting messages, additional rules, conditions, next fields, or executing
     * the final validation.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Validate 모드({@code of()}로 시작) 전용 규칙 단계 인터페이스입니다.
     * <p>
     * 규칙 설정 후 메시지 설정, 추가 규칙 설정, 조건 설정, 다른 필드 설정 및 검증 실행이 가능합니다.
     * </p>
     */
    interface ValidateRuleStep<T> extends S2FieldStep.ValidateFieldStep<T>, S2RuleMessageStep {
        @Override
        ValidateRuleStep<T> storeMessage(String lang, String template);

        ValidateRuleStep<T> ko(String message);

        ValidateRuleStep<T> en(String message);

        ValidateRuleStep<T> message(Locale locale, String message);
    }

    /**
     * Rule step interface specialized for <b>Builder Mode</b> (started by {@code builder()}).
     * <p>
     * Allows setting messages, additional rules, conditions, next fields, or finalizing
     * the validator blueprint.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Builder 모드({@code builder()}로 시작) 전용 규칙 단계 인터페이스입니다.
     * <p>
     * 규칙 설정 후 메시지 설정, 추가 규칙 설정, 조건 설정, 다른 필드 설정 및 객체 생성이 가능합니다.
     * </p>
     */
    interface BuilderRuleStep<T> extends S2FieldStep.BuilderFieldStep<T>, S2RuleMessageStep {
        @Override
        BuilderRuleStep<T> storeMessage(String lang, String template);

        BuilderRuleStep<T> ko(String message);

        BuilderRuleStep<T> en(String message);

        BuilderRuleStep<T> message(Locale locale, String message);
    }

    /**
     * Rule step interface specialized for <b>Simple Check Mode</b> (no label).
     * <p>
     * Since no label is provided, error message customization (ko/en) is not supported.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Check 모드(라벨 없이 단일 값 체크) 전용 규칙 단계 인터페이스입니다.
     * <p>
     * 라벨이 없으므로 에러 메시지 커스터마이징(ko, en 등)을 지원하지 않습니다.
     * </p>
     */
    interface SimpleCheckRuleStep<T> {
        /**
         * Adds an additional rule for evaluation.
         *
         * @param type Rule type | 규칙 타입
         * @return Current step instance | 현재 단계 인스턴스
         */
        SimpleCheckRuleStep<T> rule(S2RuleType type);

        /**
         * Adds an additional rule for evaluation with a criterion.
         *
         * @param type  Rule type | 규칙 타입
         * @param value Criterion value | 검증 기준값
         * @return Current step instance | 현재 단계 인스턴스
         */
        SimpleCheckRuleStep<T> rule(S2RuleType type, Object value);

        /**
         * Adds a custom Predicate-based rule.
         *
         * @param <V>   Field value type | 필드 값의 타입
         * @param logic Validation logic | 검증 로직
         * @return Current step instance | 현재 단계 인스턴스
         */
        <V> SimpleCheckRuleStep<T> rule(java.util.function.Predicate<V> logic);

        /**
         * Executes validation immediately.
         *
         * @return {@code true} if valid | 유효한 경우 true
         */
        boolean validate();
    }

    /**
     * Rule step interface specialized for <b>Labeled Check Mode</b> (with label).
     * <p>
     * Supports error message localization since a functional label is provided.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Check 모드(라벨 포함 단일 값 체크) 전용 규칙 단계 인터페이스입니다.
     * <p>
     * 라벨이 있으므로 로컬라이징된 에러 메시지 커스터마이징이 가능합니다.
     * </p>
     */
    interface LabeledCheckRuleStep<T> extends SimpleCheckRuleStep<T>, S2RuleMessageStep {
        @Override
        LabeledCheckRuleStep<T> storeMessage(String lang, String template);

        LabeledCheckRuleStep<T> ko(String message);

        LabeledCheckRuleStep<T> en(String message);

        LabeledCheckRuleStep<T> message(Locale locale, String message);

        @Override
        LabeledCheckRuleStep<T> rule(S2RuleType type);

        @Override
        LabeledCheckRuleStep<T> rule(S2RuleType type, Object value);

        LabeledCheckRuleStep<T> rule(S2RuleType type, Object value, String errorMessageKey);

        @Override
        <V> LabeledCheckRuleStep<T> rule(java.util.function.Predicate<V> logic);

        /**
         * Adds a custom Predicate-based rule with a message key.
         *
         * @param <V>             Field value type | 필드 값의 타입
         * @param logic           Validation logic | 검증 로직
         * @param errorMessageKey Custom message key | 커스텀 메시지 키
         * @return Current step instance | 현재 단계 인스턴스
         */
        <V> LabeledCheckRuleStep<T> rule(java.util.function.Predicate<V> logic, String errorMessageKey);

        /**
         * Executes validation with specific locale.
         *
         * @param locale Target locale | 대상 로케일
         * @return {@code true} if valid | 유효한 경우 true
         */
        boolean validate(Locale locale);
    }
}
