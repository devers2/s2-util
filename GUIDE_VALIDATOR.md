# S2Validator: Unified Server-Client Validation Guide (통합 검증 가이드) 🚀

> **"Defined Once on Server, Enforced Everywhere."**
> <br>**"서버에서 한 번 정의하고 어디서나 검증한다."**
>
> S2Util의 핵심 기능입니다. 서버에서 정의한 단 하나의 설계도로 클라이언트와 서버 양쪽에서 동일한 검증 엔진을 구동합니다.

---

## 1. The 4 Strategic Patterns (4가지 전략 패턴)

S2Validator supports four distinct usage patterns, each optimized for different scenarios. Choose the one that best fits your needs.

<br>S2Validator는 네 가지 서로 다른 사용 패턴을 지원합니다. 각각은 특정 상황에 최적화되어 있습니다.

### 1-1. Pattern A: Immediate Mode (즉각적인 검증 패턴)

**Usage:** `S2Validator.of(target, [failFast])`

**Purpose:** Quick, one-off validation within a method.
<br>**용도:** 메서드 내부에서 1회성 검증이 필요할 때.

```java
// [English] Exception Mode (Default)
// [한국어] 예외 모드 (기본값)
S2Validator.of(userInput)
    .field("email").rule(S2RuleType.EMAIL)
    .validate();  // Throws S2RuntimeException on failure

// [English] Boolean Mode
// [한국어] 논리값 모드
boolean isValid = S2Validator.of(userInput, false)
    .field("age").rule(S2RuleType.MIN_VALUE, 20)
    .validate();  // Returns true/false instead of throwing
```

> [!NOTE]
> **[English]** Default `of(target)` throws an `S2RuntimeException` on failure. Use `of(target, false)` to receive a `boolean` result.
> <br>**[한국어]** 기본 `of(target)`은 실패 시 `S2RuntimeException`을 발생시킵니다. 예외 대신 `true/false` 결과가 필요하면 `of(target, false)`를 사용하세요.

---

### 1-2. Pattern B: Blueprint Mode (검증 설계도 패턴)

**Usage:** `S2Validator.builder()`

**Purpose:** Reusable, thread-safe validator for multiple objects.
<br>**용도:** 여러 객체에 동일한 검증 규칙을 반복 적용할 때.

```java
// [English] 1. Define reusable validation blueprint once
// [한국어] 1. 재사용 가능한 검증 설계도 정의

S2Validator<UserDTO> schema = S2Validator.<UserDTO>builder()
    .field("email", "이메일").rule(S2RuleType.EMAIL)
    .field("age", "나이").rule(S2RuleType.MIN_VALUE, 18)
    .field("password", "비밀번호").rule(S2RuleType.REQUIRED)
    .build();

// [English] 2. Execute validation on multiple targets
// [한국어] 2. 여러 객체에 설계도 적용

schema.validate(userA);
schema.validate(userB);
schema.validate(userC);
// Thread-safe: can be used concurrently
```

---

### 1-3. Pattern C: Registry Mode (중앙 관리 패턴)

**Usage:** `S2ValidatorFactory.getOrRegister()`

**Purpose:** Global singleton caching with lazy initialization.
<br>**용도:** 검증기를 전역에서 캐싱하여 성능 최적화.

```java
// [English] Register validator globally (executed only once)
// [한국어] 전역 등록 (최초 1회만 실행)

S2Validator<UserDTO> validator = S2ValidatorFactory.getOrRegister(
    "USER_REGISTRATION",  // Unique key
    () -> S2Validator.<UserDTO>builder()
        .field("email").rule(S2RuleType.EMAIL)
        .field("password").rule(S2RuleType.MIN_LENGTH, 8)
        .build()
);

// [English] Retrieved from cache on subsequent calls
// [한국어] 이후 호출은 캐시된 검증기 반환

S2Validator<UserDTO> sameValidator =
    S2ValidatorFactory.getOrRegister("USER_REGISTRATION", () -> ...);
// Returns cached instance, lambda is not executed
// 캐시된 인스턴스 반환, 람다식 미실행
```

