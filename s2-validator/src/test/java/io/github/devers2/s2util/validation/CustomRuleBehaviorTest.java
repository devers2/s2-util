package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.exception.S2RuntimeException;
import io.github.devers2.s2util.validation.S2Validator.S2ValidationError;

public class CustomRuleBehaviorTest {

    @Test
    @DisplayName("커스텀 람다는 기본적으로 빈 값(null/empty)일 때 호출되지 않고 통과한다")
    void testCustomLambdaSkipsEmptyByDefault() {
        AtomicBoolean lambdaCalled = new AtomicBoolean(false);

        Map<String, Object> target = new HashMap<>();
        target.put("nickname", null);

        List<S2ValidationError> errors = new ArrayList<>();
        boolean valid = S2Validator.of(target)
                .field("nickname", "닉네임")
                .rule((String val) -> {
                    lambdaCalled.set(true);
                    return val.startsWith("user_");
                })
                .validate(errors::add);

        Assertions.assertTrue(valid);
        Assertions.assertFalse(lambdaCalled.get(), "빈 값일 때 람다가 호출되지 않아야 함");
        Assertions.assertEquals(0, errors.size());
    }

    @Test
    @DisplayName("REQUIRED와 함께 사용된 커스텀 람다는 빈 값일 때 람다가 호출되지 않고 REQUIRED 에러만 발생한다 (NPE 방지)")
    void testRequiredWithCustomLambdaOnEmpty() {
        AtomicBoolean lambdaCalled = new AtomicBoolean(false);

        Map<String, Object> target = new HashMap<>();
        target.put("password", "");

        List<S2ValidationError> errors = new ArrayList<>();
        boolean valid = S2Validator.of(target)
                .field("password", "비밀번호")
                .rule(S2RuleType.REQUIRED)
                .rule((String val) -> {
                    lambdaCalled.set(true);
                    return val.length() >= 8;
                })
                .validate(errors::add);

        Assertions.assertFalse(valid);
        Assertions.assertFalse(lambdaCalled.get(), "빈 값일 때 람다가 호출되지 않아야 함");
        Assertions.assertEquals(1, errors.size());
        Assertions.assertEquals(S2RuleType.REQUIRED.getErrorMessageKey(), errors.get(0).errorCode());
    }

    @Test
    @DisplayName("includeEmpty()가 지정된 커스텀 람다는 빈 값이어도 실행된다")
    void testIncludeEmptyExecutesOnEmpty() {
        AtomicBoolean lambdaCalled = new AtomicBoolean(false);

        Map<String, Object> target = new HashMap<>();
        target.put("contact", null);

        List<S2ValidationError> errors = new ArrayList<>();
        boolean valid = S2Validator.of(target)
                .field("contact", "연락처")
                .rule((Object val) -> {
                    lambdaCalled.set(true);
                    return val != null; // null이면 실패
                }).includeEmpty()
                .validate(errors::add);

        Assertions.assertFalse(valid);
        Assertions.assertTrue(lambdaCalled.get(), "includeEmpty() 지정 시 빈 값이어도 람다가 실행되어야 함");
        Assertions.assertEquals(1, errors.size());
    }

    @Test
    @DisplayName("일반 내장 규칙 뒤에 includeEmpty()를 호출하면 IllegalStateException이 발생한다")
    void testIncludeEmptyOnStandardRuleThrowsException() {
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> {
            S2Validator.<Map<String, Object>>builder()
                    .field("email", "이메일")
                    .rule(S2RuleType.EMAIL)
                    .includeEmpty()
                    .build();
        });

        Assertions.assertTrue(ex.getMessage().contains("includeEmpty()"));
    }

    @Test
    @DisplayName("커스텀 람다 내부에서 RuntimeException 발생 시 필드명을 포함한 S2RuntimeException으로 감싸서 던진다")
    void testLambdaExceptionWrappedInS2RuntimeException() {
        Map<String, Object> target = new HashMap<>();
        target.put("code", "ABC");

        S2RuntimeException ex = Assertions.assertThrows(S2RuntimeException.class, () -> {
            S2Validator.of(target)
                    .field("code", "코드")
                    .rule((String val) -> {
                        throw new NullPointerException("내부 널포인터");
                    })
                    .validate();
        });

        Assertions.assertTrue(ex.getMessage().contains("code"), "예외 메시지에 필드명이 포함되어야 함");
        Assertions.assertInstanceOf(NullPointerException.class, ex.getCause(), "원인 예외가 NullPointerException이어야 함");
    }

    @Test
    @DisplayName("Builder 모드 및 Check 모드에서도 includeEmpty()가 정상 동작한다")
    void testBuilderAndCheckModesWithIncludeEmpty() {
        // Builder 모드
        S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
                .field("data", "데이터")
                .rule((Object val) -> val != null).includeEmpty()
                .build();

        Map<String, Object> target = new HashMap<>();
        target.put("data", null);
        Assertions.assertFalse(validator.validate(target, err -> {}));

        // Check 모드 (라벨 없음: Boolean 반환 모드)
        boolean checkResult = S2Validator.check(null)
                .rule((Object val) -> val != null).includeEmpty()
                .validate();
        Assertions.assertFalse(checkResult);

        // Check 모드 (라벨 있음: S2RuntimeException 예외 발생 모드)
        Assertions.assertThrows(S2RuntimeException.class, () -> {
            S2Validator.check(null, "테스트")
                    .rule((Object val) -> val != null).includeEmpty()
                    .validate();
        });
    }
}
