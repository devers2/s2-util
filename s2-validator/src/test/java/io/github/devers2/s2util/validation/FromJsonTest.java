package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.devers2.s2util.json.S2JsonException;

/**
 * {@link S2Validator#fromJson(String)}: exported rules read back to an equivalent validator, definition-only keys work,
 * and mistakes in a rules definition fail with the JSON path.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link S2Validator#fromJson(String)}: 내보낸 규칙을 다시 읽으면 같은 검증기가 되고, 정의용 키가 동작하며, 규칙 정의의 실수는 JSON 경로와
 * 함께 실패하는지 확인합니다.
 */
public class FromJsonTest {

    private static S2Validator<Map<String, Object>> sample() {
        S2Validator<Map<String, Object>> item = S2Validator.<Map<String, Object>>builder()
                .field("name", "품명").rule(S2RuleType.REQUIRED)
                .field("qty", "수량").rule(S2RuleType.MIN_VALUE, 1)
                .build();
        return S2Validator.<Map<String, Object>>builder()
                .field("name", "이름")
                .field("email", "이메일").rule(S2RuleType.REQUIRED).rule(S2RuleType.EMAIL)
                .field("code", "코드").rule(S2RuleType.REGEX, "[A-Z]{3}").rule(S2RuleType.MAX_LENGTH, 3)
                .field("bizNo", "사업자번호").rule(S2RuleType.BIZRNO, true)
                .field("pwCheck", "비밀번호 확인").rule(S2RuleType.EQUALS_FIELD, "pw")
                .field("price", "가격").rule(S2RuleType.MAX_VALUE, 99.5)
                .field("memo", "메모").rule(S2RuleType.REQUIRED)
                .when("type", S2Operator.IN, List.of("A", "B")).and("age", S2Operator.GTE, 20)
                .when("vip", true)
                .field("items", "품목").rule(S2RuleType.EACH, item)
                .build();
    }

