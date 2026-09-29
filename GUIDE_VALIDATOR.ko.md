# S2Validator: 통합 검증 가이드 🚀

🌐 [English](GUIDE_VALIDATOR.md) | **한국어**

> **"서버에서 한 번 정의하고 어디서나 검증한다."**
>
> S2Util의 핵심 기능입니다. 서버에서 정의한 단 하나의 설계도로 클라이언트와 서버 양쪽에서 동일한 검증 엔진을 구동합니다.

---

## 1. 3가지 전략 패턴

S2Validator는 네 가지 서로 다른 사용 패턴을 지원합니다. 상황에 가장 적합한 패턴을 선택하여 사용하세요.

### 1-1. 즉각적인 검증 패턴

**사용법:** `S2Validator.of(target, [failFast])`

**용도:** 메서드 내부에서 1회성 검증이 필요할 때.

```java
// 예외 모드 (기본값)
S2Validator.of(userInput)
    .field("email").rule(S2RuleType.EMAIL)
    .validate();  // Throws S2ValidationException on failure

// 논리값 모드
boolean isValid = S2Validator.of(userInput, false)
    .field("age").rule(S2RuleType.MIN_VALUE, 20)
    .validate();  // Returns true/false instead of throwing
```

> [!NOTE]
> 기본 `of(target)`은 검증 실패 시 `S2ValidationException`을 발생시킵니다. 예외 발생 대신 `true/false` 결과가 필요하면 `of(target, false)`를 사용하세요.

---

### 1-2. 검증 설계도 패턴

**사용법:** `S2Validator.builder()`

**용도:** 여러 객체에 동일한 검증 규칙을 반복 적용할 때 (재사용 가능, Thread-safe).

```java
// 1. 재사용 가능한 검증 설계도 정의

S2Validator<UserDTO> schema = S2Validator.<UserDTO>builder()
    .field("email", "이메일").rule(S2RuleType.EMAIL)
    .field("age", "나이").rule(S2RuleType.MIN_VALUE, 18)
    .field("password", "비밀번호").rule(S2RuleType.REQUIRED)
    .build();

// 2. 여러 객체에 설계도 적용

schema.validate(userA);
schema.validate(userB);
schema.validate(userC);
// 스레드 안전: 여러 스레드에서 동시 호출 가능
```

---

### 1-3. 스프링 표준 통합 패턴 (권장)

**사용법:** `S2BindValidator.bind(validator)` — `S2Validator.builder()`로 만든 검증기를 Spring 에 연결합니다 (객체 하나를 즉시 검증하는 `S2Validator.of(target)`과 다름)

**용도:** 검증기 인스턴스를 직접 전달하여 스프링 표준 `BindingResult`와 통합합니다. 전역 상태가 없어 충돌하거나 시험 사이에 남는 것이 없습니다. 검증기 생성 비용은 일반적인 폼에서 1마이크로초 미만이라 상수, Spring 빈, 요청마다 생성 모두 괜찮으며, GET 폼과 POST 처리에 같은 인스턴스(또는 규칙 정의 메서드)를 쓰십시오.

```java
@Controller
@RequestMapping("/member")
public class MemberController {

    // 정적 또는 빈(Bean)으로 검증기 인스턴스 구성 (권장)
    private static final S2Validator<UserDTO> USER_VALIDATOR = S2Validator.<UserDTO>builder()
        .field("email", "이메일").rule(S2RuleType.REQUIRED).rule(S2RuleType.EMAIL)
        .field("password", "비밀번호").rule(S2RuleType.MIN_LENGTH, 8)
        .build();

    @PostMapping("/join")
    public String join(@ModelAttribute UserDTO user, BindingResult result) {
        // 검증기 인스턴스를 직접 바인딩
        S2BindValidator.bind(USER_VALIDATOR).validate(user, result);

        if (result.hasErrors()) {
            return "member/join";
        }
        return "redirect:/success";
    }
}
```

#### 바인딩 방식 고르기

