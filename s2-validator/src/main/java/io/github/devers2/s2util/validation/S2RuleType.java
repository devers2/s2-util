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

import java.util.Locale;

import io.github.devers2.s2util.core.S2Util;

/**
 * Defined catalog of built-in validation rule types.
 * <p>
 * This enum acts as a specification for various validation scenarios. Each constant
 * defines a unique error message key, default templates for multi-language support,
 * and optional regex patterns for string-based validation.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Validator에서 사용할 수 있는 내장 검증 규칙 항목들을 정의한 열거형 클래스입니다.
 * <p>
 * 각 항목은 고유한 에러 메시지 키, 다국어 지원을 위한 기본 템플릿, 그리고 정규식 기반 검증에 필요한
 * 패턴 정보를 포함하고 있습니다.
 * </p>
 *
 * <h3>Message Placeholders (메시지 플레이스홀더)</h3>
 * The templates use numbering placeholders which are substituted during error reporting:
 * <ul>
 * <li><b>{0}:</b> The label of the field being validated (e.g., "User ID").</li>
 * <li><b>{1}:</b> The criterion value (e.g., minimum length, max value, or regex pattern).</li>
 * <li><b>{0|은/는}:</b> Automated Korean postposition (Josa) selection based on {0}.</li>
 * </ul>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 */
public enum S2RuleType {

    /** 필수 입력 체크 ({0}: 필드설명) */
    REQUIRED("valid.err.required", "{0|은/는} 필수 입력 항목입니다.", "{0} is required.", null),

    /** 값이 true 인지 검증 (Boolean 타입) ({0}: 필드설명) */
    ASSERT_TRUE("valid.err.asserttrue", "{0|은/는} 반드시 true여야 합니다.", "{0} must be true.", null),

    /** 값이 false 인지 검증 (Boolean 타입) ({0}: 필드설명) */
    ASSERT_FALSE("valid.err.assertfalse", "{0|은/는} 반드시 false여야 합니다.", "{0} must be false.", null),

    /** 길이 체크 ({0}: 필드설명, {1}: 기준값) */
    LENGTH("valid.err.length", "{0|은/는} {1}자를 입력해야 합니다.", "{0} must be {1} characters.", null),

    /** 최소 길이 체크 ({0}: 필드설명, {1}: 기준값) */
    MIN_LENGTH("valid.err.minlength", "{0|은/는} 최소 {1}자 이상 입력해야 합니다.", "{0} must be at least {1} characters.", null),

    /** 최대 길이 체크 ({0}: 필드설명, {1}: 기준값) */
    MAX_LENGTH("valid.err.maxlength", "{0|은/는} 최대 {1}자를 초과할 수 없습니다.", "{0} cannot exceed {1} characters.", null),

    /** 최소 바이트 체크 ({0}: 필드설명, {1}: 기준값) */
    MIN_BYTE("valid.err.minbyte", "{0|은/는} 최소 {1}바이트 이상 입력해야 합니다.", "{0} must be at least {1} bytes.", null),

    /** 최대 바이트 체크 ({0}: 필드설명, {1}: 기준값) */
    MAX_BYTE("valid.err.maxbyte", "{0|은/는} 최대 {1}바이트를 초과할 수 없습니다.", "{0} cannot exceed {1} bytes.", null),

    /** 최소 값 체크 ({0}: 필드설명, {1}: 기준값) */
    MIN_VALUE("valid.err.minvalue", "{0|은/는} 최소 {1} 이상 입력해야 합니다.", "{0} must be at least {1}.", null),

    /** 최대 값 체크 ({0}: 필드설명, {1}: 기준값) */
    MAX_VALUE("valid.err.maxvalue", "{0|은/는} 최대 {1} 이하 입력해야 합니다.", "{0} must be at most {1}.", null),

    /** 날짜 형식 체크 (yyyyMMdd 형식) ({0}: 필드설명) */
    DATE("valid.err.date", "{0|은/는} 날짜 형식이 올바르지 않습니다.", "Invalid date format for {0}.", null),

    /** 날짜 선후 관계 확인 (기준일 이후) {0}: 대상필드명, {1}: 기준필드설명 */
    DATE_AFTER("valid.err.dateafter", "{0|은/는} {1}보다 이전일 수 없습니다.", "{0} cannot be earlier than {1}.", null),

    /** 날짜 선후 관계 확인 (기준일 이전) {0}: 대상필드명, {1}: 기준필드설명 */
    DATE_BEFORE("valid.err.datebefore", "{0|은/는} {1}보다 이후일 수 없습니다.", "{0} cannot be later than {1}.", null),

    /** 필드 값 동등 확인 ({0}: 대상필드명, {1}: 기준필드설명) */
    EQUALS_FIELD("valid.err.equalsfield", "{0|와/과} {1|이/가} 일치하지 않습니다.", "{0} must match {1}.", null),

