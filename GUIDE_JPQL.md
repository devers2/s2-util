# S2Jpql: Secure Dynamic Query Builder Guide (안전한 동적 쿼리 빌더 가이드) 🔎

> **Secure, Template-Based Dynamic Query Generation with Zero SQL Injection Risk**
> <br>**SQL Injection 걱정 없이 안전한 동적 JPQL 쿼리 생성**

Utilize Java Text Blocks (`"""`) for cleaner JPQL. `bindClause()` handles conditional clause binding, and `bindParameter()` exclusively handles parameter value binding for SQL injection prevention.

<br>Java Text Block(`"""`)으로 쿼리 가독성을 높입니다. `bindClause()`는 조건부 절 바인딩, `bindParameter()`는 파라미터 값 바인딩을 담당하여 SQL Injection을 방지합니다.

---

## 1. Core Concepts (핵심 개념)

### 1-1. The Two-Method Binding Strategy (이중 바인딩 전략)

S2Jpql uses a two-method approach to prevent SQL injection:

1. **`bindClause()`**: Conditionally includes hardcoded SQL clauses
   <br>조건부로 하드코딩된 SQL 절을 포함

2. **`bindParameter()`**: Safely binds parameter values
   <br>파라미터 값을 안전하게 바인딩

```
┌──────────────────────────────────────────────────┐
│        Template JPQL with Placeholders           │
│  WHERE 1=1 {{=cond_name}} {{=cond_price}}       │
└──────────────────────────────────────────────────┘
           │                    │
       Resolved by          Resolved by
   bindClause() ←────────→ bindParameter()
   (Hardcoded)           (Parameterized)
```

---

## 2. Basic Usage (기본 사용법)

### 2-1. Simple Query with Conditional Clauses (조건부 절이 있는 간단한 쿼리)

```java
// [English] JPQL template with placeholders
// [한국어] 플레이스홀더가 있는 JPQL 템플릿

String jpql = """
    SELECT p
    FROM Product p
    WHERE 1=1
        {{=cond_name}}
        {{=cond_price}}
    {{=sort}}
""";

// [English] Build and execute query with conditions
// [한국어] 조건을 포함하여 쿼리 빌드 및 실행

List<Product> products = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)

    // [English] Bind conditional clause
    // [한국어] 조건부 절 바인딩
    .bindClause("cond_name", name, "AND p.name LIKE :name")
        // [English] Bind the parameter value
        // [한국어] 파라미터 값 바인딩
        .bindParameter("name", name, LikeMode.ANYWHERE)

    // [English] Another conditional clause
    // [한국어] 다른 조건부 절
    .bindClause("cond_price", price, "AND p.price >= :price")
        .bindParameter("price", price)

    // [English] Conditional order by
    // [한국어] 조건부 ORDER BY
    .bindOrderBy("sort", sort)

    .build()
    .getResultList();
```

### 2-2. Conditional Binding (조건부 바인딩)

```java
// [English] Only include clause if condition is true
// [한국어] 조건이 참일 때만 절 포함

// Case 1: Null check (null 체크)
String jpql = "SELECT p FROM Product p WHERE 1=1 {{=cond_name}}";

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .bindClause("cond_name", name != null && !name.isEmpty(),
                "AND p.name LIKE :name")
        .bindParameter("name", name, LikeMode.ANYWHERE)
    .build()
    .getResultList();

// Case 2: Zero-based check (0 체크)
.bindClause("cond_price", price > 0, "AND p.price >= :price")
    .bindParameter("price", price)

// Case 3: Collection check (컬렉션 체크)
.bindClause("cond_status", !statuses.isEmpty(),
            "AND p.status IN :statuses")
    .bindParameter("statuses", statuses)
```

---

## 3. Parameter Binding Methods (파라미터 바인딩 메서드)

### 3-1. Basic Value Binding (기본 값 바인딩)

```java
// [English] Bind simple values
// [한국어] 단순 값 바인딩

String jpql = "SELECT p FROM Product p WHERE p.id = :id AND p.status = :status";

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .bindParameter("id", 123)
    .bindParameter("status", ProductStatus.ACTIVE)
    .build()
    .getResultList();
```

