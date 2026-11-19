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
 * Whitespace handling shared by the server rules, using the JavaScript definition ({@code String.prototype.trim}) so
 * the server and {@code s2.validator.js} trim the same characters.
 * <p>
 * Java's {@link String#trim()} removes only characters up to U+0020 and keeps NBSP (U+00A0) and the ideographic space
 * (U+3000), while JavaScript removes them, which made the two sides judge padded values differently.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 서버 규칙이 쓰는 공백 처리입니다. 자바스크립트({@code String.prototype.trim})와 같은 정의를 써서 서버와 {@code s2.validator.js}가 같은
 * 문자를 공백으로 지웁니다.
 * <p>
 * Java {@link String#trim()}은 U+0020 이하만 지우고 NBSP(U+00A0)·전각 공백(U+3000)은 남기지만 자바스크립트는 지우므로, 공백이 섞인
 * 값을 양쪽이 다르게 판정했습니다.
 * </p>
 */
final class S2Whitespace {

    private S2Whitespace() {
    }

    /**
     * Returns whether the character is whitespace or a line terminator in JavaScript.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 자바스크립트 기준 공백 또는 줄 끝 문자인지 반환합니다.
     *
     * @param c The character | 문자
     * @return {@code true} if whitespace | 공백이면 true
     */
    static boolean isWhitespace(char c) {
        return switch (c) {
            case '\t', '\n', '\u000B', '\f', '\r', ' ', ' ', '﻿', ' ', ' ' -> true;
            default -> Character.getType(c) == Character.SPACE_SEPARATOR;
        };
    }

    /**
     * Removes leading and trailing whitespace like JavaScript {@code trim()}.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 자바스크립트 {@code trim()}처럼 앞뒤 공백을 지웁니다.
     *
     * @param text The text | 문자열
     * @return The trimmed text | 앞뒤 공백을 지운 문자열
     */
    static String trim(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && isWhitespace(text.charAt(start))) {
            start++;
        }
        while (end > start && isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(start, end);
    }

    /**
     * Returns whether the text is empty or only whitespace (JavaScript definition).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 빈 문자열이거나 공백(자바스크립트 기준)만 있는지 반환합니다.
     *
     * @param text The text | 문자열
     * @return {@code true} if blank | 비어 있으면 true
     */
    static boolean isBlank(String text) {
        return trim(text).isEmpty();
    }
}