    /** 정규식 체크 ({0}: 필드설명, {1}: 정규식) */
    REGEX("valid.err.regex", "{0|은/는} 형식이 올바르지 않습니다.", "Invalid format for {0}.", null),

    /** 숫자 형식 체크 ({0}: 필드설명) */
    NUMBER("valid.err.number", "{0|은/는} 숫자만 입력 가능합니다.", "{0} must be a number.", "^[0-9]*$"),

    /** 텍스트 형식 체크 (숫자/특문 제외) ({0}: 필드설명) */
    TEXT_INTACT("valid.err.textintact", "{0|은/는} 텍스트만 입력 가능합니다.", "{0} must contain only text.", "^[가-힣a-zA-Z]*$"),

    /** 텍스트 형식 체크 (특문 제외) ^[가-힣a-zA-Z0-9\s\-\(\)\,\.]*$ */
    TEXT_COMBINE("valid.err.textcombine", "{0|은/는} 텍스트만 입력 가능합니다.", "{0} must contain only text and numbers.", "^[가-힣a-zA-Z0-9\\s\\-\\(\\)\\,\\.]*$"),

    /** 휴대폰 번호 형식 체크 ({0}: 필드설명) */
    MPHONE_NO("valid.err.mphone", "{0|은/는} 휴대폰 번호 형식이 올바르지 않습니다.", "Invalid mobile phone format for {0}.", "^01[016789]-\\d{3,4}-\\d{4}$"),

    /** 전화번호 형식 체크 ({0}: 필드설명) */
    TEL_NO("valid.err.telno", "{0|은/는} 전화번호 형식이 올바르지 않습니다.", "Invalid phone number format for {0}.", "^(\\d{2,3}-\\d{3,4}-\\d{4}|\\d{10,11})$"),

    /** 국제 전화번호 형식 체크 - E.164 표준 기반 ({0}: 필드설명) */
    INTERNATIONAL_TEL_NO("valid.err.intltelno", "{0|은/는} 전화번호 형식이 올바르지 않습니다.", "Invalid international phone format for {0}. (e.g., +1 123-456-7890)", "^\\+(?:[0-9] ?){6,14}[0-9]$"),

    /** 이메일 형식 체크 ({0}: 필드설명) */
    EMAIL("valid.err.email", "{0} 형식이 올바르지 않습니다.", "Invalid email format for {0}.", "^[a-zA-Z0-9_%+-]+(?:\\.[a-zA-Z0-9_%+-]+)*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$"),

    /** 우편번호 형식 체크 ({0}: 필드설명) */
    ZIP("valid.err.zip", "{0} 형식이 올바르지 않습니다.", "Invalid zip code format for {0}.", "^[0-9]{5}$"),

    /** 로그인ID 형식 체크 ({0}: 필드설명) */
    LOGIN_ID("valid.err.loginid", "{0} 형식이 올바르지 않습니다.", "Invalid Login ID format for {0}.", "^[a-zA-Z][a-zA-Z0-9._-]{4,19}$"), // 5~20자리

    /** 비밀번호 형식 체크 ({0}: 필드설명) */
    PASSWORD("valid.err.password", "{0} 형식이 올바르지 않습니다.", "Invalid password format for {0}.", "^(?=.*[0-9])(?=.*[!@#$%^&*])(?=.*[a-zA-Z]).{9,32}$"), // 9~32자리

    /** 비밀번호 찾기 질문 답변 형식 체크 ({0}: 필드설명) */
    PASSWORD_ANSWR("valid.err.passwordanswr", "{0} 형식이 올바르지 않습니다.", "Invalid password answer format for {0}.", "^[가-힣a-zA-Z0-9\\s]{10,80}$"), // 10~80자리

    /** 사업자등록번호 형식 체크 ({0}: 필드설명) */
    BIZRNO("valid.err.bizrno", "{0} 형식이 올바르지 않습니다.", "Invalid business registration number for {0}.", "^\\d{3}-\\d{2}-\\d{5}$|^\\d{10}$"), // 000-00-00000 또는 0000000000 모두 허용

    /** 사업장관리번호 형식 체크 ({0}: 필드설명) */
    NWINO("valid.err.nwino", "{0} 형식이 올바르지 않습니다.", "Invalid workplace management number for {0}.", "^\\d{3}-\\d{2}-\\d{5}-\\d{1}$|^\\d{11}$"), // 000-00-00000-0 또는 00000000000 모두 허용

    /** 주민번호/외국인번호 형식 체크 ({0}: 필드설명) */
    JUMIN("valid.err.jumin", "{0|은/는} 주민번호 형식이 올바르지 않습니다.", "Invalid resident registration number for {0}.", null),