### 3-2. String Matching with LikeMode (문자열 매칭)

```java
// [English] Different LIKE patterns
// [한국어] 다양한 LIKE 패턴

String jpql = "SELECT p FROM Product p WHERE p.name LIKE :name";

// [English] Pattern 1: ANYWHERE (contains) - "%keyword%"
// [한국어] 패턴 1: 어디든 포함 - "%keyword%"
.bindParameter("name", "laptop", LikeMode.ANYWHERE)

// [English] Pattern 2: START (prefix) - "keyword%"
// [한국어] 패턴 2: 시작 - "keyword%"
.bindParameter("name", "laptop", LikeMode.START)

// [English] Pattern 3: END (suffix) - "%keyword"
// [한국어] 패턴 3: 끝 - "%keyword"
.bindParameter("name", "pro", LikeMode.END)

// [English] Pattern 4: EXACT (no wildcards) - "keyword"
// [한국어] 패턴 4: 정확히 - "keyword"
.bindParameter("name", "laptop", LikeMode.EXACT)
```

### 3-3. Collection Binding (컬렉션 바인딩)

```java
// [English] Bind lists for IN clause
// [한국어] IN 절을 위한 리스트 바인딩

String jpql = "SELECT p FROM Product p WHERE p.id IN :ids";

List<Long> ids = List.of(1L, 2L, 3L);

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .bindParameter("ids", ids)
    .build()
    .getResultList();
```

---

## 4. Pagination (페이징)

### 4-1. Simple Pagination (단순 페이징)

```java
// [English] Offset-based pagination
// [한국어] 오프셋 기반 페이징

String jpql = "SELECT p FROM Product p ORDER BY p.id DESC";

List<Product> page1 = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .limit(0, 20)    // rows 0-19 (first page)
    .build()
    .getResultList();

List<Product> page2 = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .limit(20, 20)   // rows 20-39 (second page)
    .build()
    .getResultList();

List<Product> page3 = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .limit(40, 20)   // rows 40-59 (third page)
    .build()
    .getResultList();
```

### 4-2. Conditional Pagination (조건부 페이징)

```java
// [English] Apply pagination only when condition is true
// [한국어] 조건이 참일 때만 페이징 적용

String jpql = "SELECT p FROM Product p WHERE 1=1 {{=cond_name}}";

boolean shouldPaginate = pageSize > 0 && pageNumber >= 0;

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .bindClause("cond_name", keyword != null,
                "AND p.name LIKE :name")
        .bindParameter("name", keyword, LikeMode.ANYWHERE)

    // [English] Only apply pagination if condition is true
    // [한국어] 조건이 참일 때만 페이징 적용
    .limit(shouldPaginate, pageNumber * pageSize, pageSize)

    .build()
    .getResultList();
```

### 4-3. Offset and Limit Methods (오프셋과 리미트 메서드)

```java
// [English] Method signatures
// [한국어] 메서드 시그니처

// Unconditional pagination (무조건 페이징)
.limit(offset, limit)

// Conditional pagination (조건부 페이징)
.limit(condition, offset, limit)

// Example / 예제
.limit(true, 0, 20)        // Always paginate
.limit(keyword != null, 0, 20)  // Paginate only if keyword exists
```

---

## 5. Ordering (정렬)

### 5-1. Conditional ORDER BY (조건부 정렬)

```java
// [English] Dynamic ORDER BY based on user input
// [한국어] 사용자 입력에 따른 동적 정렬

String jpql = """
    SELECT p
    FROM Product p
    WHERE 1=1
    {{=sort}}
""";

String sortBy = request.getParameter("sort"); // "name", "price", etc.

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    // [English] Bind conditional ORDER BY
    // [한국어] 조건부 ORDER BY 바인딩
    .bindOrderBy("sort", sortBy)
    .build()
    .getResultList();
```

### 5-2. Supported Sort Values (지원되는 정렬 값)

