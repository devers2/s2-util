/**
 * S2Util Library
 *
 * Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)
 * Contact: eseungsu.dev@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For more information, please see the LICENSE file in the root directory.
 */
package io.github.devers2.s2util.core;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.devers2.s2util.exception.S2RuntimeException;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * Comprehensive date and time utility class for the S2Util library.
 * <p>
 * This utility focuses on the {@code java.time} package (JSR-310), providing
 * a robust interface for parsing, formatting, and manipulating temporal objects
 * ({@link LocalDate}, {@link LocalDateTime}, {@link OffsetDateTime}, {@link ZonedDateTime}).
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리의 종합 날짜 및 시간 유틸리티 클래스입니다.
 * <p>
 * Java 8 이상에서 제공하는 {@code java.time} 패키지를 효율적으로 다룰 수 있도록 래핑하여 제공합니다.
 * 파싱, 포맷팅, 시간대 변환, 날짜 비교 등 실무에서 빈번하게 발생하는 날짜 처리 로직을 추상화하여 제공합니다.
 * </p>
 *
 * <h3>Key Features (주요 기능)</h3>
 * <ul>
 * <li><b>Type Adaptability:</b> Supports seamless conversion between legacy {@code java.util.Date} /
 * {@code java.sql.Date} and modern {@code java.time} types.</li>
 * <li><b>Timezone Awareness:</b> Advanced support for {@link ZonedDateTime} and {@link OffsetDateTime},
 * including conversion between different geographical zones.</li>
 * <li><b>Reliable Validation:</b> Provides fail-safe parsing with optional exception throwing and
 * "validated or maximum date" fallback logic.</li>
 * <li><b>Domain Specific Logic:</b> Includes Korean-specific processing such as birthdate extraction
 * from resident registration numbers.</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 * @see java.time.format.DateTimeFormatter
 * @see java.time.temporal.Temporal
 */
public class S2DateUtil {

    private static final S2Logger logger = S2LogManager.getLogger(S2DateUtil.class);

    /**
     * Parses a date string into a {@link LocalDate} using the specified pattern.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 날짜 문자열과 패턴을 받아 {@link LocalDate} 객체로 변환합니다.
     *
     * @param dateString The date string to parse | 파싱할 날짜 문자열
     * @param pattern    The format pattern (e.g., "yyyy-MM-dd") | 포맷 패턴 (예: "yyyy-MM-dd")
     * @return The parsed {@link LocalDate} | 파싱된 {@link LocalDate} 객체
     * @throws DateTimeParseException If parsing fails | 파싱 실패 시 발생
     */
    public static LocalDate parseToLocalDate(String dateString, String pattern) throws DateTimeParseException {
        return parseToLocalDate(dateString, pattern, true);
    }

    /**
     * Parses a date string into a {@link LocalDate} using the specified pattern.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 날짜 문자열과 패턴을 받아 {@link LocalDate} 객체로 변환합니다.
     *
     * @param dateString       The date string to parse
     * @param pattern          The format pattern (e.g., "yyyy-MM-dd")
     * @param isThrowException Whether to throw an exception on parse failure
     * @return The parsed {@link LocalDate}, or {@code null} if parsing fails and {@code isThrowException} is false
     * @throws DateTimeParseException   If {@code isThrowException} is true and the string is invalid
     * @throws IllegalArgumentException If {@code isThrowException} is true and the pattern is invalid
     */
    public static LocalDate parseToLocalDate(String dateString, String pattern, boolean isThrowException) {
        LocalDate result = null;

        if (dateString == null || dateString.isBlank() || pattern == null || pattern.isBlank()) {
            throw new S2RuntimeException("날짜 문자열과 패턴은 비어 있을 수 없습니다.");
        }

        try {
            var formatter = DateTimeFormatter.ofPattern(pattern);
            result = LocalDate.parse(dateString, formatter);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            if (logger.isDebugEnabled()) {
                if (S2Util.isKorean(Locale.getDefault())) {
                    logger.debug("LocalDate 변환에 실패했습니다. 날짜 문자열: {}, 패턴: {}", dateString, pattern);
                } else {
                    logger.debug("Failed to convert to LocalDate. dateString: {}, pattern: {}", dateString, pattern);
                }
            }
            if (isThrowException) {
                throw e;
            }
        }

        return result;
    }

