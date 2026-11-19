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
}
