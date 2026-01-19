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
 * // Import only - applies to all forms with data-s2-rules
 * // 임포트만 하면 data-s2-rules가 있는 모든 폼에 적용된다.
 * import '/s2-util/js/s2.validator.js';
 *
 * or With Thymeleaf
 *
 * const contextPath = \/*[[@{/}]]*\/ '';
 * import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);
 *
 * <form id="myForm" th:data-s2-rules="${rules}">
 *   ...
 * </form>
 */
export const initS2Validator = () => {
  if (isS2ValidatorInitialized) return;

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
   *
   * @function validate
   * @param {string|HTMLFormElement} formSource - Form element selector or HTMLFormElement object | 검증할 폼 요소의 셀렉터 또는 HTMLFormElement 객체
   * @param {string|Object} [rulesSource] - Validation rules (JSON/Object). If omitted, searches element's data-s2-rules | 검증 규칙(JSON/Object). 생략 시 엘리먼트의 data-s2-rules를 탐색
   * @returns {Object} Error object {fieldName: [errorMessages]} – empty object if valid | 에러 객체 {fieldName: [errorMessages]} – 빈 객체 시 유효
   * @example
   * import { S2Validator } from '/s2-util/js/s2.validator.js';
   *
   * or With Thymeleaf
   *
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
   */
  validate(formSource, rulesSource) {
    const form = typeof formSource === 'string' ? document.querySelector(formSource) : formSource;
    if (!form || !(form instanceof HTMLFormElement)) {
      return { __system_error__: ['유효한 폼 요소를 찾을 수 없습니다.'] };
    }

    // 검증 전 모든 폼 엘리먼트의 CustomValidity 초기화 및 자동 초기화 이벤트 등록
    Array.from(form.elements).forEach((el) => {
      if (typeof el.setCustomValidity === 'function') {
        el.setCustomValidity('');

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
      return { __system_error__: ['검증 규칙 데이터(JSON) 형식이 올바르지 않거나 존재하지 않습니다.'] };
    }

    const errors = {};
    const formData = getFormData(form); // 전체 필드 값 맵 (cross-field용)
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
            // 조건 체크
            if (!isConditionSatisfied(rule, formData, prefix)) return;

            const fullPath = prefix + rule.name;
            // "products[].name" -> ".name" 추출
            const bracketIndex = fullPath.indexOf('[]');
            const suffix = fullPath.substring(bracketIndex + 2);

            // 실제 필드명: products[0].name
            const actualFieldName = collectionPrefix + '[' + idx + ']' + suffix;
            const fieldElements = form.querySelectorAll(`[name="${actualFieldName}"]`);
            if (fieldElements.length === 0) return;

            const value = getFieldValue(fieldElements);
            const fieldErrors = [];

            rule.rules.forEach((check) => {
              // 와일드카드에서는 NESTED/EACH 지원 안 함 (이미 서브 validator로 처리 가능)
              if (check.type === 'NESTED' || check.type === 'EACH') return;

              if (!validateCheck(value, check, formData, prefix)) {
                fieldErrors.push(check.message);
              }
            });

            if (fieldErrors.length > 0) {
              errors[actualFieldName] = fieldErrors;
              // 브라우저 네이티브 검증 UI 연동을 위해 첫 번째 에러 메시지 설정
              const firstMessage = fieldErrors[0];
              fieldElements.forEach((el) => {
                if (typeof el.setCustomValidity === 'function') {
                  el.setCustomValidity(firstMessage);
                }
              });
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
            if (fieldElements.length === 0) return;

            const value = getFieldValue(fieldElements);
            if (!validateCheck(value, check, formData, prefix)) {
              fieldErrors.push(check.message);
            }
          }
        });

        if (fieldErrors.length > 0) {
          errors[fullPath] = fieldErrors;
          // 브라우저 네이티브 검증 UI 연동을 위해 첫 번째 에러 메시지 설정
          const firstMessage = fieldErrors[0];
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
      });
    };

    validateRules(rules);

    // 에러 발생 시 브라우저 에러 메시지 즉시 표시 (HTML5 Validation 연동)
    if (Object.keys(errors).length > 0) {
      try {
        form.reportValidity();
      } catch (e) {
        console.warn('S2Validator: 브라우저가 에러 메시지를 표시할 수 없습니다. 비표시 필드 설정을 확인하세요.', e);
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
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 규칙에 설정된 조건(conditions)을 현재 폼 데이터와 비교하여 만족 여부를 판단합니다.
 * <p>
 * 배열(체크박스 등) 데이터일 경우 포함 여부를 확인하며, 단일 값은 일치 여부를 확인합니다.
 * 조건 그룹 간에는 OR 로직을, 각 그룹 내에서는 AND 로직을 적용합니다.
 * </p>
 *
 * @param {Object} rule - Validation rule object | 검증 규칙 객체
 * @param {Object} formData - Current form data object | 현재 폼의 데이터 객체
 * @param {string} prefix - Field name prefix (for nested paths) | 필드명 접두사 (중첩 경로용)
 * @returns {boolean} True if conditions are satisfied or no conditions exist, false otherwise | 조건을 만족하거나 조건이 없으면 true, 만족하지 않으면 false
 */
const isConditionSatisfied = (rule, formData, prefix = '') => {
  // 조건이 없으면 항상 검증 수행
  if (!rule.conditions || rule.conditions.length === 0) return true;

  // OR 연산: 하나라도 만족하는 그룹(AND 그룹)이 있으면 true
  return rule.conditions.some((group) => {
    // AND 연산: 그룹 내 모든 조건이 일치해야 함
    return group.every((cond) => {
      const fullPath = prefix + cond.field;
      const actualValue = formData[fullPath];
      const expectedValue = String(cond.value);

      if (actualValue === undefined || actualValue === null) {
        return cond.value === null;
      }
      if (cond.value === null) {
        return false;
      }

      // 실제 값이 배열(체크박스/멀티셀렉트)인 경우 포함 여부 확인
      if (Array.isArray(actualValue)) {
        return actualValue.some((v) => String(v) === expectedValue);
      }

      // 단일 값 비교
      return String(actualValue) === expectedValue;
    });
  });
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
  const isCheckbox = !isRadio && Array.from(elements).some((el) => el.type?.toLowerCase() === 'checkbox');

  if (isRadio) {
    for (let el of elements) {
      if (el.checked) return el.value;
    }
    return null;
  } else if (isCheckbox) {
    const values = [];
    for (let el of elements) {
      if (el.checked) values.push(el.value);
    }
    return values.length > 0 ? values : null;
  } else if (elements[0].type?.toLowerCase() === 'select-multiple') {
    const values = [];
    for (let option of elements[0].options) {
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
 * @returns {boolean} Validity status | 유효 여부
 */
const validateCheck = (value, rule, formData, prefix = '') => {
  // ASSERT_TRUE, ASSERT_FALSE는 null이나 빈 값이어도 검증을 수행해야 함 (체크 안 된 상태를 잡아야 하므로)
  if ((value === null || value === '') && rule.type !== 'ASSERT_TRUE' && rule.type !== 'ASSERT_FALSE') {
    return rule.type !== 'REQUIRED'; // REQUIRED만 실패, 나머지 empty 무시
  }

  // ASSERT_TRUE, ASSERT_FALSE는 단일 값(체크박스 하나 등)에 대해서만 유효함
  // 여러 개가 선택된 경우(배열 길이가 1보다 큰 경우)는 불리언 판단이 부적절하므로 실패(false) 처리
  if (Array.isArray(value) && value.length > 1 && (rule.type === 'ASSERT_TRUE' || rule.type === 'ASSERT_FALSE')) {
    return false;
  }

  // 배열인 경우 (체크박스 그룹 등) 첫 번째 값을 기준으로 판단
  const firstValue = Array.isArray(value) ? value[0] : value;

  switch (rule.type) {
    case 'REQUIRED':
      return !!value && (Array.isArray(value) ? value.length > 0 : true);
    case 'ASSERT_TRUE':
      // 체크박스는 'on' 또는 'true' (문자열/불리언) 일 때 통과
      return firstValue === true || firstValue === 'true' || firstValue === 'on';
    case 'ASSERT_FALSE':
      // 체크박스가 체크되지 않았거나 (null/undefined), 명시적 false일 때 통과
      return firstValue === false || firstValue === 'false' || firstValue === null || firstValue === undefined;
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
      return value === eqValue || (Array.isArray(value) && Array.isArray(eqValue) && value.sort().join(',') === eqValue.sort().join(','));
    }
    default:
      console.warn('Unknown check type:', rule.type);
      return true;
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
    let remainder = check % 11;
    if (remainder === 0) check = 1;
    else if (remainder === 10) check = 0;
    else check = remainder;

    let check2 = check + 2;
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
    try {
      new Date(year, month - 1, day);
      return true;
    } catch {
      return false;
    }
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
    const month = parseInt(value.substring(4, 6)) - 1;
    const day = parseInt(value.substring(6, 8));
    return new Date(year, month, day);
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
