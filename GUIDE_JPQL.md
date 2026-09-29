# S2Jpql: Secure Dynamic Query Builder Guide 🔎

🌐 **English** | [한국어](GUIDE_JPQL.ko.md)

> **Secure, Template-Based Dynamic Query Generation with Zero SQL Injection Risk**

Utilize Java Text Blocks (`"""`) for cleaner JPQL. `bindClause()` handles conditional clause binding, and `bindParameter()` exclusively handles parameter value binding for SQL injection prevention.


---

## 1. Core Concepts

### 1-1. The Two-Method Binding Strategy

S2Jpql uses a two-method approach to prevent SQL injection:

1. **`bindClause()`**: Conditionally includes hardcoded SQL clauses

2. **`bindParameter()`**: Safely binds parameter values

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

## 2. Basic Usage

### 2-1. Simple Query with Conditional Clauses

```java
// JPQL template with placeholders

String jpql = """
    SELECT p
    FROM Product p
    WHERE 1=1
        {{=cond_name}}
        {{=cond_price}}
    {{=sort}}
""";

// Build and execute query with conditions

List<Product> products = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)

    // Bind conditional clause
    .bindClause("cond_name", name, "AND p.name LIKE :name")
        // Bind the parameter value
        .bindParameter("name", name, LikeMode.ANYWHERE)

    // Another conditional clause
    .bindClause("cond_price", price, "AND p.price >= :price")
        .bindParameter("price", price)

    // Conditional order by
    .bindOrderBy("sort", sort)

    .build()
    .getResultList();
```

### 2-2. Conditional Binding

```java
// Only include clause if condition is true

// Case 1: Null check
String jpql = "SELECT p FROM Product p WHERE 1=1 {{=cond_name}}";

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .bindClause("cond_name", name != null && !name.isEmpty(),
                "AND p.name LIKE :name")
        .bindParameter("name", name, LikeMode.ANYWHERE)
    .build()
    .getResultList();

// Case 2: Zero-based check
.bindClause("cond_price", price > 0, "AND p.price >= :price")
    .bindParameter("price", price)

// Case 3: Collection check
.bindClause("cond_status", !statuses.isEmpty(),
            "AND p.status IN :statuses")
    .bindParameter("statuses", statuses)
```

---

## 3. Parameter Binding Methods

### 3-1. Basic Value Binding

```java
// Bind simple values

String jpql = "SELECT p FROM Product p WHERE p.id = :id AND p.status = :status";

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .bindParameter("id", 123)
    .bindParameter("status", ProductStatus.ACTIVE)
    .build()
    .getResultList();
```

### 3-2. String Matching with LikeMode

```java
// Different LIKE patterns

String jpql = "SELECT p FROM Product p WHERE p.name LIKE :name";

// Pattern 1: ANYWHERE (contains) - "%keyword%"
.bindParameter("name", "laptop", LikeMode.ANYWHERE)

// Pattern 2: START (prefix) - "keyword%"
.bindParameter("name", "laptop", LikeMode.START)

// Pattern 3: END (suffix) - "%keyword"
.bindParameter("name", "pro", LikeMode.END)

// Pattern 4: EXACT (no wildcards) - "keyword"
.bindParameter("name", "laptop", LikeMode.EXACT)
```

### 3-3. Collection Binding

```java
// Bind lists for IN clause

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

## 4. Pagination

### 4-1. Simple Pagination

```java
// Offset-based pagination

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

### 4-2. Conditional Pagination

```java
// Apply pagination only when condition is true

String jpql = "SELECT p FROM Product p WHERE 1=1 {{=cond_name}}";

boolean shouldPaginate = pageSize > 0 && pageNumber >= 0;

List<Product> results = S2Jpql.from(em)
    .type(Product.class)
    .query(jpql)
    .bindClause("cond_name", keyword != null,
                "AND p.name LIKE :name")
        .bindParameter("name", keyword, LikeMode.ANYWHERE)

    // Only apply pagination if condition is true
    .limit(shouldPaginate, pageNumber * pageSize, pageSize)

    .build()
    .getResultList();
```

### 4-3. Offset and Limit Methods

```java
// Method signatures

// Unconditional pagination
.limit(offset, limit)

// Conditional pagination
.limit(condition, offset, limit)

// Example
.limit(true, 0, 20)        // Always paginate
.limit(keyword != null, 0, 20)  // Paginate only if keyword exists
```

---

## 5. Ordering

### 5-1. Conditional ORDER BY

