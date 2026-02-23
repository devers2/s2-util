# S2Util User Manual (사용자 메뉴얼) 🚀

> **Write Once, Validate Anywhere.**
> S2Util is a modularized utility ecosystem designed to harmonize Server (Java) and Client (JavaScript) validation while providing near-native object manipulation and robust dynamic query generation.
> <br>S2Util은 서버(Java)와 클라이언트(JavaScript) 간의 검증 로직을 완벽하게 동기화하고, 리플렉션 없이 고성능 객체 조작 및 유연한 동적 쿼리 생성을 지원하는 통합 유틸리티 생태계입니다.

---

## 🏗️ 1. Installation & Infrastructure (설치 및 기초 설정)

### 1-1. Dependencies & Components (의존성 및 주요 컴포넌트)

#### 🎯 **Quick Start: All-in-One (모든 기능 한번에)**

Add **only one dependency** to unlock all functionality.
<br>**한 가지 의존성만 추가하면 모든 기능을 즉시 사용할 수 있습니다.**

```groovy
dependencies {
    // 🚀 S2Util 통합 패키지: 모든 모듈이 포함되어 있으며, 필요한 부분만 선택적으로 사용
    // [English] Includes: S2Validator, S2Jpql, S2Copier (Simply use what you need)
    // [한국어] 포함: S2Validator, S2Jpql, S2Copier (필요한 것만 선택적으로 사용)
    implementation 'io.github.devers2:s2-util:1.1.6'
}
```

> **[English]** S2Util is a **unified distribution** containing all modules pre-integrated. You get all capabilities without extra configuration.
> <br>**[한국어]** S2Util은 모든 모듈이 미리 통합되어 배포되므로, 추가 설정 없이 즉시 모든 기능을 사용할 수 있습니다.

---

#### 🧩 **Selective & Lightweight (선택적 경량 사용)**

S2Util is highly modular. For **minimal footprint**, add only the specific components you need.
<br>**경량 구조**를 원한다면, 필요한 기능별로 최소 의존성만 추가하세요.

| Component (컴포넌트) | Minimum Dependency (최소 의존성) | Direct Dependencies (직접 의존성) | Key Functionality (주요 기능)                                                                                      |
| :------------------- | :------------------------------- | :-------------------------------- | :----------------------------------------------------------------------------------------------------------------- |
| **S2Validator**      | `s2-validator`                   | (자동으로 s2-core 포함)           | **Unified Validation**: Server-Client synchronized rules.<br>서버-클라이언트 통합 검증 엔진                        |
| **S2BindValidator**  | `s2-validator`                   | (자동으로 s2-core 포함)           | **Spring Integration**: Seamless mapping to `BindingResult`.<br>스프링 표준 BindingResult 매핑 지원                |
| **S2Jpql**           | `s2-jpa`                         | (자동으로 s2-core 포함)           | **Dynamic Query**: Secure, template-based JPA query building.<br>안전한 템플릿 기반 동적 JPQL 생성 (JPA 설정 필요) |
| **S2Copier**         | `s2-core`                        | -                                 | **High-Perf Mapping**: Reflection-free DTO/Entity data syncing.<br>리플렉션 프리 고성능 객체 매핑                  |

> **[English]** Each module declares `api project(':s2-core')`, so transitive dependencies are automatically included when you add a sub-module.
> <br>**[한국어]** 각 모듈이 s2-core를 `api` 의존성으로 선언하고 있어, 서브 모듈 추가 시 자동으로 포함됩니다.

```groovy
dependencies {
    // [English] Option 1: Only validation needed
    // [한국어] 선택지 1: 검증 기능만 필요한 경우
    implementation 'io.github.devers2:s2-validator:1.1.6'  // (s2-core 자동 포함)

    // [English] Option 2: Only JPA dynamic queries needed
    // [한국어] 선택지 2: 동적 쿼리 기능만 필요한 경우
    implementation 'io.github.devers2:s2-jpa:1.1.6'        // (s2-core 자동 포함)

    // [English] Option 3: Only core features needed (most lightweight)
    // [한국어] 선택지 3: 객체 복사를 포함한 핵심 기능만 필요한 경우 (가장 경량)
    implementation 'io.github.devers2:s2-core:1.1.6'
}
```

