package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.validation.S2Validator.S2ValidationError;

/**
 * 2026-09-14 도입 검토 보고서에서 지적된 주요 결함 및 호환성 개선 사항을 검증하는 테스트 클래스.
 * <p>
 * 1. 비문자열 필드 키 (Integer, Enum 등) 사용 시 ClassCastException 방지
 * 2. 폼 바인딩 문자열 숫자("25") 및 문자열 기준값("19")의 MIN_VALUE / MAX_VALUE 비교 지원
 * 3. 교차 필드 검증 규칙(EQUALS_FIELD, DATE_AFTER, DATE_BEFORE)의 에러 메시지에 영문 필드명 대신 한글 라벨 노출
 * 4. Map 바인딩 체크박스 값("true", "on", "false", "off")의 ASSERT_TRUE / ASSERT_FALSE 정확한 판정
 * </p>
 */
public class DefectFixParityTest {

    enum TestFieldKey {
        USER_ID, USER_AGE
    }

    @Test
    @DisplayName("비문자열 필드 키(Integer, Enum)를 라벨 없이 사용해도 ClassCastException이 발생하지 않는다")
    void testNonStringFieldKeyWithoutLabelDoesNotThrowException() {
        Map<Object, Object> data = new HashMap<>();
        data.put(1001, "admin");
        data.put(TestFieldKey.USER_ID, "tester");

        List<S2ValidationError> errors = new ArrayList<>();

        // Integer 키
        boolean valid1 = S2Validator.of(data)
                .field(1001) // 라벨 생략 -> String.valueOf(name)인 "1001"이 라벨이 됨
                .rule(S2RuleType.REQUIRED)
                .validate(errors::add);

        Assertions.assertTrue(valid1);
        Assertions.assertEquals(0, errors.size());

        // Enum 키
        boolean valid2 = S2Validator.of(data)
                .field(TestFieldKey.USER_ID) // 라벨 생략 -> "USER_ID"가 라벨이 됨
                .rule(S2RuleType.REQUIRED)
                .validate(errors::add);

        Assertions.assertTrue(valid2);
        Assertions.assertEquals(0, errors.size());

        // 에러 발생 시 라벨 확인
        data.put(2002, "");
        boolean valid3 = S2Validator.of(data)
                .field(2002)
                .rule(S2RuleType.REQUIRED)
                .validate(errors::add, Locale.KOREAN);

        Assertions.assertFalse(valid3);
        Assertions.assertEquals(1, errors.size());
        Assertions.assertTrue(errors.get(0).defaultMessage().contains("2002"));
    }

    @Test
    @DisplayName("Map 데이터의 문자열 숫자('25') 및 문자열 기준값('19')이 MIN_VALUE / MAX_VALUE에서 정상 비교된다")
    void testNumericStringAndCriterionForMinValueAndMaxValue() {
        // 폼/HTTP 요청에서 String 형태로 들어온 숫자 데이터
        Map<String, Object> formMap = new HashMap<>();
        formMap.put("age", "25");
        formMap.put("score", "85");

        List<S2ValidationError> errors = new ArrayList<>();
        boolean valid = S2Validator.of(formMap)
                .field("age", "나이").rule(S2RuleType.MIN_VALUE, 19) // 기준값 Number(19) vs 값 String("25")
                .field("score", "점수").rule(S2RuleType.MAX_VALUE, "100") // 기준값 String("100") vs 값 String("85")
                .validate(errors::add);

        Assertions.assertTrue(valid, "문자열 숫자 '25' >= 19 및 '85' <= '100' 검증 통과해야 함");
        Assertions.assertEquals(0, errors.size());

        // 실패 케이스 검증
        formMap.put("age", "16");
        boolean invalid = S2Validator.of(formMap)
                .field("age", "나이").rule(S2RuleType.MIN_VALUE, 19)
                .validate(errors::add);

        Assertions.assertFalse(invalid, "문자열 숫자 '16' < 19 실패해야 함");
        Assertions.assertEquals(1, errors.size());
    }

