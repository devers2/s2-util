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
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * Exports {@link S2Validator} rules as JSON for the browser validator ({@code s2.validator.js}).
 * <p>
 * The same validator instance is used on the server ({@code validate}) and exported to the client
 * ({@link #getRulesJson(S2Validator, Locale)}), so both sides apply one rule definition. Pass the instance directly,
 * e.g. {@code S2BindValidator.of(validator)}; there is no global registry.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link S2Validator} 규칙을 브라우저 검증기({@code s2.validator.js})용 JSON 으로 내보냅니다.
 * <p>
 * 같은 검증기 인스턴스를 서버 검증({@code validate})과 클라이언트 내보내기({@link #getRulesJson(S2Validator, Locale)})에 함께 쓰므로
 * 양쪽이 하나의 규칙 정의를 적용합니다. 인스턴스를 직접 전달하십시오(예: {@code S2BindValidator.of(validator)}). 전역 등록부는 없습니다.
 * </p>
 *
 * @author devers2
 * @version 1.6
 * @since 1.0
 */
public final class S2ValidatorFactory {

    private static final S2Logger logger = S2LogManager.getLogger(S2ValidatorFactory.class);

    private S2ValidatorFactory() {
        // Prevent instantiation
    }

    /**
     * Generates a structural JSON representation of validation rules for client-side use.
     * <p>
     * The resulting JSON is designed to be consumed by the {@code s2.validator.js} library.
     * It includes field names, labels, rule types, regex patterns, and localized messages.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 클라이언트(JavaScript) 측에서 사용할 수 있는 검증 규칙의 구조적 JSON 표현을 생성합니다.
     * <p>
     * 생성된 JSON은 {@code s2.validator.js} 라이브러리에서 해석되어 브라우저 측 실시간 검증에 사용됩니다.
     * 필드명, 라벨, 규칙 타입, 정규식 패턴 및 로케일별 에러 메시지를 모두 포함합니다.
     * </p>
     *
     * @param validator The validator to export | 내보낼 검증기 인스턴스
     * @param locale    The locale for error message generation | 에러 메시지 생성을 위한 로케일
     * @return A JSON string representing the validation rules | 검증 규칙을 나타내는 JSON 문자열
     * @apiNote
     *          <p>
     *          <b>■ 사용 사례 1: Thymeleaf 데이터 속성에 설정 (추천)</b>
     *          </p>
     *
     *          <pre>{@code
     * // Controller (Java)
     * model.addAttribute("validationRules", validator.getRulesJson());
     *
     * // View (HTML/Thymeleaf)
     * &lt;form id="saveForm" th:data-s2-rules="${validationRules}"&gt;
     *     &lt;input type="text" name="userId" /&gt;
     *     &lt;button type="button" onclick="doSave()"&gt;저장&lt;/button&gt;
     * &lt;/form&gt;
     *
     * // Script (JS)
     * function doSave() {
     *     const errors = S2Validator.validate('#saveForm');
     * }
     * }</pre>
     *
     *          <p>
     *          <b>■ 사용 사례 2: JavaScript 변수에 직접 할당</b>
     *          </p>
     *
     *          <pre>{@code
     * const myRules = '[[${validationRules}]]';
     *
     * function doSave() {
     *     const errors = S2Validator.validate('#saveForm', myRules);
     * }
     * }</pre>
     */
    public static String getRulesJson(S2Validator<?> validator, Locale locale) {
        if (validator == null) {
            if (S2Util.isKorean()) {
                logger.warn("규칙을 내보낼 검증기가 null 입니다. 빈 규칙([])을 반환합니다.");
            } else {
                logger.warn("The validator to export is null; returning empty rules ([]).");
            }
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        appendRulesJson(sb, validator, locale);
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
    private static void appendRulesJson(StringBuilder sb, S2Validator<?> validator, Locale locale) {
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
                sb.append("\"message\":").append(toJsonString(field.getErrorMessage(rule, locale)));

                if (ruleType == S2RuleType.NESTED || ruleType == S2RuleType.EACH) {
                    if (rule.getCheckValue() instanceof S2Validator<?> sub) {
                        sb.append(",\"nestedRules\":");
                        appendRulesJson(sb, sub, locale);
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
