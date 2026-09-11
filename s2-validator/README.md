# s2-validator — Unified Dynamic Validation Library (s2-util)

🌐 **English** | [한국어](README.ko.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.devers2/s2-validator?color=brightgreen&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.devers2/s2-validator)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](../LICENSE)

> 📦 Part of the **[s2-util suite](../README.md)**.
> Works seamlessly with companion library **[`s2-support`](https://github.com/devers2/s2-support)** for extended pagination, file management, and Spring utilities.

---

## 📖 Overview

The **s2-validator** module is a unified cross-platform validation framework that enables single-configuration validation rules for both server and client environments. It provides a fluent, chainable API supporting comprehensive validation rules, conditional validation, complex custom rules, and full internationalization support (i18n). The module excels at validating nested objects with dot notation (e.g., `user.address.street`) and collection items (e.g., `items[0]`), making it ideal for complex DTO/VO hierarchies.

---

## 🥊 Why s2-validator? (vs Bean Validation)

While standard Bean Validation (JSR-380 / Hibernate Validator) works well for static, simple constraints, modern real-world enterprise applications frequently hit its limitations. **s2-validator eliminates the traditional "annotation hell" and redundant client-side coding:**

| Feature / Challenge | Standard Bean Validation (JSR-380) | ⭐ s2-validator |
| :--- | :--- | :--- |
| **Conditional Validation**<br>*(e.g. Field B is required only if A == 'X')* | Requires verbose custom validator classes or complex `@GroupSequenceProvider` (code explosion) ❌ | Fluent and expressive in just two lines:<br>`.when("paymentMethod", "CARD")`<br>`.rule(S2RuleType.REQUIRED)` ✅ |
| **Cross-Field Comparison**<br>*(Password confirm, Date ranges)* | Requires class-level annotations; errors are bound to root object (Global Error), making field-specific UI display awkward ❌ | Directly binds errors to the exact target field:<br>`.rule(S2RuleType.EQUALS_FIELD, "password")`<br>`.rule(S2RuleType.DATE_AFTER, "startDate")` ✅ |
| **Client-Side Sync**<br>*(Browser UI validation)* | Server-only. Frontend developers must **re-implement identical rules & regex in JS/TS** (Zod, Yup, etc.) ❌ | **Write Once, Validate Anywhere**: Export rules via `getRulesJson()` and import `s2.validator.js` — **zero frontend code** required for native browser tooltips & auto-focus ✅ |
| **Korean Particle Grammar** 🇰🇷<br>*(Natural error messages)* | Not supported out-of-the-box. Requires implementing a custom `MessageInterpolator` ❌ | Built-in smart particle interpolation:<br>`{0\|은/는}`, `{0\|이/가}`, `{0\|을/를}`, `{0\|과/와}` automatically adjust based on final consonants ✅ |
| **Compile-Time Typo Safety** | Runtime failures if field names are misspelled in reflection/templates ❌ | Companion `s2-validator-plugin` uses **AST static analysis** to block builds on typos before hitting runtime 🛡️ ✅ |
| **Dynamic Data / Map Validation** | Extremely cumbersome without declaring formal DTO classes ❌ | Validate unstructured data immediately without DTOs:<br>`S2Validator.of(map)...validate()` ✅ |

---

## ✨ Key Features

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
   - Default built-in messages for Korean and English on every rule
   - Custom message overrides via `.ko()` and `.en()` methods
   - Custom locale support via `.message(Locale, String)` method (e.g., Japanese, Chinese, etc.)
   - `S2ResourceBundle` integration for centralized message management
   - Message parameter substitution: `{0}`, `{1}` for field names and rule values
   - Korean particle support: `{0|은/는}`, `{0|이/가}`, `{0|을/를}`, `{0|과/와}` for grammatically correct messages

7. **Custom & Conditional Validation**
   - Functional interfaces (`Predicate`, `BiPredicate`) for user-defined validation logic
   - Reusable validation rules as standalone objects
   - `when()` with `and()` for complex AND conditions
   - Multiple `when()` chains for OR logic

8. **Error Handling & Reporting**
   - Comprehensive error information: field name, error code, error message, default message
   - Consumer-based error handler for flexible error processing
   - Fail-fast mode vs. collect-all-errors mode
   - Circular reference detection for safe nested object validation

9. **Spring Framework Integration (Optional)**
   - `S2BindValidator` for Spring Data Binding with `BindingResult`
   - Supplier pattern for lazy evaluation and convenient rule management
   - Unified validation rules: Same ruleset for both server and client
   - Standard Spring error handling with automatic error field binding

---

## 🚀 Quick Start

### 1. Installation

Add the following dependency to your `build.gradle` or `pom.xml`.

**[Gradle]**

```groovy
dependencies {
    implementation 'io.github.devers2:s2-validator:1.1.8'

    // (Optional) Required only when using Spring integration (S2BindValidator)
    implementation 'org.springframework:spring-context:6.2.19'
}
```

**[Maven]**

```xml
<dependency>
    <groupId>io.github.devers2</groupId>
    <artifactId>s2-validator</artifactId>
    <version>1.1.8</version>
</dependency>
```

#### Optional: `s2-validator-plugin` (Compile-Time Static Analysis & Dead Code Prevention)

You can optionally add the **`s2-validator-plugin`** Gradle plugin to perform AST-based static source code analysis **at build time**:
- **Field Name Validation**: Catches typos and refactoring regressions in `.field("name")` against target DTO classes.
- **Chaining Completeness Check (Dead Code Prevention)**: Detects incomplete validator chains where terminal methods are missing (`of()` without `.validate()`, `builder()` without `.build()`, `check()` without `.validate()`) and fails the build immediately.

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
    id 'io.github.devers2.validator' version '1.1.3'
}
```

> [!NOTE]
> This is a **Gradle plugin**, not a library dependency. Add it to the `plugins {}` block, **not** the `dependencies {}` block. Maven is not supported.

---

### 2. Usage

S2Validator supports two primary execution patterns:
1. **Standalone Backend Validation (No Client Setup)**: Immediate inline validation via `S2Validator.of(...)` — perfect for REST APIs, batch jobs, and service layers.
2. **Full-Stack Form Synchronization (Server + Client)**: Define rules once and bind to Spring `BindingResult` + browser tooltips via `S2BindValidator` (see [Section 2.7](#27-spring-framework-integration-s2bindvalidator)).

#### 2.1. Basic Validation (Immediate Mode vs. Blueprint Mode)

- **Immediate Mode (`S2Validator.of`)**: Perform one-off validation directly on a target object (DTO/VO or Map) with zero client setup. If `.rule(...)` is omitted, `S2RuleType.REQUIRED` is enforced automatically.
  - Call `.validate()` directly to throw an `S2RuntimeException` immediately on the first error (Fail-Fast mode).
  - Pass an error handler (`Consumer<S2ValidationError>`) to collect all errors without throwing exceptions.
- **Blueprint / Builder Mode (`S2Validator.builder`)**: Define a reusable validation blueprint. Built `S2Validator` instances are thread-safe and can be cached and reused across requests for optimal performance in high-concurrency environments.

##### Immediate Mode (`S2Validator.of`)

```java
Map<String, Object> data = new HashMap<>();
data.put("userId", "admin");
data.put("email", "test@s2.kr");
data.put("birthDate", "20250101");

