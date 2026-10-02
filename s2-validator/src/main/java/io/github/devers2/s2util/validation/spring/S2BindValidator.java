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
package io.github.devers2.s2util.validation.spring;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.BindingResult;

import io.github.devers2.s2util.validation.S2Validator;

/**
 * Bridge between S2Util Validation and Spring Framework.
 * <p>
 * This class facilitates seamless integration with Spring MVC by mapping S2Util
 * validation failures directly to Spring's {@link BindingResult}. It allows
 * developers to use a fluent, type-safe validation DSL while maintaining
 * compatibility with Spring's standard error reporting mechanisms.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 검증 엔진과 Spring Framework를 연결하는 가교 클래스입니다.
 * <p>
 * S2Util의 검증 실패 결과를 Spring MVC의 {@link BindingResult}로 직접 매핑하여, 유연한 선언적 검증
 * DSL을 사용하면서도 Spring 고유의 에러 처리 메커니즘을 그대로 유지할 수 있게 합니다.
 * </p>
 * <ul>
 * <li><b>플러그인 적용 (build.gradle):</b>
 *
 * <pre>{@code
 *  plugins {
 *      id 'io.github.devers2.validator' version '2.0.0' // 버전은 상황에 맞게 설정
 *  }
 *     }</pre>
 *
 * </li>
 * <li><b>기본 동작:</b> 플러그인이 적용된 프로젝트에서는 {@code ./gradlew build}, {@code compileJava} 등 Gradle 태스크 실행 시 자동으로 검증이 수행된다.</li>
 * <li><b>IDE 환경 설정 (필요시):</b> IDE의 'Run' 버튼으로 직접 실행 시 Gradle 빌드 사이클을 우회하는 경우가 있으며, 이 경우 검증이 누락될 수 있다. 이를 방지하기 위해 다음 설정을 권장한다.
 * <ul>
 * <li><b>VS Code:</b> {@code .vscode/launch.json}에 {@code "preLaunchTask": "classes"}를 추가하고,
 * {@code .vscode/tasks.json}에 에러 자동 노출 작업을 정의한다.
 *
 * <pre>{@code
 *  // 1. launch.json 예시
 *  {
 *    "configurations": [{
 *      "name": "www",
 *      "mainClass": "com.example.Application",
 *      "preLaunchTask": "classes" // 핵심 설정
 *    }]
 *  }
 *
 *  // 2. tasks.json 예시
 *  {
 *    "version": "2.0.0",
 *    "tasks": [{
 *      "label": "classes",
 *      "type": "shell",
 *      "command": "./gradlew",
 *      "args": ["classes"],
 *      "problemMatcher": ["$gradle"],
 *      "presentation": { "reveal": "always", "focus": true, "clear": true }
 *    }]
 *  }
 *       }</pre>
 *
 * </li>
 * <li><b>IntelliJ IDEA:</b> {@code Settings > Build, Execution, Deployment > Build Tools > Gradle} 메뉴에서 <b>Build and run using</b>을 <b>Gradle</b>로 변경한다.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h3>Technical Highlights (기술적 특징)</h3>
 * <ul>
 * <li><b>Spring Native Integration:</b> Automatically resolves the current user's
 * locale using {@link LocaleContextHolder}.</li>
 * <li><b>Contextual Binding:</b> {@link #bind(S2Validator)} wraps a validator instance in a {@link BoundContext}
 * for server-side validation and client-side rule export, without any global registry.</li>
 * <li><b>Static Analysis Friendly:</b> Designed to work with {@code s2-validator-plugin}
 * for compile-time verification of field names in DTOs.</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 * @see S2Validator
 * @see S2Validator#getRulesJson(Locale)
 */
public final class S2BindValidator {

    private S2BindValidator() {
        // Prevent instantiation
    }