```java
// [English] Order by field name (e.g., "name" → "ORDER BY p.name ASC")
// [한국어] 필드명으로 정렬 (예: "name" → "ORDER BY p.name ASC")
.bindOrderBy("sort", "name")      // → ORDER BY p.name ASC
.bindOrderBy("sort", "-name")     // → ORDER BY p.name DESC

// [English] Multiple fields separated by comma
// [한국어] 쉼표로 구분된 여러 필드
.bindOrderBy("sort", "price,-date")  // → ORDER BY p.price ASC, p.date DESC
```

---

## 6. Complete Example: Search & Pagination (전체 예제)

```java
// [English] Product search with dynamic conditions and pagination
// [한국어] 동적 조건과 페이징이 있는 상품 검색

@GetMapping("/products")
public String searchProducts(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer minPrice,
        @RequestParam(required = false) Integer maxPrice,
        @RequestParam(defaultValue = "name") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        Model model) {

    // [English] Build JPQL template
    // [한국어] JPQL 템플릿 구성
    String jpql = """
        SELECT p
        FROM Product p
        WHERE 1=1
            {{=cond_name}}
            {{=cond_min_price}}
            {{=cond_max_price}}
        {{=sort}}
    """;

    int offset = page * pageSize;

    // [English] Execute query with all conditions
    // [한국어] 모든 조건과 함께 쿼리 실행
    List<Product> results = S2Jpql.from(em)
        .type(Product.class)
        .query(jpql)

        // [English] Optional name filter
        // [한국어] 선택적 이름 필터
        .bindClause("cond_name", name != null && !name.isEmpty(),
                    "AND p.name LIKE :name")
            .bindParameter("name", name, LikeMode.ANYWHERE)

        // [English] Optional minimum price filter
        // [한국어] 선택적 최소 가격 필터
        .bindClause("cond_min_price", minPrice != null && minPrice > 0,
                    "AND p.price >= :minPrice")
            .bindParameter("minPrice", minPrice)

        // [English] Optional maximum price filter
        // [한국어] 선택적 최대 가격 필터
        .bindClause("cond_max_price", maxPrice != null && maxPrice > 0,
                    "AND p.price <= :maxPrice")
            .bindParameter("maxPrice", maxPrice)

        // [English] Dynamic ordering
        // [한국어] 동적 정렬
        .bindOrderBy("sort", sort)

        // [English] Pagination
        // [한국어] 페이징
        .limit(offset, pageSize)

        .build()
        .getResultList();

    model.addAttribute("products", results);
    model.addAttribute("page", page);
    model.addAttribute("pageSize", pageSize);
    return "products/list";
}
```

---

## 7. SQL Injection Prevention (SQL Injection 방지)

### ⚠️ Critical Security Rules (중요 보안 규칙)

> [!WARNING]
> **[English]** **ARCHITECTURE:** The `bindClause()` method is **EXCLUSIVELY** for binding dynamic SQL clauses conditionally. The `bindParameter()` method is **EXCLUSIVELY** for binding dynamic parameter values. This separation is critical to prevent SQL injection.
>
> **RULE 1: Clauses must be hardcoded**
>
> - The `clause` and `prefix`/`suffix` parameters of `bindClause()` **MUST** always be hardcoded strings
> - **NEVER** concatenate user input into clause strings
> - **NEVER** use `String.format()` or `+` operator to build clauses with variables
>
> **RULE 2: Values go through bindParameter()**
>
> - All dynamic/user-provided values **MUST** go through `bindParameter()`
> - Do NOT pass values to the `conditionValue` parameter of `bindClause()`
> - The `conditionValue` is **ONLY** for checking the condition (null check, boolean check, etc.)
>
> <br>**[한국어]** **아키텍처:** `bindClause()` 메서드는 **동적 SQL 절을 조건부로 바인딩하기 위한 것**입니다. `bindParameter()` 메서드는 **동적 파라미터 값을 바인딩하기 위한 것**입니다. 이 분리는 SQL 인젝션을 방지하기 위해 매우 중요합니다.
>
> **규칙 1: 절은 반드시 하드코딩**
>
> - `bindClause()`의 `clause`, `prefix`/`suffix` 파라미터는 **반드시** 하드코딩된 문자열이어야 합니다
> - **절대** 절 문자열에 사용자 입력을 연결하지 마세요
> - **절대** `String.format()` 또는 `+` 연산자로 변수를 포함한 절을 만들지 마세요
>
> **규칙 2: 값은 bindParameter()로**
>
> - 모든 동적/사용자 제공 값은 **반드시** `bindParameter()`를 통해야 합니다
> - `bindClause()`의 `conditionValue` 파라미터에 값을 전달하지 마세요
> - `conditionValue`는 **조건 검사(null 체크, 불린 체크 등)용도만**입니다

