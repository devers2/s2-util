package io.github.devers2.validator.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the {@code S2BindValidator.bind(...)} usage check: a bound validator whose result is never validated
 * ({@code validate}/{@code getRulesJson}) is reported as a warning.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@code S2BindValidator.bind(...)} 사용 검사를 확인합니다. 바인딩한 결과에서 {@code validate}/{@code getRulesJson}을 호출하지 않으면
 * 경고로 보고합니다.
 */
public class BindValidatorUsageTest {

    @TempDir
    Path projectDir;

    private List<?> warningsFor(String body) throws Exception {
        Path pkg = Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Path source = pkg.resolve("SampleController.java");
        Files.writeString(source, "package com.example;\n"
                + "public class SampleController {\n"
                + "    S2Validator<Object> rules() { return null; }\n"
                + "    void handle(Object cmd, Object result) {\n" + body + "\n    }\n"
                + "}\n");

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();
        @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
        CheckS2ValidatorsTask task = (CheckS2ValidatorsTask) project.getTasks().create("checkS2", (Class) CheckS2ValidatorsTask.class);

        Method analyze = CheckS2ValidatorsTask.class.getDeclaredMethod("analyzeFile", Path.class);
        analyze.setAccessible(true);
        Object result = analyze.invoke(task, source);
        Field warnings = result.getClass().getDeclaredField("bindValidatorWarnings");
        warnings.setAccessible(true);
        return (List<?>) warnings.get(result);
    }

    @Test
    void chainedValidateOrGetRulesJsonIsNotReported() throws Exception {
        assertEquals(0, warningsFor("S2BindValidator.bind(rules()).validate(cmd, result);").size());
        assertEquals(0, warningsFor("String json = S2BindValidator.bind(rules()).getRulesJson();").size());
    }

    @Test
    void variableThatIsValidatedIsNotReported() throws Exception {
        assertEquals(0, warningsFor("var bound = S2BindValidator.bind(rules());\nbound.validate(cmd, result);").size());
    }

    @Test
    void discardedBindingIsReported() throws Exception {
        List<?> warnings = warningsFor("S2BindValidator.bind(rules());");
        assertEquals(1, warnings.size());
        Field target = warnings.get(0).getClass().getDeclaredField("target");
        target.setAccessible(true);
        assertEquals("rules()", target.get(warnings.get(0)));
    }

    @Test
    void variableNeverValidatedIsReported() throws Exception {
        assertEquals(1, warningsFor("var bound = S2BindValidator.bind(rules());").size());
    }

    @Test
    void otherOfMethodsAreIgnored() throws Exception {
        assertTrue(warningsFor("java.util.List.of(1, 2);\nS2Validator.of(cmd);").isEmpty());
    }
}
