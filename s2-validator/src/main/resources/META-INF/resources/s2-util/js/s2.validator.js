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

/**
 * Initializes automatic S2Validator binding.
 * <p>
 * Listens for document-level submit events and automatically validates forms
 * with the data-s2-rules attribute. Automatically executed when the library loads,
 * so no manual invocation is required.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Validator 자동 바인딩 초기화.
 * <p>
 * document 레벨에서 제출(submit) 이벤트를 감지하여, data-s2-rules 속성이 정의된
 * 폼에 대해 자동으로 검증을 수행합니다. 라이브러리 로드 시 기본적으로 자동 실행되므로,
 * 별도로 호출할 필요가 없습니다.
 * </p>
 *
 * @function initS2Validator
 * @example
 * // ⚠️ Both forms below assume the consuming app hasn't disabled/overridden its framework's
 * // default "serve static files from classpath:/META-INF/resources/" behavior (e.g. Spring
 * // Boot's default static resource handling) — that's what actually serves this file.
 * // ⚠️ 아래 두 방식 모두, 소비 프로젝트가 프레임워크의 기본 "classpath:/META-INF/resources/
 * // 정적 파일 서빙"(예: Spring Boot 기본 정적 리소스 핸들링)을 끄거나 오버라이드하지 않았다는
 * // 전제가 있습니다 — 이 파일이 실제로 서빙되는 것도 그 기본 동작 덕분입니다.
 * //
 * // Import only - absolute path. Only works if the app is deployed at the server ROOT
 * // context path ("/"); breaks under any other context path (e.g. "/app").
 * // 임포트만 하면 되지만 절대경로라서, 앱이 서버 루트 컨텍스트 경로("/")로 배포된 경우에만
 * // 동작한다. "/app"처럼 컨텍스트 경로가 있으면 깨진다.
 * import '/s2-util/js/s2.validator.js';
 *
 * // With Thymeleaf (recommended) - @{...} resolves the actual context path at render time,
 * // so this works regardless of the deployment's context path.
 * // With Thymeleaf (권장) - @{...}가 렌더링 시점에 실제 컨텍스트 경로를 채워주므로,
 * // 배포 컨텍스트 경로와 무관하게 항상 동작한다.
 * const contextPath = \/*[[@{/}]]*\/ '';
 * import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);
 *
 * <form id="myForm" th:data-s2-rules="${rules}">
 *   ...
 * </form>
 */
export const initS2Validator = () => {
  if (isS2ValidatorInitialized) return;

  // 네이티브 제약(required, pattern, type="email" 등)이 하나라도 걸려 있으면, 브라우저가 submit
  // 이벤트 자체를 발생시키지 않고(스펙상 "제약 조건 대화형 검증" 단계에서 자체 차단) 자체 기본
  // 메시지만 띄운 뒤 끝나버린다. 그러면 아래 submit 리스너가 아예 실행되지 않아 S2Validator의
  // 커스텀 메시지는커녕 같은 폼의 다른 필드 검증까지 통째로 건너뛰게 된다. data-s2-rules가 붙은
  // 폼은 noValidate를 강제하여 네이티브 검증이 끼어들지 못하게 하고, 이 라이브러리가 검증을 전담한
  // 뒤 setCustomValidity()+reportValidity()로 동일한 네이티브 UI를 그대로 활용한다. |
  // If a field carries any native constraint (required, pattern, type="email", ...), the browser
  // blocks the submit event entirely during its own "interactively validate the constraints" step
  // and shows only its own default message — so the submit listener below never runs at all,
  // silently skipping validation for every other field in the form too, not just that one. Forms
  // with data-s2-rules get noValidate forced on so native validation can never intercept; this
  // library then owns validation end-to-end while still reusing the same native UI via
  // setCustomValidity()+reportValidity().
  const disableNativeValidation = (form) => {
    if (form instanceof HTMLFormElement) {
      form.noValidate = true;
    }
  };

  // 페이지 로드 시점에 이미 존재하는 폼 처리
  document.querySelectorAll('form[data-s2-rules]').forEach(disableNativeValidation);

  // 이후 동적으로 추가되거나(SPA/AJAX) data-s2-rules 속성이 나중에 붙는 폼까지 확실하게 커버함.
  // "사용자가 제출 전 필드를 먼저 포커스한다"처럼 상호작용 타이밍에 기대는 방식은, 필드를 한 번도
  // 건드리지 않고 곧장 제출 버튼만 누르는 극단적인 경우 놓칠 수 있어 채택하지 않는다. 대신
  // MutationObserver로 DOM 삽입/속성 변경 시점에 직접, 확정적으로 처리한다. | Also reliably covers
  // forms added dynamically (SPA/AJAX) or that get data-s2-rules set later. A timing-based
  // approach (e.g. "the user focuses a field before submitting") can miss the case where the user
  // never touches any field and goes straight for the submit button, so a MutationObserver is used
  // instead to react deterministically to DOM insertion / attribute changes.
  const scanAndDisable = (root) => {
    if (root.matches?.('form[data-s2-rules]')) disableNativeValidation(root);
    root.querySelectorAll?.('form[data-s2-rules]').forEach(disableNativeValidation);
  };

  const observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      if (mutation.type === 'childList') {
        mutation.addedNodes.forEach((node) => {
          if (node instanceof Element) scanAndDisable(node);
        });
      } else if (mutation.type === 'attributes' && mutation.target instanceof HTMLFormElement) {
        disableNativeValidation(mutation.target);
      }
    }
  });
  observer.observe(document.documentElement, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['data-s2-rules']
  });

  document.addEventListener('submit', (e) => {
    const form = e.target;

    // 선택자에 매칭되는 폼인 경우 자동 검증 수행
    if (form && form instanceof HTMLFormElement && form.matches('form[data-s2-rules]')) {
      const errors = S2Validator.validate(form);

      // 검증 에러가 존재할 경우 전송 중단
      if (Object.keys(errors).length > 0) {
        e.preventDefault();
      }
    }
  });

  isS2ValidatorInitialized = true;
};

