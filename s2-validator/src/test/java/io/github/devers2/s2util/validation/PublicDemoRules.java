package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.devers2.s2util.json.S2JsonUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 공개 데모 사이트(GitHub Pages, {@code browser-test/site})의 규칙 JSON 을 실제 서버 검증기로 만든다. 페이지에 보이는 Java 코드와 같은
 * 규칙이며, {@code ./gradlew :s2-validator:writePublicDemoRules}로 {@code site/rules.json}을 다시 만든다.
 */
public final class PublicDemoRules {

    static final Path OUTPUT = Path.of("browser-test", "site", "rules.json");

    private PublicDemoRules() {
    }

    /** Bean Validation 연결 예시 DTO (페이지의 "Bean Validation" 절과 같음) */
    public static class ProfileForm {
        @NotBlank
        @Size(max = 10)
        public String nickname;

        @NotNull
        @Min(14)
        @Max(120)
        public Integer age;

        @Pattern(regexp = "[0-9]{5}")
        public String zip;

        @Size(min = 1, max = 2)
        public List<String> tags;

        @Past
        public LocalDate birthDate;
    }

    static S2Validator<Map<String, Object>> join(boolean en) {
        return S2Validator.<Map<String, Object>>builder()
                .field("name", l("이름", "Name", en)).rule(S2RuleType.REQUIRED).rule(S2RuleType.MAX_LENGTH, 20)
                .field("email", l("이메일", "E-mail", en)).rule(S2RuleType.REQUIRED).rule(S2RuleType.EMAIL)
                .field("mobile", l("휴대폰 번호", "Mobile", en)).rule(S2RuleType.REQUIRED).rule(S2RuleType.MPHONE_NO)
                .field("password", l("비밀번호", "Password", en)).rule(S2RuleType.REQUIRED).rule(S2RuleType.PASSWORD)
                .field("passwordConfirm", l("비밀번호 확인", "Password confirmation", en)).rule(S2RuleType.REQUIRED).rule(S2RuleType.EQUALS_FIELD, "password")
                .field("memberType", l("회원 유형", "Member type", en)).rule(S2RuleType.REQUIRED)
                .field("bizNo", l("사업자등록번호", "Business No.", en)).when("memberType", "BUSINESS")
                        .rule(S2RuleType.REQUIRED).rule(S2RuleType.BIZRNO, true)
                .field("interests", l("관심사", "Interests", en)).rule(S2RuleType.MIN_SIZE, 1).rule(S2RuleType.MAX_SIZE, 3)
                .field("agree", l("이용약관 동의", "Terms", en)).rule(S2RuleType.ASSERT_TRUE)
                .build();
    }

    static S2Validator<Map<String, Object>> order(boolean en) {
        return S2Validator.<Map<String, Object>>builder()
                .field("items[].name", l("품목명", "Item", en)).rule(S2RuleType.REQUIRED)
                .field("items[].qty", l("수량", "Quantity", en)).rule(S2RuleType.REQUIRED).rule(S2RuleType.MIN_VALUE, 1).rule(S2RuleType.MAX_VALUE, 99)
                .field("startDate", l("시작일", "Start date", en)).rule(S2RuleType.REQUIRED).rule(S2RuleType.DATE)
                .field("endDate", l("종료일", "End date", en)).rule(S2RuleType.REQUIRED).rule(S2RuleType.DATE)
                        .rule(S2RuleType.DATE_AFTER, "startDate")
                .build();
    }

    static S2BeanValidation<ProfileForm> profile(boolean en) {
        return S2BeanValidation.of(ProfileForm.class)
                .labels(en ? Map.of("nickname", "Nickname", "age", "Age", "zip", "ZIP code", "tags", "Tags")
                        : Map.of("nickname", "닉네임", "age", "나이", "zip", "우편번호", "tags", "태그"));
    }

    /** 언어별 라벨 */
    private static String l(String ko, String en, boolean english) {
        return english ? en : ko;
    }

    static String render() {
        Map<String, String> entries = new LinkedHashMap<>();
        for (Locale locale : List.of(Locale.KOREAN, Locale.ENGLISH)) {
            String lang = locale.getLanguage();
            boolean en = locale == Locale.ENGLISH;
            entries.put("join." + lang, join(en).getRulesJson(locale));
            entries.put("order." + lang, order(en).getRulesJson(locale));
            entries.put("profile." + lang, profile(en).build().getRulesJson(locale));
        }
        entries.put("profile.serverOnly", S2JsonUtil.toJson(profile(false).serverOnly()));
        StringBuilder sb = new StringBuilder("{\n");
        var it = entries.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            sb.append("  \"").append(e.getKey()).append("\": ").append(e.getValue()).append(it.hasNext() ? ",\n" : "\n");
        }
        return sb.append("}\n").toString();
    }

    public static void main(String[] args) throws IOException {
        Files.createDirectories(OUTPUT.getParent());
        Files.writeString(OUTPUT, render(), StandardCharsets.UTF_8);
        System.out.println("Wrote " + OUTPUT.toAbsolutePath());
    }
}
