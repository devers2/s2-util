# S2Validator: Unified Server-Client Validation Guide 🚀

🌐 **English** | [한국어](GUIDE_VALIDATOR.ko.md)

> **"Defined Once on Server, Enforced Everywhere."**

---

## 1. The 4 Strategic Patterns

S2Validator supports four distinct usage patterns, each optimized for different scenarios. Choose the one that best fits your needs.

### 1-1. Pattern A: Immediate Mode

**Usage:** `S2Validator.of(target, [failFast])`

**Purpose:** Quick, one-off validation within a method.

```java
// Exception Mode (Default)
S2Validator.of(userInput)
    .field("email").rule(S2RuleType.EMAIL)
    .validate();  // Throws S2RuntimeException on failure

// Boolean Mode
boolean isValid = S2Validator.of(userInput, false)
    .field("age").rule(S2RuleType.MIN_VALUE, 20)
    .validate();  // Returns true/false instead of throwing
```

> [!NOTE]
> Default `of(target)` throws an `S2RuntimeException` on failure. Use `of(target, false)` to receive a `boolean` result.

---

### 1-2. Pattern B: Blueprint Mode

**Usage:** `S2Validator.builder()`

**Purpose:** Reusable, thread-safe validator for multiple objects.

```java
// 1. Define reusable validation blueprint once

S2Validator<UserDTO> schema = S2Validator.<UserDTO>builder()
    .field("email", "Email").rule(S2RuleType.EMAIL)
    .field("age", "Age").rule(S2RuleType.MIN_VALUE, 18)
    .field("password", "Password").rule(S2RuleType.REQUIRED)
    .build();

// 2. Execute validation on multiple targets

schema.validate(userA);
schema.validate(userB);
schema.validate(userC);
// Thread-safe: can be used concurrently
```

---

### 1-3. Pattern C: Spring Standard Alignment (Recommended)

**Usage:** `S2BindValidator.of(validator)`

**Purpose:** Seamless integration with Spring's `BindingResult` using direct validator instances. Bypasses the global registry, eliminating key collisions, memory leaks, and test isolation issues.

```java
@Controller
@RequestMapping("/member")
public class MemberController {

    // Define validator instance as a static constant or Spring bean (Recommended)
    private static final S2Validator<UserDTO> USER_VALIDATOR = S2Validator.<UserDTO>builder()
        .field("email", "Email").rule(S2RuleType.REQUIRED).rule(S2RuleType.EMAIL)
        .field("password", "Password").rule(S2RuleType.MIN_LENGTH, 8)
        .build();

    @PostMapping("/join")
    public String join(@ModelAttribute UserDTO user, BindingResult result) {
        // Direct instance path bypassing global registry (Recommended)
        S2BindValidator.of(USER_VALIDATOR).validate(user, result);

        if (result.hasErrors()) {
            return "member/join";
        }
        return "redirect:/success";
    }
}
```

---

### 1-4. Pattern D: Registry Mode (Optional Global Cache)

**Usage:** `S2ValidatorFactory.getOrRegister()` / `S2BindValidator.context(key, supplier)`

**Purpose:** String key-based global caching for legacy compatibility or lazy-initialized singletons.

```java
// Register validator globally (logs WARN if key is re-registered with different supplier)
S2Validator<UserDTO> validator = S2ValidatorFactory.getOrRegister(
    "USER_REGISTRATION",  // Unique key
    () -> S2Validator.<UserDTO>builder()
        .field("email").rule(S2RuleType.EMAIL)
        .field("password").rule(S2RuleType.MIN_LENGTH, 8)
        .build()
);

// S2BindValidator with string key
S2BindValidator.context("USER_REGISTRATION", this::userRules).validate(user, result);
```

> [!TIP]
> Validator build overhead is under 500ns per request. For modern Spring applications, we strongly recommend `S2BindValidator.of(validator)` over global string registries. For test isolation, use `S2Validator.resetAll()` or `S2ValidatorFactory.clear()`.

```java
// Controller with automatic Spring integration using context()

@PostMapping("/join")
public String join(
        @ModelAttribute UserDTO user,
        BindingResult result) {

    // Validates and maps errors to Spring's BindingResult

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
        .field("email", "Email").rule(S2RuleType.EMAIL)
        .field("password", "Password").rule(S2RuleType.MIN_LENGTH, 8)
        .build();
}
```

---

## 2. Core Validation Features

### 2-1. Built-in Rules

S2Validator provides extensive built-in rules via `S2RuleType` enum.

