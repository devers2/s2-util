package io.github.devers2.validator.plugin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CheckS2ValidatorsTask}.
 */
public class CheckS2ValidatorsTaskTest {

    private static int successCount = 0;
    private static int failCount = 0;
    private static final List<String> failDetails = new ArrayList<>();

    @Test
    void when_fieldExists_noException() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-ok").toFile();
        try {
            setupSource(projectDir.toPath());
            // DTO with field 'name' and validator referencing it
            createDto(projectDir.toPath(), "com.example", "Person", "private String name;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder().field(\"name\").rule(null).build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);

            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
            assertDoesNotThrow(() -> {
                try {
                    check.invoke(task);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            });
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void when_fieldMissing_throwsException() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-fail").toFile();
        try {
            setupSource(projectDir.toPath());
            // DTO without the referenced field
            createDto(projectDir.toPath(), "com.example", "Person", "private String other;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder().field(\"name\").rule(null).build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);

            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
            try {
                assertThrows(IllegalStateException.class, () -> {
                    try {
                        check.invoke(task);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException)
                            throw (RuntimeException) cause;
                        throw new RuntimeException(cause);
                    }
                });
                record(true, "when_fieldMissing_throwsException");
            } catch (Throwable t) {
                record(false, "when_fieldMissing_throwsException");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void when_and_methods_are_checked() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-when-and").toFile();
        try {
            setupSource(projectDir.toPath());
            // DTO with one present field and one missing field
            createDto(projectDir.toPath(), "com.example", "Person", "private String age;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder()\n" +
                    "            .when(\"age\").rule(null)\n" +
                    "            .and(\"missing\").rule(null)\n" +
                    "            .build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);

            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
            // missing field should cause exception
            try {
                assertThrows(IllegalStateException.class, () -> {
                    try {
                        check.invoke(task);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException)
                            throw (RuntimeException) cause;
                        throw new RuntimeException(cause);
                    }
                });
                record(true, "when_and_methods_are_checked");
            } catch (Throwable t) {
                record(false, "when_and_methods_are_checked");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void when_method_success() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-when-success").toFile();
        try {
            setupSource(projectDir.toPath());
            // DTO with 'age' field
            createDto(projectDir.toPath(), "com.example", "Person", "private String age;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder().when(\"age\").rule(null).build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);

            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
            try {
                assertDoesNotThrow(() -> {
                    try {
                        check.invoke(task);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
                record(true, "when_method_success");
            } catch (Throwable t) {
                record(false, "when_method_success");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void when_method_failure() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-when-fail").toFile();
        try {
            setupSource(projectDir.toPath());
            // DTO without 'age' field
            createDto(projectDir.toPath(), "com.example", "Person", "private String other;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder().when(\"age\").rule(null).build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);

            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
            try {
                assertThrows(IllegalStateException.class, () -> {
                    try {
                        check.invoke(task);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException)
                            throw (RuntimeException) cause;
                        throw new RuntimeException(cause);
                    }
                });
                record(true, "when_method_failure");
            } catch (Throwable t) {
                record(false, "when_method_failure");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void and_method_success() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-and-success").toFile();
        try {
            setupSource(projectDir.toPath());
            // DTO with 'age' and 'name' fields
            createDto(projectDir.toPath(), "com.example", "Person", "private String age;\nprivate String name;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder()\n" +
                    "            .when(\"age\").rule(null)\n" +
                    "            .and(\"name\").rule(null)\n" +
                    "            .build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);

            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
            try {
                assertDoesNotThrow(() -> {
                    try {
                        check.invoke(task);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
                record(true, "and_method_success");
            } catch (Throwable t) {
                record(false, "and_method_success");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void and_method_failure() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-and-fail").toFile();
        try {
            setupSource(projectDir.toPath());
            // DTO with only 'age' field
            createDto(projectDir.toPath(), "com.example", "Person", "private String age;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder()\n" +
                    "            .when(\"age\").rule(null)\n" +
                    "            .and(\"name\").rule(null)\n" +
                    "            .build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);

            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
            try {
                assertThrows(IllegalStateException.class, () -> {
                    try {
                        check.invoke(task);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof RuntimeException)
                            throw (RuntimeException) cause;
                        throw new RuntimeException(cause);
                    }
                });
                record(true, "and_method_failure");
            } catch (Throwable t) {
                record(false, "and_method_failure");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void nonS2Validator_field_calls_are_ignored() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-non-s2").toFile();
        try {
            setupSource(projectDir.toPath());
            // A class with a method call named 'field' but not S2Validator chain
            String otherSrc = "package com.example;\n" +
                    "public class OtherValidator {\n" +
                    "    void sample(){\n" +
                    "        new OtherValidator().field(\"x\");\n" +
                    "    }\n" +
                    "    void field(String s){}\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "OtherValidator.java", otherSrc);

            // No DTO created and no S2Validator usage referencing missing field -> should not throw
            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);

            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
            assertDoesNotThrow(() -> {
                try {
                    check.invoke(task);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            });
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    private static void record(boolean isSuccess, String testName) {
        if (isSuccess) {
            successCount++;
            System.out.println("[PASS] " + testName);
        } else {
            failCount++;
            failDetails.add(testName);
            System.err.println("[FAIL] " + testName);
        }
    }

    @AfterAll
    static void summary() {
        System.out.println("================================================================================");
        System.out.println("[요약] CheckS2ValidatorsTaskTest 결과 보고서");
        System.out.println(" - 총 성공 건수: " + successCount);
        System.out.println(" - 총 실패 건수: " + failCount);
        if (failCount > 0) {
            System.err.println(">>> 실패 상세 내역:");
            failDetails.forEach(detail -> System.err.println("   * " + detail));
        } else {
            System.out.println(">>> 모든 테스트를 성공적으로 통과했습니다.");
        }
        System.out.println("================================================================================");
    }

    // helper: create src/main/java directory structure
    private void setupSource(Path projectDir) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/java"));
    }

    private void createDto(Path projectDir, String pkg, String className, String fields) throws IOException {
        String packagePath = pkg.replace('.', '/');
        String src = "package " + pkg + ";\n" +
                "public class " + className + " {\n" +
                fields +
                "}\n";
        writeJavaFile(projectDir, packagePath, className + ".java", src);
    }

    private void writeJavaFile(Path projectDir, String packagePath, String fileName, String content) throws IOException {
        Path dir = projectDir.resolve("src/main/java").resolve(packagePath);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(fileName), content);
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.notExists(path))
            return;
        Files.walk(path)
                .map(Path::toFile)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(File::delete);
    }
}
