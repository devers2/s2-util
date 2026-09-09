# S2Util Library - Validator Module (s2-validator)

[English](README.md) | [한국어](README.ko.md)

---

## 📖 개요 (Overview)

**s2-validator** 모듈은 서버와 클라이언트를 아우르는 통합 검증 프레임워크로, 단일 설정으로 양쪽 환경에 동일한 검증 규칙을 적용할 수 있습니다. 유연한 체이닝 API를 제공하며 30가지 이상의 포괄적인 검증 규칙, 조건부 검증, 복잡한 커스텀 규칙, 완전한 국제화 지원(i18n)을 포함합니다. 특히 점 표기법(`user.address.street`)과 컬렉션 항목(`items[0]`)으로 중첩 객체를 검증하는 데 탁월하여, 복잡한 DTO/VO 계층 구조에 이상적입니다.

---

## ✨ 주요 기능 (Key Features)

1. **유연한 검증 체인 API (Fluent Validation Chain API)**
   - 자연스러운 읽기 쉬운 검증 규칙: `.field("fieldName").rule(S2RuleType.REQUIRED).ko("필수입력").en("Required")`
   - 순차적 규칙 적용을 위한 체이닝 메서드
   - 여러 규칙 체이닝: 여러 필드 또는 같은 필드의 여러 규칙을 동시에 검증 (예: REQUIRED와 LENGTH)
   - `when()` 조건부 검증: 조건 충족 시에만 규칙 적용

2. **성능 최적화 (Performance Optimization)**
   - **MethodHandle을 활용한 고성능 리플렉션**: MethodHandle 캐싱을 통해 리플렉션 병목 현상을 제거하고, JIT 컴파일러 최적화를 통해 네이티브에 가까운 성능 구현
   - **Caffeine을 활용한 지능형 캐싱**: W-TinyLFU 알고리즘으로 최적의 적중률 달성; 트래픽 급증 시에도 중요 데이터 축출 방지; 자동 캐시 최적화
   - **자바 버전별 적응형 스레드 팩토리**: Java 21 이상 환경에서 가상 스레드 지원; 이전 버전에서는 최적화된 플랫폼 스레드 풀 사용
   - **패턴 캐싱**: 정규식 패턴을 캐싱하여 반복 검증 시 컴파일 오버헤드 감소

3. **30가지 이상의 내장 규칙 타입 (S2RuleType)**
   - **기본**: REQUIRED, ASSERT_TRUE, ASSERT_FALSE, EQUALS_FIELD
   - **문자열**: LENGTH, MIN_LENGTH, MAX_LENGTH, REGEX
   - **숫자**: MIN_VALUE, MAX_VALUE, NUMBER, MIN_BYTE, MAX_BYTE
   - **형식**: EMAIL, URL, INTERNATIONAL_TEL_NO
   - **한국 전용 형식** 🇰🇷: MPHONE_NO, TEL_NO, ZIP, BIZRNO, NWINO, JUMIN, PASSWORD_ANSWR
   - **날짜**: DATE, DATE_AFTER, DATE_BEFORE
   - **텍스트**: TEXT_INTACT, TEXT_COMBINE
   - **커스텀**: 애플리케이션 특화 검증 로직을 위한 CustomRule

4. **크로스 플랫폼 검증 (Cross-Platform Validation)**
   - 서버 측 검증(Java)을 위한 단일 설정
   - 클라이언트 측 검증 규칙 생성(JavaScript, TypeScript 등)
   - 플랫폼 전반에 걸친 일관된 검증 동작
   - 통일된 에러 메시징을 위한 메시지 템플릿 지원

5. **고급 중첩 객체 지원 (Advanced Nested Object Support)**
   - 점 표기법: `user.address.street`, `employee.department.manager.name`
   - 대괄호 표기법: `items[0].name`, `matrix[1][2]`, `users[0].roles[1]`
   - 혼합 표기법: `company.departments[0].employees[1].salary`
   - Optional, List, Array 자동 순회 지원
   - 즉시 검증과 지연 검증 모두 지원

