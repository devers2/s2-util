package io.github.devers2.s2util.validation;

import java.util.Locale;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link S2RegexCompatibility} and its enforcement in {@link S2Validator#getRulesJson(Locale)}.
 */
public class S2RegexCompatibilityTest {

    @ParameterizedTest
    @ValueSource(strings = { "(?i)^abc$", "(?i:abc)", "(?-i)abc", "a++b", "a*+", "a?+", "a{2}+", "(?>ab)c",
            "\\p{Alpha}+", "\\P{Digit}", "\\Aabc\\z", "abc\\Z", "\\Qa.b\\E", "\\h+", "\\R", "[a-z&&[^aeiou]]", "[a-z[0-9]]" })
    void javaOnlySyntaxIsDetected(String pattern) {
        Assertions.assertNotNull(S2RegexCompatibility.findIncompatibility(pattern), pattern);
    }

    @ParameterizedTest
    @ValueSource(strings = { "^\\d+$", "\\d+", "\\++", "a+?", "a*?", "(?:ab)+", "(?=x)x", "(?!y)x", "(?<=x)y", "(?<!x)y",
            "(?<name>a)\\k<name>", "[]a]", "[^]a]", "[\\[\\]]", "\\\\p", "[+*?]+", "a{2,3}", "a{2}?", "^a|b$", "[가-힣]{2,5}" })
    void ecmaCompatibleSyntaxIsAccepted(String pattern) {
        Assertions.assertNull(S2RegexCompatibility.findIncompatibility(pattern), pattern);
        // Anything accepted must also compile in a flagless JS RegExp, wrapped as s2.validator.js does. | 허용한 패턴은 s2.validator.js 처럼 감싼 플래그 없는 JS RegExp 로도 컴파일되어야 함
        try (Context js = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
            js.getBindings("js").putMember("p", pattern);
            Assertions.assertDoesNotThrow(() -> js.eval("js", "new RegExp('^(?:' + p + ')$')"), pattern);
        }
    }

    @Test
    void javaOnlyPatternsDetectedHereReallyFailOrDifferInJs() {
        try (Context js = Context.newBuilder("js").option("engine.WarnInterpreterOnly", "false").build()) {
            for (String pattern : new String[] { "(?i)^abc$", "a++b", "(?>ab)c" }) {
                js.getBindings("js").putMember("p", pattern);
                Assertions.assertThrows(PolyglotException.class, () -> js.eval("js", "new RegExp('^(?:' + p + ')$')"), pattern);
            }
        }
    }

    @Test
    void builtInRuleRegexesAreClientCompatible() {
        for (S2RuleType type : S2RuleType.values()) {
            if (type.getRegex() != null) {
                Assertions.assertNull(S2RegexCompatibility.findIncompatibility(type.getRegex()), type.name());
            }
        }
    }

    @Test
    void getRulesJsonRejectsJavaOnlyRegexButServerValidationStillWorks() {
        S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
                .field("code", "코드").rule(S2RuleType.REGEX, "(?i)^abc$")
                .build();

        // Server-only usage keeps working | 서버 전용 사용은 그대로 동작
        Assertions.assertTrue(validator.validate(Map.of("code", "ABC"), e -> {}));

        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class,
                () -> validator.getRulesJson(Locale.KOREAN));
        Assertions.assertTrue(ex.getMessage().contains("code") && ex.getMessage().contains("(?i)^abc$"), ex.getMessage());
    }

    @Test
    void getRulesJsonRejectsJavaOnlyRegexInNestedValidator() {
        S2Validator<Object> child = S2Validator.builder().field("zip", "우편번호").rule(S2RuleType.REGEX, "\\p{Digit}{5}").build();
        S2Validator<Object> parent = S2Validator.builder().field("address", "주소").rule(S2RuleType.NESTED, child).build();

        Assertions.assertThrows(IllegalStateException.class, () -> parent.getRulesJson(Locale.KOREAN));
    }
}
