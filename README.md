# S2Util: Unified Dynamic Validator 🚀

[English](README.md) | [한국어](README.ko.md)

[![Java CI](https://github.com/devers2/s2-util/actions/workflows/ci.yml/badge.svg)](https://github.com/devers2/s2-util/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.devers2/s2-validator?color=brightgreen&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.devers2/s2-validator)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](./LICENSE)

> **"Write Once, Validate Anywhere."**  
> The smartest way to validate both **Server (Java)** and **Client (JavaScript)** with a **single configuration**.

---

## 📖 Overview

**S2Util** is a high-performance Java utility library featuring a **Unified Dynamic Validator** that seamlessly synchronizes validation logic between Server (Java) and Client (JavaScript). Designed for **production-ready** environments, it leverages advanced technologies like Method Handles and intelligent caching to ensure maximum efficiency and type safety.

---

## ✨ Why S2Util?

- **⚡ Fluent Chainable API** — Declarative, readable rule definitions with nested dot-notation (`user.address.street`) and collection bracket support (`items[0].name`)
- **🏎️ Extreme Performance** — `MethodHandle` caching eliminates reflection overhead; Caffeine (W-TinyLFU) intelligent caching; Java 21+ Virtual Thread scalability
- **🌐 Cross-Platform Synchronization** — Author rules once in Java, export directly to client-side validation (JavaScript / TypeScript)
- **🇰🇷 30+ Built-in Rules & Smart i18n** — Email, URL, Phone, Business ID, and more; full i18n with natural Korean particle interpolation (`{0|은/는}`, `{0|이/가}`)
- **🛡️ Zero-Typo Static Analysis** — The companion `s2-validator-plugin` catches DTO field typos at compile time, before they hit runtime
- **🍃 Optional Spring Integration** — `S2BindValidator` for seamless `BindingResult` binding

---

## 📦 Module Overview

| Module | Description |
| :--- | :--- |
| **[s2-core](./s2-core/README.md)** | High-performance Java utility toolkit (Reflection, Date/Time, String, System) |
| **[s2-validator](./s2-validator/README.md)** | ⭐ Unified dynamic cross-platform validation engine & Spring binding integration |
| **[s2-validator-plugin](./s2-validator-plugin/README.md)** | Gradle static analysis plugin — catches DTO field typos at compile time |
| **[s2-jpa](./s2-jpa/README.md)** | JPA query helpers and dynamic entity specifications |

> [!TIP]
> **Looking for application-level utilities?**  
> Check out the companion library **[`s2-support`](https://github.com/devers2/s2-support)**, which builds upon `s2-core` and `s2-validator` to provide ready-to-use pagination (`S2PaginationInfo`), file management (`FileManager`), Spring utilities (`S2ContextUtil`), and more.

---

## 🚀 Quick Start

### 1. Installation

Add the following dependency to your `build.gradle` (Maven Central).

**[Gradle]**

```groovy
dependencies {
    implementation 'io.github.devers2:s2-util:1.1.7'
}
```

**[Maven]**

```xml
<dependency>
    <groupId>io.github.devers2</groupId>
    <artifactId>s2-util</artifactId>
    <version>1.1.7</version>
</dependency>
```

**[Optional] S2Validator Static Analysis Plugin**

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
    id 'io.github.devers2.validator' version '1.1.2'
}
```

### 2. Usage

Unified validation for server and client.

#### [Controller]

> **Note:** This example assumes Spring Framework integration. If Spring is not available, you can use `S2Validator` and `S2ValidatorFactory` directly, but `BindingResult` integration will not be available.

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
                    .message(Locale.ENGLISH, "Password check does not match.")
                    // Set Korean error message
                    .ko("비밀번호가 일치하지 않습니다.")
                    // Set Hindi error message
                    .message(Locale.forLanguageTag("hi"), "पासवर्ड मेल नहीं खाते.")
            .field("userType", "User Type")
            .field("paymentMethod", "Payment Method")
            .field("cardNumber", "Card Number")
                // ✨ Conditional validation: cardNumber if (USER + CREDIT_CARD) OR (SELLER)
                .when("userType", "USER").and("paymentMethod", "CREDIT_CARD")
                .when("userType", "SELLER")
            .build();
}

@GetMapping("/sign-up")
public String signUpPage(@ModelAttribute("command") UserCommand command, Model model) {
    // Convert validator to JSON and pass to client for validation
    model.addAttribute("rules", S2BindValidator.context("sign-up", this::profileValidator).getRulesJson());
    return "sign-up";
}

@PostMapping("/sign-up")
public String signUp(@ModelAttribute("command") UserCommand command, BindingResult result, Model model) {
    // Perform server-side validation using the same validator configuration
    S2BindValidator.context("sign-up", this::profileValidator).validate(command, result);

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
  <form id="joinForm" th:action="@{/member/join}" method="post"
        th:object="${member}" th:data-s2-rules="${validationRules}">
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

Once `s2.validator.js` is loaded, `initS2Validator()` runs automatically — **no JavaScript code required**:

1. Disables native browser validation (`noValidate`) on all forms with `data-s2-rules`; uses `MutationObserver` to cover dynamically added forms (SPAs, modals)
2. Intercepts form submit events and parses the JSON rules
3. On failure: calls `e.preventDefault()`, focuses the first invalid field, and shows a localized error tooltip via `form.reportValidity()`
4. Real-time reset: clears error state as soon as the user types (`input`) or changes selection (`change`)

**Practical Tips:**

- **AJAX / Fetch Validation**: When submitting via `fetch` or `axios`, call `S2Validator.validate()` manually:
  ```html
  <script type="module" th:inline="javascript">
    const contextPath = /*[[@{/}]]*/ '';
    const { S2Validator } = await import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);

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

> For the full client integration guide (import maps, per-field customization, etc.), see the [s2-validator README](./s2-validator/README.md#client-side-view-integration-thymeleaf--html-guide).

---

## 📦 Core Modules

### 1. **s2-core** — Foundation Library

[s2-core/README.md](./s2-core/README.md)

The foundational library providing high-performance core utility classes. Features include:

- **High-Performance Reflection**: Method Handle-based reflection with JIT optimization
- **Intelligent Caching**: Built-in optimized lightweight cache (concurrent-safe, zero-dependency) with optional Caffeine support for extreme high-concurrency environments
- **Java Version-Adaptive Thread Factory**: Virtual Thread support (Java 21+) with platform thread fallback
- **Optimized Data Access**: `getValue()` and `setValue()` with dot notation and bracket indexing support
- **Comprehensive Utilities**: String manipulation, date/time handling, type conversion, and more

---

### 2. **s2-validator** — Unified Validation Library ⭐

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

### 3. **s2-validator-plugin** — Gradle Build Plugin

[s2-validator-plugin/README.md](./s2-validator-plugin/README.md)

A Gradle build plugin for static source code analysis to validate S2Validator field names at compile-time. Features include:

- **Static Analysis**: JavaParser AST parsing for accurate code analysis
- **Compile-Time Validation**: Detects typos and non-existent fields before runtime
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
    implementation 'com.github.ben-manes.caffeine:caffeine:3.2.4'
    implementation 'org.springframework:spring-context:6.2.19'
    implementation 'jakarta.persistence:jakarta.persistence-api:3.2.0'
}
```

[//]: # 'S2_DEPS_INFO_END'
