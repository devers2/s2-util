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
 * Korean language processing (e.g., automatic postposition completion such as {0|이}, {0|을}, {0|은}, {0|로})
 * and case conversions.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 라이브러리의 고성능 문자열 처리 유틸리티 클래스입니다.
 * <p>
 * 단순한 문자열 조작을 넘어, 메모리 효율성과 실행 속도를 최우선으로 설계되었습니다.
 * 한국어 특유의 조사 자동 완성('이', '을', '은', '로' 등 조사 중 하나만 넣어도 앞 단어에 맞추어 이/가, 을/를 등 자동 선택)
 * 로직과 다양한 케이스 변환(Camel, Snake 등) 기능을 제공합니다.
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
        if (source == null || source.isBlank() || regex == null || regex.isBlank() || replacement == null) {
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
     * Formats a message template by substituting indexed placeholders and automatically completing Korean postpositions.
     * <p>
     * Supports plain placeholders ({@code {0}}) and automated postposition modifiers (e.g., {@code {0|이}}, {@code {0|을}},
     * {@code {0|은}}, {@code {0|로}}). Specifying any representative postposition (e.g., {@code {0|이}}) automatically
     * analyzes the preceding word's final consonant (batchim) and selects the grammatically correct particle
     * from the corresponding group (e.g., choosing between "이" and "가").
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 메시지 템플릿의 인덱스({0}) 및 조사 자동 완성 구문({0|이}, {0|을}, {0|은}, {0|로} 등)을 분석하여 값을 치환합니다.
     * <p>
     * 관련된 조사군 중 하나의 조사만 지정하더라도 앞 단어의 받침 유무를 분석하여 알맞은 조사를 자동으로 완성합니다.
     * <ul>
     * <li>예: <code>{0|이}</code>만 붙여도 앞 단어가 "사과"이면 "사과가", "책"이면 "책이"처럼 "이/가" 중에서 알맞은 조사가 자동으로 완성됩니다.</li>
     * <li>예: <code>{0|을}</code>만 붙여도 앞 단어가 "총괄"이면 "총괄을", "책임자"이면 "책임자를"처럼 "을/를" 중에서 알맞은 조사가 자동으로 완성됩니다.</li>
     * <li>예: <code>{0|로}</code>만 붙여도 앞 단어가 "총괄"이면 "총괄로"('ㄹ' 받침 예외 자동 처리), "집"이면 "집으로"처럼 "으로/로" 중에서 알맞은 조사가 자동으로 완성됩니다.</li>
     * </ul>
     * 하나의 조사만 적으면 되므로 "이/가", "을/를"처럼 쌍으로 표기할 때 발생할 수 있는 오타(예: "이/로", "을/가") 걱정 없이 안전하고 간결하게 사용할 수 있습니다.
     * </p>
     *
     * <pre>{@code
     * S2StringUtil.formatMessage("{0|이} {1|을} 방문함", "사과", "창고"); // "사과가 창고를 방문함"
     * S2StringUtil.formatMessage("{0|로} 배정함", "총괄");            // "총괄로 배정함" ('ㄹ' 받침 예외 자동 처리)
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
     * Automatically completes and attaches the appropriate Korean postposition (Josa) to a word.
     * <p>
     * By providing any single postposition from a pair (e.g., "이", "가", "을", "를", "은", "는", "로", "으로"),
     * this method identifies the corresponding postposition group and automatically selects the grammatically
     * correct postposition based on whether the preceding word ends with a final consonant (batchim).
     * For example, passing "이" will result in "사과가" for "사과" and "책이" for "책".
     * Includes specialized logic for the "ㄹ" batchim exception (e.g., "서울로", "총괄로" instead of "서울으로").
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 단어 뒤에 붙을 조사를 마지막 글자의 받침 유무에 따라 자동으로 완성하여 결합합니다.
     * <p>
     * 관련된 조사 중 하나의 조사(예: "이", "가", "을", "를", "은", "는", "로", "으로" 등)만 넘겨주면,
     * 앞 단어의 받침 유무를 분석하여 해당 조사군 중 알맞은 조사를 자동으로 붙여줍니다.
     * <ul>
     * <li>예: "이"만 넣어도 "사과" 뒤에는 "사과가", "책" 뒤에는 "책이"로 "이/가" 중 알맞은 조사가 완성됩니다.</li>
     * <li>예: "을"만 넣어도 "총괄" 뒤에는 "총괄을", "책임자" 뒤에는 "책임자를"로 "을/를" 중 알맞은 조사가 완성됩니다.</li>
     * <li>예: "으로" 또는 "로"만 넣어도 "총괄" 뒤에는 "총괄로"('ㄹ' 종성 예외), "집" 뒤에는 "집으로"로 알맞게 완성됩니다.</li>
     * </ul>
     * 조사 쌍("이/가", "을/를" 등)을 일일이 적으며 발생하던 오타(예: "이/로", "을/가") 걱정 없이 간결하게 하나의 조사만 지정하여 완성할 수 있습니다.
     * </p>
     *
     * @param word The target word (e.g., "사과", "총괄")
     * @param josa The postposition to apply (e.g., "이", "을", "은", "로")
     * @return The combined string with its correct postposition
     */

    /** Josa rule: batchim form, no-batchim form, ㄹ-special flag, dual display form. */
    private record JosaRule(String batchim, String noBatchim, boolean rieulSpecial, String dualForm) {
    }

    /** Postposition rule lookup map. Maps each particle to its rule. */
    private static final java.util.Map<String, JosaRule> JOSA_RULES;
    static {
        var rules = new java.util.HashMap<String, JosaRule>();
        JosaRule r;

        r = new JosaRule("을", "를", false, "을(를)");
        rules.put("을", r);
        rules.put("를", r);

        r = new JosaRule("이", "가", false, "이(가)");
        rules.put("이", r);
        rules.put("가", r);

        r = new JosaRule("은", "는", false, "은(는)");
        rules.put("은", r);
        rules.put("는", r);

        r = new JosaRule("과", "와", false, "과(와)");
        rules.put("과", r);
        rules.put("와", r);

        r = new JosaRule("으로", "로", true, "(으)로");
        rules.put("으로", r);
        rules.put("로", r);

        r = new JosaRule("이나", "나", false, "(이)나");
        rules.put("이나", r);
        rules.put("나", r);

        r = new JosaRule("이란", "란", false, "(이)란");
        rules.put("이란", r);
        rules.put("란", r);

        r = new JosaRule("아", "야", false, "아(야)");
        rules.put("아", r);
        rules.put("야", r);

        r = new JosaRule("이어", "여", false, "이어(여)");
        rules.put("이어", r);
        rules.put("여", r);

        r = new JosaRule("이에", "예", false, "이에(예)");
        rules.put("이에", r);
        rules.put("예", r);

        r = new JosaRule("이며", "며", false, "(이)며");
        rules.put("이며", r);
        rules.put("며", r);

        r = new JosaRule("이든", "든", false, "(이)든");
        rules.put("이든", r);
        rules.put("든", r);

        r = new JosaRule("이라", "라", false, "(이)라");
        rules.put("이라", r);
        rules.put("라", r);

        JOSA_RULES = java.util.Map.copyOf(rules);
    }

    /**
     * Holds the resolved target character for postposition selection, along with classification flags.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조사 선택을 위해 분석된 대상 문자와 분류 플래그를 담는 내부 결과 객체입니다.
     */
    private static final class TargetCharResult {
        final char ch;
        final boolean isKorean;
        final boolean isNumber;
        final String trimmedWord;

        TargetCharResult(char ch, boolean isKorean, boolean isNumber, String trimmedWord) {
            this.ch = ch;
            this.isKorean = isKorean;
            this.isNumber = isNumber;
            this.trimmedWord = trimmedWord;
        }
    }

    /**
     * Resolves the target character used for postposition (Josa) selection.
     * <ol>
     *   <li>Leading/trailing whitespace is trimmed.</li>
     *   <li><b>Quotation marks</b> ({@code " "}, {@code ' '}, 「」, 『』, 〈〉, {@code <>}, 《》):
     *     <ul>
     *       <li>Quotation marks wrap titles, proper nouns, or emphasized words. The entire content functions
     *           as the principal noun phrase, so the <b>last character inside the quote</b> is used for
     *           batchim detection.
     *           (e.g. {@code "수박"을}, {@code '사과'를}, {@code 「처리방침」을}, {@code 『가이드』를})</li>
     *     </ul>
     *   </li>
     *   <li><b>Parentheses/brackets</b> ({@code ()}, {@code []}, <code>{}</code>, 【】, 〔〕, and full-width equivalents):
     *     <ul>
     *       <li>Per the National Institute of Korean Language (국립국어원) norms, parenthesized content is
     *           supplementary. The <b>last character before the opening bracket</b> is used first.
     *           (Bracket-removal test: "수임인(이승수)을" → "수임인을" [OK]; "수임인(이승수)를" → "수임인를" [ungrammatical])</li>
     *       <li>Fallback: if the string starts with a bracket (no word before it), the last character
     *           inside is used.
     *           (e.g. {@code "[공지사항]이"}, {@code "[안내]가"})</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise the last character of the trimmed word is used.</li>
     * </ol>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 조사 결합을 위해 기준이 되는 마지막 음절/문자를 추출합니다.
     * <ol>
     *   <li>앞뒤 공백을 trim 처리합니다.</li>
     *   <li><b>닫는 인용부호</b>로 끝나는 경우:
     *     <ul>
     *       <li>대상: 큰따옴표({@code “ ”}), 작은따옴표({@code ‘ ’}), 홈낫표(「」), 겹낫표(『』),
     *               홈화살괄호(〈〉, {@code <>}), 겹화살괄호(《》)</li>
     *       <li>인용부호 내부의 내용 전체가 하나의 주된 체언으로 기능하므로 <b>'부호 내부의 마지막 글자'</b> 기준으로 조사를 결정합니다.</li>
     *       <li>예: {@code “수박”을}, {@code ‘사과’를}, {@code 「개인정보 처리방침」을}, {@code 『연구 가이드』를}</li>
     *     </ul>
     *   </li>
     *   <li><b>닫는 괄호</b>로 끝나는 경우:
     *     <ul>
     *       <li>대상: 소괄호, 대괄호, 중괄호, 렌티큐러 괄호(【】), 귀괄호(〔〕)</li>
     *       <li>국립국어원 문법 규범에 따라 <b>'괄호 앞의 본말(주요 명사)'</b> 기준으로 조사를 결정합니다.
     *           (괄호 생략 검증: "수임인(이승수)을" → "수임인을"[O] vs "수임인(이승수)를" → "수임인를"[X 비문])</li>
     *       <li>예외: 괄호 앞 단어가 없는 경우 (예: "[공지사항]", "(안내)") 안쪽 글자 기준</li>
     *     </ul>
     *   </li>
     *   <li>일반 단어인 경우 마지막 글자 기준</li>
     * </ol>
     */
    private static TargetCharResult resolveTargetChar(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        char lastCh = trimmed.charAt(trimmed.length() - 1);

        boolean isQuote = false;
        int openIdx = -1;

        switch (lastCh) {
            // === Quotation marks (인용부호) ===
            case '"':
            case '\u201D': // ”
                isQuote = true;
                openIdx = Math.max(trimmed.lastIndexOf('"', trimmed.length() - 2), trimmed.lastIndexOf('\u201C'));
                break;
            case '\'':
            case '\u2019': // ’
                isQuote = true;
                openIdx = Math.max(trimmed.lastIndexOf('\'', trimmed.length() - 2), trimmed.lastIndexOf('\u2018'));
                break;
            case '\u300D': // 」
                isQuote = true;
                openIdx = trimmed.lastIndexOf('\u300C'); // 「
                break;
            case '\u300F': // 』
                isQuote = true;
                openIdx = trimmed.lastIndexOf('\u300E'); // 『
                break;
            case '\u3009': // 〉
                isQuote = true;
                openIdx = trimmed.lastIndexOf('\u3008'); // 〈
                break;
            case '>':
                isQuote = true;
                openIdx = trimmed.lastIndexOf('<');
                break;
            case '\u300B': // 》
                isQuote = true;
                openIdx = trimmed.lastIndexOf('\u300A'); // 《
                break;

            // === Parentheses / Brackets (괄호부호) ===
            case ')':
            case '\uFF09': // ）
                openIdx = Math.max(trimmed.lastIndexOf('('), trimmed.lastIndexOf('\uFF08'));
                break;
            case ']':
            case '\uFF3D': // ］
                openIdx = Math.max(trimmed.lastIndexOf('['), trimmed.lastIndexOf('\uFF3B'));
                break;
            case '}':
            case '\uFF5D': // ｝
                openIdx = Math.max(trimmed.lastIndexOf('{'), trimmed.lastIndexOf('\uFF5B'));
                break;
            case '\u3011': // 】
                openIdx = trimmed.lastIndexOf('\u3010'); // 【
                break;
            case '\u3015': // 〕
                openIdx = trimmed.lastIndexOf('\u3014'); // 〔
                break;
            default:
                break;
        }

        if (openIdx != -1 && openIdx < trimmed.length() - 1) {
            String inside = trimmed.substring(openIdx + 1, trimmed.length() - 1).trim();
            String before = trimmed.substring(0, openIdx).trim();

            if (isQuote) {
                // Quotation: inside-first | 인용부호: 부호 안쪽 우선
                if (!inside.isBlank()) {
                    char insideLastCh = inside.charAt(inside.length() - 1);
                    return new TargetCharResult(insideLastCh, isKorean(insideLastCh), isNumber(insideLastCh), trimmed);
                }
                if (!before.isBlank()) {
                    char beforeLastCh = before.charAt(before.length() - 1);
                    return new TargetCharResult(beforeLastCh, isKorean(beforeLastCh), isNumber(beforeLastCh), trimmed);
                }
            } else {
                // Brackets: before-first, inside-fallback | 괄호: 앞말 우선, 없을 때만 안쪽
                if (!before.isBlank()) {
                    char beforeLastCh = before.charAt(before.length() - 1);
                    return new TargetCharResult(beforeLastCh, isKorean(beforeLastCh), isNumber(beforeLastCh), trimmed);
                } else if (!inside.isBlank()) {
                    char insideLastCh = inside.charAt(inside.length() - 1);
                    return new TargetCharResult(insideLastCh, isKorean(insideLastCh), isNumber(insideLastCh), trimmed);
                }
            }
        }

        // Plain word | 일반 단어
        char ch = trimmed.charAt(trimmed.length() - 1);
        return new TargetCharResult(ch, isKorean(ch), isNumber(ch), trimmed);
    }

    /**
     * Checks if the character is a Korean syllable (U+AC00–U+D7A3). | 한글 완성형 음절(AC00~D7A3) 여부를 확인합니다.
     */
    private static boolean isKorean(char c) {
        return c >= 0xAC00 && c <= 0xD7A3;
    }

    /**
     * Checks if the character is an ASCII digit ({@code '0'–'9'}). | ASCII 숫자('0'~'9') 여부를 확인합니다.
     */
    private static boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Automatically completes and attaches the appropriate Korean postposition (Josa) to a word.
     * <p>
     * By providing any single postposition from a pair (e.g., "이", "가", "을", "를", "은", "는", "로", "으로"),
     * this method identifies the corresponding postposition group and automatically selects the grammatically
     * correct postposition based on whether the preceding word ends with a final consonant (batchim).
     * For example, passing "이" will result in "사과가" for "사과" and "책이" for "책".
     * Includes specialized logic for the "ㄹ" batchim exception (e.g., "서울로", "총괄로" instead of "서울으로").
     * </p>
     * <p>
     * Leading/trailing whitespace is trimmed from the input. If the word ends with parentheses or brackets,
     * the character inside (or before) the brackets is used for batchim detection via {@code resolveTargetChar}.
     * When the target character cannot be determined (e.g. Latin letters, symbols), the dual form is appended
     * without surrounding brackets (e.g. {@code "ID은(는)"}).
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 단어 뒤에 붙을 조사를 마지막 글자의 받침 유무에 따라 자동으로 완성하여 결합합니다.
     * <p>
     * 관련된 조사 중 하나의 조사(예: "이", "가", "을", "를", "은", "는", "로", "으로" 등)만 넘겨주면,
     * 앞 단어의 받침 유무를 분석하여 해당 조사군 중 알맞은 조사를 자동으로 붙여줍니다.
     * <ul>
     * <li>예: "이"만 넣어도 "사과" 뒤에는 "사과가", "책" 뒤에는 "책이"로 "이/가" 중 알맞은 조사가 완성됩니다.</li>
     * <li>예: "을"만 넣어도 "총괄" 뒤에는 "총괄을", "책임자" 뒤에는 "책임자를"로 "을/를" 중 알맞은 조사가 완성됩니다.</li>
     * <li>예: "으로" 또는 "로"만 넣어도 "총괄" 뒤에는 "총괄로"('ㄹ' 종성 예외), "집" 뒤에는 "집으로"로 알맞게 완성됩니다.</li>
     * </ul>
     * 입력 앞뒤의 공백은 trim 처리됩니다. 괄호/대괄호로 끝나는 경우 {@code resolveTargetChar}를 통해
     * 괄호 안 또는 앞의 문자를 기준으로 받침을 판별합니다.
     * 판별이 불가능한 경우(영문, 특수문자 등) 대괄호 없이 병기형을 결합합니다 (예: {@code "ID은(는)"}).
     * </p>
     *
     * @param word The target word (e.g., "사과", "총괄") | 대상 단어 (예: "사과", "총괄")
     * @param josa The postposition to apply (e.g., "이", "을", "은", "로") | 적용할 조사 (예: "이", "을", "은", "로")
     * @return The combined string with its correct postposition | 알맞은 조사가 결합된 문자열
     */
    public static String appendJosa(String word, String josa) {
        if (word == null || word.isBlank() || josa == null || josa.isBlank()) {
            return word;
        }

        TargetCharResult target = resolveTargetChar(word);
        String trimmed = target != null ? target.trimmedWord : word.trim();
        JosaRule rule = JOSA_RULES.get(josa);

        if (target != null && (target.isKorean || target.isNumber)) {
            boolean wordHasBatchim = hasBatchim(target.ch);

            if (rule != null) {
                // Identify the particle group and select the correct form based on batchim | 조사군을 식별하고, 받침 유무에 따라 적합한 조사로 자동 완성
                String targetJosa = wordHasBatchim ? rule.batchim : rule.noBatchim;

                // [Special exception] 'ㄹ' batchim → "로" for 으로/로 group (Korean ㄹ, digits 1, 7, 8)
                // [특수 예외] '으로/로' 계열: 종성이 'ㄹ'인 경우(한글 및 숫자 1, 7, 8) "로" 선택
                if (rule.rieulSpecial && wordHasBatchim && isRieulBatchim(target.ch)) {
                    targetJosa = rule.noBatchim; // "로"
                }

                return trimmed + targetJosa;
            }

            // Fixed particle not in the lookup (e.g. "의", "에", "도", "만"): append as is | 목록에 없는 고정 조사: 그대로 결합
            return trimmed + josa;
        }

        // Undeterminable (Latin letters, symbols): append the dual form (without brackets)
        String dualForm = rule != null ? rule.dualForm : josa;
        return trimmed + dualForm;
    }

    /**
     * Checks if the character has a 'ㄹ' (rieul) final consonant (Korean syllables and digits 1, 7, 8).
     * | 문자의 종성이 'ㄹ' 받침인지 확인합니다. (한글 및 숫자 1, 7, 8)
     */
    private static boolean isRieulBatchim(char c) {
        if (c >= 0xAC00 && c <= 0xD7A3) {
            return (c - 0xAC00) % 28 == 8;
        }
        return c == '1' || c == '7' || c == '8';
    }

    /**
     * Checks if the last character of the string has a Korean final consonant (Batchim).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 문자열의 마지막 글자에 받침이 있는지 확인합니다.
     * <p>
     * 주로 조사 자동 완성('은', '을', '이', '로' 등) 시 단어의 종성 유무를 파악하기 위해 사용합니다.
     * </p>
     *
     * @param text The text to check
     * @return {@code true} if it has a batchim, {@code false} otherwise
     */
    public static boolean hasBatchim(String text) {
        TargetCharResult target = resolveTargetChar(text);
        if (target == null || (!target.isKorean && !target.isNumber)) {
            return false;
        }
        return hasBatchim(target.ch);
    }

    /**
     * Checks if the given character has a Korean final consonant (Batchim).
     * <p>
     * For modern Korean syllables (U+AC00–U+D7A3), the final consonant index is computed as
     * {@code (charCode - 0xAC00) % 28 > 0}. For digits, those whose Korean reading ends with
     * a batchim (0, 1, 3, 6, 7, 8) return {@code true}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 문자에 한글 받침(종성)이 있는지 확인합니다.
     * <p>
     * 현대 한글 유니코드(AC00~D7A3) 공식에 따라 문자의 종성 인덱스를 계산하여 판별합니다.
     * 계산식: {@code (문자 코드 - 0xAC00) % 28 > 0}
     * </p>
     *
     * @param c The character to check | 확인할 문자 (한글 한 글자)
     * @return {@code true} if it has a batchim, {@code false} otherwise | 한글 받침이 있으면 true, 아니면 false
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
