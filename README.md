# s2-util: Unified Dynamic Validator 🚀

🌐 **English** | [한국어](README.ko.md)

[![Java CI](https://github.com/devers2/s2-util/actions/workflows/ci.yml/badge.svg)](https://github.com/devers2/s2-util/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.devers2/s2-validator?color=brightgreen&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.devers2/s2-validator)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](./LICENSE)

> **"Write Once, Validate Anywhere."**
> The smartest way to validate both **Server (Java)** and **Client (JavaScript)** with a **single configuration**.
> 📖 Comprehensive developer guide and architecture: **[Developer Manual (MANUAL.md)](./MANUAL.md)**
>
> 📝 Release notes and upgrade notes: **[CHANGELOG.md](./CHANGELOG.md)**

---

## 📖 Overview

**s2-util** is a Java utility suite built around `s2-validator`, which validates on the server (Java) and in the browser (JavaScript) with the same rules, plus object copying, JSON and dynamic JPQL. It runs on Java 17+, and its hot paths use MethodHandles and caches.

---

## ✨ Why s2-util?

`s2-util` fills what standard Bean Validation (Hibernate Validator) leaves out — **browser validation, short conditional rules and Korean business rules**. It complements Bean Validation rather than replacing it:

- **🌐 Write Once, Validate Anywhere** — Export the Java rules with `getRulesJson()` and load `s2.validator.js`; the browser checks the same rules, with no validation code to write for the UI.
- **⚡ Short conditional rules** — Rules such as "B is required when A is X" take a line or two with `.when(...).and(...)`.
- **🏎️ Performance** — MethodHandle caching for field access, Caffeine (W-TinyLFU) caches for metadata, virtual threads on Java 21+ (detected at runtime).
- **🇰🇷 31 built-in rules & Korean messages** — Resident, business and alien registration numbers, mobile, phone and postal codes, byte length and more, with Korean particles that follow the final consonant (`{0|은/는}`, `{0|이/가}`).
- **🛡️ Build-time checks** — Field names are strings, so the companion `s2-validator-plugin` checks paths, criteria and missing terminal calls at build time (before `compileJava`).
- **🍃 Spring MVC integration** — `S2BindValidator` puts validation errors into Spring's standard `BindingResult`.

### 🥊 At a Glance: Standard Bean Validation vs s2-validator

| Item | Standard Bean Validation (Jakarta Validation) | s2-validator |
| :--- | :--- | :--- |
| **Conditional rules**<br>_(B required if A)_ | Groups, `@GroupSequenceProvider` or a class-level constraint (verbose) | `.when("type", "VIP").rule(REQUIRED)` |
| **Cross-field rules**<br>_(password confirm, date ranges)_ | Write a class-level constraint and bind it to the field with `addPropertyNode` | Built-in `EQUALS_FIELD`, `DATE_AFTER`, `DATE_BEFORE`; messages use the other field's label |
| **Browser validation** | Server only; the UI re-implements the rules in JS/TS (Zod, ...) | The same rules exported as JSON and checked by `s2.validator.js` |
| **Korean business rules & particles** 🇰🇷 | Write your own (rules, a consonant-aware `MessageInterpolator`) | Resident, business and alien registration numbers, phone and postal codes, byte length, ... built in; `{0\|은/는}` particles adjust automatically |
| **Rules as data** | Fixed in class annotations | Read and write JSON (database, configuration), validate a `Map` without a DTO, several rule sets per DTO (per screen or role) |
| **Where rules live, field names** | Annotations on the field, so **a field name cannot be misspelled** | Field names are strings; the companion plugin checks paths and criteria at build time |
| **Ecosystem** | **The standard.** Works with Spring `@Valid`, method validation, JPA, OpenAPI docs and more | Spring `BindingResult` integration |

> Which one fits, and how to use both: see "When to use" in the [`s2-validator` README](./s2-validator/README.md).

---

## 📦 Module Overview

| Module                                                       | Description                                                                                               |
| :----------------------------------------------------------- | :-------------------------------------------------------------------------------------------------------- |
| **[`s2-core`](./s2-core/README.md)**                         | High-performance Java utility toolkit (Reflection, Date/Time, String, System)                             |
| **[`s2-validator`](./s2-validator/README.md)**               | ⭐ Unified dynamic cross-platform validation engine & Spring binding integration                          |
| **[`s2-validator-plugin`](./s2-validator-plugin/README.md)** | Gradle static analysis plugin — catches DTO field typos and incomplete chains (dead code) at compile time |
| **[`s2-jpa`](./s2-jpa/README.md)**                           | JPA query helpers and dynamic entity specifications                                                       |

> [!TIP]
> **Looking for application-level utilities?**
> There is also **[`s2-kit`](https://github.com/devers2/s2-kit)** (formerly `s2-support`), shared team modules on top of `s2-core`: document conversion and merging (PDF, Office and Hangul), pagination (`S2PaginationInfo`), file management (`FileManager`), Spring and servlet helpers, and front-end JS/CSS.

---

## 🚀 Quick Start

### 1. Installation

Add the following dependency to your `build.gradle` (Maven Central).

**[Gradle]**

```groovy
dependencies {
    implementation 'io.github.devers2:s2-util:2.0.0'
}
```

**[Maven]**

```xml
<dependency>
    <groupId>io.github.devers2</groupId>
    <artifactId>s2-util</artifactId>
    <version>2.0.0</version>
</dependency>
```

> [!NOTE]
> `s2-util` is a bundle of `s2-core`, `s2-validator` and `s2-jpa` in one JAR. Next to the individual modules or a library that brings `s2-core` (such as [`s2-kit`](https://github.com/devers2/s2-kit)) the same classes would load twice, so in that case add only the individual modules you need (`s2-core`, `s2-validator`, `s2-jpa`) instead of the bundle.

**[Optional] `s2-validator-plugin` (Compile-Time Field & Chaining Static Analysis)**

Prevent runtime errors caused by typos or field name mismatches. When using Generics (e.g., `S2Validator.<UserCommand>builder()`), this plugin performs static analysis during the build to verify that all referenced field names actually exist in the specified DTO class. It triggers a build error if a non-existent field is detected.

```groovy
// settings.gradle
pluginManagement {
    repositories {
        mavenCentral()
    }
}

// build.gradle
plugins {
    id 'io.github.devers2.validator' version '2.0.0'
}
```

### 2. Usage

`s2-validator` supports two flexible approaches depending on whether client-side UI synchronization is needed:

- **Approach A: Standalone Backend Validation** — Simple, declarative validation for services, batches, or REST APIs with zero UI setup.
- **Approach B: Full-Stack Sync Validation** — Spring `BindingResult` integration and browser validation with the same rules, no validation code to write for the UI.

---

#### Approach A. Standalone Backend Validation (No Frontend Setup Required)

Validate any DTO, VO, or Map directly in your service or controller layer. When `.rule()` is omitted, the field is automatically treated as `REQUIRED`.

```java
// Option 1: Fail-fast mode — throws S2ValidationException immediately on the first failure
S2Validator.of(command)
    .field("name", "Name") // Rule omitted -> REQUIRED by default
    .field("email", "Email").rule(S2RuleType.EMAIL) // Specifying rules disables default REQUIRED (optional); add REQUIRED explicitly if needed
    .field("birthDate", "Birth Date").rule(S2RuleType.REQUIRED).rule(S2RuleType.DATE)
    // Custom lambda rule (Server-Only: executed on backend, not serialized to JS)
    .field("age", "Age").rule((Integer age) -> age != null && age >= 19).en("Must be 19 or older.")
    .validate();

// Option 2: Collect-all mode — passes errors to handler and returns boolean
List<S2ValidationError> errors = new ArrayList<>();
boolean isValid = S2Validator.of(command)
    .field("name", "Name")
    .field("email", "Email").rule(S2RuleType.EMAIL)
    .validate(errors::add, Locale.ENGLISH);

if (!isValid) {
    errors.forEach(err -> log.warn("{}: {}", err.fieldName(), err.defaultMessage()));
}
```

---

#### Approach B. Full-Stack Sync Validation (Server + Client)

Define validation once in Java and enforce it across both backend Spring `BindingResult` and native browser HTML forms with the same rules, with no validation code to write for the UI.

##### [Controller]

> **Note:** This example assumes Spring Framework integration. Without Spring, use the validator directly: `validator.validate(target, errorHandler)` on the server and `validator.getRulesJson(locale)` for the browser; only `BindingResult` integration is unavailable.

```java
private S2Validator<UserCommand> profileValidator() {
    return S2Validator.<UserCommand>builder()
            // If no rule is specified, S2RuleType.REQUIRED is applied by default
            // "Name" is the label used in error messages
            .field("name", "Name")
            .field("password", "Password")
            // When specifying explicit rules, REQUIRED must be added manually if needed
            .field("passwordCheck", "Confirm Password")
                .rule(S2RuleType.REQUIRED)
                // Verifies value equals "password" field
                .rule(S2RuleType.EQUALS_FIELD, "password")
                    // Set English error message
                    .en("Password check does not match.")
                    .message("Password check does not match.", Locale.ENGLISH)
                    // Set Korean error message
                    .ko("비밀번호가 일치하지 않습니다.")
                    // Set Hindi error message
                    .message("पासवर्ड मेल नहीं खाते.", Locale.forLanguageTag("hi"))
            .field("userType", "User Type")
            .field("paymentMethod", "Payment Method")
            .field("cardNumber", "Card Number")
                // ✨ Conditional validation: cardNumber if (USER + CREDIT_CARD) OR (SELLER)
                .when("userType", "USER").and("paymentMethod", "CREDIT_CARD")
                .when("userType", "SELLER")
            .build();
}

// Bind once and share it between the GET form and the POST handler (the request locale is applied on each call)
private final S2BindValidator.BoundContext<UserCommand> profile = S2BindValidator.bind(profileValidator());

@GetMapping("/sign-up")
public String signUpPage(@ModelAttribute("command") UserCommand command, Model model) {
    // Convert validator to JSON and pass to client for validation
    model.addAttribute("rules", profile.getRulesJson());
    return "sign-up";
}

@PostMapping("/sign-up")
public String signUp(@ModelAttribute("command") UserCommand command, BindingResult result, Model model) {
    // Perform server-side validation using the same validator configuration
    profile.validate(command, result);

    if (result.hasErrors()) {
        return signUpPage(command, model);
    }
    userService.createUser(command);
    return "redirect:/sign-in";
}
```

#### [HTML / Client]

> **Why does importing `s2.validator.js` work without copying any files?**
> Per the Servlet 3.0+ spec, all files inside `META-INF/resources/` in a JAR are automatically served as static web resources. So `s2.validator.js` is immediately available at `/s2-util/js/s2.validator.js` with no manual setup.

**Recommended Import Methods:**

- **Option A (Recommended — Thymeleaf `th:src`)**:

  ```html
  <!-- Bind server-generated JSON rules to the form -->
  <form
    id="joinForm"
    th:action="@{/member/join}"
    method="post"
    th:object="${member}"
    th:data-s2-rules="${validationRules}"
  >
    ...
    <button type="submit">Sign Up</button>
  </form>

  <!-- Import s2.validator.js — context-path-safe via Thymeleaf @{...} -->
  <script type="module" th:src="@{/s2-util/js/s2.validator.js}"></script>
  ```

- **Option B (Inline Script Dynamic Import)**:
  ```html
  <script type="module">
    // Context-path-safe dynamic import (works under any deployment path, e.g. /app)
    // A plain '/s2-util/js/s2.validator.js' only works at the server root (/).
    const contextPath = /*[[@{/}]]*/ '';
    import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);
  </script>
  ```

**Zero-Code Client Automatic Validation:**

Once `s2.validator.js` is loaded, `initS2Validator()` runs automatically — no validation code to write:

1. Disables native browser validation (`noValidate`) on all forms with `data-s2-rules`; uses `MutationObserver` to cover dynamically added forms (SPAs, modals)
2. Intercepts form submit events and parses the JSON rules
3. On failure: calls `e.preventDefault()`, focuses the first invalid field, and shows a localized error tooltip via `form.reportValidity()`
4. Real-time reset: clears error state as soon as the user types (`input`) or changes selection (`change`)

**Practical Tips:**

- **AJAX / Fetch Validation**: When submitting via `fetch` or `axios`, call `S2Validator.validate()` manually:

  ```html
  <script type="module" th:inline="javascript">
    const contextPath = /*[[@{/}]]*/ '';
    const { S2Validator } = await import(
      `${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`
    );

    document.getElementById('ajaxBtn').addEventListener('click', async () => {
      const errors = S2Validator.validate('#joinForm');
      if (Object.keys(errors).length > 0) return; // abort if invalid

      const formData = new FormData(document.getElementById('joinForm'));
      await fetch('/api/member/join', { method: 'POST', body: formData });
    });
  </script>
  ```

- **Hidden Input Error Display (`{fieldName}_error`)**: Hidden inputs or custom UI widgets cannot show native tooltips. Add a proxy element named `{fieldName}_error` and S2Validator will auto-populate it:
  ```html
  <input type="hidden" name="profileImage" />
  <span name="profileImage_error" style="color: red; font-size: 12px;"></span>
  ```

> For the full client integration guide (import maps, per-field customization, etc.), see the [`s2-validator` README](./s2-validator/README.md#client-side-view-integration-thymeleaf--html-guide).

---

## 📦 Core Modules

### 1. **`s2-core`** — Foundation Library

[s2-core/README.md](./s2-core/README.md)

The foundational library providing high-performance core utility classes. Features include:

- **High-Performance Reflection**: Method Handle-based reflection with JIT optimization
- **Intelligent Caching**: Built-in optimized lightweight cache (concurrent-safe, zero-dependency) with optional Caffeine support for extreme high-concurrency environments
- **Java Version-Adaptive Thread Factory**: Virtual Thread support (Java 21+) with platform thread fallback
- **Optimized Data Access**: `getValue()` and `setValue()` with dot notation and bracket indexing support
- **Comprehensive Utilities**: String manipulation, date/time handling, type conversion, and more

---

### 2. **`s2-validator`** — Unified Validation Library ⭐

[s2-validator/README.md](./s2-validator/README.md)

A unified cross-platform validation library supporting both server and client with single configuration. Features include:

- **Fluent API**: Natural, chainable validation rules with sequential method application
- **30+ Built-in Rule Types**: REQUIRED, LENGTH, REGEX, EMAIL, MPHONE_NO, DATE, and more
- **Korea-specific Rules**: MPHONE_NO, TEL_NO, ZIP, BIZRNO, NWINO, JUMIN, PASSWORD_ANSWR
- **Advanced Nested Object Support**: Dot notation (`user.address.street`) and bracket indexing (`items[0]`)
- **Comprehensive i18n**: Message localization with `ko()`, `en()`, custom locales, and `S2ResourceBundle`
- **Custom & Conditional Validation**: `CustomRule` interface and `when()`/`and()` conditional logic
- **Spring Integration** (Optional): `S2BindValidator` with `BindingResult` for standard Spring error handling

---

### 3. **`s2-validator-plugin`** — Gradle Build Plugin

[s2-validator-plugin/README.md](./s2-validator-plugin/README.md)

A Gradle build plugin for static source code analysis to validate `s2-validator` field names and chaining completeness at compile-time. Features include:

- **Static Analysis**: JavaParser AST parsing for accurate code analysis
- **Compile-Time Field Validation**: Detects typos and non-existent fields before runtime
- **Chaining Completeness Check (Dead Code Prevention)**: Detects missing terminal methods (`.validate()` / `.build()`) and fails the build immediately
- **Multi-Project Support**: Scans all subprojects and modules
- **Zero Configuration**: Automatically integrates with standard Gradle build tasks
- **Smart Validation**: Skips validation for generic wildcards and incomplete type information
- **Detailed Error Reporting**: Color-coded messages with file paths and line numbers

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

[//]: # 'S2_DEPS_INFO_START'

---

**To use certain functionalities (e.g., S2BindValidator), the end-user project must explicitly add the following dependencies to be available at runtime.** Failure to include these dependencies will result in a `java.lang.NoClassDefFoundError` at runtime.

**[For Gradle Users]**

```groovy
dependencies {
    // Essential runtime dependencies for optional functionalities
    implementation 'com.github.ben-manes.caffeine:caffeine:3.3.0'
    implementation 'org.springframework:spring-context:6.2.19'
    implementation 'org.springframework.boot:spring-boot-autoconfigure:3.5.16'
    implementation 'jakarta.persistence:jakarta.persistence-api:3.2.0'
}
```

[//]: # 'S2_DEPS_INFO_END'
