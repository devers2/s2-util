# S2Util Library - Validator Module (s2-validator)

## Overview (개요)

### [English]

The **s2-validator** module is a unified cross-platform validation framework that enables single-configuration validation rules for both server and client environments. It provides a fluent, chainable API supporting comprehensive validation rules, conditional validation, complex custom rules, and full internationalization support (i18n). The module excels at validating nested objects with dot notation (e.g., `user.address.street`) and collection items (e.g., `items[0]`), making it ideal for complex DTO/VO hierarchies.

### [한국어]

**s2-validator** 모듈은 서버와 클라이언트를 아우르는 통합 검증 프레임워크로, 단일 설정으로 양쪽 환경에 동일한 검증 규칙을 적용할 수 있습니다. 유연한 체이닝 API를 제공하며 30가지 이상의 포괄적인 검증 규칙, 조건부 검증, 복잡한 커스텀 규칙, 완전한 국제화 지원(i18n)을 포함합니다. 특히 점 표기법(`user.address.street`)과 컬렉션 항목(`items[0]`)으로 중첩 객체를 검증하는 데 탁월하여, 복잡한 DTO/VO 계층 구조에 이상적입니다.

---

## ✨ Key Features (주요 기능)

### [English]

1. **Fluent Validation Chain API**
   - Natural, readable validation rules: `.field("fieldName").rule(S2RuleType.REQUIRED).ko("필수입력").en("Required")`
   - Chainable methods for sequential rule application
   - Chain multiple rules: Validate multiple fields or apply multiple rules to a single field (e.g., REQUIRED and LENGTH)
   - `when()` conditional validation: Apply rules only when conditions are met

2. **Performance Optimization**
   - **High-Performance Reflection with Method Handles**: Eliminates reflection bottlenecks through MethodHandle caching; JIT compiler optimization for near-native performance
   - **Intelligent Caching with Caffeine**: W-TinyLFU algorithm for optimal hit rates; Prevents data eviction during traffic spikes; Automatic cache optimization
   - **Java Version-Adaptive Thread Factory**: Virtual Thread support for Java 21+; Optimized platform thread pools for earlier versions
   - **Pattern Caching**: Regex patterns cached to reduce compilation overhead on repeated validations

3. **30+ Built-in Rule Types (S2RuleType)**
   - **Basic**: REQUIRED, ASSERT_TRUE, ASSERT_FALSE, EQUALS_FIELD
   - **String**: LENGTH, MIN_LENGTH, MAX_LENGTH, REGEX
   - **Numeric**: MIN_VALUE, MAX_VALUE, NUMBER, MIN_BYTE, MAX_BYTE
   - **Format**: EMAIL, URL, INTERNATIONAL_TEL_NO
   - **Korea-specific Format** 🇰🇷: MPHONE_NO, TEL_NO, ZIP, BIZRNO, NWINO, JUMIN, PASSWORD_ANSWR
   - **Date**: DATE, DATE_AFTER, DATE_BEFORE
   - **Text**: TEXT_INTACT, TEXT_COMBINE
   - **Custom**: CustomRule for application-specific validation logic

4. **Cross-Platform Validation**
   - Single configuration for server-side validation (Java)
   - Generate client-side validation rules (JavaScript, TypeScript, etc.)
   - Consistent validation behavior across platforms
   - Message template support for unified error messaging

5. **Advanced Nested Object Support**
   - Dot notation: `user.address.street`, `employee.department.manager.name`
   - Bracket notation: `items[0].name`, `matrix[1][2]`, `users[0].roles[1]`
   - Mixed notation: `company.departments[0].employees[1].salary`
   - Automatic handling of Optional, List, and Array traversal
   - Support for both immediate and lazy validation

6. **Comprehensive Internationalization (i18n)**
   - Message localization with `ko()`, `en()` methods for common locales
   - Custom locale support via `message(Locale, String)` method
   - `S2ResourceBundle` integration for centralized message management
   - Fallback to default messages when specific locale not provided
   - Message parameter substitution: `{0}`, `{1}` for field names and rule values
   - Korean particle support: `{0|은/는}`, `{0|이/가}`, `{0|을/를}`, `{0|과/와}` for grammatically correct messages