### 1-2. S2Validator Static Analysis Plugin (정적 분석 플러그인) ✨

**Stop Typos at Source.** This plugin verifies field names in your `S2Validator` definitions during the build process.
<br>`S2Validator`에서 사용하는 필드명을 빌드 타임에 정적으로 검증합니다. 존재하지 않는 필드를 참조할 경우 즉시 빌드 에러를 발생시켜 런타임 오류를 완벽하게 예방합니다.

> [!IMPORTANT]
> **Static analysis is available only when using Generics** (e.g., `S2Validator.<UserDTO>builder()`). The plugin uses the generic type information to map and verify field names.
> <br>**정적 분석은 제네릭을 사용했을 때만 수행 가능합니다** (예: `S2Validator.<UserDTO>builder()`). 플러그인은 제너릭에 명시된 타입 정보를 바탕으로 필드 존재 여부를 확인합니다.

```groovy
plugins {
    id 'io.github.devers2.validator' version '1.1.6'
}
```

### 1-3. Global Configuration (시스템 연동) - [Optional]

Register a global `ResourceBundle`. This is only required if you intend to use **Message Keys** from properties files.
<br>메시지 번들(ResourceBundle)을 선택적으로 설정합니다. 메시지 프로퍼티의 **키(Key)**를 사용하여 에러 메시지를 관리할 때만 설정하면 됩니다.

```java
// [English] Configuration to use keys from messages.properties
// [한국어] messages.properties에 정의된 키를 사용하기 위한 설정
S2BindValidator.setValidationBundle("messages");

// Example Usage (예시):
// messages.properties -> err.required={0|은/는} 필수값입니다.
.field("id", "아이디").rule(S2RuleType.REQUIRED, null, "err.required")
```

---

## 2. S2Validator: The 4 Strategic Patterns (검증 전략 패턴) 🚀

### A. Pattern: Immediate Mode (즉각적인 검증 패턴)

**Usage:** `S2Validator.of(target, [failFast])`

For quick, one-off validation within a method.
<br>특정 로직 내부에서 1회성으로 사용되는 즉각적인 검증에 사용합니다.

> [!NOTE]
> **[English]** Default `of(target)` throws an `S2RuntimeException` on failure. Use `of(target, false)` to receive a `boolean` result.
> <br>**[한국어]** 기본 `of(target)`은 실패 시 `S2RuntimeException`을 발생시킵니다. 예외 대신 `true/false` 결과가 필요하면 `of(target, false)`를 사용하세요.

```java
// 1. Exception Mode (Default)
// [한국어] 검증 실패 시 호출 즉시 S2RuntimeException 발생
S2Validator.of(userInput).field("email").rule(S2RuleType.EMAIL).validate();

// 2. Boolean Mode
// [English] Returns true/false instead of throwing an exception
// [한국어] 예외를 던지는 대신 검증 결과의 성공/실패 여부를 논리값으로 획득
boolean isValid = S2Validator.of(userInput, false)
    .field("age").rule(S2RuleType.MIN_VALUE, 20)
    .validate();
```

### B. Pattern: Blueprint Mode (검증 설계도 패턴)

**Usage:** `S2Validator.builder()`

Defines a reusable, thread-safe validator.
<br>재사용 가능한 검증 설계도(Blueprint)를 정의하여 여러 객체에 반복 적용합니다.

```java
// [English] Define a reusable validation blueprint
// [한국어] 재사용 가능한 검증 설계도 정의
S2Validator<UserDTO> schema = S2Validator.<UserDTO>builder()
    .field("id", "아이디").rule(S2RuleType.REQUIRED)
    .build();

// [English] Execute validation on target instances
// [한국어] 검증 대상을 인자로 담아 설계도를 실행하여 검증
schema.validate(userA);
schema.validate(userB);
```