// 1) Fail-fast mode: Throws S2RuntimeException immediately on the first failure
// Ideal when client integration is not required (e.g., REST APIs, service layer)
S2Validator.of(data)
    .field("userId", "User ID") // Rule omitted -> REQUIRED by default
    .field("email", "Email").rule(S2RuleType.EMAIL) // Specifying rules disables default REQUIRED (optional); add REQUIRED explicitly if needed
    .field("birthDate", "Birth Date").rule(S2RuleType.REQUIRED).rule(S2RuleType.DATE)
    .validate();

// 2) Collect-all mode: Collect all errors with an error handler without throwing
List<S2ValidationError> errors = new ArrayList<>();
boolean isValid = S2Validator.of(data)
    .field("userId", "User ID")
    .field("age", "Age").rule(S2RuleType.MIN_VALUE, 19)
    .field("email", "Email").rule(S2RuleType.EMAIL)
    .validate(errors::add, Locale.ENGLISH);

if (!isValid) {
    errors.forEach(err -> System.out.println(err.fieldName() + ": " + err.defaultMessage()));
}
```

##### Blueprint / Builder Mode (`S2Validator.builder`)

```java
// Define reusable validator blueprint
S2Validator<UserVO> userValidator = S2Validator.<UserVO>builder()
    .field("userId", "User ID").rule(S2RuleType.REQUIRED)
    .field("userPw", "Password").rule(S2RuleType.MIN_LENGTH, 8)
    .field("email", "Email").rule(S2RuleType.EMAIL)
    .build();

