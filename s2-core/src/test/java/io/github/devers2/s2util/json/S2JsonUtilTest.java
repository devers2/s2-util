package io.github.devers2.s2util.json;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.devers2.s2util.json.S2JsonUtil.Feature;

/**
 * Tests for {@link S2JsonUtil}: strict parsing, writing of the supported types, mapping, and failure behavior (never
 * {@code null} or broken output).
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link S2JsonUtil}의 엄격한 파싱, 지원 타입 쓰기, 매핑, 실패 동작({@code null}이나 깨진 결과를 내지 않음)을 확인합니다.
 */
class S2JsonUtilTest {

    enum Status {
        ACTIVE, INACTIVE
    }

    record Item(String name, int qty, BigDecimal price) {
    }

    record Order(long id, LocalDate day, Status status, List<Item> items, Map<String, Integer> counts,
            Optional<String> memo, UUID ref) {
    }

    static class Base {
        protected String createdBy;
    }

    static class User extends Base {
        private String name;
        private int age;
        private transient String secret = "hidden";
        private static String ignored = "static";
        private List<User> friends;
        private Map<String, Object> metadata;

        User() {
        }

        User(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    static class NoDefaultConstructor {
        final String value;

        NoDefaultConstructor(String value) {
            this.value = value;
        }
    }

    static class SelfRef {
        SelfRef self = this;
    }

    @Nested
    class Parsing {

        @Test
        void standardValuesMapToJavaTypes() {
            Map<String, Object> m = S2JsonUtil.parseObject(
                    "{\"s\":\"a\\u00e9\\n\",\"i\":1,\"big\":12345678901234567890,\"d\":1.5,\"e\":1e3,\"t\":true,\"n\":null,"
                            + "\"a\":[1,[2]],\"o\":{}}");
            assertEquals("aé\n", m.get("s"));
            assertEquals(1L, m.get("i"));
            assertEquals(new BigInteger("12345678901234567890"), m.get("big"));
            assertEquals(1.5, m.get("d"));
            assertEquals(1000.0, m.get("e"));
            assertEquals(true, m.get("t"));
            assertTrue(m.containsKey("n") && m.get("n") == null);
            assertEquals(List.of(1L, List.of(2L)), m.get("a"));
            assertEquals(Map.of(), m.get("o"));
            assertInstanceOf(LinkedHashMap.class, m);
        }

        @Test
        void topLevelScalarsAndArraysAreAllowed() {
            assertEquals("x", S2JsonUtil.parse(" \"x\" "));
            assertNull(S2JsonUtil.parse("null"));
            assertEquals(List.of(1L, 2L), S2JsonUtil.parseArray("[1,2]"));
            assertThrows(S2JsonException.class, () -> S2JsonUtil.parseObject("[1]"));
            assertThrows(S2JsonException.class, () -> S2JsonUtil.parseArray("{}"));
        }

        @Test
        void duplicateKeysKeepTheLastValueAndKeyOrderIsPreserved() {
            Map<String, Object> m = S2JsonUtil.parseObject("{\"b\":1,\"a\":2,\"b\":3}");
            assertEquals(List.of("b", "a"), new ArrayList<>(m.keySet()));
            assertEquals(3L, m.get("b"));
        }

        @Test
        void bigDecimalFeatureKeepsExactDecimals() {
            assertEquals(new BigDecimal("0.10"), S2JsonUtil.parse("0.10", Feature.USE_BIG_DECIMAL_FOR_FLOATS));
            assertEquals(new BigDecimal("1E+400"), S2JsonUtil.parse("1e400", Feature.USE_BIG_DECIMAL_FOR_FLOATS));
            assertThrows(S2JsonException.class, () -> S2JsonUtil.parse("1e400"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "   ", "{\"a\":1} xyz", "{\"a\":1 \"b\":2}", "{\"a\":1", "[1,2", "{\"a\":undefined}",
                "{a:1}", "{'a':1}", "[1,]", "{\"a\":1,}", "[1,,2]", "01", "+1", ".5", "1.", "1e", "-", "--1", "NaN",
                "Infinity", "\"\\x\"", "\"\\u12\"", "\"\\u12G4\"", "\"tab\there\"", "\"unterminated", "tru", "nul",
                "[1] // c", "# c\n1", "\u00a01" })
        void invalidJsonIsRejectedWithAPosition(String json) {
            S2JsonException e = assertThrows(S2JsonException.class, () -> S2JsonUtil.parse(json), json);
            assertTrue(e.getPosition() >= 0, e.getMessage());
        }

        @Test
        void nullInputIsAnArgumentError() {
            assertThrows(IllegalArgumentException.class, () -> S2JsonUtil.parse(null));
        }

        @Test
        void deepNestingFailsInsteadOfOverflowingTheStack() {
            String deep = "[".repeat(100_000) + "]".repeat(100_000);
            S2JsonException e = assertThrows(S2JsonException.class, () -> S2JsonUtil.parse(deep));
            assertTrue(e.getMessage().contains("Nesting deeper"), e.getMessage());
            String ok = "[".repeat(S2JsonUtil.MAX_DEPTH) + "]".repeat(S2JsonUtil.MAX_DEPTH);
            assertInstanceOf(List.class, S2JsonUtil.parse(ok));
        }

        @Test
        void relaxedFeaturesAreOptIn() {
            assertEquals(Map.of("a", 1L), S2JsonUtil.parse("{a:1}", Feature.ALLOW_UNQUOTED_FIELD_NAMES));
            assertEquals(Map.of("a", "b'c"), S2JsonUtil.parse("{'a':'b\\'c'}", Feature.ALLOW_SINGLE_QUOTES));
            assertEquals(List.of(1L), S2JsonUtil.parse("[1,]", Feature.ALLOW_TRAILING_COMMA));
            assertEquals(Map.of("a", 1L), S2JsonUtil.parse("{\"a\":1,}", Feature.ALLOW_TRAILING_COMMA));
            assertEquals(java.util.Arrays.asList(1L, null, 2L), S2JsonUtil.parse("[1,,2]", Feature.ALLOW_MISSING_VALUES));
            assertEquals(7L, S2JsonUtil.parse("007", Feature.ALLOW_LEADING_ZEROS_FOR_NUMBERS));
            assertEquals(1L, S2JsonUtil.parse("+1", Feature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS));
            assertEquals(0.5, S2JsonUtil.parse(".5", Feature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS));
            assertEquals(1.0, S2JsonUtil.parse("1.", Feature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS));
            assertEquals(List.of(1L), S2JsonUtil.parse("/* a */ [1] // b", Feature.ALLOW_JAVA_COMMENTS));
            assertEquals(1L, S2JsonUtil.parse("# c\n1", Feature.ALLOW_YAML_COMMENTS));
            assertEquals("x", S2JsonUtil.parse("\"\\x\"", Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
            assertEquals("a\tb", S2JsonUtil.parse("\"a\tb\"", Feature.ALLOW_UNESCAPED_CONTROL_CHARS));
            assertTrue(Double.isNaN((Double) S2JsonUtil.parse("NaN", Feature.ALLOW_NON_NUMERIC_NUMBERS)));
            assertEquals(Double.NEGATIVE_INFINITY, S2JsonUtil.parse("-Infinity", Feature.ALLOW_NON_NUMERIC_NUMBERS));
            assertThrows(S2JsonException.class, () -> S2JsonUtil.parse("/* open", Feature.ALLOW_JAVA_COMMENTS));
        }
    }

    @Nested
    class Writing {

        @Test
        void scalarsAndStrings() {
            assertEquals("null", S2JsonUtil.toJson(null));
            assertEquals("123", S2JsonUtil.toJson(123));
            assertEquals("1.5", S2JsonUtil.toJson(1.5f));
            assertEquals("true", S2JsonUtil.toJson(true));
            assertEquals("\"x\"", S2JsonUtil.toJson('x'));
            assertEquals("\"q\\\"\\\\\\n\\u0001\\u2028\"", S2JsonUtil.toJson("q\"\\\n\u0001\u2028"));
            assertEquals("12345678901234567890", S2JsonUtil.toJson(new BigInteger("12345678901234567890")));
            assertEquals("0.10", S2JsonUtil.toJson(new BigDecimal("0.10")));
        }

        @Test
        void jdkValueTypesBecomeStrings() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("day", LocalDate.of(2026, 9, 30));
            m.put("at", LocalDateTime.of(2026, 9, 30, 12, 0));
            m.put("dur", Duration.ofMinutes(90));
            m.put("date", Date.from(Instant.parse("2026-09-30T00:00:00Z")));
            m.put("id", new UUID(1, 2));
            m.put("uri", URI.create("https://x.io"));
            m.put("locale", Locale.KOREA);
            m.put("status", Status.ACTIVE);
            m.put("opt", Optional.of("v"));
            m.put("none", Optional.empty());
            assertEquals("{\"day\":\"2026-09-30\",\"at\":\"2026-09-30T12:00\",\"dur\":\"PT1H30M\","
                    + "\"date\":\"2026-09-30T00:00:00Z\",\"id\":\"00000000-0000-0001-0000-000000000002\","
                    + "\"uri\":\"https://x.io\",\"locale\":\"ko-KR\",\"status\":\"ACTIVE\",\"opt\":\"v\",\"none\":null}",
                    S2JsonUtil.toJson(m));
        }

        @Test
        void recordsPojosCollectionsAndArrays() {
            User user = new User("홍길동", 30);
            user.createdBy = "admin";
            user.friends = List.of(new User("이순신", 45));
            assertEquals("{\"createdBy\":\"admin\",\"name\":\"홍길동\",\"age\":30,\"friends\":[{\"createdBy\":null,"
                    + "\"name\":\"이순신\",\"age\":45,\"friends\":null,\"metadata\":null}],\"metadata\":null}",
                    S2JsonUtil.toJson(user));
            assertEquals("{\"name\":\"a\",\"qty\":2,\"price\":null}", S2JsonUtil.toJson(new Item("a", 2, null)));
            assertEquals("[1,2,3]", S2JsonUtil.toJson(new int[] { 1, 2, 3 }));
            assertEquals("[\"a\",null]", S2JsonUtil.toJson(java.util.Arrays.asList("a", null)));
            assertEquals("{\"1\":\"x\",\"ACTIVE\":2}", S2JsonUtil.toJson(orderedMap(1, "x", Status.ACTIVE, 2)));
        }

        @Test
        void unsupportedValuesFailInsteadOfProducingBrokenJson() {
            assertThrows(S2JsonException.class, () -> S2JsonUtil.toJson(new SelfRef()));
            assertThrows(S2JsonException.class, () -> S2JsonUtil.toJson(Map.of("x", Double.NaN)));
            assertThrows(S2JsonException.class, () -> S2JsonUtil.toJson(Map.of("f", new java.io.File("a"))));
            assertThrows(S2JsonException.class, () -> S2JsonUtil.toJson(Map.of(List.of(1), "complex key")));
            S2JsonException e = assertThrows(S2JsonException.class,
                    () -> S2JsonUtil.toJson(Map.of("list", List.of(Map.of("bad", Double.POSITIVE_INFINITY)))));
            assertTrue(e.getMessage().contains("$.list[0].bad"), e.getMessage());
            assertEquals("{\"x\":NaN}", S2JsonUtil.toJson(Map.of("x", Double.NaN), Feature.ALLOW_NON_NUMERIC_NUMBERS));
        }

        @Test
        void sharedButNotCircularReferencesAreWritten() {
            List<String> shared = List.of("s");
            assertEquals("[[\"s\"],[\"s\"]]", S2JsonUtil.toJson(List.of(shared, shared)));
        }
    }

    @Nested
    class Mapping {

        @Test
        void recordWithNestedTypesRoundTrips() {
            Order order = new Order(7L, LocalDate.of(2026, 9, 30), Status.ACTIVE,
                    List.of(new Item("a", 2, new BigDecimal("1.50"))), Map.of("x", 1), Optional.of("m"), new UUID(3, 4));
            String json = S2JsonUtil.toJson(order);
            assertEquals(order, S2JsonUtil.fromJson(json, Order.class, Feature.USE_BIG_DECIMAL_FOR_FLOATS));
        }

        @Test
        void pojoWithInheritanceAndGenericsMaps() {
            User user = S2JsonUtil.fromJson("{\"createdBy\":\"admin\",\"name\":\"강감찬\",\"age\":50,\"secret\":\"x\","
                    + "\"friends\":[{\"name\":\"을지문덕\",\"age\":60}],\"metadata\":{\"k\":[1]},\"unknown\":true}", User.class);
            assertEquals("admin", user.createdBy);
            assertEquals("강감찬", user.name);
            assertEquals(50, user.age);
            assertEquals("hidden", user.secret, "transient fields are not mapped");
            assertEquals("을지문덕", user.friends.get(0).name);
            assertEquals(Map.of("k", List.of(1L)), user.metadata);
        }

        @Test
        void listsArraysSetsAndOptionals() {
            assertEquals(List.of(new Item("a", 1, null)), S2JsonUtil.fromJsonList("[{\"name\":\"a\",\"qty\":1}]", Item.class));
            assertArrayEquals(new int[] { 1, 2 }, S2JsonUtil.fromJson("[1,2]", int[].class));
            assertEquals(Set.of("a", "b"), S2JsonUtil.fromJson("[\"a\",\"b\",\"a\"]", Set.class));
            assertInstanceOf(SortedSet.class, S2JsonUtil.convert(List.of("b", "a"), SortedSet.class));
            assertEquals(Optional.empty(), S2JsonUtil.convert(null, Optional.class));
            assertEquals(new Date(1000), S2JsonUtil.convert(1000L, Date.class));
            assertEquals(OffsetDateTime.parse("2026-09-30T10:00+09:00"),
                    S2JsonUtil.convert("2026-09-30T10:00+09:00", OffsetDateTime.class));
        }

        @Test
        void missingRecordComponentsGetDefaults() {
            assertEquals(new Item(null, 0, null), S2JsonUtil.fromJson("{}", Item.class));
        }

        @Test
        void lossyOrMismatchedValuesFailWithThePath() {
            assertPathError("{\"name\":\"a\",\"qty\":3.7}", Item.class, "$.qty");
            assertPathError("{\"name\":\"a\",\"qty\":3000000000}", Item.class, "$.qty");
            assertPathError("{\"name\":1,\"qty\":1}", Item.class, "$.name");
            assertPathError("{\"name\":\"a\",\"qty\":null}", Item.class, "$.qty");
            assertPathError("{\"id\":1,\"status\":\"GONE\"}", Order.class, "$.status");
            assertPathError("{\"id\":1,\"day\":\"2026-13-01\"}", Order.class, "$.day");
            assertPathError("{\"id\":1,\"items\":[{\"name\":\"a\"},{\"qty\":\"x\"}]}", Order.class, "$.items[1].qty");
            assertEquals(3L, S2JsonUtil.convert(3.0, Long.class), "an exact integer from a decimal is allowed");
        }

        @Test
        void unsupportedTargetsFail() {
            assertThrows(S2JsonException.class, () -> S2JsonUtil.fromJson("{\"value\":\"a\"}", NoDefaultConstructor.class));
            assertThrows(S2JsonException.class, () -> S2JsonUtil.fromJson("{}", Runnable.class));
            assertThrows(S2JsonException.class, () -> S2JsonUtil.fromJson("\"a\"", java.io.File.class));
            assertThrows(IllegalArgumentException.class, () -> S2JsonUtil.convert("a", null));
        }

        @Test
        void objectTargetReturnsTheTree() {
            Object tree = S2JsonUtil.fromJson("{\"a\":[1]}", Object.class);
            assertEquals(Map.of("a", List.of(1L)), tree);
            assertFalse(tree instanceof String);
        }

        private void assertPathError(String json, Class<?> type, String path) {
            S2JsonException e = assertThrows(S2JsonException.class, () -> S2JsonUtil.fromJson(json, type), json);
            assertTrue(e.getMessage().contains(path + " ") || e.getMessage().endsWith(path), e.getMessage());
        }
    }

    private static Map<Object, Object> orderedMap(Object... kv) {
        Map<Object, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }
}
