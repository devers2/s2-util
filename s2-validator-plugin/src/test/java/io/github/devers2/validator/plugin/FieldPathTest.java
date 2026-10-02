package io.github.devers2.validator.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Field paths are checked part by part: {@code address.city}, {@code items[0].name}, {@code products[].price} follow
 * the declared types into nested objects and array, collection and map elements, and stop quietly where the type
 * cannot be read from the sources.
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 필드 경로를 부분마다 검사하는지 확인합니다. {@code address.city}, {@code items[0].name}, {@code products[].price}는 선언 타입을 따라
 * 중첩 객체와 배열·컬렉션·맵 요소로 들어가며, 소스에서 타입을 알 수 없는 지점에서는 조용히 멈춥니다.
 */
public class FieldPathTest {

    @TempDir
    Path projectDir;

    @Test
    void everyPartOfAPathIsChecked() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'path-sample'\n");
        Files.writeString(projectDir.resolve("build.gradle"),
                "plugins {\n    id 'java'\n    id 'io.github.devers2.validator'\n}\n");
        Path pkg = Files.createDirectories(projectDir.resolve("src/main/java/com/example"));
        Files.writeString(pkg.resolve("Address.java"), "package com.example;\npublic class Address {\n    String city;\n}\n");
        Files.writeString(pkg.resolve("Item.java"),
                "package com.example;\npublic record Item(String name, int qty) {\n}\n");
        Files.writeString(pkg.resolve("Box.java"), "package com.example;\npublic class Box<T> {\n    T value;\n}\n");
        Files.writeString(pkg.resolve("Order.java"), """
                package com.example;
                import java.time.LocalDate;
                import java.util.*;
                public class Order {
                    Address address;
                    List<Item> items;
                    Item[] array;
                    Map<String, Item> byCode;
                    List<? extends Item> bounded;
                    Set<String> tags;
                    Map<String, Object> meta;
                    Box<Item> box;
                    int[][] matrix;
                    LocalDate date;
                    List<Line> lines;
                    public static class Line {
                        String sku;
                    }
                }
                """);
        Files.writeString(pkg.resolve("Rules.java"), """
                package com.example;
                public class Rules {
                    void valid() {
                        S2Validator.<Order>builder()
                                .field("address.city").field("items[0].name").field("items[].qty").field("array[].name")
                                .field("byCode[A1].qty").field("bounded[].name").field("tags[]").field("meta.anything")
                                .field("box.value.anything").field("matrix[1][2]").field("date.year").field("lines[].sku")
                                .when("items[].name", "x")
                                .build();
                    }
                    void invalid() {
                        S2Validator.<Order>builder()
                                .field("adress.city")
                                .field("address.ctiy")
                                .field("items[].nmae")
                                .field("array[0].qtyy")
                                .field("byCode[A1].nam")
                                .field("lines[].skuu")
                                .when("bounded[].wrong", "x")
                                .build();
                    }
                }
                """);

        BuildResult result = GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath()
                .withArguments("checkS2Validators").buildAndFail();
        String output = result.getOutput();
        assertTrue(output.contains("7개의 잘못된 필드명"), output);
        // A path names the missing part and the class it was looked up in | 경로는 없는 부분과 찾은 클래스를 표시
        assertTrue(output.contains("'adress.city' (메서드: field)의 'adress' 필드가 "), output);
        assertTrue(output.contains("'address.ctiy' (메서드: field)의 'ctiy' 필드가 "), output);
        assertTrue(output.contains("com.example.Address"), output);
        for (String missing : new String[] { "'nmae'", "'qtyy'", "'nam'", "'skuu'", "'wrong'" }) {
            assertTrue(output.contains(missing), missing + " not reported:\n" + output);
        }
        assertTrue(output.contains("com.example.Order.Line"), output);
        assertEquals(-1, output.indexOf("'address.city'"), output);
        // Each error points at its own line of the chain | 오류마다 체인 안의 자기 줄을 가리킴
        assertTrue(output.contains("Line 13:") && output.contains("Line 19:"), output);
    }
}
