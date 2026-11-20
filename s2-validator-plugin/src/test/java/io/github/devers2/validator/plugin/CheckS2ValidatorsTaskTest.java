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
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
    void when_recordComponentExists_noException() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-record").toFile();
        try {
            setupSource(projectDir.toPath());
            // Record DTO: components are not FieldDeclarations but must count as fields | 레코드 컴포넌트는 FieldDeclaration 이 아니지만 필드로 인정되어야 함
            writeJavaFile(projectDir.toPath(), "com/example", "PersonRecord.java",
                    "package com.example;\npublic record PersonRecord(String name, int age) {}\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<PersonRecord>builder().field(\"name\").rule(null).field(\"age\").rule(null).build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
    void when_conditionFieldIsOnOuterObject_noException() throws Exception {
        // 하위 검증기의 when 조건은 실행 시 바깥 객체까지 찾으므로(매뉴얼 '바깥 필드 참조') 대상 DTO 에 없어도 빌드를 막지 않는다(경고)
        File projectDir = Files.createTempDirectory("project-dir-when-outer").toFile();
        try {
            setupSource(projectDir.toPath());
            createDto(projectDir.toPath(), "com.example", "Member", "private String country;\n private Address address;\n");
            createDto(projectDir.toPath(), "com.example", "Address", "private String zip;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    Object sub(){\n" +
                    "        return S2Validator.<Address>builder().field(\"zip\").when(\"country\", null, \"KR\").rule(null).build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
    void when_sameVariableNameInOtherMethods_usesItsOwnInitializer() throws Exception {
        // 다른 메서드의 같은 이름 변수 초기값을 쓰지 않는다 — 이전에는 파일에서 처음 찾은 v(Person)로 Order 의 필드를 검사해 오탐
        File projectDir = Files.createTempDirectory("project-dir-var-scope").toFile();
        try {
            setupSource(projectDir.toPath());
            createDto(projectDir.toPath(), "com.example", "Person", "private String name;\n");
            createDto(projectDir.toPath(), "com.example", "Order", "private String orderNo;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    Object a(){\n" +
                    "        var v = S2Validator.<Person>builder().field(\"name\").rule(null);\n" +
                    "        return v.build();\n" +
                    "    }\n" +
                    "    Object b(){\n" +
                    "        var v = S2Validator.<Order>builder();\n" +
                    "        return v.field(\"orderNo\").rule(null).build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
    void when_builderVariableIsOnlyContinuedWithoutTerminal_chainingError() throws Exception {
        // 변수의 체인을 이어 쓰기만 하고 build() 로 끝내지 않으면 죽은 코드
        File projectDir = Files.createTempDirectory("project-dir-chain-dead").toFile();
        try {
            setupSource(projectDir.toPath());
            createDto(projectDir.toPath(), "com.example", "Person", "private String name;\n private int age;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void dead(){\n" +
                    "        var b = S2Validator.<Person>builder().field(\"name\").rule(null);\n" +
                    "        b.field(\"age\").rule(null);\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);
            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
                try {
                    check.invoke(task);
                } catch (java.lang.reflect.InvocationTargetException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof RuntimeException)
                        throw (RuntimeException) cause;
                    throw new RuntimeException(cause);
                }
            });
            org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("불완전한 체이닝"), e.getMessage());
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void when_builderVariableIsContinuedOrReturned_noChainingError() throws Exception {
        // 변수에 담은 빌더를 이어서 쓰고 끝내거나(b.field(..).build()), 다른 곳에 넘기면(return b) 죽은 코드가 아니다
        File projectDir = Files.createTempDirectory("project-dir-chain-var").toFile();
        try {
            setupSource(projectDir.toPath());
            createDto(projectDir.toPath(), "com.example", "Person", "private String name;\n private int age;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    Object continued(boolean adult){\n" +
                    "        var b = S2Validator.<Person>builder().field(\"name\").rule(null);\n" +
                    "        if (adult) b = b.field(\"age\").rule(null);\n" +
                    "        return b.field(\"age\").rule(null).build();\n" +
                    "    }\n" +
                    "    Object returned(){\n" +
                    "        var b = S2Validator.<Person>builder().field(\"name\").rule(null);\n" +
                    "        return b;\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
    void when_propertyHasOnlyAGetter_noException() throws Exception {
        // 필드 없이 getter 만 있는 속성(계산 속성)도 S2Util.getValue 가 읽으므로 오류가 아니다(이전에는 빌드를 막는 오탐)
        File projectDir = Files.createTempDirectory("project-dir-getter").toFile();
        try {
            setupSource(projectDir.toPath());
            createDto(projectDir.toPath(), "com.example", "Person",
                    "private String first;\n public String getFullName() { return first; }\n public boolean isAdult() { return true; }\n"
                            + " public int getXCoord() { return 0; }\n public String code() { return first; }\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder().field(\"fullName\").rule(null).field(\"adult\").rule(null)"
                    + ".field(\"xCoord\").rule(null).field(\"code\").rule(null).build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
    void when_sourceIsMs949Encoded_fieldTypoIsStillCaught() throws Exception {
        // UTF-8 이 아닌(MS949) 소스도 검사한다 — 엄격한 UTF-8 읽기로 바꾸면 예외로 조용히 건너뛸 수 있어 고정
        File projectDir = Files.createTempDirectory("project-dir-ms949").toFile();
        try {
            setupSource(projectDir.toPath());
            createDto(projectDir.toPath(), "com.example", "Person", "private String other;\n");
            String validatorSrc = "package com.example;\n" +
                    "// 회원 검증기\n" +
                    "public class ValidatorSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder().field(\"name\").rule(null, \"이름\").build();\n" +
                    "    }\n" +
                    "}\n";
            Path dir = projectDir.toPath().resolve("src/main/java/com/example");
            Files.createDirectories(dir);
            Files.write(dir.resolve("ValidatorSample.java"), validatorSrc.getBytes(java.nio.charset.Charset.forName("MS949")));

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);
            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
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
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void when_fileUsesJava21Syntax_fieldTypoIsStillCaught() throws Exception {
        // Java 21 문법(switch 패턴 매칭·레코드 패턴)이 있는 파일도 파싱해 검사해야 한다 — Java 17 문법으로 파싱하면
        // 실패한 파일을 조용히 건너뛰어 오타가 빌드를 통과했다
        File projectDir = Files.createTempDirectory("project-dir-java21").toFile();
        try {
            setupSource(projectDir.toPath());
            createDto(projectDir.toPath(), "com.example", "Person", "private String other;\n");
            String validatorSrc = "package com.example;\n" +
                    "public class ValidatorSample {\n" +
                    "    record Point(int x, int y) {}\n" +
                    "    String describe(Object o) {\n" +
                    "        return switch (o) {\n" +
                    "            case Point(int x, int y) when x > 0 -> \"p\" + y;\n" +
                    "            case String s -> s;\n" +
                    "            default -> \"?\";\n" +
                    "        };\n" +
                    "    }\n" +
                    "    void sample(){\n" +
                    "        S2Validator.<Person>builder().field(\"name\").rule(null).build();\n" +
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidatorSample.java", validatorSrc);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
            org.gradle.api.Task task = project.getTasks().create("checkS2", (Class) taskClass);
            java.lang.reflect.Method check = taskClass.getMethod("checkValidators");
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
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
    void when_of_without_validate_throwsException() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-chaining-of-fail").toFile();
        try {
            setupSource(projectDir.toPath());
            // S2Validator.of(...) 체인이 .validate() 없이 중단됨 (죽은 코드)
            String src = "package com.example;\n" +
                    "public class DeadCodeSample {\n" +
                    "    void sample(Object obj){\n" +
                    "        S2Validator.of(obj)\n" +
                    "            .field(\"name\", \"이름\");\n" + // validate() 없음
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "DeadCodeSample.java", src);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
                record(true, "when_of_without_validate_throwsException");
            } catch (Throwable t) {
                record(false, "when_of_without_validate_throwsException");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void when_of_with_validate_success() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-chaining-of-ok").toFile();
        try {
            setupSource(projectDir.toPath());
            // S2Validator.of(...) 체인이 .validate()로 올바르게 끝남
            String src = "package com.example;\n" +
                    "public class ValidChainSample {\n" +
                    "    void sample(Object obj){\n" +
                    "        S2Validator.of(obj)\n" +
                    "            .field(\"name\", \"이름\")\n" +
                    "            .validate();\n" + // validate() 있음
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidChainSample.java", src);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
                record(true, "when_of_with_validate_success");
            } catch (Throwable t) {
                record(false, "when_of_with_validate_success");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void when_builder_without_build_throwsException() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-chaining-builder-fail").toFile();
        try {
            setupSource(projectDir.toPath());
            // S2Validator.builder() 체인이 .build() 없이 중단됨 (죽은 코드)
            String src = "package com.example;\n" +
                    "public class DeadBuilderSample {\n" +
                    "    void sample(){\n" +
                    "        S2Validator.builder()\n" +
                    "            .field(\"name\", \"이름\");\n" + // build() 없음
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "DeadBuilderSample.java", src);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
                record(true, "when_builder_without_build_throwsException");
            } catch (Throwable t) {
                record(false, "when_builder_without_build_throwsException");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void when_builder_with_build_success() throws Exception {
        File projectDir = Files.createTempDirectory("project-dir-chaining-builder-ok").toFile();
        try {
            setupSource(projectDir.toPath());
            // S2Validator.builder() 체인이 .build()로 올바르게 끝남
            String src = "package com.example;\n" +
                    "public class ValidBuilderSample {\n" +
                    "    void sample(){\n" +
                    "        Object v = S2Validator.builder()\n" +
                    "            .field(\"name\", \"이름\")\n" +
                    "            .build();\n" + // build() 있음
                    "    }\n" +
                    "}\n";
            writeJavaFile(projectDir.toPath(), "com/example", "ValidBuilderSample.java", src);

            Project project = ProjectBuilder.builder().withProjectDir(projectDir).build();
            Class<?> taskClass = Class.forName("io.github.devers2.validator.plugin.CheckS2ValidatorsTask");
            @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
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
                record(true, "when_builder_with_build_success");
            } catch (Throwable t) {
                record(false, "when_builder_with_build_success");
                throw t;
            }
        } finally {
            deleteRecursively(projectDir.toPath());
        }
    }

    @Test
    void apply_doesNotThrow_regardlessOfJavaPluginApplicationOrder() {
        // S2ValidatorPlugin.apply()는 'check'/'compileJava' 태스크(java 플러그인이 등록함)에
        // named(...)로 즉시 연결을 시도했었는데, java 플러그인보다 먼저(또는 아예 없이) 적용되면
        // UnknownTaskException으로 즉시 실패했었다. withPlugin("java", ...)으로 지연 연결하도록
        // 고쳐, 적용 순서와 무관하게 항상 안전해야 한다.

        // 1) java 플러그인 없이 적용해도 예외가 없어야 함
        Project withoutJava = ProjectBuilder.builder().build();
        assertDoesNotThrow(() -> withoutJava.getPluginManager().apply(S2ValidatorPlugin.class));

        // 2) 이 플러그인을 먼저 적용하고 java를 나중에 적용해도, check/compileJava에 태스크
        // 의존성이 지연 연결되어야 함
        Project javaAfter = ProjectBuilder.builder().build();
        javaAfter.getPluginManager().apply(S2ValidatorPlugin.class);
        assertDoesNotThrow(() -> javaAfter.getPluginManager().apply("java"));

        boolean checkDependsOnIt = javaAfter.getTasks().getByName("check").getDependsOn().stream()
                .anyMatch(dep -> "checkS2Validators".equals(dep));
        boolean compileDependsOnIt = javaAfter.getTasks().getByName("compileJava").getDependsOn().stream()
                .anyMatch(dep -> "checkS2Validators".equals(dep));
        org.junit.jupiter.api.Assertions.assertTrue(checkDependsOnIt,
                "check should depend on checkS2Validators even when java is applied after this plugin");
        org.junit.jupiter.api.Assertions.assertTrue(compileDependsOnIt,
                "compileJava should depend on checkS2Validators even when java is applied after this plugin");

        // 3) 기존처럼 java를 먼저 적용하는 순서도 계속 정상 동작해야 함 (회귀 방지)
        Project javaFirst = ProjectBuilder.builder().build();
        javaFirst.getPluginManager().apply("java");
        assertDoesNotThrow(() -> javaFirst.getPluginManager().apply(S2ValidatorPlugin.class));
        boolean checkDependsOnItToo = javaFirst.getTasks().getByName("check").getDependsOn().stream()
                .anyMatch(dep -> "checkS2Validators".equals(dep));
        org.junit.jupiter.api.Assertions.assertTrue(checkDependsOnItToo);
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

    private void writeJavaFile(Path projectDir, String packagePath, String fileName, String content)
            throws IOException {
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