    /**
     * Parses a datetime string into a {@link LocalDateTime} using the specified pattern.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 일시 문자열과 패턴을 받아 {@link LocalDateTime} 객체로 변환합니다.
     *
     * @param dateTimeString The datetime string (requires time components) | 일시 문자열 (시간 정보 포함 필수)
     * @param pattern        The format pattern (e.g., "yyyy-MM-dd HH:mm:ss") | 포맷 패턴 (예: "yyyy-MM-dd HH:mm:ss")
     * @return The parsed {@link LocalDateTime} | 파싱된 {@link LocalDateTime} 객체
     * @throws DateTimeParseException If parsing fails | 파싱 실패 시 발생
     */
    public static LocalDateTime parseToLocalDateTime(String dateTimeString, String pattern) throws DateTimeParseException {
        return parseToLocalDateTime(dateTimeString, pattern, true);
    }

    /**
     * Parses a datetime string into a {@link LocalDateTime} with an option to throw exceptions.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 일시 문자열과 패턴을 받아 {@link LocalDateTime} 객체로 변환합니다. (예외 발생 여부 선택 가능)
     *
     * @param dateTimeString   The datetime string
     * @param pattern          The format pattern
     * @param isThrowException Whether to throw an exception on failure
     * @return The parsed {@link LocalDateTime}, or {@code null} if failed and {@code isThrowException} is false
     */
    public static LocalDateTime parseToLocalDateTime(String dateTimeString, String pattern, boolean isThrowException) {
        LocalDateTime result = null;

        if (dateTimeString == null || dateTimeString.isBlank() || pattern == null || pattern.isBlank()) {
            throw new S2RuntimeException("일시 문자열과 패턴은 비어 있을 수 없습니다.");
        }

        try {
            var formatter = DateTimeFormatter.ofPattern(pattern);
            result = LocalDateTime.parse(dateTimeString, formatter);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            if (logger.isDebugEnabled()) {
                if (S2Util.isKorean(Locale.getDefault())) {
                    logger.debug("LocalDateTime 변환에 실패했습니다. 날짜 문자열: {}, 패턴: {}", dateTimeString, pattern);
                } else {
                    logger.debug("Failed to convert to LocalDateTime. dateTimeString: {}, pattern: {}", dateTimeString, pattern);
                }
            }
            if (isThrowException) {
                throw e;
            }
        }

        return result;
    }

    /**
     * Validates whether a datetime string matches the given pattern.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 일시 문자열이 지정된 패턴에 부합하는 유효한 일시인지 확인합니다.
     *
     * @param dateTimeString The datetime string to validate
     * @param pattern        The format pattern
     * @return {@code true} if valid, {@code false} otherwise
     * @throws DateTimeParseException If validation fails and defaults to throwing
     */
    public static boolean isValidDate(String dateTimeString, String pattern) throws DateTimeParseException {
        return isValidDate(dateTimeString, pattern, true);
    }

    /**
     * Validates a datetime string with an option to throw exceptions.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 일시 문자열이 유효한 일시인지 확인합니다. (예외 발생 여부 선택 가능)
     *
     * @param dateTimeString   The datetime string
     * @param pattern          The format pattern
     * @param isThrowException Whether to throw an exception on invalid format
     * @return {@code true} if valid, {@code false} otherwise
     */
    public static boolean isValidDate(String dateTimeString, String pattern, boolean isThrowException) {
        var result = false;
        if (dateTimeString != null && !dateTimeString.isBlank() && pattern != null && !pattern.isBlank()) {
            try {
                var formatter = DateTimeFormatter.ofPattern(pattern);
                formatter.parse(dateTimeString);
                result = true;
            } catch (DateTimeParseException | IllegalArgumentException e) {
                if (logger.isDebugEnabled()) {
                    if (S2Util.isKorean(Locale.getDefault())) {
                        logger.debug("유효한 일시 여부 확인에 실패했습니다. 날짜 문자열: {}, 패턴: {}", dateTimeString, pattern);
                    } else {
                        logger.debug("Failed to validate date/time. dateTimeString: {}, pattern: {}", dateTimeString, pattern);
                    }
                }
                if (isThrowException) {
                    throw e;
                }
            }
        }
        return result;
    }

    /**
     * Compares two datetime strings based on the given pattern.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 두 일시 문자열을 비교하여 선후 관계를 확인합니다.
     *
     * @param dateTimeString          The datetime string to compare
     * @param referenceDateTimeString The reference datetime string
     * @param pattern                 The format pattern
     * @return Negative if earlier, zero if equal, positive if later than reference
     * @throws DateTimeParseException If parsing fails
     */
    public static int compareDates(String dateTimeString, String referenceDateTimeString, String pattern) throws DateTimeParseException {
        if (dateTimeString == null || dateTimeString.isBlank() || referenceDateTimeString == null || referenceDateTimeString.isBlank() || pattern == null || pattern.isBlank()) {
            throw new S2RuntimeException("일시 문자열, 패턴 또는 참조 일시는 null일 수 없습니다.");
        }

        var formatter = DateTimeFormatter.ofPattern(pattern);

        // 기준 일시 문자열을 LocalDate 객체로 파싱
        var referenceDate = LocalDateTime.parse(referenceDateTimeString, formatter);

        return compareDates(dateTimeString, pattern, referenceDate);
    }

