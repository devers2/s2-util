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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.devers2.s2util.core.S2Util;

/**
 * Internal writer that serializes {@link S2Validator} rules into the JSON consumed by {@code s2.validator.js}.
 * <p>
 * Public entry points are {@link S2Validator#getRulesJson()} and {@link S2Validator#getRulesJson(Locale)}.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link S2Validator} 규칙을 {@code s2.validator.js}가 읽는 JSON 으로 직렬화하는 내부 클래스입니다.
 * <p>
 * 공개 진입점은 {@link S2Validator#getRulesJson()}과 {@link S2Validator#getRulesJson(Locale)}입니다.
 * </p>
 */
final class S2RulesJsonWriter {

    private S2RulesJsonWriter() {
        // Prevent instantiation
    }

    /**
     * Serializes the validator's rules to JSON.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증기의 규칙을 JSON 으로 직렬화합니다.
     *
     * @param validator The validator to export | 내보낼 검증기
     * @param locale    The locale for error messages | 오류 메시지 로케일
     * @return The rules JSON | 규칙 JSON
     */
    static String write(S2Validator<?> validator, Locale locale) {
        StringBuilder sb = new StringBuilder();
        appendRulesJson(sb, validator, locale, List.of());
        return sb.toString();
    }

    /**
     * Writes S2Validator rules directly to StringBuilder in JSON format (supports recursion).
     * <p>
     * Optimizes for performance and memory by avoiding intermediate Map/List creation.
     * This method recursively traverses nested validators and conditions to construct
     * a complete JSON representation.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2Validator의 규칙들을 StringBuilder에 JSON 형식으로 직접 작성합니다 (재귀 지원).
     * 중간 Map/List 생성을 생략하여 성능과 메모리 효율을 극대화합니다.
     *
     * @param sb        The StringBuilder to append JSON content to | JSON 컨텐츠를 추가할 StringBuilder
     * @param validator The validator instance to export | 내보낼 검증기 인스턴스
     * @param locale    The locale for error message resolution | 에러 메시지 해석용 로케일
     */
    private static void appendRulesJson(StringBuilder sb, S2Validator<?> validator, Locale locale,
            List<S2Validator<?>> outerValidators) {
        sb.append("[");
        boolean firstField = true;
        for (S2Field<?> field : validator.getFields()) {
            List<S2Rule> rules = field.getRules();

            // [서버-클라이언트 일관성] 규칙이 하나도 없으면 서버와 동일하게 REQUIRED 규칙을 기본으로 적용한다.
            if (rules.isEmpty() && field.getCustomRules().isEmpty()) {
                rules = Collections.singletonList(S2Rule.required());
            }

            if (!firstField)
                sb.append(",");
            firstField = false;

            sb.append("{");
            sb.append("\"name\":\"").append(escapeJsonString(String.valueOf(field.getName()))).append("\",");
            sb.append("\"label\":\"").append(escapeJsonString(field.getLabel())).append("\",");

            // Rules 작성
            sb.append("\"rules\":[");
            boolean firstRule = true;
            for (S2Rule rule : rules) {
                if (!firstRule)
                    sb.append(",");
                firstRule = false;

                S2RuleType ruleType = rule.getRuleType();
                if (ruleType == S2RuleType.REGEX) {
                    rejectClientIncompatibleRegex(field, String.valueOf(rule.getCheckValue()));
                }
                sb.append("{");
                sb.append("\"type\":\"").append(ruleType.name()).append("\",");
                sb.append("\"regex\":").append(toJsonString(ruleType.getRegex())).append(",");
                sb.append("\"message\":").append(toJsonString(field.getErrorMessage(rule, locale, outerValidators)));

                if (ruleType == S2RuleType.NESTED || ruleType == S2RuleType.EACH) {
                    if (rule.getCheckValue() instanceof S2Validator<?> sub) {
                        sb.append(",\"nestedRules\":");
                        List<S2Validator<?>> chain = new java.util.ArrayList<>(outerValidators.size() + 1);
                        chain.add(validator);
                        chain.addAll(outerValidators);
                        appendRulesJson(sb, sub, locale, chain);
                    }
                } else {
                    sb.append(",\"value\":").append(toJsonString(rule.getCheckValue()));
                }
                sb.append("}");
            }
            sb.append("]");

            // Conditions 작성
            if (!field.getConditionGroups().isEmpty()) {
                sb.append(",\"conditions\":[");
                boolean firstGroup = true;
                for (List<S2Condition> group : field.getConditionGroups()) {
                    if (!firstGroup)
                        sb.append(",");
                    firstGroup = false;

                    sb.append("[");
                    boolean firstCond = true;
                    for (S2Condition cond : group) {
                        if (!firstCond)
                            sb.append(",");
                        firstCond = false;
                        sb.append("{");
                        sb.append("\"field\":\"").append(escapeJsonString(String.valueOf(cond.fieldName()))).append("\",");
                        sb.append("\"value\":").append(toJsonString(cond.value()));
                        sb.append("}");
                    }
                    sb.append("]");
                }
                sb.append("]");
            }

            sb.append("}");
        }
        sb.append("]");
    }

