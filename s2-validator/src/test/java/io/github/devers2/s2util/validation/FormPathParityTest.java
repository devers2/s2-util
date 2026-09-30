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
                c("행 날짜 비교(상대 표기), 같은 이름의 최상위 필드가 있어도 행 값과 비교",
                        () -> b().field("start", "전체 시작일").field("items[].start", "행 시작일")
                                .field("items[].end", "행 종료일").rule(S2RuleType.DATE_AFTER, "start").build(),
                        F.text("start", "2000-01-01"),
                        F.text("items[0].start", "2024-02-05"), F.text("items[0].end", "2024-02-01")),
                c("행 날짜 비교(와일드카드 표기), 1행 종료 < 시작",
                        () -> b().field("items[].start", "시작일").field("items[].end", "종료일").rule(S2RuleType.DATE_AFTER, "items[].start").build(),
                        F.text("items[0].start", "2024-02-05"), F.text("items[0].end", "2024-02-01")),

                // ── 점 경로(중첩 객체 필드) ──
                c("점 경로 필수, address.zip 빈 값",
                        () -> b().field("address.zip", "우편번호").rule(S2RuleType.REQUIRED).build(),
                        F.text("address.zip", "")),
                c("점 경로 조건부 필수, 조건 충족",
                        () -> b().field("address.country", "국가")
                                .field("address.zip", "우편번호").when("address.country", "KR").rule(S2RuleType.REQUIRED).build(),
                        F.text("address.country", "KR"), F.text("address.zip", "")),
                c("점 경로 조건부 필수, 조건 불충족",
                        () -> b().field("address.country", "국가")
                                .field("address.zip", "우편번호").when("address.country", "KR").rule(S2RuleType.REQUIRED).build(),
                        F.text("address.country", "US"), F.text("address.zip", "")),

                // ── NESTED ──
                c("NESTED 하위 필수 누락",
                        () -> b().field("address", "주소").rule(S2RuleType.NESTED, sub().field("zip", "우편번호").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("address.zip", "")),
                c("NESTED 하위 정상",
                        () -> b().field("address", "주소").rule(S2RuleType.NESTED, sub().field("zip", "우편번호").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("address.zip", "12345")),
                c("NESTED 하위 형제 비교(EQUALS_FIELD)",
                        () -> b().field("account", "계정").rule(S2RuleType.NESTED, sub().field("pw", "비밀번호")
                                .field("pw2", "비밀번호 확인").rule(S2RuleType.REQUIRED).rule(S2RuleType.EQUALS_FIELD, "pw").build()).build(),
                        F.text("account.pw", "a1"), F.text("account.pw2", "b2")),
                c("NESTED 하위에서 최상위 필드와 비교(DATE_AFTER)",
                        () -> b().field("globalStart", "전체 시작일")
                                .field("period", "기간").rule(S2RuleType.NESTED, sub().field("end", "종료일").rule(S2RuleType.DATE_AFTER, "globalStart").build()).build(),
                        F.text("globalStart", "2024-02-05"), F.text("period.end", "2024-02-01")),

                // ── EACH ──
                c("EACH 행 필수, 2행 중 1행 빈 값",
                        () -> b().field("items", "품목").rule(S2RuleType.EACH, sub().field("name", "품목명").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("items[0].name", "A"), F.text("items[1].name", "")),
                c("EACH 행 안 비교(DATE_AFTER)",
                        () -> b().field("items", "품목").rule(S2RuleType.EACH, sub().field("start", "시작일")
                                .field("end", "종료일").rule(S2RuleType.DATE_AFTER, "start").build()).build(),
                        F.text("items[0].start", "2024-01-01"), F.text("items[0].end", "2024-01-05"),
                        F.text("items[1].start", "2024-02-05"), F.text("items[1].end", "2024-02-01")),
                c("EACH 행에서 최상위 필드와 비교(DATE_AFTER)",
                        () -> b().field("globalStart", "전체 시작일")
                                .field("items", "품목").rule(S2RuleType.EACH, sub().field("end", "종료일").rule(S2RuleType.DATE_AFTER, "globalStart").build()).build(),
                        F.text("globalStart", "2024-02-05"), F.text("items[0].end", "2024-02-01")),

                // ── 중첩 조합 ──
                c("NESTED 안의 EACH",
                        () -> b().field("order", "주문").rule(S2RuleType.NESTED, sub()
                                .field("lines", "주문 행").rule(S2RuleType.EACH, sub().field("qty", "수량").rule(S2RuleType.MIN_VALUE, 1).build()).build()).build(),
                        F.text("order.lines[0].qty", "3"), F.text("order.lines[1].qty", "0")),
                c("EACH 안의 NESTED",
                        () -> b().field("items", "품목").rule(S2RuleType.EACH, sub()
                                .field("addr", "배송지").rule(S2RuleType.NESTED, sub().field("zip", "우편번호").rule(S2RuleType.REQUIRED).build()).build()).build(),
                        F.text("items[0].addr.zip", "12345"), F.text("items[1].addr.zip", "")),

                // ── 와일드카드 ──
                c("와일드카드 하위 경로(items[].period.start)",
                        () -> b().field("items[].period.start", "시작일").rule(S2RuleType.REQUIRED).build(),
                        F.text("items[0].period.start", "2024-01-01"), F.text("items[1].period.start", "")),
                c("와일드카드 행 조건(items[].type)",
                        () -> b().field("items[].type", "유형")
                                .field("items[].code", "코드").when("items[].type", "X").rule(S2RuleType.REQUIRED).build(),
                        F.text("items[0].type", "X"), F.text("items[0].code", ""),
                        F.text("items[1].type", "Y"), F.text("items[1].code", "")),
                c("와일드카드 행에서 최상위 필드와 비교",
                        () -> b().field("globalStart", "전체 시작일")
                                .field("items[].end", "종료일").rule(S2RuleType.DATE_AFTER, "globalStart").build(),
                        F.text("globalStart", "2024-02-05"), F.text("items[0].end", "2024-02-01"), F.text("items[1].end", "2024-03-01")),
                c("와일드카드 행이 하나도 없음",
                        () -> b().field("items[].name", "품목명").rule(S2RuleType.REQUIRED).build()),
                c("기본형 배열(tags[])",
                        () -> b().field("tags[]", "태그").rule(S2RuleType.REQUIRED).build(),
                        F.text("tags[0]", "a"), F.text("tags[1]", "")),
                c("와일드카드 인덱스 빈칸(0, 2 행만 있음)",
                        () -> b().field("items[].name", "품목명").rule(S2RuleType.REQUIRED).build(),
                        F.text("items[0].name", "A"), F.text("items[2].name", "B")),
                c("이중 와일드카드(orders[].items[].qty)",
                        () -> b().field("orders[].items[].qty", "수량").rule(S2RuleType.MIN_VALUE, 1).build(),
                        F.text("orders[0].items[0].qty", "1"), F.text("orders[0].items[1].qty", "0")),

                // ── 조건·중첩 조합 ──
                c("NESTED 안 형제 조건(when country=KR)",
                        () -> b().field("address", "주소").rule(S2RuleType.NESTED, sub().field("country", "국가")
                                .field("zip", "우편번호").when("country", "KR").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("address.country", "KR"), F.text("address.zip", "")),
                c("NESTED 안 형제 조건 불충족(country=US)",
                        () -> b().field("address", "주소").rule(S2RuleType.NESTED, sub().field("country", "국가")
                                .field("zip", "우편번호").when("country", "KR").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("address.country", "US"), F.text("address.zip", "")),
                c("EACH 행 안 형제 조건(when type=X)",
                        () -> b().field("items", "품목").rule(S2RuleType.EACH, sub().field("type", "유형")
                                .field("code", "코드").when("type", "X").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("items[0].type", "X"), F.text("items[0].code", ""),
                        F.text("items[1].type", "Y"), F.text("items[1].code", "")),
                c("NESTED 안 와일드카드 행에서 최상위 필드와 비교",
                        () -> b().field("globalStart", "전체 시작일")
                                .field("order", "주문").rule(S2RuleType.NESTED, sub()
                                        .field("lines[].end", "종료일").rule(S2RuleType.DATE_AFTER, "globalStart").build()).build(),
                        F.text("globalStart", "2024-02-05"), F.text("order.lines[0].end", "2024-02-01"), F.text("order.lines[1].end", "2024-03-01")),
                c("NESTED 대상 입력이 아예 없음",
                        () -> b().field("address", "주소").rule(S2RuleType.NESTED, sub().field("zip", "우편번호").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("other", "x")),
                c("EACH 대상 행이 아예 없음",
                        () -> b().field("items", "품목").rule(S2RuleType.EACH, sub().field("name", "품목명").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("other", "x")),
                c("3단계 점 경로(a.b.c) 최대 길이",
                        () -> b().field("company.ceo.name", "대표자명").rule(S2RuleType.MAX_LENGTH, 3).build(),
                        F.text("company.ceo.name", "홍길동전")),
                c("행 안 EQUALS_FIELD 상대 표기",
                        () -> b().field("items[].pw", "비밀번호").field("items[].pw2", "비밀번호 확인").rule(S2RuleType.EQUALS_FIELD, "pw").build(),
                        F.text("items[0].pw", "a"), F.text("items[0].pw2", "a"), F.text("items[1].pw", "a"), F.text("items[1].pw2", "b")),
                c("EACH 행 숫자 규칙(쉼표·공백)",
                        () -> b().field("items", "품목").rule(S2RuleType.EACH, sub().field("qty", "수량").rule(S2RuleType.MAX_VALUE, 100).build()).build(),
                        F.text("items[0].qty", " 50 "), F.text("items[1].qty", "1,000")),

                c("NESTED 안 조건이 최상위 필드를 참조(when globalType=X)",
                        () -> b().field("globalType", "유형")
                                .field("address", "주소").rule(S2RuleType.NESTED, sub()
                                        .field("zip", "우편번호").when("globalType", "X").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("globalType", "X"), F.text("address.zip", "")),
                c("와일드카드 행 조건이 최상위 필드를 참조(when globalType=X)",
                        () -> b().field("globalType", "유형")
                                .field("items[].code", "코드").when("globalType", "X").rule(S2RuleType.REQUIRED).build(),
                        F.text("globalType", "X"), F.text("items[0].code", "")),
                c("EACH 행 조건이 최상위 필드를 참조(when globalType=X)",
                        () -> b().field("globalType", "유형")
                                .field("items", "품목").rule(S2RuleType.EACH, sub()
                                        .field("code", "코드").when("globalType", "X").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("globalType", "X"), F.text("items[0].code", "")),

                c("EACH 행 조건, 같은 이름의 최상위 필드보다 행 값 우선",
                        () -> b().field("type", "전체 유형")
                                .field("items", "품목").rule(S2RuleType.EACH, sub().field("type", "유형")
                                        .field("code", "코드").when("type", "X").rule(S2RuleType.REQUIRED).build()).build(),
                        F.text("type", "X"), F.text("items[0].type", "Y"), F.text("items[0].code", "")),
                c("빈 값 조건(when memo = null), memo 빈 문자열",
                        () -> b().field("memo", "메모")
                                .field("reason", "사유").when("memo", null).rule(S2RuleType.REQUIRED).build(),
                        F.text("memo", ""), F.text("reason", "")),

                // ── 체크박스 그룹 ──
                c("체크박스 그룹 필수, 하나도 선택 안 함",
                        () -> b().field("hobbies", "취미").rule(S2RuleType.REQUIRED).build(),
                        F.checkbox("hobbies", "a", false), F.checkbox("hobbies", "b", false), F.checkbox("hobbies", "c", false)),
                c("체크박스 그룹 필수, 하나 선택",
                        () -> b().field("hobbies", "취미").rule(S2RuleType.REQUIRED).build(),
                        F.checkbox("hobbies", "a", false), F.checkbox("hobbies", "b", true), F.checkbox("hobbies", "c", false)),

                // ── 조건 연산자: memo 는 조건이 충족될 때만 필수이므로 memo 오류 = 조건 충족 ──
                c("EQ, 체크박스 그룹 두 개 체크 중 하나와 같음 (배열 정규화)",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("hobbies", "a").build(),
                        F.text("memo", ""), F.checkbox("hobbies", "a", true), F.checkbox("hobbies", "b", true)),
                c("NE, 다른 값 → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("type", S2Operator.NE, "A").build(),
                        F.text("memo", ""), F.text("type", "B")),
                c("NE, 같은 값 → 불충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("type", S2Operator.NE, "A").build(),
                        F.text("memo", ""), F.text("type", "A")),
                c("NE, 빈 값 → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("type", S2Operator.NE, "A").build(),
                        F.text("memo", ""), F.text("type", "")),
                c("GT 19, '20' → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("age", S2Operator.GT, 19).build(),
                        F.text("memo", ""), F.text("age", "20")),
                c("GT 19, '19' → 불충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("age", S2Operator.GT, 19).build(),
                        F.text("memo", ""), F.text("age", "19")),
                c("GT 19, '1,000' → 숫자 아님, 불충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("age", S2Operator.GT, 19).build(),
                        F.text("memo", ""), F.text("age", "1,000")),
                c("GT 19, 빈 값 → 불충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("age", S2Operator.GT, 19).build(),
                        F.text("memo", ""), F.text("age", "")),
                c("GTE 19, ' 19 ' → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("age", S2Operator.GTE, "19").build(),
                        F.text("memo", ""), F.text("age", " 19 ")),
                c("LT 19, '18.5' → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("age", S2Operator.LT, 19).build(),
                        F.text("memo", ""), F.text("age", "18.5")),
                c("LTE 19, '19.01' → 불충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("age", S2Operator.LTE, 19).build(),
                        F.text("memo", ""), F.text("age", "19.01")),
                c("IN [A,B], 'B' → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("type", S2Operator.IN, List.of("A", "B")).build(),
                        F.text("memo", ""), F.text("type", "B")),
                c("IN [A,B], 'C' → 불충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("type", S2Operator.IN, new String[] { "A", "B" }).build(),
                        F.text("memo", ""), F.text("type", "C")),
                c("IN [x,b], 체크박스 그룹에 b 포함 → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("hobbies", S2Operator.IN, List.of("x", "b")).build(),
                        F.text("memo", ""), F.checkbox("hobbies", "a", true), F.checkbox("hobbies", "b", true)),
                c("NOT_IN [A,B], 'C' → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("type", S2Operator.NOT_IN, List.of("A", "B")).build(),
                        F.text("memo", ""), F.text("type", "C")),
                c("NOT_IN [A,B], 'A' → 불충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("type", S2Operator.NOT_IN, List.of("A", "B")).build(),
                        F.text("memo", ""), F.text("type", "A")),
                c("EMPTY, 빈 값 → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("email", S2Operator.EMPTY).build(),
                        F.text("memo", ""), F.text("email", "  ")),
                c("EMPTY, 값 있음 → 불충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("email", S2Operator.EMPTY).build(),
                        F.text("memo", ""), F.text("email", "a@b.com")),
                c("NOT_EMPTY, 체크 안 한 그룹 → 불충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("hobbies", S2Operator.NOT_EMPTY).build(),
                        F.text("memo", ""), F.checkbox("hobbies", "a", false), F.checkbox("hobbies", "b", false)),
                c("NOT_EMPTY, 하나 체크한 그룹 → 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("hobbies", S2Operator.NOT_EMPTY).build(),
                        F.text("memo", ""), F.checkbox("hobbies", "a", false), F.checkbox("hobbies", "b", true)),
                c("when(field, null) 는 여전히 빈 값 조건",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED).when("email", null).build(),
                        F.text("memo", ""), F.text("email", "")),
                c("IN + and(GTE), 둘 다 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED)
                                .when("type", S2Operator.IN, List.of("A", "B")).and("age", S2Operator.GTE, 20).build(),
                        F.text("memo", ""), F.text("type", "A"), F.text("age", "20")),
                c("IN + and(GTE), 하나만 충족",
                        () -> b().field("memo", "메모").rule(S2RuleType.REQUIRED)
                                .when("type", S2Operator.IN, List.of("A", "B")).and("age", S2Operator.GTE, 20).build(),
                        F.text("memo", ""), F.text("type", "A"), F.text("age", "19")),
                c("와일드카드 행, NE 조건",
                        () -> b().field("items[].qty", "수량").rule(S2RuleType.REQUIRED).when("items[].type", S2Operator.NE, "X").build(),
                        F.text("items[0].type", "X"), F.text("items[0].qty", ""),
                        F.text("items[1].type", "Y"), F.text("items[1].qty", "")),

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

        String rulesJson = validator.getRulesJson(Locale.KOREAN);
        Set<String> clientInvalid = clientInvalidFields(rulesJson, fields);

        Assertions.assertEquals(serverInvalid, clientInvalid,
                "[폼 경로 판정 불일치] " + description + " — 규칙 JSON: " + rulesJson);
    }

    /**
     * The form-free {@code S2Validator.check(rules, data)} judges the same data the server validates (nested maps and
     * lists, as Spring binds them) exactly like the server.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void serverAndFormFreeCheckReportTheSameInvalidFields(String description,
            Supplier<S2Validator<Map<String, Object>>> validatorFactory, List<F> fields) {
        S2Validator<Map<String, Object>> validator = validatorFactory.get();
        Map<String, Object> target = toServerTarget(fields);

        Set<String> serverInvalid = new TreeSet<>();
        validator.validate(target, error -> serverInvalid.add(error.fieldName()), Locale.KOREAN);

        String rulesJson = validator.getRulesJson(Locale.KOREAN);
        String dataJson = io.github.devers2.s2util.json.S2JsonUtil.toJson(target);
        Value errors = jsContext.eval("js", "(r, d) => __S2Validator.check(r, JSON.parse(d))").execute(rulesJson, dataJson);
        Assertions.assertFalse(errors.hasMember("__system_error__"), "JS 시스템 오류: " + errors);

        Assertions.assertEquals(serverInvalid, new TreeSet<>(errors.getMemberKeys()),
                "[check() 판정 불일치] " + description + " — 데이터: " + dataJson);
    }

    private static Set<String> clientInvalidFields(String rulesJson, List<F> fields) {
        Value form = jsContext.getBindings("js").getMember("__makeForm").execute(toFieldsJson(fields));
        Value errors = jsContext.getBindings("js").getMember("__S2Validator").invokeMember("validate", form, rulesJson);
        Assertions.assertFalse(errors.hasMember("__system_error__"), "JS 시스템 오류: " + errors);
        return new TreeSet<>(errors.getMemberKeys());
    }

    /**
     * Builds the server-side target as Spring binding would: unchecked checkboxes/radios are absent, a checkbox group
     * with several elements becomes a list of the checked values, and paths such as {@code a.b[0].c} or
     * {@code tags[1]} become nested maps and lists. Missing list positions (index gaps) are left {@code null}.
     */
    private static Map<String, Object> toServerTarget(List<F> fields) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Long> checkboxCounts = new HashMap<>();
        for (F f : fields) {
            if ("checkbox".equals(f.type())) {
                checkboxCounts.merge(f.name(), 1L, Long::sum);
            }
        }
        Map<String, List<Object>> groups = new java.util.LinkedHashMap<>();
        for (F f : fields) {
            boolean group = "checkbox".equals(f.type()) && checkboxCounts.get(f.name()) > 1;
            if (group) {
                List<Object> checked = groups.computeIfAbsent(f.name(), k -> new ArrayList<>());
                if (f.checked()) {
                    checked.add(f.value());
                }
                continue;
            }
            if (("checkbox".equals(f.type()) || "radio".equals(f.type())) && !f.checked()) {
                continue;
            }
            setPath(root, f.name(), f.value());
        }
        groups.forEach((name, checked) -> {
            if (!checked.isEmpty()) {
                setPath(root, name, checked);
            }
        });
        return root;
    }

    @SuppressWarnings("unchecked")
    private static void setPath(Map<String, Object> root, String path, Object value) {
        List<Object> keys = new ArrayList<>();
        for (String segment : path.split("\\.")) {
            int bracket = segment.indexOf('[');
            keys.add(bracket < 0 ? segment : segment.substring(0, bracket));
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[(\\d+)\\]").matcher(segment);
            while (m.find()) {
                keys.add(Integer.parseInt(m.group(1)));
            }
        }
        Object container = root;
        for (int i = 0; i < keys.size(); i++) {
            Object key = keys.get(i);
            boolean last = i == keys.size() - 1;
            Object child = last ? value : (keys.get(i + 1) instanceof Integer ? new ArrayList<>() : new HashMap<String, Object>());
            if (key instanceof Integer index) {
                List<Object> list = (List<Object>) container;
                while (list.size() <= index) {
                    list.add(null);
                }
                if (last || list.get(index) == null) {
                    list.set(index, child);
                }
                container = list.get(index);
            } else {
                Map<String, Object> map = (Map<String, Object>) container;
                if (last) {
                    map.put((String) key, child);
                } else {
                    map.putIfAbsent((String) key, child);
                }
                container = map.get(key);
            }
        }
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

    private static S2FieldStep.BuilderStartStep<Object> sub() {
        return S2Validator.builder();
    }

    private static S2FieldStep.BuilderStartStep<Map<String, Object>> b() {
        return S2Validator.<Map<String, Object>>builder();
    }

    private static Arguments c(String description, Supplier<S2Validator<Map<String, Object>>> validator, F... fields) {
        return Arguments.of(description, validator, List.of(fields));
    }
}
