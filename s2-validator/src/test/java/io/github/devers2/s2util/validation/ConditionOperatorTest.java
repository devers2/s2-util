package io.github.devers2.s2util.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Server-side checks for {@link S2Operator} conditions that the form parity tests cannot cover: creation-time
 * validation, the {@code when(field, null)} overload, non-form values, and the JSON shape.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 폼 판정 일치 시험으로 다루기 어려운 {@link S2Operator} 조건의 서버 측 동작을 확인합니다. 생성 시점 검사,
 * {@code when(field, null)} 오버로드, 폼이 아닌 값, JSON 형식.
 */
public class ConditionOperatorTest {

    private static Map<String, Object> input(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void valuesThatDoNotFitTheOperatorAreRejectedAtCreation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new S2Condition("age", S2Operator.GT, "abc"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new S2Condition("age", S2Operator.LTE, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new S2Condition("type", S2Operator.IN, "A"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new S2Condition("memo", S2Operator.EMPTY, "x"));
        Assertions.assertThrows(NullPointerException.class, () -> new S2Condition("memo", null, "x"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> S2Validator.<Map<String, Object>>builder().field("memo").when("age", S2Operator.GT, "1,000"));
    }

    @Test
    void legacyConstructorAndFactoryMeanEquality() {
        Assertions.assertEquals(S2Operator.EQ, new S2Condition("type", "A").operator());
        Assertions.assertEquals(S2Operator.EQ, S2Condition.of("type", "A").operator());
        Assertions.assertEquals(new S2Condition("type", S2Operator.IN, new String[] { "A", "B" }),
                new S2Condition("type", S2Operator.IN, List.of("A", "B")));
    }

    @Test
    void nullLiteralStillMeansEmpty() {
        // when("email", null) resolves to the operator overload; it must keep meaning "email is empty". | when("email", null)은 연산자 오버로드로 해석되며 "email 이 비었음" 의미를 유지해야 함
        Assertions.assertFalse(S2Validator.of(input("email", ""), false)
                .field("memo").when("email", null).validate());
        Assertions.assertTrue(S2Validator.of(input("email", "a@b.com"), false)
                .field("memo").when("email", null).validate());
        Assertions.assertFalse(S2Validator.of(input("email", "", "age", 30), false)
                .field("memo").when("age", S2Operator.GT, 1).and("email", null).validate());
    }

    @Test
    void dtoStyleValuesAreJudged() {
        // Numbers, enums, arrays and empty collections from DTOs | DTO 의 숫자, 열거형, 배열, 빈 컬렉션
        Assertions.assertFalse(S2Validator.of(input("age", 20L), false).field("memo").when("age", S2Operator.GTE, 20).validate());
        Assertions.assertTrue(S2Validator.of(input("age", 19.5), false).field("memo").when("age", S2Operator.GTE, 20).validate());
        Assertions.assertFalse(S2Validator.of(input("unit", Locale.Category.FORMAT), false)
                .field("memo").when("unit", S2Operator.IN, List.of("FORMAT", "DISPLAY")).validate());
        Assertions.assertFalse(S2Validator.of(input("tags", new String[] { "x", "y" }), false)
                .field("memo").when("tags", "y").validate());
        Assertions.assertFalse(S2Validator.of(input("tags", List.of()), false)
                .field("memo").when("tags", S2Operator.EMPTY).validate());
    }

    enum Grade {
        GOLD {
            @Override
            public String toString() {
                return "Gold member";
            }
        }
    }

    @Test
    void enumConditionValuesAreExportedByNameLikeTheServerCompares() {
        // The server compares enums by name(); exporting toString() would make the browser compare other text. | 서버는 name()으로 비교하므로 toString()을 내보내면 브라우저가 다른 문자열을 비교함
        String json = S2Validator.<Map<String, Object>>builder()
                .field("memo").when("grade", Grade.GOLD)
                .field("note").when("grade", S2Operator.IN, List.of(Grade.GOLD))
                .build().getRulesJson(Locale.KOREAN);
        Assertions.assertTrue(json.contains("{\"field\":\"grade\",\"value\":\"GOLD\"}"), json);
        Assertions.assertTrue(json.contains("\"op\":\"IN\",\"value\":[\"GOLD\"]"), json);
        Assertions.assertFalse(S2Validator.of(input("grade", "GOLD"), false).field("memo").when("grade", Grade.GOLD).validate());
    }

    @Test
    void rulesJsonCarriesTheOperatorExceptForEq() {
        String json = S2Validator.<Map<String, Object>>builder()
                .field("memo").when("type", "A")
                .field("note").when("type", S2Operator.IN, List.of("A", "B")).and("age", S2Operator.GT, 19)
                .field("extra").when("email", S2Operator.NOT_EMPTY)
                .build().getRulesJson(Locale.KOREAN);
        Assertions.assertTrue(json.contains("{\"field\":\"type\",\"value\":\"A\"}"), json);
        Assertions.assertTrue(json.contains("{\"field\":\"type\",\"op\":\"IN\",\"value\":[\"A\",\"B\"]}"), json);
        Assertions.assertTrue(json.contains("{\"field\":\"age\",\"op\":\"GT\",\"value\":19}"), json);
        Assertions.assertTrue(json.contains("{\"field\":\"email\",\"op\":\"NOT_EMPTY\",\"value\":null}"), json);
    }
}
