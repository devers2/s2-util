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
   - `bindClause()` methods with condition checks
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
   - 조건 검사를 포함한 `bindClause()` 메서드
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
    implementation 'io.github.devers2:s2-jpa:1.1.2'
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
    .bindClause("name_cond", "John", "AND m.name = :name")
        .bindParameter("name", "John")
    .bindClause("age_cond", 30, "AND m.age > :age")
        .bindParameter("age", 30)
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
    .bindClause("search_cond", "John", "AND m.name LIKE :name")
        .bindParameter("name", "John", LikeMode.ANYWHERE)  // %John%
    .build();
```

---

### Pagination

You can apply pagination directly in the builder using `limit(offset, limit)`, which sets
JPA's `setFirstResult` and `setMaxResults` on the resulting `TypedQuery`.

```java
TypedQuery<Member> q = S2Jpql.from(entityManager)
    .type(Member.class)
    .query("SELECT m FROM Member m WHERE 1=1 {{=name_cond}}")
    .bindClause("name_cond", name, "AND m.name = :name")
        .bindParameter("name", name)
    .limit(0, 10) // offset 0, max 10 rows
    .build();

List<Member> page = q.getResultList();
```

Use `limit(offset, limit)` for simple pagination; for conditional application use the overload `limit(condition, offset, limit)`.

---

## ⚠️ Critical Security Warning: SQL Injection Prevention

### [English]

**ARCHITECTURE:** The `bindClause()` method is **EXCLUSIVELY** for binding dynamic SQL clauses conditionally. The `bindParameter()` method is **EXCLUSIVELY** for binding dynamic parameter values. This separation is critical to prevent SQL injection.

**RULE 1: Clauses must be hardcoded**

- The `clause` and `prefix`/`suffix` parameters of `bindClause()` **MUST** always be hardcoded strings
- **NEVER** concatenate user input into clause strings
- **NEVER** use `String.format()` or `+` operator to build clauses with variables

**RULE 2: Values go through bindParameter()**

- All dynamic/user-provided values **MUST** go through `bindParameter()`
- Do NOT pass values to the `conditionValue` parameter of `bindClause()`
- The `conditionValue` is **ONLY** for checking the condition (null check, boolean check, etc.)

#### SAFE Usage:

```java
// Step 1: Bind the clause conditionally (with hardcoded SQL)
String userName = userInput;  // From user request
S2Jpql.from(entityManager)
    .type(Member.class)
    .query("SELECT m FROM Member m WHERE 1=1 {{=name_search}}")
    .bindClause("name_search", userName, "AND m.name LIKE :name")  // Clause is hardcoded!
        .bindParameter("name", userName, LikeMode.ANYWHERE)  // Value bound safely here
    .build();
```

#### DANGEROUS Usage (DO NOT DO THIS):

```java
// ❌ WRONG: User input in clause string
String userName = userInput;
.bindClause("name_search", userName, "AND m.name LIKE '%" + userName + "%'")  // SQL INJECTION!

// ❌ WRONG: Trying to pass user value as parameter name
.bindClause("search", userName, "AND m.name LIKE :" + userName)  // SQL INJECTION!

// ❌ WRONG: Using String.format for dynamic clause building
String clause = String.format("AND m.name = %s", userName);  // SQL INJECTION!
.bindClause("cond", userName, clause)

// ❌ WRONG: No bindParameter call - clause parameters don't get bound
.bindClause("search", userName, "AND m.name = :name")  // Parameter :name will be NULL!
```

**Consequences of Ignoring These Rules:**

- SQL Injection vulnerabilities
- Unbound JPA parameters causing runtime errors
- Data breaches or unauthorized access

### [한국어]

**아키텍처:** `bindClause()` 메서드는 **동적 SQL 절을 조건부로 바인딩하기 위한 것**입니다. `bindParameter()` 메서드는 **동적 파라미터 값을 바인딩하기 위한 것**입니다. 이 분리는 SQL 인젝션을 방지하기 위해 매우 중요합니다.

**규칙 1: 절은 반드시 하드코딩**

- `bindClause()`의 `clause`, `prefix`/`suffix` 파라미터는 **반드시** 하드코딩된 문자열이어야 합니다
- **절대** 절 문자열에 사용자 입력을 연결하지 마세요
- **절대** `String.format()` 또는 `+` 연산자로 변수를 포함한 절을 만들지 마세요

**규칙 2: 값은 bindParameter()로**

- 모든 동적/사용자 제공 값은 **반드시** `bindParameter()`를 통해야 합니다
- `bindClause()`의 `conditionValue` 파라미터에 값을 전달하지 마세요
- `conditionValue`는 **조건 검사(null 체크, 불린 체크 등)용도만**입니다

#### 안전한 사용:

```java
// Step 1: 절을 조건부로 바인딩 (하드코딩된 SQL)
String userName = userInput;  // 사용자 입력
S2Jpql.from(entityManager)
    .type(Member.class)
    .query("SELECT m FROM Member m WHERE 1=1 {{=name_search}}")
    .bindClause("name_search", userName, "AND m.name LIKE :name")  // 절은 하드코딩됨!
        .bindParameter("name", userName, LikeMode.ANYWHERE)  // 값은 여기서 안전하게 바인딩
    .build();
```

#### 위험한 사용 (절대 하지 마세요):

```java
// ❌ 잘못됨: 사용자 입력이 절 문자열에 있음
String userName = userInput;
.bindClause("name_search", userName, "AND m.name LIKE '%" + userName + "%'")  // SQL 인젝션!

// ❌ 잘못됨: 사용자 값을 파라미터 이름으로 전달
.bindClause("search", userName, "AND m.name LIKE :" + userName)  // SQL 인젝션!

// ❌ 잘못됨: String.format으로 동적 절 만들기
String clause = String.format("AND m.name = %s", userName);  // SQL 인젝션!
.bindClause("cond", userName, clause)

// ❌ 잘못됨: bindParameter 호출 없음 - 절 파라미터가 바인딩되지 않음
.bindClause("search", userName, "AND m.name = :name")  // 파라미터 :name이 NULL 상태!
```

**이 규칙을 무시한 경우의 결과:**

- SQL 인젝션 취약점
- 바인딩되지 않은 JPA 파라미터로 인한 런타임 오류
- 데이터 유출 또는 무단 접근

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

s2-jpa Version: 1.1.2 (2026-01-27)

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
