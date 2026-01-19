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

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.github.devers2.s2util.core.S2Cache;
import io.github.devers2.s2util.core.S2DateUtil;
import io.github.devers2.s2util.core.S2StringUtil;
import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.message.S2ResourceBundle;

/**
 * Represents a single evaluation rule for field validation.
 * <p>
 * This class encapsulates the validation logic for a specific {@link S2RuleType}.
 * It stores the criterion (check value) and provides customized error message
 * mapping per language.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * 필드별 개별 검증 규칙을 관리하는 객체입니다.
 * <p>
 * 특정 {@link S2RuleType}에 대한 구체적인 검증 로직을 포함합니다. 검증의 기준이 되는 값({@code checkValue})을
 * 저장하며, 언어별로 사용자 정의 에러 메시지를 설정할 수 있는 기능을 제공합니다.
 * </p>
 *
 * <h3>Message Resolution Priority (메시지 결정 우선순위)</h3>
 * When a validation failure occurs, the error message is resolved in the following order:
 * <ol>
 * <li><b>Global Bundle:</b> If {@link S2Validator#setValidationBundle(String)} is set and contains the key.</li>
 * <li><b>Local Template:</b> If {@link #storeMessage(String, String)} was called for the current locale.</li>
 * <li><b>Default Template:</b> If {@link #storeMessage(String, String)} was called for the default system locale.</li>
 * <li><b>System Default:</b> The built-in template defined in {@link S2RuleType}.</li>
 * </ol>
 *
 * @author devers2
 * @version 1.5
 * @since 1.0
 * @see S2RuleType
 * @see S2Field
 */
public class S2Rule implements S2RuleMessageStep, Serializable {

    private static final long serialVersionUID = 5429183746201827364L;

    /** Validation rule type identifier */
    private final S2RuleType ruleType;
    /** The evaluation criterion (e.g., min/max length, regex pattern) */
    private final Object checkValue;
    /** Error message templates mapped by language (e.g., "ko", "en") */
    private final Map<String, String> messageTemplates = new HashMap<>();
    /** Custom property key for localized error message resolution */
    private String errorMessageKey;

    /** Static singleton instance for mandatory input checks (reused to reduce GC) */
    private static final S2Rule REQUIRED = new S2Rule(S2RuleType.REQUIRED);

    /**
     * Constructs a new rule with the specified type.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 검증 타입으로 새로운 규칙을 생성합니다.
     *
     * @param ruleType The type of validation (e.g., REQUIRED, EMAIL) | 검증 타입 (예: REQUIRED, EMAIL)
     */
    public S2Rule(S2RuleType ruleType) {
        this(ruleType, null, null);
    }

    /**
     * Constructs a new rule with the specified type and criterion.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 지정된 검증 타입과 기준값을 사용하여 새로운 규칙을 생성합니다.
     *
     * @param ruleType   The type of validation | 검증 타입
     * @param checkValue The criterion value (e.g., regular expression, length) | 검증 기준값 (예: 정규식, 길이 등)
     */
    public S2Rule(S2RuleType ruleType, Object checkValue) {
        this(ruleType, checkValue, null);
    }

    /**
     * Constructs a new rule with type, criterion, and custom message key.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증 타입, 기준값, 그리고 개별적으로 재정의할 에러 메시지 키를 사용하여 규칙을 생성합니다.
     *
     * @param ruleType        The type of validation | 검증 타입
     * @param checkValue      The criterion value | 검증 기준값
     * @param errorMessageKey Custom property key for error message lookup | 에러 메시지 조회를 위한 커스텀 프로퍼티 키
     */
    public S2Rule(S2RuleType ruleType, Object checkValue, String errorMessageKey) {
        if (ruleType == null) {
            throw new IllegalArgumentException("[S2Rule] ruleType cannot be null.");
        }
        if ((ruleType == S2RuleType.REGEX || (ruleType.getErrorMessageTemplate(null) != null && ruleType.getErrorMessageTemplate(null).contains("{1}"))) && S2Util.isEmpty(checkValue)) {
            throw new IllegalArgumentException("[S2Rule] checkValue cannot be null.");
        }

        this.ruleType = ruleType;
        this.checkValue = checkValue;
        // Use custom key if provided; otherwise fallback to the default key for the rule type
        this.errorMessageKey = errorMessageKey != null && !errorMessageKey.isBlank() ? errorMessageKey : ruleType.getErrorMessageKey();
    }

