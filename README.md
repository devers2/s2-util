# S2Util Library

## Overview (개요)

### [English]

**S2Util** is a comprehensive Java utility framework consisting of three core modules designed to provide high-performance, production-ready utilities for Java development. The framework emphasizes performance optimization through advanced technologies like Method Handles, intelligent caching, and adaptive threading strategies.

### [한국어]

**S2Util**은 자바 개발을 위한 고성능 프로덕션급 유틸리티를 제공하는 3개의 핵심 모듈로 구성된 종합 자바 유틸리티 프레임워크입니다. Method Handle, 지능형 캐싱, 적응형 스레드 전략 등 첨단 기술을 활용한 성능 최적화를 강조합니다.

---

## 📦 Core Modules (핵심 모듈)

### 1. **s2-core** - Foundation Library

[s2-core/README.md](./s2-core/README.md)

#### [English]

The foundational library providing high-performance core utility classes. Features include:

- **High-Performance Reflection**: Method Handle-based reflection with JIT optimization
- **Intelligent Caching**: Caffeine cache with W-TinyLFU algorithm for optimal hit rates
- **Java Version-Adaptive Thread Factory**: Virtual Thread support (Java 21+) with platform thread fallback
- **Optimized Data Access**: `getValue()` and `setValue()` with dot notation and bracket indexing support
- **Comprehensive Utilities**: String manipulation, date/time handling, type conversion, and more

#### [한국어]

고성능 핵심 유틸리티 클래스를 제공하는 기반 라이브러리입니다. 주요 기능:

- **고성능 리플렉션**: Method Handle 기반 리플렉션 (JIT 최적화)
- **지능형 캐싱**: W-TinyLFU 알고리즘의 Caffeine 캐시
- **자바 버전 적응형 스레드 팩토리**: 가상 스레드 지원 (Java 21+) 및 플랫폼 스레드 폴백
- **최적화된 데이터 접근**: 점 표기법 및 대괄호 인덱싱 지원
- **종합 유틸리티**: 문자열 조작, 날짜/시간 처리, 타입 변환 등

---

### 2. **s2-validator** - Unified Validation Framework

[s2-validator/README.md](./s2-validator/README.md)

#### [English]

A unified cross-platform validation framework supporting both server and client with single configuration. Features include:

- **Fluent API**: Natural, chainable validation rules with sequential method application
- **30+ Built-in Rule Types**: REQUIRED, LENGTH, REGEX, EMAIL, MPHONE_NO, DATE, and more
- **Korea-specific Rules**: MPHONE_NO, TEL_NO, ZIP, BIZRNO, NWINO, JUMIN, PASSWORD_ANSWR
- **Advanced Nested Object Support**: Dot notation (`user.address.street`) and bracket indexing (`items[0]`)
- **Comprehensive i18n**: Message localization with `ko()`, `en()`, custom locales, and `S2ResourceBundle`
- **Custom & Conditional Validation**: `CustomRule` interface and `when()`/`and()` conditional logic
- **Spring Integration** (Optional): `S2BindValidator` with `BindingResult` for standard Spring error handling

#### [한국어]

단일 설정으로 서버와 클라이언트 모두를 지원하는 통합 검증 프레임워크입니다. 주요 기능:

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

- **Java 17 or higher** is required to use S2Util Library.
- All modules and plugins require Java 17+.

### [한국어]

- S2Util 라이브러리를 사용하려면 **Java 17 이상**이 필요합니다.
- 모든 모듈과 플러그인은 Java 17 이상을 요구합니다.

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