6. **포괄적인 국제화(i18n) 및 한국어 조사 지원**
   - 모든 내장 규칙에 한국어/영어 기본 메시지 탑재
   - 커스텀 메시지가 필요한 경우에만 `.ko()`, `.en()`으로 재정의
   - `.message(Locale, String)` 메서드로 제3의 언어(일본어, 중국어 등) 확장 지원
   - `S2ResourceBundle` 통합으로 중앙 집중식 메시지 관리
   - 메시지 매개변수 치환: `{0}`(필드 라벨), `{1}`(규칙 기준값)
   - 한국어 조사 자동 치환: `{0|은/는}`, `{0|이/가}`, `{0|을/를}`, `{0|과/와}`로 문법에 맞는 메시지 자동 생성

7. **커스텀 및 조건부 검증 (Custom & Conditional Validation)**
   - 사용자 정의 검증 로직을 위한 람다(`Predicate`, `BiPredicate`) 지원
   - 독립 객체로의 재사용 가능한 검증 규칙
   - `when()`과 `and()`를 연결하여 복잡한 AND 조건 구현
   - 다중 `when()` 체인으로 OR 조건 구현

8. **에러 처리 및 보고 (Error Handling & Reporting)**
   - 포괄적인 에러 정보: 필드명, 에러 코드, 에러 메시지, 기본 메시지
   - 유연한 에러 처리를 위한 Consumer 기반 에러 핸들러
   - Fail-fast 모드 vs 모든 에러 수집 모드
   - 중첩 객체 검증 시 안전한 순환 참조 검출

9. **Spring Framework 통합 (선택사항)**
   - `S2BindValidator`로 Spring Data Binding과 `BindingResult` 연동
   - Supplier 패턴을 통한 지연 평가(Lazy Evaluation)와 편리한 규칙 관리
   - 통합 검증 규칙: 서버와 클라이언트 동일 룰셋 적용
   - Spring 표준 에러 처리로 자동 에러 필드 바인딩

---

## 🚀 빠른 시작 가이드 (Quick Start)

### 1. 설치 (Installation)

`build.gradle` 또는 `pom.xml`에 다음 의존성을 추가합니다.

**[Gradle]**

```groovy
dependencies {
    implementation 'io.github.devers2:s2-validator:1.1.7'

    // (선택사항) Spring 연동 기능(S2BindValidator) 사용 시에만 필요
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

#### 선택 사항: s2-validator-plugin (빌드 시점 필드명 검증)

**s2-validator-plugin** Gradle 플러그인을 선택적으로 추가하면 필드명 오타를 **빌드(컴파일) 시점에** 잡아낼 수 있습니다. 런타임 에러로 이어지기 전에 미리 방어할 수 있습니다.

**[`settings.gradle`]**

```groovy
pluginManagement {
    repositories {
        mavenCentral()
    }
}
```

**[`build.gradle`]**

```groovy
plugins {
    id 'io.github.devers2.validator' version '1.1.2'
}
```

> [!NOTE]
> 이 플러그인은 라이브러리 의존성이 아닌 **Gradle 플러그인**입니다. `dependencies {}` 블록이 아닌 `plugins {}` 블록에 추가해야 합니다. Maven은 지원하지 않습니다.

---

### 2. 사용법 (Usage)

#### 2.1. 기본 검증 (즉시 검증 vs 재사용 설계도)

- **즉시 검증 모드 (`S2Validator.of`)**: 검증 대상 객체(DTO/VO 또는 Map)에 대해 1회성 검증을 즉시 수행합니다. 에러 핸들러(`Consumer<S2ValidationError>`)를 통해 모든 오류를 리스트로 수집하거나, fail-fast 모드(`validate()`)로 첫 번째 에러 발생 시 `S2RuntimeException` 예외를 즉시 던질 수 있습니다.
- **설계도 / 빌더 모드 (`S2Validator.builder`)**: 검증 로직을 재사용 가능한 '설계도(Blueprint)'로 정의합니다. 빌드된 `S2Validator` 인스턴스는 스레드 안전(Thread-safe)하여 고동시성 환경에서 캐싱 및 반복 재사용에 최적화되어 있습니다.

##### 즉시 검증 모드 (`S2Validator.of`)

```java
Map<String, Object> data = new HashMap<>();
data.put("userId", "admin");
data.put("age", 20);
data.put("email", "test@s2.kr");

List<S2ValidationError> errors = new ArrayList<>();

