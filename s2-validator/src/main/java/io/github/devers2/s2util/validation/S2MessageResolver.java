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
import java.util.Optional;

/**
 * Looks up message templates by key, consulted before the validation bundle ({@link S2Validator#setValidationBundle}).
 * <p>
 * It keeps the validator free of framework dependencies: the Spring Boot auto-configuration plugs Spring's
 * {@code MessageSource} in through this interface, and any other source (a database, a cache) can be plugged in the
 * same way with {@link S2Validator#setMessageResolver(S2MessageResolver)}. Return the raw template (with {@code {0}}
 * and josa tokens such as <code>{0|은/는}</code> unformatted), or empty to fall back to the bundle and built-in messages.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 키로 메시지 템플릿을 찾습니다. 검증 번들({@link S2Validator#setValidationBundle})보다 먼저 조회됩니다.
 * <p>
 * 검증기가 프레임워크에 의존하지 않도록 둔 연결 지점입니다. Spring Boot 자동 설정은 이 인터페이스로 Spring 의 {@code MessageSource}를 연결하며,
 * 다른 출처(DB, 캐시)도 {@link S2Validator#setMessageResolver(S2MessageResolver)}로 같은 방식으로 연결할 수 있습니다. 치환하지 않은 원본
 * 템플릿({@code {0}}, <code>{0|은/는}</code> 같은 조사 토큰 포함)을 돌려주고, 없으면 빈 값을 돌려주어 번들과 내장 메시지로 넘어가게 하십시오.
 * </p>
 *
 * @author devers2
 * @since 2.0.0
 */
@FunctionalInterface
public interface S2MessageResolver {

    /**
     * Returns the message template for a key.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 키에 해당하는 메시지 템플릿을 반환합니다.
     *
     * @param key    The message key (for example {@code valid.err.required}) | 메시지 키
     * @param locale The locale | 로케일
     * @return The raw template, or empty when this source has none | 원본 템플릿, 없으면 빈 값
     */
    Optional<String> resolve(String key, Locale locale);
}