/**
 * Client-side validation library.
 * <p>
 * Validates form fields based on JSON rules received from the server.
 * Provides comprehensive validation support for various data types and formats.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 클라이언트 측 유효성 검증 라이브러리.
 * <p>
 * 서버에서 받은 JSON 규칙을 기반으로 폼 필드를 검증합니다.
 * 다양한 데이터 타입과 형식에 대한 포괄적인 검증 기능을 제공합니다.
 * </p>
 *
 * @module S2Validator
 */
export const S2Validator = {
  /**
   * Validates the entire form and returns an error object.
   * <p>
   * Can validate based on JSON rules received from the server, or can be used
   * for form validation by passing rule objects directly without server JSON.
   * Integrates with browser's native validation UI via HTML5 Constraint Validation API.
   * </p>
   *
   * <p>
   * <b>[한국어 설명]</b>
   * </p>
   * 전체 폼을 검증하고 에러 객체를 반환합니다.
   * <p>
   * 서버에서 받은 JSON 규칙을 기반으로 검증할 수 있으며, 서버 JSON 없이도 직접 규칙 객체를
   * 전달하여 폼 검증 용도로 사용할 수 있습니다. HTML5 Constraint Validation API를 통해
   * 브라우저의 기본 검증 UI와 통합됩니다.
   * </p>
   *
   * <h3>Supported RuleTypes (지원되는 규칙 타입)</h3>
   * <ul>
   * <li><b>REQUIRED:</b> Required field | 필수 입력</li>
   * <li><b>LENGTH:</b> Exact length | 정확한 길이</li>
   * <li><b>MIN_LENGTH:</b> Minimum length | 최소 길이</li>
   * <li><b>MAX_LENGTH:</b> Maximum length | 최대 길이</li>
   * <li><b>MIN_BYTE:</b> Minimum byte size | 최소 바이트</li>
   * <li><b>MAX_BYTE:</b> Maximum byte size | 최대 바이트</li>
   * <li><b>MIN_VALUE:</b> Minimum value (numeric) | 최소 값 (숫자)</li>
   * <li><b>MAX_VALUE:</b> Maximum value (numeric) | 최대 값 (숫자)</li>
   * <li><b>REGEX:</b> Regular expression (value: regex string) | 정규식 (value: regex 문자열)</li>
   * <li><b>NUMBER:</b> Numeric format | 숫자 형식</li>
   * <li><b>TEXT_INTACT:</b> Text as-is | 텍스트 그대로</li>
   * <li><b>TEXT_COMBINE:</b> Combined text | 텍스트 결합</li>
   * <li><b>MPHONE_NO:</b> Mobile phone number | 휴대폰 번호</li>
   * <li><b>TEL_NO:</b> Telephone number | 전화번호</li>
   * <li><b>INTERNATIONAL_TEL_NO:</b> International telephone number | 국제 전화번호</li>
   * <li><b>EMAIL:</b> Email address | 이메일</li>
   * <li><b>ZIP:</b> Postal code | 우편번호</li>
   * <li><b>LOGIN_ID:</b> Login identifier | 로그인 ID</li>
   * <li><b>PASSWORD:</b> Password | 비밀번호</li>
   * <li><b>PASSWORD_ANSWR:</b> Password answer | 비밀번호 답변</li>
   * <li><b>BIZRNO:</b> Business registration number | 사업자 번호</li>
   * <li><b>NWINO:</b> Foreigner number | 외국인 번호</li>
   * <li><b>JUMIN:</b> Resident registration number | 주민번호</li>
   * <li><b>DATE:</b> Date format | 날짜 형식</li>
   * <li><b>DATE_AFTER:</b> Date after (value: target field name) | 날짜 이후 (value: targetField 이름)</li>
   * <li><b>DATE_BEFORE:</b> Date before (value: target field name) | 날짜 이전 (value: targetField 이름)</li>
   * <li><b>EQUALS_FIELD:</b> Field equality (value: target field name) | 필드 동등 (value: targetField 이름)</li>
   * <li><b>NESTED:</b> Nested object validation (value: sub-validator) | 중첩 객체 검증 (value: 하위 S2Validator)</li>
   * <li><b>EACH:</b> List/array element iteration validation (value: sub-validator) | 리스트/배열 요소 반복 검증 (value: 하위 S2Validator)</li>
   * </ul>
   *
   * <h3>JSON Structure Example (JSON 구조 예시)</h3>
   * <pre>
   * [
   *   {
   *     "name": "userId",
   *     "label": "아이디",
   *     "rules": [
   *       { "type": "REQUIRED", "value": null, "regex": null, "message": "아이디는 필수입니다." },
   *       { "type": "MIN_LENGTH", "value": 5, "regex": null, "message": "최소 5자 이상 입력하세요." }
   *     ]
   *   },
   *   {
   *     "name": "password",
   *     "label": "비밀번호",
   *     "rules": [
   *       { "type": "PASSWORD", "value": null, "regex": "^(?=.*\\d)(?=.*[a-zA-Z]).{8,}$", "message": "비밀번호 형식이 올바르지 않습니다." },
   *       { "type": "EQUALS_FIELD", "value": "confirmPassword", "regex": null, "message": "비밀번호가 일치하지 않습니다." }
   *     ]
   *   }
   * ]
   * </pre>
   *
   * <p>
   * <b>Note:</b> validationRules can be passed as rulesSource argument or set on the form/child element.
   * </p>
   * <p>
   * <b>참고:</b> validationRules 은 JSON 구조로 rulesSource 인자로 넘겨주거나 폼 또는 하위 엘리먼트에 설정할 수 있습니다.
   * </p>
   * <pre>
   * &lt;form id="saveForm" th:data-s2-rules="${validationRules}"&gt; ... &lt;/form&gt;
   * </pre>
   * <p>
   * <b>Proxy Error Element: (프록시 에러 엘리먼트)</b><br>
   * If validation fails for a hidden field or a field where browser tooltips cannot be shown,
   * you can provide a proxy element with the name <code>"{fieldName}_error"</code> to display the error.
   * This is useful for file uploads or custom UI widgets where the actual input is hidden.
   * <br>
   * 히든 필드나 브라우저 툴팁을 표시할 수 없는 필드의 경우, <code>"{fieldName}_error"</code> 라는 이름의
   * 프록시 엘리먼트를 두어 에러를 표시할 수 있습니다. 파일 업로드나 커스텀 UI 위젯 등 실제 input이 숨겨진 경우 유용합니다.
   * </p>
   * <pre>
   * &lt;!-- Example: Hidden file input with proxy error element --&gt;
   * &lt;input type="hidden" name="files" ...&gt;
   * &lt;!-- Tooltip needed? Use a creating transparent input or just use span/div for text --&gt;
   * &lt;input name="files_error" style="width: 1px; opacity: 0;" /&gt;
   * </pre>
   *
   * @function validate
   * @param {string|HTMLFormElement} formSource - Form element selector or HTMLFormElement object | 검증할 폼 요소의 셀렉터 또는 HTMLFormElement 객체
   * @param {string|Object} [rulesSource] - Validation rules (JSON/Object). If omitted, searches element's data-s2-rules | 검증 규칙(JSON/Object). 생략 시 엘리먼트의 data-s2-rules를 탐색
   * @param {Object} [additionalData] - Additional data for validation (priority: additionalData > DOM). Use this to validate data not present in the form DOM (e.g., file arrays). | 검증을 위한 추가 데이터 (우선순위: additionalData > DOM). 폼 DOM에 없는 데이터(예: 파일 배열)를 검증할 때 사용합니다.
   * @returns {Object} Error object {fieldName: [errorMessages]} – empty object if valid | 에러 객체 {fieldName: [errorMessages]} – 빈 객체 시 유효
   * @example
   * // ⚠️ Absolute path. Only works if the app is deployed at the server ROOT context path
   * // ("/"); breaks under any other context path (e.g. "/app"). Also assumes the consuming
   * // app hasn't disabled/overridden its framework's default static-resource-from-JAR serving.
   * // ⚠️ 절대경로라서 앱이 서버 루트 컨텍스트 경로("/")로 배포된 경우에만 동작하며, 그 외
   * // 컨텍스트 경로(예: "/app")에서는 깨진다. 또한 프레임워크의 기본 JAR 정적 리소스 서빙을
   * // 끄거나 오버라이드하지 않았다는 전제도 필요하다.
   * import { S2Validator } from '/s2-util/js/s2.validator.js';
   *
   * // With Thymeleaf (recommended) - @{...} resolves the actual context path at render time,
   * // so this works regardless of the deployment's context path.
   * // With Thymeleaf (권장) - @{...}가 렌더링 시점에 실제 컨텍스트 경로를 채워주므로,
   * // 배포 컨텍스트 경로와 무관하게 항상 동작한다.
   * <script type="importmap" th:inline="javascript">
   *   {
   *     "imports": {
   *       "s2-validator": [[@{/s2-util/js/s2.validator.js}]]
   *     }
   *   }
   * </script>
   * <script type="module">
   *   import { S2Validator } from 's2-validator';
   *   // S2Validator 사용
   * </script>
   *
   * // 1. Basic usage (automatically searches HTML's data-s2-rules attribute)
   * // 1. 기본 사용 (HTML의 data-s2-rules 속성 자동 탐색)
   * const errors = S2Validator.validate('#myForm');
   *
   * <form id="myForm" th:data-s2-rules="${rules}">
   *   ...
   * </form>
   *
   * // 2. Passing custom rules directly
   * // 2. 커스텀 규칙을 직접 전달하는 경우
   * const myRules = [
   *   { name: 'email', rules: [{ type: 'EMAIL', message: '올바른 이메일 형식이 아닙니다.' }] }
   * ];
   * const errors = S2Validator.validate('#myForm', myRules);
   *
   * // 3. With additional data (prioritizes additionalData over form fields)
   * // 3. 추가 데이터와 함께 사용 (폼 필드보다 additionalData 우선)
   * const additionalData = { 'files': myFilesArray };
   * const errors = S2Validator.validate('#myForm', rules, additionalData);
   */
  validate(formSource, rulesSource, additionalData) {
    const form = typeof formSource === 'string' ? document.querySelector(formSource) : formSource;
    if (!form || !(form instanceof HTMLFormElement)) {
      return { __system_error__: ['유효한 폼 요소를 찾을 수 없습니다.'] };
    }

    // 검증 전 모든 폼 엘리먼트의 CustomValidity 초기화 및 자동 초기화 이벤트 등록
    Array.from(form.elements).forEach((el) => {
      if (typeof el.setCustomValidity === 'function') {
        el.setCustomValidity('');

        // 커스텀 에러 엘리먼트 초기화
        const errorEl = form.querySelector(`[name="${el.name}_error"]`);
        if (errorEl) {
          if (typeof errorEl.setCustomValidity === 'function') errorEl.setCustomValidity('');
          errorEl.textContent = '';
        }

        // HTML5 Validation 연동: 사용자가 입력을 시작하면 즉시 에러 상태를 해제
        if (!el.__s2_val_bound__) {
          const clearValidity = () => {
            if (el.type === 'radio' || el.type === 'checkbox') {
              // 라디오/체크박스는 그룹 전체의 에러를 해제해야 함
              const group = form.querySelectorAll(`[name="${el.name}"]`);
              group.forEach((groupEl) => groupEl.setCustomValidity(''));
            } else {
              el.setCustomValidity('');
            }
            // 커스텀 에러 엘리먼트 초기화
            const errorEl = form.querySelector(`[name="${el.name}_error"]`);
            if (errorEl) {
              if (typeof errorEl.setCustomValidity === 'function') errorEl.setCustomValidity('');
              errorEl.textContent = '';
            }
          };

          el.addEventListener('input', clearValidity);
          el.addEventListener('change', clearValidity); // 라디오/체크박스 대응
          el.__s2_val_bound__ = true;
        }
      }
    });

    let rules = [];

    if (rulesSource) {
      // rulesSource가 명시적으로 전달된 경우 처리
      try {
        rules = typeof rulesSource === 'string' ? JSON.parse(rulesSource) : rulesSource;
      } catch {
        rules = [];
      }
    }

    if (rules.length === 0) {
      if (form.dataset.s2Rules) {
        try {
          rules = JSON.parse(form.dataset.s2Rules);
        } catch {
          rules = [];
        }
      } else {
        const elementsWithRules = form.querySelector('[data-s2-rules]');
        if (elementsWithRules) {
          try {
            rules = JSON.parse(elementsWithRules.dataset.s2Rules);
          } catch {
            rules = [];
          }
        }
      }
    }

    if (rules.length === 0) {
      return {
        __system_error__: ['검증 규칙 데이터(JSON) 형식이 올바르지 않거나 존재하지 않습니다.']
      };
    }

    const errors = {};
    const formData = getFormData(form); // 전체 필드 값 맵 (cross-field용)
    if (additionalData && typeof additionalData === 'object') {
      Object.assign(formData, additionalData);
    }
    const allFieldNames = Object.keys(formData); // 모든 등록된 필드명 리스트
    const processedFields = new Set(); // 이미 처리한 와일드카드 필드

    /**
     * 규칙들을 재귀적으로 검증한다.
     * @param {Array} currentRules - 현재 레벨의 규칙 리스트
     * @param {string} prefix - 필드명 접두사 (중첩 경로용)
     */
    const validateRules = (currentRules, prefix = '') => {
      // 1단계: [] 와일드카드 필드 그룹화
      const wildcardGroups = {};

      currentRules.forEach((rule) => {
        const fullPath = prefix + rule.name;
        if (fullPath.includes('[]')) {
          // prefix 추출 (예: "products[].name" -> "products")
          const bracketIndex = fullPath.indexOf('[]');
          const collectionPrefix = fullPath.substring(0, bracketIndex);

          if (!wildcardGroups[collectionPrefix]) {
            wildcardGroups[collectionPrefix] = [];
          }
          wildcardGroups[collectionPrefix].push(rule);
        }
      });

      // 2단계: 그룹화된 와일드카드 필드들 처리
      Object.entries(wildcardGroups).forEach(([collectionPrefix, groupRules]) => {
        // 해당 컬렉션의 인덱스들 추출
        const indices = new Set();
        const pattern = new RegExp(`^${escapeRegExp(collectionPrefix)}\\[(\\d+)\\]`);
        allFieldNames.forEach((name) => {
          const match = name.match(pattern);
          if (match) indices.add(match[1]);
        });

        // 각 인덱스별로 그룹 내 모든 필드 검증
        indices.forEach((idx) => {
          groupRules.forEach((rule) => {
            // 조건 체크 (조건 필드명에 '[]'가 있으면 현재 아이템 인덱스로 치환하여 평가)
            if (!isConditionSatisfied(rule, formData, prefix, idx)) return;

            const fullPath = prefix + rule.name;
            // "products[].name" -> ".name" 추출
            const bracketIndex = fullPath.indexOf('[]');
            const suffix = fullPath.substring(bracketIndex + 2);

            // 실제 필드명: products[0].name
            const actualFieldName = collectionPrefix + '[' + idx + ']' + suffix;
            const fieldElements = form.querySelectorAll(`[name="${actualFieldName}"]`);

            let value;
            if (
              additionalData &&
              Object.prototype.hasOwnProperty.call(additionalData, actualFieldName)
            ) {
              value = additionalData[actualFieldName];
            } else {
              if (fieldElements.length === 0) return;
              value = getFieldValue(fieldElements);
            }

            const fieldErrors = [];

            rule.rules.forEach((check) => {
              // 와일드카드에서는 NESTED/EACH 지원 안 함 (이미 서브 validator로 처리 가능)
              if (check.type === 'NESTED' || check.type === 'EACH') return;

              if (!validateCheck(value, check, formData, prefix, actualFieldName)) {
                fieldErrors.push(check.message);
              }
            });

            if (fieldErrors.length > 0) {
              errors[actualFieldName] = fieldErrors;
              // 브라우저 네이티브 검증 UI 연동을 위해 첫 번째 에러 메시지 설정
              const firstMessage = fieldErrors[0];

              const errorEl = form.querySelector(`[name="${actualFieldName}_error"]`);
              if (errorEl) {
                if (typeof errorEl.setCustomValidity === 'function') {
                  errorEl.setCustomValidity(firstMessage);
                } else {
                  errorEl.textContent = firstMessage;
                }
              } else {
                fieldElements.forEach((el) => {
                  if (typeof el.setCustomValidity === 'function') {
                    el.setCustomValidity(firstMessage);
                  }
                });
              }
            }

            processedFields.add(fullPath);
          });
        });
      });

      // 3단계: 일반 필드 처리 (와일드카드가 아닌 필드)
      currentRules.forEach((rule) => {
        // 1. 조건부 검증 로직 가동
        if (!isConditionSatisfied(rule, formData, prefix)) return;

        const fullPath = prefix + rule.name;

        // 이미 와일드카드로 처리된 필드는 스킵
        if (processedFields.has(fullPath)) return;

        const fieldErrors = [];

        rule.rules.forEach((check) => {
          if (check.type === 'NESTED') {
            // 단일 객체 중첩 검증
            validateRules(check.nestedRules || [], fullPath + '.');
          } else if (check.type === 'EACH') {
            // 리스트/배열 요소 반복 검증
            // form에 존재하는 해당 prefix 기반의 인덱스들을 추출
            const indices = new Set();
            const pattern = new RegExp(`^${escapeRegExp(fullPath)}\\[(\\d+)\\]`);
            allFieldNames.forEach((name) => {
              const match = name.match(pattern);
              if (match) indices.add(match[1]);
            });

            // 추출된 각 인덱스별로 하위 규칙 검증 실행
            indices.forEach((idx) => {
              validateRules(check.nestedRules || [], `${fullPath}[${idx}].`);
            });
          } else {
            // 일반 규칙 검증
            const fieldElements = form.querySelectorAll(`[name="${fullPath}"]`);

            let value;
            if (additionalData && Object.prototype.hasOwnProperty.call(additionalData, fullPath)) {
              value = additionalData[fullPath];
            } else {
              if (fieldElements.length === 0) return;
              value = getFieldValue(fieldElements);
            }

            if (!validateCheck(value, check, formData, prefix, fullPath)) {
              fieldErrors.push(check.message);
            }
          }
        });

        if (fieldErrors.length > 0) {
          errors[fullPath] = fieldErrors;
          // 브라우저 네이티브 검증 UI 연동을 위해 첫 번째 에러 메시지 설정
          const firstMessage = fieldErrors[0];

          const errorEl = form.querySelector(`[name="${fullPath}_error"]`);
          if (errorEl) {
            if (typeof errorEl.setCustomValidity === 'function') {
              errorEl.setCustomValidity(firstMessage);
            } else {
              errorEl.textContent = firstMessage;
            }
          } else {
            const fieldElements = form.querySelectorAll(`[name="${fullPath}"]`);
            fieldElements.forEach((el) => {
              if (typeof el.setCustomValidity === 'function') {
                // 화면에 보이지 않는(offsetParent가 없는) 요소는 브라우저가 포커스하지 못하므로,
                // 검증 메시지를 설정하되 포커스 문제로 인한 오류가 발생하지 않도록 주의가 필요함.
                // CSS 수정을 통해 시각적으로만 숨기는 것을 검토할 필요가 있다.
                el.setCustomValidity(firstMessage);
              }
            });
          }
        }
      });
    };

    validateRules(rules);

    // 에러 발생 시 브라우저 에러 메시지 즉시 표시 (HTML5 Validation 연동)
    if (Object.keys(errors).length > 0) {
      try {
        form.reportValidity();
      } catch (e) {
        console.warn(
          'S2Validator: 브라우저가 에러 메시지를 표시할 수 없습니다. 비표시 필드 설정을 확인하세요.',
          e
        );
      }
    }

    return errors;
  }
};

