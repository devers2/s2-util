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
        messages.addMessage("valid.err.required", Locale.KOREAN, "{0|을} 꼭 입력하세요.");
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
    void closingOneContextKeepsTheSettingsOfAnotherLiveContext() {
        // 한 JVM 에 컨텍스트가 여럿(시험 컨텍스트 캐시·부모/자식)일 때, 먼저 만든 컨텍스트를 닫아도
        // 나중 컨텍스트가 설정한 전역 값은 지워지지 않아야 한다
        AnnotationConfigApplicationContext first = context(Map.of("s2.validator.default-locale", "en",
                "s2.validator.bundle", "first_messages"), null);
        AnnotationConfigApplicationContext second = context(Map.of("s2.validator.default-locale", "ja",
                "s2.validator.bundle", "second_messages"), koreanRequiredMessage());

        first.close();
        Assertions.assertEquals(Locale.JAPANESE, S2Validator.getDefaultLocale());
        Assertions.assertEquals("second_messages", S2Validator.getValidationBundle());
        Assertions.assertEquals(List.of("이름을 꼭 입력하세요."), messages(Locale.KOREAN));

        second.close();
        Assertions.assertEquals(Locale.getDefault(), S2Validator.getDefaultLocale(), "no live context → defaults");
        Assertions.assertNull(S2Validator.getValidationBundle());
        Assertions.assertEquals(List.of("이름은 필수 입력 항목입니다."), messages(Locale.KOREAN));
    }

    @Test
    void contextDoesNotTouchSettingsItDoesNotConfigure() {
        // 앱이 직접 설정한 번들은 번들을 설정하지 않는 컨텍스트가 열고 닫혀도 그대로
        S2Validator.setValidationBundle("app_messages");
        try (AnnotationConfigApplicationContext ignored = context(Map.of("s2.validator.default-locale", "en"), null)) {
            Assertions.assertEquals("app_messages", S2Validator.getValidationBundle());
        }
        Assertions.assertEquals("app_messages", S2Validator.getValidationBundle());
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
