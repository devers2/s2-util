package io.github.devers2.s2util.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.json.S2JsonException;
import io.github.devers2.s2util.json.S2JsonUtil;
import io.github.devers2.s2util.log.DefaultS2Logger;

/**
 * s2-core 유틸 수정 회귀 시험: S2Util·S2DateUtil·S2StringUtil·S2Template·JSON·기본 로거.
 */
class S2CoreUtilFixesTest {

    /** setter 없이 필드만 있는 클래스 */
    public static class NoSetter {
        private String name;
        private final String fixed = "f";

        public String getName() {
            return name;
        }

        public String getFixed() {
            return fixed;
        }
    }

    public static class Getters {
        public String getName() {
            return "n";
        }

        public boolean isActive() {
            return true;
        }

        public String issue() {
            return "i";
        }

        public String getaway() {
            return "g";
        }

        public String isNotBoolean() {
            return "x";
        }
    }

    @Test
    void setValueWritesFieldWhenThereIsNoSetter() {
        NoSetter target = new NoSetter();
        assertTrue(S2Util.setValue(target, "name", "v"));
        assertEquals("v", target.getName());
        assertFalse(S2Util.setValue(target, "fixed", "changed"), "final field must not be written");
        assertEquals("f", target.getFixed());
    }

    @Test
    void getValueAllReadsOnlyGetters() {
        assertEquals(new TreeSet<>(java.util.List.of("active", "name")),
                new TreeSet<>(S2Util.getValueAll(new Getters()).keySet()));
    }

    @Test
    void getPercentageHandlesNonTerminatingRatios() {
        assertEquals("33.33", S2Util.getPercentage(1, 3, 2));
        assertEquals("66.67", S2Util.getPercentage(2, 3, 2));
        assertEquals("12.5", S2Util.getPercentage(1, 8, 1));
        assertEquals("", S2Util.getPercentage(1, 0, 2));
        assertEquals("", S2Util.getPercentage(null, 3, 2));
    }

    @Test
    void dateParsingRejectsNonexistentDates() {
        assertFalse(S2DateUtil.isValidDate("2024-02-30", "yyyy-MM-dd", false));
        assertFalse(S2DateUtil.isValidDate("2023-02-29", "yyyy-MM-dd", false));
        assertNull(S2DateUtil.parseToLocalDate("2024-04-31", "yyyy-MM-dd", false));
        assertTrue(S2DateUtil.isValidDate("2024-02-29", "yyyy-MM-dd", false));
        assertEquals(LocalDate.of(2024, 1, 31), S2DateUtil.parseToLocalDate("20240131", "yyyyMMdd", false));
        assertTrue(S2DateUtil.isValidDate("24/01/31", "yy/MM/dd", false));
        assertTrue(S2DateUtil.isValidDate("2024-01-31 23:59:59", "yyyy-MM-dd HH:mm:ss", false));
        assertTrue(S2DateUtil.isValidDate("2024-01-31", "uuuu-MM-dd", false));
    }

    @Test
    void clearNewlineEscapesBackslashBeforeQuote() {
        // 입력의 \' 가 JS 문자열을 닫지 않도록 역슬래시도 이스케이프해야 한다
        assertEquals("\\\\\\'x", S2StringUtil.clearNewline("\\'x"));
        assertEquals("ab\\'c", S2StringUtil.clearNewline("a\nb'c"));
    }

    @Test
    void substringByBytesCountsSupplementaryCharactersAsFourBytes() {
        String text = "a\uD83D\uDE00b"; // a😀b = 1 + 4 + 1 바이트
        assertEquals("a", S2StringUtil.substringByBytes(text, 0, 4));
        assertEquals("a\uD83D\uDE00", S2StringUtil.substringByBytes(text, 0, 5));
        assertEquals("가나", S2StringUtil.substringByBytes("가나다", 0, 7));
    }

