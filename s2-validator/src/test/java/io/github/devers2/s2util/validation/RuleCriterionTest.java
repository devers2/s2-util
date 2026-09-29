package io.github.devers2.s2util.validation;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Creation-time validation of numeric rule criteria, platform-independent byte counting, and DATE value handling.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 숫자 규칙 기준값의 생성 시점 검증, 플랫폼 문자셋에 무관한 바이트 계산, DATE 값 처리를 확인합니다.
 */
public class RuleCriterionTest {

    @Test
    void nonFiniteOrNonNumericValueCriteriaAreRejectedAtCreation() {
        for (Object bad : new Object[] { Double.NaN, Double.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, "abc", "1,000", "NaN" }) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> new S2Rule(S2RuleType.MIN_VALUE, bad), String.valueOf(bad));
            Assertions.assertThrows(IllegalArgumentException.class, () -> new S2Rule(S2RuleType.MAX_VALUE, bad), String.valueOf(bad));
        }
    }

    @Test
    void finiteValueCriteriaAreAccepted() {
        for (Object ok : new Object[] { 19, 19L, 2.5, "19", " 19 ", "-3.5e2", new java.math.BigDecimal("100.25") }) {
            Assertions.assertDoesNotThrow(() -> new S2Rule(S2RuleType.MIN_VALUE, ok), String.valueOf(ok));
        }
    }

    @Test
    void nonIntegerLengthAndByteCriteriaAreRejectedAtCreation() {
        for (S2RuleType type : new S2RuleType[] { S2RuleType.LENGTH, S2RuleType.MIN_LENGTH, S2RuleType.MAX_LENGTH,
                S2RuleType.MIN_BYTE, S2RuleType.MAX_BYTE }) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> new S2Rule(type, "abc"), type.name());
            Assertions.assertThrows(IllegalArgumentException.class, () -> new S2Rule(type, 2.5), type.name());
            Assertions.assertDoesNotThrow(() -> new S2Rule(type, 5), type.name());
            Assertions.assertDoesNotThrow(() -> new S2Rule(type, "5"), type.name());
        }
    }

    @Test
    void rulesJsonNeverContainsNonFiniteNumbers() {
        // Previously MAX_VALUE NaN produced "value":NaN, invalid JSON that disabled client validation for the whole form. | 이전에는 "value":NaN 이 나가 폼 전체의 클라이언트 검증이 꺼졌음
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> S2Validator.<Map<String, Object>>builder().field("x", "x").rule(S2RuleType.MAX_VALUE, Double.NaN).build());
        String json = S2ValidatorFactory.getRulesJson(
                S2Validator.<Map<String, Object>>builder().field("x", "x").rule(S2RuleType.MAX_VALUE, 100).build(), Locale.KOREAN);
        Assertions.assertFalse(json.contains("NaN") || json.contains("Infinity"), json);
    }

    @Test
    void dateAcceptsOldDatesAndDateObjects() {
        Assertions.assertTrue(S2Validator.check(java.time.LocalDate.of(1925, 1, 1)).rule(S2RuleType.DATE).validate());
        Assertions.assertTrue(S2Validator.check(new java.util.Date()).rule(S2RuleType.DATE).validate());
        Assertions.assertTrue(S2Validator.check(java.sql.Date.valueOf("1900-01-01")).rule(S2RuleType.DATE).validate());
        Assertions.assertFalse(S2Validator.check(java.time.LocalTime.NOON).rule(S2RuleType.DATE).validate());
    }

    @Test
    void byteRulesCountUtf8BytesRegardlessOfPlatformCharset() {
        // "한글" is 6 bytes in UTF-8 (4 in MS949). The client counts UTF-8 via Blob, so the server must too. | "한글"은 UTF-8 6바이트(MS949 4바이트). 클라이언트가 Blob 으로 UTF-8 을 세므로 서버도 같아야 함
        Assertions.assertFalse(S2Validator.check("한글").rule(S2RuleType.MAX_BYTE, 5).validate());
        Assertions.assertTrue(S2Validator.check("한글").rule(S2RuleType.MAX_BYTE, 6).validate());
        Assertions.assertTrue(S2Validator.check("한글").rule(S2RuleType.MIN_BYTE, 6).validate());
    }
}