// Reuse across requests and threads safely
UserVO user = new UserVO();
user.setUserId("user01");
user.setUserPw("pass1234");
user.setEmail("user01@s2.kr");

List<S2ValidationError> errors = new ArrayList<>();
boolean isValid = userValidator.validate(user, errors::add, Locale.ENGLISH);
```

---

#### 2.2. Single Value Check (`S2Validator.check`)

Quickly validate an individual value or variable without creating a DTO or Map:
- **Boolean Mode (`check(value)`)**: Returns `true`/`false` without throwing exceptions.
- **Exception Mode with Label (`check(value, label)`)**: Throws `S2RuntimeException` with a localized message on failure.
- **Custom Predicate**: Validate with custom lambda functions.

```java
// 1) Boolean Mode: returns true/false without throwing exceptions
boolean isValidEmail = S2Validator.check("test@s2.kr")
    .rule(S2RuleType.REQUIRED)
    .rule(S2RuleType.EMAIL)
    .validate();

// 2) Exception Mode with Label: throws S2RuntimeException on failure
S2Validator.check(userInput, "Name")
    .rule(S2RuleType.REQUIRED) // Uses default message or .en() for customization
    .validate();

// 3) Custom Predicate validation
boolean isAdult = S2Validator.check(25)
    .rule((Integer age) -> age >= 19)
    .validate();
```

---

#### 2.3. Cross-Field Validation

Validate relationships between multiple fields (e.g., date ranges, password confirmation):
- **Type-based rules**: Use built-in rules like `DATE_AFTER`, `DATE_BEFORE`, and `EQUALS_FIELD`.
- **BiPredicate custom rule**: Access both the field value and the entire target object `(value, target) -> boolean`.

```java
Map<String, Object> form = new HashMap<>();
form.put("startDate", "2025-01-01");
form.put("endDate", "2025-01-10");
form.put("password", "s2secret123");
form.put("confirmPassword", "s2secret123");

// 1) Built-in type-based cross-validation
S2Validator.of(form)
    .field("startDate", "Start Date").rule(S2RuleType.DATE_BEFORE, "endDate")
    .field("endDate", "End Date").rule(S2RuleType.DATE_AFTER, "startDate")
    .field("confirmPassword", "Confirm Password").rule(S2RuleType.EQUALS_FIELD, "password")
    .validate(errors::add, Locale.ENGLISH);

// 2) Custom cross-validation with BiPredicate (value, target)
S2Validator.of(form)
    .field("confirmPassword", "Confirm Password")
    .rule((value, target) -> {
        String password = S2Util.getValue(target, "password", "");
        return password.equals(value);
    }).en("{0} must match original password.")
    .validate(errors::add, Locale.ENGLISH);
