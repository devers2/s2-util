package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 서버(Java)와 클라이언트(JavaScript)의 검증 규칙 동작 일관성을 검증하는 테스트.
 * <p>
 * GraalJS를 이용해 {@code s2.validator.js}의 {@code validateCheck} 함수를 직접 호출하고,
 * 동일한 입력에 대해 Java 쪽 {@link S2Rule#isValid(Object, Object)} 와 같은 결과를 내는지 비교한다.
 * </p>
 * <p>
 * 새 {@link S2RuleType}을 추가할 때 {@link #testCases()} 에 항목을 추가하면
 * 서버-클라이언트 양쪽이 자동으로 검증된다.
 * </p>
 */
public class ServerClientParityTest {

    private static Context jsContext;
    private static Value validateCheckFn;

    @BeforeAll
    static void setupJsEngine() throws IOException {
        jsContext = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        // s2.validator.js 로드 (클래스패스에서 읽음)
        String jsSource;
        try (InputStream is = ServerClientParityTest.class.getResourceAsStream(
                "/META-INF/resources/s2-util/js/s2.validator.js")) {
            if (is == null) throw new IOException("s2.validator.js not found on classpath");
            jsSource = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // ESM export 구문을 GraalJS에서 실행 가능하도록 변환
        jsSource = jsSource.replaceAll("export\\s+const\\s+", "const ");
        jsSource = jsSource.replaceAll("export\\s+function\\s+", "function ");
        jsSource = jsSource.replaceAll("export\\s+default\\s+", "");
        // initS2Validator() 자동 실행 방지 (DOM이 없는 테스트 환경)
        jsSource = jsSource.replace("initS2Validator();", "// initS2Validator();");

        // 브라우저 DOM API 스텁 (initS2Validator에서 document/MutationObserver 참조하므로)
        String domStubs = """
                var document = {
                    querySelector: function() { return null; },
                    querySelectorAll: function() { return []; },
                    documentElement: {},
                    readyState: 'complete',
                    addEventListener: function() {},
                    forms: []
                };
                var window = { MutationObserver: undefined };
                var MutationObserver = function() { this.observe = function() {}; };
                var HTMLFormElement = function() {};
                var console = {
                    log: function() {},
                    warn: function() {},
                    error: function() {}
                };
                """;

        // Blob polyfill (MIN_BYTE, MAX_BYTE에서 new Blob([str]).size 사용)
        String blobPolyfill = """
                function Blob(parts) {
                    var total = 0;
                    for (var i = 0; i < parts.length; i++) {
                        var s = String(parts[i]);
                        // UTF-8 바이트 길이 계산
                        for (var j = 0; j < s.length; j++) {
                            var c = s.charCodeAt(j);
                            if (c <= 0x7F) total += 1;
                            else if (c <= 0x7FF) total += 2;
                            else if (c >= 0xD800 && c <= 0xDBFF) { total += 4; j++; }
                            else total += 3;
                        }
                    }
                    this.size = total;
                }
                """;

        jsContext.eval("js", domStubs);
        jsContext.eval("js", blobPolyfill);
        // validateCheck를 globalThis에 바인딩
        jsSource += "\nglobalThis.__validateCheck = validateCheck;\n";
        jsContext.eval("js", jsSource);

        validateCheckFn = jsContext.eval("js", "globalThis.__validateCheck");
    }

    @AfterAll
    static void tearDown() {
        if (jsContext != null) jsContext.close();
    }

    // ========================================================================================
    // 테스트 케이스 정의 — 새 룰 추가 시 여기에 한 줄만 추가하면 된다
    // ========================================================================================

    /**
     * 각 테스트 케이스: (설명, S2RuleType, checkValue, 테스트값, 기대결과)
     * <p>
     * 서버와 클라이언트 양쪽에서 동일한 규칙·동일한 값으로 검증하여 결과가 같은지 비교한다.
     * </p>
     */
    static Stream<Arguments> testCases() {
        return Stream.of(
                // ── REQUIRED ─────────────────────────────────────────────
                Arguments.of("REQUIRED: null → 실패", S2RuleType.REQUIRED, null, null, false),
                Arguments.of("REQUIRED: 빈문자열 → 실패", S2RuleType.REQUIRED, null, "", false),
                Arguments.of("REQUIRED: 공백만 → 실패", S2RuleType.REQUIRED, null, "   ", false),
                Arguments.of("REQUIRED: 값 있음 → 성공", S2RuleType.REQUIRED, null, "hello", true),

                // ── ASSERT_TRUE ──────────────────────────────────────────
                Arguments.of("ASSERT_TRUE: 'true' → 성공", S2RuleType.ASSERT_TRUE, null, "true", true),
                Arguments.of("ASSERT_TRUE: 'on' → 성공", S2RuleType.ASSERT_TRUE, null, "on", true),
                Arguments.of("ASSERT_TRUE: 'false' → 실패", S2RuleType.ASSERT_TRUE, null, "false", false),
                Arguments.of("ASSERT_TRUE: null → 실패", S2RuleType.ASSERT_TRUE, null, null, false),

                // ── ASSERT_FALSE ─────────────────────────────────────────
                Arguments.of("ASSERT_FALSE: 'false' → 성공", S2RuleType.ASSERT_FALSE, null, "false", true),
                Arguments.of("ASSERT_FALSE: 'off' → 성공", S2RuleType.ASSERT_FALSE, null, "off", true),
                Arguments.of("ASSERT_FALSE: null → 성공", S2RuleType.ASSERT_FALSE, null, null, true),
                Arguments.of("ASSERT_FALSE: 'true' → 실패", S2RuleType.ASSERT_FALSE, null, "true", false),

                // ── LENGTH ───────────────────────────────────────────────
                Arguments.of("LENGTH: 정확히 5자 → 성공", S2RuleType.LENGTH, 5, "hello", true),
                Arguments.of("LENGTH: 4자 → 실패", S2RuleType.LENGTH, 5, "hell", false),
                Arguments.of("LENGTH: 빈값 → 성공(skip)", S2RuleType.LENGTH, 5, null, true),

                // ── MIN_LENGTH ───────────────────────────────────────────
                Arguments.of("MIN_LENGTH: 5자 이상 → 성공", S2RuleType.MIN_LENGTH, 3, "hello", true),
                Arguments.of("MIN_LENGTH: 2자 → 실패", S2RuleType.MIN_LENGTH, 3, "hi", false),
                Arguments.of("MIN_LENGTH: 빈값 → 성공(skip)", S2RuleType.MIN_LENGTH, 3, null, true),

                // ── MAX_LENGTH ───────────────────────────────────────────
                Arguments.of("MAX_LENGTH: 3자 이하 → 성공", S2RuleType.MAX_LENGTH, 5, "hi", true),
                Arguments.of("MAX_LENGTH: 6자 → 실패", S2RuleType.MAX_LENGTH, 5, "toolong", false),
                Arguments.of("MAX_LENGTH: 빈값 → 성공(skip)", S2RuleType.MAX_LENGTH, 5, null, true),

                // ── MIN_BYTE ─────────────────────────────────────────────
                Arguments.of("MIN_BYTE: 한글 3바이트 → 성공", S2RuleType.MIN_BYTE, 3, "가", true),
                Arguments.of("MIN_BYTE: ASCII 1바이트 → 실패", S2RuleType.MIN_BYTE, 3, "a", false),
                Arguments.of("MIN_BYTE: 빈값 → 성공(skip)", S2RuleType.MIN_BYTE, 3, null, true),

                // ── MAX_BYTE ─────────────────────────────────────────────
                Arguments.of("MAX_BYTE: ASCII 2바이트 → 성공", S2RuleType.MAX_BYTE, 5, "ab", true),
                Arguments.of("MAX_BYTE: 한글 2자(6바이트) → 실패", S2RuleType.MAX_BYTE, 5, "가나", false),
                Arguments.of("MAX_BYTE: 빈값 → 성공(skip)", S2RuleType.MAX_BYTE, 5, null, true),

                // ── MIN_VALUE ────────────────────────────────────────────
                Arguments.of("MIN_VALUE: 10 >= 5 → 성공", S2RuleType.MIN_VALUE, 5, 10, true),
                Arguments.of("MIN_VALUE: 3 < 5 → 실패", S2RuleType.MIN_VALUE, 5, 3, false),
                Arguments.of("MIN_VALUE: 빈값 → 성공(skip)", S2RuleType.MIN_VALUE, 5, null, true),

                // ── MAX_VALUE ────────────────────────────────────────────
                Arguments.of("MAX_VALUE: 3 <= 5 → 성공", S2RuleType.MAX_VALUE, 5, 3, true),
                Arguments.of("MAX_VALUE: 10 > 5 → 실패", S2RuleType.MAX_VALUE, 5, 10, false),
                Arguments.of("MAX_VALUE: 빈값 → 성공(skip)", S2RuleType.MAX_VALUE, 5, null, true),

                // ── REGEX ────────────────────────────────────────────────
                Arguments.of("REGEX: 숫자 패턴 매칭 → 성공", S2RuleType.REGEX, "^[0-9]+$", "12345", true),
                Arguments.of("REGEX: 문자 포함 → 실패", S2RuleType.REGEX, "^[0-9]+$", "12a45", false),
                Arguments.of("REGEX: 빈값 → 성공(skip)", S2RuleType.REGEX, "^[0-9]+$", null, true),

                // ── NUMBER ───────────────────────────────────────────────
                Arguments.of("NUMBER: 숫자만 → 성공", S2RuleType.NUMBER, null, "12345", true),
                Arguments.of("NUMBER: 문자 포함 → 실패", S2RuleType.NUMBER, null, "123a5", false),

                // ── TEXT_INTACT ───────────────────────────────────────────
                Arguments.of("TEXT_INTACT: 한글+영문 → 성공", S2RuleType.TEXT_INTACT, null, "홍길동abc", true),
                Arguments.of("TEXT_INTACT: 숫자 포함 → 실패", S2RuleType.TEXT_INTACT, null, "홍길동123", false),

                // ── EMAIL ────────────────────────────────────────────────
                Arguments.of("EMAIL: 유효한 이메일 → 성공", S2RuleType.EMAIL, null, "test@example.com", true),
                Arguments.of("EMAIL: @ 없음 → 실패", S2RuleType.EMAIL, null, "testexample.com", false),

                // ── MPHONE_NO ────────────────────────────────────────────
                Arguments.of("MPHONE_NO: 010-1234-5678 → 성공", S2RuleType.MPHONE_NO, null, "010-1234-5678", true),
                Arguments.of("MPHONE_NO: 잘못된 형식 → 실패", S2RuleType.MPHONE_NO, null, "02-1234-5678", false),

                // ── TEL_NO ───────────────────────────────────────────────
                Arguments.of("TEL_NO: 02-1234-5678 → 성공", S2RuleType.TEL_NO, null, "02-1234-5678", true),
                Arguments.of("TEL_NO: 잘못된 형식 → 실패", S2RuleType.TEL_NO, null, "1234", false),

                // ── INTERNATIONAL_TEL_NO ─────────────────────────────────
                Arguments.of("INTERNATIONAL_TEL_NO: +82 10 1234 5678 → 성공", S2RuleType.INTERNATIONAL_TEL_NO, null, "+82 10 1234 5678", true),
                Arguments.of("INTERNATIONAL_TEL_NO: 국제형식 아님 → 실패", S2RuleType.INTERNATIONAL_TEL_NO, null, "010-1234-5678", false),

                // ── ZIP ──────────────────────────────────────────────────
                Arguments.of("ZIP: 5자리 → 성공", S2RuleType.ZIP, null, "12345", true),
                Arguments.of("ZIP: 6자리 → 실패", S2RuleType.ZIP, null, "123456", false),

                // ── LOGIN_ID ─────────────────────────────────────────────
                Arguments.of("LOGIN_ID: 유효한 ID → 성공", S2RuleType.LOGIN_ID, null, "admin01", true),
                Arguments.of("LOGIN_ID: 숫자 시작 → 실패", S2RuleType.LOGIN_ID, null, "1admin", false),

                // ── PASSWORD ─────────────────────────────────────────────
                Arguments.of("PASSWORD: 유효한 비밀번호 → 성공", S2RuleType.PASSWORD, null, "Pass1234!", true),
                Arguments.of("PASSWORD: 특수문자 없음 → 실패", S2RuleType.PASSWORD, null, "Pass1234", false),

                // ── PASSWORD_ANSWR ───────────────────────────────────────
                Arguments.of("PASSWORD_ANSWR: 유효한 답변 → 성공", S2RuleType.PASSWORD_ANSWR, null, "서울특별시 강남구 역삼동", true),
                Arguments.of("PASSWORD_ANSWR: 너무 짧음 → 실패", S2RuleType.PASSWORD_ANSWR, null, "짧은답", false),

                // ── BIZRNO ───────────────────────────────────────────────
                Arguments.of("BIZRNO: 하이픈 형식 → 성공", S2RuleType.BIZRNO, null, "123-45-67890", true),
                Arguments.of("BIZRNO: 10자리 숫자 → 성공", S2RuleType.BIZRNO, null, "1234567890", true),
                Arguments.of("BIZRNO: 잘못된 형식 → 실패", S2RuleType.BIZRNO, null, "12345", false),

                // ── NWINO ────────────────────────────────────────────────
                Arguments.of("NWINO: 하이픈 형식 → 성공", S2RuleType.NWINO, null, "123-45-67890-1", true),
                Arguments.of("NWINO: 11자리 숫자 → 성공", S2RuleType.NWINO, null, "12345678901", true),
                Arguments.of("NWINO: 잘못된 형식 → 실패", S2RuleType.NWINO, null, "12345", false),

                // ── JUMIN ────────────────────────────────────────────────
                // 테스트용 유효한 주민번호 (체크섬 통과하는 가상번호: 900101-1234568)
                Arguments.of("JUMIN: 유효한 번호 → 성공", S2RuleType.JUMIN, null, "9001011234568", true),
                Arguments.of("JUMIN: 하이픈 포함 유효한 번호 → 성공", S2RuleType.JUMIN, null, "900101-1234568", true),
                Arguments.of("JUMIN: 체크섬 불일치 → 실패", S2RuleType.JUMIN, null, "9001011234560", false),
                Arguments.of("JUMIN: 빈값 → 성공(skip)", S2RuleType.JUMIN, null, null, true),

                // ── DATE ─────────────────────────────────────────────────
                Arguments.of("DATE: yyyyMMdd → 성공", S2RuleType.DATE, null, "20240101", true),
                Arguments.of("DATE: yyyy-MM-dd → 성공", S2RuleType.DATE, null, "2024-01-01", true),
                Arguments.of("DATE: 존재하지 않는 날짜 → 실패", S2RuleType.DATE, null, "20240230", false),
                Arguments.of("DATE: 빈값 → 성공(skip)", S2RuleType.DATE, null, null, true),

                // ── TEXT_COMBINE ──────────────────────────────────────────
                Arguments.of("TEXT_COMBINE: 한글+숫자 → 성공", S2RuleType.TEXT_COMBINE, null, "홍길동123", true),
                Arguments.of("TEXT_COMBINE: 특수문자 → 실패", S2RuleType.TEXT_COMBINE, null, "홍길동@#$", false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    @DisplayName("서버-클라이언트 검증 결과 일치 확인")
    void testServerClientParity(String description, S2RuleType ruleType, Object checkValue,
            Object testValue, boolean expectedResult) {
        // ── 서버 측 검증 ──
        boolean serverResult = serverValidate(ruleType, checkValue, testValue);

        // ── 클라이언트(JS) 측 검증 ──
        boolean clientResult = clientValidate(ruleType, checkValue, testValue);

        // ── 양쪽 결과가 기대값과 일치하는지 확인 ──
        Assertions.assertEquals(expectedResult, serverResult,
                "[서버] " + description);
        Assertions.assertEquals(expectedResult, clientResult,
                "[클라이언트] " + description);
        Assertions.assertEquals(serverResult, clientResult,
                "[일관성] 서버와 클라이언트 결과가 다름: " + description
                        + " (서버=" + serverResult + ", 클라이언트=" + clientResult + ")");
    }

    // ========================================================================================
    // 빈 값 정책 일관성 전용 테스트 — 서버·클라이언트 모두 빈 값은 REQUIRED/ASSERT 외 통과
    // ========================================================================================

    static Stream<Arguments> emptyValueTestCases() {
        return Stream.of(
                // REQUIRED, ASSERT_TRUE, ASSERT_FALSE는 빈 값에서도 검증 수행
                Arguments.of("REQUIRED", S2RuleType.REQUIRED, null, null, false),
                Arguments.of("ASSERT_TRUE", S2RuleType.ASSERT_TRUE, null, null, false),
                Arguments.of("ASSERT_FALSE", S2RuleType.ASSERT_FALSE, null, null, true),
                // 나머지는 빈 값이면 무조건 통과(skip)
                Arguments.of("LENGTH", S2RuleType.LENGTH, 5, null, true),
                Arguments.of("MIN_LENGTH", S2RuleType.MIN_LENGTH, 3, null, true),
                Arguments.of("MAX_LENGTH", S2RuleType.MAX_LENGTH, 5, null, true),
                Arguments.of("MIN_BYTE", S2RuleType.MIN_BYTE, 3, null, true),
                Arguments.of("MAX_BYTE", S2RuleType.MAX_BYTE, 5, null, true),
                Arguments.of("MIN_VALUE", S2RuleType.MIN_VALUE, 5, null, true),
                Arguments.of("MAX_VALUE", S2RuleType.MAX_VALUE, 5, null, true),
                Arguments.of("REGEX", S2RuleType.REGEX, "^[0-9]+$", null, true),
                Arguments.of("NUMBER", S2RuleType.NUMBER, null, null, true),
                Arguments.of("EMAIL", S2RuleType.EMAIL, null, null, true),
                Arguments.of("DATE", S2RuleType.DATE, null, null, true),
                Arguments.of("JUMIN", S2RuleType.JUMIN, null, null, true)
        );
    }

    @ParameterizedTest(name = "빈 값 정책: {0}")
    @MethodSource("emptyValueTestCases")
    @DisplayName("빈 값(null) 정책이 서버-클라이언트 동일")
    void testEmptyValuePolicyParity(String ruleName, S2RuleType ruleType, Object checkValue,
            Object testValue, boolean expectedResult) {
        boolean serverResult = serverValidate(ruleType, checkValue, testValue);
        boolean clientResult = clientValidate(ruleType, checkValue, testValue);

        Assertions.assertEquals(serverResult, clientResult,
                "[빈 값 정책 불일치] " + ruleName + ": 서버=" + serverResult + ", 클라이언트=" + clientResult);
        Assertions.assertEquals(expectedResult, serverResult,
                "[빈 값 정책] 서버 결과가 기대와 다름: " + ruleName);
    }

    // ========================================================================================
    // 교차 필드 검증 일관성 테스트 (EQUALS_FIELD, DATE_AFTER, DATE_BEFORE)
    // ========================================================================================

    @org.junit.jupiter.api.Test
    @DisplayName("교차 필드 검증(EQUALS_FIELD, DATE_AFTER, DATE_BEFORE) 서버-클라이언트 일치 확인")
    void testCrossFieldParity() {
        Map<String, Object> data = new HashMap<>();
        data.put("password", "Secret123!");
        data.put("confirmPassword", "Secret123!");
        data.put("wrongPassword", "Wrong123!");
        data.put("startDate", "2024-01-01");
        data.put("endDate", "2024-01-05");
        data.put("pastDate", "2023-12-31");

        // EQUALS_FIELD 일치 → 성공
        assertCrossFieldParity(S2RuleType.EQUALS_FIELD, "password", "Secret123!", data, true);
        // EQUALS_FIELD 불일치 → 실패
        assertCrossFieldParity(S2RuleType.EQUALS_FIELD, "wrongPassword", "Secret123!", data, false);

        // DATE_AFTER: 2024-01-05 >= startDate(2024-01-01) → 성공
        assertCrossFieldParity(S2RuleType.DATE_AFTER, "startDate", "2024-01-05", data, true);
        // DATE_AFTER: 2023-12-31 >= startDate(2024-01-01) → 실패
        assertCrossFieldParity(S2RuleType.DATE_AFTER, "startDate", "2023-12-31", data, false);

        // DATE_BEFORE: 2024-01-01 <= endDate(2024-01-05) → 성공
        assertCrossFieldParity(S2RuleType.DATE_BEFORE, "endDate", "2024-01-01", data, true);
        // DATE_BEFORE: 2024-01-05 <= pastDate(2023-12-31) → 실패
        assertCrossFieldParity(S2RuleType.DATE_BEFORE, "pastDate", "2024-01-05", data, false);
    }

    private void assertCrossFieldParity(S2RuleType ruleType, String targetField, Object value,
            Map<String, Object> formData, boolean expected) {
        // 서버 측 검증
        S2Rule rule = new S2Rule(ruleType, targetField);
        boolean serverResult = rule.isValid(value, formData);

        // 클라이언트(JS) 측 검증
        Map<String, Object> jsRule = new HashMap<>();
        jsRule.put("type", ruleType.name());
        jsRule.put("value", targetField);
        jsRule.put("message", "test");
        boolean clientResult = validateCheckFn.execute(value, jsRule, formData, "", "currentField").asBoolean();

        Assertions.assertEquals(expected, serverResult, "[서버 교차필드] " + ruleType + " (" + targetField + ")");
        Assertions.assertEquals(expected, clientResult, "[클라이언트 교차필드] " + ruleType + " (" + targetField + ")");
        Assertions.assertEquals(serverResult, clientResult, "[일관성 교차필드] " + ruleType);
    }

    // ========================================================================================
    // JSON 구조 일관성 테스트 — getRulesJson이 JS가 기대하는 구조를 생성하는지
    // ========================================================================================

    @org.junit.jupiter.api.Test
    @DisplayName("getRulesJson이 JS validateCheck가 기대하는 JSON 키를 모두 포함한다")
    void testRulesJsonStructure() {
        S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
                .field("email", "이메일").rule(S2RuleType.REQUIRED).rule(S2RuleType.EMAIL)
                .field("name", "이름").rule(S2RuleType.MIN_LENGTH, 2)
                .field("age", "나이").rule(S2RuleType.MIN_VALUE, 0).rule(S2RuleType.MAX_VALUE, 200)
                .build();

        String json = S2ValidatorFactory.getRulesJson(validator, java.util.Locale.KOREAN);

        // JS에서 JSON을 파싱하고 구조를 검증
        jsContext.eval("js", "var __testRules = " + json + ";");
        Value rules = jsContext.eval("js", "__testRules");

        Assertions.assertTrue(rules.hasArrayElements(), "JSON은 배열이어야 함");
        Assertions.assertEquals(3, rules.getArraySize(), "필드 3개");

        // 첫 번째 필드(email)의 구조 확인
        Value emailField = rules.getArrayElement(0);
        Assertions.assertTrue(emailField.hasMember("name"), "name 키 필요");
        Assertions.assertTrue(emailField.hasMember("label"), "label 키 필요");
        Assertions.assertTrue(emailField.hasMember("rules"), "rules 키 필요");
        Assertions.assertEquals("email", emailField.getMember("name").asString());

        // 규칙 객체의 구조 확인 (type, regex, message, value 키)
        Value firstRule = emailField.getMember("rules").getArrayElement(0);
        Assertions.assertTrue(firstRule.hasMember("type"), "type 키 필요");
        Assertions.assertTrue(firstRule.hasMember("message"), "message 키 필요");
        Assertions.assertEquals("REQUIRED", firstRule.getMember("type").asString());
    }

    // ========================================================================================
    // 내부 유틸리티
    // ========================================================================================

    /**
     * 서버 측(Java) 검증: S2Rule.isValid() 직접 호출
     */
    private boolean serverValidate(S2RuleType ruleType, Object checkValue, Object value) {
        S2Rule rule;
        if (checkValue != null) {
            rule = new S2Rule(ruleType, checkValue);
        } else {
            rule = new S2Rule(ruleType);
        }
        return rule.isValid(value, null);
    }

    /**
     * 클라이언트 측(JS) 검증: GraalJS에서 validateCheck() 직접 호출
     */
    private boolean clientValidate(S2RuleType ruleType, Object checkValue, Object testValue) {
        // JS 규칙 객체 구성: { type: "REQUIRED", value: ..., regex: ..., message: "test" }
        Map<String, Object> jsRule = new HashMap<>();
        jsRule.put("type", ruleType.name());

        if (ruleType == S2RuleType.REGEX) {
            jsRule.put("value", checkValue);
            jsRule.put("regex", null);
        } else {
            jsRule.put("value", checkValue);
            jsRule.put("regex", ruleType.getRegex());
        }
        jsRule.put("message", "test");

        // formData (cross-field 미사용)
        Map<String, Object> formData = new HashMap<>();

        return validateCheckFn.execute(testValue, jsRule, formData, "", "testField").asBoolean();
    }
}