    /**
     * Stores a custom message template for a specific language.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 특정 언어와 그에 해당하는 에러 메시지 템플릿을 저장합니다.
     *
     * @param language Language code (e.g., "ko", "en") | 언어 코드 (예: "ko", "en")
     * @param template Message template (e.g., "{0} is required.") | 메시지 템플릿 (예: "{0}은(는) 필수입니다.")
     * @return This rule instance for chaining | 체이닝을 위한 현재 규칙 인스턴스
     */
    @Override
    public S2RuleMessageStep storeMessage(String language, String template) {
        if (language != null && !language.isBlank() && template != null) {
            this.messageTemplates.put(language, template);
        }
        return this;
    }

    /**
     * Returns a singleton {@code REQUIRED} rule instance.
     * <p>
     * <b>Performance Tip:</b> Since "Required" check is the most frequent operation,
     * this method returns a pre-allocated static instance to minimize GC overhead.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 필수 입력 검증을 위한 정적 {@code S2Rule} 인스턴스를 반환합니다.
     * <p>
     * 빈번하게 발생하는 필수 체크 작업 시 객체 생성 오버헤드를 제거하기 위해 정적 싱글톤 인스턴스를 재사용합니다.
     * </p>
     *
     * @return The singleton {@link S2RuleType#REQUIRED} rule | 필수 입력 검증용 싱글톤 규칙 인스턴스
     */
    public static S2Rule required() {
        return REQUIRED;
    }

