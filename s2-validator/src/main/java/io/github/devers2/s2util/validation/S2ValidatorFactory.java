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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;

/**
 * Central registry and factory for {@link S2Validator} instances.
 * <p>
 * This class provides a high-performance caching mechanism for validation blueprints.
 * It ensures that expensive-to-build validators are constructed only once (Lazy Initialization)
 * and reused efficiently across the entire application lifetime.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * {@link S2Validator} 인스턴스의 중앙 저장소 및 팩토리 클래스입니다.
 * <p>
 * 검증 로직 설계도(Blueprint)인 {@link S2Validator} 객체들을 효율적으로 관리하며, 복잡한 규칙 생성
 * 비용을 최소화하기 위해 지연 초기화(Lazy Initialization) 기반의 캐싱 메커니즘을 제공합니다.
 * 애플리케이션 수명 주기 동안 한 번 생성된 검증기를 멀티스레드 환경에서 안전하게 재사용할 수 있게 합니다.
 * </p>
 *
 * <h3>Key Capabilities (주요 역량)</h3>
 * <ul>
 * <li><b>Lazy Registration:</b> Use {@link #getOrRegister(String, Supplier)} to define
 * validators only when they are first requested.</li>
 * <li><b>Thread-Safe Caching:</b> Powered by {@link ConcurrentHashMap} for lock-free
 * read access in high-concurrency environments.</li>
 * <li><b>Client-Side Synchronization:</b> Generates JS-compatible JSON metadata to
 * synchronize validation rules between Server (Java) and Client (JavaScript).</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public final class S2ValidatorFactory {

    private static final S2Logger logger = S2LogManager.getLogger(S2ValidatorFactory.class);

    /** 검증 컨텍스트별 빌더 저장소 */
    private static final Map<String, S2Validator<?>> validatorCache = new ConcurrentHashMap<>();

    private S2ValidatorFactory() {
        // Prevent instantiation
    }

    /**
     * Retrieves a cached validator or registers a new one using a lazy supplier.
     * <p>
     * <b>Thread Safety:</b> This method is thread-safe. If multiple threads request the
     * same key simultaneously, the supplier will be executed only once.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시된 검증기를 조회하거나, 없는 경우 제공된 서플라이어(Supplier)를 통해 새 검증기를 등록합니다.
     * <p>
     * <b>스레드 안정성:</b> 멀티스레드 환경에서 안전하게 동작합니다. 동일한 키에 대해 여러 스레드가
     * 동시에 요청하더라도 검증기 생성 로직은 단 한 번만 실행됨을 보장합니다.
     * </p>
     *
     * @param <T>               The target type handled by the validator | 검증기가 처리하는 대상 타입
     * @param contextKey        Unique identifier for the validation context (e.g., "MEMBER_JOIN") | 검증 컨텍스트의 고유 식별자
     * @param validatorSupplier Lambda or method reference to build the validator | 검증기 생성을 위한 람다 또는 메서드 참조
     * @return The cached or newly created {@link S2Validator} instance | 캐시된 또는 새로 생성된 S2Validator 인스턴스
     * @implNote
     *
     *           <pre>{@code
     * // Example 1: Basic usage with inline lambda
     * S2Validator<UserDTO> validator = S2ValidatorFactory.getOrRegister(
     *     "USER_JOIN", () -> S2Validator.builder()
     *         .field("userId", "아이디").rule(S2RuleType.REQUIRED)
     *         .field("userPw", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
     *         .build()
     * );
     *
     * // Later, get the same validator instance from cache
     * UserDTO newUser = new UserDTO();
     * newUser.setUserId("admin");
     * validator.validate(newUser);  // Reuses cached validator
     *
     * // Example 2: Using method reference in controller
     *           @RestController
     *           public class MemberController {
     *           private S2Validator<MemberDTO> getMemberValidator() {
     *           return S2Validator.<MemberDTO>builder()
     *           .field("memberId", "회원ID").rule(S2RuleType.REQUIRED)
     *           .field("email", "이메일").rule(S2RuleType.EMAIL)
     *           .field("password", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
     *           .field("confirmPw", "비밀번호 확인")
     *           .rule(
     *           (value, target) -> S2Util.getValue(target, "password", "").equals(value)
     *           ).ko("비밀번호가 일치하지 않습니다.")
     *           .build();
     *           }
     *
     *           @PostMapping("/join")
     *           public String joinMember(MemberDTO dto, BindingResult result) {
     *           // First call: builds and caches validator
     *           S2Validator<MemberDTO> validator = S2ValidatorFactory.getOrRegister("MEMBER_JOIN", this::getMemberValidator);
     *
     *           validator.validate(
     *           dto, error -> result.rejectValue(
     *           error.fieldName(), error.errorCode(),
     *           error.errorArgs(), error.defaultMessage()
     *           )
     *           );
     *
     *           if (result.hasErrors()) {
     *           return "join/form";
     *           }
     *           // Process successful join...
     *           }
     *           }
     *
     *           // Example 3: Multi-threaded safety demonstration
     *           // Even if multiple threads call simultaneously, the supplier executes only once
     *           ExecutorService executor = Executors.newFixedThreadPool(10);
     *           for (int i = 0; i < 100; i++) {
     *           executor.submit(() -> {
     *           S2Validator<?> v = S2ValidatorFactory.getOrRegister(
     *           "SHARED_KEY",
     *           () -> S2Validator.builder().field("test").rule(S2RuleType.REQUIRED).build()
     *           );
     *           // All threads receive the exact same instance
     *           });
     *           }
     *
     *           // Example 4: Retrieve cached validator without supplier
     *           S2Validator<?> cached = S2ValidatorFactory.getValidator("USER_JOIN");
     *           if (cached != null) {
     *           // Use cached validator
     *           }
     * }</pre>
     */
    @SuppressWarnings("unchecked")
    public static <T> S2Validator<T> getOrRegister(String contextKey, Supplier<S2Validator<T>> validatorSupplier) {
        // computeIfAbsent를 사용하여 캐시에 없으면 supplier.get()(빌더 build)을 실행하여 저장
        return (S2Validator<T>) validatorCache.computeIfAbsent(contextKey, k -> validatorSupplier.get());
    }

    /**
     * Retrieves a validator instance from the cache.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 캐시에서 검증기 인스턴스를 조회합니다.
     *
     * @param key The context key registered in the cache | 캐시에 등록된 컨텍스트 키
     * @return The registered S2Validator instance (or null if not found) | 등록된 S2Validator 인스턴스 (없으면 null)
     */
    public static S2Validator<?> getValidator(String key) {
        return validatorCache.get(key);
    }

    /**
     * Returns a JSON string for sharing validation rules with the client (JavaScript).
     * <p>
     * The JSON returned by this method is structured to be interpretable by the
     * {@code s2.validator.js} library. It is typically used in HTML data attributes
     * or assigned directly to JavaScript variables.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 클라이언트(JavaScript)와 검증 규칙을 공유하기 위한 JSON 문자열을 반환합니다.
     * <p>
     * 이 메서드가 반환하는 JSON은 {@code s2.validator.js} 라이브러리에서 해석 가능한 구조이며,
     * 주로 HTML의 data 속성에 담거나 JavaScript 변수에 직접 할당하여 사용합니다.
     * </p>
     *
     * @param contextKey The registered validation context key | 등록된 검증 규칙 키
     * @param locale     The locale for error message generation | 에러 메시지 처리를 위한 로케일
     * @return A JSON string containing the validation rules | 서버에서 정의된 검증 규칙이 포함된 JSON 문자열
     * @implNote
     *           <p>
     *           <b>■ 사용 사례 1: Thymeleaf 데이터 속성에 설정 (추천)</b>
     *           </p>
     *
     *           <pre>{@code
     * // Controller (Java)
     * model.addAttribute("validationRules", validator.getRulesJson());
     *
     * // View (HTML/Thymeleaf)
     * &lt;form id="saveForm" th:data-rules="${validationRules}"&gt;
     *     &lt;input type="text" name="userId" /&gt;
     *     &lt;button type="button" onclick="doSave()"&gt;저장&lt;/button&gt;
     * &lt;/form&gt;
     *
     * // Script (JS)
     * function doSave() {
     *     const errors = S2Validator.validateForm('#saveForm');
     * }
     * }</pre>
     *
     *           <p>
     *           <b>■ 사용 사례 2: JavaScript 변수에 직접 할당</b>
     *           </p>
     *
     *           <pre>{@code
     * const myRules = '[[${validationRules}]]';
     *
     * function doSave() {
     *     const errors = S2Validator.validateForm('#saveForm', myRules);
     * }
     * }</pre>
     */
    public static String getRulesJson(String contextKey, Locale locale) {
        S2Validator<?> validator = validatorCache.get(contextKey);
        return getRulesJson(validator, locale);
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
     * @implNote
     *           <p>
     *           <b>■ 사용 사례 1: Thymeleaf 데이터 속성에 설정 (추천)</b>
     *           </p>
     *
     *           <pre>{@code
     * // Controller (Java)
     * model.addAttribute("validationRules", validator.getRulesJson());
     *
     * // View (HTML/Thymeleaf)
     * &lt;form id="saveForm" th:data-rules="${validationRules}"&gt;
     *     &lt;input type="text" name="userId" /&gt;
     *     &lt;button type="button" onclick="doSave()"&gt;저장&lt;/button&gt;
     * &lt;/form&gt;
     *
     * // Script (JS)
     * function doSave() {
     *     const errors = S2Validator.validateForm('#saveForm');
     * }
     * }</pre>
     *
     *           <p>
     *           <b>■ 사용 사례 2: JavaScript 변수에 직접 할당</b>
     *           </p>
     *
     *           <pre>{@code
     * const myRules = '[[${validationRules}]]';
     *
     * function doSave() {
     *     const errors = S2Validator.validateForm('#saveForm', myRules);
     * }
     * }</pre>
     */
    public static String getRulesJson(S2Validator<?> validator, Locale locale) {
        if (validator == null) {
            if (S2Util.isKorean(Locale.getDefault())) {
                logger.warn("해당 키에 등록된 규칙이 없습니다.");
            } else {
                logger.warn("No rules registered for the given key.");
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
