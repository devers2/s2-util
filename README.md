# S2Util: Unified Dynamic Validator 🚀

[English](README.md) | [한국어](README.ko.md)

[![Java CI](https://github.com/devers2/s2-util/actions/workflows/ci.yml/badge.svg)](https://github.com/devers2/s2-util/actions/workflows/ci.yml)

> **Write Once, Validate Anywhere.**
> The smartest way to validate both **Server (Java)** and **Client (JavaScript)** with a **single configuration**.

---

## 📖 Overview

**S2Util** is a high-performance Java **utility library** featuring a **Unified Dynamic Validator** that seamlessly synchronizes validation logic between Server (Java) and Client (JavaScript). Designed for **production-ready** environments, it leverages advanced technologies like Method Handles and intelligent caching to ensure maximum efficiency and type safety.

---

## 🚀 Quick Start

### 1. Installation

Add the following dependencies to your `build.gradle` (using Maven Central).

```groovy
dependencies {
    implementation 'io.github.devers2:s2-util:1.1.7'
}
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

```html
<!-- Inject the validation rules JSON string passed from the controller -->
<form id="myForm" th:data-s2-rules="${rules}">...</form>

<script type="module">
  // s2.validator.js is served automatically from the JAR's META-INF/resources
  // (assuming the app hasn't disabled its framework's default static-resource-from-JAR
  // serving, e.g. Spring Boot's default static resource handling).
  //
  // The path below is context-path-safe (works no matter what path the app is deployed under),
  // via Thymeleaf's @{...} link-URL expression. A plain absolute import like
  // `import '/s2-util/js/s2.validator.js'` only works when the app is deployed at the server
  // ROOT context path ("/") - it breaks under any other context path (e.g. "/app").
  const contextPath = /*[[@{/}]]*/ '';
  import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);
  // Just importing the script automatically performs validation using the browser's native UI during submit.
</script>
```

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

### 2. **s2-validator** — Unified Validation Library

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