/**
 * Creates a complete form data map (for cross-field validation).
 * <p>
 * Groups fields with the same name and extracts their values.
 * Supports text inputs, radio groups, checkbox groups, and select elements.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 전체 폼 데이터 맵을 생성합니다 (cross-field 검증용).
 * <p>
 * 동일한 이름을 가진 필드들을 그룹화하여 값을 추출합니다.
 * 텍스트 입력, 라디오 그룹, 체크박스 그룹, 셀렉트 요소를 지원합니다.
 * </p>
 *
 * @function getFormData
 * @param {HTMLFormElement} form - Form element | 폼 요소
 * @returns {Object} Field name to value map | 필드 이름: 값 맵
 */
const getFormData = (form) => {
  const data = {};
  const processedNames = new Set();
  const elements = form.elements;

  for (let i = 0; i < elements.length; i++) {
    const name = elements[i].name;
    if (name && !processedNames.has(name)) {
      const fieldElements = form.querySelectorAll(`[name="${name}"]`);
      data[name] = getFieldValue(Array.from(fieldElements));
      processedNames.add(name);
    }
  }
  return data;
};

/**
 * Determines whether the conditions set in the rule are satisfied by comparing with current form data.
 * <p>
 * For array data (checkboxes, etc.), checks for inclusion; for single values, checks for equality.
 * Implements OR logic between condition groups and AND logic within each group.
 * Normalizes Boolean, Enum, Number, and String values for consistent comparison.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 규칙에 설정된 조건(conditions)을 현재 폼 데이터와 비교하여 만족 여부를 판단합니다.
 * <p>
 * 배열(체크박스 등) 데이터일 경우 포함 여부를 확인하며, 단일 값은 일치 여부를 확인합니다.
 * 조건 그룹 간에는 OR 로직을, 각 그룹 내에서는 AND 로직을 적용합니다.
 * Boolean, Enum, Number, String 값을 정규화하여 일관된 비교를 수행합니다.
 * </p>
 *
 * @param {Object} rule - Validation rule object | 검증 규칙 객체
 * @param {Object} formData - Current form data object | 현재 폼의 데이터 객체
 * @param {string} prefix - Field name prefix (for nested paths) | 필드명 접두사 (중첩 경로용)
 * @param {string|number} [wildcardIndex] - Current wildcard item's index. When given, a condition
 *   field name containing "[]" (e.g. "items[].type") is resolved against this item (e.g.
 *   "items[3].type") instead of being looked up as a literal, always-undefined key. | 현재
 *   와일드카드 아이템의 인덱스. 지정되면 조건의 필드명에 포함된 "[]"(예: "items[].type")를 해당
 *   아이템 기준으로 치환하여(예: "items[3].type") 조회합니다.
 * @returns {boolean} True if conditions are satisfied or no conditions exist, false otherwise | 조건을 만족하거나 조건이 없으면 true, 만족하지 않으면 false
 */
