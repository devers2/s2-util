package io.github.devers2.s2util.validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.json.S2JsonUtil;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * {@link S2BeanValidation}으로 만든 규칙이 실제 Bean Validation(Hibernate Validator)보다 엄격하지 않은지 무작위 입력으로 확인하는 시험.
 * <p>
 * 원칙: S2(서버)가 실패로 본 필드는 Bean Validation 도 실패여야 한다(브라우저가 서버가 받을 값을 막으면 안 됨). 또한 변환한 규칙의 서버 판정과
 * 브라우저({@code s2.validator.js} {@code check()}) 판정이 같아야 한다. 문서에 적은 알려진 예외(앞뒤 공백이 있는 값의 최소 길이, NBSP 같은
 * 유니코드 공백)는 따로 걸러 확인한다.
 * </p>
 */
class BeanValidationParityFuzzTest {

    private static final long SEED = 20261120L;
    private static final int SAMPLES = 3000;

    private static final String[] PIECES = { "a", "z", "Z", "0", "5", "9", "12", "99", "99.5", "100", "-1", "1e3", ".5",
            "5.", "+5", "가", " ", "\t", " ", "@", "x@y", "a@b.c", ".", "-", "_", "abc", "abcdefgh" };

    public static class Form {
        @NotBlank
        @Size(max = 8)
        public String name;

        @Size(min = 2, max = 6)
        @Pattern(regexp = "[a-z0-9]*")
        public String code;

        @NotNull
        @Min(1)
        @Max(120)
        public Integer age;

        @DecimalMin("0")
        @DecimalMax("99.5")
        public String amount;

        @Email(regexp = "[^@\\s]+@[^@\\s]+")
        public String email;

        @Size(max = 2)
        public List<String> tags;

        @AssertTrue
        public boolean agree;

        @PositiveOrZero
        public Integer point;
    }

    private static Context jsContext;
    private static Value check;
    private static ValidatorFactory factory;

    @BeforeAll
    static void setup() throws IOException {
        factory = Validation.buildDefaultValidatorFactory();
        jsContext = Context.newBuilder("js").allowHostAccess(HostAccess.ALL)
                .option("engine.WarnInterpreterOnly", "false").build();
        String source;
        try (InputStream is = BeanValidationParityFuzzTest.class
                .getResourceAsStream("/META-INF/resources/s2-util/js/s2.validator.js")) {
            Assertions.assertNotNull(is);
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        source = source.replaceAll("export\\s+const\\s+", "const ").replace("initS2Validator();", "");
        jsContext.eval("js", "var console = { log() {}, warn() {}, error() {} };");
        jsContext.eval("js", source + "\nglobalThis.__check = (r, d) => S2Validator.check(r, JSON.parse(d));\n");
        check = jsContext.eval("js", "globalThis.__check");
    }

    @AfterAll
    static void tearDown() {
        if (jsContext != null) {
            jsContext.close();
        }
        if (factory != null) {
            factory.close();
        }
    }

    private static String randomText(Random random) {
        if (random.nextInt(5) == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int parts = random.nextInt(4);
        for (int i = 0; i < parts; i++) {
            sb.append(PIECES[random.nextInt(PIECES.length)]);
        }
        return sb.toString();
    }

    private static Form randomForm(Random random) {
        Form form = new Form();
        form.name = randomText(random);
        form.code = randomText(random);
        form.age = random.nextInt(4) == 0 ? null : random.nextInt(140) - 10;
        form.amount = randomText(random);
        form.email = randomText(random);
        if (random.nextBoolean()) {
            List<String> tags = new ArrayList<>();
            int size = random.nextInt(4);
            for (int i = 0; i < size; i++) {
                tags.add(PIECES[random.nextInt(PIECES.length)]);
            }
            form.tags = tags;
        }
        form.agree = random.nextBoolean();
        form.point = random.nextInt(3) == 0 ? null : random.nextInt(20) - 10;
        return form;
    }

    private static Map<String, Object> toData(Form form) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", form.name);
        data.put("code", form.code);
        data.put("age", form.age);
        data.put("amount", form.amount);
        data.put("email", form.email);
        data.put("tags", form.tags);
        data.put("agree", form.agree);
        data.put("point", form.point);
        return data;
    }

    /** 문서에 적은 알려진 예외: S2 는 앞뒤 공백(유니코드 공백 포함)을 빼고 최소 길이·필수를 판정 */
    private static boolean knownException(String field, Form form) {
        Object value = toData(form).get(field);
        return value instanceof String text && !S2Whitespace.trim(text).equals(text);
    }

    @Test
    @DisplayName("변환한 규칙은 Bean Validation 보다 엄격하지 않고, 서버와 브라우저 판정이 같다")
    void convertedRulesAreNeverStricterThanBeanValidation() {
        S2Validator<Form> rules = S2BeanValidation.of(Form.class).build();
        String rulesJson = rules.getRulesJson(Locale.KOREAN);
        jakarta.validation.Validator beanValidator = factory.getValidator();
        Random random = new Random(SEED);
        List<String> stricter = new ArrayList<>();
        List<String> mismatches = new ArrayList<>();
        int compared = 0;

        for (int i = 0; i < SAMPLES; i++) {
            Form form = randomForm(random);
            Set<String> beanInvalid = beanValidator.validate(form).stream()
                    .map(v -> v.getPropertyPath().toString()).collect(Collectors.toCollection(TreeSet::new));
            Set<String> s2Invalid = new TreeSet<>();
            rules.validate(form, e -> s2Invalid.add(e.fieldName()), Locale.KOREAN);
            String dataJson = S2JsonUtil.toJson(toData(form));
            Set<String> jsInvalid = new TreeSet<>(check.execute(rulesJson, dataJson).getMemberKeys());

            if (!s2Invalid.equals(jsInvalid)) {
                mismatches.add(dataJson + " server=" + s2Invalid + " js=" + jsInvalid);
            }
            for (String field : s2Invalid) {
                if (!beanInvalid.contains(field) && !knownException(field, form)) {
                    stricter.add(field + " " + dataJson + " bean=" + beanInvalid);
                }
            }
            compared++;
        }

        Assertions.assertEquals(SAMPLES, compared);
        Assertions.assertTrue(mismatches.isEmpty(), "서버·브라우저 불일치 " + mismatches.size() + "건:\n"
                + String.join("\n", mismatches.subList(0, Math.min(10, mismatches.size()))));
        Assertions.assertTrue(stricter.isEmpty(), "Bean Validation 보다 엄격 " + stricter.size() + "건:\n"
                + String.join("\n", stricter.subList(0, Math.min(10, stricter.size()))));
    }
}