```

---

#### 2.4. Conditional Validation (`when`, `and`)

Apply validation rules only when specified conditions are met (`String`, `Boolean`, `Number`, etc.):
- **Single condition**: `.when("fieldName", value)`
- **AND condition**: `.when(...).and(...)`
- **OR condition**: Multiple `.when(...)` chains

```java
S2Validator<Map<String, Object>> validator = S2Validator.<Map<String, Object>>builder()
    // Simple condition: required only when isPremium == true
    .field("premiumEmail", "Premium Email")
        .when("isPremium", true)
        .rule(S2RuleType.REQUIRED)

    // AND condition: useAdvanced == 'Y' AND advancedMode == 'FULL'
    .field("advancedOption", "Advanced Option")
        .when("useAdvanced", "Y").and("advancedMode", "FULL")
        .rule(S2RuleType.REQUIRED)

    // OR condition: useAdvanced == 'Y' OR isTest == 'true'
    .field("otherOption", "Other Option")
        .when("useAdvanced", "Y").when("isTest", "true")
        .rule(S2RuleType.REQUIRED)
    .build();
```

---

#### 2.5. Nested Objects & Collections

Effortlessly navigate and validate complex hierarchical structures:
- **Dot Notation (`user.address.street`) & Bracket Notation (`items[0].name`)**
- **Wildcard Syntax (`products[].name`)**: Automatically iterates and validates every element in a collection or array. Supports per-item conditional validation (e.g., `.when("products[].type", "NORMAL")`).
- **Recursive Sub-Validators (`NESTED` & `EACH`)**: Validate single nested objects (`NESTED`) or collection items (`EACH`) using child validator blueprints.

```java
// 1) Dot Notation & Bracket Notation
S2Validator<Map<String, Object>> dotValidator = S2Validator.<Map<String, Object>>builder()
    .field("user.address.street", "Street Address").rule(S2RuleType.REQUIRED)
    .field("users[0].name", "First User Name").rule(S2RuleType.REQUIRED)
    .build();

// 2) Wildcard Syntax: automatically iterates collection / array items
S2Validator<Map<String, Object>> wildcardValidator = S2Validator.<Map<String, Object>>builder()
    .field("products[].name", "Product Name").rule(S2RuleType.REQUIRED)
    .field("products[].price", "Price").rule(S2RuleType.MIN_VALUE, 0)
    .when("products[].type", "NORMAL") // Per-item conditional validation supported
    .build();

// 3) Recursive Sub-Validators (NESTED & EACH)
S2Validator<AddressVO> addressValidator = S2Validator.<AddressVO>builder()
    .field("zipCode", "Postal Code").rule(S2RuleType.REQUIRED)
    .field("street", "Street Address").rule(S2RuleType.REQUIRED)
    .build();

S2Validator<ItemVO> itemValidator = S2Validator.<ItemVO>builder()
    .field("itemId", "Item ID").rule(S2RuleType.REQUIRED)
    .field("quantity", "Quantity").rule(S2RuleType.MIN_VALUE, 1)
    .build();

S2Validator<OrderVO> orderValidator = S2Validator.<OrderVO>builder()
    .field("orderId", "Order ID").rule(S2RuleType.REQUIRED)
    .field("shippingAddress", "Shipping Address").rule(S2RuleType.NESTED, addressValidator) // Single nested object
    .field("items", "Order Items").rule(S2RuleType.EACH, itemValidator)                   // Collection / array elements
    .build();