const isConditionSatisfied = (rule, formData, prefix = '', wildcardIndex = null) => {
  // 조건이 없으면 항상 검증 수행
  if (!rule.conditions || rule.conditions.length === 0) return true;

  // OR 연산: 하나라도 만족하는 그룹(AND 그룹)이 있으면 true
  return rule.conditions.some((group) => {
    // AND 연산: 그룹 내 모든 조건이 일치해야 함
    return group.every((cond) => {
      let condField = cond.field;
      if (wildcardIndex !== null && condField.includes('[]')) {
        const bracketIndex = condField.indexOf('[]');
        condField =
          condField.substring(0, bracketIndex) +
          '[' +
          wildcardIndex +
          ']' +
          condField.substring(bracketIndex + 2);
      }
      const fullPath = prefix + condField;
      const actualValue = formData[fullPath];
      const normalizedActual = normalizeConditionValue(actualValue);
      const normalizedExpected = normalizeConditionValue(cond.value);

      if (normalizedActual === undefined || normalizedActual === null) {
        return normalizedExpected === null;
      }
      if (normalizedExpected === null) {
        return false;
      }

      // 실제 값이 배열(체크박스/멀티셀렉트)인 경우 포함 여부 확인
      if (Array.isArray(normalizedActual)) {
        return normalizedActual.some((v) => v === normalizedExpected);
      }

      // 단일 값 비교
      return normalizedActual === normalizedExpected;
    });
  });
};