### C. Pattern: Registry Mode (중앙 관리 패턴)

**Usage:** `S2ValidatorFactory.getOrRegister()`

Global singleton caching. The construction logic executes only once.
<br>검증기를 전역 저장소에 캐싱합니다. 생성 로직은 최초 1회만 실행되어 성능이 극대화됩니다.

```java
// [English] Centralized Caching and Reuse
// [한국어] 중앙 집중식 캐싱 및 재사용
S2Validator<UserDTO> v = S2ValidatorFactory.getOrRegister("JOIN_RULES", () ->
    S2Validator.<UserDTO>builder().field("name").rule(S2RuleType.REQUIRED).build()
);
```

### D. Pattern: Spring Standard Alignment (스프링 표준 통합 패턴)

**Usage:** `S2BindValidator.context()`

Seamlessly maps S2Util results to Spring's standard `BindingResult`.
<br>S2Util의 검증 결과를 스프링 표준 객체인 `BindingResult`로 자동 매핑합니다.

```java
@PostMapping("/join")
public String join(@ModelAttribute UserDTO user, BindingResult result) {
    // [English] Bridges S2Validator with Spring ecosystem
    // [한국어] S2Validator와 스프링 생태계 연결
    S2BindValidator.context("JOIN_CTX", this::joinRules).validate(user, result);

    if (result.hasErrors()) {
        return "joinForm"; // Standard Spring error handling flow
    }
    return "redirect:/success";
}
```

---

## 3. Messaging & I18n (메시지 및 다국어 처리) 🌍

### 3-1. Inline Localization (.en, .ko, .message)

Specify messages or keys directly in the chain.
<br>체이닝 과정에서 다국어 메시지나 메시지 키를 즉시 설정합니다.

```java
.field("age", "나이")
    // [English] (1) Use Message Key (Requires setValidationBundle setup)
    // [한국어] (1) 메시지 키 사용 (setValidationBundle 설정 필요)
    .rule(S2RuleType.MIN_VALUE, 19, "err.key.adult")
    // [English] (2) Language specific strings
    // [한국어] (2) 언어별 명시적 메시지 설정
    .ko("성인만 가입 가능합니다.")
    .en("Only adults are allowed.")
    .message(Locale.FRANCE, "Seuls les adultes...")
```

### 3-2. Korean Particle Handling (한국어 조사 자동 선택) 🇰🇷

Automatically selects 은/는, 이/가 based on the field label.
<br>라벨 단어에 맞춰 적절한 조사를 자동으로 선택하여 자연스러운 메시지를 생성합니다.

```java
.field("id", "아이디").ko("{0|은/는} 필수입니다.") // -> "아이디는 필수입니다."
.field("name", "이름").ko("{0|은/는} 필수입니다.") // -> "이름은 필수입니다."
```

---

## 🛠️ 4. Advanced Validation Mechanics (고급 검증 기법)

### 4-1. Object Graph Navigation (경로 탐색)

- **Dot (`.`)**: Deep traversal (e.g., `user.address.street`).
  <br>**점 표기법**: 중첩 객체 탐색
- **Bracket (`[n]`)**: Specific index access (e.g., `orders[0].id`).
  <br>**인덱스 표기법**: 리스트/배열의 특정 순번 접근
- **Wildcard (`[]`)**: List-wide validation (e.g., `items[].price` validates every price in the list).
  <br>**와일드카드**: 컬렉션 내 모든 요소를 일괄 검증

### 4-2. Recursive Validation (재귀 및 구성 검증)

Reuse existing validators to handle hierarchical data structures.
<br>기존에 정의된 검증기를 다른 검증기의 규칙으로 자식 구성 요소에 재사용합니다.

