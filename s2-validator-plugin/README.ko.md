# s2-validator-plugin — Gradle 빌드 플러그인 (s2-util)

🌐 [English](README.md) | **한국어**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.devers2/s2-validator-plugin?color=brightgreen&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.devers2/s2-validator-plugin)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](../LICENSE)

> 📦 **[s2-util 제품군](../README.ko.md)**의 Gradle 정적 분석 플러그인입니다.

---

## 📖 개요 (Overview)

**s2-validator-plugin**은 `s2-validator` 설정에 사용되는 필드명을 정적 소스 코드 분석으로 검증하는 Gradle 빌드 플러그인입니다. `s2-validator`는 점 표기법과 배열 인덱싱을 통해 강력한 중첩 객체 검증을 지원하지만, 컴파일 시점에 대상 DTO 클래스에 지정된 필드가 실제로 존재하는지 확인할 수 없습니다. 이 플러그인이 그 간격을 메워, 오타, 존재하지 않는 필드, 잘못된 필드 참조를 **런타임 전에** 감지하여 잘못된 설정을 방지하고 빌드 프로세스 중 조기 에러 감지를 가능하게 합니다. 또한 플러그인은 **체이닝 완결성**을 검증합니다 — `S2Validator.of()`, `builder()`, `check()` 로 시작한 모든 체인이 반드시 종단 메서드(`.validate()` 또는 `.build()`)로 끝나는지 확인합니다. 불완전한 체인은 죽은 코드(Dead Code)입니다: 검증 로직이 정의되어 있지만 **실제로는 실행되지 않습니다**.

---

## ✨ 주요 기능 (Key Features)

1. **JavaParser를 활용한 정적 소스 코드 분석**
   - AST(추상 구문 트리) 파싱으로 정확한 코드 분석
   - S2Validator 설정의 모든 `.field("fieldName")` 호출 감지
   - 런타임 오버헤드 없음: 빌드 시점에만 분석 수행

2. **컴파일 시 필드 검증**
   - 지정된 필드명이 대상 DTO 클래스에 실제로 존재하는지 확인
   - 런타임 에러가 되기 전에 필드명 오타 감지
   - 상속 지원: 부모 클래스의 필드도 함께 검증

3. **다중 프로젝트 지원**
   - 루트 Gradle 프로젝트 내의 모든 서브프로젝트 스캔
   - 여러 모듈 간 DTO 클래스 찾기
   - 복잡한 다중 모듈 빌드 전반에 걸친 통일된 검증

4. **설정 불필요**
   - 표준 Gradle 빌드 태스크 중 자동으로 검증 적용
   - `compileJava` 태스크 전에 실행: 무효한 코드 컴파일 방지
   - CI/CD 파이프라인을 위한 `check` 태스크 통합
   - `bootRun` 및 기타 JavaExec 태스크 지원

5. **성능 최적화를 위한 필드 캐싱**
   - 분석된 DTO 필드 정보를 메모리에 캐싱
   - 반복적인 검증 확인을 위해 캐시된 데이터 재사용
   - 반복적인 파일 I/O 및 파싱 작업 최소화

6. **스마트 검증 스킵**
   - `?` 또는 `Object`를 가진 제네릭 타입에 대한 검증 스킵
   - 완전한 타입 정보를 사용할 수 없는 경우를 우아하게 처리
   - 불완전한 제네릭 타입 매개변수로 인한 거짓 양성 방지

7. **상세한 에러 보고**
   - 색상 코딩된 명확한 에러 메시지
   - 파일 경로, 줄 번호, 문제가 있는 필드명 표시
   - 지정된 필드가 없는 대상 DTO 클래스 식별
   - 예시: `'address' 필드가 UserDTO에 없습니다`

8. **에러 발생 시 빌드 실패**
   - 엄격한 검증 모드: 에러 감지 시 즉시 빌드 실패
   - 잘못된 코드가 빌드 파이프라인을 진행하지 못하도록 방지
   - 올바르게 설정된 검증 도구만 프로덕션에 도달하도록 보장

9. **체이닝 완결성 검사 (Dead Code 감지)**
   - 종단 메서드가 누락된 불완전한 검증기 체인을 감지
   - `S2Validator.of()` 체인은 반드시 `.validate()`로 끝나야 함 — 누락 시 검증이 **실행되지 않는 죽은 코드** 생성
   - `S2Validator.builder()` 체인은 반드시 `.build()`로 끝나야 함 — 누락 시 검증기가 **생성되지 않는 죽은 코드** 생성
   - `S2Validator.check()` 체인은 반드시 `.validate()`로 끝나야 함 — 누락 시 검사가 **수행되지 않는 죽은 코드** 생성
   - 오류 발생 파일, 줄 번호, 시작 메서드, 기대 종단 메서드를 명확히 보고
   - 즉시 빌드 실패: 죽은 코드가 프로덕션에 배포되는 것을 방지

---

## 🔧 설치 (Installation)

### settings.gradle

```groovy
pluginManagement {
    repositories {
        mavenCentral()
    }
}
```

### build.gradle

```groovy
plugins {
    id 'io.github.devers2.validator' version '1.1.2'
}
```

---

## ⚙️ 요구사항 (Requirements)

- 본 프로젝트는 **JDK 21** 환경에서 빌드되었으나, **Java 17 이상**의 모든 환경에서 안정적으로 사용할 수 있습니다.
- **Gradle 8.0 이상** 사용을 권장합니다.

---

## 🔄 호환성 (Compatibility)

본 플러그인은 **s2-validator 1.1.0 버전 이상**을 지원하여 최신 기능과의 최적의 호환성을 보장합니다.

---

## 📜 라이선스 및 저작권 (License & Copyright)

본 라이브러리는 **Apache License 2.0** 하에 제공됩니다. 사용자는 라이선스의 의무 사항(저작권 고지, 소스 코드 공개 범위 등)을 준수하는 조건 하에 자유롭게 사용, 수정 및 재배포가 가능합니다. 상세한 조건은 **[LICENSE](./LICENSE)** 파일을 반드시 확인해 주세요.

- **저작권 2020 - 2026 devers2 (이승수, 대한민국 대전)**
- 문의: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**제3자 라이브러리 고지:** 본 프로젝트는 외부 라이브러리를 사용합니다. 상세한 제3자 라이브러리 고지사항은 **[licenses/NOTICE](./licenses/NOTICE)** 파일을 참조해 주세요.

---

s2-validator-plugin Version: 1.1.2 (2026-08-12)
