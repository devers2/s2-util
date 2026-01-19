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
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Fluent API interface for defining target fields and their metadata.
 * <p>
 * This interface follows the <b>Step Pattern</b>, guiding the user through a
 * specific sequence: {@code field() -> rule() -> [when() -> and()] -> field() or terminal}.
 * It differentiates between "Immediate Validation" (started by {@code of()}) and
 * "Validator Blueprinting" (started by {@code builder()}).
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 검증 대상 필드 및 메타데이터 정의를 위한 Fluent API 인터페이스입니다.
 * <p>
 * <b>Step Pattern</b>을 따름으로써 사용자가 올바른 순서({@code field() -> rule() -> [when() -> and()] -> field() 또는 종료})로
 * API를 호출하도록 유도합니다. "즉시 검증 모드({@code of()}로 시작)"와 "재사용 검증기 생성 모드({@code builder()}로 시작)"에 따라
 * 노출되는 메서드를 다르게 제어합니다.
 * </p>
 *
 * @param <T> The type of the root target object
 * @author devers2
 * @version 1.5
 * @since 1.0
 * @see S2RuleStep
 */
public interface S2FieldStep<T> {

    /**
     * Common base interface for all validation builder steps.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 모든 유효성 검증 빌더 단계의 공통 기반 인터페이스입니다.
     */
    interface BaseStep<T> {
        /**
         * Starts a new field validation configuration.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 새로운 필드에 대한 검증 설정을 시작합니다.
         *
         * @param name  Unique identifier for the data (field name, map key, etc.) | 데이터의 식별자 (필드명, 맵 키 등)
         * @param label Logical name shown to users in error messages | 사용자에게 노출될 논리적 명칭 (라벨)
         * @return The next step interface for field configuration | 필드 설정을 위한 다음 단계 인터페이스
         */
        BaseStep<T> field(Object name, String label);

        /**
         * Starts a new field validation configuration where the label defaults to the name.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 새로운 필드 검증 설정을 시작합니다. 별도 라벨이 지정되지 않을 경우 이름과 동일하게 설정됩니다.
         *
         * @param name Unique identifier for the data | 데이터의 식별자
         * @return The next step interface for field configuration | 필드 설정을 위한 다음 단계 인터페이스
         */
        default BaseStep<T> field(Object name) {
            return field(name, null);
        }
    }

    // =================================================================
    // Validate Mode Step Interfaces (started by S2Validator.of)
    // =================================================================

    /**
     * Start step interface specialized for <b>Validation Mode</b> (started by {@code of()}).
     * <p>
     * Only {@code field()} calls are permitted at this stage.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Validate 모드({@code of()}로 시작)의 시작 단계 인터페이스입니다.
     * 오직 {@code field()} 호출만 허용됩니다.
     */
    interface ValidateStartStep<T> extends BaseStep<T> {
        @Override
        ValidateFieldStep<T> field(Object name, String label);

        @Override
        default ValidateFieldStep<T> field(Object name) {
            return field(name, null);
        }
    }

    /**
     * Field configuration step interface specialized for <b>Validation Mode</b>.
     * <p>
     * Allows chain calls to {@code rule()}, {@code when()}, or next {@code field()}.
     * Exposes the terminal {@code validate()} methods to execute the engine immediately.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Validate 모드({@code of()}로 시작)의 필드 설정 단계 인터페이스입니다.
     * <p>
     * 규칙정의({@code rule()}), 조건설정({@code when()}) 및 다른 필드로의 이동({@code field()})이 가능하며,
     * 즉시 검증을 수행하고 결과를 반환하는 {@code validate()} 메서드들을 제공합니다.
     * </p>
     */
    interface ValidateFieldStep<T> extends BaseStep<T> {
        @Override
        ValidateFieldStep<T> field(Object name, String label);

        @Override
        default ValidateFieldStep<T> field(Object name) {
            return field(name, null);
        }

        S2RuleStep.ValidateRuleStep<T> rule(S2RuleType type);

        S2RuleStep.ValidateRuleStep<T> rule(S2RuleType type, Object value);

        S2RuleStep.ValidateRuleStep<T> rule(S2RuleType type, Object value, String errorMessageKey);

        <V> S2RuleStep.ValidateRuleStep<T> rule(Predicate<V> logic);