7. **Custom & Conditional Validation**
   - `CustomRule` interface for user-defined validation logic
   - Reusable validation rules as standalone objects
   - `when()` with `and()` for complex AND conditions
   - Multiple `when()` chains for OR logic

8. **Error Handling & Reporting**
   - Comprehensive error information: field name, error code, error message, default message
   - Consumer-based error handler for flexible error processing
   - Fail-fast mode vs. collect-all-errors mode
   - Circular reference detection for safe nested object validation

9. **Spring Framework Integration** (Optional)
   - `S2BindValidator` for Spring Data Binding with `BindingResult`
   - Supplier pattern for lazy evaluation and convenient rule management
   - Unified validation rules: Same ruleset for both server and client
   - Standard Spring error handling with automatic error field binding

### [한국어]

1. **유연한 검증 체인 API**
   - 자연스러운 읽기 쉬운 검증 규칙: `.field("fieldName").rule(S2RuleType.REQUIRED).ko("필수입력").en("Required")`
   - 순차적 규칙 적용을 위한 체이닝 메서드
   - 여러 규칙 체이닝: 여러 필드 또는 같은 필드의 여러 규칙을 동시에 검증 (예: REQUIRED와 LENGTH)
   - `when()` 조건부 검증: 조건 충족 시에만 규칙 적용

2. **성능 최적화**
   - **MethodHandle을 활용한 고성능 리플렉션**: MethodHandle 캐싱을 통해 리플렉션 병목 현상을 제거하고, JIT 컴파일러 최적화를 통해 네이티브에 가까운 성능을 구현
   - **Caffeine을 활용한 지능형 캐싱**: W-TinyLFU 알고리즘으로 최적의 적중률 달성; 트래픽 급증 시에도 중요 데이터 축출 방지; 자동 캐시 최적화
   - **자바 버전별 적응형 스레드 팩토리**: Java 21 이상 환경에서 가상 스레드 지원; 이전 버전에서는 최적화된 플랫폼 스레드 풀 사용
   - **패턴 캐싱**: 정규식 패턴을 캐싱하여 반복 검증 시 컴파일 시간 감소

3. **30가지 이상의 내장 규칙 타입(S2RuleType)**
   - **기본**: REQUIRED, ASSERT_TRUE, ASSERT_FALSE, EQUALS_FIELD
   - **문자열**: LENGTH, MIN_LENGTH, MAX_LENGTH, REGEX
   - **숫자**: MIN_VALUE, MAX_VALUE, NUMBER, MIN_BYTE, MAX_BYTE
   - **형식**: EMAIL, URL, INTERNATIONAL_TEL_NO
   - **한국 전용 형식** 🇰🇷: MPHONE_NO, TEL_NO, ZIP, BIZRNO, NWINO, JUMIN, PASSWORD_ANSWR
   - **날짜**: DATE, DATE_AFTER, DATE_BEFORE
   - **텍스트**: TEXT_INTACT, TEXT_COMBINE
   - **커스텀**: 애플리케이션 특화 검증 로직을 위한 CustomRule

4. **크로스 플랫폼 검증**
   - 서버측 검증(Java)을 위한 단일 설정
   - 클라이언트측 검증 규칙 생성(JavaScript, TypeScript 등)
   - 플랫폼 전반에 걸친 일관된 검증 동작
   - 통일된 에러 메시징을 위한 메시지 템플릿 지원

5. **고급 중첩 객체 지원**
   - 점 표기법: `user.address.street`, `employee.department.manager.name`
   - 대괄호 표기법: `items[0].name`, `matrix[1][2]`, `users[0].roles[1]`
   - 혼합 표기법: `company.departments[0].employees[1].salary`
   - Optional, List, Array 자동 처리
   - 즉시 검증과 지연 검증 모두 지원