```java
// [English] 1. Define sub-validator (Blueprint)
// [한국어] 1. 하위 검증기 정의 (설계도)
S2Validator<ItemDTO> subValidator = S2Validator.<ItemDTO>builder()
    .field("name", "상품명").rule(S2RuleType.REQUIRED)
    .field("price", "가격").rule(S2RuleType.MIN_VALUE, 0)
    .build();

// [English] 2. Use in parent validator
// [한국어] 2. 부모 검증기에서 사용
S2Validator.<OrderDTO>builder()
    // [English] Case A: Validate all items in a list (EACH)
    // [한국어] 사례 A: 목록/컬렉션 내 모든 요소 반복 검증 (EACH)
    .field("items", "목록").rule(S2RuleType.EACH, subValidator)

    // [English] Case B: Validate a single nested object (NESTED)
    // [한국어] 사례 B: 단일 중첩 객체 내부 검증 (NESTED)
    .field("info", "정보").rule(S2RuleType.NESTED, subValidator)
    .build();
```

### 4-3. Custom Logic: Predicate & BiPredicate (사용자 정의 로직)

Inject Lambda for business rules. `BiPredicate` allows multi-field comparison.
<br>람다식을 주입하여 복잡한 비즈니스 규칙을 처리합니다.

> [!WARNING]
> **[English] Server-Only Limitation**: Custom Lambda rules are **not** synchronized to JavaScript automatically. For full system-wide synchronization, prefer built-in `S2RuleType` definitions.
> <br>**[한국어] 서버 전용 제약**: 람다 기반의 커스텀 규칙은 클라이언트 JS로 자동 변환되지 않습니다. 서버-클라이언트 전체 동기화가 필요한 경우 가급적 내장된 `S2RuleType`을 사용하세요.

```java
.field("endDate", "종료일")
    .rule((val, target) -> {
        String start = S2Util.getValue(target, "startDate");
        return start.compareTo((String)val) <= 0;
    }).ko("종료일은 시작일 이후여야 합니다.")
```

---

## 💡 5. Unified Integration: Server-Client Sync (서버-클라이언트 통합 검증) ✨

**"Defined Once on Server, Enforced Everywhere."** You can synchronize validation logic between Java and JavaScript with zero extra effort.
<br>**"서버에서 한 번 정의하고 어디서나 검증한다."** S2Util의 핵심 기능입니다. 서버에서 정의한 단 하나의 설계도로 클라이언트와 서버 양쪽에서 동일한 검증 엔진을 구동합니다.

### 5-1. End-to-End Implementation Example (전 과정 구현 예제)

#### 1. [Server] Define Shared Validation Rules (서버: 공통 규칙 정의)

Define your validation blueprint in a method.
<br>재사용을 위해 별도의 메서드에 설계도를 정의합니다.

```java
private S2Validator<UserCommand> signupRules() {
    return S2Validator.<UserCommand>builder()
            .field("userId", "ID").rule(S2RuleType.REQUIRED)
            .field("password", "Password").rule(S2RuleType.REQUIRED).rule(S2RuleType.MIN_LENGTH, 8)
            .field("confirmPw", "Confirm Password")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.EQUALS_FIELD, "password")
                .ko("비밀번호 확인이 일치하지 않습니다.")
            .build();
}
```

#### 2. [Controller] Pattern A: Initial Load (컨트롤러: 초기 로드 - 규칙 전달)

Pass the rules as a JSON string to the client.
<br>`GET` 요청 시 규칙을 JSON으로 추출하여 전달합니다.

```java
@GetMapping("/signup")
public String signupPage(@ModelAttribute("command") UserCommand command, Model model) {
    // [English] Uses the 'Pattern: Registry Mode' internally for performance (caching)
    // [한국어] 내부적으로 '중앙 관리 패턴(Registry)'을 사용하여 성능 최적화(캐싱)가 자동으로 수행됨
    String rules = S2BindValidator.context("signup", this::signupRules).getRulesJson();
    model.addAttribute("rules", rules);
    return "signup";
}
```

