package io.github.devers2.s2util.validation;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import io.github.devers2.s2util.validation.spring.S2SpringValidator;

/**
 * {@link S2Validator#validateAll(Object, Locale)} 결과 객체와 Spring {@code SmartValidator} 어댑터 시험.
 */
class ValidationResultAndSpringAdapterTest {

    public static class JoinForm {
        private String name;
        private String email;
        private String nickname;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }

    public static class AdminJoinForm extends JoinForm {
    }

    private static final S2Validator<JoinForm> RULES = S2Validator.<JoinForm>builder()
            .field("name", "이름").rule(S2RuleType.REQUIRED).rule(S2RuleType.MAX_LENGTH, 3)
            .field("email", "이메일").rule(S2RuleType.EMAIL)
            .build();

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private static JoinForm form(String name, String email) {
        JoinForm form = new JoinForm();
        form.setName(name);
        form.setEmail(email);
        return form;
    }

    @Test
    @DisplayName("validateAll: 예외 없이 모든 오류를 모으고 toMap 은 필드별 메시지 목록(s2.validator.js 와 같은 형식)")
    void validateAllCollectsEveryError() {
        S2ValidationResult result = RULES.validateAll(form("홍길동님", "bad"), Locale.KOREAN);

        Assertions.assertFalse(result.isValid());
        Assertions.assertEquals(2, result.getErrors().size());
        Assertions.assertEquals(Map.of("name", List.of("이름은 최대 3자를 초과할 수 없습니다."),
                "email", List.of("이메일 형식이 올바르지 않습니다.")), result.toMap());
        Assertions.assertEquals(List.of("name", "email"), List.copyOf(result.toMap().keySet()), "검증 순서 유지");
        Assertions.assertEquals(1, result.getErrors("email").size());
        S2ValidationException e = Assertions.assertThrows(S2ValidationException.class, result::throwIfInvalid);
        Assertions.assertEquals("name", e.getFieldName());
    }

    @Test
    @DisplayName("validateAll: 유효하면 오류가 없고 throwIfInvalid 는 아무것도 하지 않는다")
    void validateAllValid() {
        S2ValidationResult result = RULES.validateAll(form("홍길동", "a@b.co"));

        Assertions.assertTrue(result.isValid());
        Assertions.assertTrue(result.toMap().isEmpty());
        Assertions.assertDoesNotThrow(result::throwIfInvalid);
    }

    @Test
    @DisplayName("S2SpringValidator: 대상 타입과 하위 타입만 지원한다")
    void adapterSupportsTypeAndSubtypes() {
        S2SpringValidator<JoinForm> adapter = S2SpringValidator.of(JoinForm.class, RULES);

        Assertions.assertTrue(adapter.supports(JoinForm.class));
        Assertions.assertTrue(adapter.supports(AdminJoinForm.class));
        Assertions.assertFalse(adapter.supports(String.class));
        Assertions.assertSame(RULES, adapter.getValidator());
    }

    @Test
    @DisplayName("S2SpringValidator: DataBinder 에서 다른 검증기(Bean Validation 자리)와 함께 같은 BindingResult 에 기록된다")
    void adapterRunsWithOtherValidatorsInDataBinder() {
        // Bean Validation 역할을 하는 다른 Spring 검증기
        Validator other = new Validator() {
            @Override
            public boolean supports(Class<?> clazz) {
                return JoinForm.class.isAssignableFrom(clazz);
            }

            @Override
            public void validate(Object target, Errors errors) {
                errors.rejectValue("nickname", "NotBlank", "nickname must not be blank");
            }
        };
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        DataBinder binder = new DataBinder(form("", "bad"), "joinForm");
        binder.addValidators(other, S2SpringValidator.of(JoinForm.class, RULES));

        binder.validate();

        BindingResult result = binder.getBindingResult();
        Assertions.assertEquals(3, result.getFieldErrorCount());
        Assertions.assertNotNull(result.getFieldError("nickname"));
        Assertions.assertEquals("이름 is required.", result.getFieldError("name").getDefaultMessage(), "요청 로케일(영어) 메시지");
        Assertions.assertEquals("valid.err.email", result.getFieldError("email").getCode());
    }

    @Test
    @DisplayName("S2SpringValidator: 대상에 없는 경로의 오류도 예외 없이 FieldError 로 기록한다")
    void adapterRecordsUnreadablePath() {
        S2Validator<JoinForm> rules = S2Validator.<JoinForm>builder().field("phone", "전화").build();
        DataBinder binder = new DataBinder(form("a", null), "joinForm");
        binder.addValidators(S2SpringValidator.of(JoinForm.class, rules));

        binder.validate();

        Assertions.assertEquals("phone", binder.getBindingResult().getFieldError().getField());
    }
}