### 7-1. SAFE Usage (안전한 사용법)

```java
// ✅ CORRECT: Clause is hardcoded, value is parameterized
String jpql = "SELECT p FROM Product p WHERE 1=1 {{=cond_name}}";

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)

    // Clause is hardcoded (built at development time)
    .bindClause("cond_name", userInput != null,
                "AND p.name LIKE :name")  // ← Hardcoded string
        // Value is parameterized (bound at runtime)
        .bindParameter("name", userInput, LikeMode.ANYWHERE)  // ← Parameterized

    .build()
    .getResultList();
```

### 7-2. DANGEROUS Usage (위험한 사용법 - 절대 금지)

```java
// ❌ WRONG: User input in clause string (SQL INJECTION!)
.bindClause("cond", userInput,
            "AND p.name LIKE '%" + userInput + "%'")  // ← INJECTION!

// ❌ WRONG: Using String.format for dynamic clause building
String clause = String.format("AND p.name = %s", userInput);
.bindClause("cond", userInput, clause)  // ← INJECTION!

// ❌ WRONG: Dynamic field names without binding
String sortField = request.getParameter("sortBy");
String jpql = "SELECT p FROM Product p ORDER BY p." + sortField;  // ← INJECTION!

// ❌ WRONG: No bindParameter call - value not bound
.bindClause("search", userInput, "AND p.name = :name")
    // Missing: .bindParameter("name", userInput)
    // :name will remain unbound and cause SQL errors!
```

---

## 8. Advanced Features (고급 기능)

### 8-1. Multiple Conditions (다중 조건)

```java
// [English] Build complex queries with multiple optional conditions
// [한국어] 여러 선택적 조건으로 복잡한 쿼리 구성

String jpql = """
    SELECT p
    FROM Product p
    WHERE 1=1
        {{=cond_status}}
        {{=cond_category}}
        {{=cond_price_range}}
        {{=cond_rating}}
    {{=sort}}
""";

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)

    .bindClause("cond_status", status != null,
                "AND p.status = :status")
        .bindParameter("status", status)

    .bindClause("cond_category", category != null,
                "AND p.category = :category")
        .bindParameter("category", category)

    .bindClause("cond_price_range", minPrice != null && maxPrice != null,
                "AND p.price BETWEEN :minPrice AND :maxPrice")
        .bindParameter("minPrice", minPrice)
        .bindParameter("maxPrice", maxPrice)

    .bindClause("cond_rating", minRating != null,
                "AND p.rating >= :minRating")
        .bindParameter("minRating", minRating)

    .bindOrderBy("sort", sort)
    .limit(page * pageSize, pageSize)

    .build()
    .getResultList();
```

### 8-2. JOIN Conditions (조인 조건)

```java
// [English] Query with JOINs and multiple conditions
// [한국어] JOIN과 다중 조건이 있는 쿼리

String jpql = """
    SELECT p
    FROM Product p
    JOIN p.category c
    JOIN p.reviews r
    WHERE 1=1
        {{=cond_category}}
        {{=cond_rating}}
    GROUP BY p.id
    HAVING COUNT(r) > :minReviews
    {{=sort}}
""";

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)

    .bindClause("cond_category", categoryId != null,
                "AND c.id = :categoryId")
        .bindParameter("categoryId", categoryId)

    .bindClause("cond_rating", minRating != null,
                "AND r.rating >= :minRating")
        .bindParameter("minRating", minRating)

    .bindParameter("minReviews", 1)
    .bindOrderBy("sort", sort)
    .limit(page * pageSize, pageSize)

    .build()
    .getResultList();
```