    /**
     * Binds a validator instance to the current Spring environment.
     * <p>
     * The returned {@link BoundContext} validates into a {@link BindingResult} ({@code validate}) and exports the same
     * rules to the browser ({@code getRulesJson}). Call both with validators built from the same rule definition so the
     * GET form and the POST handler apply identical rules. Building a validator is cheap (well under a microsecond for a
     * typical form), so it can be built per request; keep it in a field or Spring bean if the rule definition itself is
     * expensive.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증기 인스턴스를 현재 Spring 환경에 바인딩합니다.
     * <p>
     * 반환된 {@link BoundContext}는 {@link BindingResult}로 검증({@code validate})하고, 같은 규칙을 브라우저용으로 내보냅니다({@code getRulesJson}).
     * 같은 규칙 정의로 만든 검증기로 두 가지를 호출하면 GET 폼과 POST 처리기가 동일한 규칙을 적용합니다. 검증기 생성 비용은 일반적인 폼에서
     * 1마이크로초 미만이라 요청마다 만들어도 되며, 규칙 정의 자체가 무거우면 필드나 Spring 빈에 보관하십시오.
     * </p>
     *
     * @param <T>       The DTO or Domain model type | DTO 또는 도메인 모델 타입
     * @param validator The validator instance to bind | 바인딩할 검증기 인스턴스
     * @return A {@link BoundContext} for fluent execution | 유연한 실행을 위한 BoundContext 객체
     * @throws IllegalArgumentException If {@code validator} is null | validator 가 null 인 경우
     * @apiNote
     *
     *          <pre>
     * {@code
     * // Example 1: Spring MVC controller (GET form + POST submit share one rule definition)
     * &#64;Controller
     * &#64;RequestMapping("/member")
     * public class MemberController {
     *
     *     private S2Validator<MemberDTO> memberRules() {
     *         return S2Validator.<MemberDTO>builder()
     *             .field("userId", "아이디").rule(S2RuleType.REQUIRED)
     *                 .rule(S2RuleType.MIN_LENGTH, 4).ko("{0|은/는} 최소 {1}자 이상이어야 합니다.")
     *             .field("userPw", "비밀번호").rule(S2RuleType.REQUIRED).rule(S2RuleType.PASSWORD)
     *             .field("confirmPw", "비밀번호 확인")
     *                 .rule(S2RuleType.REQUIRED)
     *                 .rule(S2RuleType.EQUALS_FIELD, "userPw").ko("비밀번호가 일치하지 않습니다.")
     *             .field("email", "이메일").rule(S2RuleType.EMAIL)
     *             .build();
     *     }
     *
     *     &#64;GetMapping("/join")
     *     public String joinForm(Model model) {
     *         model.addAttribute("member", new MemberDTO());
     *         model.addAttribute("validationRules", S2BindValidator.bind(memberRules()).getRulesJson());
     *         return "member/join";
     *     }
     *
     *     &#64;PostMapping("/join")
     *     public String joinSubmit(@ModelAttribute("member") MemberDTO member, BindingResult result, Model model) {
     *         S2BindValidator.bind(memberRules()).validate(member, result);
     *
     *         if (result.hasErrors()) {
     *             model.addAttribute("validationRules", S2BindValidator.bind(memberRules()).getRulesJson());
     *             return "member/join";
     *         }
     *         memberService.join(member);
     *         return "redirect:/member/welcome";
     *     }
     * }
     *
     * // Example 2: Thymeleaf template (member/join.html)
     * <form id="joinForm" th:action="@{/member/join}" method="post"
     *       th:object="${member}" th:data-s2-rules="${validationRules}">
     *     <input type="text" th:field="*{userId}" />
     *     <span th:errors="*{userId}"></span>
     *     <input type="password" th:field="*{userPw}" />
     *     <input type="password" th:field="*{confirmPw}" />
     *     <button type="submit">가입하기</button>
     * </form>
     * <script type="module" src="/s2-util/js/s2.validator.js"></script>
     * <!-- Forms with data-s2-rules are validated automatically on submit.
     *      Manual call: const errors = S2Validator.validate('#joinForm');
     *      errors is an object { fieldName: [messages] }; empty when valid. -->
     *
     * // Example 3: REST API with a JSON error response
     * &#64;PostMapping("/api/validate")
     * public ResponseEntity<?> validateData(@RequestBody Map<String, Object> data) {
     *     BindingResult result = new MapBindingResult(data, "apiData");
     *     S2BindValidator.bind(apiRules()).validate(data, result);
     *
     *     if (result.hasErrors()) {
     *         Map<String, String> errorMap = result.getFieldErrors().stream()
     *             .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
     *         return ResponseEntity.badRequest().body(errorMap);
     *     }
     *     return ResponseEntity.ok("Validation passed");
     * }
     * }
     *           </pre>
     */
    public static <T> BoundContext<T> bind(S2Validator<T> validator) {
        if (validator == null) {
            throw new IllegalArgumentException("validator cannot be null");
        }
        return new BoundContext<>(validator);
    }