    @Test
    @DisplayName("교차 필드 검증(EQUALS_FIELD, DATE_AFTER) 에러 메시지가 영문 필드명 대신 대상 필드의 한글 라벨을 노출한다")
    void testCrossFieldErrorMessageUsesTargetFieldLabel() {
        Map<String, Object> data = new HashMap<>();
        data.put("password", "Secret123!");
        data.put("confirmPassword", "DifferentPassword!");
        data.put("startDate", "2024-01-10");
        data.put("endDate", "2024-01-01"); // 종료일이 시작일보다 이전

        S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
                .field("password", "비밀번호").rule(S2RuleType.REQUIRED)
                .field("confirmPassword", "비밀번호 확인").rule(S2RuleType.EQUALS_FIELD, "password")
                .field("startDate", "시작일").rule(S2RuleType.REQUIRED)
                .field("endDate", "종료일").rule(S2RuleType.DATE_AFTER, "startDate")
                .build();

        List<S2ValidationError> errors = new ArrayList<>();
        validator.validate(data, errors::add, Locale.KOREAN);

        Assertions.assertEquals(2, errors.size());

        // EQUALS_FIELD: "[password]" 대신 "비밀번호" 라벨이 치환되었는지 확인
        S2ValidationError pwError = errors.stream()
                .filter(e -> "confirmPassword".equals(e.fieldName()))
                .findFirst()
                .orElseThrow();
        Assertions.assertTrue(pwError.defaultMessage().contains("비밀번호"),
                "에러 메시지에 영문 필드명이 아닌 대상 필드 라벨 '비밀번호'가 포함되어야 함: " + pwError.defaultMessage());
        Assertions.assertFalse(pwError.defaultMessage().contains("[password]"),
                "에러 메시지에 내부 영문 필드명 '[password]'가 노출되어서는 안 됨");

        // DATE_AFTER: "startDate" 대신 "시작일" 라벨이 치환되었는지 확인
        S2ValidationError dateError = errors.stream()
                .filter(e -> "endDate".equals(e.fieldName()))
                .findFirst()
                .orElseThrow();
        Assertions.assertTrue(dateError.defaultMessage().contains("시작일"),
                "에러 메시지에 대상 필드 라벨 '시작일'이 포함되어야 함: " + dateError.defaultMessage());
        Assertions.assertFalse(dateError.defaultMessage().contains("startDate"),
                "에러 메시지에 내부 영문 필드명 'startDate'가 노출되어서는 안 됨");

        // JSON 생성 시에도 메시지에 한글 라벨이 치환되었는지 확인
        String json = S2ValidatorFactory.getRulesJson(validator, Locale.KOREAN);
        Assertions.assertTrue(json.contains("비밀번호"), "규칙 JSON 메시지에도 한글 라벨이 반영되어야 함");
        Assertions.assertFalse(json.contains("[password]"), "규칙 JSON 메시지에 영문 필드명이 노출되면 안 됨");
    }

    @Test
    @DisplayName("Map 바인딩의 문자열 불리언('true', 'on', 'false', 'off')이 ASSERT_TRUE / ASSERT_FALSE에서 올바르게 판정된다")
    void testMapCheckboxStringBooleans() {
        // 체크된 상태 (true, on)
        Map<String, Object> checkedMap = new HashMap<>();
        checkedMap.put("agree", "true");
        checkedMap.put("agreeOn", "on");

        boolean validTrue = S2Validator.of(checkedMap)
                .field("agree", "약관 동의").rule(S2RuleType.ASSERT_TRUE)
                .field("agreeOn", "이용 동의").rule(S2RuleType.ASSERT_TRUE)
                .validate();

        Assertions.assertTrue(validTrue, "문자열 'true'와 'on'은 ASSERT_TRUE 통과해야 함");

        // 미체크/해제 상태 (false, off)
        Map<String, Object> uncheckedMap = new HashMap<>();
        uncheckedMap.put("optOut", "false");
        uncheckedMap.put("optOff", "off");

        boolean validFalse = S2Validator.of(uncheckedMap)
                .field("optOut", "수신 거부").rule(S2RuleType.ASSERT_FALSE)
                .field("optOff", "광고 해제").rule(S2RuleType.ASSERT_FALSE)
                .validate();

        Assertions.assertTrue(validFalse, "문자열 'false'와 'off'는 ASSERT_FALSE 통과해야 함");

        // 거꾸로 값 검증 (false를 ASSERT_TRUE에 전달하면 실패)
        Map<String, Object> badMap = new HashMap<>();
        badMap.put("agree", "false");
        boolean invalid = S2Validator.of(badMap)
                .field("agree", "약관 동의").rule(S2RuleType.ASSERT_TRUE)
                .validate(err -> {});

        Assertions.assertFalse(invalid, "문자열 'false'는 ASSERT_TRUE 실패해야 함");
    }
}
