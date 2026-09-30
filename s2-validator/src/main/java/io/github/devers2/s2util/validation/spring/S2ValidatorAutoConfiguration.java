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

import java.util.Optional;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;

import io.github.devers2.s2util.validation.S2MessageResolver;
import io.github.devers2.s2util.validation.S2Validator;

/**
 * Spring Boot auto-configuration for s2-validator: adding s2-validator to a Spring Boot application is enough.
 * <ul>
 * <li>Message keys ({@code valid.err.required}, rule keys) are looked up in Spring's {@code MessageSource} first, so
 * {@code messages.properties} (and {@code spring.messages.*}) can override validation messages. Disable with
 * {@code s2.validator.use-message-source=false}.</li>
 * <li>{@code s2.validator.bundle} and {@code s2.validator.default-locale} set the validation bundle and fallback
 * language.</li>
 * <li>The request language already follows Spring MVC's {@code LocaleResolver}: {@link S2BindValidator} reads
 * {@code LocaleContextHolder}.</li>
 * </ul>
 * <p>
 * The validator itself has no Spring dependency. This class is only loaded by Spring Boot (through
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}), and Spring Boot is a
 * compile-only dependency, so applications without Spring are unaffected. The settings are the validator's global
 * settings; they are applied when the context starts and reset when it closes.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * s2-validator 의 Spring Boot 자동 설정입니다. Spring Boot 애플리케이션에 s2-validator 를 추가하기만 하면 됩니다.
 * <ul>
 * <li>메시지 키({@code valid.err.required}, 규칙 키)를 Spring {@code MessageSource}에서 먼저 찾으므로 {@code messages.properties}
 * ({@code spring.messages.*})로 검증 메시지를 바꿀 수 있습니다. {@code s2.validator.use-message-source=false}로 끕니다.</li>
 * <li>{@code s2.validator.bundle}, {@code s2.validator.default-locale}로 검증 번들과 대체 언어를 설정합니다.</li>
 * <li>요청 언어는 이미 Spring MVC 의 {@code LocaleResolver}를 따릅니다({@link S2BindValidator}가 {@code LocaleContextHolder}를 읽음).</li>
 * </ul>
 * <p>
 * 검증기 자체는 Spring 에 의존하지 않습니다. 이 클래스는 Spring Boot 만 로드하며(AutoConfiguration.imports 경유), Spring Boot 는 컴파일
 * 전용 의존성이라 Spring 이 없는 애플리케이션에는 영향이 없습니다. 설정은 검증기의 전역 설정으로, 컨텍스트가 시작될 때 적용되고 닫힐 때
 * 초기화됩니다.
 * </p>
 *
 * @author devers2
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "s2.validator", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(S2ValidatorProperties.class)
public class S2ValidatorAutoConfiguration {

    /**
     * Applies the {@code s2.validator.*} settings to the validator for the lifetime of the context.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 컨텍스트가 살아 있는 동안 {@code s2.validator.*} 설정을 검증기에 적용합니다.
     *
     * @param properties    The bound properties | 바인딩된 설정
     * @param messageSource Spring's message source, if unique | Spring 메시지 소스 (유일할 때)
     * @return The settings lifecycle bean | 설정 생명주기 빈
     */
    @Bean
    public Settings s2ValidatorSettings(S2ValidatorProperties properties, ObjectProvider<MessageSource> messageSource) {
        return new Settings(properties, properties.isUseMessageSource() ? messageSource.getIfUnique() : null);
    }

    /**
     * Applies the settings on start and resets what it set on close, so contexts started one after another (tests) do
     * not leak settings.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 시작 시 설정을 적용하고 종료 시 자신이 설정한 것만 초기화합니다. 차례로 시작되는 컨텍스트(시험)에 설정이 남지 않습니다.
     */
    public static final class Settings implements InitializingBean, DisposableBean {

        private final S2ValidatorProperties properties;
        private final MessageSource messageSource;

        Settings(S2ValidatorProperties properties, MessageSource messageSource) {
            this.properties = properties;
            this.messageSource = messageSource;
        }

        @Override
        public void afterPropertiesSet() {
            if (properties.getBundle() != null && !properties.getBundle().isBlank()) {
                S2Validator.setValidationBundle(properties.getBundle().trim());
            }
            if (properties.getDefaultLocale() != null) {
                S2Validator.setDefaultLocale(properties.getDefaultLocale());
            }
            if (messageSource != null) {
                S2Validator.setMessageResolver(messageSourceResolver(messageSource));
            }
        }

        @Override
        public void destroy() {
            if (properties.getBundle() != null && !properties.getBundle().isBlank()) {
                S2Validator.resetValidationBundle();
            }
            if (properties.getDefaultLocale() != null) {
                S2Validator.resetDefaultLocale();
            }
            if (messageSource != null) {
                S2Validator.resetMessageResolver();
            }
        }

        /**
         * Returns the raw template without MessageFormat processing (no arguments are passed). A result equal to the key
         * is treated as missing, because {@code spring.messages.use-code-as-default-message} returns the key itself.
         */
        private static S2MessageResolver messageSourceResolver(MessageSource messageSource) {
            return (key, locale) -> {
                String message = messageSource.getMessage(key, null, null, locale);
                return message == null || message.equals(key) ? Optional.empty() : Optional.of(message);
            };
        }
    }
}