    /**
     * Compares a datetime string against a {@link LocalDateTime} object.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 일시 문자열을 {@link LocalDateTime} 객체와 비교합니다.
     *
     * @param dateTimeString The datetime string to compare
     * @param pattern        The format pattern
     * @param referenceDate  The reference {@link LocalDateTime} object
     * @return Negative if earlier, zero if equal, positive if later than reference
     * @throws DateTimeParseException If parsing fails
     */
    public static int compareDates(String dateTimeString, String pattern, LocalDateTime referenceDate) throws DateTimeParseException {
        if (dateTimeString == null || dateTimeString.isBlank() || pattern == null || pattern.isBlank() || referenceDate == null) {
            throw new S2RuntimeException("일시 문자열, 패턴 또는 참조 일시는 null일 수 없습니다.");
        }

        var formatter = DateTimeFormatter.ofPattern(pattern);

        // 일시 문자열을 LocalDate 객체로 파싱
        var dateToCompare = LocalDateTime.parse(dateTimeString, formatter);

        // LocalDate의 compareTo 메서드를 사용하여 비교
        return dateToCompare.compareTo(referenceDate);
    }

    /**
     * Parses a datetime string with offset into an {@link OffsetDateTime}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 일시 문자열, 패턴, 시간대 오프셋을 받아 {@link OffsetDateTime} 객체로 변환합니다.
     *
     * <h3>Pattern Examples (패턴 예시)</h3>
     * <ul>
     * <li>{@code yyyy-MM-dd'T'HH:mm:ssXXX} &rarr; {@code 2025-10-12T10:00:00+09:00} (ISO 8601)</li>
     * <li>{@code O}: GMT+9</li>
     * <li>{@code X}: +09, Z (UTC: +00)</li>
     * <li>{@code XXX}: +09:00</li>
     * </ul>
     *
     * @param dateTimeString The datetime string (must include offset)
     * @param pattern        The pattern including offset indicators (O, X, Z)
     *                       yyyy-MM-dd'T'HH:mm:ssXXX → 2025-10-12T10:00:00+09:00 ('T': ISO 8601 국제표준, 'T' 또는 공백으로 날짜와 시간을 구분)
     *                       yyyy-MM-dd HH:mm:ss O → 2025-10-12 10:00:00 GMT+9
     *                       O: GMT+9
     *                       OO, OOO, OOOO: GMT+9
     *                       X: +09, Z {@code {+09: KST(한국표준시), Z: UTC(+00)}}
     *                       XX: +0900, Z
     *                       XXX: +09:00, Z
     *                       XXXX: +090000, Z
     *                       XXXXX: +09:00:00, Z
     *                       Z, ZZ, ZZZ: +0900
     *                       ZZZZ: GMT+09:00
     *                       ZZZZZ: +09:00, Z
     * @return The parsed {@link OffsetDateTime}
     * @throws DateTimeParseException if the string cannot be parsed
     */
    public static OffsetDateTime parseToOffsetDateTime(String dateTimeString, String pattern) throws DateTimeParseException {
        return parseToOffsetDateTime(dateTimeString, pattern, true);
    }

    /**
     * Parses a datetime string with offset into an {@link OffsetDateTime} with an option to throw exceptions.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 일시 문자열, 패턴, 시간대 오프셋을 받아 {@link OffsetDateTime} 객체로 변환합니다. (예외 발생 여부 선택 가능)
     *
     * @param dateTimeString   The datetime string (must include offset)
     * @param pattern          The pattern including offset indicators (O, X, Z)
     *                         yyyy-MM-dd'T'HH:mm:ssXXX → 2025-10-12T10:00:00+09:00 ('T': ISO 8601 국제표준, 'T' 또는 공백으로 날짜와 시간을 구분)
     *                         yyyy-MM-dd HH:mm:ss O → 2025-10-12 10:00:00 GMT+9
     *                         O: GMT+9
     *                         OO, OOO, OOOO: GMT+9
     *                         X: +09, Z {@code {+09: KST(한국표준시), Z: UTC(+00)}}
     *                         XX: +0900, Z
     *                         XXX: +09:00, Z
     *                         XXXX: +090000, Z
     *                         XXXXX: +09:00:00, Z
     *                         Z, ZZ, ZZZ: +0900
     *                         ZZZZ: GMT+09:00
     *                         ZZZZZ: +09:00, Z
     * @param isThrowException Whether to throw an exception on failure
     * @return The parsed {@link OffsetDateTime}, or {@code null} if failed and {@code isThrowException} is false
     */
    public static OffsetDateTime parseToOffsetDateTime(String dateTimeString, String pattern, boolean isThrowException) {
        OffsetDateTime result = null;

        if (dateTimeString == null || dateTimeString.isBlank() || pattern == null || pattern.isBlank()) {
            if (isThrowException) {
                throw new S2RuntimeException("일시 문자열과 패턴은 비어 있을 수 없습니다.");
            } else {
                if (logger.isDebugEnabled()) {
                    if (S2Util.isKorean(Locale.getDefault())) {
                        logger.debug("OffsetDateTime 변환에 실패했습니다. 날짜 문자열: {}, 패턴: {}", dateTimeString, pattern);
                    } else {
                        logger.debug("Failed to convert to OffsetDateTime. dateTimeString: {}, pattern: {}", dateTimeString, pattern);
                    }
                }
                return null;
            }
        }

        try {
            var formatter = DateTimeFormatter.ofPattern(pattern);
            result = OffsetDateTime.parse(dateTimeString, formatter);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            if (isThrowException) {
                throw e;
            }
        }

        return result;
    }

