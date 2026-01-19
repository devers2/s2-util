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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance string manipulation utility for the S2Util library.
 * <p>
 * This class provides advanced string processing capabilities, prioritizing
 * memory efficiency and execution speed. It includes specialized logic for
 * Korean language processing (e.g., automated postposition selection) and
 * case conversions.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리의 고성능 문자열 처리 유틸리티 클래스입니다.
 * <p>
 * 단순한 문자열 조작을 넘어, 메모리 효율성과 실행 속도를 최우선으로 설계되었습니다.
 * 한국어 특유의 조사 처리(은/는, 이/가 등) 자동화 로직과 다양한 케이스 변환(Camel, Snake 등) 기능을 제공합니다.
 * </p>
 *
 * <h3>Optimization Strategies (최적화 전략)</h3>
 * <ul>
 * <li><b>O(N) Traversal:</b> Many methods avoid heavy regex engines for simple character replacements,
 * performing single-pass O(N) traversals with {@link StringBuilder}.</li>
 * <li><b>Regex Caching:</b> Integration with {@link S2Cache} ensures that pattern compilation
 * overhead is minimized.</li>
 * <li><b>Lookup Tables:</b> Uses boolean arrays (Lookup Tables) for character set matching to
 * achieve O(1) lookup performance during replacement tasks.</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public class S2StringUtil {

    private S2StringUtil() {
        // Prevent instantiation
    }

    /**
     * Replaces each substring of the source string that matches the given regular expression.
     * <p>
     * <b>Performance Tip:</b> This method is significantly faster than {@link String#replaceAll(String, String)}
     * because it uses pre-compiled patterns from {@link S2Cache}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 주어진 정규표현식과 일치하는 모든 부분을 새로운 문자열로 치환합니다.
     * <p>
     * 표준 {@code String.replaceAll()} 대비 내부적으로 캐싱된 패턴을 활용하므로 반복 호출 시 성능이 우수합니다.
     * </p>
     *
     * @param source      The original string
     * @param regex       The regular expression to match
     * @param replacement The string to be substituted for each match
     * @return The resulting string
     */
    public static String replaceAll(String source, String regex, String replacement) {
        if (source == null || source.isBlank() || regex == null || regex.isBlank() || replacement == null || replacement.isBlank()) {
            return source;
        }

        return S2Cache.getPattern(regex)
                .map(pattern -> pattern.matcher(source).replaceAll(replacement))
                .orElse(source);
    }

    /**
     * Replaces all occurrences of specified characters with a single replacement string.
     * <p>
     * This implementation bypasses the regex engine and uses a <b>Lookup Table</b> (boolean array)
     * for O(1) character checking, combined with a single-pass {@link StringBuilder} traversal.
     * This makes it substantially faster and more memory-efficient than regex-based alternatives.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 원본 문자열에서 지정된 여러 개의 대상 문자들을 찾아 지정된 문자열로 치환합니다.
     * <p>
     * 정규식 엔진을 거치지 않고 직접 루프를 순회하며, 불린 배열 기반의 룩업 테이블을 사용하여
     * 극도의 속도와 메모리 효율을 보장합니다.
     * </p>
     *
     * <pre>{@code
     * // Remove hyphens and dots
     * S2StringUtil.replaceChars("010-1234.5678", "", '-', '.'); // Result: "01012345678"
     * }</pre>
     *
     * @param source      The original string
     * @param replacement The string to substitute (use "" for removal)
     * @param targets     Characters to be replaced (varargs)
     * @return The modified string, or original if no modifications occurred
     */
    public static String replaceChars(String source, String replacement, char... targets) {
        if (source == null || source.isBlank() || targets == null || targets.length == 0) {
            return source;
        }

        // 1. 빠른 조회를 위한 Lookup Table 생성 (유니코드 대응)
        // 메모리 점유율을 줄이고 싶다면 BitSet 사용을 고려할 수 있으나 성능 면에서는 boolean 배열이 가장 빠르다.
        boolean[] targetMap = new boolean[65536];
        for (char t : targets) {
            targetMap[t] = true;
        }

        int len = source.length();
        StringBuilder sb = new StringBuilder(len);
        boolean modified = false;

        // 2. 단일 순회 치환 로직
        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);
            if (targetMap[c]) {
                if (replacement != null && !replacement.isBlank()) {
                    sb.append(replacement);
                }
                modified = true;
            } else {
                sb.append(c);
            }
        }

        // 3. 수정된 사항이 없다면 원본 반환 (메모리 절약)
        return modified ? sb.toString() : source;
    }

    /**
     * Removes all occurrences of specified characters from the source string.
     * <p>
     * This is a convenience wrapper for {@link #replaceChars(String, String, char...)}
     * with an empty replacement.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 원본 문자열에서 지정된 여러 개의 문자들을 모두 제거합니다.
     *
     * @param source  The original string | 원본 문자열
     * @param targets Characters to be removed (varargs) | 제거할 문자들 (가변 인자)
     * @return The resulting string with target characters removed | 대상 문자들이 제거된 문자열
     */
    public static String removeChars(String source, char... targets) {
        return replaceChars(source, "", targets);
    }

    private static final String SPLIT_REGEX = "(?<=[a-z0-9])(?=[A-Z])|[-_\\s.]+";

    /**
     * Capitalizes the first letter of a string and lowers the rest.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 문자열의 첫 글자를 대문자로, 나머지를 소문자로 변환합니다.
     *
     * @param str The string to process
     * @return Formatted string
     */
    private static String capitalizeFirstLetter(String str) {
        if (str == null || str.isBlank())
            return str;
        if (str.length() == 1)
            return str.toUpperCase();
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Converts a string to PascalCase (e.g., "hello_world" to "HelloWorld").
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 입력 문자열을 PascalCase 형식으로 변환합니다. (모든 단어의 첫 글자를 대문자로)
     *
     * @param input The original string | 원본 문자열
     * @return PascalCase string | PascalCase 문자열
     */
    public static String toPascalCase(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        var optionalPattern = S2Cache.getPattern(SPLIT_REGEX);
        if (optionalPattern.isEmpty())
            return input;

        Pattern pattern = optionalPattern.get();
        String[] parts = pattern.split(input.trim());

        StringBuilder result = new StringBuilder(input.length());

        for (String part : parts) {
            if (!part.isBlank()) {
                result.append(capitalizeFirstLetter(part));
            }
        }

        return result.toString();
    }

    /**
     * Converts a string to camelCase (e.g., "Hello_World" to "helloWorld").
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 입력 문자열을 camelCase 형식으로 변환합니다. (첫 단어는 소문자, 이후 단어의 첫 글자는 대문자로)
     *
     * @param input The original string | 원본 문자열
     * @return camelCase string | camelCase 문자열
     */
    public static String toCamelCase(String input) {
        if (input == null || input.isBlank())
            return "";
        String pascalCase = toPascalCase(input);

        if (pascalCase.isBlank())
            return "";
        if (pascalCase.length() == 1)
            return pascalCase.toLowerCase();

        return Character.toLowerCase(pascalCase.charAt(0)) + pascalCase.substring(1);
    }

    /**
     * Converts a string to snake_case (e.g., "helloWorld" to "hello_world").
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 입력 문자열을 snake_case 형식으로 변환합니다. (모두 소문자, 단어 구분은 언더스코어 '_')
     *
     * @param input The original string | 원본 문자열
     * @return snake_case string | snake_case 문자열
     */
    public static String toSnakeCase(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        var optionalPattern = S2Cache.getPattern(SPLIT_REGEX);
        if (optionalPattern.isEmpty())
            return input;

        String[] parts = optionalPattern.get().split(input.trim());
        StringBuilder result = new StringBuilder(input.length() + parts.length);

        boolean first = true;
        for (String part : parts) {
            if (!part.isBlank()) {
                if (!first) {
                    result.append("_");
                }
                result.append(part.toLowerCase());
                first = false;
            }
        }

        return result.toString();
    }

    /**
     * Converts a string to kebab-case (e.g., "helloWorld" to "hello-world").
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 입력 문자열을 kebab-case 형식으로 변환합니다. (모두 소문자, 단어 구분은 하이픈 '-')
     *
     * @param input The original string | 원본 문자열
     * @return kebab-case string | kebab-case 문자열
     */
    public static String toKebabCase(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        var optionalPattern = S2Cache.getPattern(SPLIT_REGEX);
        if (optionalPattern.isEmpty())
            return input;

        String[] parts = optionalPattern.get().split(input.trim());
        StringBuilder result = new StringBuilder(input.length() + parts.length);

        boolean first = true;
        for (String part : parts) {
            if (!part.isBlank()) {
                if (!first) {
                    result.append("-");
                }
                result.append(part.toLowerCase());
                first = false;
            }
        }

        return result.toString();
    }

    /**
     * Formats a message template by substituting indexed placeholders and automated Korean postpositions.
     * <p>
     * Supports both plain placeholders ({@code {0}}) and linguistic placeholders ({@code {0|이/가}}).
     * The latter automatically selects the grammatically correct postposition based on the
     * final character of the substituted value.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 메시지 템플릿의 인덱스({0}) 및 조사 패턴({0|은/는})을 분석하여 값을 치환합니다.
     * <p>
     * 단순 치환뿐만 아니라 한글의 종성 유무(받침)에 따른 조사 선택 로직을 포함하여
     * 자연스러운 문장 생성을 지원합니다.
     * </p>
     *
     * <pre>{@code
     * S2StringUtil.formatMessage("{0|이/가} {1|을/를} 방문함", "사과", "창고"); // "사과가 창고를 방문함"
     * }</pre>
     *
     * @param template The message template containing placeholders
     * @param args     Arguments for substitution
     * @return The formatted message string
     */
    public static String formatMessage(String template, Object... args) {
        if (template == null || args == null || args.length == 0) {
            return template;
        }

        // 정규식 설명: {숫자} 또는 {숫자|조사패턴} 추출
        // Group 1: 숫자(인덱스), Group 2: 파이프(|) 뒤의 문자열 전체
        var optionalPattern = S2Cache.getPattern("\\{(\\d+)(?:\\|([^}]+))?\\}");
        if (optionalPattern.isEmpty())
            return template;

        Matcher matcher = optionalPattern.get().matcher(template);

        // 초기 용량 최적화: 템플릿 길이 + (인자 개수 * 12)
        // 비트 연산 (args.length << 3) + (args.length << 2) 은 args.length * 12와 동일하며 미세하게 빠름
        StringBuilder sb = new StringBuilder(template.length() + (args.length << 3) + (args.length << 2));

        while (matcher.find()) {
            // 1. 인덱스 수동 파싱 (Integer.parseInt 보다 미세하게 빠름)
            String indexStr = matcher.group(1);
            int index = 0;
            for (int i = 0, len = indexStr.length(); i < len; i++) {
                index = index * 10 + (indexStr.charAt(i) - '0');
            }

            if (index >= args.length)
                continue;

            // 2. 값 확보 및 조사 패턴 확인
            Object arg = args[index];
            String value = arg == null ? "" : String.valueOf(arg);
            String josaPattern = matcher.group(2);

            // 3. 치환 문자열 결정 (조사 패턴이 있으면 appendJosa 호출)
            String replacement = josaPattern != null && !josaPattern.isBlank()
                    ? appendJosa(value, josaPattern)
                    : value;

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Automatically selects and attaches the appropriate Korean postposition (Josa) to a word.
     * <p>
     * Includes specialized logic for the "ㄹ" batchim exception (e.g., "서울로" instead of "서울으로").
     * Supports both standard and user-reversed patterns (e.g., both "은/는" and "는/은").
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 한글 조사를 단어의 마지막 글자 받침 유무에 따라 자동으로 선택하여 결합합니다.
     * <p>
     * 특히 '으로/로'의 경우 종성이 'ㄹ'인 특수 예외 상황을 정확하게 인지하여 처리합니다.
     * </p>
     *
     * @param word The target word (e.g., "사과")
     * @param josa The postposition pair (e.g., "이/가", "을/를")
     * @return The combined string with its correct postposition
     */
    public static String appendJosa(String word, String josa) {
        if (word == null || word.isBlank() || josa == null || josa.isBlank()) {
            return word;
        }

        char lastChar = word.charAt(word.length() - 1);

        // 한글 또는 숫자인 경우에만 조사 처리 진행
        if (isKoreanOrNumber(lastChar)) {
            boolean wordHasBatchim = hasBatchim(lastChar);

            // "/" 구분자가 있는 경우 (가변 조사 처리)
            int slashIdx = josa.indexOf('/');
            if (slashIdx != -1) {
                String first = josa.substring(0, slashIdx);
                String second = josa.substring(slashIdx + 1);

                // 사용자가 순서를 바꿔 썼을 경우(예: 는/은)를 대비한 로직
                // 첫 번째 조사가 받침이 있을 때 사용하는 것인지 확인
                boolean firstIsForBatchim = hasBatchim(first.charAt(0));

                // 단어의 받침 유무와 조사의 용도가 일치하면 first, 아니면 second 선택
                String targetJosa = (wordHasBatchim == firstIsForBatchim) ? first : second;

                // [특수 예외] "으로/로" 처리: 'ㄹ' 받침인 경우 "로"를 선택해야 함
                if (wordHasBatchim && "으로/로".equals(josa)) {
                    // 한글 유니코드 상 'ㄹ' 종성 인덱스는 8임
                    if (lastChar >= 0xAC00 && (lastChar - 0xAC00) % 28 == 8) {
                        targetJosa = second; // "서울으로" -> "서울로"
                    }
                }

                return word + targetJosa;
            }

            // "/" 없는 경우: 고정 접사나 단일 조사로 판단하여 그대로 결합
            return word + josa;
        }

        // 판별 불가(영문/특수문자 등) 시 대괄호 강조 (기존 정책 유지)
        return "[" + word + "]";
    }

    /** 한글 또는 숫자 여부 확인 */
    private static boolean isKoreanOrNumber(char c) {
        return (c >= 0xAC00 && c <= 0xD7A3) || (c >= '0' && c <= '9');
    }

    /**
     * Checks if the last character of the string has a Korean final consonant (Batchim).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 문자열의 마지막 글자에 받침이 있는지 확인합니다.
     * <p>
     * 주로 조사(은/는, 이/가 등)를 결정하기 위해 단어의 종성 유무를 파악할 때 사용합니다.
     * </p>
     *
     * @param text The text to check
     * @return {@code true} if it has a batchim, {@code false} otherwise
     */
    public static boolean hasBatchim(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return hasBatchim(text.charAt(text.length() - 1));
    }

    /**
     * 지정된 문자에 한글 받침(종성)이 있는지 확인한다.
     * <p>
     * 현대 한글 유니코드(AC00~D7A3) 공식에 따라 문자의 종성 인덱스를 계산하여 판별한다.
     * 계산식: (문자 코드 - 0xAC00) % 28 > 0
     * </p>
     *
     * @param c 확인할 문자 (한글 한 글자)
     * @return 한글 받침이 있으면 true, 한글이 아니거나 받침이 없으면 false
     */
    public static boolean hasBatchim(char c) {
        // 1. 한글 판별
        if (c >= 0xAC00 && c <= 0xD7A3) {
            return (c - 0xAC00) % 28 > 0;
        }

        // 2. 숫자 판별 (받침이 있는 숫자: 0, 1, 3, 6, 7, 8)
        // 0(영), 1(일), 3(삼), 6(육), 7(칠), 8(팔)
        if (c >= '0' && c <= '9') {
            return switch (c) {
                case '0', '1', '3', '6', '7', '8' -> true;
                default -> false;
            };
        }

        // 그 외(영어, 특수문자 등)는 받침이 없다고 가정하는 것이 보통 안전함
        return false;
    }

    /**
     * 유니코드 문자열을 실제 문자열로 변환한다.
     *
     * @param unicode Unicode 문자열
     * @return 변환된 문자열
     */
    public static String decodeUnicode(String unicode) {
        if (unicode == null || unicode.isBlank()) {
            return unicode;
        }

        StringBuilder result = new StringBuilder();
        int length = unicode.length();

        for (int i = 0; i < length; i++) {
            // 현재 문자가 '\'이고 다음 문자를 확인할 수 있는 경우
            if (i < length - 1 && unicode.charAt(i) == '\\') {
                // 유니코드 이스케이프 시퀀스 체크
                if (i < length - 5 && unicode.charAt(i + 1) == 'u') {
                    try {
                        String hex = unicode.substring(i + 2, i + 6);
                        int codePoint = Integer.parseInt(hex, 16);
                        result.append((char) codePoint);
                        i += 5; // 다음 문자로 이동
                        continue;
                    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                        // 잘못된 유니코드 시퀀스인 경우 원래 문자 그대로 추가
                        result.append(unicode.charAt(i));
                    }
                } else {
                    // 다른 이스케이프 시퀀스 처리 (\n, \t 등)
                    switch (unicode.charAt(i + 1)) {
                        case 'n':
                            result.append('\n');
                            i++;
                            break;
                        case 't':
                            result.append('\t');
                            i++;
                            break;
                        case 'r':
                            result.append('\r');
                            i++;
                            break;
                        case 'b':
                            result.append('\b');
                            i++;
                            break;
                        case 'f':
                            result.append('\f');
                            i++;
                            break;
                        case '\\':
                            result.append('\\');
                            i++;
                            break;
                        default:
                            result.append(unicode.charAt(i));
                            break;
                    }
                    continue;
                }
            }
            result.append(unicode.charAt(i));
        }

        return result.toString();
    }

    /**
     * NewLine 제거
     *
     * @param value 문자열
     * @return NewLine 제거 문자열
     */
    public static String clearNewline(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        int len = value.length();
        // 이스케이프 문자(\)가 추가될 것을 고려하여 약간의 여유 공간(16) 확보
        StringBuilder sb = new StringBuilder(len + 16);
        boolean modified = false;

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                    modified = true; // 제거 (아무것도 하지 않음)
                    break;
                case '\'':
                    sb.append("\\'"); // 이스케이프 치환
                    modified = true;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }

        return modified ? sb.toString() : value;
    }

    /**
     * 문자열을 Byte 단위로 substring 한다.
     *
     * @param stringValue 문자열
     * @param beginBytes  시작 Byte
     * @param endBytes    최종 Byte
     * @return substring 된 문자열
     * @details
     *          <dl>
     *          <dd>한글의 경우 3Byte 단위로 자르지 않으면 문자열이 깨진다 → UTF-8 기준 한글은 3Byte, 알파벳이나 숫자 띄어쓰기는 1Byte 단위로 substring 한다.</dd>
     *          </dl>
     */
    public static String substringByBytes(String stringValue, int beginBytes, int endBytes) {
        if (stringValue == null || stringValue.isBlank() || endBytes < 1) {
            return "";
        }

        if (beginBytes < 0) {
            beginBytes = 0;
        }

        var len = stringValue.length();
        var beginIndex = -1;
        var endIndex = 0;
        var curBytes = 0;

        for (int i = 0; i < len; i++) {
            var c = stringValue.charAt(i);
            int charByteSize;

            // [성능 최적화] getBytes().length 대신 유니코드 범위로 바이트 수 계산
            // 0x00 ~ 0x7F (0 ~ 127) : 1 Byte (ASCII)
            // 0x80 ~ 0x7FF (128 ~ 2047) : 2 Byte
            // 0x800 ~ (2048 ~ ) : 3 Byte (한글 등 대부분의 다국어)
            if (c < 0x80) {
                charByteSize = 1;
            } else if (c < 0x800) {
                charByteSize = 2;
            } else {
                charByteSize = 3;
            }

            curBytes += charByteSize;

            // 시작 인덱스 설정 (목표 바이트에 도달하거나 넘어선 순간의 인덱스)
            if (beginIndex == -1 && curBytes >= beginBytes) {
                beginIndex = i;
            }

            // 종료 바이트를 초과하면 중단
            if (curBytes > endBytes) {
                break;
            } else {
                endIndex = i + 1;
            }
        }

        // 시작 위치를 찾지 못한 경우(문자열이 너무 짧은 경우 등)
        if (beginIndex == -1) {
            return "";
        }

        return stringValue.substring(beginIndex, endIndex);
    }

    /**
     * 입력 문자열에서 제어 문자 및 유효하지 않은 문자를 제거
     */
    public static String sanitizeInput(String input) {
        if (input == null)
            return "";
        // 제어 문자 (0x00-0x1F, 0x7F) 제거
        return S2StringUtil.replaceAll(input, "[\\p{Cntrl}]", "");
    }

    /**
     * 숫자 여부를 확인한다.(is Not a Number)
     *
     * @param number 문자열
     * @return 숫자 여부
     */
    public static boolean isNaN(String number) {
        boolean result = false;
        try {
            Double.parseDouble(number);
        } catch (NullPointerException | NumberFormatException e) {
            result = true;
        }
        return result;
    }

}