```

---

#### 2.6. Built-in Messages & Localization

- **Default Built-in Messages**: Every built-in rule in `S2RuleType` already provides default messages for both English and Korean out-of-the-box (e.g., `REQUIRED` generates `"{0} is required."` for English, and `"{0|은/는} 필수 입력 항목입니다."` for Korean). **You do not need to call `.en()` or `.ko()` in standard situations.**
- **Custom Messages (`.en()`, `.ko()`)**: Use `.en("...")` or `.ko("...")` only when you need to customize or override the default message for a specific rule.
- **Other Languages (`.message(Locale, String)`)**: For languages other than English and Korean (e.g., Japanese, Chinese, French, etc.), use `.message(Locale locale, String template)` (e.g., `.message(Locale.JAPANESE, "{0}は必須入力項目です。")`).
- **Message Placeholders**:
  - `{0}`: Field label (e.g., "User ID", "Username").
  - `{1}`: Rule criteria value (e.g., minimum length, numeric threshold).
- **Korean Postposition (Josa) Support**: `{0|은/는}`, `{0|이/가}`, `{0|을/를}`, `{0|과/와}` automatically select the grammatically correct Korean particle based on whether `{0}` ends with a final consonant.
- **Global ResourceBundle**: Centralize messages across the application using `S2Validator.setValidationBundle("messages/validation")`.

```java
S2Validator<UserVO> validator = S2Validator.<UserVO>builder()
    // 1) Default message usage: No .en()/.ko() needed!
    //    EN -> "User ID is required." | KO -> "User ID는 필수 입력 항목입니다."
    .field("userId", "User ID").rule(S2RuleType.REQUIRED)

    // 2) Custom message override via .en(), .ko()
    .field("userPw", "Password")
        .rule(S2RuleType.MIN_LENGTH, 8)
            .en("{0} must be at least {1} characters for security.")
            .ko("{0|은/는} 보안을 위해 최소 {1}자 이상이어야 합니다.")

    // 3) Support for other languages via .message(Locale, String)
    .field("email", "Email")
        .rule(S2RuleType.EMAIL)
            .message(Locale.JAPANESE, "{0}の形式が正しくありません。")
            .message(Locale.SIMPLIFIED_CHINESE, "{0}格式不正确。")
    .build();

// Global ResourceBundle integration (e.g., messages/validation_en.properties)
S2Validator.setValidationBundle("messages/validation");
```

---

#### 2.7. Spring Framework Integration (`S2BindValidator`)

Seamlessly bridges `s2-validator` validation with Spring MVC's `BindingResult`, and exposes the exact same server rules to client-side JavaScript (`s2.validator.js`) via `getRulesJson()`.

```java
@Controller
@RequestMapping("/member")
public class MemberController {

    // 1) Supplier pattern for validation rules (compiled once and cached)
    private S2Validator<MemberDTO> memberRules() {
        return S2Validator.<MemberDTO>builder()
            .field("userId", "User ID").rule(S2RuleType.REQUIRED)
            .field("userPw", "Password").rule(S2RuleType.MIN_LENGTH, 8)
            .field("confirmPw", "Confirm Password")
                .rule((value, target) -> S2Util.getValue(target, "userPw", "").equals(value))
                .en("Passwords do not match.")
            .field("email", "Email").rule(S2RuleType.EMAIL)
            .build();
    }

    // 2) GET: Pass rules JSON to the view for client-side validation
    @GetMapping("/join")
    public String joinForm(Model model) {
        model.addAttribute("member", new MemberDTO());
        // Generate JSON metadata for client-side s2.validator.js
        String rulesJson = S2BindValidator.context("MEMBER_JOIN", this::memberRules).getRulesJson();
        model.addAttribute("validationRules", rulesJson);
        return "member/join";
    }

