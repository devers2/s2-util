package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * End-to-end server/client parity through the real usage path: server validator → rules JSON → form → JS validate(form).
 * <p>
 * {@link ServerClientParityTest} compares a single rule by calling the internal {@code validateCheck()} directly,
 * so it does not cover JSON serialization, form value extraction (trimming, checkbox collection), conditions,
 * or wildcard row paths. This test drives the public {@code S2Validator.validate(form, rules)} against a minimal
 * fake DOM and compares the set of invalid field paths with the server result for the same input.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 실제 사용 경로(서버 검증기 → 규칙 JSON → 폼 → JS validate(form))로 서버·클라이언트 판정 일치를 확인합니다.
 * <p>
 * {@link ServerClientParityTest}는 내부 함수 {@code validateCheck()}로 규칙 하나를 비교하므로 JSON 직렬화, 폼 값 추출(공백 제거,
 * 체크박스 수집), 조건, 와일드카드 행 경로를 거치지 않습니다. 이 시험은 최소 가짜 DOM 위에서 공개 API
 * {@code S2Validator.validate(form, rules)}를 실행하고, 같은 입력에 대한 서버 결과와 오류 필드 경로 집합을 비교합니다.
 * </p>
 */
public class FormPathParityTest {

    private static Context jsContext;

    /** A form control: name, type (text/checkbox/radio/hidden), value, checked | 폼 컨트롤: 이름, 타입, 값, 체크 여부 */
    record F(String name, String type, String value, boolean checked) {
        static F text(String name, String value) {
            return new F(name, "text", value, false);
        }

        static F checkbox(String name, String value, boolean checked) {
            return new F(name, "checkbox", value, checked);
        }
    }