| 방식 | 언제 쓰나 |
|---|---|
| **필드** `private final BoundContext<T> x = S2BindValidator.bind(rules());` | 기본. 컨트롤러 인스턴스마다 한 번 만들어 GET 폼과 POST 처리가 공유합니다. |
| **생성자** `this.x = S2BindValidator.bind(rules());` | 규칙 정의가 주입받은 의존성(예: 코드 목록 서비스)을 쓰는 경우. |
| **호출마다** `S2BindValidator.bind(rules()).validate(...)` | 규칙이 실행 중에 바뀌는 경우(예: DB 에서 선택지를 불러옴). 일반적인 폼에서 생성 비용은 약 0.5µs 입니다. |

> [!WARNING]
> 필드 초기화는 생성자 본문보다 **먼저** 실행됩니다. `rules()`가 생성자에서 넣거나 `@Autowired`로 주입받는 의존성을 읽으면 그 시점에는 아직 `null`이라 필드 초기화에서 `NullPointerException`이 납니다. 대입 뒤 생성자에서 바인딩하거나 호출마다 바인딩하십시오:
>
> ```java
> private final CodeService codeService;
> private final S2BindValidator.BoundContext<SignupCommand> signup;
>
> public SignupController(CodeService codeService) {
>     this.codeService = codeService;
>     this.signup = S2BindValidator.bind(signupRules()); // 여기서는 signupRules()가 codeService 를 써도 됨
> }
> ```

`BoundContext`는 검증기만 들고 있고 요청 로케일은 호출할 때마다 읽으므로, 필드에 두어도 스레드에 안전합니다.

---

## 2. 핵심 검증 기능

### 2-1. 내장 규칙

S2Validator는 `S2RuleType` 열거형을 통해 다양한 내장 검증 규칙을 제공합니다.

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
    .rule(S2RuleType.REGEX, "^[A-Za-z0-9]+$")  // Alphanumeric only
```

**Common Rules:**

| 규칙                       | 용도                     |
| -------------------------- | ------------------------ |
| `REQUIRED`                 | null 또는 빈 문자열 불가 |
| `EMAIL`                    | 이메일 형식 검증         |
| `MIN_VALUE`, `MAX_VALUE`   | 숫자 범위 제한           |
| `MIN_LENGTH`, `MAX_LENGTH` | 문자열 길이 제한         |
| `PATTERN`                  | 정규식 매칭              |
| `EQUALS_FIELD`             | 타 필드와의 값 일치 여부 |
| `EACH`                     | 컬렉션 내 모든 요소 검증 |
| `NESTED`                   | 중첩 객체 검증           |

---

### 2-2. 경로 탐색

점(Dot), 인덱스, 와일드카드 등의 직관적인 표기법으로 중첩 속성에 접근합니다.

```java
// 점 표기법으로 중첩 객체 탐색

.field("user.address.street", "거리명")
    .rule(S2RuleType.REQUIRED)

// 인덱스 표기법으로 특정 요소 검증

.field("orders[0].totalPrice", "첫 주문 총가격")
    .rule(S2RuleType.MIN_VALUE, 1000)

// 와일드카드로 리스트 모든 요소 검증

.field("items[].price", "상품 가격")
    .rule(S2RuleType.MIN_VALUE, 0)
```

---

### 2-3. 재귀 및 구성 검증

하위 검증기를 부모 검증기의 규칙으로 재사용하여 계층 구조를 검증합니다.

```java
// 1. 하위 검증기 정의

S2Validator<ItemDTO> itemValidator = S2Validator.<ItemDTO>builder()
    .field("name", "상품명").rule(S2RuleType.REQUIRED)
    .field("price", "가격").rule(S2RuleType.MIN_VALUE, 0)
    .build();

// 2. 부모 검증기에서 재사용

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

### 2-4. 사용자 정의 로직

람다식을 주입하여 복잡한 도메인 비즈니스 규칙을 유연하게 처리합니다.