---

## 9. Best Practices (모범 사례)

```
1. ✅ Always use Text Blocks (""") for readability
   가독성을 위해 항상 텍스트 블록 사용

2. ✅ Start WHERE clause with "1=1" for optional conditions
   선택적 조건을 위해 WHERE 절을 "1=1"로 시작

3. ✅ Use meaningful placeholder names {{=cond_*}}
   의미 있는 플레이스홀더명 사용

4. ✅ Always bind condition and parameter together
   조건과 파라미터를 항상 함께 바인딩

5. ✅ Use LikeMode for string matching
   문자열 매칭에는 LikeMode 사용

6. ✅ Validate user input before using
   사용 전에 사용자 입력 검증

7. ❌ NEVER concatenate user input into SQL
   SQL에 사용자 입력을 연결하지 말 것

8. ❌ NEVER skip bindParameter for values
   값을 bindParameter 없이 사용하지 말 것

9. ✅ Use Registry Pattern for frequently used queries
   자주 사용되는 쿼리는 Registry Pattern 사용

10. ✅ Test pagination with boundary values
    경계 값으로 페이징 테스트
```

---

## 10. Performance Tips (성능 팁)

```
1. Use offset-based pagination for better performance
   성능을 위해 오프셋 기반 페이징 사용

2. Avoid unnecessary JOINs in optional conditions
   선택적 조건에서 불필요한 JOIN 피하기

3. Use appropriate index on WHERE clause fields
   WHERE 절 필드에 적절한 인덱스 설정

4. Cache frequently used validators in Registry Mode
   자주 사용되는 검증기는 Registry 모드로 캐싱

5. Monitor query performance with EXPLAIN
   EXPLAIN으로 쿼리 성능 모니터링

6. Avoid large offset values for pagination
   페이징 시 큰 오프셋 값 피하기
   (Use keyset pagination for large datasets)
   (대용량 데이터셋에는 keyset pagination 사용)
```

---

## 11. Troubleshooting (문제 해결)

| Problem                  | Cause                    | Solution                                  |
| ------------------------ | ------------------------ | ----------------------------------------- |
| Placeholder not replaced | Typo in placeholder name | Check {{=placeholder_name}} spelling      |
| Parameter null           | bindParameter not called | Always pair bindClause with bindParameter |
| SQL Injection warning    | User input in clause     | Use hardcoded clause strings only         |
| Unexpected query result  | Wrong condition logic    | Test condition evaluation separately      |
| Pagination returns empty | Wrong offset/limit       | Verify page number and page size          |

---

## 12. Integration with S2Validator & S2Copier (다른 모듈과 통합)

S2Jpql can be combined with S2Validator and S2Copier for complete data flow:

<br>S2Jpql은 S2Validator와 S2Copier와 함께 사용하여 완전한 데이터 흐름을 구성할 수 있습니다:

```java
// 1. Validate input with S2Validator
// 입력 검증
ProductSearchRequest request = new ProductSearchRequest(...);
S2Validator.of(request)
    .field("pageSize").rule(S2RuleType.MAX_VALUE, 100)
    .validate();

// 2. Query database with S2Jpql
// 데이터베이스 조회
List<Product> dbResults = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .bindClause("cond_name", request.getName() != null,
                "AND p.name LIKE :name")
        .bindParameter("name", request.getName(), LikeMode.ANYWHERE)
    .limit(request.getPage() * 20, 20)
    .build()
    .getResultList();

// 3. Transform results with S2Copier
// 결과 변환
List<ProductDto> dtoResults = dbResults.stream()
    .map(product -> S2Copier.from(product)
        .map("id", "productId")
        .to(ProductDto.class))
    .collect(Collectors.toList());
```
