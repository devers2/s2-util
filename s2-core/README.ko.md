# S2Util Library - Core Module (s2-core)

🌐 [English](README.md) | **한국어**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.devers2/s2-core?color=brightgreen&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.devers2/s2-core)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](../LICENSE)

> 📦 **[S2Util 제품군](../README.ko.md)**의 핵심 기반 모듈입니다.

---

## 📖 개요 (Overview)

**s2-core** 모듈은 Java 개발에 최적화된 고성능 핵심 유틸리티 클래스를 제공하는 S2Util 프로젝트의 기반 라이브러리입니다. 효율적인 리플렉션을 위한 Method Handles, **외부 의존성 없는 자체 고속 캐시(선택적 Caffeine 가속 지원)**, Java 버전에 적응하는 스레드 팩토리 등의 고급 기술을 활용합니다. 핵심 기능으로는 최적화된 데이터 접근/조작 메서드(`getValue()`, `setValue()`), 문자열 유틸리티, 날짜/시간 처리, 타입 변환 등이 있습니다.

---

## ✨ 주요 기능 (Key Features)

1. **Method Handles 기반 고성능 리플렉션**
   - 표준 Java 리플렉션(`java.lang.reflect`)의 성능 병목 현상 제거
   - JIT 컴파일러가 MethodHandle 호출을 네이티브에 준하는 성능으로 최적화
   - 반복 접근을 위해 ConcurrentHashMap에 전략적 캐싱

2. **지능형 캐싱 (듀얼 모드)**
   - **기본 모드**: 속도와 단순성에 최적화된 내장 `S2OptimisticCache` (외부 의존성 없음)
     - Lock-Free 읽기 및 낙관적/원자적 쓰기
     - 시퀀스 기반 LRU 제거 전략
   - **선택 모드**: 엔터프라이즈급 워크로드를 위한 **Caffeine Cache** 원활한 통합
     - 고트래픽 시나리오에서 적중률을 극대화하는 W-TinyLFU 알고리즘
     - classpath에 Caffeine이 존재하면 자동으로 활성화
     - **확인 방법**: `S2Cache.isCaffeineEnabled()` 호출로 활성화 여부 확인 가능

3. **Java 버전 적응형 스레드 팩토리**
   - Java 21+ 환경에서 가상 스레드(Virtual Thread) 지원
   - 이전 버전 환경에서는 최적화된 플랫폼 스레드 풀로 자동 폴백
   - 버전 간 호환성을 위한 단일화된 API

4. **최적화된 데이터 접근 및 조작**
   - `getValue()`: 점 표기법(`user.address.street`)을 통해 중첩 객체에서 값 추출
   - `setValue()`: 배열/컬렉션 지원을 통해 중첩 구조에 값 설정
   - Map, List, Array, Record, DTO/VO, JPA 하이버네이트 프록시 지원
   - 대괄호 인덱싱 지원 (`users[0].name`, `matrix[1][2]`)

5. **포괄적인 유틸리티 모듈**
   - **S2Cache**: 패턴 기반 제거 정책을 갖춘 고급 캐싱
   - **S2ThreadUtil**: 버전 인식 최적화가 적용된 스레드 및 실행기 관리
   - **S2StringUtil**: 문자 치환, 검증, 인코딩을 지원하는 문자열 조작; 정규식 패턴 캐싱으로 컴파일 오버헤드 감소
   - **S2DateUtil**: 날짜/시간 파싱, 포맷팅, 타임존 처리

6. **다단계 접근 모드**
   - **Public 모드**: 최고 성능으로 공개 규약(Getter/Setter) 준수
   - **Private 모드**: 명시적으로 필요한 경우 비공개 멤버 접근 허용

---

## 🚀 빠른 시작 가이드 (Quick Start)

### 1. 설치 (Installation)

`build.gradle` 또는 `pom.xml`에 다음 의존성을 추가합니다.

**[Gradle]**

```groovy
dependencies {
    implementation 'io.github.devers2:s2-core:1.1.7'
}
```

**[Maven]**

```xml
<dependency>
    <groupId>io.github.devers2</groupId>
    <artifactId>s2-core</artifactId>
    <version>1.1.7</version>
</dependency>
```

### 2. 주요 사용법 (Usage Examples)

#### 중첩 객체 접근 및 수정 (`getValue` / `setValue`)

점 표기법(dot notation) 및 인덱스 표기법을 통해 DTO, Map, List, Record 등의 필드에 직관적이고 빠르게 접근합니다:

```java
// 점 표기법 및 배열 인덱싱을 통한 필드 값 조회
String street = S2Util.getValue(user, "address.street");
String firstRole = S2Util.getValue(user, "roles[0].name");

// 동적 필드 값 설정
S2Util.setValue(user, "address.city", "Seoul");
```

#### 고성능 리플렉션 및 캐싱

```java
// MethodHandle 기반 고성능 프로퍼티 접근
Object value = S2Util.getValue(targetDto, "fieldName");

// 외부 의존성 없는 자체 고속 캐시 (Caffeine이 classpath에 있으면 자동 가속)
boolean isCaffeineActive = S2Cache.isCaffeineEnabled();
```

#### 자바 버전 적응형 스레드 유틸리티

```java
// Java 21+ 환경에서는 가상 스레드(Virtual Thread)를 사용하고 이전 버전에서는 최적화된 풀로 자동 폴백
ExecutorService executor = S2ThreadUtil.getCommonExecutor();
```

---

## ⚙️ 요구사항 (Requirements)

본 프로젝트는 **JDK 21** 환경에서 빌드되었으나, **Java 17 이상**의 모든 환경에서 안정적으로 사용할 수 있습니다.

---

## 📦 의존성 (Dependencies)

본 모듈은 **필수 런타임 의존성이 전혀 없습니다 (ZERO dependencies)**.

- **Caffeine Cache**: 선택 사항. 대규모 동시성 환경을 위한 고급 캐싱 기능이 필요한 경우에만 추가합니다.

---

## 📜 라이선스 및 저작권 (License & Copyright)

본 라이브러리는 **Apache License 2.0** 하에 제공됩니다. 사용자는 라이선스의 의무 사항(저작권 고지, 소스 코드 공개 범위 등)을 준수하는 조건 하에 자유롭게 사용, 수정 및 재배포가 가능합니다. 상세한 조건은 **[LICENSE](./LICENSE)** 파일을 반드시 확인해 주세요.

- **저작권 2020 - 2026 devers2 (이승수, 대한민국 대전)**
- 문의: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**제3자 라이브러리 고지:** 본 프로젝트는 외부 라이브러리를 사용합니다. 상세한 제3자 라이브러리 고지사항은 **[licenses/NOTICE](./licenses/NOTICE)** 파일을 참조해 주세요.

---

s2-core Version: 1.1.7 (2026-09-09)

[//]: # 'S2_DEPS_INFO_START'

---

**특정 기능(예: S2BindValidator)을 사용하려면 런타임에 다음 의존성을 엔드유저 프로젝트에 명시적으로 추가해야 합니다.** 이 의존성이 누락되면 런타임에 `java.lang.NoClassDefFoundError`가 발생합니다.

**[Gradle 사용자]**

```groovy
dependencies {
    // 선택적 기능을 위한 필수 런타임 의존성
    implementation 'com.github.ben-manes.caffeine:caffeine:3.2.4'
}
```

[//]: # 'S2_DEPS_INFO_END'