    /**
     * Converts an {@link OffsetDateTime} to a {@link LocalDateTime} (stripping the offset).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@link OffsetDateTime}에서 오프셋 정보를 제거하고 로컬 시간 부분을 {@link LocalDateTime}으로 반환합니다.
     *
     * @param offsetDateTime The {@link OffsetDateTime} to convert
     * @return The resulting {@link LocalDateTime}
     */
    public static LocalDateTime toLocalDateTime(OffsetDateTime offsetDateTime) {
        if (Objects.isNull(offsetDateTime)) {
            return null;
        }
        return offsetDateTime.toLocalDateTime();
    }

    /**
     * Converts a {@link LocalDateTime} to an {@link OffsetDateTime} by applying the specified offset.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@link LocalDateTime}에 지정된 {@link ZoneOffset}을 적용하여 {@link OffsetDateTime}으로 변환합니다.
     *
     * @param localDateTime The {@link LocalDateTime} to convert
     * @param offset        The target {@link ZoneOffset}
     * @return The resulting {@link OffsetDateTime}
     */
    public static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime, ZoneOffset offset) {
        if (Objects.isNull(localDateTime) || Objects.isNull(offset)) {
            return null;
        }
        // atOffset() 메서드를 사용하여 LocalDateTime에 오프셋 정보를 부여
        return localDateTime.atOffset(offset);
    }

    /**
     * Converts a {@link LocalDateTime} to an {@link OffsetDateTime} using the system's default timezone.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@link LocalDateTime}을 현재 시스템의 기본 시간대를 사용하여 {@link OffsetDateTime}으로 변환합니다.
     *
     * @param localDateTime The {@link LocalDateTime} to convert
     * @return The resulting {@link OffsetDateTime}
     */
    public static OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (Objects.isNull(localDateTime)) {
            return null;
        }
        // 시스템 기본 ZoneId
        var systemZone = ZoneId.systemDefault();

        // ZonedDateTime으로 변환 후 오프셋 정보만 추출하여 OffsetDateTime으로 변환
        return localDateTime.atZone(systemZone).toOffsetDateTime();
    }

    /**
     * Formats a {@link LocalDateTime} into a string based on the specified pattern.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@link LocalDateTime} 객체를 지정된 패턴의 문자열로 변환합니다.
     *
     * @param localDateTime The {@link LocalDateTime} to format
     * @param pattern       The target format pattern
     * @return The formatted string
     */
    public static String toDateTimeString(LocalDateTime localDateTime, String pattern) {
        if (Objects.isNull(localDateTime) || pattern == null || pattern.isBlank()) {
            return null;
        }
        try {
            var formatter = DateTimeFormatter.ofPattern(pattern);
            return localDateTime.format(formatter);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            if (logger.isDebugEnabled()) {
                if (S2Util.isKorean(Locale.getDefault())) {
                    logger.debug("LocalDateTime 문자열 변환에 실패했습니다. {}", e.getMessage());
                } else {
                    logger.debug("Failed to convert LocalDateTime to string. {}", e.getMessage());
                }
            }
            return null;
        }
    }

    /**
     * Extracts the local time components from an {@link OffsetDateTime} and formats them.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@link OffsetDateTime}에서 오프셋을 제외한 로컬 시간 부분만을 추출하여 문자열로 변환합니다.
     *
     * @param offsetDateTime The {@link OffsetDateTime} to format
     * @param pattern        The pattern (should not contain offset symbols)
     * @return The formatted string
     */
    public static String toDateTimeString(OffsetDateTime offsetDateTime, String pattern) {
        if (Objects.isNull(offsetDateTime) || pattern == null || pattern.isBlank()) {
            return null;
        }

        return toDateTimeString(toLocalDateTime(offsetDateTime), pattern);
    }

    /**
     * Formats an {@link OffsetDateTime} into an absolute instant string (including timezone).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@link OffsetDateTime} 객체를 지정된 패턴의 문자열로 변환합니다. (오프셋/타임존 정보 포함)
     *
     * @param offsetDateTime The {@link OffsetDateTime} to format
     * @param pattern        The pattern (must include offset symbols like XXX, O, Z)
     * @return The formatted instant string
     *         yyyy-MM-dd'T'HH:mm:ssXXX → 2025-10-12T10:00:00+09:00 ('T': ISO 8601 국제표준, 'T' 또는 공백으로 날짜와 시간을 구분)
     *         yyyy-MM-dd HH:mm:ss O → 2025-10-12 10:00:00 GMT+9
     *         O: GMT+9
     *         OO, OOO, OOOO: GMT+9
     *         X: +09, Z {@code {+09: KST(한국표준시), Z: UTC(+00)}}
     *         XX: +0900, Z
     *         XXX: +09:00, Z
     *         XXXX: +090000, Z
     *         XXXXX: +09:00:00, Z
     *         Z, ZZ, ZZZ: +0900
     *         ZZZZ: GMT+09:00
     *         ZZZZZ: +09:00, Z
     */
    public static String toInstantString(OffsetDateTime offsetDateTime, String pattern) {
        if (Objects.isNull(offsetDateTime) || pattern == null || pattern.isBlank()) {
            return null;
        }
        try {
            var formatter = DateTimeFormatter.ofPattern(pattern);
            return offsetDateTime.format(formatter);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            // 패턴 오류 처리 (주로 오프셋 기호(XXX)가 빠졌을 때 발생)
            if (logger.isDebugEnabled()) {
                if (S2Util.isKorean(Locale.getDefault())) {
                    logger.debug("OffsetDateTime 문자열 변환에 실패했습니다. {}", e.getMessage());
                } else {
                    logger.debug("Failed to convert OffsetDateTime to string. {}", e.getMessage());
                }
            }
            return null;
        }
    }

    /**
     * Parses a date string and returns it if valid and not after the maximumDate;
     * otherwise, returns the maximumDate itself.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 주어진 날짜 문자열을 파싱하고 유효성을 검사하여, 최대 허용 날짜 이전의 날짜 또는
     * 최대 허용 날짜(Maximum Date)를 반환합니다.
     * <p>
     * 입력값이 비어있거나, 파싱에 실패하거나, 파싱된 날짜가 최대치를 초과한 경우 {@code maximumDate}를 반환합니다.
     * </p>
     *
     * @param dateString  Date string to validate
     * @param pattern     Pattern for parsing (e.g., "yyyy-MM-dd")
     * @param maximumDate The upper limit date to return if validation fails or limit is exceeded
     * @return A valid {@link LocalDate} not exceeding {@code maximumDate}
     * @throws NullPointerException if {@code pattern} or {@code maximumDate} is null
     */
    public static LocalDate getValidatedOrMaximumDate(String dateString, String pattern, LocalDate maximumDate) {
        Objects.requireNonNull(pattern, "날짜 형식 패턴(pattern)은 null일 수 없습니다.");
        Objects.requireNonNull(maximumDate, "최대 날짜(maximumDate)는 null일 수 없습니다.");

        if (dateString == null || dateString.isBlank()) {
            return maximumDate;
        }

        try {
            var formatter = DateTimeFormatter.ofPattern(pattern);
            var date = LocalDate.parse(dateString, formatter);

            // 대상 날짜가 최대 허용 날짜 이전이라면 대상 날짜, 이후라면 최대 허용 날짜를 반환한다.
            return date.isAfter(maximumDate) ? maximumDate : date;
        } catch (DateTimeException e) {
            return maximumDate;
        }
    }

    /**
     * Validates and returns the parsed date string, or the {@code maximumDate} if invalid or exceeded.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 주어진 날짜 문자열을 파싱/검증하여, 유효한 날짜 또는 최대 허용 날짜({@code maximumDate})를 패턴 형식으로 반환합니다.
     *
     * @param dateString  The date string to validate
     * @param pattern     The format pattern
     * @param maximumDate The upper limit
     * @return The formatted date string (within the limit)
     */
    public static String getValidatedOrMaximumDateString(String dateString, String pattern, LocalDate maximumDate) {
        var date = getValidatedOrMaximumDate(dateString, pattern, maximumDate);
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Gets the current time at the specified zone.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 특정 지역의 현재 시간을 {@link ZonedDateTime} 객체로 반환합니다.
     *
     * @param zoneIdStr The zone identifier (e.g., "Asia/Seoul")
     * @return The current {@link ZonedDateTime} for that zone
     */
    public static ZonedDateTime nowAtZone(String zoneIdStr) {
        if (zoneIdStr == null || zoneIdStr.isBlank()) {
            return ZonedDateTime.now(ZoneId.systemDefault());
        }
        return ZonedDateTime.now(ZoneId.of(zoneIdStr));
    }

    /**
     * Converts a {@link ZonedDateTime} to a different timezone while keeping the same instant.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 원본 시점을 유지하면서 대상 지역의 시간으로 변환합니다. (예: 서울 -> 뉴욕)
     *
     * @param source          The original {@link ZonedDateTime}
     * @param targetZoneIdStr The identifier of the target zone
     * @return The converted {@link ZonedDateTime}
     */
    public static ZonedDateTime convertToZone(ZonedDateTime source, String targetZoneIdStr) {
        if (source == null || targetZoneIdStr == null || targetZoneIdStr.isBlank()) {
            return source;
        }
        return source.withZoneSameInstant(ZoneId.of(targetZoneIdStr));
    }

    /**
     * Parses a datetime string into a {@link ZonedDateTime} using a pattern and an optional zone ID.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 일시 문자열, 패턴, 지역 ID를 받아 {@link ZonedDateTime} 객체로 변환합니다.
     *
     * @param dateTimeString The datetime string
     * @param pattern        The format pattern
     *                       예시:
     *                       - "yyyy-MM-dd HH:mm:ss" (지역 ID를 별도 인자로 받을 때)
     *                       - "yyyy-MM-dd HH:mm:ss VV" -> 2026-01-03 20:00:00 Asia/Seoul
     *                       - "yyyy-MM-dd HH:mm:ss XXX '['VV']'" -> 2026-01-03 20:00:00+09:00 [Asia/Seoul]
     * @param zoneIdStr      The fallback zone ID if none present in the pattern
     * @return The parsed {@link ZonedDateTime}
     */
    public static ZonedDateTime parseToZonedDateTime(String dateTimeString, String pattern, String zoneIdStr) {
        if (dateTimeString == null || dateTimeString.isBlank() || pattern == null || pattern.isBlank()) {
            return null;
        }

        try {
            var formatter = DateTimeFormatter.ofPattern(pattern);
            var zoneId = (zoneIdStr == null || zoneIdStr.isBlank()) ? ZoneId.systemDefault() : ZoneId.of(zoneIdStr);

            // 패턴에 지역 정보(V)나 오프셋 정보가 포함되어 있는지 확인하여 파싱함
            if (pattern.contains("V") || pattern.contains("z") || pattern.contains("X") || pattern.contains("Z")) {
                return ZonedDateTime.parse(dateTimeString, formatter);
            } else {
                // 시간 정보만 있는 경우 입력받은 zoneId와 결합함
                var localDateTime = java.time.LocalDateTime.parse(dateTimeString, formatter);
                return ZonedDateTime.of(localDateTime, zoneId);
            }
        } catch (java.time.DateTimeException | IllegalArgumentException e) {
            if (logger.isDebugEnabled()) {
                if (S2Util.isKorean(Locale.getDefault())) {
                    logger.debug("ZonedDateTime 변환에 실패했습니다. 날짜 문자열: {}, 패턴: {}", dateTimeString, pattern);
                } else {
                    logger.debug("Failed to convert to ZonedDateTime. dateTimeString: {}, pattern: {}", dateTimeString, pattern);
                }
            }
            return null;
        }
    }

    /**
     * Formats a {@link ZonedDateTime} into a string based on the specified pattern.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * {@link ZonedDateTime} 객체에 지정된 패턴을 적용하여 문자열로 반환합니다.
     *
     * @param zdt     The {@link ZonedDateTime} to format
     * @param pattern The target pattern
     * @return The formatted string
     */
    public static String toZonedString(ZonedDateTime zdt, String pattern) {
        if (zdt == null || pattern == null || pattern.isBlank()) {
            return null;
        }
        try {
            return zdt.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                if (S2Util.isKorean(Locale.getDefault())) {
                    logger.debug("ZonedDateTime 문자열 변환에 실패했습니다. {}", e.getMessage());
                } else {
                    logger.debug("Failed to convert ZonedDateTime to string. {}", e.getMessage());
                }
            }
            return null;
        }
    }

    /**
     * Converts an input object to a {@link Temporal} object with the highest possible precision.
     * <p>
     * Prioritization: {@link ZonedDateTime} &gt; {@link OffsetDateTime} &gt; {@link LocalDateTime} &gt;
     * {@link LocalDate} (converted via {@code atStartOfDay()}).
     * If native types are not matched, an optional custom converter is applied.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 입력된 객체를 분석하여 가능한 가장 높은 정밀도의 {@link Temporal} 객체로 변환합니다.
     * <p>
     * 우선순위: ZonedDateTime &gt; OffsetDateTime &gt; LocalDateTime &gt; LocalDate (00시 변환).
     * 기본 타입으로 처리가 안 될 경우, 외부에서 주입된 customConverter를 통해 최종 변환을 시도합니다.
     * </p>
     *
     * @param value           The input object (Date, Temporal, etc.)
     * @param customConverter Optional function for additional conversion logic
     * @return The converted {@link Temporal} object, or {@code null} if failed
     */
    public static Temporal toMaxPrecisionTemporal(Object value, Function<Object, Temporal> customConverter) {
        if (value == null)
            return null;

        // 1. ZonedDateTime: 타임존 정보까지 포함한 최상위 타입
        if (value instanceof ZonedDateTime zdt) {
            return zdt;
        }
        // 2. OffsetDateTime: UTC 오프셋 정보를 포함
        if (value instanceof OffsetDateTime odt) {
            return odt;
        }
        // 3. LocalDateTime: 날짜와 시간 정보를 포함
        if (value instanceof LocalDateTime ldt) {
            return ldt;
        }
        // 4. LocalDate: 날짜만 존재하므로 00시 00분으로 변환
        if (value instanceof LocalDate ld) {
            return ld.atStartOfDay();
        }
        // 5. 기타 Temporal 구현체 처리
        if (value instanceof Temporal t) {
            return t;
        }

        // 사용자 정의 컨버터 시도
        if (customConverter != null) {
            try {
                Object result = customConverter.apply(value);
                // 리턴값이 실제로 Temporal의 구현체인지 최종 확인
                if (result instanceof Temporal t) {
                    return t;
                } else if (result != null) {
                    // 잘못된 타입이 리턴된 경우 로그를 남기고 무시
                    if (logger.isDebugEnabled()) {
                        if (S2Util.isKorean(Locale.getDefault())) {
                            logger.debug("사용자 정의 컨버터가 Temporal이 아닌 타입을 반환했습니다: {}", result.getClass().getName());
                        } else {
                            logger.debug("Custom converter returned a non-Temporal type: {}", result.getClass().getName());
                        }
                    }
                }
            } catch (Exception e) {
                // 로그 출력 시 경어체를 사용함.
                if (logger.isDebugEnabled()) {
                    if (S2Util.isKorean(Locale.getDefault())) {
                        logger.debug("사용자 정의 컨버터 실행 중 예외가 발생했습니다. 값: {}", value, e);
                    } else {
                        logger.debug("Exception occurred while executing custom converter. value: {}", value, e);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Formats various types of date/time objects into a string using the specified pattern.
     * <p>
     * Supports legacy {@link java.util.Date}, {@link java.sql.Date}, and modern {@code java.time} types.
     * If the input is a string, it attempts to infer the date components based on its length
     * (e.g., 14 chars for yyyyMMddHHmmss).
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 입력된 객체(Date, LocalDateTime, Number, String 등)를 특정 규격의 문자열로 포맷팅합니다.
     * <p>
     * 레거시 Date 클래스와 최신 Time API를 모두 지원하며, 문자열 입력 시 길이를 분석하여
     * 날짜 성분을 추론(Inference)하는 지능형 기능을 포함합니다.
     * </p>
     *
     * @param <T>      Type of the input object
     * @param pValue   The date/time object to format
     * @param pPattern The target format pattern (DateTimeFormatter pattern, yyyy 년 MM 월 dd 일 HH 시 mm 분 ss 초 E 요일)
     * @return The formatted date string, or the original string representation if formatting fails
     */
    public static <T> String dateFormat(T pValue, String pPattern) {
        var result = "";

        if (pValue != null) {
            var oriValue = String.valueOf(pValue);

            if (pPattern != null && !pPattern.isBlank()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pPattern);

                if (pValue instanceof java.time.LocalDateTime ldt) {
                    result = ldt.format(formatter);
                } else if (pValue instanceof java.time.LocalDate ld) {
                    result = ld.format(formatter);
                } else if (pValue instanceof java.sql.Date sqlDate) {
                    result = sqlDate.toLocalDate().atStartOfDay().format(formatter);
                } else if (pValue instanceof java.util.Date date) {
                    // Date를 LocalDateTime으로 변환하여 처리함
                    result = date.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
                            .format(formatter);
                } else {
                    var value = S2StringUtil.replaceAll(oriValue, "[^0-9]", "");
                    LocalDateTime ldtParsed = null;

                    try {
                        if (value.length() >= 14) {
                            // yyyyMMddHHmmss
                            ldtParsed = LocalDateTime.of(
                                    Integer.parseInt(value.substring(0, 4)),
                                    Integer.parseInt(value.substring(4, 6)),
                                    Integer.parseInt(value.substring(6, 8)),
                                    Integer.parseInt(value.substring(8, 10)),
                                    Integer.parseInt(value.substring(10, 12)),
                                    Integer.parseInt(value.substring(12, 14))
                            );
                        } else if (value.length() >= 12) {
                            // yyyyMMddHHmm
                            ldtParsed = LocalDateTime.of(
                                    Integer.parseInt(value.substring(0, 4)),
                                    Integer.parseInt(value.substring(4, 6)),
                                    Integer.parseInt(value.substring(6, 8)),
                                    Integer.parseInt(value.substring(8, 10)),
                                    Integer.parseInt(value.substring(10, 12)), 0
                            );
                        } else if (value.length() >= 10) {
                            // yyyyMMddHH
                            ldtParsed = LocalDateTime.of(
                                    Integer.parseInt(value.substring(0, 4)),
                                    Integer.parseInt(value.substring(4, 6)),
                                    Integer.parseInt(value.substring(6, 8)),
                                    Integer.parseInt(value.substring(8, 10)), 0, 0
                            );
                        } else if (value.length() >= 8) {
                            // yyyyMMdd
                            ldtParsed = LocalDateTime.of(
                                    Integer.parseInt(value.substring(0, 4)),
                                    Integer.parseInt(value.substring(4, 6)),
                                    Integer.parseInt(value.substring(6, 8)), 0, 0, 0
                            );
                        } else if (value.length() >= 6) {
                            // yyyyMM
                            ldtParsed = LocalDateTime.of(
                                    Integer.parseInt(value.substring(0, 4)),
                                    Integer.parseInt(value.substring(4, 6)), 1, 0, 0, 0
                            );
                        }

                        if (ldtParsed != null) {
                            result = ldtParsed.format(formatter);
                        }
                    } catch (Exception e) {
                        System.err.println("날짜 변환 중 오류가 발생하였습니다: " + e.getMessage());
                    }
                }
            }

            if (result == null || result.isBlank()) {
                result = oriValue;
            }
        }

        return result;
    }

    /**
     * Extracts the birthdate from a Korean Resident Registration Number (or Foreigner Registration Number).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 주민등록번호(또는 외국인등록번호)에서 생년월일을 추출합니다.
     * <p>
     * 뒷자리의 첫 번째 숫자(성별/세기 구분자)를 분석하여 1800년대, 1900년대, 2000년대 출생을 정확히 판별합니다.
     * </p>
     *
     * @param resdNo   Resident Registration Number (with or without hyphens)
     * @param is8digit Whether to return an 8-digit birthdate (e.g., "19900101") or 6-digit (e.g., "900101")
     * @return The birthdate string
     */
    public static String getBirthDayFromResdNo(String resdNo, boolean is8digit) {
        var birthDay = "";

        var resdNoVal = resdNo != null ? S2StringUtil.replaceAll(resdNo, "[^0-9]", "") : "";
        if (resdNoVal != null && !resdNoVal.isBlank() && resdNoVal.length() == 13) {
            birthDay = resdNoVal.substring(0, 6);

            if (is8digit) {
                switch (resdNoVal.substring(6, 7)) {
                    case "9", "0" -> birthDay = "18" + birthDay;
                    case "1", "2", "5", "6" -> birthDay = "19" + birthDay;
                    case "3", "4", "7", "8" -> birthDay = "20" + birthDay;
                }
            }
        }

        return birthDay;
    }

    /**
     * Generates a list of years between the start and end values.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 시작 연도와 종료 연도 범위를 포함하는 연도 목록을 생성합니다.
     *
     * @param startYy Start year (defaults to current year)
     * @param endYy   End year (defaults to current year)
     * @return List of years
     */
    public static List<Integer> getYearList(Integer startYy, Integer endYy) {
        var curYy = Year.now().getValue();
        var minYy = curYy - 100;
        var maxYy = 9999;

        // 입력값이 null이면 현재 연도를 기본값으로 사용하고, 범위를 제한함
        var finalStartYy = startYy == null ? curYy : Math.max(minYy, Math.min(startYy, maxYy));
        var finalEndYy = endYy == null ? curYy : Math.max(minYy, Math.min(endYy, maxYy));

        if (finalStartYy > finalEndYy) {
            // 시작 연도가 크면 역순으로 정렬하여 반환
            return IntStream.rangeClosed(finalEndYy, finalStartYy)
                    .boxed()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
        } else {
            // 종료 연도가 크거나 같으면 정순으로 반환
            return IntStream.rangeClosed(finalStartYy, finalEndYy)
                    .boxed()
                    .collect(Collectors.toList());
        }
    }

}
