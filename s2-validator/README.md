# S2Util Library - Validator Module (s2-validator)

## Overview (개요)

### [English]

The **s2-validator** module is a unified cross-platform validation framework that enables single-configuration validation rules for both server and client environments. It provides a fluent, chainable API supporting comprehensive validation rules, conditional validation, complex custom rules, and full internationalization support (i18n). The module excels at validating nested objects with dot notation (e.g., `user.address.street`) and collection items (e.g., `items[0]`), making it ideal for complex DTO/VO hierarchies.

### [한국어]

**s2-validator** 모듈은 서버와 클라이언트를 아우르는 통합 검증 프레임워크로, 단일 설정으로 양쪽 환경에 동일한 검증 규칙을 적용할 수 있습니다. 유연한 체이닝 API를 제공하며 30가지 이상의 포괄적인 검증 규칙, 조건부 검증, 복잡한 커스텀 규칙, 완전한 국제화 지원(i18n)을 포함합니다. 특히 점 표기법(`user.address.street`)과 컬렉션 항목(`items[0]`)으로 중첩 객체를 검증하는 데 탁월하여, 복잡한 DTO/VO 계층 구조에 이상적입니다.

---

## ✨ Key Features (주요 기능)

### [English]

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
   - Message localization with `ko()`, `en()` methods for common locales
   - Custom locale support via `message(Locale, String)` method
   - `S2ResourceBundle` integration for centralized message management
   - Fallback to default messages when specific locale not provided
   - Message parameter substitution: `{0}`, `{1}` for field names and rule values
   - Korean particle support: `{0|은/는}`, `{0|이/가}`, `{0|을/를}`, `{0|과/와}` for grammatically correct messages

7. **Custom & Conditional Validation**
   - `CustomRule` interface for user-defined validation logic
   - Reusable validation rules as standalone objects
   - `when()` with `and()` for complex AND conditions
   - Multiple `when()` chains for OR logic

8. **Error Handling & Reporting**
   - Comprehensive error information: field name, error code, error message, default message
   - Consumer-based error handler for flexible error processing
   - Fail-fast mode vs. collect-all-errors mode
   - Circular reference detection for safe nested object validation

9. **Spring Framework Integration** (Optional)
   - `S2BindValidator` for Spring Data Binding with `BindingResult`
   - Supplier pattern for lazy evaluation and convenient rule management
   - Unified validation rules: Same ruleset for both server and client
   - Standard Spring error handling with automatic error field binding

### [한국어]

1. **유연한 검증 체인 API**
   - 자연스러운 읽기 쉬운 검증 규칙: `.field("fieldName").rule(S2RuleType.REQUIRED).ko("필수입력").en("Required")`
   - 순차적 규칙 적용을 위한 체이닝 메서드
   - 여러 규칙 체이닝: 여러 필드 또는 같은 필드의 여러 규칙을 동시에 검증 (예: REQUIRED와 LENGTH)
   - `when()` 조건부 검증: 조건 충족 시에만 규칙 적용

2. **성능 최적화**
   - **MethodHandle을 활용한 고성능 리플렉션**: MethodHandle 캐싱을 통해 리플렉션 병목 현상을 제거하고, JIT 컴파일러 최적화를 통해 네이티브에 가까운 성능을 구현
   - **Caffeine을 활용한 지능형 캐싱**: W-TinyLFU 알고리즘으로 최적의 적중률 달성; 트래픽 급증 시에도 중요 데이터 축출 방지; 자동 캐시 최적화
   - **자바 버전별 적응형 스레드 팩토리**: Java 21 이상 환경에서 가상 스레드 지원; 이전 버전에서는 최적화된 플랫폼 스레드 풀 사용
   - **패턴 캐싱**: 정규식 패턴을 캐싱하여 반복 검증 시 컴파일 시간 감소

