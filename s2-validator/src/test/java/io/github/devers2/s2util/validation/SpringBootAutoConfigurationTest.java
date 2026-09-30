package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.MapPropertySource;

import io.github.devers2.s2util.validation.spring.S2ValidatorAutoConfiguration;

/**
 * The Spring Boot auto-configuration applies {@code s2.validator.*}, looks messages up in Spring's MessageSource, and
 * resets the global settings when the context closes.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * Spring Boot 자동 설정이 {@code s2.validator.*}를 적용하고, Spring MessageSource 에서 메시지를 찾으며, 컨텍스트가 닫히면 전역 설정을
 * 초기화하는지 확인합니다.
 */
public class SpringBootAutoConfigurationTest {

    @AfterEach
    void reset() {
        S2Validator.resetAll();
    }

    private static AnnotationConfigApplicationContext context(Map<String, Object> properties, StaticMessageSource messages) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        if (messages != null) {
            context.getBeanFactory().registerSingleton("messageSource", messages);
        }
        context.register(S2ValidatorAutoConfiguration.class);
        context.refresh();
        return context;
    }

    private static StaticMessageSource koreanRequiredMessage() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("valid.err.required", Locale.KOREAN, "{0|을/를} 꼭 입력하세요.");
        return messages;
    }

    private static List<String> messages(Locale locale) {
        List<String> result = new ArrayList<>();
        S2Validator.<Map<String, Object>>builder().field("name", "이름").build()
                .validate(Map.of(), e -> result.add(e.defaultMessage()), locale);
        return result;
    }

    @Test
    void messageSourceOverridesBuiltInMessagesUntilTheContextCloses() {
        try (AnnotationConfigApplicationContext ignored = context(Map.of(), koreanRequiredMessage())) {
            Assertions.assertEquals(List.of("이름을 꼭 입력하세요."), messages(Locale.KOREAN));
            // Only a Korean message is defined, so English keeps the built-in text | 한국어 메시지만 있으므로 영어는 내장 문구 유지
            Assertions.assertEquals(List.of("이름 is required."), messages(Locale.ENGLISH));
        }
        Assertions.assertEquals(List.of("이름은 필수 입력 항목입니다."), messages(Locale.KOREAN), "reset after close");
    }

    @Test
    void propertiesSetTheDefaultLocale() {
        try (AnnotationConfigApplicationContext ignored = context(Map.of("s2.validator.default-locale", "en"), null)) {
            Assertions.assertEquals(Locale.ENGLISH, S2Validator.getDefaultLocale());
        }
        Assertions.assertEquals(Locale.getDefault(), S2Validator.getDefaultLocale(), "reset after close");
    }

    @Test
    void messageSourceCanBeTurnedOffOrTheWholeAutoConfigurationDisabled() {
        try (AnnotationConfigApplicationContext ignored = context(Map.of("s2.validator.use-message-source", "false"),
                koreanRequiredMessage())) {
            Assertions.assertEquals(List.of("이름은 필수 입력 항목입니다."), messages(Locale.KOREAN));
        }
        try (AnnotationConfigApplicationContext context = context(
                Map.of("s2.validator.enabled", "false", "s2.validator.default-locale", "en"), koreanRequiredMessage())) {
            Assertions.assertEquals(0, context.getBeanNamesForType(S2ValidatorAutoConfiguration.Settings.class).length);
            Assertions.assertEquals(List.of("이름은 필수 입력 항목입니다."), messages(Locale.KOREAN));
        }
    }

    @Test
    void useCodeAsDefaultMessageIsNotMistakenForATemplate() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.setUseCodeAsDefaultMessage(true);
        try (AnnotationConfigApplicationContext ignored = context(Map.of(), messages)) {
            Assertions.assertEquals(List.of("이름은 필수 입력 항목입니다."), messages(Locale.KOREAN));
        }
    }

    @Test
    void autoConfigurationIsRegisteredForSpringBoot() {
        List<String> candidates = new ArrayList<>();
        ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).forEach(candidates::add);
        Assertions.assertTrue(candidates.contains(S2ValidatorAutoConfiguration.class.getName()), candidates.toString());
    }
}
