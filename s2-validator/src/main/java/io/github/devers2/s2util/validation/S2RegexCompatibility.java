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
package io.github.devers2.s2util.validation;

/**
 * Detects Java regex syntax that the browser (ECMAScript {@code RegExp} without flags) cannot evaluate the same way.
 * <p>
 * Rules exported to the client via {@link S2Validator#getRulesJson(java.util.Locale)} are evaluated
 * by {@code new RegExp(pattern)} in {@code s2.validator.js}. Java-only syntax either throws a {@code SyntaxError}
 * there (the field can never pass, so the form cannot be submitted) or silently means something else. This scanner
 * is a conservative heuristic for the common cases; it does not fully parse regular expressions.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 브라우저(플래그 없는 ECMAScript {@code RegExp})가 같은 의미로 평가할 수 없는 Java 정규식 문법을 찾아냅니다.
 * <p>
 * 클라이언트로 내보낸 규칙은 {@code s2.validator.js}에서 {@code new RegExp(pattern)}으로 평가됩니다. Java 전용 문법은 그곳에서
 * {@code SyntaxError}를 내거나(해당 필드가 절대 통과하지 못해 폼을 제출할 수 없음) 조용히 다른 뜻이 됩니다. 이 검사기는 흔한 경우를
 * 잡는 보수적 휴리스틱이며 정규식을 완전히 해석하지는 않습니다.
 * </p>
 */
final class S2RegexCompatibility {

    /** Escapes that are Java-only or mean a literal letter in a flagless JS RegExp | Java 전용이거나 플래그 없는 JS RegExp 에서 문자 그대로가 되는 이스케이프 */
    private static final String JAVA_ONLY_ESCAPES = "AZzGQEhHRXpPea";

    private S2RegexCompatibility() {
    }

    /**
     * Returns a description of the first Java-only construct found, or {@code null} if none is found.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 처음 발견한 Java 전용 문법의 설명을 반환하고, 없으면 {@code null}을 반환합니다.
     *
     * @param pattern The Java regex pattern | Java 정규식 패턴
     * @return The incompatibility description, or null | 비호환 설명 (없으면 null)
     */
    static String findIncompatibility(String pattern) {
        if (pattern == null) {
            return null;
        }
        boolean inClass = false;
        int length = pattern.length();
        for (int i = 0; i < length; i++) {
            char c = pattern.charAt(i);
            char next = i + 1 < length ? pattern.charAt(i + 1) : '\0';

            if (c == '\\') {
                if (JAVA_ONLY_ESCAPES.indexOf(next) >= 0) {
                    return "Java-only escape '\\" + next + "'";
                }
                i++; // skip the escaped character | 이스케이프된 문자 건너뜀
                continue;
            }

            if (inClass) {
                if (c == '&' && next == '&') {
                    return "character class intersection '&&'";
                }
                if (c == '[') {
                    return "nested character class (union) '[...[...]]'";
                }
                if (c == ']') {
                    inClass = false;
                }
                continue;
            }

            if (c == '[') {
                inClass = true;
                // A ']' right after '[' or '[^' is a literal in Java | '[' 또는 '[^' 바로 뒤의 ']' 는 Java 에서 문자 그대로
                if (next == '^') {
                    i++;
                }
                if (i + 1 < length && pattern.charAt(i + 1) == ']') {
                    i++;
                }
                continue;
            }

            if (c == '(' && next == '?') {
                char spec = i + 2 < length ? pattern.charAt(i + 2) : '\0';
                if (spec == '>') {
                    return "atomic group '(?>...)'";
                }
                if (Character.isLetter(spec) || spec == '-') {
                    return "inline flag group '(?" + spec + "...)'";
                }
                i++; // consume '?' so it is not read as a quantifier | '?' 를 수량자로 읽지 않도록 소비
                continue;
            }

            if ((c == '*' || c == '+' || c == '?' || c == '}') && next == '+') {
                return "possessive quantifier '" + c + "+'";
            }
        }
        return null;
    }
}
