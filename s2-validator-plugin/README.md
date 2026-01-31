# S2 Validator Plugin - Gradle Build Plugin (s2-validator-plugin)

## Overview (개요)

### [English]

The **s2-validator-plugin** is a Gradle build plugin that performs static source code analysis to validate field names used in S2Validator configurations. While S2Validator leverages dot notation and array indexing for powerful nested object validation, it cannot verify at compile-time whether specified field names actually exist in target DTO classes. This plugin fills that gap by detecting typos, non-existent fields, and incorrect field references **before runtime**, preventing misconfiguration errors and enabling early error detection during the build process.

### [한국어]

**s2-validator-plugin**은 S2Validator 설정에 사용되는 필드명을 정적 소스 코드 분석으로 검증하는 Gradle 빌드 플러그인입니다. S2Validator는 점 표기법과 배열 인덱싱을 통해 강력한 중첩 객체 검증을 지원하지만, 컴파일 시점에 대상 DTO 클래스에 지정된 필드가 실제로 존재하는지 확인할 수 없습니다. 이 플러그인이 그 간격을 메워, 오타, 존재하지 않는 필드, 잘못된 필드 참조를 **런타임 전에** 감지하여 잘못된 설정을 방지하고 빌드 프로세스 중 조기 에러 감지를 가능하게 합니다.

---

## ✨ Key Features (주요 기능)

### [English]

1. **Static Source Code Analysis with JavaParser**
   - AST (Abstract Syntax Tree) parsing for accurate code analysis
   - Detects all `.field("fieldName")` calls in S2Validator configurations
   - Zero runtime overhead: Analysis happens only at build-time

2. **Compile-Time Field Validation**
   - Verifies that specified field names actually exist in target DTO classes
   - Identifies typos in field names before they cause runtime errors
   - Supports inheritance: Validates fields from parent classes as well

3. **Multi-Project Support**
   - Scans all subprojects within the root Gradle project
   - Finds DTO classes across multiple modules
   - Unified validation across complex multi-module builds

4. **Zero Configuration Required**
   - Automatically applies validation during standard Gradle build tasks
   - Executes before `compileJava` task: Prevents compilation of invalid code
   - Integrates with `check` task for CI/CD pipelines
   - Supports `bootRun` and other JavaExec tasks

5. **Field Caching for Performance**
   - Caches analyzed DTO field information in memory
   - Reuses cached data for repeated validation checks
   - Minimizes repeated file I/O and parsing operations

6. **Smart Validation Skipping**
   - Skips validation for generic types with `?` or `Object`
   - Gracefully handles cases where complete type information is unavailable
   - Prevents false positives from incomplete generic type parameters

7. **Detailed Error Reporting**
   - Clear error messages with color-coded output
   - Shows file paths, line numbers, and problematic field names
   - Identifies which target DTO class lacks the specified field
   - Example: `'address' 필드가 UserDTO에 없습니다` (Field 'address' not found in UserDTO)

8. **Build Failure on Errors**
   - Strict validation mode: Build fails immediately if errors detected
   - Prevents invalid code from progressing through the build pipeline
   - Ensures only properly configured validators reach production

### [한국어]

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

---

## 🔧 Installation (설치)

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
    id 'io.github.devers2.validator' version '1.1.1'
}
```

---

## ⚙️ Requirements (요구사항)

### [English]

- This project is built with **JDK 21**, but it can be used reliably in all environments running **Java 17 or higher**.
- **Gradle 8.0 or higher** is recommended.

### [한국어]

- 본 프로젝트는 **JDK 21** 환경에서 빌드되었으나, **Java 17 이상**의 모든 환경에서 안정적으로 사용할 수 있습니다.
- **Gradle 8.0 이상** 사용을 권장합니다.

### [한국어]

- 이 플러그인을 사용하려면 **Java 17 이상**이 필요합니다.
- **Gradle 8.0 이상** 사용을 권장합니다.

---

## Compatibility (호환성)

### [English]

This plugin supports **s2-validator version 1.1.0 or higher** to ensure optimal functionality and compatibility with the latest features.

### [한국어]

본 플러그인은 **s2-validator 1.1.0 버전 이상**을 지원하여 최신 기능과의 최적의 호환성을 보장합니다.

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

s2-validator-plugin Version: 1.1.1 (2026-02-01)
