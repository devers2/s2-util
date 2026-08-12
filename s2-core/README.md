# S2Util Library - Core Module (s2-core)

## Overview (개요)

### [English]

The **s2-core** module is the foundational library of the S2Util project, providing high-performance core utility classes optimized for Java development. It leverages advanced technologies such as Method Handles for efficient reflection, **built-in optimized lightweight cache for zero-dependency caching (with optional Caffeine support)**, and adaptive thread factories for different Java versions. Core features include optimized data access/manipulation methods (`getValue()`, `setValue()`), string utilities, date/time handling, and type conversion.

### [한국어]

**s2-core** 모듈은 S2Util 프로젝트의 기반이 되는 라이브러리로, 자바 개발에 필요한 고성능 핵심 유틸리티 클래스와 필수 공통 로직을 제공합니다. Method Handle을 활용한 효율적인 리플렉션, **외부 라이브러리 없이 동작하는 자체 고성능 경량 캐시**, 자바 버전별 적응형 스레드 팩토리 등 첨단 기술을 활용합니다. 최적화된 데이터 접근/조작 메서드(`getValue()`, `setValue()`), 문자열 유틸리티, 날짜/시간 처리, 타입 변환 등의 핵심 기능을 제공합니다.

---

## ✨ Key Features (주요 기능)

### [English]

1. **High-Performance Reflection with Method Handles**
   - Eliminates performance bottlenecks of standard Java reflection (`java.lang.reflect`)
   - JIT compiler optimizes MethodHandle calls to near-native performance
   - Strategic caching of MethodHandles in ConcurrentHashMap for repeated access

2. **Intelligent Caching (Dual Mode)**
   - **Default**: Built-in `S2OptimisticCache` optimized for speed and simplicity (No external dependencies)
     - Lock-free reads & Optimistic/Atomic writes
     - Sequence-based LRU eviction strategy
   - **Optional**: Seamless integration with **Caffeine Cache** for enterprise-grade workloads
     - W-TinyLFU algorithm for maximizing hit rates in high-traffic scenarios
     - Automatically activated when Caffeine is present in the classpath
     - **Verification**: You can check if Caffeine is enabled by calling `S2Cache.isCaffeineEnabled()`.

3. **Java Version-Adaptive Thread Factory**
   - Virtual Thread support for Java 21+ environments
   - Fallback to optimized platform thread pools for earlier versions
   - Unified API for cross-version compatibility

4. **Optimized Data Access & Manipulation**
   - `getValue()`: Extracts values from nested objects with dot notation (`user.address.street`)
   - `setValue()`: Sets values in nested structures with array/collection support
   - Support for Maps, Lists, Arrays, Records, DTO/VO, and JPA Hibernate proxies
   - Bracket notation support (`users[0].name`, `matrix[1][2]`)

5. **Comprehensive Utility Modules**
   - **S2Cache**: Advanced caching with pattern-based eviction policies
   - **S2ThreadUtil**: Thread and executor management with version-aware optimization
   - **S2StringUtil**: String manipulation with character replacement, validation, encoding; Pattern caching for regex operations reduces compilation overhead
   - **S2DateUtil**: Date/time parsing, formatting, timezone handling

6. **Multi-Level Access Modes**
   - **Public Mode**: Adheres to public contracts (Getter/Setter) with maximum performance
   - **Private Mode**: Enables private member access when explicitly required

### [한국어]

1. **Method Handle을 활용한 고성능 리플렉션**
   - Java 표준 리플렉션(`java.lang.reflect`)의 성능 병목 제거
   - JIT 컴파일러가 MethodHandle 호출을 네이티브에 가까운 수준으로 최적화
   - ConcurrentHashMap에서 MethodHandle을 전략적으로 캐싱하여 반복 접근 시 오버헤드 제거

2. **지능형 캐싱 (듀얼 모드 지원)**
   - **기본**: 외부 의존성 없는 자체 구현 `S2OptimisticCache` 탑재
     - Lock-free 조회 및 낙관적/원자적 생성으로 최고의 성능 보장
     - Sequence 기반 LRU 축출 정책으로 메모리 효율 극대화
   - **선택 사항**: 엔터프라이즈급 부하 처리를 위한 **Caffeine Cache** 완벽 연동
     - 클래스패스에 Caffeine 라이브러리 존재 시 자동 감지 및 활성화
     - W-TinyLFU 알고리즘을 통한 극한의 캐시 적중률 제공
     - **확인 방법**: `S2Cache.isCaffeineEnabled()` 메서드로 Caffeine 활성화 여부를 확인할 수 있습니다.

3. **자바 버전별 적응형 스레드 팩토리**
   - Java 21 이상 환경에서 가상 스레드(Virtual Thread) 지원
   - 이전 버전 환경에서는 최적화된 플랫폼 스레드 풀로 폴백
   - 버전 간 호환성을 위한 통일된 API

4. **최적화된 데이터 접근 및 조작**
   - `getValue()`: 점 표기법(`user.address.street`)을 사용한 중첩 객체값 추출
   - `setValue()`: 배열/컬렉션 지원을 통한 중첩 구조값 설정
   - Map, List, Array, Record, DTO/VO, JPA Hibernate 프록시 지원
   - 대괄호 표기법 지원(`users[0].name`, `matrix[1][2]`)

5. **종합적인 유틸리티 모듈**
   - **S2Cache**: 패턴 기반 축출 정책의 고급 캐싱
   - **S2ThreadUtil**: 버전 인식형 최적화를 포함한 스레드 및 실행기 관리
   - **S2StringUtil**: 문자 치환, 검증, 인코딩 등의 문자열 조작; 정규식 캐싱으로 컴파일 오버헤드 감소
   - **S2DateUtil**: 날짜/시간 파싱, 포매팅, 타임존 처리

6. **다층 접근 모드**
   - **Public 모드**: Public 계약(Getter/Setter) 준수로 최대 성능 달성
   - **Private 모드**: 명시적으로 필요한 경우 private 멤버 접근 활성화

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

## ⚙️ Requirements (요구사항)

### [English]

This project is built with **JDK 21**, but it can be used reliably in all environments running **Java 17 or higher**.

### [한국어]

본 프로젝트는 **JDK 21** 환경에서 빌드되었으나, **Java 17 이상**의 모든 환경에서 안정적으로 사용할 수 있습니다.

---

## 📦 Dependencies (의존성)

This module has **ZERO mandatory runtime dependencies**.

- **Caffeine Cache**: Optional. Add this dependency only if you require advanced caching features for high-concurrency environments.

---

s2-core Version: 1.1.7 (2026-08-12)

[//]: # 'S2_DEPS_INFO_START'

---

**To use certain functionalities (e.g., S2BindValidator), the end-user project must explicitly add the following dependencies to be available at runtime.** Failure to include these dependencies will result in a `java.lang.NoClassDefFoundError` at runtime.

**[For Gradle Users]**

```groovy
dependencies {
    // Essential runtime dependencies for optional functionalities
    implementation 'com.github.ben-manes.caffeine:caffeine:3.2.3'
}
```

[//]: # 'S2_DEPS_INFO_END'