6. **포괄적인 국제화(i18n) 지원**
   - `ko()`, `en()` 메서드를 통한 일반적인 로케일 메시지 로컬라이제이션
   - `message(Locale, String)` 메서드를 통한 커스텀 로케일 지원
   - `S2ResourceBundle` 통합으로 중앙 집중식 메시지 관리
   - 특정 로케일이 없을 때 기본 메시지로 폴백
   - 메시지 매개변수 치환: `{0}`, `{1}`로 필드명과 규칙값 지정
   - 한국어 조사 지원: `{0|은/는}`, `{0|이/가}`, `{0|을/를}`, `{0|과/와}`로 문법 올바른 메시지 생성

7. **커스텀 및 조건부 검증**
   - 사용자 정의 검증 로직을 위한 `CustomRule` 인터페이스
   - 독립 객체로의 재사용 가능한 검증 규칙
   - `when()`과 `and()`를 연결하여 복잡한 AND 조건 구현
   - 다중 `when()` 체인으로 OR 조건 구현

8. **에러 처리 및 보고**
   - 포괄적인 에러 정보: 필드명, 에러 코드, 에러 메시지, 기본 메시지
   - 유연한 에러 처리를 위한 Consumer 기반 에러 핸들러
   - Fail-fast 모드 vs. 모든 에러 수집 모드
   - 중첩 객체 검증 시 안전한 순환 참조 검출

9. **Spring Framework 통합** (선택사항)

- `S2BindValidator`로 Spring Data Binding과 `BindingResult` 연동
- Supplier 패턴을 통한 지연 평가(Lazy Evaluation)와 편리한 규칙 관리
- 통합 검증 규칙: 서버와 클라이언트 동일 룰셋 적용
- Spring 표준 에러 처리로 자동 에러 필드 바인딩

---

## 🚀 Quick Start (빠른 시작 가이드)

### 1. Installation (설치)

Add the following dependency to your `build.gradle` or `pom.xml`.

**[Gradle]**

```groovy
dependencies {
    implementation 'io.github.devers2:s2-validator:1.1.7'

    // (Optional) Required only when using Spring integration (S2BindValidator)
    implementation 'org.springframework:spring-context:6.2.19'
}
```

**[Maven]**

```xml
<dependency>
    <groupId>io.github.devers2</groupId>
    <artifactId>s2-validator</artifactId>
    <version>1.1.7</version>
</dependency>
```

---

### 2. Usage (사용법)

#### 2.1. Basic Validation (기본 검증: 즉시 검증 vs 재사용 빌더)

##### Immediate Mode (`S2Validator.of`)
검증 대상 인스턴스(DTO/VO 또는 Map)에 대해 1회성 유효성 검증을 즉시 수행합니다.

```java
Map<String, Object> data = new HashMap<>();
data.put("userId", "admin");
data.put("age", 20);
data.put("email", "test@s2.kr");

List<S2ValidationError> errors = new ArrayList<>();

// 1) Collect all errors with an error handler
boolean isValid = S2Validator.of(data)
    .field("userId", "아이디").rule(S2RuleType.REQUIRED)
    .field("age", "나이").rule(S2RuleType.MIN_VALUE, 19)
    .field("email", "이메일").rule(S2RuleType.EMAIL)
    .validate(errors::add, Locale.KOREAN);

if (!isValid) {
    errors.forEach(err -> System.out.println(err.fieldName() + ": " + err.defaultMessage()));
}

// 2) Fail-fast mode: Throws S2RuntimeException on the first error
S2Validator.of(data)
    .field("userId", "아이디").rule(S2RuleType.REQUIRED)
    .field("email", "이메일").rule(S2RuleType.EMAIL)
    .validate();
```

##### Blueprint / Builder Mode (`S2Validator.builder`)
검증 로직을 재사용 가능한 '설계도(Blueprint)'로 정의하여 멀티스레드 및 고동시성 환경에서 캐싱 및 재사용합니다.