```java
// Dynamic ORDER BY based on user input

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
    // Bind conditional ORDER BY
    .bindOrderBy("sort", sortBy)
    .build()
    .getResultList();
```

### 5-2. Supported Sort Values

```java
// Order by field name (e.g., "name" → "ORDER BY p.name ASC")
.bindOrderBy("sort", "name")      // → ORDER BY p.name ASC
.bindOrderBy("sort", "-name")     // → ORDER BY p.name DESC

// Multiple fields separated by comma
.bindOrderBy("sort", "price,-date")  // → ORDER BY p.price ASC, p.date DESC
```

---

## 6. Complete Example: Search & Pagination

```java
// Product search with dynamic conditions and pagination

@GetMapping("/products")
public String searchProducts(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer minPrice,
        @RequestParam(required = false) Integer maxPrice,
        @RequestParam(defaultValue = "name") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        Model model) {

    // Build JPQL template
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

    // Execute query with all conditions
    List<Product> results = S2Jpql.from(em)
        .type(Product.class)
        .query(jpql)

        // Optional name filter
        .bindClause("cond_name", name != null && !name.isEmpty(),
                    "AND p.name LIKE :name")
            .bindParameter("name", name, LikeMode.ANYWHERE)

        // Optional minimum price filter
        .bindClause("cond_min_price", minPrice != null && minPrice > 0,
                    "AND p.price >= :minPrice")
            .bindParameter("minPrice", minPrice)

        // Optional maximum price filter
        .bindClause("cond_max_price", maxPrice != null && maxPrice > 0,
                    "AND p.price <= :maxPrice")
            .bindParameter("maxPrice", maxPrice)

        // Dynamic ordering
        .bindOrderBy("sort", sort)

        // Pagination
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

## 7. SQL Injection Prevention

### ⚠️ Critical Security Rules

> [!WARNING]
> **ARCHITECTURE:** The `bindClause()` method is **EXCLUSIVELY** for binding dynamic SQL clauses conditionally. The `bindParameter()` method is **EXCLUSIVELY** for binding dynamic parameter values. This separation is critical to prevent SQL injection.
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

### 7-1. SAFE Usage

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

### 7-2. DANGEROUS Usage

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

## 8. Advanced Features

### 8-1. Multiple Conditions

```java
// Build complex queries with multiple optional conditions

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

### 8-2. JOIN Conditions

```java
// Query with JOINs and multiple conditions

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

## 9. Best Practices

```
1. ✅ Always use Text Blocks (""") for readability

2. ✅ Start WHERE clause with "1=1" for optional conditions

3. ✅ Use meaningful placeholder names {{=cond_*}}

4. ✅ Always bind condition and parameter together

5. ✅ Use LikeMode for string matching

6. ✅ Validate user input before using

7. ❌ NEVER concatenate user input into SQL

8. ❌ NEVER skip bindParameter for values

9. ✅ Put frequently used queries in reusable query-building methods

10. ✅ Test pagination with boundary values
    경계 값으로 페이징 테스트
```

---

## 10. Performance Tips

```
1. Use offset-based pagination for better performance

2. Avoid unnecessary JOINs in optional conditions

3. Use appropriate index on WHERE clause fields

4. Reuse query-building methods instead of duplicating conditions

5. Monitor query performance with EXPLAIN

6. Avoid large offset values for pagination
   (Use keyset pagination for large datasets)
  
```

---

## 11. Troubleshooting

| Problem                  | Cause                    | Solution                                  |
| ------------------------ | ------------------------ | ----------------------------------------- |
| Placeholder not replaced | Typo in placeholder name | Check {{=placeholder_name}} spelling      |
| Parameter null           | bindParameter not called | Always pair bindClause with bindParameter |
| SQL Injection warning    | User input in clause     | Use hardcoded clause strings only         |
| Unexpected query result  | Wrong condition logic    | Test condition evaluation separately      |
| Pagination returns empty | Wrong offset/limit       | Verify page number and page size          |

---

## 12. Integration with S2Validator & S2Copier

S2Jpql can be combined with S2Validator and S2Copier for complete data flow:


```java
// 1. Validate input with S2Validator
ProductSearchRequest request = new ProductSearchRequest(...);
S2Validator.of(request)
    .field("pageSize").rule(S2RuleType.MAX_VALUE, 100)
    .validate();

// 2. Query database with S2Jpql
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
List<ProductDto> dtoResults = dbResults.stream()
    .map(product -> S2Copier.from(product)
        .map("id", "productId")
        .to(ProductDto.class))
    .collect(Collectors.toList());
```
