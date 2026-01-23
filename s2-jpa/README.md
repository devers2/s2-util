# S2Util Library - JPA Module (s2-jpa)

## Overview (개요)

### [English]

The **s2-jpa** module provides a powerful and type-safe way to build dynamic JPQL (Jakarta Persistence Query Language) queries. It extends the S2Template class to offer specialized functionality for JPA operations, including conditional parameter binding, LIKE query safety, and flexible query construction. This module is designed for developers who need to create complex, dynamic database queries with a fluent and readable API.

### [한국어]

**s2-jpa** 모듈은 동적 JPQL (Jakarta Persistence Query Language) 쿼리를 구축하는 강력하고 타입 안전한 방법을 제공합니다. S2Template 클래스를 확장하여 JPA 작업을 위한 특화된 기능을 제공하며, 조건부 파라미터 바인딩, LIKE 쿼리 안전성, 유연한 쿼리 구성을 포함합니다. 복잡하고 동적인 데이터베이스 쿼리를 유연하고 읽기 쉬운 API로 생성해야 하는 개발자를 위해 설계되었습니다.

---

## ✨ Key Features (주요 기능)

### [English]

1. **Dynamic JPQL Query Building**
   - Template-based query construction with `{{=key}}` placeholders
   - Conditional clause inclusion based on parameter presence
   - Support for complex WHERE, ORDER BY, and JOIN clauses

2. **Type-Safe Parameter Binding**
   - `setParameter()` methods with condition checks
   - Automatic parameter type handling
   - Support for various parameter types (String, Number, Date, etc.)

3. **LIKE Query Safety**
   - `LikeMode` enum for safe wildcard (%) placement
   - Prevention of SQL injection in LIKE queries
   - Support for START, END, CONTAIN, and EXACT modes

4. **Fluent API Design**
   - Method chaining for readable query construction
   - `of()` factory method for easy instantiation
   - `build()` method to create TypedQuery

5. **Integration with S2Template**
   - Inherits all S2Template binding capabilities
   - Additional JPA-specific enhancements
   - Consistent API across S2Util modules

### [한국어]

1. **동적 JPQL 쿼리 빌딩**
   - `{{=key}}` 플레이스홀더를 사용한 템플릿 기반 쿼리 구성
   - 파라미터 존재 여부에 따른 조건부 절 포함
   - 복잡한 WHERE, ORDER BY, JOIN 절 지원

2. **타입 안전한 파라미터 바인딩**
   - 조건 검사를 포함한 `setParameter()` 메서드
   - 자동 파라미터 타입 처리
   - 다양한 파라미터 타입 지원 (String, Number, Date 등)

3. **LIKE 쿼리 안전성**
   - 안전한 와일드카드(%) 배치를 위한 `LikeMode` 열거형
   - LIKE 쿼리에서의 SQL 인젝션 방지
   - START, END, CONTAIN, EXACT 모드 지원

4. **유연한 API 디자인**
   - 읽기 쉬운 쿼리 구성을 위한 메서드 체이닝
   - 쉬운 인스턴스화를 위한 `of()` 팩토리 메서드
   - TypedQuery 생성을 위한 `build()` 메서드

5. **S2Template과의 통합**
   - 모든 S2Template 바인딩 기능 상속
   - 추가 JPA 특화 향상
   - S2Util 모듈 전반에 걸친 일관된 API

---

## 🚀 Quick Start (빠른 시작 가이드)

### 1. Installation (설치)

Add the following dependency to your `build.gradle`.

```groovy
dependencies {
    implementation 'io.github.devers2:s2-jpa:1.0.0'
}
```

### 2. Usage (사용법)

```java
// Create a dynamic JPQL query
String jpql = S2Jpql.of(
        """
        SELECT m FROM Member m
        WHERE 1=1
        {{=name_cond}}
        {{=age_cond}}
        {{=order_clause}}
        """
)
.setParameter("name", name, LikeMode.CONTAIN, "AND m.name LIKE :name")
.setParameter("age", age, "AND m.age > :age")
.setOrder(orderBy, "ORDER BY m." + orderBy + " " + direction)
.build();

// Execute the query
TypedQuery<Member> query = entityManager.createQuery(jpql, Member.class);
List<Member> results = query.getResultList();
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

s2-jpa Version: 1.0.0 (2026-01-23)

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