```java
// Define reusable validator blueprint
S2Validator<UserVO> userValidator = S2Validator.<UserVO>builder()
    .field("userId", "아이디").rule(S2RuleType.REQUIRED)
        .rule(S2RuleType.MIN_LENGTH, 4).ko("{0|은/는} 최소 {1}자 이상이어야 합니다.")
    .field("userPw", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
    .field("email", "이메일").rule(S2RuleType.EMAIL)
    .build();

// Reuse across requests/threads
UserVO user = new UserVO();
user.setUserId("user01");
user.setUserPw("pass1234");
user.setEmail("user01@s2.kr");

List<S2ValidationError> errors = new ArrayList<>();
boolean isValid = userValidator.validate(user, errors::add, Locale.KOREAN);
```

---

#### 2.2. Single Value Check (단일 값 검증 - `S2Validator.check`)

DTO나 Map 없이 단일 변수나 값 하나만을 빠르게 검증할 때 사용합니다.

```java
// 1) Boolean Mode: returns true/false without throwing exceptions
boolean isValidEmail = S2Validator.check("test@s2.kr")
    .rule(S2RuleType.REQUIRED)
    .rule(S2RuleType.EMAIL)
    .validate();

// 2) Exception Mode with Label: throws S2RuntimeException with custom localized message
S2Validator.check(userInput, "이름")
    .rule(S2RuleType.REQUIRED).ko("{0|은/는} 필수 입력 항목입니다.")
    .validate();

// 3) Custom Predicate validation
boolean isAdult = S2Validator.check(25)
    .rule((Integer age) -> age >= 19)
    .validate();
```

---

#### 2.3. Cross-Field Validation (교차 필드 검증)

두 개 이상의 필드 간 상관관계(비밀번호 확인, 날짜 선후 관계 등)를 검증합니다.

```java
Map<String, Object> form = new HashMap<>();
form.put("startDate", "2025-01-01");
form.put("endDate", "2025-01-10");
form.put("password", "s2secret123");
form.put("confirmPassword", "s2secret123");

// 1) Built-in type-based cross-validation
S2Validator.of(form)
    .field("startDate", "시작일").rule(S2RuleType.DATE_BEFORE, "endDate")
    .field("endDate", "종료일").rule(S2RuleType.DATE_AFTER, "startDate")
    .field("confirmPassword", "비밀번호 확인").rule(S2RuleType.EQUALS_FIELD, "password")
    .validate(errors::add, Locale.KOREAN);

// 2) Custom cross-validation with BiPredicate (value, target)
S2Validator.of(form)
    .field("confirmPassword", "비밀번호 확인")
    .rule((value, target) -> {
        String password = S2Util.getValue(target, "password", "");
        return password.equals(value);
    }).ko("{0|은/는} 원본 비밀번호와 일치해야 합니다.")
    .validate(errors::add, Locale.KOREAN);
```

---

#### 2.4. Conditional Validation (조건부 검증 - `when`, `and`)

특정 조건이 만족될 때만 해당 필드의 유효성 검증을 실행합니다 (`String`, `Boolean`, `Number` 등 다양한 조건값 지원).

```java
S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
    // Simple condition: isPremium == true 일 때만 필수
    .field("premiumEmail", "프리미엄 이메일")
        .when("isPremium", true)
        .rule(S2RuleType.REQUIRED)

    // AND condition: useAdvanced == 'Y' AND advancedMode == 'FULL'
    .field("advancedOption", "고급 옵션")
        .when("useAdvanced", "Y").and("advancedMode", "FULL")
        .rule(S2RuleType.REQUIRED)

    // OR condition: useAdvanced == 'Y' OR isTest == 'true'
    .field("otherOption", "기타 옵션")
        .when("useAdvanced", "Y").when("isTest", "true")
        .rule(S2RuleType.REQUIRED)
    .build();
```

---

#### 2.5. Nested Objects & Collections (중첩 객체 및 컬렉션 검증)