    private static Map<String, Object> input(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    private static List<String> errors(S2Validator<Map<String, Object>> v, Map<String, Object> target, Locale locale) {
        List<String> errors = new ArrayList<>();
        v.validate(target, e -> errors.add(e.fieldName() + "=" + e.defaultMessage()), locale);
        return errors;
    }

    @Test
    void exportedRulesReadBackToTheSameRulesJson() {
        String exported = sample().getRulesJson(Locale.KOREAN);
        S2Validator<Map<String, Object>> read = S2Validator.fromJson(exported);
        Assertions.assertEquals(exported, read.getRulesJson(Locale.KOREAN));
    }

    @Test
    void exportedRulesReadBackJudgeTheSame() {
        S2Validator<Map<String, Object>> original = sample();
        S2Validator<Map<String, Object>> read = S2Validator.fromJson(original.getRulesJson(Locale.KOREAN));
        List<Map<String, Object>> inputs = List.of(
                input(),
                input("name", "홍", "email", "bad", "code", "abcd", "bizNo", "123-45-67890", "pw", "a", "pwCheck", "b",
                        "price", 100, "type", "A", "age", 30, "items", List.of(Map.of("qty", 0))),
                input("name", "홍", "email", "a@b.com", "code", "ABC", "bizNo", "220-81-62517", "pw", "a", "pwCheck", "a",
                        "price", 10, "type", "A", "age", 19, "memo", "", "items", List.of(Map.of("name", "x", "qty", 2))),
                input("name", "홍", "email", "a@b.com", "vip", "true"));
        for (Map<String, Object> in : inputs) {
            Assertions.assertEquals(errors(original, in, Locale.KOREAN), errors(read, in, Locale.KOREAN), in.toString());
        }
    }

    @Test
    void definitionKeysSetMessagesPerLanguage() {
        S2Validator<Map<String, Object>> v = S2Validator.fromJson("""
                {"schemaVersion": 1, "fields": [
                  {"name": "memo", "label": "메모",
                   "rules": [{"type": "MAX_LENGTH", "value": 3,
                              "message": "{0} too long",
                              "messages": {"ko": "{0|은/는} 3자 이하"}}]},
                  {"name": "type", "label": "유형"}
                ]}""");
        Map<String, Object> in = input("memo", "abcd");
        Assertions.assertEquals(List.of("memo=메모는 3자 이하", "type=유형은 필수 입력 항목입니다."), errors(v, in, Locale.KOREAN));
        Assertions.assertEquals(List.of("memo=메모 too long", "type=유형 is required."), errors(v, in, Locale.ENGLISH));
    }

    @Test
    void conditionsAndNestedRulesFromADefinition() {
        S2Validator<Map<String, Object>> v = S2Validator.fromJson("""
                {"schemaVersion": 1, "fields": [
                  {"name": "reason", "conditions": [[{"field": "status", "op": "IN", "value": ["REJECT", "HOLD"]}],
                                                    [{"field": "amount", "op": "GT", "value": 1000000}]]},
                  {"name": "items", "rules": [{"type": "EACH", "nestedRules": [
                     {"name": "qty", "rules": [{"type": "MIN_VALUE", "value": 1}]}]}]}
                ]}""");
        Assertions.assertTrue(v.validate(input("status", "OK", "amount", 10, "items", List.of(Map.of("qty", 1))), e -> {}));
        Assertions.assertFalse(v.validate(input("status", "HOLD", "items", List.of()), e -> {}));
        Assertions.assertFalse(v.validate(input("amount", 2000000), e -> {}));
        Assertions.assertFalse(v.validate(input("items", List.of(Map.of("qty", 0))), e -> {}));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "typo in a key | {\"schemaVersion\":1,\"fields\":[{\"name\":\"a\",\"rules\":[{\"type\":\"REQUIRED\",\"mesage\":\"x\"}]}]} | $.fields[0].rules[0].mesage",
            "unknown rule type | {\"schemaVersion\":1,\"fields\":[{\"name\":\"a\",\"rules\":[{\"type\":\"REQUIRE\"}]}]} | $.fields[0].rules[0].type",
            "unknown operator | {\"schemaVersion\":1,\"fields\":[{\"name\":\"a\",\"conditions\":[[{\"field\":\"b\",\"op\":\"GREATER\",\"value\":1}]]}]} | $.fields[0].conditions[0][0].op",
            "bad condition value | {\"schemaVersion\":1,\"fields\":[{\"name\":\"a\",\"conditions\":[[{\"field\":\"b\",\"op\":\"GT\",\"value\":\"x\"}]]}]} | $.fields[0].conditions[0][0].value",
            "bad criterion | {\"schemaVersion\":1,\"fields\":[{\"name\":\"a\",\"rules\":[{\"type\":\"MIN_VALUE\",\"value\":\"abc\"}]}]} | $.fields[0].rules[0].value",
            "newer schema | {\"schemaVersion\":2,\"fields\":[{\"name\":\"a\"}]} | $.schemaVersion",
            "missing schema | {\"fields\":[{\"name\":\"a\"}]} | $.schemaVersion",
            "no fields | {\"schemaVersion\":1,\"fields\":[]} | $.fields",
            "missing name | {\"schemaVersion\":1,\"fields\":[{\"label\":\"a\"}]} | $.fields[0].name",
            "NESTED without rules | {\"schemaVersion\":1,\"fields\":[{\"name\":\"a\",\"rules\":[{\"type\":\"NESTED\"}]}]} | $.fields[0].rules[0].nestedRules",
            "nested error path | {\"schemaVersion\":1,\"fields\":[{\"name\":\"a\",\"rules\":[{\"type\":\"EACH\",\"nestedRules\":[{\"name\":\"b\",\"rules\":[{\"type\":\"X\"}]}]}]}]} | $.fields[0].rules[0].nestedRules[0].rules[0].type",
            "empty condition group | {\"schemaVersion\":1,\"fields\":[{\"name\":\"a\",\"conditions\":[[]]}]} | $.fields[0].conditions[0]" })
    void invalidDefinitionsFailWithTheJsonPath(String description, String json, String path) {
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> S2Validator.fromJson(json));
        Assertions.assertTrue(e.getMessage().contains(" " + path + ": "), e.getMessage());
    }

    @Test
    void syntaxErrorsAreJsonExceptions() {
        Assertions.assertThrows(S2JsonException.class, () -> S2Validator.fromJson("{\"schemaVersion\":1,"));
        Assertions.assertThrows(S2JsonException.class, () -> S2Validator.fromJson("[]"));
    }
}
