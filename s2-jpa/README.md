# s2-jpa — Fluent Dynamic JPQL Query Builder (s2-util)

🌐 **English** | [한국어](README.ko.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.devers2/s2-jpa?color=brightgreen&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.devers2/s2-jpa)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](../LICENSE)

> 📦 Part of the **[s2-util suite](../README.md)**.  
> Works seamlessly with **[`s2-validator`](../s2-validator/README.md)** for query parameter validation and **[`s2-support`](https://github.com/devers2/s2-support)** for pagination (`S2PaginationInfo`).

---

## 📖 Overview

The **s2-jpa** module provides a powerful and type-safe way to build dynamic JPQL (Jakarta Persistence Query Language) queries. It extends the S2Template class to offer specialized functionality for JPA operations, including conditional parameter binding, LIKE query safety, and flexible query construction. The module features a fluent API design that integrates seamlessly with S2Template's binding methods, making it easy to create complex, dynamic database queries with automatic parameter binding and logging capabilities.

---

## ✨ Key Features

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
   - Consistent API across `s2-util` modules

6. **Built-in Logging**
   - Automatic logging of rendered JPQL queries
   - Parameter binding details for debugging
   - Execution flow visibility for development

---

## 🚀 Quick Start

### 1. Installation

Add the following dependency to your `build.gradle` or `pom.xml`.

**[Gradle]**

```groovy
dependencies {
    implementation 'io.github.devers2:s2-jpa:1.1.8'
}
```

**[Maven]**

```xml
<dependency>
    <groupId>io.github.devers2</groupId>
    <artifactId>s2-jpa</artifactId>
    <version>1.1.8</version>
</dependency>
```

### 2. Usage

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

> [!TIP]
> **Integration with `s2-support` (`S2PaginationInfo`):**  
> If you are using the companion library **[`s2-support`](https://github.com/devers2/s2-support)**, you can directly pass `getFirstRecordIndex()` and `getRecordCountPerPage()` to streamline pagination calculations and list queries:
> ```java
> S2PaginationInfo pagination = new S2PaginationInfo();
> pagination.setCurrentPageNo(pageNo);
> pagination.setRecordCountPerPage(10);
> pagination.setTotalRecordCount(totalCount);
>
> TypedQuery<Member> q = S2Jpql.from(entityManager)
>     .type(Member.class)
>     .query("SELECT m FROM Member m WHERE 1=1 {{=name_cond}}")
>     .bindClause("name_cond", name, "AND m.name = :name")
>         .bindParameter("name", name)
>     .limit(pagination.getFirstRecordIndex(), pagination.getRecordCountPerPage())
>     .build();
> ```

---

## ⚠️ Critical Security Warning: SQL Injection Prevention

**ARCHITECTURE:** The `bindClause()` method is **EXCLUSIVELY** for binding dynamic SQL clauses conditionally. The `bindParameter()` method is **EXCLUSIVELY** for binding dynamic parameter values. This separation is critical to prevent SQL injection.

**RULE 1: Clauses must be hardcoded**

- The `clause` and `prefix`/`suffix` parameters of `bindClause()` **MUST** always be hardcoded strings
- **NEVER** concatenate user input into clause strings
- **NEVER** use `String.format()` or `+` operator to build clauses with variables

**RULE 2: Values go through bindParameter()**

- All dynamic/user-provided values **MUST** go through `bindParameter()`
- Do NOT pass values to the `conditionValue` parameter of `bindClause()`
- The `conditionValue` is **ONLY** for checking the condition (null check, boolean check, etc.)

### SAFE Usage:

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

### DANGEROUS Usage (DO NOT DO THIS):

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

---

## ⚙️ Requirements

This project is built with **JDK 21**, but it can be used reliably in all environments running **Java 17 or higher**.

---

## 📜 License & Copyright

This library is provided under the **Apache License 2.0**. You are free to use, modify, and distribute this software, provided that you comply with the obligations of the license (such as copyright notice and source code disclosure requirements). For detailed terms and conditions, please refer to the **[LICENSE](./LICENSE)** file.

- **Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)**
- Contact: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**Third-party Notice:** This project uses external libraries. For detailed third-party license notices, please refer to the **[licenses/NOTICE](./licenses/NOTICE)** file.

---

s2-jpa Version: 1.1.8 (2026-09-11)

[//]: # 'S2_DEPS_INFO_START'

---

**To use certain functionalities (e.g., S2BindValidator), the end-user project must explicitly add the following dependencies to be available at runtime.** Failure to include these dependencies will result in a `java.lang.NoClassDefFoundError` at runtime.

**[For Gradle Users]**

```groovy
dependencies {
    // Essential runtime dependencies for optional functionalities
    implementation 'jakarta.persistence:jakarta.persistence-api:3.2.0'
}
```

[//]: # 'S2_DEPS_INFO_END'