복잡한 계층 구조를 점 표기법(Dot Notation), 대괄호 인덱스(Bracket Notation), 와일드카드(`[]`), 재귀 검증기(`NESTED`, `EACH`)로 손쉽게 검증합니다.

```java
// 1) Dot Notation & Bracket Notation
S2Validator<Map<String, Object>> dotValidator = S2Validator.<Map<String, Object>>builder()
    .field("user.address.street", "도로명 주소").rule(S2RuleType.REQUIRED)
    .field("users[0].name", "첫 번째 사용자명").rule(S2RuleType.REQUIRED)
    .build();

// 2) Wildcard Syntax: collection / array 항목 전체 자동 순회 검증
S2Validator<Map<String, Object>> wildcardValidator = S2Validator.<Map<String, Object>>builder()
    .field("products[].name", "상품명").rule(S2RuleType.REQUIRED)
    .field("products[].price", "가격").rule(S2RuleType.MIN_VALUE, 0)
    .when("products[].type", "NORMAL") // 와일드카드 필드별 개별 조건부 검증 지원
    .build();

// 3) Recursive Sub-Validators (NESTED & EACH)
S2Validator<AddressVO> addressValidator = S2Validator.<AddressVO>builder()
    .field("zipCode", "우편번호").rule(S2RuleType.REQUIRED)
    .field("street", "도로명 주소").rule(S2RuleType.REQUIRED)
    .build();

S2Validator<ItemVO> itemValidator = S2Validator.<ItemVO>builder()
    .field("itemId", "상품 ID").rule(S2RuleType.REQUIRED)
    .field("quantity", "수량").rule(S2RuleType.MIN_VALUE, 1)
    .build();

S2Validator<OrderVO> orderValidator = S2Validator.<OrderVO>builder()
    .field("orderId", "주문번호").rule(S2RuleType.REQUIRED)
    .field("shippingAddress", "배송지").rule(S2RuleType.NESTED, addressValidator) // 단일 중첩 객체
    .field("items", "주문 항목 목록").rule(S2RuleType.EACH, itemValidator)          // 컬렉션/배열 각 요소
    .build();
```

---

#### 2.6. Multi-language Messages & Korean Particle Support (i18n 및 한국어 조사 처리)

한국어 조사(`은/는`, `이/가`, `을/를`, `과/와`)를 자동으로 판별하여 문법적으로 완벽한 에러 메시지를 생성합니다.

```java
S2Validator<UserVO> validator = S2Validator.<UserVO>builder()
    .field("userName", "사용자명")
        .rule(S2RuleType.REQUIRED)
            .ko("{0|은/는} 필수 입력 항목입니다.") // -> "사용자명은 필수 입력 항목입니다."
            .en("{0} is required.")
        .rule(S2RuleType.MIN_LENGTH, 4)
            .ko("{0|이/가} 최소 {1}자 이상이어야 합니다.") // -> "사용자명이 최소 4자 이상이어야 합니다."
            .en("{0} must be at least {1} characters.")
    .build();

// Global ResourceBundle integration (e.g., messages/validation.properties)
S2Validator.setValidationBundle("messages/validation");
```

---

#### 2.7. Spring Framework Integration (Spring MVC 연동 및 클라이언트 규칙 공유 - `S2BindValidator`)

Spring의 `BindingResult`와 완벽히 호환되며, `getRulesJson()`을 통해 클라이언트 JavaScript(`s2.validator.js`)와 동일한 검증 규칙을 즉시 공유합니다.