```java
.field("email", "Email")
    .rule(S2RuleType.REQUIRED)          // Not null/empty
    .rule(S2RuleType.EMAIL)              // Valid email format
    .rule(S2RuleType.MAX_LENGTH, 100)    // Max 100 characters

.field("age", "Age")
    .rule(S2RuleType.REQUIRED)
    .rule(S2RuleType.MIN_VALUE, 0)       // >= 0
    .rule(S2RuleType.MAX_VALUE, 150)     // <= 150

.field("password", "Password")
    .rule(S2RuleType.REQUIRED)
    .rule(S2RuleType.MIN_LENGTH, 8)      // At least 8 characters
    .rule(S2RuleType.PATTERN, "^[A-Za-z0-9]+$")  // Alphanumeric only
```

**Common Rules:**

| Rule                       | Purpose          |
| -------------------------- | ---------------- |
| `REQUIRED`                 | Not null/empty   |
| `EMAIL`                    | Valid email      |
| `MIN_VALUE`, `MAX_VALUE`   | Numeric range    |
| `MIN_LENGTH`, `MAX_LENGTH` | String length    |
| `PATTERN`                  | Regex matching   |
| `EQUALS_FIELD`             | Field comparison |
| `EACH`                     | List validation  |
| `NESTED`                   | Nested object    |

---

### 2-2. Object Graph Navigation

Access nested properties using familiar notation.

```java
// Dot notation for nested objects

.field("user.address.street", "Street")
    .rule(S2RuleType.REQUIRED)

// Index notation for specific list elements

.field("orders[0].totalPrice", "First Order Total")
    .rule(S2RuleType.MIN_VALUE, 1000)

// Wildcard notation for all list elements

.field("items[].price", "Item Price")
    .rule(S2RuleType.MIN_VALUE, 0)
```

---

### 2-3. Recursive & Composite Validation

Reuse validators for hierarchical structures.

```java
// 1. Define sub-validator

S2Validator<ItemDTO> itemValidator = S2Validator.<ItemDTO>builder()
    .field("name", "Item Name").rule(S2RuleType.REQUIRED)
    .field("price", "Price").rule(S2RuleType.MIN_VALUE, 0)
    .build();

// 2. Reuse in parent validator

S2Validator<OrderDTO> orderValidator = S2Validator.<OrderDTO>builder()
    // Validate each item in a list (EACH)
    .field("items", "Item List")
        .rule(S2RuleType.EACH, itemValidator)

    // Validate a single nested object (NESTED)
    .field("shippingInfo", "Shipping Info")
        .rule(S2RuleType.NESTED, itemValidator)

    .build();
```

---

### 2-4. Custom Logic: Predicate & BiPredicate

Inject Lambda for complex business rules.

```java
// Single field validation (Predicate)

.field("age", "Age")
    .rule(val -> (Integer) val >= 18)
    .message("Only adults can sign up.")

// Multi-field validation (BiPredicate)
// Note: Custom lambdas skip execution on null/empty values by default.
// If the field is mandatory, always chain with REQUIRED.

.field("confirmPassword", "Confirm Password")
    .rule(S2RuleType.REQUIRED)
    .rule((val, target) -> {
        String password = S2Util.getValue(target, "password");
        return password.equals(val);
    })
    .message("Passwords do not match.")

// Complex business logic

.field("endDate", "End Date")
    .rule((val, target) -> {
        String startDate = S2Util.getValue(target, "startDate");
        return startDate.compareTo((String)val) <= 0;
    })
    .message("End date must be after start date.")

// If the custom lambda MUST evaluate even when value is null/empty (.includeEmpty())
// e.g., "Either primary or secondary contact is required"
.field("secondaryContact", "Secondary Contact")
    .rule((val, target) -> {
        return val != null || S2Util.isNotEmpty(S2Util.getValue(target, "primaryContact"));
    }).includeEmpty()
    .message("Either primary or secondary contact is required.")
```

> [!NOTE]
> **Custom Lambda Empty-Value (Short-Circuit) Policy:**
> Aligned with Bean Validation and YAVI conventions, custom lambda rules skip evaluation when the field value is `null` or empty (`S2Util.isEmpty(value)`), treating it as valid. This prevents accidental `NullPointerException`s on optional fields.
> - If a field is required, chain `.rule(S2RuleType.REQUIRED)`.
> - If the lambda itself needs to inspect empty/null values, specify `.includeEmpty()`.
> - Uncaught runtime exceptions inside lambdas are wrapped in `S2RuntimeException` with field path context and the original exception as cause.

> [!WARNING]
> Custom Lambda rules are **not** synchronized to JavaScript automatically. Use built-in `S2RuleType` for full client-server synchronization.

---

## 3. Messaging & Localization 🌍

### 3-1. Message Customization

Specify error messages at the field level.

```java
.field("email", "Email")
    .rule(S2RuleType.EMAIL)
    // Option 1: Message key (requires bundle setup)
    .message("validation.email.invalid")

    // Option 2: Direct message
    .message("Please enter a valid email address.")
```

