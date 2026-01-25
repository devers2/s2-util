# S2Util: Unified Dynamic Validator 🚀

[![Java CI](https://github.com/devers2/s2-util/actions/workflows/ci.yml/badge.svg)](https://github.com/devers2/s2-util/actions/workflows/ci.yml)

> **Write Once, Validate Anywhere.**<br>
> **(Java & JavaScript) 한 번의 작성**으로 **서버와 클라이언트 모두를 검증**하는 가장 스마트한 방법.

---

## Overview (개요)

### [English]

**S2Util** is a high-performance Java **utility library** featuring a **Unified Dynamic Validator** that seamlessly synchronizes validation logic between Server (Java) and Client (JavaScript). Designed for **production-ready** environments, it leverages advanced technologies like Method Handles and intelligent caching to ensure maximum efficiency and type safety.

### [한국어]

**S2Util**은 서버(Java)와 클라이언트(JavaScript) 간의 검증 로직을 완벽하게 동기화하는 **통합 동적 검증 라이브러리**입니다. **프로덕션급(Production-ready)** 성능과 안정성을 목표로 설계되었으며, Method Handle 및 지능형 캐싱과 같은 첨단 기술을 활용하여 최적의 실행 속도와 정적 타입 안전성을 제공합니다.

---

## 🚀 Quick Start (빠른 시작 가이드)

### 1. Installation (설치)

Add the following dependencies to your `build.gradle` (using Maven Central).

```groovy
dependencies {
    implementation 'io.github.devers2:s2-util:1.1.0'
}

// [Optional] S2Validator Static Analysis Plugin
// ✨ Prevent runtime errors caused by typos or field name mismatches.
// When using Generics (e.g., S2Validator.<UserCommand>builder()), this plugin performs
// static analysis during the build to verify that all referenced field names
// actually exist in the specified DTO class.
// It triggers a build error if a non-existent field is detected.
// (제네릭을 사용한 경우(예: S2Validator.<UserCommand>builder()), 빌드 시점에 정적 분석을 수행합니다.
// 명시된 DTO에 실제 필드가 있는지 확인하여, 오타 등으로 존재하지 않는 필드를 참조하면
// 빌드 에러를 발생시켜 런타임 오류를 완벽히 예방합니다.)
plugins {
    id 'io.github.devers2.validator' version '1.1.0'
}
```

### 2. Usage (사용법)

Unified validation for server and client.

#### [Controller]

> **Note:** This example assumes Spring Framework integration. If Spring is not available, you can use `S2Validator` and `S2ValidatorFactory` directly, but `BindingResult` integration will not be available.<br>
> (참고: 이 예제는 Spring Framework 통합을 가정합니다. Spring이 없는 환경에서도 `S2Validator` 및 `S2ValidatorFactory`를 직접 사용하여 검증할 수 있으나, `BindingResult` 연동은 불가능합니다.)

```java
private S2Validator<UserCommand> profileValidator() {
    return S2Validator.<UserCommand>builder()
            // If no rule is specified, S2RuleType.REQUIRED is applied by default
            // (Rule이 없으면 기본적으로 REQUIRED 적용)
            // "Name" is the label used in error messages (에러 메시지에 사용될 라벨)
            .field("name", "Name")
            .field("password", "Password")
            // When specifying explicit rules, REQUIRED must be added manually if needed
            // (직접 Rule 지정 시 필수 체크가 필요하면 REQUIRED 별도 지정)
            .field("passwordCheck", "Confirm Password")
                .rule(S2RuleType.REQUIRED)
                // Verifies value equals "password" field (password 필드와 동일한 값인지 검증)
                .rule(S2RuleType.EQUALS_FIELD, "password")
                    // Set English error message (영문 에러 메시지 설정)
                    .en("Password check does not match.")
                    .message(Locale.ENGLISH, "Password check does not match.")
                    // Set Korean error message (한글 에러 메시지 설정)
                    .ko("비밀번호가 일치하지 않습니다.")
                    // Set Hindi error message (힌디어 에러 메시지 설정)
                    .message(Locale.forLanguageTag("hi"), "पासवर्ड मेल नहीं खाते.")
            .field("userType", "User Type")
            .field("paymentMethod", "Payment Method")
            .field("cardNumber", "Card Number")
                // ✨ Conditional validation: cardNumber if (USER + CREDIT_CARD) OR (SELLER)
                // (일반회원의 카드결제 건 또는 판매자일 경우 카드번호 검증)
                .when("userType", "USER").and("paymentMethod", "CREDIT_CARD")
                .when("userType", "SELLER")
            .build();
}

@GetMapping("/sign-up")
public String signUpPage(@ModelAttribute("command") UserCommand command, Model model) {
    // Convert validator to JSON and pass to client for validation
    // (클라이언트 유효성 검증을 위해 JSON으로 변환하여 전달)
    model.addAttribute("rules", S2BindValidator.context("sign-up", this::profileValidator).getRulesJson());
    return "sign-up";
}

@PostMapping("/sign-up")
public String signUp(@ModelAttribute("command") UserCommand command, BindingResult result, Model model) {
    // Perform server-side validation using the same validator configuration
    // (설정된 검증기로 서버 측에서도 동일하게 검증 수행)
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
<!-- 컨트롤러에서 전달받은 검증 규칙(JSON 문자열)을 폼의 data 속성에 주입 -->
<form id="myForm" th:data-s2-rules="${rules}">...</form>

<script type="module">
  // s2.validator.js is served automatically from the JAR's META-INF/resources.
  // (s2.validator.js는 라이브러리 내부(META-INF/resources)에 포함되어 있어 별도 설정 없이 바로 로드됩니다.)
  import '/s2-util/js/s2.validator.js';
  // Just importing the script automatically performs validation using the browser's native UI during submit, matching the server-side rules.
  // (임포트만 하면 폼 전송 시 브라우저 네이티브 UI를 통해 서버와 동일한 검증이 자동으로 수행됩니다.)
</script>
```

---

## 📦 Core Modules (핵심 모듈)

### 1. **s2-core** - Foundation Library

[s2-core/README.md](./s2-core/README.md)

#### [English]

The foundational library providing high-performance core utility classes. Features include:

- **High-Performance Reflection**: Method Handle-based reflection with JIT optimization
- **Intelligent Caching**: Built-in optimized lightweight cache (concurrent-safe, zero-dependency) with optional Caffeine support for extreme high-concurrency environments
- **Java Version-Adaptive Thread Factory**: Virtual Thread support (Java 21+) with platform thread fallback
- **Optimized Data Access**: `getValue()` and `setValue()` with dot notation and bracket indexing support
- **Comprehensive Utilities**: String manipulation, date/time handling, type conversion, and more

#### [한국어]

고성능 핵심 유틸리티 클래스를 제공하는 기반 라이브러리입니다. 주요 기능:

- **고성능 리플렉션**: Method Handle 기반 리플렉션 (JIT 최적화)
- **지능형 캐싱**: 외부 의존성 없는 자체 고성능 동시성 경량 캐시 제공 (대규모 트래픽 환경을 위한 선택적 Caffeine 지원)
- **자바 버전 적응형 스레드 팩토리**: 가상 스레드 지원 (Java 21+) 및 플랫폼 스레드 폴백
- **최적화된 데이터 접근**: 점 표기법 및 대괄호 인덱싱 지원
- **종합 유틸리티**: 문자열 조작, 날짜/시간 처리, 타입 변환 등

---

### 2. **s2-validator** - Unified Validation Library

[s2-validator/README.md](./s2-validator/README.md)

#### [English]

A unified cross-platform validation Library supporting both server and client with single configuration. Features include:

- **Fluent API**: Natural, chainable validation rules with sequential method application
- **30+ Built-in Rule Types**: REQUIRED, LENGTH, REGEX, EMAIL, MPHONE_NO, DATE, and more
- **Korea-specific Rules**: MPHONE_NO, TEL_NO, ZIP, BIZRNO, NWINO, JUMIN, PASSWORD_ANSWR
- **Advanced Nested Object Support**: Dot notation (`user.address.street`) and bracket indexing (`items[0]`)
- **Comprehensive i18n**: Message localization with `ko()`, `en()`, custom locales, and `S2ResourceBundle`
- **Custom & Conditional Validation**: `CustomRule` interface and `when()`/`and()` conditional logic
- **Spring Integration** (Optional): `S2BindValidator` with `BindingResult` for standard Spring error handling

#### [한국어]

단일 설정으로 서버와 클라이언트 모두를 지원하는 통합 검증 라이브러리입니다. 주요 기능:

- **유연한 API**: 자연스러운 체이닝 검증 규칙
- **30가지 이상 내장 규칙**: REQUIRED, LENGTH, REGEX, EMAIL, MPHONE_NO, DATE 등
- **한국 전용 규칙**: MPHONE_NO, TEL_NO, ZIP, BIZRNO, NWINO, JUMIN, PASSWORD_ANSWR
- **고급 중첩 객체 지원**: 점 표기법 및 대괄호 인덱싱
- **포괄적 i18n**: 로컬라이제이션 및 S2ResourceBundle 통합
- **커스텀 및 조건부 검증**: CustomRule 인터페이스와 when()/and() 조건 로직
- **Spring 통합** (선택사항): BindingResult를 통한 표준 Spring 에러 처리

---

### 3. **s2-validator-plugin** - Gradle Build Plugin

[s2-validator-plugin/README.md](./s2-validator-plugin/README.md)

#### [English]

A Gradle build plugin for static source code analysis to validate S2Validator field names at compile-time. Features include:

- **Static Analysis**: JavaParser AST parsing for accurate code analysis
- **Compile-Time Validation**: Detects typos and non-existent fields before runtime
- **Multi-Project Support**: Scans all subprojects and modules
- **Zero Configuration**: Automatically integrates with standard Gradle build tasks
- **Smart Validation**: Skips validation for generic wildcards and incomplete type information
- **Detailed Error Reporting**: Color-coded messages with file paths and line numbers

#### [한국어]

S2Validator 필드명을 컴파일 타임에 정적 분석으로 검증하는 Gradle 빌드 플러그인입니다. 주요 기능:

- **정적 분석**: JavaParser AST 파싱으로 정확한 코드 분석
- **컴파일 타임 검증**: 런타임 이전에 오타와 존재하지 않는 필드 감지
- **다중 프로젝트 지원**: 모든 서브프로젝트 및 모듈 스캔
- **설정 불필요**: 표준 Gradle 빌드 태스크 자동 통합
- **스마트 검증**: 제네릭 와일드카드 및 불완전한 타입 정보는 검증 스킵
- **상세 에러 보고**: 색상 코딩된 메시지와 파일 경로/줄 번호

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

[//]: # 'S2_DEPS_INFO_START'

---

**To use certain functionalities (e.g., S2BindValidator), the end-user project must explicitly add the following dependencies to be available at runtime.** Failure to include these dependencies will result in a `java.lang.NoClassDefFoundError` at runtime.

**[For Gradle Users]**

```groovy
dependencies {
    // Essential runtime dependencies for optional functionalities
    implementation 'com.github.ben-manes.caffeine:caffeine:3.2.3'
    implementation 'org.springframework:spring-context:6.1.1'
    implementation 'jakarta.persistence:jakarta.persistence-api:3.1.0'
}
```

[//]: # 'S2_DEPS_INFO_END'
