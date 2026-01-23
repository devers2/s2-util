# S2Util Library - JPA Module (s2-jpa)

## Overview (개요)

### [English]

The **s2-jpa** module provides a powerful and type-safe way to build dynamic JPQL (Jakarta Persistence Query Language) queries. It extends the S2Template class to offer specialized functionality for JPA operations, including conditional parameter binding, LIKE query safety, and flexible query construction. The module features a fluent API design that integrates seamlessly with S2Template's binding methods, making it easy to create complex, dynamic database queries with automatic parameter binding and logging capabilities.

### [한국어]

**s2-jpa** 모듈은 동적 JPQL (Jakarta Persistence Query Language) 쿼리를 구축하는 강력하고 타입 안전한 방법을 제공합니다. S2Template 클래스를 확장하여 JPA 작업을 위한 특화된 기능을 제공하며, 조건부 파라미터 바인딩, LIKE 쿼리 안전성, 유연한 쿼리 구성을 포함합니다. Fluent API 디자인으로 S2Template의 바인딩 메소드와 완벽하게 통합되어, 자동 파라미터 바인딩과 로깅 기능으로 복잡하고 동적인 데이터베이스 쿼리를 쉽게 생성할 수 있습니다.

---

## ✨ Key Features (주요 기능)

### [English]

1. **Dynamic JPQL Query Building**
   - Template-based query construction with `{{=key}}` placeholders
   - Conditional clause inclusion based on parameter presence
   - Support for complex WHERE, ORDER BY, and JOIN clauses

2. **Type-Safe Parameter Binding**
   - `applyClause()` methods with condition checks
   - Automatic parameter type handling
   - Support for various parameter types (String, Number, Date, etc.)

3. **LIKE Query Safety**
   - `LikeMode` enum for safe wildcard (%) placement
   - Prevention of SQL injection in LIKE queries
   - Support for ANYWHERE, START, and END modes

4. **Fluent API Design**
   - Method chaining for readable query construction
   - `from()` factory method for EntityManager integration
   - `build()` method to create TypedQuery with automatic parameter binding

5. **Full S2Template Integration**
   - Inherits all S2Template binding capabilities (bind, bindWhen, bindIn, etc.)
   - Additional JPA-specific enhancements
   - Consistent API across S2Util modules

6. **Built-in Logging**
   - Automatic logging of rendered JPQL queries
   - Parameter binding details for debugging
   - Execution flow visibility for development

### [한국어]

1. **동적 JPQL 쿼리 빌딩**
   - `{{=key}}` 플레이스홀더를 사용한 템플릿 기반 쿼리 구성
   - 파라미터 존재 여부에 따른 조건부 절 포함
   - 복잡한 WHERE, ORDER BY, JOIN 절 지원

2. **안전한 파라미터 바인딩**
   - 조건 검사를 포함한 `applyClause()` 메서드
   - 자동 파라미터 타입 처리
   - 다양한 파라미터 타입 지원 (String, Number, Date 등)

3. **LIKE 쿼리 안전성**
   - 안전한 와일드카드(%) 배치를 위한 `LikeMode` 열거형
   - LIKE 쿼리에서의 SQL 인젝션 방지
   - ANYWHERE, START, END 모드 지원

4. **유연한 API 디자인**
   - 읽기 쉬운 쿼리 구성을 위한 메서드 체이닝
   - EntityManager 통합을 위한 `from()` 팩토리 메서드
   - 자동 파라미터 바인딩으로 TypedQuery 생성을 위한 `build()` 메서드

5. **완전한 S2Template 통합**
   - 모든 S2Template 바인딩 기능 상속 (bind, bindWhen, bindIn 등)
   - 추가 JPA 특화 향상
   - S2Util 모듈 전반에 걸친 일관된 API

6. **내장 로깅 기능**
   - 렌더링된 JPQL 쿼리의 자동 로깅
   - 디버깅을 위한 파라미터 바인딩 상세 정보
   - 개발 시 실행 흐름 가시성

---

## 🚀 Quick Start (빠른 시작 가이드)

### 1. Installation (설치)

Add the following dependency to your `build.gradle`.

```groovy
dependencies {
    implementation 'io.github.devers2:s2-jpa:1.0.5'
}
```

### 2. Usage (사용법)

```java
// Create a dynamic JPQL query with Fluent API
TypedQuery<Member> query = S2Jpql.from(entityManager)
    .type(Member.class)
    .query("""
        SELECT m FROM Member m
        WHERE 1=1
        {{=name_cond}}
        {{=age_cond}}
        {{=where_clause}}
        {{=order_clause}}
        """)
    .applyClause("name_cond", "name", "John", "AND m.name = :name")
    .applyClause("age_cond", "age", 30, "AND m.age > :age")
    .bindOrderBy("order_clause", "m.createdAt DESC")
    .build();

// Execute the query
List<Member> results = query.getResultList();
```

#### Advanced Usage with LIKE Modes

```java
TypedQuery<Member> searchQuery = S2Jpql.from(entityManager)
    .type(Member.class)
    .query("SELECT m FROM Member m WHERE m.name LIKE :name")
    .applyClause("dummy", "name", "John", "dummy", LikeMode.ANYWHERE)  // %John%
    .build();
```

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

s2-jpa Version: 1.0.5 (2026-01-23)

[//]: # 'S2_DEPS_INFO_START'

---

**To use certain functionalities (e.g., S2BindValidator), the end-user project must explicitly add the following dependencies to be available at runtime.** Failure to include these dependencies will result in a `java.lang.NoClassDefFoundError` at runtime.

**[For Gradle Users]**

```groovy
dependencies {
    // Essential runtime dependencies for optional functionalities
    implementation 'jakarta.persistence:jakarta.persistence-api:3.1.0'
}
```

[//]: # 'S2_DEPS_INFO_END'