#### 3. [View] Pattern B: Client Enforcement (뷰: 클라이언트 검증 자동화)

Inject the JSON into the form.
<br>전달받은 규칙을 폼에 주입합니다.

```html
<form id="signupForm" th:data-s2-rules="${rules}">
  <input name="userId" type="text" />
  <span th:errors="*{userId}"></span>
  <button type="submit">Join Now</button>
</form>

<script type="module">
  import '/s2-util/js/s2.validator.js';
</script>
```

#### 4. [Controller] Pattern C: Final Server Verification (컨트롤러: 최종 서버 검증)

Perform identical validation on the server side.
<br>`POST` 요청 시 동일한 설계도로 최종 서버 검증을 수행합니다.

```java
@PostMapping("/signup")
public String signup(@ModelAttribute("command") UserCommand command, BindingResult result, Model model) {
    // [English] Reuses the identical logic defined in 'signupRules'
    // [한국어] 'signupRules'에 정의된 설계도를 그대로 재사용하여 무결성 보장
    S2BindValidator.context("signup", this::signupRules).validate(command, result);

    if (result.hasErrors()) {
        return signupPage(command, model);
    }
    userService.save(command);
    return "redirect:/welcome";
}
```

### 5-2. Technical Architecture (기술 아키택처) ⚙️

- **[English] Asset Location**: `s2.validator.js` is physically located inside the `s2-validator.jar` at `META-INF/resources/s2-util/js/`.
- **[한국어] 에셋 위치**: `s2.validator.js`는 JAR 파일 내부의 `META-INF/resources/s2-util/js/` 경로에 포함되어 있습니다.
- **[English] Automatic Binding**: Imported JS script automatically monitors all forms with `data-s2-rules`.
- **[한국어] 자동 바인딩**: 임포트된 JS는 `data-s2-rules` 속성을 가진 모든 폼을 자동으로 감시하여 바인딩합니다.

---

## 6. S2Jpql: Secure Dynamic Query (안전한 동적 쿼리 빌더) 🔎

Utilize Java Text Blocks (`"""`) for cleaner JPQL. `bindClause()` handles conditional clause binding, and `bindParameter()` exclusively handles parameter value binding for SQL injection prevention.
<br>Java Text Block(`"""`)으로 쿼리 가독성을 높입니다. `bindClause()`는 조건부 절 바인딩, `bindParameter()`는 파라미터 값 바인딩을 담당하여 SQL Injection을 방지합니다.

```java
String jpql = """
    SELECT p
    FROM Product p
    WHERE 1=1
        {{=cond_name}}
        {{=cond_price}}
    {{=sort}}
""";

return S2Jpql.from(em).type(Product.class).query(jpql)
    // [English] Conditional clause with hardcoded SQL, then bind the value separately
    // [한국어] 하드코딩된 SQL로 조건부 절을 추가하고, 파라미터는 별도로 바인딩
    .bindClause("cond_name", name, "AND p.name LIKE :name")
        .bindParameter("name", name, LikeMode.ANYWHERE)
    .bindClause("cond_price", price, "AND p.price >= :price")
        .bindParameter("price", price)
    .bindOrderBy("sort", sort)
    .build().getResultList();
```

### Pagination (페이징)

You can apply pagination via the builder using `limit(offset, limit)` which will call
`setFirstResult(offset)` and `setMaxResults(limit)` on the resulting `TypedQuery`.

```java
S2Jpql.from(em).type(Product.class).query(jpql)
    .bindClause("cond_name", name, "AND p.name LIKE :name")
        .bindParameter("name", name, LikeMode.ANYWHERE)
    .limit(0, 20) // first page: rows 0..19
    .build().getResultList();
```

Use the conditional overload `limit(condition, offset, limit)` when you want to apply pagination only when a condition is met.

### ⚠️ Critical Security Warning: SQL Injection Prevention