    // 3) POST: Execute server-side validation and automatically bind to BindingResult
    @PostMapping("/join")
    public String joinSubmit(@ModelAttribute MemberDTO member, BindingResult result, Model model) {
        // Execute server validation: automatically registers field errors to BindingResult
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

##### Client-Side View Integration (Thymeleaf & HTML Guide)

###### 1) HTML Form Markup

Bind the JSON rules string passed from the controller to the form's `th:data-s2-rules` attribute:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Member Registration</title>
</head>
<body>
    <h2>Join Us</h2>

    <!-- Pass the server-generated JSON rules via th:data-s2-rules -->
    <form id="joinForm" th:action="@{/member/join}" method="post"
          th:object="${member}" th:data-s2-rules="${validationRules}">

        <div>
            <label for="userId">User ID:</label>
            <input type="text" id="userId" th:field="*{userId}" />
            <span th:errors="*{userId}" style="color: red;"></span>
        </div>

        <div>
            <label for="userPw">Password:</label>
            <input type="password" id="userPw" th:field="*{userPw}" />
            <span th:errors="*{userPw}" style="color: red;"></span>
        </div>

        <div>
            <label for="confirmPw">Confirm Password:</label>
            <input type="password" id="confirmPw" th:field="*{confirmPw}" />
            <span th:errors="*{confirmPw}" style="color: red;"></span>
        </div>

        <button type="submit">Sign Up</button>
    </form>

    <!-- Import s2.validator.js -->
    <script type="module" th:src="@{/s2-util/js/s2.validator.js}"></script>
</body>
</html>
```

###### 2) JavaScript Import & Static Resource Mechanism

> [!NOTE]
> **Why does importing `s2.validator.js` work without copying any files?**
>
> According to the Servlet 3.0+ specification and Spring Boot's default static resource handling conventions, all files located inside a library JAR's `META-INF/resources/` directory are automatically served as static web resources from the root (`/`) path.
>
> Because `s2.validator.js` is packaged inside `s2-validator.jar` at `META-INF/resources/s2-util/js/s2.validator.js`, you do not need to download or copy the file to your project's `static` folder. The browser can access it directly via `/s2-util/js/s2.validator.js`!

**Recommended Import Methods:**

- **Option A (Recommended - Thymeleaf `th:src`)**:
  Handles dynamic deployment context paths (e.g., `/`, `/my-app`) automatically via Thymeleaf's `@{...}` syntax:
  ```html
  <script type="module" th:src="@{/s2-util/js/s2.validator.js}"></script>
  ```

- **Option B (Inline Script Dynamic Import)**:
  Dynamically imports the module within an inline script block using Thymeleaf's evaluated context path:
  ```html
  <script type="module" th:inline="javascript">
      const contextPath = /*[[@{/}]]*/ '';
      import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);
  </script>
  ```

- **Option C (Import Map for Modular / AJAX Usage)**:
  Defines an import map for clean ES module imports, especially useful for manual AJAX validation:
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
      // Use S2Validator where needed
  </script>
  ```

###### 3) Zero-Code Client Automatic Validation

When `s2.validator.js` is loaded, `initS2Validator()` runs automatically. You do **not** need to write any JavaScript code; everything happens out-of-the-box:

1. **Disables Native Validation (`noValidate`)**: Automatically sets `form.noValidate = true` on all forms with `data-s2-rules` to prevent browser raw English tooltips, using `MutationObserver` to also cover dynamic forms (SPAs, modals, AJAX).
2. **Intercepts Submit Events**: Listens for document-level form submissions (clicks or Enter key) and parses the JSON rules.
3. **Blocks Submit & Focuses First Error**: If any validation fails, calls `e.preventDefault()`, automatically focuses the first invalid element, and displays the localized error message using the browser's native tooltip via `form.reportValidity()`.
4. **Real-time Error Reset**: Automatically clears the error state (`setCustomValidity('')`) as soon as the user starts typing (`input`) or changes selection (`change`).

###### 4) Practical Tips for Beginners

- **Tip 1: Manual Validation for AJAX / Fetch**:
  When submitting data via `fetch` or `axios` instead of standard form submission, invoke `S2Validator.validate()` directly:
  ```html
  <script type="module" th:inline="javascript">
      const contextPath = /*[[@{/}]]*/ '';
      const { S2Validator } = await import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);

      document.getElementById('ajaxSubmitBtn').addEventListener('click', async () => {
          // Run form validation (focuses first error and shows tooltip automatically)
          const errors = S2Validator.validate('#joinForm');

          // Abort if errors exist
          if (Object.keys(errors).length > 0) {
              console.warn('Validation errors:', errors);
              return;
          }

          // Proceed with AJAX request if valid
          const formData = new FormData(document.getElementById('joinForm'));
          const response = await fetch('/api/member/join', {
              method: 'POST',
              body: formData
          });
      });
  </script>
  ```

- **Tip 2: Proxy Error Elements for Hidden Inputs (`{fieldName}_error`)**:
  Hidden inputs (`<input type="hidden">`) or custom UI widgets cannot show browser native tooltips. Add a proxy element named `{fieldName}_error` (e.g., a `span` or `div`), and S2Validator will automatically populate it with the error message text:
  ```html
  <!-- Hidden input -->
  <input type="hidden" name="profileImage" />

  <!-- Proxy error element for displaying error text -->
  <span name="profileImage_error" style="color: red; font-size: 12px;"></span>
  ```

---

#### 2.8. Compile-Time Static Analysis & Chaining Completeness (`s2-validator-plugin`)

When you add the optional **`s2-validator-plugin`** (see [Installation](#1-installation)), the plugin performs **static analysis (AST-based)** of your project's source code **before `compileJava`** runs. It inspects every `.field("fieldName")` call to verify that the field exists on the target class, and verifies that every validator chain is properly terminated with `.validate()` or `.build()`.

**What it catches:**

- **Field Name Typos**: Misspelling field names (e.g., `.field("userNaem")` when the actual field is `userName`)
- **Non-existent Field References**: Referencing fields that do not exist on the target DTO/VO class
- **Refactoring Regressions**: Outdated field references left after renaming or refactoring DTO fields
- **Incomplete Chaining (Dead Code)**:
  - Omitting `.validate()` on `S2Validator.of(...)` chains — validation is **never executed**
  - Omitting `.build()` on `S2Validator.builder()` chains — validator is **never created**
  - Omitting `.validate()` on `S2Validator.check(...)` chains — check is **never performed**

**Example — build fails immediately with clear error reporting:**

```
> Task :compileJava FAILED

[S2Validator Field Check Error]
❌ 1 file(s) contained invalid field names.
  📄 src/main/java/com/example/UserController.java
    ⚠️  Line 42: 'address' (method: field) field not found in UserDTO

[S2Validator Chaining Error]
🚫 1 file(s) contained 1 incomplete chaining error(s) (Dead Code).
   Validation logic is NEVER executed unless properly terminated!
  📄 src/main/java/com/example/UserService.java
    🚫 Line 28: S2Validator.of() chain does not end with .validate() (dead code)
```

**Key behaviors:**

- Zero configuration required — auto-activates on `compileJava`, `check`, and `bootRun` tasks
- Supports class inheritance: checks fields declared in parent classes as well
- Chaining completeness check: prevents dead validation code from reaching production
- Works in multi-project builds
- Requires `s2-validator` 1.1.0+, Java 17+, Gradle 8.0+

For full plugin documentation, see the [`s2-validator-plugin` README](../s2-validator-plugin/README.md).

---

## ⚙️ Requirements

This project is built with **JDK 21**, but it can be used reliably in all environments running **Java 17 or higher**.

---

## 📜 License & Copyright

This library is provided under the **Apache License 2.0**. You are free to use, modify, and distribute this software, provided that you comply with the obligations of the license (such as copyright notice and source code disclosure requirements). For detailed terms and conditions, please refer to the **[LICENSE](./LICENSE)** file.

- **Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)**
- Contact: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**Third-party Notice:** This project uses external libraries. For detailed third-party license notices, please refer to the **[licenses/NOTICE](./licenses/NOTICE)** file.

---

s2-validator Version: 1.1.8 (2026-09-11)

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