```java
// 단일 필드 검증 (Predicate)

.field("age", "나이")
    .rule(val -> (Integer) val >= 18)
    .ko("성인만 가입 가능합니다.")

// 다중 필드 검증 (BiPredicate)
// 주의: 커스텀 람다는 기본적으로 빈 값(null/empty)일 때 실행을 건너뜁니다.
// 필드가 필수여야 한다면 반드시 REQUIRED 규칙과 함께 체이닝해야 합니다.

.field("confirmPassword", "비밀번호 확인")
    .rule(S2RuleType.REQUIRED)
    .rule((val, target) -> {
        String password = S2Util.getValue(target, "password");
        return password.equals(val);
    })
    .ko("비밀번호가 일치하지 않습니다.")

// 복잡한 비즈니스 로직

.field("endDate", "종료일")
    .rule((val, target) -> {
        String startDate = S2Util.getValue(target, "startDate");
        return startDate.compareTo((String)val) <= 0;
    })
    .ko("종료일은 시작일 이후여야 합니다.")

// 빈 값(null/empty)인 경우에도 람다를 반드시 실행해야 하는 경우 (.includeEmpty())
// 예: "이메일 또는 전화번호 중 하나는 필수"와 같은 상호 배타적 검증
.field("secondaryContact", "보조 연락처")
    .rule((val, target) -> {
        // null 또는 빈 값도 람다로 전달되어 직접 조건 판단 가능
        return val != null || S2Util.isNotEmpty(S2Util.getValue(target, "primaryContact"));
    }).includeEmpty()
    .ko("기본 연락처 또는 보조 연락처 중 하나는 필수입니다.")
```

> [!NOTE]
> **커스텀 람다의 빈 값 단락(Short-circuit) 정책:**
> 표준 Bean Validation 및 YAVI와 동일하게, 커스텀 람다 규칙은 필드 값이 `null` 또는 비어 있을 때 기본적으로 실행되지 않고 통과합니다. 이는 `REQUIRED`가 없는 선택 입력 필드에서 불필요한 `NullPointerException`을 방지하기 위함입니다.
> - 값이 반드시 있어야 한다면 `.rule(S2RuleType.REQUIRED)`를 함께 체이닝하세요.
> - 빈 값 상태 자체를 람다 내부에서 직접 확인해야 하는 특수 규칙의 경우 `.includeEmpty()`를 선언하세요.
> - 람다 내부에서 처리되지 않은 런타임 예외가 발생하면, 오류가 난 필드 경로 정보가 포함된 `S2RuleExecutionException`으로 감싸서 전달됩니다.

> [!WARNING]
> 람다 기반 커스텀 규칙은 클라이언트(JavaScript)로 자동 변환되지 않습니다. 클라이언트-서버 동기화가 필요하면 내장 `S2RuleType`을 사용하세요.

---

## 3. 메시지 및 다국어

### 3-1. 메시지 사용자정의

필드별로 사용자 정의 오류 메시지나 메시지 번들 키를 지정합니다.

```java
// 옵션 1: S2Validator.setValidationBundle("messages/validation")로 설정한 번들에서 메시지 키로 조회
.field("email", "이메일")
    .rule(S2RuleType.EMAIL, null, "validation.email.invalid")

// 옵션 2: 언어별 직접 메시지
.field("email", "이메일")
    .rule(S2RuleType.EMAIL)
    .ko("올바른 이메일 주소를 입력하십시오.")
    .en("Please enter a valid email address.")
```

### 3-2. 언어별 메시지

다양한 로케일(한국어, 영어, 프랑스어 등)에 맞춘 다국어 메시지를 설정합니다.

```java
.field("password", "비밀번호")
    .rule(S2RuleType.MIN_LENGTH, 8)
    .ko("비밀번호는 8자 이상이어야 합니다.")
    .en("Password must be at least 8 characters.")
    .message(Locale.FRANCE, "Le mot de passe doit comporter au moins 8 caractères.")
```

### 3-3. 한국어 조사 자동 선택

라벨 단어의 받침(종성) 유무를 판별하여 알맞은 한국어 조사를 자동 선택합니다.

```java
// 자동 조사 선택

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

## 4. 전 과정 구현

### 1단계: 검증 규칙 정의

```java
// 컨트롤러에서 검증 규칙 정의

@Controller
public class AuthController {

    // 재사용 가능한 검증 규칙
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

    // 한 번 바인딩해 GET 폼과 POST 처리에서 함께 사용
    private final S2BindValidator.BoundContext<SignupCommand> signup = S2BindValidator.bind(signupRules());
}
```

### 2단계: 클라이언트에 규칙 전달

```java
// GET 요청: 클라이언트에 규칙 전달