**Benefits:**

- ✅ Lazy initialization (필요할 때만 생성)
- ✅ Global caching (전역 캐싱)
- ✅ Zero overhead on cache hits (캐시 히트 시 오버헤드 없음)
- ✅ Thread-safe singleton pattern (스레드 안전)

---

### 1-4. Pattern D: Spring Standard Alignment (스프링 표준 통합 패턴)

**Usage:** `S2BindValidator.context()`

**Purpose:** Seamless integration with Spring's `BindingResult`.
<br>**용도:** 스프링 표준 `BindingResult`와 통합.

```java
// [English] Controller with automatic Spring integration
// [한국어] 스프링 자동 통합

@PostMapping("/join")
public String join(
        @ModelAttribute UserDTO user,
        BindingResult result) {

    // [English] Validates and maps errors to Spring's BindingResult
    // [한국어] S2 검증 결과를 스프링 BindingResult로 자동 매핑

    S2BindValidator.context("JOIN_RULES", this::joinRules)
        .validate(user, result);

    if (result.hasErrors()) {
        return "joinForm";  // Standard Spring flow
    }

    userService.save(user);
    return "redirect:/success";
}

private S2Validator<UserDTO> joinRules() {
    return S2Validator.<UserDTO>builder()
        .field("email", "이메일").rule(S2RuleType.EMAIL)
        .field("password", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
        .build();
}
```

---

## 2. Core Validation Features (핵심 검증 기능)

### 2-1. Built-in Rules (내장 규칙)

S2Validator provides extensive built-in rules via `S2RuleType` enum.

<br>S2Validator는 `S2RuleType` 열거형을 통해 광범위한 내장 규칙을 제공합니다.

```java
.field("email", "이메일")
    .rule(S2RuleType.REQUIRED)          // Not null/empty
    .rule(S2RuleType.EMAIL)              // Valid email format
    .rule(S2RuleType.MAX_LENGTH, 100)    // Max 100 characters

.field("age", "나이")
    .rule(S2RuleType.REQUIRED)
    .rule(S2RuleType.MIN_VALUE, 0)       // >= 0
    .rule(S2RuleType.MAX_VALUE, 150)     // <= 150

.field("password", "비밀번호")
    .rule(S2RuleType.REQUIRED)
    .rule(S2RuleType.MIN_LENGTH, 8)      // At least 8 characters
    .rule(S2RuleType.PATTERN, "^[A-Za-z0-9]+$")  // Alphanumeric only
```

**Common Rules:**

| Rule                       | Purpose          | 용도             |
| -------------------------- | ---------------- | ---------------- |
| `REQUIRED`                 | Not null/empty   | null/공백 불가   |
| `EMAIL`                    | Valid email      | 이메일 형식      |
| `MIN_VALUE`, `MAX_VALUE`   | Numeric range    | 숫자 범위        |
| `MIN_LENGTH`, `MAX_LENGTH` | String length    | 문자열 길이      |
| `PATTERN`                  | Regex matching   | 정규식           |
| `EQUALS_FIELD`             | Field comparison | 필드 비교        |
| `EACH`                     | List validation  | 리스트 모든 요소 |
| `NESTED`                   | Nested object    | 중첩 객체        |

---

### 2-2. Object Graph Navigation (경로 탐색)

Access nested properties using familiar notation.

<br>친숙한 표기법으로 중첩 속성에 접근합니다.

```java
// [English] Dot notation for nested objects
// [한국어] 점 표기법으로 중첩 객체 탐색

.field("user.address.street", "거리명")
    .rule(S2RuleType.REQUIRED)

// [English] Index notation for specific list elements
// [한국어] 인덱스 표기법으로 특정 요소 검증

.field("orders[0].totalPrice", "첫 주문 총가격")
    .rule(S2RuleType.MIN_VALUE, 1000)

// [English] Wildcard notation for all list elements
// [한국어] 와일드카드로 리스트 모든 요소 검증

.field("items[].price", "상품 가격")
    .rule(S2RuleType.MIN_VALUE, 0)
```

---