    /**
     * 중첩 객체 검증 ({0}: 필드설명)
     *
     * @implNote
     *
     *           <pre>{@code
     * // 하위(자식) 검증기 정의
     * S2Validator<Address> addressValidator = S2Validator.<Address>builder()
     *     .field("zipCode", "우편번호").rule(REQUIRED).rule(NUMBER)
     *     .field("city", "도시").rule(REQUIRED)
     *     .build();
     *
     * // 상위(부모) 검증기에서 NESTED 사용
     * S2Validator<Order> orderValidator = S2Validator.<Order>builder()
     *     .field("orderId", "주문번호").rule(REQUIRED)
     *     .field("shippingAddress", "배송처")
     *         .rule(S2RuleType.NESTED, addressValidator) // 중첩 객체 검증!
     *     .build();
     * }</pre>
     */
    NESTED("valid.err.nested", "{0|은/는} 하위 유효성 검증에 실패했습니다.", "{0} failed nested validation.", null),

    /**
     * 리스트/배열의 각 요소 검증 ({0}: 필드설명)
     *
     * @implNote
     *
     *           <pre>{@code
     * // 하위(자식) 검증기 정의
     * S2Validator<File> fileValidator = S2Validator.<File>builder()
     *     .field("fileName", "파일명").rule(REQUIRED)
     *     .field("fileSize", "파일크기").rule(REQUIRED).rule(MIN_VALUE, 0)
     *     .build();
     *
     * // 상위(부모) 검증기에서 EACH 사용
     * S2Validator<Product> productValidator = S2Validator.<Product>builder()
     *     .field("productId", "상품ID").rule(REQUIRED)
     *     .field("productName", "상품명").rule(REQUIRED)
     *     .field("files", "첨부파일")
     *         .rule(S2RuleType.EACH, fileValidator) // 리스트/배열의 각 요소 검증!
     *     .build();
     * }</pre>
     */
    EACH("valid.err.each", "{0|의/의} 요소가 유효성 검증에 실패했습니다.", "One or more elements in {0} failed validation.", null);

    /**
     * Error message property key.
     * <p>
     * 에러 메시지 프로퍼티 키입니다.
     */
    private final String errorMessageKey;

    /**
     * Default Korean message template.
     * <p>
     * 기본 한국어 메시지 템플릿입니다.
     */
    private final String errorMessageTemplateKo;

    /**
     * Default English message template.
     * <p>
     * 기본 영어 메시지 템플릿입니다.
     */
    private final String errorMessageTemplateEn;

    /**
     * Built-in regex pattern for the rule (if applicable).
     * <p>
     * 규칙에 정의된 내장 정규식 패턴입니다 (해당하는 경우).
     */
    private final String regex;

    S2RuleType(String messageKey, String errorMessageTemplateKo, String errorMessageTemplateEn, String regex) {
        this.errorMessageKey = messageKey;
        this.errorMessageTemplateKo = errorMessageTemplateKo;
        this.errorMessageTemplateEn = errorMessageTemplateEn;
        this.regex = regex;
    }

    /**
     * Returns the error message property key.
     *
     * @return The message key (e.g., "valid.err.required") | 에러 메시지 프로퍼티 키 (예: "valid.err.required")
     */
    public String getErrorMessageKey() {
        return errorMessageKey;
    }

    /**
     * Returns the appropriate error message template for the given locale.
     * <p>
     * If the requested locale is not Korean, it defaults to the English template
     * if available; otherwise, it falls back to the Korean template.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 현재 로케일에 가장 적합한 기본 에러 메시지 템플릿을 반환합니다.
     * <p>
     * 요청된 로케일이 비한국어권일 경우 우선적으로 영문 템플릿을 탐색하며, 없을 경우 국문 템플릿을 반환합니다.
     * </p>
     *
     * @param locale The user's current locale | 사용자의 현재 로케일
     * @return The message template string | 메시지 템플릿 문자열
     */
    public String getErrorMessageTemplate(Locale locale) {
        return !S2Util.isKorean(locale) && errorMessageTemplateEn != null && !errorMessageTemplateEn.isBlank() ? errorMessageTemplateEn : errorMessageTemplateKo;
    }

    /**
     * Returns the regex pattern associated with this rule type.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 이 검증 규칙에 정의된 기본 정규식 패턴 문자열을 반환합니다.
     *
     * @return The regex string, or {@code null} if not applicable | 정규식 문자열 (없을 경우 null)
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Checks whether this rule type includes a built-in regex pattern.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 이 규칙 타입이 정규식 패턴을 포함하고 있는지 여부를 반환합니다.
     *
     * @return {@code true} if a regex is defined, {@code false} otherwise | 정규식이 정의되어 있으면 true
     */
    public boolean hasRegex() {
        return regex != null;
    }

}