/**
 * Normalizes condition values for consistent comparison (Boolean, String, etc.).
 * <p>
 * Boolean values are normalized to "true"/"false" (lowercase).
 * String values are trimmed and Boolean strings are normalized.
 * Other values are converted to string.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 조건 비교를 위해 Boolean, String 등의 값을 정규화합니다.
 * <p>
 * Boolean 값은 "true"/"false" (소문자)로 정규화되고,
 * String 값은 공백이 제거되며 Boolean 문자열은 소문자로 정규화됩니다.
 * 다른 타입은 문자열로 변환됩니다.
 * </p>
 *
 * @function normalizeConditionValue
 * @param {any} val - Value to normalize | 정규화할 값
 * @returns {string|null} Normalized value | 정규화된 값
 */
const normalizeConditionValue = (val) => {
  if (val === null || val === undefined) {
    return null;
  }

  // Boolean 처리: true -> "true", false -> "false"
  if (typeof val === 'boolean') {
    return val ? 'true' : 'false';
  }

  // String 처리: 공백 제거, Boolean 문자열 정규화
  if (typeof val === 'string') {
    const trimmed = val.trim();
    if (trimmed.toLowerCase() === 'true') {
      return 'true';
    }
    if (trimmed.toLowerCase() === 'false') {
      return 'false';
    }
    return trimmed;
  }

  // Number 및 기타: 문자열로 변환
  return String(val);
};

