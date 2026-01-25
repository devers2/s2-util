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

import java.util.Locale;
import java.util.Optional;

import io.github.devers2.s2util.core.S2Cache;
import io.github.devers2.s2util.core.S2StringUtil;
import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * Centralized multi-language message manager for the S2Util library.
 * <p>
 * This class provides a high-level API for retrieving localized strings from
 * {@link java.util.ResourceBundle}. It is designed for efficiency and ease of use:
 * <ul>
 * <li><b>Performance:</b> Leverages {@link S2Cache} to cache both found and
 * not-found (Optional.empty()) resources, preventing repeated disk/classpath IO.</li>
 * <li><b>Intelligent Formatting:</b> Integrates with {@link S2StringUtil#formatMessage(String, Object...)}
 * to support automatic Korean josa (postposition) handling in error messages.</li>
 * <li><b>Extensibility:</b> Allows setting a {@code defaultBasename} to synchronize
 * with host application settings (e.g., {@code spring.messages.basename}).</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리의 다국어 메시지 처리를 담당하는 중앙 관리 클래스입니다.
 * <p>
 * {@link java.util.ResourceBundle}로부터 로케일별 메시지를 조회하고 포맷팅하는 고수준 API를 제공합니다.
 * <ul>
 * <li><b>성능 최적화:</b> {@link S2Cache}를 활용하여 리소스 검색 결과를 캐싱함으로써 반복적인
 * 클래스패스 탐색 부하를 최소화합니다. (부재 리소스에 대한 중복 탐색 방지 포함)</li>
 * <li><b>지능형 포맷팅:</b> {@link S2StringUtil#formatMessage(String, Object...)}와 연동되어
 * 한국어 특유의 조사('은/는', '이/가' 등)를 상황에 맞게 자동 교정 처리합니다.</li>
 * <li><b>확장성:</b> {@code defaultBasename} 설정을 통해 애플리케이션의 기존 메시지 설정과
 * 쉽게 동기화하여 사용할 수 있습니다.</li>
 * </ul>
 * </p>
 *
 * @apiNote
 *
 *          <pre>{@code
 * // 초기화 (애플리케이션 시작 시 한 번만)
 * S2ResourceBundle.setDefaultBasename("messages/myapp");
 *
 * // 사용
 * String msg = S2ResourceBundle.getMessage("valid.err.required", Locale.KOREA, "이름");
 * // → "이름은(는) 필수 입력 항목입니다."
 * }</pre>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public final class S2ResourceBundle {

    private static final S2Logger logger = S2LogManager.getLogger(S2ResourceBundle.class);

    /** 기본 basename (초기값: null → 사용 전 setDefaultBasename 필수) */
    private static String defaultBasename;

    private S2ResourceBundle() {
        // Prevent instantiation
    }

    /**
     * Sets the default basename for localized resources.
     * <p>
     * Every project using this common library should call this once at application startup.
     * For Spring Boot projects, it is recommended to align this with the
     * {@code spring.messages.basename} property.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 기본 basename을 설정합니다.
     * <p>
     * 공통 JAR를 사용하는 각 프로젝트에서 애플리케이션 시작 시 한 번 호출해야 합니다.
     * Spring Boot의 경우 {@code spring.messages.basename}과 일치시키는 것이 좋습니다.
     * </p>
     *
     * @param basename Base name of the property files (e.g., "messages/validation") | 프로퍼티 파일의 기본 이름 (예: "messages/validation")
     * @throws IllegalArgumentException If basename is null or blank | basename이 null 또는 blank일 경우
     */
    public static void setDefaultBasename(String basename) {
        if (basename == null || basename.isBlank()) {
            throw new IllegalArgumentException("basename cannot be null or blank.");
        }
        if (defaultBasename != null && logger.isInfoEnabled()) {
            if (S2Util.isKorean()) {
                logger.info("S2ResourceBundle 기본 베이스네임 변경됨: {} → {}", defaultBasename, basename);
            } else {
                logger.info("S2ResourceBundle default basename changed: {} → {}", defaultBasename, basename);
            }
        }
        defaultBasename = basename;
    }

    /**
     * Returns the currently configured default basename.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 설정된 기본 basename을 반환합니다.
     *
     * @return The currently set basename (or null if not set) | 현재 설정된 basename (설정되지 않았으면 null)
     */
    public static String getDefaultBasename() {
        return defaultBasename;
    }

    /**
     * Retrieves the raw message from the resource bundle (uses default basename).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 리소스 번들에 정의된 원본 메시지를 조회합니다. (기본 basename 사용)
     *
     * @param key    The message key to look up | 조회할 메시지 키
     * @param locale Target locale | 대상 로케일 (null일 경우 Locale.getDefault() 사용)
     * @return An Optional containing the raw template | 원본 템플릿을 담은 Optional
     */
    public static Optional<String> getMessage(String key, Locale locale) {
        return getMessage(null, key, locale);
    }

    /**
     * Retrieves the raw message from a specific resource bundle.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 특정 리소스 번들에 정의된 원본 메시지를 조회합니다. (명시적 basename 사용)
     *
     * @param basename Base path of the resource bundle | 리소스 번들 기본 경로 (null일 경우 defaultBasename 사용)
     * @param key      The message key to look up | 조회할 메시지 키
     * @param locale   Target locale | 대상 로케일 (null일 경우 Locale.getDefault() 사용)
     * @return An Optional containing the raw template | 원본 템플릿을 담은 Optional
     */
    public static Optional<String> getMessage(String basename, String key, Locale locale) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        // 사용할 basename 결정
        String targetBasename = basename != null && !basename.isBlank() ? basename : defaultBasename;
        if (targetBasename == null || targetBasename.isBlank()) {
            return Optional.empty();
        }

        // 로케일 설정
        Locale targetLocale = (locale != null) ? locale : Locale.getDefault();

        return S2Cache.getResourceBundle(targetBasename, targetLocale)
                .filter(bundle -> bundle.containsKey(key))
                .map(bundle -> bundle.getString(key));
    }

    /**
     * Resolves a localized message and substitutes parameters.
     * <p>
     * This method combines {@link #getMessage(String, Locale)} with the intelligent
     * formatting engine of {@link S2StringUtil}. It supports indexed placeholders
     * like {@code {0}}, {@code {1}}, and special Korean postposition modifiers
     * like {@code {0|은/는}}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 리소스 번들에서 메시지를 조회하고 인자를 치환하여 반환합니다.
     * <p>
     * 조회된 템플릿의 {@code {0}}, {@code {1}} 등 인덱스 기반 치환자를 실제 값으로 변경하며,
     * {@code {0|은/는}}과 같은 특수 구문을 통해 한국어 조사를 자동으로 교정 처리하는
     * {@link S2StringUtil}의 지능형 포맷팅 기능을 제공합니다.
     * </p>
     *
     * @param key    The message key to look up | 조회할 메시지 키
     * @param locale The target locale (defaults to {@link Locale#getDefault()} if null) | 대상 로케일 (null일 경우 Locale.getDefault() 사용)
     * @param args   Arguments to fill into the message template | 메시지 템플릿 내 치환 대상 인자 목록
     * @return The formatted message, or {@link Optional#empty()} if not found | 포맷팅된 메시지 Optional, 발견되지 않은 경우 Optional.empty()
     */
    public static Optional<String> getMessage(String key, Locale locale, Object... args) {
        return getMessage(null, key, locale, args);
    }

    /**
     * Resolves a localized message and substitutes parameters (explicit basename).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 리소스 번들에서 메시지를 조회하고 인자를 치환하여 반환합니다. (명시적 basename 사용)
     * <p>
     * 내부적으로 원본 템플릿을 조회한 후, {@link S2StringUtil#formatMessage(String, Object...)}를 통해
     * {0}, {1} 등의 치환자를 실제 값으로 변환합니다.
     * </p>
     *
     * @param basename Base path of the resource bundle | 리소스 번들 기본 경로 (null일 경우 defaultBasename 사용)
     * @param key      The message key to look up | 조회할 메시지 키
     * @param locale   Target locale | 대상 로케일 (null일 경우 Locale.getDefault() 사용)
     * @param args     Arguments to fill into the message template | 메시지 템플릿 내 치환 대상 인자 목록
     * @return Formatted message Optional | 인자가 치환된 메시지 Optional (메시지가 없으면 Optional.empty())
     */
    public static Optional<String> getMessage(String basename, String key, Locale locale, Object... args) {
        return getMessage(basename, key, locale)
                .map(template -> {
                    if (args == null || args.length == 0) {
                        return template;
                    }
                    return S2StringUtil.formatMessage(template, args);
                });
    }

    /**
     * Resolves a localized message or returns the key if not found (default basename).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 리소스 번들에서 메시지를 조회하고 실패할 경우 기본값으로 제공된 키를 반환합니다. (기본 basename 사용)
     * <p>
     * 결과가 없을 경우 입력받은 key를 그대로 반환함으로써 화면에 최소한의 식별자가 노출되도록 합니다.
     * </p>
     *
     * @param key    The message key to look up | 메시지 키
     * @param locale Target locale | 로케일
     * @param args   Arguments to fill | 치환 인자
     * @return The formatted message, or the key itself if lookup fails | 인지가 치환된 메시지 또는 메시지 키
     */
    public static String getMessageOrDefault(String key, Locale locale, Object... args) {
        return getMessage(key, locale, args).orElse(key);
    }

    /**
     * Resolves a localized message or returns the key if not found (explicit basename).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 리소스 번들에서 메시지를 조회하고 실패할 경우 기본값으로 제공된 키를 반환합니다. (명시적 basename 사용)
     *
     * @param basename Base path of the resource bundle | 리소스 번들 기본 경로
     * @param key      The message key to look up | 조회할 메시지 키
     * @param locale   Target locale | 대상 로케일
     * @param args     Arguments to fill | 메시지 템플릿 내 치환 대상 인자 목록
     * @return The formatted message, or the key itself if lookup fails | 인자가 치환된 메시지 또는 메시지 키
     */
    public static String getMessageOrDefault(String basename, String key, Locale locale, Object... args) {
        return getMessage(basename, key, locale, args).orElse(key);
    }

}
