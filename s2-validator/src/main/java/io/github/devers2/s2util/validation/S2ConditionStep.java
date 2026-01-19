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

/**
 * Fluent API interface for sequentially adding multiple conditions (AND logic).
 * <p>
 * This interface represents a transient state in the validation builder where
 * a user can continue adding more conditions using {@code and()} or move back to
 * defining rules for the field.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 여러 조건을 순차적으로 추가(논리적 AND)하기 위한 Fluent API 인터페이스입니다.
 * <p>
 * 유효성 검증 빌더 과정에서 조건 설정 직후의 상태를 나타내며, {@code and()}를 통해 조건을
 * 확장하거나 다시 해당 필드에 대한 규칙 정의로 복귀할 수 있는 연결 고리 역할을 합니다.
 * </p>
 *
 * @param <T> The type of the root target object
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public interface S2ConditionStep<T> {

    /**
     * Appends an additional condition using logical <b>AND</b>.
     * <p>
     * All conditions added within the same chain must be satisfied for the
     * validation rules of the field to be triggered.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 논리적 <b>AND</b> 연산으로 추가적인 조건을 설정합니다.
     * <p>
     * 동일한 체인 내에 추가된 모든 조건이 동시에 만족될 때만 해당 필드의 검증 규칙이 실행됩니다.
     * </p>
     *
     * @param fieldName The name of the field to inspect in the target object | 대상 객체 내의 확인 대상 필드 이름
     * @param value     The expected value | 기대되는 기준 값
     * @return The next step in the condition chain | 조건 체인의 다음 단계 인터페이스
     */
    S2ConditionStep<T> and(Object fieldName, Object value);

    // =================================================================
    // Mode-Specific Condition Steps
    // =================================================================

    /**
     * Condition step interface specialized for <b>Validation Mode</b>.
     * <p>
     * Allows extending the condition chain with {@code and()}, adding rules,
     * or moving to next fields and terminal validation.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Validate 모드({@code of()}로 시작) 전용 조건 단계 인터페이스입니다.
     * <p>
     * 조건 설정 후 AND 조건 연장, 규칙 설정, 새로운 조건 설정, 다른 필드 설정 및 검증 실행이 가능합니다.
     * </p>
     */
    interface ValidateConditionStep<T> extends S2ConditionStep<T>, S2FieldStep.ValidateFieldStep<T> {
        @Override
        ValidateConditionStep<T> and(Object fieldName, Object value);
    }

    /**
     * Condition step interface specialized for <b>Builder Mode</b>.
     * <p>
     * Allows extending the condition chain with {@code and()}, adding rules,
     * or moving to next fields and finalizing the builder.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * Builder 모드({@code builder()}로 시작) 전용 조건 단계 인터페이스입니다.
     * <p>
     * 조건 설정 후 AND 조건 연장, 규칙 설정, 새로운 조건 설정, 다른 필드 설정 및 객체 생성이 가능합니다.
     * </p>
     */
    interface BuilderConditionStep<T> extends S2ConditionStep<T>, S2FieldStep.BuilderFieldStep<T> {
        @Override
        BuilderConditionStep<T> and(Object fieldName, Object value);
    }
}
