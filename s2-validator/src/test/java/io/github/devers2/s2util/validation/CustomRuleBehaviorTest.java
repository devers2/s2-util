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
    @DisplayName("람다 실행 오류는 S2RuleExecutionException, 입력 오류는 S2ValidationException 으로 구분되고 원인 문구는 메시지에 노출되지 않는다")
    void testLambdaErrorAndValidationFailureHaveDistinctTypes() {
        S2RuleExecutionException ruleEx = Assertions.assertThrows(S2RuleExecutionException.class, () -> {
            S2Validator.check("ABC", "코드")
                    .rule((String val) -> {
                        throw new NullPointerException("secret-internal-detail");
                    })
                    .validate();
        });
        Assertions.assertFalse(ruleEx.getMessage().contains("secret-internal-detail"), "원인 예외 문구가 메시지에 노출되면 안 됨");
        Assertions.assertInstanceOf(NullPointerException.class, ruleEx.getCause());

        S2ValidationException valEx = Assertions.assertThrows(S2ValidationException.class, () -> {
            S2Validator.check("USR-1", "코드")
                    .rule((String val) -> val.startsWith("ADM-"))
                    .validate();
        });
        Assertions.assertNull(valEx.getCause());
        Assertions.assertFalse(S2RuleExecutionException.class.isInstance(valEx));
    }

    @Test
    @DisplayName("와일드카드 행의 람다 실행 오류는 선언 이름(items[].qty)이 아닌 실제 행 경로(items[1].qty)를 담는다")
    void testWildcardLambdaErrorUsesConcreteRowPath() {
        Map<String, Object> row0 = new HashMap<>();
        row0.put("qty", "1");
        Map<String, Object> row1 = new HashMap<>();
        row1.put("qty", "boom");
        Map<String, Object> target = new HashMap<>();
        target.put("items", List.of(row0, row1));

        S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
                .field("items[].qty", "수량")
                .rule((String val) -> Integer.parseInt(val) > 0)
                .build();

        S2RuleExecutionException ex = Assertions.assertThrows(S2RuleExecutionException.class,
                () -> validator.validate(target, err -> {}));
        Assertions.assertEquals("items[1].qty", ex.getFieldName());
        Assertions.assertInstanceOf(NumberFormatException.class, ex.getCause());
    }

    @Test
    @DisplayName("예외 모드의 검증 실패는 필드 경로를 S2ValidationException 에 담는다")
    void testValidationExceptionCarriesFieldName() {
        Map<String, Object> target = new HashMap<>();
        target.put("documentId", null);

        S2ValidationException ex = Assertions.assertThrows(S2ValidationException.class, () -> {
            S2Validator.of(target)
                    .field("documentId", "문서 ID")
                    .validate();
        });
        Assertions.assertEquals("documentId", ex.getFieldName());
        Assertions.assertEquals(S2RuleType.REQUIRED.getErrorMessageKey(), ex.getErrorCode());
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

    @Test
    @DisplayName("check(value, label) 예외는 내부 키 \"value\" 대신 라벨을 필드 이름으로 담는다")
    void testCheckWithLabelReportsLabelAsFieldName() {
        S2ValidationException valEx = Assertions.assertThrows(S2ValidationException.class,
                () -> S2Validator.check("", "이름").rule(S2RuleType.REQUIRED).validate());
        Assertions.assertEquals("이름", valEx.getFieldName());
        Assertions.assertEquals(S2RuleType.REQUIRED.getErrorMessageKey(), valEx.getErrorCode());
        Assertions.assertTrue(valEx.getMessage().contains("이름"), valEx.getMessage());

        S2RuleExecutionException ruleEx = Assertions.assertThrows(S2RuleExecutionException.class,
                () -> S2Validator.check("x", "코드").rule((String v) -> {
                    throw new IllegalStateException("boom");
                }).validate());
        Assertions.assertEquals("코드", ruleEx.getFieldName());
        Assertions.assertTrue(ruleEx.getMessage().contains("코드"), ruleEx.getMessage());
        Assertions.assertInstanceOf(IllegalStateException.class, ruleEx.getCause());
    }
}