    /**
     * Executes the validation logic for this rule against the provided value.
     * <p>
     * This method handles diverse validation logic including numeric ranges,
     * regex patterns (via {@link S2Cache}), date comparisons, and specialized
     * Korean identifiers (Resident Registration Number).
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 주어진 값에 대해 현재 규칙의 유효성 검증 로직을 실행합니다.
     * <p>
     * 숫자 범위, 정규식 패턴(S2Cache 활용), 날짜 비교, 한글 전용 식별자(주민번호 등)를 포함한
     * 20여 종 이상의 검증 알고리즘을 지원합니다.
     * </p>
     *
     * @param value  The individual value to validate | 검증할 개별 값
     * @param target The root target object (allowing cross-field validation) | 루트 대상 객체 (필드 간 상관관계 검증용)
     * @return {@code true} if valid | 유효한 경우 true
     */
    @SuppressWarnings("unchecked")
    public boolean isValid(Object value, Object target) {
        if (ruleType == S2RuleType.REQUIRED) {
            // 가장 자주 검사하는 필수 입력 체크 부터 한다.
            return S2Util.isNotEmpty(value);
        } else if (S2Util.isEmpty(value)) {
            // 필수 입력 체크가 아닐 때 값이 없으면 무조건 유효(true)하다.
            return true;
        }

        return switch (ruleType) {
            case ASSERT_TRUE -> value instanceof Boolean b && b;
            case ASSERT_FALSE -> value instanceof Boolean b && !b;
            case LENGTH -> {
                var targetValue = String.valueOf(value);
                var length = Integer.parseInt(String.valueOf(checkValue));
                yield targetValue.length() == length;
            }
            case MIN_LENGTH -> {
                var targetValue = String.valueOf(value);
                var minLength = Integer.parseInt(String.valueOf(checkValue));
                yield targetValue.length() >= minLength;
            }
            case MAX_LENGTH -> {
                var targetValue = String.valueOf(value);
                var maxLength = Integer.parseInt(String.valueOf(checkValue));
                yield targetValue.length() <= maxLength;
            }
            case MIN_BYTE -> {
                var targetValue = String.valueOf(value);
                var minByte = Integer.parseInt(String.valueOf(checkValue));
                yield targetValue.getBytes().length >= minByte;
            }
            case MAX_BYTE -> {
                var targetValue = String.valueOf(value);
                var maxByte = Integer.parseInt(String.valueOf(checkValue));
                yield targetValue.getBytes().length <= maxByte;
            }
            case MIN_VALUE -> {
                if (value instanceof Integer targetValue && checkValue instanceof Integer minValue) {
                    yield targetValue >= minValue;
                } else if (value instanceof Long targetValue && checkValue instanceof Long minValue) {
                    yield targetValue >= minValue;
                } else if (value instanceof Float targetValue && checkValue instanceof Float minValue) {
                    yield targetValue >= minValue;
                } else if (value instanceof Double targetValue && checkValue instanceof Double minValue) {
                    yield targetValue >= minValue;
                }
                yield false;
            }
            case MAX_VALUE -> {
                if (value instanceof Integer targetValue && checkValue instanceof Integer maxValue) {
                    yield targetValue <= maxValue;
                } else if (value instanceof Long targetValue && checkValue instanceof Long maxValue) {
                    yield targetValue <= maxValue;
                } else if (value instanceof Float targetValue && checkValue instanceof Float maxValue) {
                    yield targetValue <= maxValue;
                } else if (value instanceof Double targetValue && checkValue instanceof Double maxValue) {
                    yield targetValue <= maxValue;
                }
                yield false;
            }
            case REGEX, NUMBER, TEXT_INTACT, TEXT_COMBINE, MPHONE_NO, TEL_NO, INTERNATIONAL_TEL_NO, EMAIL, ZIP, LOGIN_ID, PASSWORD, PASSWORD_ANSWR, BIZRNO, NWINO -> {
                var regex = ruleType == S2RuleType.REGEX ? String.valueOf(checkValue) : ruleType.getRegex();
                yield S2Cache.getPattern(regex)
                        .map(pattern -> pattern.matcher(String.valueOf(value)).matches())
                        .orElse(false);
            }
            case DATE -> {
                // 타입에 따라 다르게 처리: 문자열 파싱 or Temporal 객체 valid 체크
                if (value instanceof Temporal temporal) {
                    // Temporal 객체 (LocalDate, LocalDateTime 등): 년도 범위만 체크 (포맷 검증 불필요)
                    try {
                        int year = temporal.get(java.time.temporal.ChronoField.YEAR);
                        if (year < (Year.now().getValue() - 100)) {
                            yield false;
                        }
                        yield true;
                    } catch (DateTimeException e) {
                        yield false;
                    }
                }

                if (value instanceof String dateString) {
                    // 날짜 문자열에서 특수문자를 제거
                    dateString = S2StringUtil.removeChars(dateString, '-', '.');
                    if (dateString.length() != 8) {
                        yield false;
                    }
                    try {
                        var year = Integer.parseInt(dateString.substring(0, 4));
                        var month = Integer.parseInt(dateString.substring(4, 6));
                        var day = Integer.parseInt(dateString.substring(6, 8));
                        if (year < (Year.now().getValue() - 100)) {
                            yield false;
                        }
                        LocalDate.of(year, month, day);
                        yield true;
                    } catch (DateTimeException | NumberFormatException e) {
                        yield false;
                    }
                }

                yield false;
            }
            case DATE_AFTER -> {
                Object targetValue = S2Util.getValue(target, checkValue);
                if (S2Util.isEmpty(targetValue))
                    yield true; // 타겟 empty 시 무시 (optional 의미)

                // value/targetValue를 LocalDate로 변환 후 비교
                Temporal temporal1 = toMaxPrecisionTemporal(value);
                Temporal temporal2 = toMaxPrecisionTemporal(targetValue);
                if (temporal1 == null || temporal2 == null) {
                    yield true;
                }
                yield ((Comparable<Temporal>) temporal1).compareTo(temporal2) >= 0;
            }
            case DATE_BEFORE -> {
                Object targetValue = S2Util.getValue(target, checkValue);
                if (S2Util.isEmpty(targetValue))
                    yield true;

                Temporal temporal1 = toMaxPrecisionTemporal(value);
                Temporal temporal2 = toMaxPrecisionTemporal(targetValue);
                if (temporal1 == null || temporal2 == null) {
                    yield true;
                }
                yield ((Comparable<Temporal>) temporal1).compareTo(temporal2) <= 0;
            }
            case EQUALS_FIELD -> {
                Object targetValue = S2Util.getValue(target, checkValue);
                yield Objects.equals(value, targetValue);
            }
            case JUMIN -> {
                var jumin = S2StringUtil.removeChars(String.valueOf(value), '-');

                if (jumin.length() != 13) {
                    yield false;
                }

                var flag = Character.getNumericValue(jumin.charAt(6));
                var isKorean = flag < 5 || flag > 8;
                var check = 0;

                for (var i = 0; i < 12; i++) {
                    if (isKorean) {
                        check += ((i % 8 + 2) * Character.getNumericValue(jumin.charAt(i)));
                    } else {
                        check += ((9 - i % 8) * Character.getNumericValue(jumin.charAt(i)));
                    }
                }

                if (isKorean) {
                    check = 11 - (check % 11);
                    check %= 10;
                } else {
                    var remainder = check % 11;
                    if (remainder == 0) {
                        check = 1;
                    } else if (remainder == 10) {
                        check = 0;
                    } else {
                        check = remainder;
                    }

                    var check2 = check + 2;
                    check = (check2 > 9) ? (check2 - 10) : check2;
                }

                yield check == Character.getNumericValue(jumin.charAt(12));
            }
            case NESTED, EACH -> true;
            default -> false;
        };
    }