    @Test
    void caseConversionIgnoresDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertEquals("userId", S2StringUtil.toCamelCase("user_id"));
            assertEquals("user_id", S2StringUtil.toSnakeCase("UserID"));
            assertEquals("user-id", S2StringUtil.toKebabCase("UserID"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void replaceCharsHandlesNonAsciiTargets() {
        assertEquals("a_b_c", S2StringUtil.replaceChars("a가b나c", "_", '가', '나'));
        assertEquals("abc", S2StringUtil.removeChars("a-b.c", '-', '.'));
    }

    @Test
    void templateDoesNotReexpandBindingValues() {
        assertEquals("A=x{{=b}} B=Y", S2Template.of("A={{=a}} B={{=b}}").bind("a", "x{{=b}}").bind("b", "Y").render());
        assertEquals("1 1", S2Template.of("{{=a}} {{=a}}{{=missing}}").bind("a", "1").render());
        assertEquals("x", S2Template.of("x{{=unclosed").render().replace("{{=unclosed", ""));
        // 기본 S2Template 은 따옴표를 리터럴로 보지 않는다(메시지의 아포스트로피 뒤에서도 빈 줄 정리)
        assertEquals("It's\nok", S2Template.of("It's  \n\n  \nok").render());
    }

    @Test
    void bindInQuotesNonNumericValuesAndRejectsBackslash() {
        // 숫자·논리값은 그대로, 날짜·enum 등은 문자열 리터럴, 따옴표는 두 번
        assertEquals("IN (1, 2.5, 10000000000000000000, true)",
                S2Template.of("{{=v}}").bindIn("v", java.util.List.of(1, 2.5, new java.math.BigInteger("10000000000000000000"), true), "IN (", ")").render());
        assertEquals("IN ('2024-01-31', 'MONDAY', 'O''Brien', NULL)",
                S2Template.of("{{=v}}").bindIn("v", java.util.Arrays.asList(LocalDate.of(2024, 1, 31), java.time.DayOfWeek.MONDAY, "O'Brien", null), "IN (", ")").render());
        // MySQL·MariaDB 에서 a\' 가 따옴표를 닫아 구문이 주입되던 값은 거부
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> S2Template.of("{{=v}}").bindIn("v", java.util.List.of("a\\' OR 1=1 -- "), "IN (", ")"));
        // NaN 같은 비유한 실수는 숫자 리터럴이 아니므로 문자열로
        assertEquals("('NaN')", S2Template.of("{{=v}}").bindIn("v", java.util.List.of(Double.NaN), "(", ")").render());
    }

    @Test
    void jsonUnicodeEscapeAcceptsAsciiHexOnly() {
        assertEquals("A", S2JsonUtil.parse("\"\\u0041\""));
        org.junit.jupiter.api.Assertions.assertThrows(S2JsonException.class,
                () -> S2JsonUtil.parse("\"\\u\u0660\u0660\u0664\u0661\""));
        assertEquals(Map.of("a", 1L), S2JsonUtil.parse("{\"a\":1}"));
    }

    @Test
    void defaultLoggerSkipsDebugByDefault() {
        // 시스템 속성 s2.log.level 을 주지 않은 시험 JVM 에서는 INFO 부터 출력
        DefaultS2Logger logger = new DefaultS2Logger("t");
        assertFalse(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
    }

    @Test
    void dateTextFieldsAcceptEnglishOnNonEnglishLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.KOREA);
            assertTrue(S2DateUtil.isValidDate("11:30 PM", "hh:mm a", false));
            assertTrue(S2DateUtil.isValidDate("11:30 pm", "hh:mm a", false));
            assertTrue(S2DateUtil.isValidDate("11:30 오후", "hh:mm a", false));
            assertFalse(S2DateUtil.isValidDate("13:30 PM", "hh:mm a", false));
            assertEquals(LocalDate.of(2024, 1, 31), S2DateUtil.parseToLocalDate("31 Jan 2024", "dd MMM yyyy", false));
            assertNull(S2DateUtil.parseToLocalDate("30 Feb 2024", "dd MMM yyyy", false));
            // 따옴표 안의 문자는 문자형 필드가 아니다
            assertTrue(S2DateUtil.isValidDate("2024-01-31 at", "yyyy-MM-dd 'at'", false));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void toJsonEscapesHtmlSensitiveCharacters() {
        String json = S2JsonUtil.toJson(Map.of("k", "</script><b>&"));
        assertFalse(json.contains("<") || json.contains(">") || json.contains("&"), json);
        assertEquals(Map.of("k", "</script><b>&"), S2JsonUtil.parse(json));
    }

    public static class SqlDates {
        public java.sql.Date day;
        public java.sql.Time time;
        public java.sql.Timestamp at;
    }

    public static class ParentBean {
        public String name = "parent";
    }

    public static class ChildBean extends ParentBean {
        public String name = "child";
    }

    @Test
    void jsonWritesSqlDateTypesByTheirMeaningAndReadsThemBack() {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Seoul"));
            SqlDates value = new SqlDates();
            value.day = java.sql.Date.valueOf("2024-02-01");
            value.time = java.sql.Time.valueOf("09:30:00");
            value.at = java.sql.Timestamp.valueOf("2024-02-01 09:30:00.123456789");

            String json = S2JsonUtil.toJson(value);
            assertTrue(json.contains("\"day\":\"2024-02-01\""), json);
            assertTrue(json.contains("\"time\":\"09:30\""), json);
            assertTrue(json.contains(".123456789Z"), json);

            SqlDates back = S2JsonUtil.fromJson(json, SqlDates.class);
            assertEquals(value.day, back.day);
            assertEquals(value.time, back.time);
            assertEquals(value.at, back.at);
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }

    @Test
    void jsonWritesShadowedFieldOnce() {
        assertEquals("{\"name\":\"child\"}", S2JsonUtil.toJson(new ChildBean()));
    }

    public static class ThrowingGetter {
        public String ok = "fine";
        public static String getStaticOnly() {
            return "static";
        }
        public String getBroken() {
            throw new IllegalStateException("lazy load failed");
        }
    }

    public static class Holder {
        public ThrowingGetter getInner() {
            return new ThrowingGetter();
        }
    }

    @Test
    void getValuePropagatesGetterFailureButNotMissingProperty() {
        // getter 자체의 실패는 빈 값으로 삼키지 않고 속성 경로와 원인을 담아 알린다
        var e = org.junit.jupiter.api.Assertions.assertThrows(io.github.devers2.s2util.exception.S2PropertyAccessException.class,
                () -> S2Util.getValue(new ThrowingGetter(), "broken"));
        assertEquals("broken", e.getPropertyPath());
        assertEquals(ThrowingGetter.class, e.getTargetClass());
        assertTrue(e.getCause() instanceof IllegalStateException);
        // 중첩 경로는 요청한 전체 경로로 알린다
        var nested = org.junit.jupiter.api.Assertions.assertThrows(io.github.devers2.s2util.exception.S2PropertyAccessException.class,
                () -> S2Util.getValue(new Holder(), "inner.broken"));
        assertEquals("inner.broken", nested.getPropertyPath());
        assertEquals(Holder.class, nested.getTargetClass());
        // 기본값 오버로드도 getter 실패는 알린다(값이 없을 때만 기본값)
        org.junit.jupiter.api.Assertions.assertThrows(io.github.devers2.s2util.exception.S2PropertyAccessException.class,
                () -> S2Util.getValue(new ThrowingGetter(), "broken", "default"));
        // 없는 속성·같은 이름의 static 메서드는 오류가 아니라 빈 값
        assertNull(S2Util.getValue(new ThrowingGetter(), "missing"));
        assertEquals("default", S2Util.getValue(new ThrowingGetter(), "missing", "default"));
        assertNull(S2Util.getValue(new ThrowingGetter(), "staticOnly"));
        assertEquals("fine", S2Util.getValue(new ThrowingGetter(), "ok"));
    }

    public static class CopyTarget {
        public String ok;
        public String broken;
    }

    @Test
    void copierAndSetValuePropagateGetterFailure() {
        // 복사: getter 실패는 값이 빠진 채 넘어가지 않고 알리며, exclude 로 뺄 수 있다
        org.junit.jupiter.api.Assertions.assertThrows(io.github.devers2.s2util.exception.S2PropertyAccessException.class,
                () -> S2Copier.from(new ThrowingGetter()).to(CopyTarget.class));
        assertEquals("fine", S2Copier.from(new ThrowingGetter()).exclude("broken").to(CopyTarget.class).ok);
        // setValue: 경로 중간을 읽는 getter 실패는 알리고, 쓸 곳이 없는 것은 false
        org.junit.jupiter.api.Assertions.assertThrows(io.github.devers2.s2util.exception.S2PropertyAccessException.class,
                () -> S2Util.setValue(new ThrowingGetter(), "broken.x", "v"));
        assertFalse(S2Util.setValue(new ThrowingGetter(), "missing", "v"));
    }

    @Test
    void getValueWithoutDefaultReturnsStoredValueAndDefaultOverloadReplacesEmpty() {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("blank", "   ");
        m.put("empty", "");
        m.put("list", java.util.List.of());
        m.put("num", "abc");
        // 기본값 없는 호출: 저장된 값 그대로(빈 값 판단은 isEmpty)
        assertEquals("   ", S2Util.getValue(m, "blank"));
        assertEquals("", S2Util.getValue(m, "empty"));
        assertEquals(java.util.List.of(), S2Util.getValue(m, "list"));
        assertTrue(S2Util.isEmpty(S2Util.getValue(m, "blank")));
        assertEquals("   ", S2Util.getValue(m, "blank", String.class));
        assertNull(S2Util.getValue(m, "num", Integer.class)); // 변환 실패는 null
        assertNull(S2Util.getValue(m, "missing"));
        // 기본값 있는 호출: 빈 값·없음·변환 실패면 기본값
        assertEquals("def", S2Util.getValue(m, "blank", "def"));
        assertEquals("def", S2Util.getValue(m, "empty", "def"));
        assertEquals(7, (int) S2Util.getValue(m, "num", 7));
        assertEquals("def", S2Util.getValue(m, "missing", "def"));
        // cast 도 같은 원칙
        assertEquals("", S2Util.cast(""));
        assertEquals("d", S2Util.cast(" ", "d"));
        // getValueAll 은 저장된 값 그대로
        assertEquals("", S2Util.getValueAll(new java.util.HashMap<>(Map.of("a", ""))).get("a"));
    }

    public static class LombokBoolean {
        private Boolean isDeleted = true;
        private String is3d = "x";

        public Boolean getIsDeleted() {
            return isDeleted;
        }

        public void setIsDeleted(Boolean isDeleted) {
            this.isDeleted = isDeleted;
        }

        public static String getInstanceName() {
            return "static";
        }
    }

    public record ThrowingRecord(String name) {
        public String name() {
            throw new IllegalStateException("boom");
        }
    }

    public static class ThrowingSetter {
        public String name;

        public void setName(String name) {
            throw new IllegalArgumentException("invalid");
        }
    }

    public static class ThrowingCtor {
        public ThrowingCtor() {
            throw new IllegalStateException("ctor");
        }
    }

    @Test
    void s2UtilReviewFixes() {
        var ex = io.github.devers2.s2util.exception.S2PropertyAccessException.class;
        // Lombok 의 Boolean isXxx → getIsXxx(), getter 모양 이름의 필드, static getter 는 getValueAll 에서 제외
        assertEquals(Boolean.TRUE, S2Util.getValue(new LombokBoolean(), "isDeleted"));
        assertEquals("x", S2Util.getValue(new LombokBoolean(), "is3d"));
        assertFalse(S2Util.getValueAll(new LombokBoolean()).containsKey("instanceName"));
        // 레코드 접근자·setter·생성자 자체의 실패는 알린다(타입이 맞지 않는 값은 false)
        org.junit.jupiter.api.Assertions.assertThrows(ex, () -> S2Util.getValue(new ThrowingRecord("a"), "name"));
        org.junit.jupiter.api.Assertions.assertThrows(ex, () -> S2Util.setValue(new ThrowingSetter(), "name", "v"));
        assertFalse(S2Util.setValue(new LombokBoolean(), "isDeleted", "not a boolean"));
        org.junit.jupiter.api.Assertions.assertThrows(io.github.devers2.s2util.exception.S2RuntimeException.class,
                () -> S2Util.createInstance(ThrowingCtor.class));
        // cast: 정수는 정확히 맞는 값만, 숫자 문자열 공백 허용, BigDecimal·enum, Boolean 은 정해진 단어만
        assertNull(S2Util.cast(3.7, Integer.class));
        assertNull(S2Util.cast(10_000_000_000L, Integer.class));
        assertEquals(1, (int) S2Util.cast(1.0, Integer.class));
        assertEquals(12, (int) S2Util.cast("12 ", Integer.class));
        assertEquals(new java.math.BigDecimal("1000.50"), S2Util.cast("1000.50", java.math.BigDecimal.class));
        assertEquals(java.time.DayOfWeek.MONDAY, S2Util.cast("MONDAY", java.time.DayOfWeek.class));
        assertEquals(Boolean.TRUE, S2Util.cast("on", Boolean.class));
        assertEquals(Boolean.TRUE, S2Util.cast("Y", Boolean.class));
        assertEquals(Boolean.FALSE, S2Util.cast("N", Boolean.class));
        assertNull(S2Util.cast("abc", Boolean.class));
        assertEquals(Boolean.FALSE, S2Util.cast("abc", false));
        // Optional: 비었거나 안의 값이 빈 값이면 빈 값
        assertTrue(S2Util.isEmpty(java.util.Optional.empty()));
        assertTrue(S2Util.isEmpty(java.util.Optional.of(" ")));
        assertFalse(S2Util.isEmpty(java.util.Optional.of("a")));
    }

    @Test
    void setValueWritesFlatMapLiteralKeyLikeGetValueReads() {
        Map<String, Object> flat = new java.util.HashMap<>();
        // 첫 마디("address")가 없으면 경로 전체를 키로 쓰고, getValue 로 그대로 읽힌다
        assertTrue(S2Util.setValue(flat, "address.zip", "12345"));
        assertTrue(S2Util.setValue(flat, "items[0].name", "pen"));
        assertEquals("12345", flat.get("address.zip"));
        assertEquals("12345", S2Util.getValue(flat, "address.zip"));
        assertEquals("pen", S2Util.getValue(flat, "items[0].name"));
        // 첫 마디가 있으면 이전처럼 중첩 경로로 쓴다
        Map<String, Object> nested = new java.util.HashMap<>();
        nested.put("address", new java.util.HashMap<String, Object>());
        assertTrue(S2Util.setValue(nested, "address.zip", "999"));
        assertEquals("999", S2Util.getValue(nested, "address.zip"));
        assertFalse(nested.containsKey("address.zip"));
        // 수정할 수 없는 Map 은 false
        assertFalse(S2Util.setValue(Map.of(), "address.zip", "x"));
        // 대소문자 구분 없는 Boolean 변환
        assertEquals(Boolean.TRUE, S2Util.cast(" TRUE ", Boolean.class));
        assertEquals(Boolean.FALSE, S2Util.cast("Off", Boolean.class));
    }

    @Test
    void s2CacheReadsJdkPublicMembersAndDoesNotThrowOnClosedModules() {
        // 공개 클래스의 공개 메서드는 모듈이 열려 있지 않아도 읽는다(이전에는 null + ERROR 로그)
        assertEquals(3, (int) S2Util.getValue("abc", "length"));
        assertEquals(2, (int) S2Util.getValue(new java.math.BigDecimal("1.50"), "scale"));
        // JDK 클래스의 필드 목록도 예외 없이(접근 설정은 열린 모듈만)
        assertTrue(S2Cache.getFields(java.util.ArrayList.class).isPresent());
        // 동적 캐시의 값 타입 검사는 로그 수준과 무관
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            java.util.function.Function<String, java.util.Optional<Integer>> wrong = (java.util.function.Function) (java.util.function.Function<String, java.util.Optional<String>>) k -> java.util.Optional.of("text");
            S2Cache.resolve("s2.test.corrupted", "k", String.class, Integer.class, 10, wrong);
        });
    }

    @Test
    void s2DateUtilReviewFixes() {
        java.util.TimeZone previous = java.util.TimeZone.getDefault();
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Seoul"));
        try {
            // 날짜만 있는 패턴도 비교(이전에는 LocalDateTime 으로만 파싱해 예외)
            assertEquals(-1, Integer.signum(S2DateUtil.compareDates("2024-01-01", "2024-02-01", "yyyy-MM-dd")));
            assertEquals(1, Integer.signum(S2DateUtil.compareDates("2024-01-01 10:00", "2024-01-01 09:00", "yyyy-MM-dd HH:mm")));
            // isThrowException=false 면 빈 입력도 null
            assertNull(S2DateUtil.parseToLocalDate("", "yyyy-MM-dd", false));
            assertNull(S2DateUtil.parseToLocalDateTime(" ", "yyyy-MM-dd HH:mm", false));
            // Instant 는 시스템 시간대, Offset/Zoned 는 자기 오프셋으로
            assertEquals("2024-01-02 00:00", S2DateUtil.dateFormat(java.time.Instant.parse("2024-01-01T15:00:00Z"), "yyyy-MM-dd HH:mm"));
            assertEquals("2024-01-01 09:30+09:00", S2DateUtil.toDateTimeString(java.time.OffsetDateTime.parse("2024-01-01T09:30:00+09:00"), "yyyy-MM-dd HH:mmXXX"));
            assertNull(S2DateUtil.toDateTimeString(java.time.LocalDateTime.of(2024, 1, 1, 0, 0), "yyyy-MM-dd XXX")); // 값에 없는 필드는 null
            // 오프셋 x 를 시간대 정보로 보고, 따옴표 리터럴 'z' 는 무시
            assertEquals(java.time.Instant.parse("2024-01-01T10:00:00Z"),
                    S2DateUtil.parseToZonedDateTime("2024-01-01 10:00 +0000", "yyyy-MM-dd HH:mm xx", "Asia/Seoul").toInstant());
            assertEquals(java.time.ZoneId.of("Asia/Seoul"),
                    S2DateUtil.parseToZonedDateTime("2024-01-01z10:00", "yyyy-MM-dd'z'HH:mm", "Asia/Seoul").getZone());
        } finally {
            java.util.TimeZone.setDefault(previous);
        }
    }

    @Test
    void s2StringUtilReviewFixes() {
        // 공백 치환 문자열은 지우지 않고 공백으로 바꾼다, 공백만 있는 원본도 처리
        assertEquals("a b", S2StringUtil.replaceChars("a_b", " ", '_'));
        assertEquals("___", S2StringUtil.replaceChars("   ", "_", ' '));
        assertEquals("xxx", S2StringUtil.replaceAll("   ", " ", "x"));
        // 대문자 약어 경계
        assertEquals("HttpServer", S2StringUtil.toPascalCase("HTTPServer"));
        assertEquals("get_http_response_code", S2StringUtil.toSnakeCase("getHTTPResponseCode"));
        assertEquals("user_id", S2StringUtil.toSnakeCase("userID"));
        assertEquals("userId", S2StringUtil.toCamelCase("user_id"));
        // 바이트 단위 자르기: 시작 바이트에서 시작하는 문자부터(걸친 문자는 빼고)
        assertEquals("bc", S2StringUtil.substringByBytes("abc", 1, 3));
        assertEquals("글", S2StringUtil.substringByBytes("한글", 3, 6));
        assertEquals("글", S2StringUtil.substringByBytes("한글", 1, 6));
        assertEquals("한", S2StringUtil.substringByBytes("한글", 0, 4));
        assertEquals("  ", S2StringUtil.substringByBytes("   ", 0, 2));
        // 인라인 <script> 안에서 블록을 닫지 못하게, JS 줄 구분자 제거
        assertEquals("a\\u003C/script>b", S2StringUtil.clearNewline("a</script>\u2028b"));
        // 잘못된 \\u 는 그대로(역슬래시 한 번), 부호는 16진수가 아님
        assertEquals("a\\uZZZZb", S2StringUtil.decodeUnicode("a\\uZZZZb"));
        assertEquals("\\u-001", S2StringUtil.decodeUnicode("\\u-001"));
        assertEquals("한", S2StringUtil.decodeUnicode("\\uD55C"));
    }

    @Test
    void templateKeyIgnoresSurroundingSpaces() {
        assertEquals("A x B", S2Template.of("A {{= name }} B").bind("name", "x").render());
        assertEquals("ORDER BY 'kim'", S2Template.of("{{=o}}").bindIn("o", java.util.List.of("kim"), "ORDER BY ").render());
    }
}
