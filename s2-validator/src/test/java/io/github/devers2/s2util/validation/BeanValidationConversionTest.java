package io.github.devers2.s2util.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * {@link S2BeanValidation} 변환 대응표 시험.
 */
class BeanValidationConversionTest {

    interface AdminOnly {
    }

    public static class Address {
        @NotBlank
        public String zip;
    }

    public static class Item {
        @NotBlank
        public String name;
        @Min(1)
        public Integer qty;
    }

    public static class Node {
        @NotBlank
        public String title;
        @Valid
        public Node parent;
    }

    public static class JoinForm {
        @NotBlank
        @Size(max = 20)
        public String name;

        @NotNull
        @Size(min = 2, max = 10)
        public String nickname;

        @NotEmpty
        public String memo;

        @Email
        public String email;

        @Email(regexp = "[^@]+@example\\.com")
        public String workEmail;

        @Pattern(regexp = "[0-9]{3}-[0-9]{4}")
        public String code;

        @Pattern(regexp = "abc", flags = Pattern.Flag.CASE_INSENSITIVE)
        public String caseless;

        @Pattern(regexp = "\\Aabc\\z")
        public String javaOnly;

        @NotNull
        @Min(1)
        @Max(120)
        public Integer age;

        @DecimalMin("0.5")
        @DecimalMax(value = "100", inclusive = false)
        public String amount;

        @PositiveOrZero
        public int point;

        @NegativeOrZero
        public int debt;

        @Positive
        public int count;

        @Size(min = 1, max = 3)
        public List<String> tags;

        @AssertTrue
        public boolean agree;

        @AssertTrue
        public Boolean optionalAgree;

        @AssertFalse
        public Boolean blocked;

        @Past
        public java.time.LocalDate birthDate;

        @Digits(integer = 3, fraction = 0)
        public String digits;

        @NotBlank(groups = AdminOnly.class)
        public String adminNote;

        @Valid
        public Address address;

        public List<@Valid Item> items;

        public List<@NotBlank String> keywords;

        @Valid
        public Node node;

        @Size(min = 2, message = "이름은 {min}자 이상")
        public String withMessage;
    }

    public record Point(@NotBlank String label, @Min(0) int x) {
    }

    /** 필드별 규칙을 "필드: 규칙(기준값)" 목록으로 */
    private static Map<String, String> describe(S2Validator<?> validator) {
        return validator.getFields().stream().collect(Collectors.toMap(f -> String.valueOf(f.getName()),
                f -> f.getRules().stream()
                        .map(r -> r.getRuleType() + (r.getCheckValue() == null || r.getCheckValue() instanceof S2Validator
                                ? "" : "(" + r.getCheckValue() + ")"))
                        .collect(Collectors.joining(",")),
                (a, b) -> a, java.util.LinkedHashMap::new));
    }

    @Test
    @DisplayName("안전 모드: BV 보다 엄격하지 않은 제약만 변환한다")
    void safeModeConvertsOnlyNeverStricterConstraints() {
        Map<String, String> rules = describe(S2BeanValidation.of(JoinForm.class).build());

        Assertions.assertEquals("REQUIRED,MAX_LENGTH(20)", rules.get("name"));
        Assertions.assertEquals("MIN_LENGTH(2),MAX_LENGTH(10)", rules.get("nickname"), "문자열 @NotNull 은 제외");
        Assertions.assertNull(rules.get("memo"), "@NotEmpty 는 제외");
        Assertions.assertNull(rules.get("email"), "기본 @Email 은 제외");
        Assertions.assertEquals("REGEX([^@]+@example\\.com)", rules.get("workEmail"), "@Email(regexp) 은 REGEX");
        Assertions.assertEquals("REGEX([0-9]{3}-[0-9]{4})", rules.get("code"));
        Assertions.assertNull(rules.get("caseless"), "flags 가 있는 @Pattern 은 제외");
        Assertions.assertNull(rules.get("javaOnly"), "Java 전용 정규식은 제외");
        Assertions.assertEquals("REQUIRED,MIN_VALUE(1),MAX_VALUE(120)", rules.get("age"), "숫자 @NotNull 은 REQUIRED");
        Assertions.assertEquals("MIN_VALUE(0.5)", rules.get("amount"), "경계 미포함 @DecimalMax 는 제외");
        Assertions.assertEquals("MIN_VALUE(0)", rules.get("point"));
        Assertions.assertEquals("MAX_VALUE(0)", rules.get("debt"));
        Assertions.assertNull(rules.get("count"), "@Positive(경계 미포함)는 제외");
        Assertions.assertEquals("MIN_SIZE(1),MAX_SIZE(3)", rules.get("tags"), "컬렉션 @Size 는 개수");
        Assertions.assertEquals("ASSERT_TRUE", rules.get("agree"), "primitive boolean 의 @AssertTrue");
        Assertions.assertNull(rules.get("optionalAgree"), "null 가능한 Boolean 의 @AssertTrue 는 제외");
        Assertions.assertEquals("ASSERT_FALSE", rules.get("blocked"));
        Assertions.assertNull(rules.get("birthDate"));
        Assertions.assertNull(rules.get("digits"));
        Assertions.assertNull(rules.get("adminNote"), "Default 가 아닌 그룹은 제외");
        Assertions.assertEquals("NESTED", rules.get("address"));
        Assertions.assertEquals("EACH", rules.get("items"), "List<@Valid Item> 은 EACH");
        Assertions.assertNull(rules.get("keywords"), "컨테이너 요소 제약은 제외");
        Assertions.assertEquals("NESTED", rules.get("node"), "자기 참조는 한 단계만 펼침");
    }