    /**
     * Sets the base name of the resource bundle to be commonly referred to by the validation system.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증 시스템에서 공통으로 참조할 메시지 번들(ResourceBundle)의 기본 이름을 설정합니다.
     * <p>
     * 내부적으로 {@link S2Validator#setValidationBundle(String)}을 호출하여 설정값을 공유합니다.
     * </p>
     *
     * @param bundleName Base name of the resource bundle (including path) | 리소스 번들의 BaseName (경로 포함)
     */
    public static void setValidationBundle(String bundleName) {
        S2Validator.setValidationBundle(bundleName);
    }

    /**
     * Contextual wrapper that binds a validator to the current Spring environment.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증기를 현재 Spring 실행 환경(BindingResult, Locale 등)에 바인딩하는 컨텍스트 래퍼입니다.
     *
     * @param <T> The target type
     */
    public static class BoundContext<T> {
        private final S2Validator<T> validator;

        private BoundContext(S2Validator<T> validator) {
            this.validator = validator;
        }

        /**
         * Validates the target object and records errors into the BindingResult.
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 대상 객체를 검증하고 발생한 에러를 Spring의 {@link BindingResult}에 기록합니다.
         *
         * @param target        The target object to validate | 검증 대상 객체
         * @param bindingResult Spring's binding result object | 스프링 바인딩 결과 객체
         */
        @SuppressWarnings("null")
        public void validate(T target, BindingResult bindingResult) {
            validator.validate(
                    target,
                    (error -> {
                        bindingResult.rejectValue(
                                error.fieldName(),
                                error.errorCode() != null ? error.errorCode() : "",
                                error.errorArgs(),
                                error.defaultMessage());
                    }), LocaleContextHolder.getLocale());
        }

        /**
         * Returns a JSON rule string for client-side sharing.
         * <p>
         * The JSON returned by this method is structured to be interpretable by the
         * {@code s2.validator.js} library. It is typically used in HTML data attributes
         * or assigned directly to JavaScript variables.
         * </p>
         *
         * <p>
         * <b>[한국어 설명]</b>
         * </p>
         * 클라이언트(Browser)와 검증 규칙을 공유하기 위한 JSON 문자열을 반환합니다.
         *
         * @return A JSON string containing the validation rules | 서버에서 정의된 검증 규칙이 포함된 JSON 문자열
         * @see S2Validator#getRulesJson(Locale)
         * @apiNote
         *
         *          <pre>{@code
         * // Controller (Java): uses the current request locale (LocaleContextHolder)
         * model.addAttribute("validationRules", S2BindValidator.bind(validator).getRulesJson());
         *
         * // View (HTML/Thymeleaf)
         * &lt;form id="saveForm" th:data-s2-rules="${validationRules}"&gt;
         *     &lt;input type="text" name="userId" /&gt;
         *     &lt;button type="button" onclick="doSave()"&gt;저장&lt;/button&gt;
         * &lt;/form&gt;
         *
         * // Script (JS)
         * // Just import it and it will be applied to all forms that have data-s2-rules. | 임포트만 하면 data-s2-rules가 있는 모든 폼에 적용된다.
         * import '/s2-util/js/s2.validator.js';
         *
         * or With Thymeleaf
         *
         * const contextPath = \/*[[@{/}]]*\/ '';
         * import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);
         *  }</pre>
         */
        public String getRulesJson() {
            return validator.getRulesJson(LocaleContextHolder.getLocale());
        }
    }

}