/**
 * Extracts field value by type (text, radio, checkbox, select, etc.).
 * <p>
 * For group fields (radio, checkbox), iterates through the entire element list to check state.
 * Returns single value for radio/text, array for checkboxes, or null for empty fields.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 필드 값 추출 (타입별: text, radio, checkbox, select 등).
 * <p>
 * 그룹 필드(radio, checkbox)의 경우 전체 엘리먼트 리스트를 순회하며 상태를 확인합니다.
 * 라디오/텍스트는 단일 값을, 체크박스는 배열을, 빈 필드는 null을 반환합니다.
 * </p>
 *
 * @function getFieldValue
 * @param {NodeList|Array|HTMLCollection} elements - Field elements | 필드 요소들
 * @returns {string|Array|null} Field value | 필드 값
 */
const getFieldValue = (elements) => {
  if (!elements || elements.length === 0) return null;

  // 그룹 내에 하나의 라디오 버튼이라도 있으면 전체를 라디오 그룹으로 처리
  const isRadio = Array.from(elements).some((el) => el.type?.toLowerCase() === 'radio');
  const isCheckbox =
    !isRadio && Array.from(elements).some((el) => el.type?.toLowerCase() === 'checkbox');

  if (isRadio) {
    for (const el of elements) {
      if (el.checked) return el.value;
    }
    return null;
  } else if (isCheckbox) {
    const values = [];
    for (const el of elements) {
      if (el.checked) values.push(el.value);
    }
    return values.length > 0 ? values : null;
  } else if (elements[0].type?.toLowerCase() === 'select-multiple') {
    const values = [];
    for (const option of elements[0].options) {
      if (option.selected) values.push(option.value);
    }
    return values;
  } else {
    // text, password, email, number, tel, select-one 등 일반 입력 필드
    const val = elements[0].value?.trim();
    return val !== undefined && val !== '' ? val : null;
  }
};

/**
 * Validates an individual check.
 * <p>
 * Executes validation logic based on rule type. Supports numeric comparison,
 * string length/pattern matching, date comparison, cross-field validation, and more.
 * Empty values pass all checks except REQUIRED, ASSERT_TRUE, and ASSERT_FALSE.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 개별 체크를 검증합니다.
 * <p>
 * 규칙 타입에 따라 검증 로직을 실행합니다. 숫자 비교, 문자열 길이/패턴 매칭,
 * 날짜 비교, 필드 간 교차 검증 등을 지원합니다.
 * 빈 값은 REQUIRED, ASSERT_TRUE, ASSERT_FALSE를 제외한 모든 검사를 통과합니다.
 * </p>
 *
 * @function validateCheck
 * @param {any} value - Field value | 필드 값
 * @param {Object} rule - Rule object {type, value, regex, message} | 규칙 객체 {type, value, regex, message}
 * @param {Object} formData - Complete form data map | 전체 폼 데이터 맵
 * @param {string} prefix - Field name prefix (for nested paths) | 필드명 접두사 (중첩 경로용)
 * @param {string} [fieldName] - Full field name, used only for the "unsupported rule type" diagnostic log | 전체 필드명, "미지원 규칙 타입" 진단 로그에만 사용
 * @returns {boolean} Validity status | 유효 여부
 */
