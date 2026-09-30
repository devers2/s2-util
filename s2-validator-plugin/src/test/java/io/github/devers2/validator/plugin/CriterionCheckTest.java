package io.github.devers2.validator.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code checkS2Validators} checks rule criteria and condition values written as literals: mismatches fail the build,
 * a comparison field missing from the DTO is a warning, and valid or non-literal values pass.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@code checkS2Validators}가 리터럴로 적힌 규칙 기준값·조건 값을 검사하는지 확인합니다. 맞지 않으면 빌드 실패, DTO 에 없는 비교 대상 필드는
 * 경고, 올바르거나 리터럴이 아닌 값은 통과입니다.
 */
public class CriterionCheckTest {

    @TempDir
    Path projectDir;

    private GradleRunner setUp(String body) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'criterion-sample'\n");
        Files.writeString(projectDir.resolve("build.gradle"),
                "plugins {\n    id 'java'\n    id 'io.github.devers2.validator'\n}\n");
        Path pkg = Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.writeString(pkg.resolve("Member.java"),
                "package com.example;\npublic class Member {\n    private String pw;\n    private String pwCheck;\n    private int age;\n}\n");
        Files.writeString(pkg.resolve("Samples.java"), "package com.example;\n"
                + "import java.util.List;\nimport java.util.Map;\n"
                + "import io.github.devers2.s2util.validation.S2RuleType;\n"
                + "import io.github.devers2.s2util.validation.S2Operator;\n"
                + "import static io.github.devers2.s2util.validation.S2RuleType.*;\n"
                + "public class Samples {\n    static final int LIMIT = 10;\n" + body + "}\n");
        return GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath()
                .withArguments("checkS2Validators");
    }

    @Test
    void literalMismatchesFailTheBuild() throws IOException {
        List<String> bad = List.of(
                ".rule(S2RuleType.MAX_LENGTH, \"abc\")",
                ".rule(S2RuleType.MAX_LENGTH, 10.5)",
                ".rule(S2RuleType.MIN_VALUE, \"19살\")",
                ".rule(S2RuleType.MAX_LENGTH)",
                ".rule(S2RuleType.EMAIL, \"^.+$\")",
                ".rule(S2RuleType.REGEX, \"[a-z\")",
                ".rule(S2RuleType.JUMIN, \"yes\")",
                ".rule(S2RuleType.NESTED, \"address\")",
                ".rule(S2RuleType.EQUALS_FIELD, 1)",
                ".when(\"age\", S2Operator.GT, \"abc\")",
                ".when(\"type\", S2Operator.IN, \"A\")",
                ".when(\"memo\", S2Operator.NOT_EMPTY).and(\"memo\", S2Operator.EMPTY, \"x\")",
                ".rule(MAX_LENGTH, \"x\")");
        StringBuilder body = new StringBuilder("    void bad() {\n");
        for (String call : bad) {
            body.append("        S2Validator.<Map<String, Object>>builder().field(\"f\")").append(call).append(".build();\n");
        }
        body.append("    }\n");

        BuildResult result = setUp(body.toString()).buildAndFail();
        String output = result.getOutput();
        assertTrue(output.contains("13개의 잘못된 규칙 기준값"), output);
        for (int line = 10; line <= 22; line++) {
            assertTrue(output.contains("Line " + line + ":"), "line " + line + " not reported:\n" + output);
        }
        assertTrue(output.contains("정규식 문법 오류"), output);
        assertTrue(output.contains("런타임은 조용히 무시"), output);
    }

    @Test
    void validAndNonLiteralValuesPass() throws IOException {
        String body = """
                    void good(S2Validator<Object> sub, int max) {
                        S2Validator.<Map<String, Object>>builder()
                            .field("a").rule(S2RuleType.REQUIRED, null, "err.required")
                            .field("b").rule(MAX_LENGTH, 10).rule(S2RuleType.MAX_LENGTH, "10").rule(S2RuleType.MIN_LENGTH, LIMIT)
                            .field("c").rule(S2RuleType.MIN_VALUE, -1.5).rule(S2RuleType.MAX_VALUE, " 19 ").rule(S2RuleType.MAX_VALUE, max)
                            .field("d").rule(S2RuleType.REGEX, "\\\\d+").rule(S2RuleType.JUMIN, true).rule(S2RuleType.BIZRNO, "true")
                            .field("e").rule(S2RuleType.NESTED, sub).rule(S2RuleType.JUMIN)
                            .field("f").when("age", S2Operator.GTE, 20).and("type", S2Operator.IN, List.of("A"))
                            .field("g").when("memo", S2Operator.NOT_EMPTY).rule((String s) -> s.isEmpty())
                            .build();
                        S2Validator.<Member>builder().field("pwCheck").rule(S2RuleType.EQUALS_FIELD, "pw").build();
                    }
                """;
        BuildResult result = setUp(body).build();
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkS2Validators").getOutcome());
        assertFalse(result.getOutput().contains("Criterion"), result.getOutput());
    }

    @Test
    void missingComparisonFieldIsAWarningOnly() throws IOException {
        String body = """
                    void warn() {
                        S2Validator.<Member>builder().field("pwCheck").rule(S2RuleType.EQUALS_FIELD, "password").build();
                    }
                """;
        BuildResult result = setUp(body).build();
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkS2Validators").getOutcome());
        assertTrue(result.getOutput().contains("[S2Validator Criterion Warning]"), result.getOutput());
        assertTrue(result.getOutput().contains("'password'가 com.example.Member에 없습니다"), result.getOutput());
        assertTrue(Files.readString(projectDir.resolve("build/s2-validator/checkS2Validators.txt")).contains("password"));
    }
}
