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
package io.github.devers2.s2util.validation.spring;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot properties for s2-validator ({@code s2.validator.*}).
 *
 * <pre>{@code
 * s2:
 *   validator:
 *     bundle: messages/validation   # optional validation message bundle (ResourceBundle base name)
 *     default-locale: ko            # fallback language for messages
 *     use-message-source: true      # look message keys up in Spring's MessageSource first (default true)
 *     enabled: true                 # set false to skip the auto-configuration
 * }</pre>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * s2-validator 의 Spring Boot 설정({@code s2.validator.*})입니다. {@code bundle}은 검증 메시지 번들(ResourceBundle 기본 이름),
 * {@code default-locale}은 메시지 대체 언어, {@code use-message-source}는 Spring {@code MessageSource}를 먼저 조회할지 여부(기본 true),
 * {@code enabled}는 자동 설정 사용 여부입니다.
 *
 * @author devers2
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "s2.validator")
public class S2ValidatorProperties {

    /** Whether the auto-configuration applies | 자동 설정 적용 여부 */
    private boolean enabled = true;

    /** Validation message bundle base name, such as {@code messages/validation} | 검증 메시지 번들 기본 이름 */
    private String bundle;

    /** Fallback language for messages; the JVM default when unset | 메시지 대체 언어 (없으면 JVM 기본값) */
    private Locale defaultLocale;

    /** Look message keys up in Spring's MessageSource before the bundle | 번들보다 먼저 Spring MessageSource 에서 메시지 키 조회 */
    private boolean useMessageSource = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public boolean isUseMessageSource() {
        return useMessageSource;
    }

    public void setUseMessageSource(boolean useMessageSource) {
        this.useMessageSource = useMessageSource;
    }
}
