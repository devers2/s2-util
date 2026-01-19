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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import io.github.devers2.s2util.core.S2Util;
import io.github.devers2.s2util.log.S2LogManager;
import io.github.devers2.s2util.log.S2Logger;
import io.github.devers2.s2util.message.S2ResourceBundle;
import io.github.devers2.s2util.validation.S2Validator.S2ValidationError;
import io.github.devers2.s2util.validation.spring.S2BindValidator;

/**
 * Comprehensive smoke test suite for S2Util and Validator.
 * <p>
 * This class contains integration tests that verify the correctness of S2Util core utilities,
 * S2Validator validation logic, S2BindValidator Spring integration, and various edge cases.
 * </p>
 *
 * <p>
 * <b>[한국어 설명]</b>
 * </p>
 * S2Util 및 Validator의 종합 스모크 테스트 모음입니다.
 * <p>
 * S2Util 핵심 유틸리티, S2Validator 검증 로직, S2BindValidator Spring 통합 및 다양한 엣지 케이스의
 * 정확성을 검증하는 통합 테스트를 포함합니다.
 * </p>
 */
public class SmokeTest {

    private static S2Logger logger;
    private static int successCount = 0;
    private static int failCount = 0;
    private static final List<String> failDetails = new ArrayList<>();

    @BeforeAll
    static void setup() {
        logger = S2LogManager.getLogger(SmokeTest.class);
        // 테스트용 리소스 번들 설정 (src/test/resources/test_messages.properties)
        S2ResourceBundle.setDefaultBasename("test_messages");
    }

    /**
     * Supports direct execution via JavaExec.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * JavaExec 등을 통한 직접 실행을 지원합니다.
     */
    public static void main(String[] args) {
        setup();
        new SmokeTest().testComprehensive();
    }

    @org.junit.jupiter.api.Test
    void testComprehensive() {
        logger.info("================================================================================");
        logger.info("[시작] S2Util & Validator 종합 테스트");
        logger.info("================================================================================");

        // 1. S2Util 테스트 (Map & VO)
        testS2Util();

        // 2. S2Validator 실패 테스트 (의도됨)
        testS2ValidatorFailure();

        // 3. S2Validator 성공 테스트 (통과 사례)
        testS2ValidatorSuccess();

        // 4. S2Validator ResourceBundle 테스트 (커스텀 메시지)
        testS2ValidatorResourceBundle();

        // 5. S2Validator 타입 기반 교차 필드 검증 테스트
        testS2ValidatorCrossFieldByType();

        // 6. S2Validator 교차 필드 검증 테스트 (BiPredicate)
        testS2ValidatorCross();

        // 7. S2BindValidator 체이닝 & JSON 생성 테스트 (지연 등록)
        testS2BindValidatorChaining();

        // 8. S2BindValidator 검증 테스트 (MapBindingResult 활용)
        testS2BindValidatorExecution();

        // 9. S2BindValidator 제너릭 유연성 테스트
        testS2BindValidatorGeneric();

        // 10. S2Validator 조건부 유효성 검증 테스트 (when)
        testS2ValidatorConditional();

        // 11. S2Validator 복합 규칙 및 체이닝 테스트
        testS2ValidatorComplexRules();

        // 12. S2Validator 타입 캐스팅 오류 방어 테스트
        testS2ValidatorCastingSafety();

        // 13. S2Validator 단일 값 검증 테스트 (check)
        testS2ValidatorValueCheck();

        // 14. Validator Chain Integrity (혼선 방지) 테스트
        testChainIntegrity();

        // 15. 중첩 경로 및 인덱스 구문 테스트
        testNestedValidation();

        // 16. 순환 참조 방지 및 최대 깊이 제한 테스트
        testCircularReference();

        // 17. JSON 생성 시 기본 REQUIRED 규칙 적용 테스트 (규칙 없는 필드)
        testS2ValidatorDefaultRequiredJson();

        logger.info("================================================================================");
        logger.info("[요약] 테스트 결과 보고서");
        logger.info("--------------------------------------------------------------------------------");
        logger.info(" - 총 성공 건수: {}", successCount);
        logger.info(" - 총 실패 건수: {}", failCount);
        if (failCount > 0) {
            logger.error(">>> 실패 상세 내역:");
            failDetails.forEach(detail -> logger.error("   * {}", detail));
        } else {
            logger.info(">>> 모든 테스트를 성공적으로 통과했습니다 (의도된 유효성 검사 실패 포함)");
        }
        logger.info("================================================================================");
        logger.info("[종료] S2Util & Validator 종합 테스트");
        logger.info("================================================================================");
    }

    /**
     * Tests circular reference detection functionality.
     * <p>
     * Verifies that the validator detects circular references when the same object instance
     * appears again in the validation path using an IdentityHashMap-based Set.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 순환 참조 감지 기능을 테스트합니다.
     * <p>
     * IdentityHashMap 기반 Set을 사용하여 동일한 객체 인스턴스가
     * 검증 경로에 다시 나타나면 순환 참조로 감지하는지 확인합니다.
     * </p>
     */
    private void testCircularReference() {
        logger.info(">>> 16. 순환 참조 감지 테스트");

        // 순환 참조 테스트 (자기 자신을 참조하는 객체)
        try {
            Map<String, Object> circular = new java.util.HashMap<>();
            circular.put("self", circular); // 자기 자신 참조
            circular.put("name", "Circular");

            logger.info("  [Action] 자기 참조 객체 검증 시작 (순환 참조 감지 확인)");

            // NESTED 규칙으로 자기 자신을 다시 호출하도록 설정
            S2Validator<Object> selfValidator = S2Validator.builder()
                    .field("name").rule(S2RuleType.REQUIRED)
                    .field("self").rule(S2RuleType.NESTED, S2Validator.builder().field("name").rule(S2RuleType.REQUIRED).build())
                    .build();

            List<S2ValidationError> circularErrors = new java.util.ArrayList<>();
            boolean result = selfValidator.validate(circular, circularErrors::add);

            boolean isCircularOk = false;
            if (!result && !circularErrors.isEmpty()) {
                for (var err : circularErrors) {
                    if (err.defaultMessage().contains("순환 참조")) {
                        logger.info("  [PASS] 순환 참조 에러 정상 감지: {} - {}", err.fieldName(), err.defaultMessage());
                        isCircularOk = true;
                    }
                }
            }
            record(isCircularOk, "순환 참조(Self-Reference) 감지 테스트");
        } catch (Exception e) {
            logger.error("  [FAIL] 순환 참조 처리 중 예외 발생: ", e);
            record(false, "순환 참조 감지 테스트 기술 오류");
        }
    }

