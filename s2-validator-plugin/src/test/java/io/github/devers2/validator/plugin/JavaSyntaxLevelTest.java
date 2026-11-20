package io.github.devers2.validator.plugin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 실제 플러그인 클래스로더에서, 최신 Java 문법(Java 25: 모듈 import, 생성자에서 super() 앞의 문장)을 쓴 파일도 읽어 검사하는지 확인한다.
 * 실행 시 로드되는 JavaParser 가 옛 버전이면(Gradle 배포판 것) 파싱 실패 경고만 나오고 오타를 놓친다.
 */
public class JavaSyntaxLevelTest {

    @TempDir
    Path projectDir;

    @Test
    void java25SyntaxFileIsCheckedInTheRealPluginClassLoader() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'java25-sample'\n");
        Files.writeString(projectDir.resolve("build.gradle"),
                "plugins {\n    id 'java'\n    id 'io.github.devers2.validator'\n}\n");
        Path pkg = Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.writeString(pkg.resolve("Member.java"), "package com.example;\npublic class Member {\n    String name;\n}\n");
        Files.writeString(pkg.resolve("Rules.java"), """
                package com.example;
                import module java.base;
                public class Rules extends Object {
                    final int size;
                    Rules(int size) {
                        if (size < 0) throw new IllegalArgumentException();
                        super();
                        this.size = size;
                    }
                    Object rules() {
                        return S2Validator.<Member>builder().field("nmae").build();
                    }
                }
                """);

        BuildResult result = GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath()
                .withArguments("checkS2Validators").buildAndFail();
        String output = result.getOutput();
        assertTrue(output.contains("'nmae'"), output);
        assertFalse(output.contains("해석하지 못해"), output);
    }
}