```java
@Controller
@RequestMapping("/member")
public class MemberController {

    // 1) Supplier 패턴을 이용한 검증 규칙 정의 (최초 호출 시 1회 컴파일 후 캐싱)
    private S2Validator<MemberDTO> memberRules() {
        return S2Validator.<MemberDTO>builder()
            .field("userId", "아이디").rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.MIN_LENGTH, 4).ko("{0|은/는} 최소 {1}자 이상이어야 합니다.")
            .field("userPw", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
            .field("confirmPw", "비밀번호 확인")
                .rule((value, target) -> S2Util.getValue(target, "userPw", "").equals(value))
                .ko("비밀번호가 일치하지 않습니다.")
            .field("email", "이메일").rule(S2RuleType.EMAIL)
            .build();
    }

    // 2) GET: HTML 폼에 클라이언트 검증 규칙 JSON 전달
    @GetMapping("/join")
    public String joinForm(Model model) {
        model.addAttribute("member", new MemberDTO());
        // 클라이언트(s2.validator.js) 연동용 JSON 메타데이터 생성
        String rulesJson = S2BindValidator.context("MEMBER_JOIN", this::memberRules).getRulesJson();
        model.addAttribute("validationRules", rulesJson);
        return "member/join";
    }

    // 3) POST: 서버 측 검증 수행 및 BindingResult 자동 바인딩
    @PostMapping("/join")
    public String joinSubmit(@ModelAttribute MemberDTO member, BindingResult result, Model model) {
        // 서버 측 검증 실행: 오류 발생 시 BindingResult에 필드 에러 자동 등록
        S2BindValidator.context("MEMBER_JOIN", this::memberRules).validate(member, result);

        if (result.hasErrors()) {
            model.addAttribute("validationRules",
                S2BindValidator.context("MEMBER_JOIN", this::memberRules).getRulesJson());
            return "member/join";
        }

        memberService.join(member);
        return "redirect:/member/welcome";
    }
}
```

##### Client-Side View Integration (Thymeleaf & HTML 연동 가이드)

###### 1) HTML 폼 작성 (member/join.html)

서버 Controller에서 `model.addAttribute("validationRules", ...)`로 전달한 검증 규칙 JSON을 `<form>` 태그의 `th:data-s2-rules` 속성에 바인딩합니다.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>회원가입</title>
</head>
<body>
    <h2>회원가입</h2>

    <!-- 폼에 th:data-s2-rules 속성으로 서버에서 생성한 규칙 JSON을 전달합니다 -->
    <form id="joinForm" th:action="@{/member/join}" method="post"
          th:object="${member}" th:data-s2-rules="${validationRules}">

        <div>
            <label for="userId">아이디:</label>
            <input type="text" id="userId" th:field="*{userId}" />
            <span th:errors="*{userId}" style="color: red;"></span>
        </div>

        <div>
            <label for="userPw">비밀번호:</label>
            <input type="password" id="userPw" th:field="*{userPw}" />
            <span th:errors="*{userPw}" style="color: red;"></span>
        </div>

        <div>
            <label for="confirmPw">비밀번호 확인:</label>
            <input type="password" id="confirmPw" th:field="*{confirmPw}" />
            <span th:errors="*{confirmPw}" style="color: red;"></span>
        </div>

        <button type="submit">가입하기</button>
    </form>

    <!-- s2.validator.js 임포트 (아래 임포트 방법 참조) -->
    <script type="module" th:src="@{/s2-util/js/s2.validator.js}"></script>
