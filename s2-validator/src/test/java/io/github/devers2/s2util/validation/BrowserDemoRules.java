package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the rules JSON for the browser test pages ({@code s2-validator/browser-test/demo}) from real server
 * validators, so the browser tests run the exact JSON the server would send.
 * <p>
 * Regenerate with {@code ./gradlew :s2-validator:writeBrowserDemoRules}; {@code BrowserDemoRulesTest} fails when the
 * committed file is out of date.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 브라우저 시험 페이지({@code s2-validator/browser-test/demo})용 규칙 JSON 을 실제 서버 검증기로 만듭니다. 브라우저 시험이 서버가 보낼 JSON 을
 * 그대로 쓰게 하기 위함입니다.
 * <p>
 * {@code ./gradlew :s2-validator:writeBrowserDemoRules}로 다시 만들며, 커밋된 파일이 낡으면 {@code BrowserDemoRulesTest}가 실패합니다.
 * </p>
 */
public final class BrowserDemoRules {

    /** Output path relative to the s2-validator module | s2-validator 모듈 기준 출력 경로 */
    static final Path OUTPUT = Path.of("browser-test", "demo", "rules.json");

    private BrowserDemoRules() {
    }

    /**
     * Returns the rules JSON of each demo form, keyed by form id.
     *
     * @return form id → rules JSON | 폼 id → 규칙 JSON
     */
    static Map<String, String> rulesByForm() {
        Map<String, String> forms = new LinkedHashMap<>();

        forms.put("basic", S2Validator.builder()
                .field("name", "이름").rule(S2RuleType.REQUIRED)
                .field("email", "이메일").rule(S2RuleType.REQUIRED).rule(S2RuleType.EMAIL)
                .field("qty", "수량").rule(S2RuleType.MAX_VALUE, 100)
                .field("code", "코드").rule(S2RuleType.MAX_LENGTH, 3)
                .build().getRulesJson(Locale.KOREAN));

        forms.put("hidden", S2Validator.builder()
                .field("token", "토큰").rule(S2RuleType.REQUIRED)
                .field("memo", "메모").rule(S2RuleType.REQUIRED)
                .field("grade", "등급").rule(S2RuleType.REQUIRED)
                .build().getRulesJson(Locale.KOREAN));

        forms.put("checkbox", S2Validator.builder()
                .field("agree", "이용약관").rule(S2RuleType.ASSERT_TRUE)
                .field("hobbies", "취미").rule(S2RuleType.REQUIRED)
                .build().getRulesJson(Locale.KOREAN));

        forms.put("rows", S2Validator.builder()
                .field("items[].name", "품목명").rule(S2RuleType.REQUIRED)
                .field("items[].qty", "수량").rule(S2RuleType.MIN_VALUE, 1)
                .build().getRulesJson(Locale.KOREAN));

        forms.put("renderer", S2Validator.builder()
                .field("name", "이름").rule(S2RuleType.REQUIRED)
                .field("email", "이메일").rule(S2RuleType.REQUIRED).rule(S2RuleType.EMAIL)
                .field("token", "토큰").rule(S2RuleType.REQUIRED)
                .build().getRulesJson(Locale.KOREAN));

        return forms;
    }

    /**
     * Renders all forms as one JSON object: {@code {"formId": [rules...], ...}}.
     *
     * @return The JSON document | JSON 문서
     */
    static String render() {
        StringBuilder sb = new StringBuilder("{\n");
        var entries = rulesByForm().entrySet().iterator();
        while (entries.hasNext()) {
            var e = entries.next();
            sb.append("  \"").append(e.getKey()).append("\": ").append(e.getValue());
            sb.append(entries.hasNext() ? ",\n" : "\n");
        }
        return sb.append("}\n").toString();
    }

    /**
     * Writes {@link #OUTPUT}; run from the s2-validator module directory.
     *
     * @param args Unused | 사용 안 함
     * @throws IOException If writing fails | 쓰기 실패 시
     */
    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT.getParent());
        Files.writeString(OUTPUT, render(), StandardCharsets.UTF_8);
        System.out.println("Wrote " + OUTPUT.toAbsolutePath());
    }
}