        <V> S2RuleStep.ValidateRuleStep<T> rule(Predicate<V> logic, String errorMessageKey);

        <V> S2RuleStep.ValidateRuleStep<T> rule(BiPredicate<V, T> logic);

        <V> S2RuleStep.ValidateRuleStep<T> rule(BiPredicate<V, T> logic, String errorMessageKey);

        S2ConditionStep.ValidateConditionStep<T> when(Object fieldName, Object value);

        /**
         * Executes validation immediately and returns the result.
         *
         * @return {@code true} if all rules passed | 규칙을 모두 통과한 경우 true
         */
        boolean validate();

        /**
         * Executes validation and passes any errors to the specified handler.
         *
         * @param handler Consumer that handles validation errors | 에러 발생 시 호출될 핸들러
         * @return {@code true} if all rules passed | 규칙을 모두 통과한 경우 true
         */
        boolean validate(Consumer<S2Validator.S2ValidationError> handler);

        /**
         * Executes validation using the specified locale.
         *
         * @param locale Locale for error message resolution | 에러 메시지 처리를 위한 로케일
         * @return {@code true} if all rules passed | 규칙을 모두 통과한 경우 true
         */
        boolean validate(Locale locale);

        /**
         * Executes validation and handles errors using the specified handler and locale.
         *
         * @param handler Consumer that handles validation errors | 에러 발생 시 호출될 핸들러
         * @param locale  Locale for error message resolution | 에러 메시지 처리를 위한 로케일
         * @return {@code true} if all rules passed | 규칙을 모두 통과한 경우 true
         */
        boolean validate(Consumer<S2Validator.S2ValidationError> handler, Locale locale);
    }

    // =================================================================
    // Builder Mode Step Interfaces (started by S2Validator.builder)
    // =================================================================

    /**
     * Start step interface specialized for <b>Builder Mode</b> (started by {@code builder()}).
     * <p>
     * Only {@code field()} calls are permitted at this stage.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Builder 모드({@code builder()}로 시작)의 시작 단계 인터페이스입니다.
     * 오직 {@code field()} 호출만 허용됩니다.
     */
    interface BuilderStartStep<T> extends BaseStep<T> {
        @Override
        BuilderFieldStep<T> field(Object name, String label);

        @Override
        default BuilderFieldStep<T> field(Object name) {
            return field(name, null);
        }
    }

    /**
     * Field configuration step interface specialized for <b>Builder Mode</b>.
     * <p>
     * Allows chain calls to {@code rule()}, {@code when()}, or next {@code field()}.
     * Exposes the terminal {@code build()} method to generate the reusable validator.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Builder 모드({@code builder()}로 시작)의 필드 설정 단계 인터페이스입니다.
     * <p>
     * 규칙정의({@code rule()}), 조건설정({@code when()}) 및 다른 필드로의 이동({@code field()})이 가능하며,
     * 최종적으로 설계도 인스턴스를 생성하는 {@code build()} 메서드를 제공합니다.
     * </p>
     */
    interface BuilderFieldStep<T> extends BaseStep<T> {
        @Override
        BuilderFieldStep<T> field(Object name, String label);

        @Override
        default BuilderFieldStep<T> field(Object name) {
            return field(name, null);
        }

        S2RuleStep.BuilderRuleStep<T> rule(S2RuleType type);

        S2RuleStep.BuilderRuleStep<T> rule(S2RuleType type, Object value);

        S2RuleStep.BuilderRuleStep<T> rule(S2RuleType type, Object value, String errorMessageKey);

        <V> S2RuleStep.BuilderRuleStep<T> rule(Predicate<V> logic);

        <V> S2RuleStep.BuilderRuleStep<T> rule(Predicate<V> logic, String errorMessageKey);

        <V> S2RuleStep.BuilderRuleStep<T> rule(BiPredicate<V, T> logic);

        <V> S2RuleStep.BuilderRuleStep<T> rule(BiPredicate<V, T> logic, String errorMessageKey);

        S2ConditionStep.BuilderConditionStep<T> when(Object fieldName, Object value);

        /**
         * Completes the builder process and generates a reusable validator instance.
         *
         * @return A thread-safe, reusable S2Validator instance | 재사용 가능한 S2Validator 인스턴스
         */
        S2Validator<T> build();
    }

}