### 3-2. Language-Specific Messages

Set messages for different locales.

```java
.field("password", "Password")
    .rule(S2RuleType.MIN_LENGTH, 8)
    // Set English message
    .en("Password must be at least 8 characters.")
    // Set French message
    .message(Locale.FRANCE, "Le mot de passe doit comporter au moins 8 caractères.")
```

### 3-3. Korean Particle Handling 🇰🇷

Automatically selects appropriate particles based on field label.

```java
// Automatic particle selection

.field("id", "ID")
    .rule(S2RuleType.REQUIRED)
    .message("{0} is required.")
    // Result: "아이디는 필수입니다." (auto particle)

.field("name", "Name")
    .rule(S2RuleType.REQUIRED)
    .message("{0} is required.")
    // Result: "이름은 필수입니다." (auto particle)

.field("email", "Email")
    .rule(S2RuleType.EMAIL)
    .message("{0} is invalid.")
    // Result: "이메일이 올바르지 않습니다." (auto particle)
```

**Supported Particles:**

- `{0|은/는}` → 은 / 는
- `{0|이/가}` → 이 / 가
- `{0|을/를}` → 을 / 를
- `{0|과/와}` → 과 / 와

---

## 4. End-to-End Implementation

### Step 1: Define Validation Rules

```java
// ServerController.java

@Controller
public class AuthController {

    // Reusable validation blueprint
    private S2Validator<SignupCommand> signupRules() {
        return S2Validator.<SignupCommand>builder()
            .field("userId", "User ID")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.MIN_LENGTH, 3)
                .rule(S2RuleType.MAX_LENGTH, 20)

            .field("email", "Email")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.EMAIL)

            .field("password", "Password")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.MIN_LENGTH, 8)
                .message("Must be at least 8 characters.")

            .field("confirmPassword", "Confirm Password")
                .rule(S2RuleType.REQUIRED)
                .rule(S2RuleType.EQUALS_FIELD, "password")
                .message("Passwords do not match.")

            .build();
    }
}
```

### Step 2: Serve Rules to Client

```java
// GET request: Serve validation rules to client

@GetMapping("/signup")
public String signupPage(
        @ModelAttribute("command") SignupCommand command,
        Model model) {

    // Extract rules as JSON
    String rules = S2BindValidator.context("signup", this::signupRules)
        .getRulesJson();

    model.addAttribute("rules", rules);
    return "signup";  // Thymeleaf template
}
```

### Step 3: Inject Rules into Form

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
<script type="module">
  import '/s2-util/js/s2.validator.js';
</script>
```

### Step 4: Server-Side Final Validation

```java
// POST request: Final server validation

@PostMapping("/signup")
public String signup(
        @ModelAttribute("command") SignupCommand command,
        BindingResult result,
        Model model) {

    // Reuse identical rules from GET
    S2BindValidator.context("signup", this::signupRules)
        .validate(command, result);

    if (result.hasErrors()) {
        // Return to form with validation errors
        return signupPage(command, model);
    }

    // All validation passed
    userService.createUser(command);
    return "redirect:/welcome";
}
```

---

## 5. Architecture Overview

### Component Diagram

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

### Asset Delivery

- **Location:** `s2-validator.js` inside `s2-validator.jar`
- **Path:** `META-INF/resources/s2-util/js/s2.validator.js`
- **Auto-binding:** Forms with `data-s2-rules` attribute are automatically monitored

---

## 6. Best Practices

```
1. ✅ Define rules once in a dedicated method

2. ✅ Use Pattern C (Registry) for high-traffic apps

3. ✅ Always perform server-side validation

4. ✅ Use built-in S2RuleType for client sync

5. ✅ Test validation on both server and client

6. ❌ Don't trust client-side validation alone

7. ❌ Don't hardcode error messages

8. ✅ Leverage Korean particle handling
```

---

## 7. Error Handling

### Server-Side Errors

```java
// Exception handling

try {
    S2Validator.of(data)
        .field("email").rule(S2RuleType.EMAIL)
        .validate();
} catch (S2RuntimeException e) {
    // Get detailed error information
    String message = e.getMessage();
    List<S2ErrorDetail> errors = e.getErrors();
}
```

### Spring Integration

```java
// BindingResult captures errors automatically

if (result.hasErrors()) {
    result.getAllErrors().forEach(error -> {
        System.out.println(error.getDefaultMessage());
    });
}
```

---

## 8. Performance Tips

```
1. Use Pattern C (Registry Mode) for validators

2. Cache validation results when possible

3. Avoid complex lambda rules in loops

4. Reuse validators (don't recreate)

5. Use PATTERN rule for string validation
```