@GetMapping("/signup")
public String signupPage(
        @ModelAttribute("command") SignupCommand command,
        Model model) {

    // 규칙을 JSON으로 추출
    String rules = signup.getRulesJson();

    model.addAttribute("rules", rules);
    return "signup";  // Thymeleaf template
}
```

### 3단계: 폼에 규칙 주입

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

<!-- S2 Validator JavaScript 임포트 -->
<script type="module">
  import '/s2-util/js/s2.validator.js';
</script>
```

### 4단계: 서버 최종 검증

```java
// POST 요청: 서버 최종 검증

@PostMapping("/signup")
public String signup(
        @ModelAttribute("command") SignupCommand command,
        BindingResult result,
        Model model) {

    // GET에서 정의한 규칙을 그대로 재사용
    signup.validate(command, result);

    if (result.hasErrors()) {
        // 검증 오류와 함께 폼으로 돌아가기
        return signupPage(command, model);
    }

    // 모든 검증 통과
    userService.createUser(command);
    return "redirect:/welcome";
}
```

---

## 5. 아키텍처 개요

### 컴포넌트 다이어그램

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
│  S2BindValidator.bind(v).validate(data, result)              │
│  ├─ Same rule definitions                                   │
│  ├─ Error mapping to BindingResult                          │
│  └─ Server-side enforcement                                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
                  [Business Logic]
```

### 에셋 전달

- **위치:** `s2-validator.jar` 내부
- **경로:** `META-INF/resources/s2-util/js/s2.validator.js`
- **자동 바인딩:** `data-s2-rules` 속성을 가진 폼은 자동으로 감시 및 유효성 검사 수행

---

## 6. 모범 사례

```
1. ✅ 규칙을 별도 메서드에 정의하여 중복 정의 방지
2. ✅ 스프링 MVC 폼은 Pattern C (S2BindValidator.bind) 사용
3. ✅ 항상 서버 측 최종 검증 수행 (클라이언트 단독 신뢰 금지)
4. ✅ 클라이언트 동기화가 필요한 경우 내장 S2RuleType 활용
5. ✅ 서버와 클라이언트 양쪽에서 검증 로직 테스트
6. ❌ 클라이언트 검증에만 의존하지 말 것
7. ❌ 오류 메시지를 하드코딩하지 말고 메시지 키/번들 활용 권장
8. ✅ 한국어 조사 자동 선택({0|은/는}) 기능 적극 활용
```

---

## 7. 오류 처리

### 서버 오류 처리

```java
// 예외 처리

try {
    S2Validator.of(data)
        .field("email").rule(S2RuleType.EMAIL)
        .validate();
} catch (S2ValidationException e) {
    // 상세 오류 정보 획득 (최초 실패 1건)
    String message = e.getMessage();
    String field = e.getFieldName();     // 예: "email" 또는 "items[0].name"
    String errorCode = e.getErrorCode(); // 실패한 규칙의 메시지 키
} catch (S2RuleExecutionException e) {
    // 커스텀 규칙(람다) 자체에서 예외 발생. 원인 예외는 e.getCause()
}
```

### 스프링 통합

```java
// BindingResult가 오류를 자동으로 캡처

if (result.hasErrors()) {
    result.getAllErrors().forEach(error -> {
        System.out.println(error.getDefaultMessage());
    });
}
```

---

## 8. 성능 팁

```
1. 검증기 생성 비용은 작아서(일반적인 가입 폼 약 0.5µs) 요청마다 S2BindValidator.bind(rules())로 만들어도 됩니다.
   규칙 정의 자체가 무거울 때(예: DB 에서 선택지를 불러옴)만 필드·빈에 보관하십시오
2. 가장 비싼 단계는 GET 폼용 규칙 JSON 생성(약 15µs)입니다. 매우 자주 열리는 화면이면 JSON 문자열을 캐시하십시오
3. 대량 루프 내부에서 무거운 람다 규칙 생성 회피
4. 문자열 형식 검증에는 내장 규칙이나 REGEX 사용: 서버에서 실행되고 브라우저로도 내보내집니다
```
