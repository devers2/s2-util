package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.validation.S2Validator.S2ValidationError;

/**
 * Verdicts (not only server/client agreement) for NESTED/EACH/wildcard rules that refer to outer objects, and for
 * index gaps. {@link FormPathParityTest} checks that the browser reaches the same verdicts.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 바깥 객체를 참조하는 NESTED/EACH/와일드카드 규칙과 인덱스 빈칸의 판정 자체(서버·클라이언트 일치만이 아니라)를 확인합니다. 브라우저가 같은 판정을
 * 내리는지는 {@link FormPathParityTest}가 확인합니다.
 */
public class NestedContextTest {

    private static List<String> errors(S2Validator<Object> validator, Map<String, Object> target) {
        List<String> paths = new ArrayList<>();
        validator.validate(target, (S2ValidationError e) -> paths.add(e.fieldName()), Locale.KOREAN);
        return paths;
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void nestedRuleComparesWithRootField() {
        S2Validator<Object> v = S2Validator.builder().field("globalStart", "전체 시작일")
                .field("period", "기간").rule(S2RuleType.NESTED, S2Validator.builder()
                        .field("end", "종료일").rule(S2RuleType.DATE_AFTER, "globalStart").build())
                .build();
        Assertions.assertEquals(List.of("period.end"),
                errors(v, map("globalStart", "2024-02-05", "period", map("end", "2024-02-01"))));
        Assertions.assertEquals(List.of(),
                errors(v, map("globalStart", "2024-02-05", "period", map("end", "2024-03-01"))));
    }

    @Test
    void eachRowComparesWithRootField() {
        S2Validator<Object> v = S2Validator.builder().field("globalStart", "전체 시작일")
                .field("items", "품목").rule(S2RuleType.EACH, S2Validator.builder()
                        .field("end", "종료일").rule(S2RuleType.DATE_AFTER, "globalStart").build())
                .build();
        Assertions.assertEquals(List.of("items[0].end"), errors(v, map("globalStart", "2024-02-05",
                "items", List.of(map("end", "2024-02-01"), map("end", "2024-03-01")))));
    }

    @Test
    void siblingInTheSameObjectWinsOverTheRoot() {
        // "start" exists both in the row and at the root: the row value is used | 행과 루트 모두 "start"가 있으면 행 값 사용
        S2Validator<Object> v = S2Validator.builder().field("start", "전체 시작일")
                .field("items", "품목").rule(S2RuleType.EACH, S2Validator.builder()
                        .field("start", "시작일").field("end", "종료일").rule(S2RuleType.DATE_AFTER, "start").build())
                .build();
        Assertions.assertEquals(List.of(), errors(v, map("start", "2030-01-01",
                "items", List.of(map("start", "2024-01-01", "end", "2024-01-05")))));
    }

    @Test
    void conditionsInsideNestedAndEachCanReferToRootFields() {
        S2Validator<Object> nested = S2Validator.builder().field("globalType", "유형")
                .field("address", "주소").rule(S2RuleType.NESTED, S2Validator.builder()
                        .field("zip", "우편번호").when("globalType", "X").rule(S2RuleType.REQUIRED).build())
                .build();
        Assertions.assertEquals(List.of("address.zip"), errors(nested, map("globalType", "X", "address", map("zip", ""))));
        Assertions.assertEquals(List.of(), errors(nested, map("globalType", "Y", "address", map("zip", ""))));

        S2Validator<Object> each = S2Validator.builder().field("globalType", "유형")
                .field("items", "품목").rule(S2RuleType.EACH, S2Validator.builder()
                        .field("code", "코드").when("globalType", "X").rule(S2RuleType.REQUIRED).build())
                .build();
        Assertions.assertEquals(List.of("items[0].code"),
                errors(each, map("globalType", "X", "items", List.of(map("code", "")))));
    }

    @Test
    void siblingConditionFieldWinsOverTheRoot() {
        S2Validator<Object> v = S2Validator.builder().field("type", "전체 유형")
                .field("items", "품목").rule(S2RuleType.EACH, S2Validator.builder()
                        .field("type", "유형").field("code", "코드").when("type", "X").rule(S2RuleType.REQUIRED).build())
                .build();
        // root type=X but the row's own type=Y decides | 루트 type=X 여도 행 자신의 type=Y 가 결정
        Assertions.assertEquals(List.of(), errors(v, map("type", "X", "items", List.of(map("type", "Y", "code", "")))));
    }

    @Test
    void indexGapsAreNotRows() {
        S2Validator<Object> wildcard = S2Validator.builder().field("items[].name", "품목명").rule(S2RuleType.REQUIRED).build();
        List<Object> rows = new ArrayList<>(Arrays.asList(map("name", "A"), null, map("name", "")));
        Assertions.assertEquals(List.of("items[2].name"), errors(wildcard, map("items", rows)));

        S2Validator<Object> each = S2Validator.builder().field("items", "품목")
                .rule(S2RuleType.EACH, S2Validator.builder().field("name", "품목명").rule(S2RuleType.REQUIRED).build()).build();
        Assertions.assertEquals(List.of("items[2].name"), errors(each, map("items", rows)));
    }

    @Test
    void outerFieldLabelIsUsedInMessagesAndRulesJson() {
        S2Validator<Object> v = S2Validator.builder().field("globalStart", "전체 시작일")
                .field("items", "품목").rule(S2RuleType.EACH, S2Validator.builder()
                        .field("end", "종료일").rule(S2RuleType.DATE_AFTER, "globalStart").build())
                .field("period", "기간").rule(S2RuleType.NESTED, S2Validator.builder()
                        .field("end", "기간 종료일").rule(S2RuleType.DATE_AFTER, "globalStart").build())
                .build();
        List<S2ValidationError> errors = new ArrayList<>();
        v.validate(map("globalStart", "2024-02-05", "items", List.of(map("end", "2024-02-01")), "period", map("end", "2024-02-01")),
                errors::add, Locale.KOREAN);

        Assertions.assertEquals(2, errors.size(), errors.toString());
        for (S2ValidationError e : errors) {
            Assertions.assertTrue(e.defaultMessage().contains("전체 시작일"), e.defaultMessage());
            Assertions.assertEquals("전체 시작일", e.errorArgs()[1]);
        }
        String json = v.getRulesJson(Locale.KOREAN);
        Assertions.assertTrue(json.contains("종료일은 전체 시작일보다"), json);
        Assertions.assertTrue(json.contains("기간 종료일은 전체 시작일보다"), json);
        Assertions.assertFalse(json.contains("globalStart보다"), json);
    }
}