3. **30가지 이상의 내장 규칙 타입(S2RuleType)**
   - **기본**: REQUIRED, ASSERT_TRUE, ASSERT_FALSE, EQUALS_FIELD
   - **문자열**: LENGTH, MIN_LENGTH, MAX_LENGTH, REGEX
   - **숫자**: MIN_VALUE, MAX_VALUE, NUMBER, MIN_BYTE, MAX_BYTE
   - **형식**: EMAIL, URL, INTERNATIONAL_TEL_NO
   - **한국 전용 형식** 🇰🇷: MPHONE_NO, TEL_NO, ZIP, BIZRNO, NWINO, JUMIN, PASSWORD_ANSWR
   - **날짜**: DATE, DATE_AFTER, DATE_BEFORE
   - **텍스트**: TEXT_INTACT, TEXT_COMBINE
   - **커스텀**: 애플리케이션 특화 검증 로직을 위한 CustomRule

4. **크로스 플랫폼 검증**
   - 서버측 검증(Java)을 위한 단일 설정
   - 클라이언트측 검증 규칙 생성(JavaScript, TypeScript 등)
   - 플랫폼 전반에 걸친 일관된 검증 동작
   - 통일된 에러 메시징을 위한 메시지 템플릿 지원

5. **고급 중첩 객체 지원**
   - 점 표기법: `user.address.street`, `employee.department.manager.name`
   - 대괄호 표기법: `items[0].name`, `matrix[1][2]`, `users[0].roles[1]`
   - 혼합 표기법: `company.departments[0].employees[1].salary`
   - Optional, List, Array 자동 처리
   - 즉시 검증과 지연 검증 모두 지원

6. **포괄적인 국제화(i18n) 지원**
   - `ko()`, `en()` 메서드를 통한 일반적인 로케일 메시지 로컬라이제이션
   - `message(Locale, String)` 메서드를 통한 커스텀 로케일 지원
   - `S2ResourceBundle` 통합으로 중앙 집중식 메시지 관리
   - 특정 로케일이 없을 때 기본 메시지로 폴백
   - 메시지 매개변수 치환: `{0}`, `{1}`로 필드명과 규칙값 지정
   - 한국어 조사 지원: `{0|은/는}`, `{0|이/가}`, `{0|을/를}`, `{0|과/와}`로 문법 올바른 메시지 생성

7. **커스텀 및 조건부 검증**
   - 사용자 정의 검증 로직을 위한 `CustomRule` 인터페이스
   - 독립 객체로의 재사용 가능한 검증 규칙
   - `when()`과 `and()`를 연결하여 복잡한 AND 조건 구현
   - 다중 `when()` 체인으로 OR 조건 구현

8. **에러 처리 및 보고**
   - 포괄적인 에러 정보: 필드명, 에러 코드, 에러 메시지, 기본 메시지
   - 유연한 에러 처리를 위한 Consumer 기반 에러 핸들러
   - Fail-fast 모드 vs. 모든 에러 수집 모드
   - 중첩 객체 검증 시 안전한 순환 참조 검출

9. **Spring Framework 통합** (선택사항)

- `S2BindValidator`로 Spring Data Binding과 `BindingResult` 연동
- Supplier 패턴을 통한 지연 평가(Lazy Evaluation)와 편리한 규칙 관리
- 통합 검증 규칙: 서버와 클라이언트 동일 룰셋 적용
- Spring 표준 에러 처리로 자동 에러 필드 바인딩

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

s2-validator Version: 1.1.5 (2026-01-29)

[//]: # 'S2_DEPS_INFO_START'

---

**To use certain functionalities (e.g., S2BindValidator), the end-user project must explicitly add the following dependencies to be available at runtime.** Failure to include these dependencies will result in a `java.lang.NoClassDefFoundError` at runtime.

**[For Gradle Users]**

```groovy
dependencies {
    // Essential runtime dependencies for optional functionalities
    implementation 'org.springframework:spring-context:6.2.15'
}
```

[//]: # 'S2_DEPS_INFO_END'