</body>
</html>
```

---

###### 2) JavaScript 파일 임포트 방법 및 동작 원리

> [!NOTE]
> **Q. JavaScript 파일을 별도로 다운로드하거나 프로젝트에 복사하지 않아도 동작하는 이유가 무엇인가요?**
>
> Servlet 3.0+ 표준 사양 및 Spring Boot의 기본 정적 리소스(Static Resource) 처리 메커니즘에 따라, 의존성 라이브러리(JAR) 내부의 `META-INF/resources/` 경로에 위치한 파일들은 웹 애플리케이션의 루트(`/`) 정적 자원으로 자동 서빙됩니다.
>
> `s2-validator` 라이브러리 JAR 내부에 `META-INF/resources/s2-util/js/s2.validator.js`가 패키징되어 있으므로, 개발자가 JS 파일을 따로 프로젝트의 `src/main/resources/static` 등으로 복사할 필요 없이 브라우저에서 `/s2-util/js/s2.validator.js` URL로 바로 접근할 수 있습니다.

**권장 임포트 방식 (배포 환경 Context Path 대응):**

- **방식 A (가장 권장 - Thymeleaf `th:src`)**:
  배포 환경에 따라 애플리케이션의 Context Path(예: `/`, `/my-app`)가 달라지더라도 Thymeleaf의 `@{...}` 링크 표현식이 실제 컨텍스트 경로를 자동으로 완성해 줍니다.
  ```html
  <script type="module" th:src="@{/s2-util/js/s2.validator.js}"></script>
  ```

- **방식 B (인라인 스크립트 동적 import)**:
  인라인 `<script>` 안에서 모듈을 동적으로 가져와야 할 때 컨텍스트 경로를 안전하게 조합하여 임포트합니다.
  ```html
  <script type="module" th:inline="javascript">
      const contextPath = /*[[@{/}]]*/ '';
      import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);
  </script>
  ```

- **방식 C (importmap 활용 - AJAX 수동 검증 시 유용)**:
  ```html
  <script type="importmap" th:inline="javascript">
  {
      "imports": {
          "s2-validator": [[@{/s2-util/js/s2.validator.js}]]
      }
  }
  </script>
  <script type="module">
      import { S2Validator } from 's2-validator';
      // 필요한 곳에서 S2Validator 사용
  </script>
  ```

---

###### 3) 클라이언트 자동 검증 동작 방식 (자바스크립트 코드 한 줄 없이 폼 검증)

`s2.validator.js`는 파일이 로드되는 순간 내부의 `initS2Validator()`가 자체 실행되어 브라우저에 필요한 이벤트 리스너를 자동 등록합니다. 따라서 개발자가 **추가적인 자바스크립트 코드를 전혀 작성하지 않아도** 다음과 같은 과정이 100% 자동으로 처리됩니다.

1. **브라우저 기본 네이티브 제약 검증 비활성화 (`noValidate`)**:
   - `data-s2-rules` 속성이 선언된 모든 `<form>`에 `form.noValidate = true`를 자동으로 설정합니다. 이를 통해 브라우저 고유의 투박한 기본 영문 검증 팝업이 가로채는 것을 방지하고, S2Validator가 정의된 규칙과 커스텀 메시지를 전담하도록 합니다.
   - `MutationObserver`가 등록되어 있어 SPA 페이지 전환, 모달 팝업, AJAX 등으로 페이지 로드 이후에 DOM에 추가된 폼까지 실시간으로 감지하여 `noValidate`를 적용합니다.

2. **폼 전송(submit) 이벤트 자동 가로채기**:
   - 사용자가 `<button type="submit">`을 누르거나 입력 필드에서 Enter 키를 눌러 폼 제출을 시도할 때, `document` 레벨에서 이벤트를 가로챕니다.
   - 폼의 `data-s2-rules`에 저장된 JSON 규칙을 파싱하고, 현재 입력된 모든 폼 필드 값을 대상으로 규칙을 즉시 평가합니다.

3. **에러 발생 시 즉각 전송 차단 & 네이티브 툴팁 안내**:
   - 검증 규칙 위반이 1건이라도 발생하면 즉시 `e.preventDefault()`를 실행하여 **서버로의 폼 전송을 차단**합니다.
   - HTML5 Constraint Validation API (`form.reportValidity()`)를 호출하여 오류가 발생한 첫 번째 필드로 **자동 포커스(Focus)**를 이동시키고, 서버에서 정의했던 한국어/다국어 안내 메시지를 **브라우저 네이티브 말풍선(툴팁)**으로 띄워줍니다.

4. **실시간 에러 해제 (`input` / `change`)**:
   - 사용자가 잘못 입력된 필드에 다시 키보드로 입력을 시작(`input`)하거나 라디오/체크박스를 선택(`change`)하는 순간, 해당 필드의 에러 상태를 즉시 해제하여 깔끔한 사용자 경험(UX)을 제공합니다.

---

###### 4) 초보자를 위한 실무 팁 (Advanced Usage)

- **Tip 1: AJAX / Fetch 비동기 통신 시 수동 검증**
  전체 페이지를 다시 로드하는 일반적인 폼 submit 대신, `fetch`나 `axios`로 비동기 API 통신을 수행할 때는 `S2Validator.validate()` 메서드를 직접 호출할 수 있습니다.
  ```html
  <script type="module" th:inline="javascript">
      const contextPath = /*[[@{/}]]*/ '';
      const { S2Validator } = await import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);

      document.getElementById('ajaxSubmitBtn').addEventListener('click', async () => {
          // 폼 검증 실행 (오류 발견 시 자동으로 첫 번째 입력창에 포커스 및 툴팁 표시)
          const errors = S2Validator.validate('#joinForm');

          // 에러가 존재하면 비동기 전송 중단
          if (Object.keys(errors).length > 0) {
              console.warn('검증 실패 목록:', errors);
              return;
          }

          // 검증 통과 시 AJAX 요청 진행
          const formData = new FormData(document.getElementById('joinForm'));
          const response = await fetch('/api/member/join', {
              method: 'POST',
              body: formData
          });
      });
  </script>
  ```

- **Tip 2: 숨겨진 필드(Hidden input)나 파일 업로드를 위한 프록시 에러 요소 (`{fieldName}_error`)**
  `<input type="hidden">`이나 커스텀 파일 업로드 위젯처럼 브라우저 기본 툴팁을 띄울 수 없는 숨김 요소의 경우, `{fieldName}_error`라는 `name`을 가진 프록시 엘리먼트를 폼 안에 두면 해당 요소에 에러 메시지가 텍스트로 자동 출력됩니다.
  ```html
  <!-- 숨겨진 필드 -->
  <input type="hidden" name="profileImage" />

  <!-- 에러 메시지가 표시될 프록시 요소 (span, div 등) -->
  <span name="profileImage_error" style="color: red; font-size: 12px;"></span>
  ```

---

## ⚙️ Requirements (요구사항)

### [English]

This project is built with **JDK 21**, but it can be used reliably in all environments running **Java 17 or higher**.

### [한국어]

본 프로젝트는 **JDK 21** 환경에서 빌드되었으나, **Java 17 이상**의 모든 환경에서 안정적으로 사용할 수 있습니다.

---

## 📜 License & Copyright

### [English]

This library is provided under the **Apache License 2.0**. You are free to use, modify, and distribute this software, provided that you comply with the obligations of the license (such as copyright notice and source code disclosure requirements). For detailed terms and conditions, please refer to the **[LICENSE](./LICENSE)** file.

- **Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)**
- Contact: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**Third-party Notice:** This project uses external libraries. For detailed third-party license notices, please refer to the **[licenses/NOTICE](./licenses/NOTICE)** file.

### [한국어]

본 라이브러리는 **Apache License 2.0** 하에 제공됩니다. 사용자는 라이선스의 의무 사항(저작권 고지, 소스 코드 공개 범위 등)을 준수하는 조건 하에 자유롭게 사용, 수정 및 재배포가 가능합니다. 상세한 조건은 **[LICENSE](./LICENSE)** 파일을 반드시 확인해 주세요.

- **저작권 2020 - 2026 devers2 (이승수, 대한민국 대전)**
- 문의: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**제3자 라이브러리 고지:** 본 프로젝트는 외부 라이브러리를 사용합니다. 상세한 제3자 라이브러리 고지사항은 **[licenses/NOTICE](./licenses/NOTICE)** 파일을 참조해 주세요.

---

s2-validator Version: 1.1.7 (2026-08-12)

[//]: # 'S2_DEPS_INFO_START'

---

**To use certain functionalities (e.g., S2BindValidator), the end-user project must explicitly add the following dependencies to be available at runtime.** Failure to include these dependencies will result in a `java.lang.NoClassDefFoundError` at runtime.

**[For Gradle Users]**

```groovy
dependencies {
    // Essential runtime dependencies for optional functionalities
    implementation 'org.springframework:spring-context:6.2.19'
}
```

[//]: # 'S2_DEPS_INFO_END'