const validateCheck = (value, rule, formData, prefix = '', fieldName = '') => {
  // ASSERT_TRUE, ASSERT_FALSE는 null이나 빈 값이어도 검증을 수행해야 함 (체크 안 된 상태를 잡아야 하므로)
  if (
    (value === null || value === '') &&
    rule.type !== 'ASSERT_TRUE' &&
    rule.type !== 'ASSERT_FALSE'
  ) {
    return rule.type !== 'REQUIRED'; // REQUIRED만 실패, 나머지 empty 무시
  }

  // ASSERT_TRUE, ASSERT_FALSE는 단일 값(체크박스 하나 등)에 대해서만 유효함
  // 여러 개가 선택된 경우(배열 길이가 1보다 큰 경우)는 불리언 판단이 부적절하므로 실패(false) 처리
  if (
    Array.isArray(value) &&
    value.length > 1 &&
    (rule.type === 'ASSERT_TRUE' || rule.type === 'ASSERT_FALSE')
  ) {
    return false;
  }

  // 배열인 경우 (체크박스 그룹 등) 첫 번째 값을 기준으로 판단
  const firstValue = Array.isArray(value) ? value[0] : value;

  switch (rule.type) {
    case 'REQUIRED':
      // 서버(S2Util.isEmpty)와 동일하게 숫자 0과 boolean false는 "값 있음"으로 취급함(단순 truthy
      // 판단(!!value)은 0/false를 falsy로 봐서 additionalData로 원시값이 들어오면 오판했었음) |
      // Matches the server (S2Util.isEmpty): numeric 0 and boolean false count as "present", unlike
      // a plain truthy check (!!value) which treats them as falsy and misjudges raw additionalData values.
      if (Array.isArray(value)) return value.length > 0;
      if (typeof value === 'string') return value.trim().length > 0;
      return value !== null && value !== undefined;
    case 'ASSERT_TRUE':
      // 체크박스는 'on' 또는 'true' (문자열/불리언) 일 때 통과
      return firstValue === true || firstValue === 'true' || firstValue === 'on';
    case 'ASSERT_FALSE':
      // 체크박스가 체크되지 않았거나 (null/undefined), 명시적 false일 때 통과
      return (
        firstValue === false ||
        firstValue === 'false' ||
        firstValue === null ||
        firstValue === undefined
      );
    case 'LENGTH':
      return String(value).length === parseInt(rule.value);
    case 'MIN_LENGTH':
      return String(value).length >= parseInt(rule.value);
    case 'MAX_LENGTH':
      return String(value).length <= parseInt(rule.value);
    case 'MIN_BYTE':
      return new Blob([String(value)]).size >= parseInt(rule.value);
    case 'MAX_BYTE':
      return new Blob([String(value)]).size <= parseInt(rule.value);
    case 'MIN_VALUE':
      return parseFloat(value) >= parseFloat(rule.value);
    case 'MAX_VALUE':
      return parseFloat(value) <= parseFloat(rule.value);
    case 'REGEX':
    case 'NUMBER':
    case 'TEXT_INTACT':
    case 'TEXT_COMBINE':
    case 'MPHONE_NO':
    case 'TEL_NO':
    case 'INTERNATIONAL_TEL_NO':
    case 'EMAIL':
    case 'ZIP':
    case 'LOGIN_ID':
    case 'PASSWORD':
    case 'PASSWORD_ANSWR':
    case 'BIZRNO':
    case 'NWINO': {
      const regex = rule.regex || rule.value; // REGEX는 value, 나머지 regex
      return new RegExp(regex).test(String(value));
    }
    case 'JUMIN':
      return validateJumin(String(value)); // 서버 로직 복제
    case 'DATE':
      return validateDate(value); // 문자열/날짜 객체 지원
    case 'DATE_AFTER': {
      const afterValue = formData[prefix + rule.value];
      if (!afterValue) return true;
      const date1 = parseDate(value);
      const date2 = parseDate(afterValue);
      return date1 && date2 && date1 >= date2;
    }
    case 'DATE_BEFORE': {
      const beforeValue = formData[prefix + rule.value];
      if (!beforeValue) return true;
      const date3 = parseDate(value);
      const date4 = parseDate(beforeValue);
      return date3 && date4 && date3 <= date4;
    }
    case 'EQUALS_FIELD': {
      const eqValue = formData[prefix + rule.value];
      return (
        value === eqValue ||
        (Array.isArray(value) &&
          Array.isArray(eqValue) &&
          value.sort().join(',') === eqValue.sort().join(','))
      );
    }
    default:
      // 모르는 규칙 타입은 무조건 통과(fail-open)시키지 않고 실패 처리함 — 조용히 통과되면 서버가
      // 거부하는 값이 클라이언트에서만 통과된 것처럼 보여서 더 위험함. 대신 개발자가 바로 원인을 알고
      // s2.validator.js에 케이스를 추가할 수 있도록 console.error로 필드명/타입을 명확히 남긴다. |
      // Unknown rule types now fail closed instead of silently passing (fail-open) — a silent pass would
      // let data through client-side that the server rejects. console.error surfaces the exact field and
      // rule type so a developer can immediately add the missing case to s2.validator.js.
      console.error(
        `[S2Validator] 지원하지 않는 규칙 타입입니다: "${rule.type}" (필드: "${fieldName}"). ` +
          's2.validator.js의 validateCheck()에 해당 case를 추가해야 클라이언트 검증이 정상 동작합니다.'
      );
      return false;
  }
};

