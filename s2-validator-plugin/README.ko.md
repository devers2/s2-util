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
   - 경로 검사: `address.city`, `items[0].name`, `products[].price`는 선언 타입을 따라 중첩 객체와 배열·컬렉션(`List`,
     `Set` 등)·맵 값의 요소 타입까지 부분마다 확인 (`Map`, 타입 변수, JDK 타입처럼 소스로 알 수 없는 지점에서는 멈춤)
   - 중첩 DTO: 다른 클래스 안에 선언한 `record`·static 클래스(`Join.Form`)도 검사

3. **다중 프로젝트 지원**
   - 루트 Gradle 프로젝트 내의 모든 서브프로젝트 스캔
   - 여러 모듈 간 DTO 클래스 찾기
   - 복잡한 다중 모듈 빌드 전반에 걸친 통일된 검증

4. **설정 불필요**
   - 표준 Gradle 빌드 태스크 중 자동으로 검증 적용
   - `compileJava` 태스크 전에 실행: 무효한 코드 컴파일 방지
   - CI/CD 파이프라인을 위한 `check` 태스크 통합
   - `bootRun` 및 기타 JavaExec 태스크 지원

5. **증분 빌드·빌드 캐시**
   - 모든 프로젝트의 `src/main/java`를 태스크 입력으로 선언: 바뀐 소스가 없으면 `UP-TO-DATE`로 건너뜀
   - `@CacheableTask`: `--build-cache`에서 이전 결과를 `FROM-CACHE`로 복원
   - 한 번의 실행 안에서는 분석한 DTO 필드 정보를 메모리에 캐싱
   - `bind(...)` 사용 경고는 `build/s2-validator/checkS2Validators.txt`에도 남음 (`UP-TO-DATE`일 때는 다시 출력되지 않음)

6. **대상 DTO 추론**
   - 명시적 타입 인자: `S2Validator.<UserDTO>builder()`, `S2Validator.<UserDTO>of(dto)`
   - 타입 추론: `S2Validator.of(dto)`는 인자의 선언 타입(메서드·람다 파라미터, 지역 변수, 필드, `var x = new UserDTO()`,
     `new UserDTO()`, 캐스트)에서 DTO 를 찾음
   - `builder()`는 Java 가 대입 대상에서 타입을 추론하지 않으므로 타입 인자가 필요
   - `?`, `Object`, JDK 타입(`Map` 등), 판별할 수 없는 타입은 검증 스킵 (소스가 없는 클래스는 한 번만 알림)

7. **상세한 에러 보고**
   - 색상 코딩된 명확한 에러 메시지
   - 파일 경로, 줄 번호, 문제가 있는 필드명 표시
   - 지정된 필드가 없는 대상 DTO 클래스 식별
   - 예시: `'address' 필드가 UserDTO에 없습니다`, `'address.ctiy'의 'ctiy' 필드가 Address에 없습니다`
   - 여러 줄로 이어 쓴 체인에서도 오류가 난 `.field(...)` 줄을 표시

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

10. **규칙 기준값·조건 값 검사**
   - 소스에 리터럴로 적힌 기준값이 규칙에 맞는지 검사 (규칙별 전용 메서드 없이 컴파일 시점에 잡음)
   - 빌드 실패: `.rule(MAX_LENGTH, "abc")`·`10.5`, `.rule(MIN_VALUE, "19살")`, 기준값 누락(`.rule(MAX_LENGTH)`),
     `NESTED`/`EACH`에 문자열, `when(field, GT, "abc")`, `IN`에 목록이 아닌 값, `EMPTY`에 비교 값
   - 런타임이 **조용히 틀리는** 경우도 빌드 실패: 문법이 틀린 `REGEX`(항상 형식 오류로 판정), 기준값을 받지 않는 규칙에 준 값
     (`.rule(EMAIL, "...")`는 무시됨, 메시지 키용 `null`은 허용), `JUMIN`/`BIZRNO`의 true/false 가 아닌 값(끔으로 처리)
   - 경고: `EQUALS_FIELD`/`DATE_AFTER`/`DATE_BEFORE`의 비교 대상 필드가 DTO 에 없음 (NESTED/EACH 하위 검증기는 바깥 필드를 참조할 수 있어 경고만)
   - 변수·상수·메서드 호출로 넘긴 값은 건너뛰며, 런타임이 검증기를 만들 때 검사합니다

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
    id 'io.github.devers2.validator' version '2.0.0'
}
```

---

## ⚙️ 요구사항 (Requirements)

- 본 프로젝트는 **JDK 21** 환경에서 빌드되었으나, **Java 17 이상**의 모든 환경에서 안정적으로 사용할 수 있습니다.
- **Gradle 8.0 이상** 사용을 권장합니다.

---

## 🔄 호환성 (Compatibility)

본 플러그인은 **s2-validator 2.0.0** 과 함께 사용합니다. 필드 이름 검사는 1.1.0 이상에서도 동작하지만, 규칙 기준값·조건 값 검사는 2.0.0 의 규칙(`S2RuleType`, `S2Operator`)을 기준으로 합니다.

---

## 📜 라이선스 및 저작권 (License & Copyright)

본 라이브러리는 **Apache License 2.0** 하에 제공됩니다. 사용자는 라이선스의 의무 사항(저작권 고지, 소스 코드 공개 범위 등)을 준수하는 조건 하에 자유롭게 사용, 수정 및 재배포가 가능합니다. 상세한 조건은 **[LICENSE](./LICENSE)** 파일을 반드시 확인해 주세요.

- **저작권 2020 - 2026 devers2 (이승수, 대한민국 대전)**
- 문의: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**제3자 라이브러리 고지:** 본 프로젝트는 외부 라이브러리를 사용합니다. 상세한 제3자 라이브러리 고지사항은 **[licenses/NOTICE](./licenses/NOTICE)** 파일을 참조해 주세요.

---

s2-validator-plugin Version: 2.0.0 (2026-10-02)