// 1) 모든 에러를 핸들러로 수집
boolean isValid = S2Validator.of(data)
    .field("userId", "아이디").rule(S2RuleType.REQUIRED)
    .field("age", "나이").rule(S2RuleType.MIN_VALUE, 19)
    .field("email", "이메일").rule(S2RuleType.EMAIL)
    .validate(errors::add, Locale.KOREAN);

if (!isValid) {
    errors.forEach(err -> System.out.println(err.fieldName() + ": " + err.defaultMessage()));
}

// 2) Fail-fast 모드: 첫 번째 에러 발생 시 S2RuntimeException 던짐
S2Validator.of(data)
    .field("userId", "아이디").rule(S2RuleType.REQUIRED)
    .field("email", "이메일").rule(S2RuleType.EMAIL)
    .validate();
```

##### 설계도 / 빌더 모드 (`S2Validator.builder`)

```java
// 재사용 가능한 검증기 설계도 정의
S2Validator<UserVO> userValidator = S2Validator.<UserVO>builder()
    .field("userId", "아이디").rule(S2RuleType.REQUIRED)
    .field("userPw", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
    .field("email", "이메일").rule(S2RuleType.EMAIL)
    .build();

// 요청 및 스레드 간 안전하게 반복 재사용
UserVO user = new UserVO();
user.setUserId("user01");
user.setUserPw("pass1234");
user.setEmail("user01@s2.kr");

List<S2ValidationError> errors = new ArrayList<>();
boolean isValid = userValidator.validate(user, errors::add, Locale.KOREAN);
```

---

#### 2.2. 단일 값 검증 (`S2Validator.check`)

DTO나 Map 객체를 생성하지 않고 개별 변수나 값 하나만을 신속하게 검증할 때 사용합니다:
- **Boolean 모드 (`check(value)`)**: 예외 발생 없이 `true`/`false` 불린 결과를 반환합니다.
- **라벨 지정 예외 모드 (`check(value, label)`)**: 검증 실패 시 라벨이 반영된 다국어 메시지와 함께 `S2RuntimeException` 예외를 던집니다.
- **커스텀 Predicate 검증**: 람다 표현식을 통해 원하는 검증 로직을 즉시 적용할 수 있습니다.

```java
// 1) Boolean 모드: 예외 없이 true/false 반환
boolean isValidEmail = S2Validator.check("test@s2.kr")
    .rule(S2RuleType.REQUIRED)
    .rule(S2RuleType.EMAIL)
    .validate();

// 2) 라벨 지정 예외 모드: 실패 시 S2RuntimeException 발생
S2Validator.check(userInput, "이름")
    .rule(S2RuleType.REQUIRED) // 기본 메시지 사용 또는 .ko()로 커스텀 가능
    .validate();

// 3) 커스텀 Predicate 검증
boolean isAdult = S2Validator.check(25)
    .rule((Integer age) -> age >= 19)
    .validate();
```

---

#### 2.3. 교차 필드 검증 (Cross-Field Validation)

두 개 이상의 필드 간 상관관계(날짜 선후 관계, 비밀번호 일치 여부 등)를 검증합니다:
- **타입 기반 규칙**: 내장된 `DATE_AFTER`, `DATE_BEFORE`, `EQUALS_FIELD` 등을 사용합니다.
- **BiPredicate 커스텀 규칙**: 필드 값과 대상 객체 전체를 전달받아 `(value, target) -> boolean` 형태로 자유롭게 교차 검증합니다.

```java
Map<String, Object> form = new HashMap<>();
form.put("startDate", "2025-01-01");
form.put("endDate", "2025-01-10");
form.put("password", "s2secret123");
form.put("confirmPassword", "s2secret123");

// 1) 내장 타입 기반 교차 검증
S2Validator.of(form)
    .field("startDate", "시작일").rule(S2RuleType.DATE_BEFORE, "endDate")
    .field("endDate", "종료일").rule(S2RuleType.DATE_AFTER, "startDate")
    .field("confirmPassword", "비밀번호 확인").rule(S2RuleType.EQUALS_FIELD, "password")
    .validate(errors::add, Locale.KOREAN);

// 2) BiPredicate 람다 기반 커스텀 교차 검증 (value, target)
S2Validator.of(form)
    .field("confirmPassword", "비밀번호 확인")
    .rule((value, target) -> {
        String password = S2Util.getValue(target, "password", "");
        return password.equals(value);
    }).ko("{0|은/는} 원본 비밀번호와 일치해야 합니다.")
    .validate(errors::add, Locale.KOREAN);
```

---

#### 2.4. 조건부 검증 (`when`, `and`)

지정한 조건이 충족될 때만 해당 필드의 유효성 검증을 실행합니다 (`String`, `Boolean`, `Number` 등 다양한 타입 지원):
- **단일 조건**: `.when("fieldName", value)`
- **AND 조건 (모두 만족)**: `.when(...).and(...)`
- **OR 조건 (하나라도 만족)**: 다중 `.when(...)` 체이닝

```java
S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
    // 단일 조건: isPremium == true 일 때만 필수
    .field("premiumEmail", "프리미엄 이메일")
        .when("isPremium", true)
        .rule(S2RuleType.REQUIRED)

    // AND 조건: useAdvanced == 'Y' AND advancedMode == 'FULL'
    .field("advancedOption", "고급 옵션")
        .when("useAdvanced", "Y").and("advancedMode", "FULL")
        .rule(S2RuleType.REQUIRED)

    // OR 조건: useAdvanced == 'Y' OR isTest == 'true'
    .field("otherOption", "기타 옵션")
        .when("useAdvanced", "Y").when("isTest", "true")
        .rule(S2RuleType.REQUIRED)
    .build();
```

---

#### 2.5. 중첩 객체 및 컬렉션 검증 (Nested Objects & Collections)

복잡한 계층 구조의 객체 그래프와 컬렉션을 손쉽게 검증합니다:
- **점 표기법 (`user.address.street`) 및 대괄호 인덱스 (`items[0].name`)**
- **와일드카드 문법 (`products[].name`)**: 컬렉션/배열의 모든 요소를 자동 순회하며 검증합니다. 아이템별 조건부 검증(`.when("products[].type", "NORMAL")`)도 완벽히 지원합니다.
- **하위 검증기 재귀 검증 (`NESTED` & `EACH`)**: 독립된 하위 검증기 블루프린트를 중첩 단일 객체(`NESTED`) 또는 컬렉션 항목(`EACH`) 검증에 재사용합니다.

```java
// 1) 점 표기법 및 대괄호 표기법
S2Validator<Map<String, Object>> dotValidator = S2Validator.<Map<String, Object>>builder()
    .field("user.address.street", "도로명 주소").rule(S2RuleType.REQUIRED)
    .field("users[0].name", "첫 번째 사용자명").rule(S2RuleType.REQUIRED)
    .build();

// 2) 와일드카드 문법: 컬렉션/배열 전체 자동 순회 검증
S2Validator<Map<String, Object>> wildcardValidator = S2Validator.<Map<String, Object>>builder()
    .field("products[].name", "상품명").rule(S2RuleType.REQUIRED)
    .field("products[].price", "가격").rule(S2RuleType.MIN_VALUE, 0)
    .when("products[].type", "NORMAL") // 와일드카드 필드별 개별 조건부 검증 지원
    .build();

// 3) 하위 검증기를 활용한 재귀 검증 (NESTED & EACH)
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

#### 2.6. 기본 내장 메시지 및 다국어 커스터마이징 (Built-in Messages & Localization)

- **기본 내장 메시지 완비**: `S2RuleType`의 모든 내장 규칙은 한국어와 영어 기본 메시지를 이미 기본 탑재하고 있습니다 (예: `REQUIRED`는 한국어 로케일에서 `"{0|은/는} 필수 입력 항목입니다."`, 영어 로케일에서 `"{0} is required."` 자동 생성). **따라서 일반적인 상황에서는 `.ko()`나 `.en()`을 호출할 필요가 없습니다.**
- **커스텀 메시지 재정의 (`.ko()`, `.en()`)**: 기본 메시지 대신 특정 문구를 사용하고 싶을 때만 `.ko("...")` 또는 `.en("...")`을 체이닝하여 메시지를 재정의합니다.
- **기타 언어 지원 (`.message(Locale, String)`)**: 한국어/영어 외의 다국어(일본어, 중국어, 프랑스어 등) 메시지가 필요할 때는 `.message(Locale locale, String template)` 메서드를 사용하여 원하는 로케일의 메시지를 자유롭게 등록할 수 있습니다 (예: `.message(Locale.JAPANESE, "{0}は必須入力項目です。")`).
- **메시지 플레이스홀더**:
  - `{0}`: 필드 라벨 (예: "사용자명", "아이디").
  - `{1}`: 규칙 기준값 (예: 최소 자릿수, 최소 금액 등).
- **한국어 자동 조사 지원**: `{0|은/는}`, `{0|이/가}`, `{0|을/를}`, `{0|과/와}` 문법을 지원하여 라벨의 받침 유무에 따라 문법에 맞는 조사를 자동으로 선택·치환합니다.
- **전역 리소스 번들 연동**: `S2Validator.setValidationBundle("messages/validation")`을 설정하면 프로퍼티 파일(`messages/validation_ko.properties` 등)을 통한 중앙 집중식 메시지 관리가 가능합니다.

```java
S2Validator<UserVO> validator = S2Validator.<UserVO>builder()
    // 1) 기본 메시지 사용: 별도 .ko()/.en() 호출 불필요!
    //    KO -> "아이디는 필수 입력 항목입니다." | EN -> "아이디 is required."
    .field("userId", "아이디").rule(S2RuleType.REQUIRED)

    // 2) 커스텀 메시지 오버라이딩 (.ko, .en)
    .field("userPw", "비밀번호")
        .rule(S2RuleType.MIN_LENGTH, 8)
            .ko("{0|은/는} 보안을 위해 최소 {1}자 이상이어야 합니다.")
            .en("{0} must be at least {1} characters for security.")

    // 3) 기타 다국어 지원 (.message)
    .field("email", "이메일")
        .rule(S2RuleType.EMAIL)
            .message(Locale.JAPANESE, "{0}の形式が正しくありません。")
            .message(Locale.SIMPLIFIED_CHINESE, "{0}格式不正确。")
    .build();

// 전역 리소스 번들 연동 (예: messages/validation_ko.properties)
S2Validator.setValidationBundle("messages/validation");
```

---

#### 2.7. Spring Framework 연동 (`S2BindValidator`)

S2Util 검증 로직을 Spring MVC의 `BindingResult`와 자연스럽게 연결하고, `getRulesJson()`을 통해 클라이언트 JavaScript(`s2.validator.js`)와 동일한 검증 규칙을 완벽하게 공유합니다.

```java
@Controller
@RequestMapping("/member")
public class MemberController {

    // 1) 검증 규칙 Supplier 정의 (최초 1회 컴파일 후 캐싱)
    private S2Validator<MemberDTO> memberRules() {
        return S2Validator.<MemberDTO>builder()
            .field("userId", "아이디").rule(S2RuleType.REQUIRED)
            .field("userPw", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
            .field("confirmPw", "비밀번호 확인")
                .rule((value, target) -> S2Util.getValue(target, "userPw", "").equals(value))
                .ko("비밀번호가 일치하지 않습니다.")
            .field("email", "이메일").rule(S2RuleType.EMAIL)
            .build();
    }

    // 2) GET: 클라이언트 검증 규칙 JSON을 뷰로 전달
    @GetMapping("/join")
    public String joinForm(Model model) {
        model.addAttribute("member", new MemberDTO());
        // s2.validator.js 연동용 JSON 메타데이터 생성
        String rulesJson = S2BindValidator.context("MEMBER_JOIN", this::memberRules).getRulesJson();
        model.addAttribute("validationRules", rulesJson);
        return "member/join";
    }

    // 3) POST: 서버 측 검증 수행 및 BindingResult 자동 바인딩
    @PostMapping("/join")
    public String joinSubmit(@ModelAttribute MemberDTO member, BindingResult result, Model model) {
        // 서버 검증 실행: 오류 발생 시 BindingResult에 필드 에러 자동 등록
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

---

##### 클라이언트 뷰 연동 (Thymeleaf & HTML 가이드)

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

    <!-- s2.validator.js 임포트 -->
    <script type="module" th:src="@{/s2-util/js/s2.validator.js}"></script>
</body>
</html>
```

###### 2) JavaScript 파일 임포트 방법 및 동작 원리

> [!NOTE]
> **JavaScript 파일을 별도로 다운로드하거나 프로젝트에 복사하지 않아도 동작하는 이유**
>
> Servlet 3.0+ 표준 사양 및 Spring Boot의 기본 정적 리소스(Static Resource) 처리 메커니즘에 따라, 의존성 라이브러리(JAR) 내부의 `META-INF/resources/` 경로에 위치한 파일들은 웹 애플리케이션의 루트(`/`) 정적 자원으로 자동 서빙됩니다.
> 
> `s2-validator` 라이브러리 JAR 내부에 `META-INF/resources/s2-util/js/s2.validator.js`가 패키징되어 있으므로, 개발자가 JS 파일을 따로 프로젝트의 `src/main/resources/static` 등으로 복사할 필요 없이 브라우저에서 `/s2-util/js/s2.validator.js` URL로 바로 접근할 수 있습니다.

**권장 임포트 방식:**

- **방식 A (가장 권장 - Thymeleaf `th:src`)**:
  배포 환경의 Context Path(예: `/`, `/my-app`)를 Thymeleaf의 `@{...}` 문법이 자동으로 보정해 주므로 가장 안전하고 권장되는 방식입니다.
  ```html
  <script type="module" th:src="@{/s2-util/js/s2.validator.js}"></script>
  ```

- **방식 B (인라인 스크립트 동적 import)**:
  인라인 `<script>` 태그 안에서 모듈을 동적으로 임포트할 때 유용합니다.
  ```html
  <script type="module" th:inline="javascript">
      const contextPath = /*[[@{/}]]*/ '';
      import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);
  </script>
  ```

- **방식 C (importmap 활용 - AJAX 수동 검증 시 유용)**:
  ES 모듈의 깔끔한 임포트를 지원하며, AJAX 수동 검증 등에서 모듈 객체를 직접 참조할 때 유용합니다.
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

###### 3) 클라이언트 자동 검증 동작 방식 (자바스크립트 코드 한 줄 없이 폼 검증)

`s2.validator.js`는 파일이 로드되는 순간 내부의 `initS2Validator()`가 자체 실행되어 필요한 이벤트 리스너를 자동 등록합니다. 따라서 개발자가 **추가적인 자바스크립트 코드를 전혀 작성하지 않아도** 다음과 같은 과정이 100% 자동으로 처리됩니다:

1. **브라우저 기본 네이티브 제약 검증 비활성화 (`noValidate`)**: `data-s2-rules` 속성이 선언된 모든 `<form>`에 `form.noValidate = true`를 자동 적용하여 브라우저 고유의 투박한 기본 영문 검증 팝업을 차단하고 S2Validator가 전담하도록 설정합니다 (`MutationObserver`로 동적 폼까지 실시간 감지).
2. **폼 전송(submit) 이벤트 자동 가로채기**: 사용자가 제출 버튼을 누르거나 Enter 키로 폼 제출을 시도할 때 `document` 레벨에서 이벤트를 가로채고, `data-s2-rules`의 JSON 규칙을 파싱하여 현재 입력값을 즉시 검사합니다.
3. **에러 발생 시 전송 차단 & 네이티브 말풍선(툴팁) 안내**: 검증 규칙 위반이 1건이라도 발생하면 즉시 `e.preventDefault()`로 **서버 전송을 차단**합니다. 이후 오류가 발생한 첫 번째 필드로 **자동 포커스(Focus)**를 이동시키고, 한국어/다국어 안내 메시지를 **브라우저 네이티브 말풍선(툴팁)**으로 띄워줍니다.
4. **실시간 에러 해제 (`input` / `change`)**: 사용자가 잘못 입력된 필드에 다시 키보드로 입력을 시작(`input`)하거나 라디오/체크박스를 선택(`change`)하는 순간, 해당 필드의 에러 상태를 즉시 해제합니다.

###### 4) 초보자를 위한 실무 팁

- **Tip 1: 비동기 통신(AJAX / Fetch) 시 수동 검증**:
  페이지 새로고침 없는 비동기 API 통신(`fetch` / `axios`)을 수행할 때는 `S2Validator.validate()`를 직접 호출하여 수동으로 검증합니다:
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

- **Tip 2: 숨겨진 필드를 위한 프록시 에러 요소 (`{fieldName}_error`)**:
  `<input type="hidden">`이나 커스텀 파일 업로드 컴포넌트처럼 화면에 보이지 않아 브라우저 툴팁을 띄울 수 없는 숨김 요소의 경우, `{fieldName}_error`라는 `name`을 가진 프록시 엘리먼트를 폼 안에 두면 해당 요소에 에러 메시지가 텍스트로 자동 출력됩니다:
  ```html
  <!-- 숨겨진 필드 -->
  <input type="hidden" name="profileImage" />

  <!-- 에러 메시지가 표시될 프록시 요소 (span, div 등) -->
  <span name="profileImage_error" style="color: red; font-size: 12px;"></span>
  ```

---

#### 2.8. 빌드 시점 필드명 검증 (`s2-validator-plugin`)

선택 사항으로 **s2-validator-plugin**을 추가하면 (설치 방법은 [설치 섹션](#1-설치-installation) 참고), 플러그인이 **`compileJava` 실행 전** 프로젝트 소스 코드 전체를 **정적 분석(AST 기반)**합니다. 코드 내에 있는 모든 `.field("fieldName")` 호출을 탐색하여 해당 필드가 대상 클래스에 실제로 존재하는지 검사합니다.

**잡아낼 수 있는 오류:**

- 필드명 오타 (예: 실제 필드는 `userName`인데 `.field("userNaem")`으로 잘못 입력)
- DTO/VO 클래스에 존재하지 않는 필드 참조
- 클래스 이름 변경 또는 리팩토링 후 남겨진 잘못된 필드명

**예시 — 빌드가 즉시 실패하며 명확한 에러를 표시:**

```
> Task :compileJava FAILED

error: [S2Validator] Field validation failed:
  'address' 필드가 UserDTO에 없습니다
  -> UserController.java:42: .field("address")

  Possible fix: Did you mean 'addressInfo'?
```

**주요 특징:**

- 별도 설정 없음 — `compileJava`, `check`, `bootRun` 태스크 실행 시 자동으로 동작
- 클래스 상속 지원: 부모 클래스에 선언된 필드도 함께 검사
- 멀티 프로젝트 빌드 환경에서도 동작
- `s2-validator` 1.1.0+, Java 17+, Gradle 8.0+ 필요

플러그인 전체 문서는 [s2-validator-plugin README](../s2-validator-plugin/README.md)를 참조하세요.

---

## ⚙️ 요구사항 (Requirements)

본 프로젝트는 **JDK 21** 환경에서 빌드되었으나, **Java 17 이상**의 모든 환경에서 안정적으로 사용할 수 있습니다.

---

## 📜 라이선스 및 저작권 (License & Copyright)

본 라이브러리는 **Apache License 2.0** 하에 제공됩니다. 사용자는 라이선스의 의무 사항(저작권 고지, 소스 코드 공개 범위 등)을 준수하는 조건 하에 자유롭게 사용, 수정 및 재배포가 가능합니다. 상세한 조건은 **[LICENSE](./LICENSE)** 파일을 반드시 확인해 주세요.

- **저작권 2020 - 2026 devers2 (이승수, 대한민국 대전)**
- 문의: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**제3자 라이브러리 고지:** 본 프로젝트는 외부 라이브러리를 사용합니다. 상세한 제3자 라이브러리 고지사항은 **[licenses/NOTICE](./licenses/NOTICE)** 파일을 참조해 주세요.

---

s2-validator Version: 1.1.7 (2026-08-12)

[//]: # 'S2_DEPS_INFO_START'

---

**특정 기능(예: S2BindValidator)을 사용하려면 런타임에 다음 의존성을 엔드유저 프로젝트에 명시적으로 추가해야 합니다.** 이 의존성이 누락되면 런타임에 `java.lang.NoClassDefFoundError`가 발생합니다.

**[Gradle 사용자]**

```groovy
dependencies {
    // 선택적 기능을 위한 필수 런타임 의존성
    implementation 'org.springframework:spring-context:6.2.19'
}
```

[//]: # 'S2_DEPS_INFO_END'
