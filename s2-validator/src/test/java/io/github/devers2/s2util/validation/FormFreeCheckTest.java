package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.json.S2JsonUtil;

/**
 * {@code S2Validator.check(rules, data)} with typed application state (numbers, booleans, nested rows) judges like the
 * server, which receives the same JSON.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 타입이 있는 애플리케이션 상태(숫자, 불리언, 중첩 행)에 대한 {@code S2Validator.check(rules, data)}가 같은 JSON 을 받는 서버와 같게
 * 판정하는지 확인합니다.
 */
public class FormFreeCheckTest {

    private static Context js;

    @BeforeAll
    static void setUp() throws IOException {
        String source;
        try (InputStream is = FormFreeCheckTest.class.getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        js = Context.newBuilder("js").allowHostAccess(HostAccess.ALL).option("engine.WarnInterpreterOnly", "false").build();
        // No DOM at all: check() must not need one | DOM 이 전혀 없음: check()는 DOM 이 필요 없어야 함
        js.eval("js", "var console = { log() {}, warn() {}, error() {} };");
        js.eval("js", source.replaceAll("export\\s+const\\s+", "const ") + "\nglobalThis.__S2Validator = S2Validator;\n");
    }

    @AfterAll
    static void tearDown() {
        js.close();
    }

    private static S2Validator<Map<String, Object>> rules() {
        S2Validator<Map<String, Object>> item = S2Validator.<Map<String, Object>>builder()
                .field("qty", "수량").rule(S2RuleType.MIN_VALUE, 1)
                .build();
        return S2Validator.<Map<String, Object>>builder()
                .field("name", "이름")
                .field("age", "나이").rule(S2RuleType.MIN_VALUE, 19)
                .field("agree", "동의").rule(S2RuleType.ASSERT_TRUE)
                .field("tags", "태그").rule(S2RuleType.REQUIRED)
                .field("items", "품목").rule(S2RuleType.EACH, item)
                .field("rows[].code", "코드").rule(S2RuleType.REQUIRED).rule(S2RuleType.MAX_LENGTH, 3)
                .field("address", "주소").rule(S2RuleType.NESTED, S2Validator.<Object>builder()
                        .field("zip", "우편번호").rule(S2RuleType.ZIP).build())
                .field("memo", "메모").rule(S2RuleType.REQUIRED).when("age", S2Operator.LT, 19)
                .build();
    }

    private static Set<String> server(Map<String, Object> data) {
        Set<String> invalid = new TreeSet<>();
        rules().validate(data, e -> invalid.add(e.fieldName()), Locale.KOREAN);
        return invalid;
    }

    private static Value check(Object rules, Object data) {
        return js.eval("js", "(r, d) => __S2Validator.check(r, JSON.parse(d))").execute(rules, S2JsonUtil.toJson(data));
    }

    private static Set<String> client(Map<String, Object> data) {
        return new TreeSet<>(check(rules().getRulesJson(Locale.KOREAN), data).getMemberKeys());
    }

    @Test
    void typedStateIsJudgedLikeTheServer() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "");
        data.put("age", 17);
        data.put("agree", false);
        data.put("tags", List.of());
        data.put("items", List.of(Map.of("qty", 0), Map.of("qty", 3)));
        data.put("rows", List.of(Map.of("code", "ABCD"), Map.of("code", "AB")));
        data.put("address", Map.of("zip", "12"));
        Set<String> expected = server(data);
        Assertions.assertEquals(Set.of("name", "age", "agree", "tags", "items[0].qty", "rows[0].code", "address.zip", "memo"),
                expected);
        Assertions.assertEquals(expected, client(data));
    }

    @Test
    void validStateHasNoErrors() {
        Map<String, Object> data = Map.of("name", "홍길동", "age", 20, "agree", true, "tags", List.of("a"),
                "items", List.of(Map.of("qty", 1)), "rows", List.of(Map.of("code", "A")), "address", Map.of("zip", "12345"));
        Assertions.assertEquals(Set.of(), server(data));
        Assertions.assertEquals(Set.of(), client(data));
    }

    @Test
    void missingFieldsAreEmptyButMissingObjectsAreSkipped() {
        // name missing → required error; address missing → its fields are not validated (like the server) | name 없음 → 필수 오류, address 없음 → 그 아래는 검증 안 함
        Map<String, Object> data = Map.of("age", 20, "agree", true, "tags", List.of("a"));
        Assertions.assertEquals(Set.of("name"), server(data));
        Assertions.assertEquals(Set.of("name"), client(data));
    }

    @Test
    void messagesAndFlatKeysAndBadRules() {
        Value errors = check(rules().getRulesJson(Locale.KOREAN), Map.of("age", 20, "agree", true, "tags", List.of("a")));
        Assertions.assertEquals("이름은 필수 입력 항목입니다.", errors.getMember("name").getArrayElement(0).asString());

        Value flat = check(rules().getRulesJson(Locale.KOREAN),
                Map.of("name", "a", "age", 20, "agree", true, "tags", List.of("a"), "items[0].qty", 0));
        Assertions.assertEquals(Set.of("items[0].qty"), new TreeSet<>(flat.getMemberKeys()));

        Assertions.assertTrue(check("not json", Map.of()).hasMember("__system_error__"));
    }
}
