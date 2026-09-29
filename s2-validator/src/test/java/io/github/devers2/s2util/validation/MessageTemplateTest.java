package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.validation.S2Validator.S2ValidationError;

/**
 * Message template API: {@code message(template)}, {@code message(template, locale)}, {@code ko}, {@code en}.
 * <p>
 * Lookup order: request language → language-independent default ({@code message(template)}) → default locale's
 * language → built-in message.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 메시지 템플릿 API 를 확인합니다. 조회 순서: 요청 언어 → 언어 구분 없는 기본값({@code message(template)}) → 기본 로케일 언어 → 내장 메시지.
 */
public class MessageTemplateTest {

    @AfterEach
    void reset() {
        S2Validator.resetAll();
    }

    private static String firstMessage(S2Validator<Map<String, Object>> validator, Locale locale) {
        List<S2ValidationError> errors = new ArrayList<>();
        validator.validate(Map.of("name", ""), errors::add, locale);
        return errors.get(0).defaultMessage();
    }

    @Test
    void defaultTemplateAppliesToEveryLanguage() {
        S2Validator<Map<String, Object>> v = S2Validator.<Map<String, Object>>builder()
                .field("name", "이름").rule(S2RuleType.REQUIRED).message("{0|은/는} 꼭 입력해 주세요.")
                .build();

        Assertions.assertEquals("이름은 꼭 입력해 주세요.", firstMessage(v, Locale.KOREAN));
        Assertions.assertEquals("이름은 꼭 입력해 주세요.", firstMessage(v, Locale.ENGLISH));
        Assertions.assertEquals("이름은 꼭 입력해 주세요.", firstMessage(v, Locale.JAPANESE));
    }

    @Test
    void languageSpecificTemplateOverridesTheDefault() {
        S2Validator<Map<String, Object>> v = S2Validator.<Map<String, Object>>builder()
                .field("name", "Name").rule(S2RuleType.REQUIRED)
                .message("기본 문구")
                .en("{0} is required.")
                .message("{0}は必須です。", Locale.JAPAN) // country part is ignored
                .build();

        Assertions.assertEquals("Name is required.", firstMessage(v, Locale.US));
        Assertions.assertEquals("Nameは必須です。", firstMessage(v, Locale.JAPANESE));
        Assertions.assertEquals("기본 문구", firstMessage(v, Locale.KOREAN));
    }

    @Test
    void defaultTemplateComesBeforeTheDefaultLocaleTemplate() {
        S2Validator.setDefaultLocale(Locale.KOREAN);
        S2Validator<Map<String, Object>> v = S2Validator.<Map<String, Object>>builder()
                .field("name", "이름").rule(S2RuleType.REQUIRED)
                .ko("한국어 문구")
                .message("기본 문구")
                .build();

        Assertions.assertEquals("기본 문구", firstMessage(v, Locale.FRENCH));
        Assertions.assertEquals("한국어 문구", firstMessage(v, Locale.KOREAN));
    }

    @Test
    void withoutAnyTemplateTheBuiltInMessageIsUsed() {
        S2Validator<Map<String, Object>> v = S2Validator.<Map<String, Object>>builder()
                .field("name", "Name").rule(S2RuleType.REQUIRED)
                .build();

        Assertions.assertEquals("Name is required.", firstMessage(v, Locale.ENGLISH));
    }

    @Test
    void customLambdaRuleSupportsTheDefaultTemplate() {
        S2Validator<Map<String, Object>> v = S2Validator.<Map<String, Object>>builder()
                .field("code", "코드").rule((String s) -> s.startsWith("ADM-")).message("{0|은/는} ADM- 로 시작해야 합니다.")
                .build();

        List<S2ValidationError> errors = new ArrayList<>();
        v.validate(Map.of("code", "USR-1"), errors::add, Locale.ENGLISH);
        Assertions.assertEquals("코드는 ADM- 로 시작해야 합니다.", errors.get(0).defaultMessage());
    }

    @Test
    void messageChainsForImmediateAndSingleValueModes() {
        List<S2ValidationError> errors = new ArrayList<>();
        S2Validator.of(Map.of("name", ""))
                .field("name", "이름").rule(S2RuleType.REQUIRED).message("기본 문구")
                .validate(errors::add, Locale.ENGLISH);
        Assertions.assertEquals("기본 문구", errors.get(0).defaultMessage());

        S2ValidationException ex = Assertions.assertThrows(S2ValidationException.class,
                () -> S2Validator.check("", "이름").rule(S2RuleType.REQUIRED).message("값 문구").validate(Locale.ENGLISH));
        Assertions.assertEquals("값 문구", ex.getMessage());
    }

    @Test
    void rulesJsonUsesTheResolvedTemplate() {
        S2Validator<Map<String, Object>> v = S2Validator.<Map<String, Object>>builder()
                .field("name", "이름").rule(S2RuleType.REQUIRED).message("기본 문구").en("English text")
                .build();

        Assertions.assertTrue(v.getRulesJson(Locale.ENGLISH).contains("English text"));
        Assertions.assertTrue(v.getRulesJson(Locale.KOREAN).contains("기본 문구"));
    }
}
