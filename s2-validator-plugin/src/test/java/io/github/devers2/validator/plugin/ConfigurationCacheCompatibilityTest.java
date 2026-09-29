package io.github.devers2.validator.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Functional test: {@code checkS2Validators} must run with the configuration cache enabled and reuse it.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 기능 시험: {@code checkS2Validators}가 configuration cache 를 켠 상태에서 실행되고 캐시를 재사용해야 합니다.
 */
public class ConfigurationCacheCompatibilityTest {

    @TempDir
    Path projectDir;

    @Test
    void checkTaskRunsAndReusesConfigurationCache() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'cc-sample'\n");
        Files.writeString(projectDir.resolve("build.gradle"),
                "plugins {\n    id 'java'\n    id 'io.github.devers2.validator'\n}\n");
        Path pkg = Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.writeString(pkg.resolve("Person.java"),
                "package com.example;\npublic class Person {\n    private String name;\n}\n");
        Files.writeString(pkg.resolve("ValidatorSample.java"),
                "package com.example;\npublic class ValidatorSample {\n    void sample() {\n"
                        + "        S2Validator.<Person>builder().field(\"name\").rule(null).build();\n    }\n}\n");

        GradleRunner runner = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("checkS2Validators", "--configuration-cache", "--stacktrace");

        BuildResult first = runner.build();
        assertEquals(TaskOutcome.SUCCESS, first.task(":checkS2Validators").getOutcome());
        assertTrue(first.getOutput().contains("Configuration cache entry stored"), first.getOutput());

        BuildResult second = runner.build();
        assertEquals(TaskOutcome.SUCCESS, second.task(":checkS2Validators").getOutcome());
        assertTrue(second.getOutput().contains("Configuration cache entry reused"), second.getOutput());
    }
}
