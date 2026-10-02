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
 * DTOs declared inside another type (a record in a controller, a static nested class) are checked like top-level
 * ones, with only their own fields.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 다른 타입 안에 선언한 DTO(컨트롤러 안의 record, static 중첩 클래스)도 최상위 DTO 처럼, 그 타입 자신의 필드만으로 검사하는지 확인합니다.
 */
public class NestedDtoTest {

    @TempDir
    Path projectDir;

    private BuildResult check(String validators) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'nested-sample'\n");
        Files.writeString(projectDir.resolve("build.gradle"),
                "plugins {\n    id 'java'\n    id 'io.github.devers2.validator'\n}\n");
        Path pkg = Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.writeString(pkg.resolve("Base.java"), "package com.example;\npublic class Base {\n    protected Long id;\n}\n");
        Files.writeString(pkg.resolve("Join.java"), """
                package com.example;
                public class Join {
                    private String secret;
                    public record Form(String name, Integer age) {
                    }
                    public static class Edit extends Base {
                        private String memo;
                    }
                    void own() {
                        S2Validator.<Form>builder().field("name").field("wrong1").build();
                        S2Validator.<Form>builder().field("secret").build();
                    }
                }
                """);
        Path other = Files.createDirectories(projectDir.resolve("src/main/java/com/example/web"));
        Files.writeString(other.resolve("Uses.java"), "package com.example.web;\n" + validators);
        return GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath()
                .withArguments("checkS2Validators").buildAndFail();
    }

    @Test
    void nestedDtosAreCheckedWithTheirOwnFields() throws IOException {
        BuildResult result = check("""
                import com.example.Join;
                import com.example.Join.Edit;
                public class Uses {
                    void scoped() { S2Validator.<Join.Form>builder().field("age").field("wrong2").build(); }
                    void imported() { S2Validator.<Edit>builder().field("memo").field("id").field("wrong3").build(); }
                    void inferred(Join.Form form) { S2Validator.of(form).field("wrong4").validate(); }
                }
                """);
        String output = result.getOutput();
        for (int i = 1; i <= 4; i++) {
            assertTrue(output.contains("'wrong" + i + "'"), "wrong" + i + " not reported:\n" + output);
        }
        // The outer class's field is not a field of the nested DTO | 바깥 클래스 필드는 중첩 DTO 의 필드가 아님
        assertTrue(output.contains("'secret'"), output);
        // Own, record and inherited fields pass | 자신·레코드·상속 필드는 통과
        for (String valid : new String[] { "'name'", "'age'", "'memo'", "'id'" }) {
            assertFalse(output.contains(valid), valid + " reported:\n" + output);
        }
        assertTrue(output.contains("com.example.Join.Form"), output);
        assertFalse(output.contains("DTO 소스를 찾을 수 없어"), output);
    }
}