    /**
     * Checks if the value is invalid according to this rule.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증 대상 값이 이 규칙에 유효하지 않은지 확인합니다 ({@code !isValid()}).
     *
     * @param value  The value to validate | 검증할 인자 값
     * @param target The root target object | 루트 대상 객체
     * @return {@code true} if invalid | 유효하지 않은 경우 true
     */
    public boolean isInvalid(Object value, Object target) {
        return !isValid(value, target);
    }

    /**
     * Converts an input (String or Temporal) into a high-precision Temporal for comparison.
     * <p>
     * Priority: {@code OffsetDateTime} &gt; {@code LocalDateTime} &gt; {@code LocalDate}.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 입력값(문자열 또는 Temporal)을 정밀도가 가장 높은 시간 객체로 변환합니다.
     * <p>
     * 우선순위: OffsetDateTime &gt; LocalDateTime &gt; LocalDate 순으로 파싱을 시도합니다.
     * </p>
     *
     * @param value The value to convert | 변환할 값
     * @return High-precision Temporal object, or null on failure | 고정밀 Temporal 객체 (실패 시 null)
     */
    private static Temporal toMaxPrecisionTemporal(Object value) {
        return S2DateUtil.toMaxPrecisionTemporal(value, (val) -> {
            if (val instanceof String dateString) {
                dateString = S2StringUtil.removeChars(dateString, '-', '.');
                if (dateString.length() == 8)
                    return S2DateUtil.parseToLocalDate(dateString, "yyyyMMdd");
            }
            return null;
        });
    }

    /**
     * Resolves the appropriate error message template based on locale and precedence.
     * <p>
     * Follows the 4-step resolution hierarchy described in the class-level documentation.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 로케일과 우선순위를 고려하여 최종 에러 메시지 템플릿을 결정합니다.
     * <p>
     * 클래스 레벨 문서에 기술된 4단계 결정 시스템을 기반으로 최적의 메시지를 탐색합니다.
     * </p>
     *
     * @param locale The user's current locale | 사용자의 현재 로케일
     * @return The resolved message template | 결정된 메시지 템플릿
     */
    public String getErrorMessageTemplate(Locale locale) {
        return S2ResourceBundle.getMessage(S2Validator.getValidationBundle(), errorMessageKey, locale).orElseGet(() -> {
            String template = messageTemplates.get(locale.getLanguage());
            if (template == null || template.isBlank()) {
                template = messageTemplates.get(S2Validator.getDefaultLocale().getLanguage());
            }
            if (template == null || template.isBlank()) {
                // 직접 설정한 템플릿이 없으면 S2RuleType의 기본값 사용
                template = ruleType.getErrorMessageTemplate(locale);
            }
            return template;
        });
    }

    /**
     * Checks if this rule matches the given type.
     *
     * @param type The type to compare | 비교할 타입
     * @return {@code true} if matched | 일치하는 경우 true
     */
    public boolean isType(S2RuleType type) {
        return this.ruleType == type;
    }

    /**
     * Returns the validation rule type.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 설정된 검증 규칙 타입을 반환합니다.
     *
     * @return The rule type | 검증 규칙 타입
     */
    public S2RuleType getRuleType() {
        return ruleType;
    }

    /**
     * Returns the criterion value (checkValue).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증의 기준이 되는 값(checkValue)을 반환합니다.
     *
     * @return The value being used for verification | 검증 기준값
     */
    public Object getCheckValue() {
        return checkValue;
    }

    /**
     * Returns the localized error message property key.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 설정된 에러 메시지 프로퍼티 키를 반환합니다.
     *
     * @return The property key | 에러 메시지 프로퍼티 키
     */
    public String getErrorMessageKey() {
        return errorMessageKey;
    }

}