    /**
     * Converts an object to a JSON string representation (removes Jackson dependency).
     * <p>
     * Supports Map, List, String, Number, Boolean, null, and Enum types.
     * For unsupported types, falls back to {@code toString()}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 객체를 JSON 문자열로 변환합니다 (Jackson 의존성 제거).
     * <p>
     * Map, List, String, Number, Boolean, null, Enum을 지원합니다.
     * </p>
     *
     * @param obj The object to convert | 변환할 객체
     * @return JSON string representation | JSON 문자열 표현
     */
    /**
     * Fails fast when a REGEX rule uses Java-only syntax that the browser cannot evaluate the same way.
     * <p>
     * Checked only when rules are exported to the client, so server-only validators may keep using Java syntax.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * REGEX 규칙이 브라우저에서 같은 의미로 평가될 수 없는 Java 전용 문법을 쓰면 즉시 실패시킵니다.
     * <p>
     * 규칙을 클라이언트로 내보낼 때만 검사하므로, 서버 전용 검증기는 Java 문법을 계속 쓸 수 있습니다.
     * </p>
     *
     * @param field   The field that owns the rule | 규칙을 가진 필드
     * @param pattern The regex pattern | 정규식 패턴
     * @throws IllegalStateException If the pattern is not client compatible | 클라이언트 호환이 아닌 경우
     */
    private static void rejectClientIncompatibleRegex(S2Field<?> field, String pattern) {
        String problem = S2RegexCompatibility.findIncompatibility(pattern);
        if (problem == null) {
            return;
        }
        String fieldName = String.valueOf(field.getName());
        if (S2Util.isKorean()) {
            throw new IllegalStateException("필드 '" + fieldName + "'의 REGEX 규칙 \"" + pattern
                    + "\"은(는) 브라우저(JavaScript)에서 같은 의미로 평가할 수 없는 Java 전용 문법을 사용합니다: " + problem
                    + ". 클라이언트 검증에 쓰려면 ECMAScript 호환 문법으로 바꾸십시오(예: (?i) 대신 [Aa] 문자 클래스).");
        }
        throw new IllegalStateException("REGEX rule \"" + pattern + "\" on field '" + fieldName
                + "' uses Java-only syntax that the browser (JavaScript) cannot evaluate the same way: " + problem
                + ". Use ECMAScript-compatible syntax for client-side validation (e.g. [Aa] instead of (?i)).");
    }

    private static String toJsonString(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof String) {
            return "\"" + escapeJsonString((String) obj) + "\"";
        }

        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }

        if (obj instanceof Enum<?>) {
            return "\"" + escapeJsonString(obj.toString()) + "\"";
        }

        if (obj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(toJsonString(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }

        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first)
                    sb.append(",");
                first = false;
                sb.append("\"").append(escapeJsonString(entry.getKey().toString())).append("\":");
                sb.append(toJsonString(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }

        // Fallback: toString() 사용
        return "\"" + escapeJsonString(obj.toString()) + "\"";
    }

    /**
     * Escapes special JSON characters in a string.
     * <p>
     * Handles quotes, backslashes, control characters (\b, \f, \n, \r, \t),
     * and Unicode escaping for characters below 0x20.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * JSON 문자열 이스케이핑 처리를 수행합니다.
     *
     * @param str The string to escape | 이스케이핑할 문자열
     * @return Escaped string | 이스케이핑된 문자열
     */
    private static String escapeJsonString(String str) {
        if (str == null)
            return "";

        StringBuilder sb = new StringBuilder(str.length() + 16);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
