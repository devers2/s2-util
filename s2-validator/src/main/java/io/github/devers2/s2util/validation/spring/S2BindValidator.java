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
import java.util.function.Supplier;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.BindingResult;

import io.github.devers2.s2util.validation.S2Validator;
import io.github.devers2.s2util.validation.S2ValidatorFactory;

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
 *      id 'io.github.devers2.validator' version '1.0.0' // 버전은 상황에 맞게 설정
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
 *      "mainClass": "biz.placelink.redwit.lp.ApplicationBootstrap",
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
 * <li><b>Contextual Binding:</b> Encapsulates validation logic into a {@link BoundContext}
 * to manage specific server-side validation and client-side rule synchronization.</li>
 * <li><b>Static Analysis Friendly:</b> Designed to work with {@code s2-validator-plugin}
 * for compile-time verification of field names in DTOs.</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 * @see S2Validator
 * @see S2ValidatorFactory
 */
public final class S2BindValidator {

    private S2BindValidator() {
        // Prevent instantiation
    }

    /**
     * Initializes a validation context with lazy registration support.
     * <p>
     * Use this method to obtain a {@link BoundContext} which handles both server-side
     * execution and client-side metadata generation.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지연 등록(Lazy Registration) 기능을 지원하는 검증 컨텍스트를 초기화합니다.
     * <p>
     * 서버 측 검증 실행과 클라이언트 측 메타데이터 동기화를 모두 처리할 수 있는 {@link BoundContext}를
     * 획득할 때 사용합니다.
     * </p>
     *
     * @param <T>               The DTO or Domain model type | DTO 또는 도메인 모델 타입
     * @param contextKey        Identifier for the validation logic (cached in {@link S2ValidatorFactory}) | 검증 로직 식별자 (S2ValidatorFactory에 캐싱됨)
     * @param validatorSupplier Functional supplier that builds the validator on first call | 첫 호출 시 검증기를 생성하는 함수형 서플라이어
     * @return A {@link BoundContext} for fluent execution | 유연한 실행을 위한 BoundContext 객체
     * @implNote
     *
     *           <pre>
     * {@code
     * // Example 1: Complete Spring MVC Controller Integration
     * &#64;Controller
     * &#64;RequestMapping("/member")
     * public class MemberController {
     *
     *     // Define validation rules as a private method
     *     private S2Validator<MemberDTO> memberRules() {
     *         return S2Validator.builder()
     *             .field("userId", "아이디").rule(S2RuleType.REQUIRED)
     *                 .rule(S2RuleType.MIN_LENGTH, 4).ko("{0|은/는} 최소 {1}자 이상이어야 합니다.")
     *             .field("userPw", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
     *             .field("confirmPw", "비밀번호 확인")
     *                 .rule((value, target) ->
     *                     S2Util.getValue(target, "userPw", "").equals(value)
     *                 ).ko("비밀번호가 일치하지 않습니다.")
     *             .field("email", "이메일").rule(S2RuleType.EMAIL)
     *             .build();
     *     }
     *
     *     // GET: Display join form with client-side validation rules
     *     &#64;GetMapping("/join")
     *     public String joinForm(Model model) {
     *         model.addAttribute("member", new MemberDTO());
     *
     *         // Generate JSON for client-side validation
     *         String rulesJson = S2BindValidator.context("MEMBER_JOIN", this::memberRules)
     *             .getRulesJson();
     *
     *         model.addAttribute("validationRules", rulesJson);
     *         return "member/join";
     *     }
     *
     *     // POST: Process form submission with server-side validation
     *     &#64;PostMapping("/join")
     *     public String joinSubmit(@ModelAttribute MemberDTO member,
     *                             BindingResult result, Model model) {
     *
     *         // Execute server-side validation
     *         S2BindValidator.context("MEMBER_JOIN", this::memberRules)
     *             .validate(member, result);
     *
     *         if (result.hasErrors()) {
     *             // Re-display form with error messages
     *             model.addAttribute("validationRules",
     *                 S2BindValidator.context("MEMBER_JOIN", this::memberRules)
     *                     .getRulesJson()
     *             );
     *             return "member/join";
     *         }
     *
     *         // Process successful submission
     *         memberService.join(member);
     *         return "redirect:/member/welcome";
     *     }
     * }
     *
     * // Example 2: Thymeleaf Template (member/join.html)
     * <!-- HTML Form -->
     * <form id="joinForm" th:action="@{/member/join}" method="post"
     *       th:object="${member}" th:data-rules="${validationRules}">
     *
     *     <input type="text" th:field="*{userId}" />
     *     <span th:errors="*{userId}"></span>
     *
     *     <input type="password" th:field="*{userPw}" />
     *     <span th:errors="*{userPw}"></span>
     *
     *     <input type="password" th:field="*{confirmPw}" />
     *     <span th:errors="*{confirmPw}"></span>
     *
     *     <button type="button" onclick="validateAndSubmit()">가입하기</button>
     * </form>
     *
     * <!-- JavaScript -->
     * <script>
     * function validateAndSubmit() {
     *     // Client-side validation using s2.validator.js
     *     const errors = S2Validator.validateForm('#joinForm');
     *
     *     if (errors.length === 0) {
     *         document.getElementById('joinForm').submit();
     *     } else {
     *         errors.forEach(err => console.error(err.message));
     *     }
     * }
     * </script>
     *
     * // Example 3: REST API with JSON response
     * &#64;RestController
     * public class ApiController {
     *     &#64;PostMapping("/api/validate")
     *     public ResponseEntity<?> validateData(@RequestBody Map<String, Object> data) {
     *         BindingResult result = new MapBindingResult(data, "apiData");
     *
     *         S2BindValidator.context("API_DATA", this::apiRules)
     *             .validate(data, result);
     *
     *         if (result.hasErrors()) {
     *             Map<String, String> errorMap = result.getFieldErrors().stream()
     *                 .collect(Collectors.toMap(
     *                     FieldError::getField,
     *                     FieldError::getDefaultMessage
     *                 ));
     *             return ResponseEntity.badRequest().body(errorMap);
     *         }
     *
     *         return ResponseEntity.ok("Validation passed");
     *     }
     * }
     * }
     *           </pre>
     */
    public static <T> BoundContext<T> context(String contextKey, Supplier<S2Validator<T>> validatorSupplier) {
        S2Validator<T> validator = S2ValidatorFactory.getOrRegister(contextKey, validatorSupplier);
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
                                error.defaultMessage()
                        );
                    }), LocaleContextHolder.getLocale()
            );
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
         * @see S2ValidatorFactory#getRulesJson(String, Locale)
         * @implNote
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
            return S2ValidatorFactory.getRulesJson(validator, LocaleContextHolder.getLocale());
        }
    }

}