### 2-3. Recursive & Composite Validation (재귀 및 구성 검증)

Reuse validators for hierarchical structures.

<br>기존 검증기를 다른 검증기의 규칙으로 재사용합니다.

```java
// [English] 1. Define sub-validator
// [한국어] 1. 하위 검증기 정의

S2Validator<ItemDTO> itemValidator = S2Validator.<ItemDTO>builder()
    .field("name", "상품명").rule(S2RuleType.REQUIRED)
    .field("price", "가격").rule(S2RuleType.MIN_VALUE, 0)
    .build();

// [English] 2. Reuse in parent validator
// [한국어] 2. 부모 검증기에서 재사용

S2Validator<OrderDTO> orderValidator = S2Validator.<OrderDTO>builder()
    // Validate each item in a list (EACH)
    .field("items", "상품 목록")
        .rule(S2RuleType.EACH, itemValidator)

    // Validate a single nested object (NESTED)
    .field("shippingInfo", "배송 정보")
        .rule(S2RuleType.NESTED, itemValidator)

    .build();
```

---

### 2-4. Custom Logic: Predicate & BiPredicate (사용자 정의 로직)

Inject Lambda for complex business rules.

<br>람다식을 주입하여 복잡한 비즈니스 규칙을 처리합니다.

```java
// [English] Single field validation (Predicate)
// [한국어] 단일 필드 검증 (Predicate)

.field("age", "나이")
    .rule(val -> (Integer) val >= 18)
    .ko("성인만 가입 가능합니다.")

// [English] Multi-field validation (BiPredicate)
// [한국어] 다중 필드 검증 (BiPredicate)

.field("confirmPassword", "비밀번호 확인")
    .rule((val, target) -> {
        String password = S2Util.getValue(target, "password");
        return password.equals(val);
    })
    .ko("비밀번호가 일치하지 않습니다.")

// [English] Complex business logic
// [한국어] 복잡한 비즈니스 로직

.field("endDate", "종료일")
    .rule((val, target) -> {
        String startDate = S2Util.getValue(target, "startDate");
        return startDate.compareTo((String)val) <= 0;
    })
    .ko("종료일은 시작일 이후여야 합니다.")
```

> [!WARNING]
> **[English]** Custom Lambda rules are **not** synchronized to JavaScript automatically. Use built-in `S2RuleType` for full client-server synchronization.
> <br>**[한국어]** 람다 기반 커스텀 규칙은 클라이언트로 자동 변환되지 않습니다. 클라이언트-서버 동기화가 필요하면 내장 `S2RuleType`을 사용하세요.

---

## 3. Messaging & Localization (메시지 및 다국어) 🌍

### 3-1. Message Customization (메시지 사용자정의)

Specify error messages at the field level.

<br>필드별로 오류 메시지를 사용자정의합니다.

```java
.field("email", "이메일")
    .rule(S2RuleType.EMAIL)
    // [English] Option 1: Message key (requires bundle setup)
    // [한국어] 옵션 1: 메시지 키 (번들 설정 필요)
    .message("validation.email.invalid")

    // [English] Option 2: Direct message
    // [한국어] 옵션 2: 직접 메시지
    .message("Please enter a valid email address.")
```

### 3-2. Language-Specific Messages (언어별 메시지)

Set messages for different locales.

<br>다양한 언어로 메시지를 설정합니다.

```java
.field("password", "비밀번호")
    .rule(S2RuleType.MIN_LENGTH, 8)
    // [English] Set Korean message
    .ko("비밀번호는 8자 이상이어야 합니다.")
    // [English] Set English message
    .en("Password must be at least 8 characters.")
    // [English] Set French message
    .message(Locale.FRANCE, "Le mot de passe doit comporter au moins 8 caractères.")
```

### 3-3. Korean Particle Handling (한국어 조사 자동 선택) 🇰🇷

Automatically selects appropriate particles based on field label.

<br>라벨 단어의 종성 유무에 따라 자동으로 적절한 조사를 선택합니다.

