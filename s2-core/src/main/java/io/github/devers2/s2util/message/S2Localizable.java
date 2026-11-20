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
package io.github.devers2.s2util.message;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Optional;

/**
 * An exception that can describe itself in the user's language. {@link #getMessage()} stays English for logs and
 * searches; {@link #getLocalizedMessage(Locale)} renders the {@link S2MessageKey} for a request's locale.
 * <pre>{@code
 * // Spring: show the user's language, never internal details
 * @ExceptionHandler(Exception.class)
 * ResponseEntity<String> handle(Exception e, Locale locale) {
 *     var message = S2Localizable.findLocalizedMessage(e, locale).orElse(genericMessage(locale));
 *     return ResponseEntity.badRequest().body(message);
 * }
 * }</pre>
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 사용자의 언어로 자신을 설명할 수 있는 예외입니다. {@link #getMessage()} 는 로그·검색용으로 영어를 유지하고,
 * {@link #getLocalizedMessage(Locale)} 가 요청 로케일에 맞게 {@link S2MessageKey} 를 문구로 만듭니다.
 */
public interface S2Localizable {

    /**
     * The message key, or {@code null} when this exception has no user-facing message.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 메시지 키. 사용자에게 보여 줄 문구가 없는 예외면 {@code null}.
     * @return Message key or {@code null} | 메시지 키 또는 {@code null}
     */
    S2MessageKey getMessageKey();

    /**
     * The English message for logs (implemented by {@link Throwable}).
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 로그용 영어 메시지 ({@link Throwable} 이 구현).
     * @return Message | 메시지
     */
    String getMessage();

    /**
     * The message in the given language, or the English message when there is no key or no translation.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 주어진 언어의 문구. 키나 번역이 없으면 영어 메시지.
     * @param locale User's locale | 사용자 로케일
     * @return Message | 문구
     */
    default String getLocalizedMessage(Locale locale) {
        var messageKey = getMessageKey();
        if (messageKey == null) {
            return getMessage();
        }
        return messageKey.resolve(locale).orElseGet(this::getMessage);
    }

    /**
     * Finds the first user-facing message in an exception and its causes (wrapped exceptions keep the inner one's
     * message). Empty when none has a translatable key, so the caller shows a generic message instead of internal
     * details.
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 예외와 그 원인들에서 처음 나오는 사용자용 문구를 찾습니다(감싼 예외도 안쪽 예외의 문구를 씀). 번역 가능한 키가 하나도 없으면
     * 빈 값이므로, 호출 측은 내부 정보 대신 일반 문구를 보여 줍니다.
     * @param error  Exception | 예외
     * @param locale User's locale | 사용자 로케일
     * @return Message in the user's language | 사용자 언어의 문구
     */
    static Optional<String> findLocalizedMessage(Throwable error, Locale locale) {
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        for (var t = error; t != null && seen.add(t); t = t.getCause()) {
            if (t instanceof S2Localizable localizable && localizable.getMessageKey() != null) {
                var message = localizable.getMessageKey().resolve(locale);
                if (message.isPresent()) {
                    return message;
                }
            }
        }
        return Optional.empty();
    }

}