> [!WARNING]
> **[English]** **ARCHITECTURE:** The `bindClause()` method is **EXCLUSIVELY** for binding dynamic SQL clauses conditionally. The `bindParameter()` method is **EXCLUSIVELY** for binding dynamic parameter values. This separation is critical to prevent SQL injection.
>
> **RULE 1: Clauses must be hardcoded**
>
> - The `clause` and `prefix`/`suffix` parameters of `bindClause()` **MUST** always be hardcoded strings
> - **NEVER** concatenate user input into clause strings
> - **NEVER** use `String.format()` or `+` operator to build clauses with variables
>
> **RULE 2: Values go through bindParameter()**
>
> - All dynamic/user-provided values **MUST** go through `bindParameter()`
> - Do NOT pass values to the `conditionValue` parameter of `bindClause()`
> - The `conditionValue` is **ONLY** for checking the condition (null check, boolean check, etc.)
>
> <br>**[한국어]** **아키텍처:** `bindClause()` 메서드는 **동적 SQL 절을 조건부로 바인딩하기 위한 것**입니다. `bindParameter()` 메서드는 **동적 파라미터 값을 바인딩하기 위한 것**입니다. 이 분리는 SQL 인젝션을 방지하기 위해 매우 중요합니다.
>
> **규칙 1: 절은 반드시 하드코딩**
>
> - `bindClause()`의 `clause`, `prefix`/`suffix` 파라미터는 **반드시** 하드코딩된 문자열이어야 합니다
> - **절대** 절 문자열에 사용자 입력을 연결하지 마세요
> - **절대** `String.format()` 또는 `+` 연산자로 변수를 포함한 절을 만들지 마세요
>
> **규칙 2: 값은 bindParameter()로**
>
> - 모든 동적/사용자 제공 값은 **반드시** `bindParameter()`를 통해야 합니다
> - `bindClause()`의 `conditionValue` 파라미터에 값을 전달하지 마세요
> - `conditionValue`는 **조건 검사(null 체크, 불린 체크 등)용도만**입니다

#### SAFE Usage (안전한 사용):

```java
// Step 1: Bind clause conditionally (clause is hardcoded)
S2Jpql.from(em).type(Product.class).query(jpql)
    .bindClause("cond_name", userInput, "AND p.name LIKE :name")  // Clause is hardcoded!
        .bindParameter("name", userInput, LikeMode.ANYWHERE)  // Value bound safely here
    .build();
```

#### DANGEROUS Usage (위험한 사용 - 절대 하지 마세요):

```java
// ❌ WRONG: User input in clause string
.bindClause("cond", userInput, "AND p.name LIKE '%" + userInput + "%'")  // SQL INJECTION!

// ❌ WRONG: Using String.format for dynamic clause building
String clause = String.format("AND p.name = %s", userInput);  // SQL INJECTION!
.bindClause("cond", userInput, clause)

// ❌ WRONG: No bindParameter call - parameters don't get bound
.bindClause("search", userInput, "AND p.name = :name")  // Parameter :name will be NULL!
```

Failure to follow these rules can result in **SQL Injection vulnerabilities**.
<br>이 규칙을 따르지 않으면 **SQL 인젝션 취약점**이 발생할 수 있습니다.

---

## 7. S2Copier: Zero-Reflection Copy (고성능 객체 복사) 📋

Optimized with `MethodHandle` for maximum throughput between Entities and DTOs.
<br>`MethodHandle`로 최적화되어 리플렉션 고유의 병목 없이 데이터를 매핑합니다.

```java
// [English] Advanced Mapping and Partial Update
// [한국어] 고급 매핑 및 부분 업데이트 지원
S2Copier.from(requestDto)
    .exclude("id", "secret") // Field exclusion (필드 제외)
    .map("nickName", "displayName") // Property name sync (필드명 매핑)
    .ignoreNulls() // Supports selective updates (null 무시, PATCH 지원)
    .to(existingEntity); // Naturally triggers JPA Dirty Checking
```