```java
// [English] Automatic particle selection
// [한국어] 자동 조사 선택

.field("id", "아이디")
    .rule(S2RuleType.REQUIRED)
    .ko("{0|은/는} 필수입니다.")
    // 결과: "아이디는 필수입니다." (자동 선택)

.field("name", "이름")
    .rule(S2RuleType.REQUIRED)
    .ko("{0|은/는} 필수입니다.")
    // 결과: "이름은 필수입니다." (자동 선택)

.field("email", "이메일")
    .rule(S2RuleType.EMAIL)
    .ko("{0|이/가} 올바르지 않습니다.")
    // 결과: "이메일이 올바르지 않습니다." (자동 선택)
```

**Supported Particles:**

- `{0|은/는}` → 은 / 는
- `{0|이/가}` → 이 / 가
- `{0|을/를}` → 을 / 를
- `{0|과/와}` → 과 / 와

---

## 4. End-to-End Implementation (전 과정 구현)

### Step 1: Define Validation Rules (1단계: 검증 규칙 정의)

```java
// [English] ServerController.java
// [한국어] 컨트롤러에서 검증 규칙 정의

@Controller
public class AuthController {

    // [English] Reusable validation blueprint
    // [한국어] 재사용 가능한 검증 규칙
    private S2Validator<SignupCommand> signupRules() {
        return S2Validator.<SignupCommand>builder()
            .field("userId", "사용자ID")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.MIN_LENGTH, 3)
                .rule(S2RuleType.MAX_LENGTH, 20)

            .field("email", "이메일")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.EMAIL)

            .field("password", "비밀번호")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.MIN_LENGTH, 8)
                .ko("8자 이상이어야 합니다.")

            .field("confirmPassword", "비밀번호확인")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.EQUALS_FIELD, "password")
                .ko("비밀번호가 일치하지 않습니다.")

            .build();
    }
}
```

### Step 2: Serve Rules to Client (2단계: 클라이언트에 규칙 전달)

```java
// [English] GET request: Serve validation rules to client
// [한국어] GET 요청: 클라이언트에 규칙 전달

@GetMapping("/signup")
public String signupPage(
        @ModelAttribute("command") SignupCommand command,
        Model model) {

    // [English] Extract rules as JSON
    // [한국어] 규칙을 JSON으로 추출
    String rules = S2BindValidator.context("signup", this::signupRules)
        .getRulesJson();

    model.addAttribute("rules", rules);
    return "signup";  // Thymeleaf template
}
```

### Step 3: Inject Rules into Form (3단계: 폼에 규칙 주입)

```html
<!-- signup.html (Thymeleaf) -->
<form id="signupForm" th:data-s2-rules="${rules}" method="POST">
  <div class="form-group">
    <label for="userId">User ID</label>
    <input id="userId" name="userId" type="text" class="form-control" required />
    <span th:errors="*{userId}" class="text-danger"></span>
  </div>

  <div class="form-group">
    <label for="email">Email</label>
    <input id="email" name="email" type="email" class="form-control" required />
    <span th:errors="*{email}" class="text-danger"></span>
  </div>

  <div class="form-group">
    <label for="password">Password</label>
    <input id="password" name="password" type="password" class="form-control" required />
    <span th:errors="*{password}" class="text-danger"></span>
  </div>

  <div class="form-group">
    <label for="confirmPassword">Confirm Password</label>
    <input id="confirmPassword" name="confirmPassword" type="password" class="form-control" required />
    <span th:errors="*{confirmPassword}" class="text-danger"></span>
  </div>

  <button type="submit" class="btn btn-primary">Sign Up</button>
</form>

<!-- [English] Import S2 Validator JavaScript -->
<!-- [한국어] S2 Validator JavaScript 임포트 -->
<script type="module">
  import '/s2-util/js/s2.validator.js';
</script>
```

### Step 4: Server-Side Final Validation (4단계: 서버 최종 검증)

