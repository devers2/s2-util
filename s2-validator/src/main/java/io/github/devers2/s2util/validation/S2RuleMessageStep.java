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
 * Core interface for localized error message customization.
 * <p>
 * This interface is implemented by validation rules ({@link S2Rule} and {@link S2Field.S2CustomRule})
 * to allow users to bind custom error templates directly to a rule definition.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 다국어 에러 메시지 커스터마이징을 위한 핵심 인터페이스입니다.
 * <p>
 * 모든 검증 규칙({@link S2Rule}, {@link S2Field.S2CustomRule})은 이 인터페이스를 구현하며,
 * 사용자가 검증 정의 직후 {@code .ko("메시지")}, {@code .en("Message")}와 같이 직관적으로
 * 에러 템플릿을 바인딩할 수 있게 합니다.
 * </p>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public interface S2RuleMessageStep {

    /**
     * Stores a message template for a specific locale.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 특정 로케일에 대한 메시지 템플릿을 저장합니다.
     *
     * @param locale   Target locale | 대상 로케일
     * @param template Message template | 메시지 템플릿
     * @return This step object for chaining | 체이닝을 위한 현재 객체
     */
    default S2RuleMessageStep message(Locale locale, String template) {
        if (locale == null)
            return this;
        // getLanguage()를 사용하여 국가 정보를 버리고 언어만 취함
        return storeMessage(locale.getLanguage(), template);
    }

    /**
     * Saves a message template for a specific language code to the underlying storage.
     * <p>
     * This method is designed for internal use. External callers should prefer
     * {@link #message(Locale, String)}, {@link #ko(String)}, or {@link #en(String)}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 특정 언어 코드({@code ko}, {@code en} 등)에 대한 메시지 템플릿을 실제 저장소에 저장합니다.
     *
     * @param language Language code (e.g. "ko", "en") | 언어 코드 (예: "ko", "en")
     * @param template Message template | 메시지 템플릿
     * @return This step object for chaining | 체이닝을 위한 현재 객체
     */
    S2RuleMessageStep storeMessage(String language, String template);

    /**
     * Sets a Korean (ko) message template.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 한국어(ko) 메시지 템플릿을 설정합니다.
     *
     * @param template Korean message template | 한국어 메시지 템플릿
     * @return This step object for chaining | 현재 객체
     */
    default S2RuleMessageStep ko(String template) {
        return message(Locale.KOREAN, template);
    }

    /**
     * Sets an English (en) message template.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 영어(en) 메시지 템플릿을 설정합니다.
     *
     * @param template English message template | 영어 메시지 템플릿
     * @return This step object for chaining | 현재 객체
     */
    default S2RuleMessageStep en(String template) {
        return message(Locale.ENGLISH, template);
    }
}