    /**
     * Test Value Object (VO) for validation testing.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 검증 테스트용 Value Object입니다.
     */
    public static class TestVO {
        private String userId;
        private String userName;
        private int age;
        private String email;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "TestVO{userId='" + userId + "', userName='" + userName + "', age=" + age + ", email='" + email + "'}";
        }

    }

    /**
     * Dedicated Value Object for generic type testing.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 제너릭 타입 테스트용 전용 Value Object입니다.
     */
    public static class SpecificVO {
        private String name;
        private int score;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }

    /**
     * Validation rule supplier for chaining tests.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 신규 체이닝 테스트를 위한 규칙 생성 레시피(Supplier)입니다.
     */
    private S2Validator<Object> memberRules() {
        return S2Validator.builder()
                .field("userId", "아이디").rule(S2RuleType.REQUIRED)
                .field("userPw", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
                .field("confirmPw", "비밀번호 확인")
                .rule((value, target) -> S2Util.getValue(target, "userPw", "").equals(value)).ko("비밀번호가 일치하지 않습니다.")
                .build();
    }

    /**
     * Example supplier that returns a specific type.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 구체적인 타입을 반환하는 서플라이어 예시입니다.
     */
    private S2Validator<SpecificVO> specificRules() {
        return S2Validator.<SpecificVO>builder()
                .field("name", "이름").rule(S2RuleType.REQUIRED)
                .field("score", "점수").rule(S2RuleType.MIN_VALUE, 0)
                .build();
    }

    /**
     * Records test results (success/failure).
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 테스트 결과를 기록합니다.
     *
     * @param isSuccess Whether the test passed | 테스트 통과 여부
     * @param testName  Name of the test | 테스트 이름
     */
    private void record(boolean isSuccess, String testName) {
        if (isSuccess) {
            successCount++;
            logger.info("[PASS] {}", testName);
        } else {
            failCount++;
            failDetails.add(testName);
            logger.error("[FAIL] {}", testName);
        }
    }

    /**
     * Tests S2Util's value injection and extraction capabilities.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2Util의 값 주입 및 추출 기능을 테스트합니다.
     */
    private void testS2Util() {
        logger.info(">>> 1. S2Util 테스트 (setValue & getValue)");

        try {
            // Map Test
            logger.info("[Action] Map 조작 테스트 중...");
            Map<String, Object> map = new HashMap<>();
            S2Util.setValue(map, "name", "홍길동");
            S2Util.setValue(map, "age", 30);

            Object mapName = S2Util.getValue(map, "name");
            Object mapAge = S2Util.getValue(map, "age");
            logger.info("   -> Map 값 조회 결과: name={}, age={}", mapName, mapAge);

            record("홍길동".equals(mapName) && Integer.valueOf(30).equals(mapAge), "Map Set/GetValue 테스트");

            // VO Test
            logger.info("[Action] VO 조작 테스트 중...");
            TestVO vo = new TestVO();
            S2Util.setValue(vo, "userId", "user01");
            S2Util.setValue(vo, "age", 25);

            Object voId = S2Util.getValue(vo, "userId");
            Object voAge = S2Util.getValue(vo, "age");
            logger.info("   -> VO 값 조회 결과: userId={}, age={}", voId, voAge);

            record("user01".equals(voId) && Integer.valueOf(25).equals(voAge), "VO Set/GetValue 테스트");
        } catch (Exception e) {
            logger.error("   -> testS2Util 오류: ", e);
            record(false, "S2Util 기술 오류: " + e.getMessage());
        }
    }

    /**
     * Tests that the validator correctly detects intentional errors.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 의도적으로 에러를 발생시켜 검증기가 이를 잡아내는지 테스트합니다.
     */
    private void testS2ValidatorFailure() {
        logger.info(">>> 2. S2Validator 실패 테스트 (의도됨)");

        Map<String, Object> data = new HashMap<>();
        data.put("requiredField", "");
        data.put("lengthField", "123");

        List<S2ValidationError> errors = new ArrayList<>();

        S2Validator.of(data)
                .field("requiredField", "필수항목").rule(S2RuleType.REQUIRED)
                .field("lengthField", "고정길이").rule(S2RuleType.LENGTH, 5)
                .validate(error -> errors.add(error), Locale.KOREAN);

        logger.info("   -> 감지된 총 에러 수 (의도됨): {}", errors.size());
        errors.forEach(err -> logger.info("      [감지된 에러] {}", err.defaultMessage()));

        record(errors.size() == 2, "Validator 에러 감지 테스트 (2개 에러 예상)");
    }

    /**
     * Tests that validation passes for valid data.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 정상 데이터에 대해 검증 통과 여부를 테스트합니다.
     */
    private void testS2ValidatorSuccess() {
        logger.info(">>> 3. S2Validator 성공 테스트 (통과 사례)");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", "admin");
        data.put("age", 20);
        data.put("email", "test@s2.kr");

        logger.info("[Action] 정상 데이터 확인 중: {}", data);

        List<S2ValidationError> errors = new ArrayList<>();

        S2Validator.of(data)
                .field("userId", "아이디").rule(S2RuleType.REQUIRED)
                .field("age", "나이").rule(S2RuleType.MIN_VALUE, 19)
                .field("email", "이메일").rule(S2RuleType.EMAIL)
                .validate(error -> errors.add(error), Locale.KOREAN);

        boolean isOk = errors.isEmpty();
        if (!isOk) {
            errors.forEach(err -> logger.error("   -> 예상치 못한 에러: {}", err.defaultMessage()));
        } else {
            logger.info("   -> 모든 유효성 검사를 예상대로 통과했습니다.");
        }
        record(isOk, "Validator 성공 사례 테스트");
    }

    /**
     * Tests custom error message extraction via ResourceBundle.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 리소스 번들을 통한 커스텀 에러 메시지 추출을 테스트합니다.
     */
    private void testS2ValidatorResourceBundle() {
        logger.info(">>> 4. S2Validator ResourceBundle 테스트 (커스텀 메시지)");

        S2Validator.setValidationBundle("test_messages");

        Map<String, Object> data = new HashMap<>();
        data.put("customField", "");

        logger.info("[Action] ResourceBundle에서 커스텀 메시지 확인 중 (키: test.err.custom)");

        List<S2ValidationError> errors = new ArrayList<>();

        S2Validator.of(data)
                .field("customField", "커스텀필드").rule(S2RuleType.REQUIRED, null, "test.err.custom")
                .validate(error -> errors.add(error), Locale.KOREAN);

        boolean bundleOk = !errors.isEmpty() && errors.get(0).defaultMessage().contains("리소스 번들에서 정의된 메시지입니다");
        if (bundleOk) {
            logger.info("   -> 번들에서 메시지를 성공적으로 가져왔습니다: {}", errors.get(0));
        } else {
            logger.error("   -> 커스텀 메시지를 가져오지 못했습니다. 결과: {}", errors.isEmpty() ? "에러 없음" : errors.get(0));
        }
        record(bundleOk, "Validator ResourceBundle (messages.properties) 테스트");
    }

    /**
     * Tests cross-field validation functionality using S2RuleType.
     * <p>
     * Verifies that date sequence relationships (DATE_AFTER, DATE_BEFORE) and field equality
     * (EQUALS_FIELD) errors are correctly detected.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2RuleType을 이용한 교차 필드 검증 기능을 테스트합니다.
     * <p>
     * 날짜 선후 관계(DATE_AFTER, DATE_BEFORE)와 필드 값 일치(EQUALS_FIELD) 오류를
     * 정상적으로 탐지하는지 확인합니다.
     * </p>
     */

    private void testS2ValidatorCrossFieldByType() {

        logger.info(">>> 5. S2Validator 타입 기반 교차 필드 검증 테스트");

        // 1. 에러 데이터 테스트 (종료일 < 시작일, 비밀번호 불일치)

        Map<String, Object> data = new HashMap<>();

        data.put("startDate", "2025-01-10");

        data.put("endDate", "2025-01-01"); // DATE_AFTER 에러

        data.put("password", "s2secret123");

        data.put("confirmPassword", "wrongPassword"); // EQUALS_FIELD 에러

        List<S2ValidationError> errors = new ArrayList<>();

        S2Validator.of(data)
                .field("endDate", "종료일").rule(S2RuleType.DATE_AFTER, "startDate")
                .field("startDate", "시작일").rule(S2RuleType.DATE_BEFORE, "endDate")
                .field("confirmPassword", "비밀번호 확인").rule(S2RuleType.EQUALS_FIELD, "password")
                .validate(errors::add, Locale.KOREAN);

        record(errors.size() == 3, "타입 기반 교차 필드 검증 실패 테스트 (3개 에러 예상)");

        errors.forEach(err -> logger.info("      [감지된 에러] {}", err.defaultMessage()));

        // 2. 정상 데이터 테스트

        data.put("endDate", "2025-01-20");

        data.put("confirmPassword", "s2secret123");

        List<S2ValidationError> successErrors = new ArrayList<>();

        S2Validator.of(data)
                .field("endDate", "종료일").rule(S2RuleType.DATE_AFTER, "startDate")
                .field("startDate", "시작일").rule(S2RuleType.DATE_BEFORE, "endDate")
                .field("confirmPassword", "비밀번호 확인").rule(S2RuleType.EQUALS_FIELD, "password")
                .validate(successErrors::add, Locale.KOREAN);

        record(successErrors.isEmpty(), "타입 기반 교차 필드 검증 성공 테스트 (0개 에러 예상)");

    }

    /**
     * Tests cross-field validation using BiPredicate.
     * <p>
     * Verifies that password mismatches and date sequence errors are correctly detected.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * 교차 필드 검증(BiPredicate) 기능을 테스트합니다.
     * <p>
     * 비밀번호 불일치와 날짜 선후 관계 오류를 정상적으로 탐지하는지 확인합니다.
     * </p>
     */
    private void testS2ValidatorCross() {
        logger.info(">>> 6. S2Validator 교차 필드 검증 테스트 (BiPredicate)");

        // 1. 에러 데이터 테스트 (비밀번호 불일치, 종료일이 시작일보다 빠름)
        Map<String, Object> data = new HashMap<>();
        data.put("password", "s2secret123");
        data.put("confirmPw", "wrongPw"); // 불일치
        data.put("startDate", "2025-01-01");
        data.put("endDate", "2024-12-31"); // 시작일보다 빠름

        List<S2ValidationError> errors = new ArrayList<>();

        S2Validator.of(data)
                // 비밀번호 확인 교차 검증 (BiPredicate)
                .field("confirmPw", "비밀번호 확인")
                .rule((value, target) -> {
                    String password = S2Util.getValue(target, "password", "");
                    return password.equals(value);
                }).ko("{0|은/는} 원본 비밀번호와 일치해야 합니다.")
                // 날짜 범위 교차 검증 (BiPredicate)
                .field("endDate", "종료일")
                .rule((value, target) -> {
                    String startDate = S2Util.getValue(target, "startDate", "");
                    return startDate.compareTo((String) value) <= 0;
                }).ko("{0|은/는} 시작일보다 이후여야 합니다.")
                .validate(errors::add, Locale.KOREAN);

        record(errors.size() == 2, "교차 필드 검증 실패 테스트 (2개 에러 예상)");

        // 메시지 일치 검증
        boolean messageOk = true;
        String expectedPwMsg = "비밀번호 확인은 원본 비밀번호와 일치해야 합니다.";
        String expectedDateMsg = "종료일은 시작일보다 이후여야 합니다.";

        for (S2ValidationError error : errors) {
            logger.info("      [감지된 에러] {}", error.defaultMessage());

            if ("confirmPw".equals(error.fieldName()) && !error.defaultMessage().equals(expectedPwMsg)) {
                logger.error("      [메시지 오류] confirmPw 필드: 예상='{}', 실제='{}'", expectedPwMsg, error.defaultMessage());
                messageOk = false;
            }
            if ("endDate".equals(error.fieldName()) && !error.defaultMessage().equals(expectedDateMsg)) {
                logger.error("      [메시지 오류] endDate 필드: 예상='{}', 실제='{}'", expectedDateMsg, error.defaultMessage());
                messageOk = false;
            }
        }

        record(messageOk, "교차 필드 검증 에러 메시지 일치 테스트");

        // 2. 정상 데이터 테스트 (비밀번호 일치, 종료일이 시작일 이후)
        data.put("confirmPw", "s2secret123");
        data.put("endDate", "2025-12-31");

        List<S2ValidationError> successErrors = new ArrayList<>();

        S2Validator.of(data)
                .field("password", "비밀번호")
                .rule((value, target) -> {
                    String password = S2Util.getValue(target, "password", "");
                    return password.equals(value);
                }, "error")
                .field("endDate", "종료일").rule((value, target) -> {
                    String startDate = S2Util.getValue(target, "startDate", "");
                    return startDate.compareTo((String) value) <= 0;
                }, "error")
                .validate(successErrors::add, Locale.KOREAN);

        record(successErrors.isEmpty(), "교차 필드 검증 성공 테스트 (0개 에러 예상)");
    }

    /**
     * Tests S2BindValidator chaining and JSON generation.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2BindValidator 체이닝 및 JSON 생성을 테스트합니다.
     */
    private void testS2BindValidatorChaining() {
        logger.info(">>> 7. S2BindValidator 체이닝 & JSON 테스트");
        // 지연 실행을 통해 "MEMBER_JOIN" 키로 최초 등록 및 JSON 반환함
        String json = S2BindValidator.context("MEMBER_JOIN", this::memberRules).getRulesJson();

        record(json != null && json.contains("userId") && json.contains("userPw"), "S2BindValidator JSON 생성 테스트");
        logger.debug("생성된 JSON: {}", json);
    }

    /**
     * Tests S2BindValidator.validate() execution.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2BindValidator.validate() 실행을 테스트합니다.
     */
    private void testS2BindValidatorExecution() {
        logger.info(">>> 8. S2BindValidator 유효성 검증 실행 테스트");
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("userId", ""); // 에러 예상
        invalidData.put("userPw", "123"); // 에러 예상
        invalidData.put("confirmPw", "456"); // 에러 예상

        // MapBindingResult를 사용하여 Spring BindingResult를 시뮬레이션함
        BindingResult bindingResult = new MapBindingResult(invalidData, "memberDto");

        // 체이닝을 통한 검증 수행함
        S2BindValidator.context("MEMBER_JOIN", this::memberRules).validate(invalidData, bindingResult);

        record(bindingResult.getErrorCount() == 3, "S2BindValidator 검증 수행 테스트 (에러 3건 예상)");

        if (bindingResult.hasErrors()) {
            bindingResult.getFieldErrors().forEach(
                    e -> logger.info(" [검증결과] 필드: {}, 메시지: {}", e.getField(), e.getDefaultMessage())
            );
        }
    }

    /**
     * Tests S2BindValidator generic flexibility.
     * <p>
     * Verifies that Supplier&lt;S2Validator&lt;SpecificVO&gt;&gt; forms are accepted.
     * </p>
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2BindValidator 제너릭 유연성을 테스트합니다.
     * <p>
     * Supplier&lt;S2Validator&lt;SpecificVO&gt;&gt; 형태가 수용되는지 확인합니다.
     * </p>
     */
    private void testS2BindValidatorGeneric() {
        logger.info(">>> 9. S2BindValidator 제너릭 유연성 테스트");

        // 구체적인 타입을 반환하는 메서드 참조 (this::specificRules) 가 에러 없이 전달되어야 함
        try {
            S2BindValidator.BoundContext<SpecificVO> ctx = S2BindValidator.context("SPECIFIC", this::specificRules);
            String json = ctx.getRulesJson();

            SpecificVO vo = new SpecificVO();
            vo.setName(""); // 에러 유도
            vo.setScore(-10); // 에러 유도

            BindingResult bindingResult = new MapBindingResult(new HashMap<>(), "specificVo");
            ctx.validate(vo, bindingResult);

            boolean isOk = bindingResult.getErrorCount() == 2 && json.contains("score");
            record(isOk, "S2BindValidator 제너릭(SubType) 수용 테스트");

            if (bindingResult.hasErrors()) {
                bindingResult.getFieldErrors().forEach(
                        e -> logger.info(" [검증결과] 필드: {}, 메시지: {}", e.getField(), e.getDefaultMessage())
                );
            }
        } catch (Exception e) {
            logger.error(" [FAIL] 제너릭 테스트 중 예외 발생: ", e);
            record(false, "S2BindValidator 제너릭 수용 테스트 기술 오류");
        }
    }

    /**
     * Tests conditional validation using S2Field's when().
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * S2Field의 when()을 사용한 조건부 유효성 검증을 테스트합니다.
     */

    private void testS2ValidatorConditional() {

        logger.info(">>> 10. S2Validator 조건부 유효성 검증 테스트 (when)");

        // 테스트 데이터

        Map<String, Object> data = new HashMap<>();

        data.put("type", "A"); // type 필드

        data.put("fieldA", ""); // fieldA는 type='A'일 때만 필수

        data.put("fieldB", ""); // fieldB는 type='B'일 때만 필수

        // 검증 규칙

        // S2Field[] fields = {

        // S2Field.of("fieldA", "필드A").when(S2Condition.of("type", "A")).addCheck(S2RuleType.REQUIRED),

        // S2Field.of("fieldB", "필드B").when(S2Condition.of("type", "B")).addCheck(S2RuleType.REQUIRED)

        // };

        S2Validator<Object> testValidator = S2Validator.builder()
                .field("fieldA", "필드A").when("type", "A")
                .rule(S2RuleType.REQUIRED)
                .field("fieldB", "필드B").when("type", "B")
                .build();

        // 1. type='A'일 때 fieldA는 검증 실패, fieldB는 검증 통과(스킵)

        List<S2ValidationError> errors = new ArrayList<>();

        testValidator.validate(data, errors::add);

        record(errors.size() == 1 && errors.get(0).fieldName().equals("fieldA"), "조건부 (when type='A') 테스트");

        // 2. type='B'로 변경

        data.put("type", "B");

        errors.clear();

        testValidator.validate(data, errors::add);

        record(errors.size() == 1 && errors.get(0).fieldName().equals("fieldB"), "조건부 (when type='B') 테스트");

        // 3. AND/OR 조건 테스트

        data.clear();

        data.put("useAdvanced", "Y");

        data.put("advancedOption", ""); // useAdvanced='Y' AND advancedMode='FULL' 일때만 필수

        data.put("otherOption", ""); // useAdvanced='Y' 또는 isTest='true' 일때 필수

        S2Validator<Object> testValidator2 = S2Validator.builder()
                .field("advancedOption", "고급옵션")
                // AND 조건: useAdvanced='Y' 이고 advancedMode='FULL' 이어야 함
                .when("useAdvanced", "Y").and("advancedMode", "FULL")
                .rule(S2RuleType.REQUIRED)
                .field("otherOption", "기타옵션")
                // OR 조건: useAdvanced='Y' 이거나 isTest='true' 여야 함
                .when("useAdvanced", "Y").when("isTest", "true")
                .rule(S2RuleType.REQUIRED)
                .build();

        // 3-1. AND 조건 불만족 (advancedMode가 없음) -> advancedOption 검증 스킵

        // OR 조건 만족 (useAdvanced='Y') -> otherOption 검증 실패

        errors.clear();

        testValidator2.validate(data, errors::add);

        record(errors.size() == 1 && errors.get(0).fieldName().equals("otherOption"), "조건부 (AND 스킵 / OR 통과) 테스트");

        // 3-2. AND 조건 만족 -> advancedOption 검증 실패

        data.put("advancedMode", "FULL");

        errors.clear();

        testValidator2.validate(data, errors::add);

        record(errors.size() == 2, "조건부 (AND 통과 / OR 통과) 테스트");

        // 3-3. AND/OR 조건 모두 불만족 -> 모두 검증 스킵

        data.put("useAdvanced", "N");

        data.put("advancedMode", "SIMPLE");

        errors.clear();

        testValidator2.validate(data, errors::add);

        record(errors.isEmpty(), "조건부 (모두 스킵) 테스트");

    }

    /**
     * Tests complex chaining rules including rule, customRule, when, and, message.
     *
     * <p>
     * <b>[한국어 설명]</b>
     * </p>
     * rule, customRule, when, and, message 등 복합적인 체이닝 규칙을 테스트합니다.
     */
    private void testS2ValidatorComplexRules() {
        logger.info(">>> 11. S2Validator 복합 규칙 및 체이닝 테스트");

        S2Validator<Map<String, Object>> complexValidator = S2Validator.<Map<String, Object>>builder()
                // --- 필드: username ---
                // 다중 규칙: 필수, 최소길이 4
                .field("username", "사용자명")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.MIN_LENGTH, 4).ko("{0|은/는} 최소 {1}자 이상이어야 합니다.")

                // --- 필드: userType ---
                // 커스텀 규칙: ADMIN, MEMBER, GUEST 중 하나여야 함
                .field("userType", "사용자 타입")
                .rule(S2RuleType.REQUIRED)
                .rule((String value) -> List.of("ADMIN", "MEMBER", "GUEST").contains(value))
                .ko("{0|은/는} 허용된 타입(ADMIN, MEMBER, GUEST)이 아닙니다.")

                // --- 필드: adminId ---
                // 조건부 검증 (when): userType이 'ADMIN'일 때만 필수, 'ADM-'으로 시작해야 함
                .field("adminId", "관리자 ID")
                .when("userType", "ADMIN")
                .rule(S2RuleType.REQUIRED)
                .rule((String v) -> v.startsWith("ADM-")).ko("{0|은/는} 'ADM-'으로 시작해야 합니다.")

                // --- 필드: accessLevel ---
                // 복합 조건부 검증 (when + and): userType='ADMIN'이고, hasExtraPermissions=true일 때만 필수, 최소값 5
                .field("accessLevel", "접근 레벨")
                .when("userType", "ADMIN").and("hasExtraPermissions", true)
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.MIN_VALUE, 5).ko("관리자의 {0|은/는} {1} 이상이어야 합니다.")

                // --- 필드: email ---
                // 다중 규칙 (표준+커스텀), 커스텀 메시지
                .field("email", "이메일 주소")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.EMAIL)
                .rule((String v) -> !v.endsWith("spam.com")).message(Locale.ENGLISH, "Email from 'spam.com' is not allowed.")

                // --- 필드: confirmPassword ---
                // 교차 필드 검증 (BiPredicate)
                .field("confirmPassword", "비밀번호 확인")
                .rule((value, target) -> S2Util.getValue(target, "password", "").equals(value))
                .ko("비밀번호가 일치하지 않습니다.")

                .build();

        // 시나리오 1: 복합적인 실패 케이스 (ADMIN 사용자)
        logger.info("  [시나리오 1] 복합 실패 (ADMIN)");
        Map<String, Object> failData = new HashMap<>();
        failData.put("username", "t"); // 에러 1: 최소길이 위반
        failData.put("userType", "ADMIN");
        failData.put("adminId", "invalid-id"); // 에러 2: 'ADM-' 시작 위반
        failData.put("hasExtraPermissions", true);
        failData.put("accessLevel", 3); // 에러 3: 최소값 위반
        failData.put("email", "tester@spam.com"); // 에러 4: 커스텀 규칙 위반
        failData.put("password", "password123");
        failData.put("confirmPassword", "password456"); // 에러 5: 교차 필드 검증 위반

        List<S2ValidationError> errors = new ArrayList<>();
        complexValidator.validate(failData, errors::add);
        errors.forEach(e -> logger.info("    [감지된 에러] 필드: '{}', 메시지: '{}'", e.fieldName(), e.defaultMessage()));
        record(errors.size() == 5, "복합 규칙 실패 테스트 (5개 에러 예상)");

        // 시나리오 2: 조건부 규칙 건너뛰기 (MEMBER 사용자)
        logger.info("  [시나리오 2] 조건부 규칙 건너뛰기 (MEMBER)");
        Map<String, Object> skipData = new HashMap<>();
        skipData.put("username", "member01");
        skipData.put("userType", "MEMBER");
        skipData.put("adminId", ""); // 'when' 조건 불일치로 검증 안함
        skipData.put("hasExtraPermissions", false);
        skipData.put("accessLevel", 0); // 'when' 조건 불일치로 검증 안함
        skipData.put("email", "member@s2.kr");
        skipData.put("password", "password123");
        skipData.put("confirmPassword", "password123");

        errors.clear();
        complexValidator.validate(skipData, errors::add);
        record(errors.isEmpty(), "복합 규칙 조건부 건너뛰기 테스트 (0개 에러 예상)");

        // 시나리오 3: 모든 규칙 통과 (ADMIN 사용자)
        logger.info("  [시나리오 3] 모든 규칙 통과 (ADMIN)");
        Map<String, Object> successData = new HashMap<>();
        successData.put("username", "superadmin");
        successData.put("userType", "ADMIN");
        successData.put("adminId", "ADM-001");
        successData.put("hasExtraPermissions", true);
        successData.put("accessLevel", 10);
        successData.put("email", "admin@s2.kr");
        successData.put("password", "securePassword!@");
        successData.put("confirmPassword", "securePassword!@");

        errors.clear();
        complexValidator.validate(successData, errors::add);
        record(errors.isEmpty(), "복합 규칙 성공 테스트 (0개 에러 예상)");
    }

    /**
     * 커스텀 규칙에서 타입 불일치(ClassCastException) 발생 시 안전하게 false를 반환하는지 테스트한다.
     */
    private void testS2ValidatorCastingSafety() {
        logger.info(">>> 12. S2Validator 타입 캐스팅 오류 방어 테스트");

        Map<String, Object> data = new HashMap<>();
        data.put("age", "not-a-number"); // Integer가 기대되는데 String이 들어옴

        List<S2ValidationError> errors = new ArrayList<>();

        // Integer를 기대하는 Predicate를 사용하지만 실제 값은 String인 경우
        S2Validator.<Map<String, Object>>of(data)
                .field("age", "나이")
                .rule((Integer age) -> age > 19).ko("나이는 19세 이상이어야 합니다.")
                .validate(errors::add, Locale.KOREAN);

        // ClassCastException이 발생하더라도 catch되어 false(검증 실패)로 처리되어야 함
        record(errors.size() == 1, "타입 불일치 방어 테스트 (에러 1건 예상)");
        if (!errors.isEmpty()) {
            logger.info("   -> 감지된 에러 (타입오류 방어됨): {}", errors.get(0).defaultMessage());
        }
    }

    // =========================================================================
    // 유효성 검사 체인 무결성 확인
    // =========================================================================

    /**
     * 필드와 규칙, 메시지가 체이닝 과정에서 섞이지 않는지 검증한다.
     * <p>
     * A 필드 설정 후 B 필드로 넘어갔다가 다시 A 필드를 설정하는 것이 아니라,
     * 빌더 패턴에서는 순차적으로 필드가 추가되므로 "A -> B" 순서로 정의된 내용이
     * 정확히 격리되어 저장되었는지 확인한다.
     * </p>
     */
    private void testChainIntegrity() {
        logger.info(">>> 14. Validator Chain Integrity (혼선 방지) 테스트");

        try {
            // 복잡한 체인 구성
            // 타입 힌트 또는 원시 사용: 가장 간단한 방법은 Object 또는 var를 사용하는 것입니다.
            S2Validator<Object> validator = S2Validator.<Object>builder()
                    // 필드 A
                    .field("fieldA", "A필드")
                    .rule(S2RuleType.REQUIRED).ko("A-Req")
                    .rule(S2RuleType.MIN_LENGTH, 5).ko("A-Len")
                    // 필드 B (컨텍스트 전환)
                    .field("fieldB", "B필드")
                    .rule(S2RuleType.EMAIL).ko("B-Email")
                    // 필드 C (처음에는 규칙 없음, 그 다음 규칙)
                    .field("fieldC")
                    .rule(S2RuleType.REQUIRED) // default msg
                    .build();

            // 내부 구조 검증 (Reflection 없이 공개 메서드로 검증)
            // S2Field는 getFields()로 접근 가능
            List<io.github.devers2.s2util.validation.S2Field<Object>> fields = validator.getFields();

            boolean isCountOk = fields.size() == 3;
            if (!isCountOk) {
                record(false, "Chain Integrity 필드 개수 불일치 (" + fields.size() + ")");
                return;
            }

            // 필드 A 검증
            var fa = fields.get(0);
            boolean faOk = "fieldA".equals(fa.getName()) && fa.getRules().size() == 2;
            boolean faMsgOk = "A-Req".equals(fa.getErrorMessage(fa.getRules().get(0), Locale.KOREAN))
                    && "A-Len".equals(fa.getErrorMessage(fa.getRules().get(1), Locale.KOREAN));

            // 필드 B 검증
            var fb = fields.get(1);
            boolean fbOk = "fieldB".equals(fb.getName()) && fb.getRules().size() == 1;
            boolean fbMsgOk = "B-Email".equals(fb.getErrorMessage(fb.getRules().get(0), Locale.KOREAN));

            // 필드 C 검증
            var fc = fields.get(2);
            boolean fcOk = "fieldC".equals(fc.getName()) && fc.getRules().size() == 1;

            boolean integrity = faOk && faMsgOk && fbOk && fbMsgOk && fcOk;
            record(integrity, "Chain Integrity 구조 및 메시지 격리 테스트");

            if (!integrity) {
                logger.error("   -> 구조 불일치 상세: A[{}, msg{}], B[{}, msg{}], C[{}]", faOk, faMsgOk, fbOk, fbMsgOk, fcOk);
            }

        } catch (Exception e) {
            logger.error("   -> testChainIntegrity 오류: ", e);
            record(false, "Chain Integrity 기술 오류: " + e.getMessage());
        }
    }

    // =========================================================================
    // 중첩 경로 및 유효성 검사 테스트
    // =========================================================================

    /**
     * S2Util의 중첩 경로(Nested Path) 지원 기능과 이를 이용한 Validator 검증을 테스트한다.
     * (Dot Notation: "user.address.street")
     */
    private void testNestedValidation() {
        logger.info(">>> 15. Nested Path & Validation (중첩 경로) 테스트");

        try {
            // 1. S2Util.getValue / setValue 중첩 테스트 (Map)
            Map<String, Object> deepMap = new HashMap<>();
            Map<String, Object> subMap = new HashMap<>();
            subMap.put("zip", "12345");
            deepMap.put("user", subMap);

            // 중첩 Get
            String zip = S2Util.getValue(deepMap, "user.zip");
            boolean getOk = "12345".equals(zip);
            if (!getOk)
                record(false, "Nested getValue failed (Map): " + zip);

            // 중첩 Set
            S2Util.setValue(deepMap, "user.addr", "Seoul");
            String addr = S2Util.getValue(deepMap, "user.addr");
            boolean setOk = "Seoul".equals(addr);
            if (!setOk)
                record(false, "Nested setValue failed (Map): " + addr);

            boolean utilOk = getOk && setOk;
            record(utilOk, "S2Util Dot-Notation (Map) 지원 테스트");

            // 2. S2Util 중첩 테스트 (VO)
            // 임시 VO 구조 생성 (TestVO 안에 Map이 있다고 가정하거나, 그냥 Map을 VO처럼 씀)
            // SmokeTest에 적절한 Nested VO가 없으므로 Map 구조를 활용하되,
            // 실제 Validator 연동성을 중점적으로 봅니다.

            // 3. S2Validator 중첩 유효성 검사
            /*
             * {
             * "runner": {
             * "name": "Bolt",
             * "record": {
             * "time": 9.58
             * }
             * }
             * }
             */
            Map<String, Object> runner = new HashMap<>();
            runner.put("name", "Bolt");
            Map<String, Object> rec = new HashMap<>();
            rec.put("time", 9.58);
            runner.put("record", rec);

            Map<String, Object> data = new HashMap<>();
            data.put("runner", runner);

            // builder()를 사용하여 재사용 가능한 검증기 생성
            S2Validator<Map<String, Object>> v = S2Validator.<Map<String, Object>>builder()
                    .field("runner.name").rule(S2RuleType.REQUIRED)
                    .field("runner.record.time")
                    .rule(S2RuleType.MIN_VALUE, 9.0)
                    .rule(S2RuleType.MAX_VALUE, 10.0)
                    .field("runner.record.date") // 누락된 필드
                    .rule(S2RuleType.REQUIRED)
                    .build();

            // 명시적으로 데이터 검증
            List<io.github.devers2.s2util.validation.S2Validator.S2ValidationError> errors = new ArrayList<>();
            boolean isValid = v.validate(data, errors::add);

            // 예상: runner.name(pass), runner.record.time(pass), runner.record.date(fail - REQUIRED)
            boolean resultOk = !isValid && errors.size() == 1
                    && "runner.record.date".equals(errors.get(0).fieldName());

            record(resultOk, "S2Validator Nested Field Validation (Dot-Notation) 테스트");

            if (!resultOk) {
                logger.error("   -> Nested Validator 결과 불일치: Valid={}, Errors={}", isValid, errors);
            }

            // 4. 인덱스 구문 테스트 (대괄호 표기법)
            List<String> userList = new ArrayList<>();
            userList.add("UserA");
            userList.add("UserB");
            Map<String, Object> listMap = new HashMap<>();
            listMap.put("users", userList);

            // [0]으로 가져오기
            String user0 = S2Util.getValue(listMap, "users[0]");
            boolean indexGetOk = "UserA".equals(user0);
            if (!indexGetOk)
                record(false, "Bracket Notation getValue failed: " + user0);

            // [1]로 설정하기
            S2Util.setValue(listMap, "users[1]", "UserB_Updated");
            String user1 = S2Util.getValue(listMap, "users.1"); // 혼합 확인
            boolean indexSetOk = "UserB_Updated".equals(user1);
            if (!indexSetOk)
                record(false, "Bracket Notation setValue failed: " + user1);

            record(indexGetOk && indexSetOk, "S2Util Bracket Syntax ([i]) 지원 테스트");

            // 5. 대괄호 표기법을 사용한 검증기
            // listMap: { "users": ["UserA", "UserB_Updated"] }
            S2Validator<Map<String, Object>> vIndex = S2Validator.<Map<String, Object>>builder()
                    .field("users[0]").rule(S2RuleType.REQUIRED) // UserA
                    .field("users[1]").rule(S2RuleType.REGEX, "^User.*") // UserB_Updated
                    .field("users[2]") // 범위를 벗어남 -> null
                    .rule(S2RuleType.REQUIRED) // 실패해야 함
                    .build();

            List<io.github.devers2.s2util.validation.S2Validator.S2ValidationError> indexErrors = new ArrayList<>();
            boolean indexValid = vIndex.validate(listMap, indexErrors::add);

            // 예상: users[0] 통과, users[1] 통과, users[2] 실패
            boolean indexResultOk = !indexValid && indexErrors.size() == 1
                    && "users[2]".equals(indexErrors.get(0).fieldName());

            record(indexResultOk, "S2Validator Bracket Syntax Rule 테스트");

            if (!indexResultOk) {
                logger.error("   -> Bracket Validator 결과 불일치: Valid={}, Errors={}", indexValid, indexErrors);
            }

            // 6. Optional Unwrapping 테스트
            Map<String, Object> optData = new HashMap<>();
            optData.put("name", "OptUser");
            java.util.Optional<Map<String, Object>> optTarget = java.util.Optional.of(optData);

            // Optional을 통해 가져오기
            String optName = S2Util.getValue(optTarget, "name");
            boolean optGetOk = "OptUser".equals(optName);

            // Optional을 통해 설정하기
            S2Util.setValue(optTarget, "name", "OptUser_Updated");
            String optNameNew = S2Util.getValue(optData, "name");
            boolean optSetOk = "OptUser_Updated".equals(optNameNew);

            record(optGetOk && optSetOk, "S2Util Optional Unwrapping (Wrap Target) 지원 테스트");

            // 7. 재귀 검증 (EACH_VALID & NESTED)
            S2Validator<Map<String, Object>> childValidator = S2Validator.<Map<String, Object>>builder()
                    .field("id", "아이디").rule(S2RuleType.REQUIRED)
                    .field("val", "값").rule(S2RuleType.MIN_VALUE, 10)
                    .build();

            // 사례 A: EACH (컬렉션)
            Map<String, Object> parent = new HashMap<>();
            List<Map<String, Object>> items = new ArrayList<>();
            items.add(Map.of("id", "A", "val", 20)); // 통과
            // items.add(Map.of("id", "", "val", 5)); // 오류 2 (Map.of가 null을 포함하는가? 아니오, 빈 문자열은 REQUIRED 실패에 충분한가? 실제로 REQUIRED는 S2Util.isNotEmpty("")를 확인)
            Map<String, Object> item2 = new HashMap<>();
            item2.put("id", ""); // REQUIRED 실패해야 함
            item2.put("val", 5); // MIN_VALUE 실패해야 함
            items.add(item2);
            parent.put("items", items);

            S2Validator<Map<String, Object>> parentValidator = S2Validator.<Map<String, Object>>builder()
                    .field("items", "항목리스트").rule(S2RuleType.EACH, childValidator)
                    .build();

            List<S2ValidationError> recErrors = new ArrayList<>();
            boolean recValid = parentValidator.validate(parent, recErrors::add);

            boolean recOk = !recValid && recErrors.size() == 2 &&
                    recErrors.stream().anyMatch(e -> "items[1].id".equals(e.fieldName())) &&
                    recErrors.stream().anyMatch(e -> "items[1].val".equals(e.fieldName()));

            record(recOk, "S2Validator 재귀 (EACH) 테스트");

            // 사례 B: NESTED (단일 객체)
            Map<String, Object> subData = new HashMap<>();
            subData.put("id", "");
            subData.put("val", 5);
            parent.put("sub", subData);

            S2Validator<Map<String, Object>> nestValidator = S2Validator.<Map<String, Object>>builder()
                    .field("sub", "서브이미지").rule(S2RuleType.NESTED, childValidator)
                    .build();

            List<S2ValidationError> nestErrors = new ArrayList<>();
            boolean nestValid = nestValidator.validate(parent, nestErrors::add);

            boolean nestOk = !nestValid && nestErrors.size() == 2 &&
                    nestErrors.stream().anyMatch(e -> "sub.id".equals(e.fieldName())) &&
                    nestErrors.stream().anyMatch(e -> "sub.val".equals(e.fieldName()));

            record(nestOk, "S2Validator 재귀 (NESTED) 테스트");

            // 8. JSON 생성 (재귀)
            String json = S2ValidatorFactory.getRulesJson(parentValidator, Locale.KOREA);
            boolean jsonOk = json != null && json.contains("items") && json.contains("nestedRules") && json.contains("아이디");

            record(jsonOk, "S2Validator 재귀 JSON 직렬화 테스트");
            if (!jsonOk) {
                logger.error("   -> JSON 결과 불일치: {}", json);
            } else {
                logger.info("   -> 생성된 JSON: {}", json);
            }

            // 9. Wildcard Syntax (items[].name)
            Map<String, Object> wildcardTarget = new HashMap<>();
            List<Map<String, Object>> products = new ArrayList<>();
            products.add(Map.of("name", "Product1", "price", 100));

            Map<String, Object> invalidProduct = new HashMap<>();
            invalidProduct.put("name", ""); // Should fail REQUIRED
            invalidProduct.put("price", -10); // Should fail MIN_VALUE
            products.add(invalidProduct);

            wildcardTarget.put("products", products);

            S2Validator<Map<String, Object>> wildcardValidator = S2Validator.<Map<String, Object>>builder()
                    .field("products[].name", "상품명").rule(S2RuleType.REQUIRED)
                    .field("products[].price", "가격").rule(S2RuleType.MIN_VALUE, 0)
                    .build();

            List<S2ValidationError> wildcardErrors = new ArrayList<>();
            boolean wildcardValid = wildcardValidator.validate(wildcardTarget, wildcardErrors::add);

            boolean wildcardOk = !wildcardValid && wildcardErrors.size() == 2 &&
                    wildcardErrors.stream().anyMatch(e -> "products[1].name".equals(e.fieldName())) &&
                    wildcardErrors.stream().anyMatch(e -> "products[1].price".equals(e.fieldName()));

            record(wildcardOk, "S2Validator Wildcard Syntax (items[].field) 테스트");
            if (!wildcardOk) {
                logger.error("   -> Wildcard errors: {}", wildcardErrors);
            }

        } catch (Exception e) {
            logger.error("   -> testNestedValidation 오류: ", e);
            record(false, "Nested Validation 기술 오류: " + e.getMessage());
        }
    }

    /**
     * S2Validator.check() 단일 값 검증 기능을 테스트한다.
     */
    private void testS2ValidatorValueCheck() {
        logger.info(">>> 13. S2Validator 단일 값 검증 테스트 (check)");

        try {
            // 1. 단순 값 검증 (Boolean 모드)
            logger.info("[Action] check(value) - Boolean 모드 테스트 중...");
            boolean result1 = S2Validator.check("test")
                    .rule(S2RuleType.REQUIRED)
                    .rule(S2RuleType.LENGTH, 4)
                    .validate();
            record(result1, "check(value) 성공 케이스 테스트");

            boolean result2 = S2Validator.check("")
                    .rule(S2RuleType.REQUIRED)
                    .validate();
            record(!result2, "check(value) 필수값 미입력 실패 테스트");

            boolean result3 = S2Validator.check(20)
                    .rule(S2RuleType.MIN_VALUE, 18)
                    .validate();
            record(result3, "check(value) 숫자 범위 성공 테스트");

            // 2. 라벨 및 메시지 설정 (Exception 모드)
            logger.info("[Action] check(value, label) - Exception 모드 테스트 중...");
            boolean result4 = S2Validator.check("valid", "라벨")
                    .rule(S2RuleType.REQUIRED)
                    .validate();
            record(result4, "check(value, label) 성공 케이스 테스트");

            try {
                S2Validator.check((String) null, "이름")
                        .rule(S2RuleType.REQUIRED).ko("{0|은/는} 필수값입니다.")
                        .validate();
                record(false, "check(value, label) 예외 발생 테스트 (예외 발생 안함 오류)");
            } catch (io.github.devers2.s2util.exception.S2RuntimeException e) {
                boolean isMsgOk = "이름은 필수값입니다.".equals(e.getMessage());
                record(isMsgOk, "check(value, label) 커스텀 에러 메시지 검증");
            }

            // 3. 커스텀 Predicate
            logger.info("[Action] check(value) - Custom Predicate 테스트 중...");
            boolean result5 = S2Validator.check("custom")
                    .rule((String s) -> s.startsWith("cus"))
                    .validate();
            record(result5, "check(value) Custom Predicate 성공 테스트");

            boolean result6 = S2Validator.check("failed")
                    .rule((String s) -> s.startsWith("cus"))
                    .validate();
            record(!result6, "check(value) Custom Predicate 실패 테스트");

        } catch (Exception e) {
            logger.error("   -> testS2ValidatorValueCheck 오류: ", e);
            record(false, "S2Validator value check 기술 오류: " + e.getMessage());
        }
    }

    /**
     * 규칙 없이 필드만 정의된 경우 JSON 생성 시 기본 REQUIRED 규칙이 포함되는지 테스트한다.
     */
    private void testS2ValidatorDefaultRequiredJson() {
        logger.info(">>> 17. JSON 기본 REQUIRED 규칙 적용 테스트");

        try {
            // 규칙 없이 필드만 정의
            S2Validator<Object> validator = S2Validator.builder()
                    .field("testField", "테스트필드")
                    .build();

            String json = S2ValidatorFactory.getRulesJson(validator, Locale.KOREAN);
            logger.info("  [Action] 생성된 JSON: {}", json);

            // JSON에 REQUIRED 규칙이 포함되어 있는지 확인
            boolean isOk = json.contains("\"testField\"") && json.contains("\"REQUIRED\"");

            record(isOk, "규칙 없는 필드의 JSON 기본 REQUIRED 적용 테스트");
        } catch (Exception e) {
            logger.error("  [FAIL] JSON 기본 규칙 테스트 중 예외 발생: ", e);
            record(false, "JSON 기본 규칙 적용 테스트 기술 오류");
        }
    }

}