    @Test
    @DisplayName("실용 모드: @NotEmpty·문자열 @NotNull·@Email 도 변환한다")
    void practicalModeAddsMoreMappings() {
        Map<String, String> rules = describe(S2BeanValidation.of(JoinForm.class).practical().build());

        Assertions.assertEquals("REQUIRED,MIN_LENGTH(2),MAX_LENGTH(10)", rules.get("nickname"));
        Assertions.assertEquals("REQUIRED", rules.get("memo"));
        Assertions.assertEquals("EMAIL", rules.get("email"));
        Assertions.assertEquals("REGEX([^@]+@example\\.com),EMAIL", rules.get("workEmail"));
    }

    @Test
    @DisplayName("서버 전용 목록: 변환하지 않은 제약을 경로와 어노테이션으로 알려 준다")
    void serverOnlyListsUnconvertedConstraints() {
        List<String> serverOnly = S2BeanValidation.of(JoinForm.class).serverOnly();

        for (String expected : List.of("memo @NotEmpty", "email @Email", "caseless @Pattern", "javaOnly @Pattern",
                "amount @DecimalMax", "count @Positive", "optionalAgree @AssertTrue", "birthDate @Past", "digits @Digits",
                "keywords <element constraints>", "node.parent @Valid (recursive)")) {
            Assertions.assertTrue(serverOnly.contains(expected), expected + " 없음: " + serverOnly);
        }
        Assertions.assertFalse(serverOnly.stream().anyMatch(s -> s.startsWith("adminNote")), "다른 그룹은 목록에도 없음");
    }

    @Test
    @DisplayName("중첩 경로·행 규칙이 실제로 검증되고 라벨·직접 쓴 메시지가 적용된다")
    void nestedRulesLabelsAndMessages() {
        S2Validator<JoinForm> validator = S2BeanValidation.of(JoinForm.class)
                .labels(Map.of("name", "이름"))
                .labelResolver(path -> path.equals("address.zip") ? "우편번호" : null)
                .build();
        JoinForm form = new JoinForm();
        form.agree = true;
        form.address = new Address();
        Item item = new Item();
        item.qty = 0;
        form.items = List.of(item);
        form.withMessage = "a";
        List<String> errors = new ArrayList<>();

        validator.validate(form, e -> errors.add(e.fieldName() + "=" + e.defaultMessage()), Locale.KOREAN);

        Assertions.assertTrue(errors.contains("name=이름은 필수 입력 항목입니다."), errors.toString());
        Assertions.assertTrue(errors.contains("address.zip=우편번호는 필수 입력 항목입니다."), errors.toString());
        Assertions.assertTrue(errors.contains("items[0].name=name은(는) 필수 입력 항목입니다."), "라벨이 없으면 필드명: " + errors);
        Assertions.assertTrue(errors.stream().anyMatch(e -> e.startsWith("items[0].qty=")), errors.toString());
        Assertions.assertTrue(errors.contains("withMessage=이름은 2자 이상"), "{min} 치환: " + errors);
    }

    @Test
    @DisplayName("record 구성요소를 읽고, 접근자 메서드를 getter 제약으로 보고하지 않는다")
    void recordComponents() {
        S2BeanValidation<Point> conversion = S2BeanValidation.of(Point.class);

        Assertions.assertEquals(Map.of("label", "REQUIRED", "x", "MIN_VALUE(0)"), describe(conversion.build()));
        Assertions.assertEquals(List.of(), conversion.serverOnly());
    }

    @Test
    @DisplayName("toBuilder 로 S2 규칙을 이어 쓰고, 규칙 JSON 에 변환 규칙이 들어간다")
    void continueWithS2Rules() {
        S2Validator<Point> validator = S2BeanValidation.of(Point.class)
                .toBuilder()
                .field("label", "라벨").rule(S2RuleType.MAX_LENGTH, 5)
                .build();

        Assertions.assertEquals(3, validator.getFields().size());
        String json = validator.getRulesJson(Locale.KOREAN);
        Assertions.assertTrue(json.contains("\"MIN_VALUE\"") && json.contains("\"MAX_LENGTH\""), json);
    }
}