```java
// [English] POST request: Final server validation
// [한국어] POST 요청: 서버 최종 검증

@PostMapping("/signup")
public String signup(
        @ModelAttribute("command") SignupCommand command,
        BindingResult result,
        Model model) {

    // [English] Reuse identical rules from GET
    // [한국어] GET에서 정의한 규칙을 그대로 재사용
    S2BindValidator.context("signup", this::signupRules)
        .validate(command, result);

    if (result.hasErrors()) {
        // [English] Return to form with validation errors
        // [한국어] 검증 오류와 함께 폼으로 돌아가기
        return signupPage(command, model);
    }

    // [English] All validation passed
    // [한국어] 모든 검증 통과
    userService.createUser(command);
    return "redirect:/welcome";
}
```

---

## 5. Architecture Overview (아키텍처 개요)

### Component Diagram (컴포넌트 다이어그램)

```
┌─────────────────────────────────────────────────────────────┐
│                    Web Browser                              │
├─────────────────────────────────────────────────────────────┤
│  HTML Form + s2.validator.js                                │
│  ├─ Real-time validation                                    │
│  ├─ Instant error messages                                  │
│  └─ Client-side enforcement                                 │
└────────────────────────┬────────────────────────────────────┘
                         │ POST (JSON)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring MVC Controller                          │
├─────────────────────────────────────────────────────────────┤
│  S2BindValidator.context().validate(data, result)          │
│  ├─ Same rule definitions                                   │
│  ├─ Error mapping to BindingResult                          │
│  └─ Server-side enforcement                                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
                  [Business Logic]
```

### Asset Delivery (에셋 전달)

- **Location:** `s2-validator.js` inside `s2-validator.jar`
  <br>**위치:** `s2-validator.jar` 내부
- **Path:** `META-INF/resources/s2-util/js/s2.validator.js`
- **Auto-binding:** Forms with `data-s2-rules` attribute are automatically monitored
  <br>**자동 바인딩:** `data-s2-rules` 속성을 가진 폼은 자동으로 감시됨

---

## 6. Best Practices (모범 사례)

```
1. ✅ Define rules once in a dedicated method
   규칙을 별도의 메서드에 정의하여 한 번만

2. ✅ Use Pattern C (Registry) for high-traffic apps
   고트래픽 앱에서는 Pattern C (Registry) 사용

3. ✅ Always perform server-side validation
   항상 서버 검증 수행 (클라이언트 검증 신뢰 금지)

4. ✅ Use built-in S2RuleType for client sync
   클라이언트 동기화를 위해 내장 규칙 사용

5. ✅ Test validation on both server and client
   서버와 클라이언트 양쪽 검증 테스트

6. ❌ Don't trust client-side validation alone
   클라이언트 검증만 신뢰하지 말 것

7. ❌ Don't hardcode error messages
   오류 메시지를 하드코딩하지 말 것

8. ✅ Leverage Korean particle handling
   한국어 조사 자동 선택 기능 활용
```

---

## 7. Error Handling (오류 처리)

### Server-Side Errors (서버 오류 처리)

```java
// [English] Exception handling
// [한국어] 예외 처리

try {
    S2Validator.of(data)
        .field("email").rule(S2RuleType.EMAIL)
        .validate();
} catch (S2RuntimeException e) {
    // [English] Get detailed error information
    // [한국어] 상세 오류 정보 획득
    String message = e.getMessage();
    List<S2ErrorDetail> errors = e.getErrors();
}
```

### Spring Integration (스프링 통합)

```java
// [English] BindingResult captures errors automatically
// [한국어] BindingResult가 오류를 자동으로 캡처

if (result.hasErrors()) {
    result.getAllErrors().forEach(error -> {
        System.out.println(error.getDefaultMessage());
    });
}
```

---

## 8. Performance Tips (성능 팁)

```
1. Use Pattern C (Registry Mode) for validators
   검증기는 Pattern C (Registry) 사용

2. Cache validation results when possible
   가능하면 검증 결과 캐싱

3. Avoid complex lambda rules in loops
   루프에서 복잡한 람다 규칙 피하기

4. Reuse validators (don't recreate)
   검증기 재사용 (재생성 금지)

5. Use PATTERN rule for string validation
   문자열 검증에는 PATTERN 규칙 사용
```
