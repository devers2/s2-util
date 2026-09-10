# s2-util: Unified Dynamic Validator 🚀

🌐 [English](README.md) | **한국어**

[![Java CI](https://github.com/devers2/s2-util/actions/workflows/ci.yml/badge.svg)](https://github.com/devers2/s2-util/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.devers2/s2-validator?color=brightgreen&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.devers2/s2-validator)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](./LICENSE)

> **"Write Once, Validate Anywhere."**
> **(Java & JavaScript) 한 번의 작성**으로 **서버와 클라이언트 모두를 검증**하는 가장 스마트한 방법.

---

## 📖 개요 (Overview)

**s2-util**은 서버(Java)와 클라이언트(JavaScript) 간의 검증 로직을 완벽하게 동기화하는 **통합 동적 검증 라이브러리**(`s2-validator`)를 포함한 유틸리티 제품군입니다. **프로덕션급(Production-ready)** 성능과 안정성을 목표로 설계되었으며, Method Handle 및 지능형 캐싱과 같은 첨단 기술을 활용하여 최적의 실행 속도와 정적 타입 안전성을 제공합니다.

---

## ✨ s2-util을 사용해야 하는 이유

`s2-util`은 표준 Java Bean Validation(Hibernate Validator)의 고질적인 한계와 실무 엔터프라이즈의 중복 코딩 고통을 해결하기 위해 개발되었습니다:

- **🌐 Write Once, Validate Anywhere** — Java로 정의한 규칙을 `getRulesJson()`으로 내보내고 `s2.validator.js`를 로드하면, **프론트엔드 JavaScript 코드 0줄로 브라우저 네이티브 툴팁/포커스 자동 검증**이 수행됩니다.
- **⚡ 어노테이션 지옥 없는 유연한 조건부 검증** — 복잡한 커스텀 어노테이션이나 `@GroupSequenceProvider` 없이, `.when(...).and(...)` 체이닝 단 2줄로 조건부 검증을 명쾌하게 구현합니다.
- **🏎️ 극한의 성능 최적화** — `MethodHandle` 캐싱으로 일반 리플렉션 오버헤드를 완전 제거(JIT 인라이닝 최적화), Caffeine(W-TinyLFU) 지능형 캐시, Java 21+ 가상 스레드(Virtual Thread) 완벽 대응.
- **🇰🇷 30+ 내장 규칙 & 스마트 i18n** — 이메일, URL, 연락처, 사업자번호 등 풍부한 기본 규칙과 한국어 받침에 따른 조사 자동 보정(`{0|은/는}`, `{0|이/가}`, `{0|을/를}`) 기본 내장.
- **🛡️ 빌드 시점 필드명 및 체이닝 정적 검증** — 동반 플러그인 `s2-validator-plugin`이 AST 정적 분석으로 DTO 필드 오타와 종단 메서드 누락(죽은 코드)을 빌드 단계(`compileJava`)에서 사전에 차단합니다.
- **🍃 매끄러운 Spring MVC 연동** — `S2BindValidator`를 통해 Spring 표준 `BindingResult`로 검증 오류를 자동 바인딩합니다.

### 🥊 한눈에 비교: 표준 Bean Validation vs s2-validator

| 실무 문제 및 유스케이스 | 표준 Bean Validation (JSR-380) | ⭐ s2-validator (s2-util) |
| :--- | :--- | :--- |
| **동적 조건부 검증**<br>*(A 값에 따라 B 필수)* | 커스텀 어노테이션 작성 또는 `@GroupSequenceProvider` 필요 (코드 급증) ❌ | 직관적인 2줄 표현:<br>`.when("type", "VIP").rule(REQUIRED)` ✅ |
| **크로스 필드 비교**<br>*(비밀번호 확인, 기간)* | 클래스 레벨 어노테이션 작성 필요; 루트 객체(Global Error)에 바인딩 ❌ | 해당 필드에 정확히 에러 바인딩:<br>`.rule(EQUALS_FIELD, "pw")` ✅ |
| **브라우저 / 프론트엔드 동기화** | 서버 전용. 프론트엔드에서 JS/TS(Zod 등)로 **동일 규칙 중복 코딩** 필수 ❌ | **프론트엔드 코드 0줄**: `th:data-s2-rules` 주입 시 네이티브 툴팁/자동 포커스 ✅ |
| **한국어 맞춤 조사 처리** 🇰🇷 | 기본 미지원. 받침 판별 커스텀 `MessageInterpolator` 직접 구현 ❌ | `{0\|은/는}`, `{0\|이/가}` 등 **조사 자동 보정 기본 내장** ✅ |
| **필드 오타 및 체이닝 안전성** | 필드 바인딩 오타나 검증 누락 시 런타임까지 방치 ❌ | `s2-validator-plugin`으로 **필드 오타 & 죽은 코드 컴파일 시점 AST 정적 차단** 🛡️ ✅ |

---

## 📦 모듈 구성

| 모듈 | 설명 |
| :--- | :--- |
| **[`s2-core`](./s2-core/README.ko.md)** | 고성능 Java 유틸리티 툴킷 (리플렉션, 날짜/시간, 문자열, 시스템) |
| **[`s2-validator`](./s2-validator/README.ko.md)** | ⭐ 서버·클라이언트 통합 유효성 검증 엔진 & Spring 바인딩 통합 |
| **[`s2-validator-plugin`](./s2-validator-plugin/README.ko.md)** | Gradle 정적 분석 플러그인 — DTO 필드 오타 및 체이닝 누락(죽은 코드)을 컴파일 타임에 감지 |
| **[`s2-jpa`](./s2-jpa/README.ko.md)** | JPA 쿼리 헬퍼 및 동적 엔티티 스펙 |

> [!TIP]
> **실무 애플리케이션 레벨의 유틸리티가 필요하신가요?**
> `s2-core`와 `s2-validator`를 기반으로 페이징(`S2PaginationInfo`), 파일 관리(`FileManager`), Spring 빈 정적 조회(`S2ContextUtil`), 암호화/이미지 유틸리티 등을 제공하는 동반 라이브러리 **[`s2-support`](https://github.com/devers2/s2-support)**를 확인해 보세요.

---

## 🚀 빠른 시작 가이드 (Quick Start)

### 1. 설치 (Installation)

`build.gradle`에 다음 의존성을 추가합니다 (Maven Central 사용).

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

**[선택 사항] S2Validator 정적 분석 플러그인**

오타나 필드명 불일치로 인한 런타임 오류를 방지합니다. 제네릭을 사용하는 경우(예: `S2Validator.<UserCommand>builder()`), 이 플러그인은 빌드 시점에 정적 분석을 수행하여 지정된 모든 필드명이 DTO 클래스에 실제로 존재하는지 확인합니다. 존재하지 않는 필드가 감지되면 즉시 빌드 에러가 발생합니다.

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

### 2. 사용법 (Usage)

S2Validator는 클라이언트(화면) 연동 필요 여부에 따라 두 가지 방식을 유연하게 지원합니다:
- **방식 A: 백엔드 단독 즉시 검증** — UI 연동 없이 서비스 계층, 배치, 또는 REST API에서 즉시 수행하는 간결한 검증.
- **방식 B: 풀스택 통합 검증 (서버 + 클라이언트)** — Spring `BindingResult` 연동과 프론트엔드 JavaScript 0줄 브라우저 툴팁 자동 동기화.

---

#### 방식 A. 백엔드 단독 즉시 검증 (클라이언트 연동 불필요 시)

서비스 레이어나 REST API에서 DTO, VO, Map을 즉시 검증합니다. `.field()`에 별도 규칙을 지정하지 않으면 기본값으로 필수값(`REQUIRED`)이 자동 적용됩니다.

```java
// 1) 즉시 예외 발생 모드 (Fail-Fast) — 실패 시 S2RuntimeException 발생
S2Validator.of(command)
    .field("name", "이름") // 규칙 생략 시 기본 REQUIRED 자동 적용
    .field("email", "이메일").rule(S2RuleType.EMAIL) // 규칙 지정 시 기본 REQUIRED 미적용(선택 입력), 필수 체크 필요 시 명시적 추가 필요
    .field("birthDate", "생년월일").rule(S2RuleType.REQUIRED).rule(S2RuleType.DATE)
    .validate();

// 2) 에러 수집 모드 — 에러 핸들러로 전체 오류 목록을 받아 처리 (논리값 반환)
List<S2ValidationError> errors = new ArrayList<>();
boolean isValid = S2Validator.of(command)
    .field("name", "이름")
    .field("email", "이메일").rule(S2RuleType.EMAIL)
    .validate(errors::add, Locale.KOREAN);

if (!isValid) {
    errors.forEach(err -> log.warn("{}: {}", err.fieldName(), err.defaultMessage()));
}
```

---

#### 방식 B. 풀스택 통합 검증 (서버 + 클라이언트 연동 필요 시)

서버에서 정의한 단 하나의 검증 규칙으로 백엔드 Spring `BindingResult`와 프론트엔드 브라우저 폼을 **JavaScript 코드 0줄**로 완벽하게 동기화합니다.

##### [Controller]

> **참고:** 이 예제는 Spring Framework 통합을 가정합니다. Spring이 없는 환경에서도 `S2Validator` 및 `S2ValidatorFactory`를 직접 사용하여 검증할 수 있으나, `BindingResult` 연동은 불가능합니다.

```java
private S2Validator<UserCommand> profileValidator() {
    return S2Validator.<UserCommand>builder()
            // Rule이 없으면 기본적으로 REQUIRED 적용
            // "이름"은 에러 메시지에 사용될 라벨
            .field("name", "이름")
            .field("password", "비밀번호")
            // 직접 Rule 지정 시 필수 체크가 필요하면 REQUIRED 별도 지정
            .field("passwordCheck", "비밀번호 확인")
                .rule(S2RuleType.REQUIRED)
                // password 필드와 동일한 값인지 검증
                .rule(S2RuleType.EQUALS_FIELD, "password")
                    // 영문 에러 메시지 설정
                    .en("Password check does not match.")
                    .message(Locale.ENGLISH, "Password check does not match.")
                    // 한글 에러 메시지 설정
                    .ko("비밀번호가 일치하지 않습니다.")
                    // 힌디어 에러 메시지 설정
                    .message(Locale.forLanguageTag("hi"), "पासवर्ड मेल नहीं खाते.")
            .field("userType", "회원 유형")
            .field("paymentMethod", "결제 방법")
            .field("cardNumber", "카드 번호")
                // ✨ 조건부 검증: 일반회원의 카드결제 건 또는 판매자일 경우 카드번호 검증
                .when("userType", "USER").and("paymentMethod", "CREDIT_CARD")
                .when("userType", "SELLER")
            .build();
}

@GetMapping("/sign-up")
public String signUpPage(@ModelAttribute("command") UserCommand command, Model model) {
    // 클라이언트 유효성 검증을 위해 JSON으로 변환하여 전달
    model.addAttribute("rules", S2BindValidator.context("sign-up", this::profileValidator).getRulesJson());
    return "sign-up";
}

@PostMapping("/sign-up")
public String signUp(@ModelAttribute("command") UserCommand command, BindingResult result, Model model) {
    // 설정된 검증기로 서버 측에서도 동일하게 검증 수행
    S2BindValidator.context("sign-up", this::profileValidator).validate(command, result);

    if (result.hasErrors()) {
        return signUpPage(command, model);
    }
    userService.createUser(command);
    return "redirect:/sign-in";
}
```

#### [HTML / Client]

> **`s2.validator.js`를 별도로 복사하지 않아도 되는 이유?**
> Servlet 3.0+ 스펙에 따라, JAR 내부의 `META-INF/resources/` 경로에 있는 파일은 정적 웹 리소스로 자동 제공됩니다. 따라서 `s2.validator.js`는 별도 설정 없이 `/s2-util/js/s2.validator.js`로 즉시 접근할 수 있습니다.

**권장 임포트 방식:**

- **Option A (권장 — Thymeleaf `th:src`)**:
  ```html
  <!-- 서버에서 생성한 JSON 규칙을 폼에 바인딩 -->
  <form id="joinForm" th:action="@{/member/join}" method="post"
        th:object="${member}" th:data-s2-rules="${validationRules}">
    ...
    <button type="submit">회원가입</button>
  </form>

  <!-- Thymeleaf @{...}를 통해 컨텍스트 경로 안전하게 임포트 -->
  <script type="module" th:src="@{/s2-util/js/s2.validator.js}"></script>
  ```

- **Option B (인라인 스크립트 동적 임포트)**:
  ```html
  <script type="module">
    // 컨텍스트 경로에 안전한 동적 임포트 (e.g. /app 배포 환경에서도 동작)
    // 단순한 '/s2-util/js/s2.validator.js'는 서버 루트(/) 배포 시에만 동작합니다.
    const contextPath = /*[[@{/}]]*/ '';
    import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);
  </script>
  ```

**Zero-Code 클라이언트 자동 검증:**

`s2.validator.js`가 로드되면 `initS2Validator()`가 자동으로 실행됩니다 — **별도 JavaScript 코드 불필요**:

1. `data-s2-rules` 속성을 가진 모든 폼의 네이티브 검증을 비활성화(`noValidate`); `MutationObserver`로 동적으로 추가된 폼(SPA, 모달 등)도 자동 감지
2. 폼 전송 이벤트를 가로채고 JSON 규칙을 파싱
3. 검증 실패 시: `e.preventDefault()` 호출, 첫 번째 오류 필드로 포커스, `form.reportValidity()`로 로컬라이징된 오류 툴팁 표시
4. 실시간 오류 초기화: 사용자가 입력(`input`) 또는 선택 변경(`change`) 시 즉시 오류 상태 해제

**실용 팁:**

- **AJAX / Fetch 검증**: `fetch`나 `axios`로 제출하는 경우, `S2Validator.validate()`를 직접 호출하세요:
  ```html
  <script type="module" th:inline="javascript">
    const contextPath = /*[[@{/}]]*/ '';
    const { S2Validator } = await import(`${contextPath.endsWith('/') ? contextPath : contextPath + '/'}s2-util/js/s2.validator.js`);

    document.getElementById('ajaxBtn').addEventListener('click', async () => {
      const errors = S2Validator.validate('#joinForm');
      if (Object.keys(errors).length > 0) return; // 오류 있으면 중단

      const formData = new FormData(document.getElementById('joinForm'));
      await fetch('/api/member/join', { method: 'POST', body: formData });
    });
  </script>
  ```

- **히든 입력 오류 표시 (`{fieldName}_error`)**: 히든 입력이나 커스텀 UI 위젯은 네이티브 툴팁을 표시할 수 없습니다. `{fieldName}_error`라는 이름의 프록시 요소를 추가하면 S2Validator가 자동으로 오류 메시지를 채웁니다:
  ```html
  <input type="hidden" name="profileImage" />
  <span name="profileImage_error" style="color: red; font-size: 12px;"></span>
  ```

> 전체 클라이언트 통합 가이드(import maps, 필드별 커스터마이징 등)는 [`s2-validator` README](./s2-validator/README.ko.md#client-side-view-integration-thymeleaf--html-guide)를 참고하세요.


---

## 📦 핵심 모듈 (Core Modules)

### 1. **`s2-core`** — 기반 라이브러리

[s2-core/README.ko.md](./s2-core/README.ko.md)

고성능 핵심 유틸리티 클래스를 제공하는 기반 라이브러리입니다. 주요 기능:

- **고성능 리플렉션**: Method Handle 기반 리플렉션 (JIT 최적화)
- **지능형 캐싱**: 외부 의존성 없는 자체 고성능 동시성 경량 캐시 제공 (대규모 트래픽 환경을 위한 선택적 Caffeine 지원)
- **자바 버전 적응형 스레드 팩토리**: 가상 스레드 지원 (Java 21+) 및 플랫폼 스레드 폴백
- **최적화된 데이터 접근**: 점 표기법 및 대괄호 인덱싱 지원
- **종합 유틸리티**: 문자열 조작, 날짜/시간 처리, 타입 변환 등

---

### 2. **`s2-validator`** — 통합 검증 라이브러리 ⭐

[s2-validator/README.ko.md](./s2-validator/README.ko.md)

단일 설정으로 서버와 클라이언트 모두를 지원하는 통합 검증 라이브러리입니다. 주요 기능:

- **유연한 API**: 자연스러운 체이닝 검증 규칙
- **30가지 이상 내장 규칙**: REQUIRED, LENGTH, REGEX, EMAIL, MPHONE_NO, DATE 등
- **한국 전용 규칙**: MPHONE_NO, TEL_NO, ZIP, BIZRNO, NWINO, JUMIN, PASSWORD_ANSWR
- **고급 중첩 객체 지원**: 점 표기법 및 대괄호 인덱싱
- **포괄적 i18n**: 로컬라이제이션 및 S2ResourceBundle 통합
- **커스텀 및 조건부 검증**: CustomRule 인터페이스와 when()/and() 조건 로직
- **Spring 통합** (선택사항): BindingResult를 통한 표준 Spring 에러 처리

---

### 3. **`s2-validator-plugin`** — Gradle 빌드 플러그인

[s2-validator-plugin/README.ko.md](./s2-validator-plugin/README.ko.md)

`s2-validator` 필드명과 체이닝 완결성을 컴파일 타임에 정적 분석으로 검증하는 Gradle 빌드 플러그인입니다. 주요 기능:

- **정적 분석**: JavaParser AST 파싱으로 정확한 코드 분석
- **컴파일 타임 필드 검증**: 런타임 이전에 오타와 존재하지 않는 필드 감지
- **체이닝 완결성 검사 (죽은 코드 방지)**: 종단 메서드(`.validate()` / `.build()`) 누락으로 인한 미실행 코드 자동 감지 및 빌드 차단
- **다중 프로젝트 지원**: 모든 서브프로젝트 및 모듈 스캔
- **설정 불필요**: 표준 Gradle 빌드 태스크 자동 통합
- **스마트 검증**: 제네릭 와일드카드 및 불완전한 타입 정보는 검증 스킵
- **상세 에러 보고**: 색상 코딩된 메시지와 파일 경로/줄 번호

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

[//]: # 'S2_DEPS_INFO_START'

---

**특정 기능(예: S2BindValidator)을 사용하려면 런타임에 다음 의존성을 엔드유저 프로젝트에 명시적으로 추가해야 합니다.** 이 의존성이 누락되면 런타임에 `java.lang.NoClassDefFoundError`가 발생합니다.

**[Gradle 사용자]**

```groovy
dependencies {
    // 선택적 기능을 위한 필수 런타임 의존성
    implementation 'com.github.ben-manes.caffeine:caffeine:3.2.4'
    implementation 'org.springframework:spring-context:6.2.19'
    implementation 'jakarta.persistence:jakarta.persistence-api:3.2.0'
}
```

[//]: # 'S2_DEPS_INFO_END'
