package io.github.devers2.validator.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code checkS2Validators} infers the DTO of {@code S2Validator.of(dto)} without a type argument, and is
 * incremental and cacheable.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@code checkS2Validators}가 타입 인자 없는 {@code S2Validator.of(dto)}의 DTO 를 추론하고, 증분 빌드·빌드 캐시를 지원하는지
 * 확인합니다.
 */
public class TargetInferenceAndCachingTest {

    @TempDir
    Path projectDir;

    private Path setUp(String validatorBody) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'infer-sample'\n");
        Files.writeString(projectDir.resolve("build.gradle"),
                "plugins {\n    id 'java'\n    id 'io.github.devers2.validator'\n}\n");
        Path pkg = Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.writeString(pkg.resolve("Person.java"),
                "package com.example;\npublic class Person {\n    private String name;\n}\n");
        Files.writeString(pkg.resolve("Samples.java"), "package com.example;\n"
                + "import java.util.Map;\nimport com.external.Foreign;\n"
                + "public class Samples {\n" + validatorBody + "}\n");
        return pkg;
    }

    private GradleRunner runner(String... args) {
        return GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath().withArguments(args);
    }

    @Test
    void ofArgumentTypeIsInferredFromItsDeclaration() throws IOException {
        setUp("""
                    private Person person;
                    void param(Person p) { S2Validator.of(p).field("wrong1").validate(); }
                    void local() { Person q = load(); S2Validator.of(q).field("wrong2").validate(); }
                    void varLocal() { var r = new Person(); S2Validator.of(r).field("wrong3").validate(); }
                    void created() { S2Validator.of(new Person()).field("wrong4").validate(); }
                    void field() { S2Validator.of(this.person).field("wrong5").validate(); }
                    void lambda() { java.util.function.Consumer<Person> c = (Person s) -> S2Validator.of(s).field("wrong6").validate(); }
                    void failFast(Person p) { S2Validator.of(p, false).field("wrong7").validate(); }
                    void valid(Person p) { S2Validator.of(p).field("name").validate(); }
                    Person load() { return null; }
                """);

        BuildResult result = runner("checkS2Validators").buildAndFail();
        for (int i = 1; i <= 7; i++) {
            assertTrue(result.getOutput().contains("'wrong" + i + "'"), "wrong" + i + " not reported:\n" + result.getOutput());
        }
        assertFalse(result.getOutput().contains("'name'"), result.getOutput());
        assertTrue(result.getOutput().contains("7개의 잘못된 필드명"), result.getOutput());
    }

    @Test
    void jdkAndUnknownTypesAreSkippedQuietly() throws IOException {
        setUp("""
                    void map(Map<String, Object> m) { S2Validator.of(m).field("anything").validate(); }
                    void string(String s) { S2Validator.of(s).field("anything").validate(); }
                    void untyped(Object o) { S2Validator.of(o).field("anything").validate(); }
                    void call() { S2Validator.of(load()).field("anything").validate(); }
                    void foreign1(Foreign f) { S2Validator.of(f).field("a").validate(); }
                    void foreign2(Foreign f) { S2Validator.of(f).field("b").validate(); }
                    Person load() { return null; }
                """);

        BuildResult result = runner("checkS2Validators").build();
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkS2Validators").getOutcome());
        String output = result.getOutput();
        assertFalse(output.contains("java.util.Map"), output);
        // A class without source is reported once, not per call | 소스 없는 클래스는 호출마다가 아니라 한 번만 기록
        assertEquals(1, output.split("com.external.Foreign", -1).length - 1, output);
    }

    @Test
    void taskIsUpToDateAndCacheable() throws IOException {
        Path pkg = setUp("    void param(Person p) { S2Validator.of(p).field(\"name\").validate(); }\n");
        // TestKit shares its Gradle home across runs; a per-project cache keeps earlier runs from answering. | TestKit 은 Gradle 홈을 공유하므로 프로젝트별 캐시로 이전 실행의 결과를 배제
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'infer-sample'\n"
                + "buildCache { local { directory = new File(rootDir, '.build-cache') } }\n");

        assertEquals(TaskOutcome.SUCCESS,
                runner("checkS2Validators", "--build-cache").build().task(":checkS2Validators").getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE,
                runner("checkS2Validators", "--build-cache").build().task(":checkS2Validators").getOutcome());

        // A DTO change is an input change | DTO 변경은 입력 변경
        Files.writeString(pkg.resolve("Person.java"),
                "package com.example;\npublic class Person {\n    private String name;\n    private int age;\n}\n");
        assertEquals(TaskOutcome.SUCCESS,
                runner("checkS2Validators", "--build-cache").build().task(":checkS2Validators").getOutcome());

        // After clean, the result comes from the build cache | clean 후 결과는 빌드 캐시에서 복원
        BuildResult cached = runner("clean", "checkS2Validators", "--build-cache").build();
        assertEquals(TaskOutcome.FROM_CACHE, cached.task(":checkS2Validators").getOutcome());
        assertTrue(Files.readString(projectDir.resolve("build/s2-validator/checkS2Validators.txt"))
                .startsWith("S2Validator check passed"));
    }
}
