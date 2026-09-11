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
package io.github.devers2.s2util.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the return value of a method call must be consumed or chained to a terminal call.
 * <p>
 * In {@code S2Validator}, this annotation is applied to intermediate step interfaces
 * (e.g., {@code ValidateFieldStep}, {@code BuilderFieldStep}) and starter methods to prevent
 * incomplete validation chains. When processed by the annotation processor or static analysis,
 * leaving a chain incomplete (e.g., calling {@code of(...).field(...)} without a terminal {@code .validate()})
 * results in a compile-time error.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 메서드 호출의 반환값을 반드시 소비(사용)하거나 종단 메서드({@code validate()} / {@code build()})까지
 * 체이닝을 완료해야 함을 나타내는 어노테이션입니다.
 * <p>
 * {@code S2Validator}의 단계별 체이닝 인터페이스 및 시작 메서드에 적용되어, {@code of()} 체인이 {@code .validate()}로 끝나지 않거나
 * {@code builder()} 체인이 {@code .build()}로 끝나지 않고 중간에 중단된 경우 '죽은 코드(Dead Code)'로 간주되어
 * 컴파일 타임 에러를 발생시킵니다.
 * </p>
 *
 * @author devers2
 * @version 1.5
 * @since 1.1
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.CLASS)
public @interface CheckReturnValue {

    /**
     * Warning or error message displayed when the return value is ignored.
     *
     * @return The message shown when the return value is ignored
     */
    String message() default "⚠️ [S2Validator] Return value is ignored! " +
            "Incomplete chaining means validation logic is NEVER executed (Dead Code). " +
            "Chain starting with of() MUST end with .validate(); builder() MUST end with .build(). " +
            "| 반환값이 무시되었습니다! " +
            "체이닝이 완결되지 않으면 검증 로직이 실행되지 않는 '죽은 코드(Dead Code)'가 됩니다. " +
            "of() 체인은 반드시 .validate()로, builder() 체인은 반드시 .build()로 끝나야 합니다.";
}