/**
 * JUMIN (Resident Registration Number) validation (replicates server logic).
 * <p>
 * Validates Korean resident registration numbers using checksum algorithm.
 * Supports both Korean citizens (flag < 5 or > 8) and foreigners.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * JUMIN 검증 (서버 로직 복제).
 * <p>
 * 체크섬 알고리즘을 사용하여 주민등록번호의 유효성을 검증합니다.
 * 내국인(flag < 5 또는 > 8)과 외국인 모두 지원합니다.
 * </p>
 *
 * @function validateJumin
 * @param {string} jumin - Resident registration number string | 주민번호 문자열
 * @returns {boolean} Validity status | 유효 여부
 */
const validateJumin = (jumin) => {
  jumin = jumin.replace(/-/g, '');
  if (jumin.length !== 13) return false;

  const flag = parseInt(jumin.charAt(6));
  const isKorean = flag < 5 || flag > 8;
  let check = 0;

  for (let i = 0; i < 12; i++) {
    if (isKorean) {
      check += ((i % 8) + 2) * parseInt(jumin.charAt(i));
    } else {
      check += (9 - (i % 8)) * parseInt(jumin.charAt(i));
    }
  }

  if (isKorean) {
    check = 11 - (check % 11);
    check %= 10;
  } else {
    const remainder = check % 11;
    if (remainder === 0) check = 1;
    else if (remainder === 10) check = 0;
    else check = remainder;

    const check2 = check + 2;
    check = check2 > 9 ? check2 - 10 : check2;
  }

  return check === parseInt(jumin.charAt(12));
};

/**
 * DATE validation (string yyyyMMdd or yyyy-MM-dd, or Date object).
 * <p>
 * Validates date format and ensures the year is within acceptable range
 * (not more than 100 years in the past). Supports both string and Date object inputs.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * DATE 검증 (문자열 yyyyMMdd or yyyy-MM-dd, or Date 객체).
 * <p>
 * 날짜 형식을 검증하고 연도가 허용 가능한 범위 내에 있는지 확인합니다
 * (과거 100년 이내). 문자열과 Date 객체 입력을 모두 지원합니다.
 * </p>
 *
 * @function validateDate
 * @param {string|Date} value - Value to validate | 검증 값
 * @returns {boolean} Validity status | 유효 여부
 */
const validateDate = (value) => {
  if (typeof value === 'string') {
    value = value.replace(/[-.]/g, '');
    if (value.length !== 8) return false;
    const year = parseInt(value.substring(0, 4));
    const month = parseInt(value.substring(4, 6));
    const day = parseInt(value.substring(6, 8));
    if (isNaN(year) || isNaN(month) || isNaN(day)) return false;
    if (year < new Date().getFullYear() - 100) return false;
    // JS Date는 존재하지 않는 날짜(2월 30일 등)를 예외 없이 다음 날짜로 굴려버리므로, 만든 Date를
    // 되짚어 입력값과 일치하는지 확인해야 서버(LocalDate.of)와 동일하게 걸러낼 수 있다 | JS Date silently
    // rolls invalid dates over (e.g. Feb 30 -> Mar 2) instead of throwing, so the constructed Date must
    // be checked back against the input to reject them the same way the server's LocalDate.of() does.
    const parsed = new Date(year, month - 1, day);
    return (
      parsed.getFullYear() === year && parsed.getMonth() === month - 1 && parsed.getDate() === day
    );
  } else if (value instanceof Date) {
    const year = value.getFullYear();
    return year >= new Date().getFullYear() - 100;
  }
  return false;
};

/**
 * Date parsing (yyyy-MM-dd or yyyyMMdd to Date).
 * <p>
 * Converts string date formats or Date objects into standardized Date objects.
 * Returns null if the input cannot be parsed.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 날짜 파싱 (yyyy-MM-dd or yyyyMMdd to Date).
 * <p>
 * 문자열 날짜 형식 또는 Date 객체를 표준화된 Date 객체로 변환합니다.
 * 입력을 파싱할 수 없는 경우 null을 반환합니다.
 * </p>
 *
 * @function parseDate
 * @param {string|Date} value - Value to parse | 파싱 값
 * @returns {Date|null} Date object or null | Date 객체 or null
 */
const parseDate = (value) => {
  if (typeof value === 'string') {
    value = value.replace(/[-.]/g, '');
    if (value.length !== 8) return null;
    const year = parseInt(value.substring(0, 4));
    const month = parseInt(value.substring(4, 6));
    const day = parseInt(value.substring(6, 8));
    if (isNaN(year) || isNaN(month) || isNaN(day)) return null;
    // validateDate()와 동일하게 굴러간(rolled-over) 날짜는 null로 걸러냄
    const parsed = new Date(year, month - 1, day);
    if (
      parsed.getFullYear() !== year ||
      parsed.getMonth() !== month - 1 ||
      parsed.getDate() !== day
    )
      return null;
    return parsed;
  } else if (value instanceof Date) {
    return value;
  }
  return null;
};

/**
 * Escapes special characters for use in a regular expression.
 * <p>
 * Ensures that characters like ., *, +, ?, ^, $, {, }, (, ), |, [, ], and \ are treated as literals.
 * </p>
 *
 * @function escapeRegExp
 * @param {string} string - The string to escape
 * @returns {string} The escaped string
 */
const escapeRegExp = (string) => {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // $& means the whole matched string
};

// 초기화 상태 관리용 (중복 리스너 등록 방지)
let isS2ValidatorInitialized = false;

// 라이브러리 로드 시 기본 초기화
initS2Validator();