    @BeforeAll
    static void setupJsEngine() throws IOException {
        jsContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        String jsSource;
        try (InputStream is = FormPathParityTest.class.getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            if (is == null) {
                throw new IOException("s2.validator.js not found on classpath");
            }
            jsSource = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        jsSource = jsSource.replaceAll("export\\s+const\\s+", "const ");
        jsSource = jsSource.replaceAll("export\\s+function\\s+", "function ");
        jsSource = jsSource.replaceAll("export\\s+default\\s+", "");
        jsSource = jsSource.replace("initS2Validator();", "// initS2Validator();");

        // Minimal fake DOM: only what S2Validator.validate(form) touches. | 최소 가짜 DOM: S2Validator.validate(form)가 사용하는 기능만
        String fakeDom = """
                var document = {
                    querySelector: function() { return null; },
                    querySelectorAll: function() { return []; },
                    documentElement: {},
                    readyState: 'complete',
                    addEventListener: function() {},
                    createElement: function() { return {}; }
                };
                var window = {};
                var MutationObserver = function() { this.observe = function() {}; };
                var console = { log: function() {}, warn: function() {}, error: function() {} };
                class HTMLFormElement {
                    constructor(elements) { this.elements = elements; this.dataset = {}; }
                    querySelectorAll(sel) {
                        const m = sel.match(/^\\[name="(.+)"\\]$/);
                        return m ? this.elements.filter((e) => e.name === m[1]) : [];
                    }
                    querySelector(sel) { return this.querySelectorAll(sel)[0] ?? null; }
                    reportValidity() { return true; }
                }
                function Blob(parts) { this.size = unescape(encodeURIComponent(parts.join(''))).length; }
                globalThis.__makeForm = function(fieldsJson) {
                    const els = JSON.parse(fieldsJson).map((f) => ({
                        name: f.name, type: f.type, value: f.value, checked: f.checked,
                        offsetParent: {}, setCustomValidity: function() {}, addEventListener: function() {}
                    }));
                    return new HTMLFormElement(els);
                };
                """;
        jsContext.eval("js", fakeDom);
        jsContext.eval("js", jsSource + "\nglobalThis.__S2Validator = S2Validator;\n");
    }

    @AfterAll
    static void tearDown() {
        if (jsContext != null) {
            jsContext.close();
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                // ── 정규식 전체 일치 ──
                c("REGEX 앵커 없음(\\d+), abc123",
                        () -> b().field("code", "코드").rule(S2RuleType.REGEX, "\\d+").build(),
                        F.text("code", "abc123")),
                c("REGEX 앞 앵커만(^\\d+), 123abc",
                        () -> b().field("code", "코드").rule(S2RuleType.REGEX, "^\\d+").build(),
                        F.text("code", "123abc")),
                c("REGEX 대체와 양끝 앵커(^a|b$), ax",
                        () -> b().field("code", "코드").rule(S2RuleType.REGEX, "^a|b$").build(),
                        F.text("code", "ax")),

                // ── 숫자 ──
                c("MIN_VALUE 19, 폼 문자열 '25'",
                        () -> b().field("age", "나이").rule(S2RuleType.MIN_VALUE, 19).build(),
                        F.text("age", "25")),
                c("MIN_VALUE 19, '25abc'",
                        () -> b().field("age", "나이").rule(S2RuleType.MIN_VALUE, 19).build(),
                        F.text("age", "25abc")),
                c("MAX_VALUE 100, '1,000'",
                        () -> b().field("qty", "수량").rule(S2RuleType.MAX_VALUE, 100).build(),
                        F.text("qty", "1,000")),

                // ── 공백 ──
                c("MAX_LENGTH 3, 'abc '",
                        () -> b().field("code", "코드").rule(S2RuleType.MAX_LENGTH, 3).build(),
                        F.text("code", "abc ")),
                c("MIN_LENGTH 4, 'ab  '",
                        () -> b().field("code", "코드").rule(S2RuleType.MIN_LENGTH, 4).build(),
                        F.text("code", "ab  ")),
                c("REQUIRED, 공백만",
                        () -> b().field("name", "이름").rule(S2RuleType.REQUIRED).build(),
                        F.text("name", "   ")),

                // ── 체크박스 ──
                c("ASSERT_TRUE, 미체크",
                        () -> b().field("agree", "약관").rule(S2RuleType.ASSERT_TRUE).build(),
                        F.checkbox("agree", "true", false)),
                c("ASSERT_TRUE, 체크(on)",
                        () -> b().field("agree", "약관").rule(S2RuleType.ASSERT_TRUE).build(),
                        F.checkbox("agree", "on", true)),

                // ── 형식 ──
                c("EMAIL, abc",
                        () -> b().field("email", "이메일").rule(S2RuleType.EMAIL).build(),
                        F.text("email", "abc")),

                // ── 조건부 ──
                c("조건부 필수, type=VIP 이고 vipCode 없음",
                        () -> b().field("type", "유형").field("vipCode", "VIP 코드").when("type", "VIP").rule(S2RuleType.REQUIRED).build(),
                        F.text("type", "VIP"), F.text("vipCode", "")),
                c("조건부 필수, type=NORMAL 이고 vipCode 없음",
                        () -> b().field("type", "유형").field("vipCode", "VIP 코드").when("type", "VIP").rule(S2RuleType.REQUIRED).build(),
                        F.text("type", "NORMAL"), F.text("vipCode", "")),

                // ── 주민번호 (검증번호 검사는 규칙 JSON 의 value 로 전달) ──
                c("JUMIN 기본값, 검증번호 불일치 번호",
                        () -> b().field("rrn", "주민번호").rule(S2RuleType.JUMIN).build(),
                        F.text("rrn", "900101-1234560")),
                c("JUMIN(true), 검증번호 불일치 번호",
                        () -> b().field("rrn", "주민번호").rule(S2RuleType.JUMIN, true).build(),
                        F.text("rrn", "900101-1234560")),

                // ── 필드 간 비교 ──
                c("EQUALS_FIELD 불일치",
                        () -> b().field("pw", "비밀번호").field("pw2", "비밀번호 확인").rule(S2RuleType.REQUIRED).rule(S2RuleType.EQUALS_FIELD, "pw").build(),
                        F.text("pw", "Secret123!"), F.text("pw2", "Other123!")),
                c("EQUALS_FIELD 앞뒤 공백만 다름",
                        () -> b().field("pw", "비밀번호").field("pw2", "비밀번호 확인").rule(S2RuleType.REQUIRED).rule(S2RuleType.EQUALS_FIELD, "pw").build(),
                        F.text("pw", "Secret123!"), F.text("pw2", " Secret123! ")),
                c("DATE_AFTER 최상위, 종료 < 시작",
                        () -> b().field("start", "시작일").field("end", "종료일").rule(S2RuleType.DATE_AFTER, "start").build(),
                        F.text("start", "2024-01-05"), F.text("end", "2024-01-01")),

                // ── 와일드카드 행 ──
                c("행 필수, 2행 중 1행 빈 값",
                        () -> b().field("items[].name", "품목명").rule(S2RuleType.REQUIRED).build(),
                        F.text("items[0].name", "A"), F.text("items[1].name", "")),
                c("행 날짜 비교(상대 표기), 1행 종료 < 시작",
                        () -> b().field("items[].start", "시작일").field("items[].end", "종료일").rule(S2RuleType.DATE_AFTER, "start").build(),
                        F.text("items[0].start", "2024-01-01"), F.text("items[0].end", "2024-01-03"),
                        F.text("items[1].start", "2024-02-05"), F.text("items[1].end", "2024-02-01")),
                c("행 날짜 비교(와일드카드 표기), 1행 종료 < 시작",
                        () -> b().field("items[].start", "시작일").field("items[].end", "종료일").rule(S2RuleType.DATE_AFTER, "items[].start").build(),
                        F.text("items[0].start", "2024-02-05"), F.text("items[0].end", "2024-02-01")),

                // ── 가입 폼 형태(정상 입력은 양쪽 모두 통과해야 함) ──
                c("가입 폼 정상 입력",
                        () -> b()
                                .field("name", "이름")
                                .field("password", "비밀번호").rule(S2RuleType.REQUIRED).rule(S2RuleType.PASSWORD)
                                .field("passwordCheck", "비밀번호 확인").rule(S2RuleType.REQUIRED).rule(S2RuleType.EQUALS_FIELD, "password")
                                .field("privacy1Agreed", "이용약관").rule(S2RuleType.REQUIRED).rule(S2RuleType.ASSERT_TRUE)
                                .build(),
                        F.text("name", "홍길동"), F.text("password", "Passw0rd!"), F.text("passwordCheck", "Passw0rd!"),
                        F.checkbox("privacy1Agreed", "true", true)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void serverAndClientReportTheSameInvalidFields(String description, Supplier<S2Validator<Map<String, Object>>> validatorFactory,
            List<F> fields) {
        S2Validator<Map<String, Object>> validator = validatorFactory.get();

        Set<String> serverInvalid = new TreeSet<>();
        validator.validate(toServerTarget(fields), error -> serverInvalid.add(error.fieldName()), Locale.KOREAN);

        String rulesJson = S2ValidatorFactory.getRulesJson(validator, Locale.KOREAN);
        Set<String> clientInvalid = clientInvalidFields(rulesJson, fields);

        Assertions.assertEquals(serverInvalid, clientInvalid,
                "[폼 경로 판정 불일치] " + description + " — 규칙 JSON: " + rulesJson);
    }

    private static Set<String> clientInvalidFields(String rulesJson, List<F> fields) {
        Value form = jsContext.getBindings("js").getMember("__makeForm").execute(toFieldsJson(fields));
        Value errors = jsContext.getBindings("js").getMember("__S2Validator").invokeMember("validate", form, rulesJson);
        Assertions.assertFalse(errors.hasMember("__system_error__"), "JS 시스템 오류: " + errors);
        return new TreeSet<>(errors.getMemberKeys());
    }

    /**
     * Builds the server-side Map as Spring Map binding would: unchecked checkboxes are absent, and indexed names
     * ({@code items[0].name}) become nested lists of maps.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toServerTarget(List<F> fields) {
        Map<String, Object> root = new HashMap<>();
        for (F f : fields) {
            if (("checkbox".equals(f.type()) || "radio".equals(f.type())) && !f.checked()) {
                continue;
            }
            int bracket = f.name().indexOf('[');
            if (bracket < 0) {
                root.put(f.name(), f.value());
                continue;
            }
            String listName = f.name().substring(0, bracket);
            int index = Integer.parseInt(f.name().substring(bracket + 1, f.name().indexOf(']')));
            String property = f.name().substring(f.name().indexOf("].") + 2);
            List<Map<String, Object>> rows = (List<Map<String, Object>>) root.computeIfAbsent(listName, k -> new ArrayList<>());
            while (rows.size() <= index) {
                rows.add(new HashMap<>());
            }
            rows.get(index).put(property, f.value());
        }
        return root;
    }

    private static String toFieldsJson(List<F> fields) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < fields.size(); i++) {
            F f = fields.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"name\":").append(jsonString(f.name()))
                    .append(",\"type\":").append(jsonString(f.type()))
                    .append(",\"value\":").append(jsonString(f.value()))
                    .append(",\"checked\":").append(f.checked()).append('}');
        }
        return sb.append(']').toString();
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static S2FieldStep.BuilderStartStep<Map<String, Object>> b() {
        return S2Validator.<Map<String, Object>>builder();
    }

    private static Arguments c(String description, Supplier<S2Validator<Map<String, Object>>> validator, F... fields) {
        return Arguments.of(description, validator, List.of(fields));
    }
}
