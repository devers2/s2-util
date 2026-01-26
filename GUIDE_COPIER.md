# S2Copier: Zero-Reflection Copy Guide (고성능 객체 복사 가이드) 📋

> **Optimized with `MethodHandle` for maximum throughput between Entities and DTOs.**
> <br>**`MethodHandle`로 최적화되어 리플렉션 고유의 병목 없이 데이터를 매핑합니다.**

---

## 1. Quick Start (빠른 시작)

### 1-1. Basic Copy (기본 복사)

```java
// [English] Simple object copy between DTO and Entity
// [한국어] DTO와 Entity 간의 간단한 복사

User entity = userRepository.findById(1L).orElseThrow();
UserDto dto = S2Copier.from(entity).to(UserDto.class);
```

### 1-2. Advanced Features (고급 기능)

```java
// [English] Advanced Mapping, Exclusion, and Partial Update
// [한국어] 고급 매핑, 제외, 부분 업데이트

S2Copier.from(requestDto)
    .exclude("id", "secret")              // Field exclusion (필드 제외)
    .map("nickName", "displayName")       // Property name sync (필드명 매핑)
    .ignoreNulls()                        // Supports selective updates (null 무시, PATCH 지원)
    .to(existingEntity);                  // Naturally triggers JPA Dirty Checking
```

---

## 2. Core Features (핵심 기능)

### 2-1. Field Exclusion (필드 제외)

```java
// [English] Exclude sensitive or system fields from copying
// [한국어] 민감한 필드나 시스템 필드 제외

User user = S2Copier.from(sourceUser)
    .exclude("id", "password", "secret")
    .to(User.class);

// User.id, password, secret are NOT copied
// 복사되지 않음: id, password, secret
```

### 2-2. Field Mapping (필드명 매핑)

```java
// [English] Map source field names to different target field names
// [한국어] 원본 필드명과 대상 필드명이 다를 때 매핑

UserDto dto = S2Copier.from(entity)
    .map("id", "userId")                 // entity.id → dto.userId
    .map("name", "fullName")             // entity.name → dto.fullName
    .map("address.city", "location")     // entity.address.city → dto.location
    .to(UserDto.class);

// Now entity.id → dto.userId
// entity.id는 dto.userId로 매핑됨
```

### 2-3. Null-Aware Copying (null 무시)

```java
// [English] Supports partial updates (PATCH semantics)
// [한국어] 부분 업데이트 지원 (PATCH 의미론)

User existingUser = userRepository.findById(1L).orElseThrow();
UserUpdateDto updateDto = new UserUpdateDto("NewName", null, "newemail@example.com");

S2Copier.from(updateDto)
    .ignoreNulls()        // Only non-null values are copied
    .to(existingUser);    // Null values are NOT copied

// Result: name changed, age unchanged (was null), email changed
// 결과: 이름만 변경, 나이는 유지 (null이었음), 이메일 변경
```

### 2-4. Map ↔ DTO Conversion (Map과 DTO 상호 변환)

```java
// [English] Copy from DTO to Map
// [한국어] DTO에서 Map으로 복사

UserDto dto = new UserDto("user001", "Alice", 30);

Map<String, Object> map = S2Copier.from(dto)
    .map("id", "userId")              // Field mapping still works
    .to(new HashMap<>());

// map: {userId: "user001", name: "Alice", age: 30}

// [English] Copy from Map to DTO
// [한국어] Map에서 DTO로 복사

Map<String, Object> sourceMap = Map.of(
    "userId", "user002",
    "name", "Bob",
    "age", 25
);

UserDto result = S2Copier.from(sourceMap)
    .map("userId", "id")              // Reverse mapping
    .to(UserDto.class);

// result.id = "user002", result.name = "Bob", result.age = 25
```

---

## 3. Deep Copy with Circular Reference Protection (깊은 복사와 순환참조 방지)

S2Copier supports **deep copy** mode to create independent copies of nested objects, collections, and maps. Circular references are automatically detected and handled to prevent infinite loops.

<br>S2Copier는 중첩 객체와 컬렉션의 **깊은 복사**를 지원하여 독립적인 복사본을 생성합니다. 순환참조는 자동으로 감지되어 무한 루프를 방지합니다.

### 3-1. Deep Copy All Fields (모든 필드 깊은 복사)

```java
// [English] Deep copy all fields including nested objects
// [한국어] 중첩 객체를 포함한 모든 필드 깊은 복사

User userCopy = S2Copier.from(originalUser)
    .deep()                           // Enable deep copy for all fields
    .to(User.class);

// Modifications to nested objects in the original don't affect the copy
originalUser.getAddress().setCity("NewCity");
assert userCopy.getAddress().getCity().equals("Seoul"); // ✓ Not affected

// 원본의 중첩 객체 수정이 복사본에 영향을 주지 않음
```

### 3-2. Deep Copy Specific Fields (특정 필드만 깊은 복사)

```java
// [English] Mixed shallow and deep copy mode (performance optimization)
// [한국어] 혼합 모드 (성능 최적화)

UserDetail userDetail = S2Copier.from(source)
    .deepOnly("tags", "addresses")    // Only these fields are deeply copied
    .to(UserDetail.class);

// Simple string fields are shallow copied (reference copy)
// Collection fields are deep copied (independent copy)
// 단순 문자열은 얕은 복사, 컬렉션은 깊은 복사
```

### 3-3. How Deep Copy Works (깊은 복사 동작 방식)

#### Shallow Copy (얕은 복사) - Default behavior

```java
// [English] Default: shallow copy shares references
// [한국어] 기본: 얕은 복사는 참조를 공유

User original = new User("Alice", new Address("Seoul"));
User copy = S2Copier.from(original).to(User.class);

// Shallow copy: Address object is shared
assert copy.getAddress() == original.getAddress(); // Same reference!

original.getAddress().setCity("Busan");
assert copy.getAddress().getCity().equals("Busan"); // ✗ Copy affected

// 복사본이 영향을 받음
```

#### Deep Copy (깊은 복사) - With deep() method

```java
// [English] With deep(): independent copy of nested objects
// [한국어] deep() 사용: 중첩 객체 독립적 복사

User original = new User("Alice", new Address("Seoul"));
User copy = S2Copier.from(original).deep().to(User.class);

// Deep copy: Address object is independently copied
assert copy.getAddress() != original.getAddress(); // Different object

original.getAddress().setCity("Busan");
assert copy.getAddress().getCity().equals("Seoul"); // ✓ Copy not affected

// 복사본이 영향을 받지 않음
```

### 3-4. Circular Reference Handling (순환참조 처리)

#### The Problem (문제점)

```java
// [English] Circular reference creates infinite loop
// [한국어] 순환참조는 무한 루프 생성

ClassA a = new ClassA("A");
ClassB b = new ClassB("B");
a.setRef(b);      // A points to B
b.setRef(a);      // B points back to A (circular!)

// Without proper handling, deep copy would loop infinitely
// 적절한 처리 없으면 무한 루프 발생
```

#### The Solution (해결책)

S2Copier uses **IdentityHashMap** to track already-copied objects:

1. **Visited Tracking**: Store copied objects in `IdentityHashMap<Object, Object>`
2. **Cycle Detection**: Before copying, check if object is already in the map
3. **Reference Reuse**: Return already-copied instance instead of re-copying

<br>S2Copier는 **IdentityHashMap**을 사용하여 복사된 객체를 추적합니다:

1. **방문 추적**: 복사된 객체를 IdentityHashMap에 저장
2. **순환참조 감지**: 복사 전에 맵에 있는지 확인
3. **참조 재사용**: 이미 복사된 인스턴스 반환

```java
// [English] Graceful handling of circular references
// [한국어] 순환참조 자동 처리

ClassA aCopy = S2Copier.from(a).deep().to(ClassA.class);

// aCopy and aCopy.getRef().getRef() are properly linked
// without infinite recursion
assert aCopy != a;
assert aCopy.getRef() != b;
assert aCopy.getRef().getRef() == aCopy; // Circular reference preserved correctly

// 순환참조가 올바르게 유지됨 (무한 루프 없음)
```

### 3-5. Safety Mechanism: MAX_DEPTH Limit (안전 메커니즘: MAX_DEPTH 제한)

A **MAX_DEPTH limit (100)** provides a secondary safeguard against unexpectedly deep object graphs:

<br>**MAX_DEPTH 제한(100)**은 예상치 못한 깊은 객체 그래프에 대한 2차 보호를 제공합니다:

```java
// [English] If nesting exceeds MAX_DEPTH, exception is thrown
// [한국어] 중첩이 MAX_DEPTH를 초과하면 예외 발생

try {
    ClassA copy = S2Copier.from(deeplyNested).deep().to(ClassA.class);
} catch (RuntimeException e) {
    if (e.getMessage().contains("Maximum deep copy depth exceeded")) {
        // Handle MAX_DEPTH exceeded (MAX_DEPTH 초과 처리)
        System.out.println("Object nesting too deep");
    }
}
```

---

## 4. Supported Types for Deep Copy (깊은 복사 지원 타입)

### 4-1. Primitive Wrappers (원시 래퍼)

```java
// [English] Immutable types: returned as-is
// [한국어] 불변 타입: 그대로 반환 (복사하지 않음)

Integer, Long, Double, Float, Boolean, Byte, Short, Character
String
```

### 4-2. Collections (컬렉션)

```java
// [English] Recursively deep copy each element
// [한국어] 각 요소를 재귀적으로 깊게 복사

List, ArrayList, LinkedList
Set, HashSet, LinkedHashSet
Iterable implementations
```

### 4-3. Maps (맵)

```java
// [English] Recursively deep copy each value
// [한국어] 각 값을 재귀적으로 깊게 복사

HashMap, LinkedHashMap
TreeMap, ConcurrentHashMap
Map implementations
```

### 4-4. Custom Objects (사용자 정의 클래스)

```java
// [English] Any class with:
// [한국어] 다음 조건을 만족하는 클래스:

// 1. Accessible fields (공개 필드 또는 S2Cache 지원)
// 2. A no-argument constructor (no-arg 생성자)
// 3. Proper getters/setters (S2Util 호환 getter/setter)

public class User {
    private String name;
    private Address address;

    public User() {}  // Required: no-arg constructor (필수)
    public User(String name, Address address) { ... }

    // Getters/setters for S2Copier
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}
```

---

## 5. Advanced Usage (고급 사용법)

### 5-1. Combining All Features (모든 기능 결합)

```java
// [English] Deep copy + field mapping + exclusion + null handling
// [한국어] 깊은 복사 + 필드 매핑 + 제외 + null 처리

User copy = S2Copier.from(source)
    .deep()                       // Deep copy mode
    .deepOnly("preferences")      // Only deep copy this field
    .map("id", "userId")          // Field name mapping
    .exclude("secret")            // Exclude sensitive fields
    .ignoreNulls()                // Ignore null values
    .to(User.class);
```

### 5-2. Map Target with Deep Copy (Map 대상 깊은 복사)

```java
// [English] Deep copy from DTO to Map
// [한국어] DTO에서 Map으로 깊은 복사

UserDto dto = new UserDto("user001", "Alice", 30, new Address("Seoul"));

Map<String, Object> deepMap = S2Copier.from(dto)
    .deep()
    .to(new HashMap<>());

// All nested objects are independently copied
// 모든 중첩 객체가 독립적으로 복사됨
```

### 5-3. Fluent Chaining (유연한 체이닝)

```java
// [English] All methods return the copier for easy chaining
// [한국어] 모든 메서드는 copier를 반환하여 체이닝 가능

UserDto result = S2Copier.from(source)
    .deep()
    .deepOnly("tags", "addresses")
    .exclude("id")
    .map("userId", "id")
    .ignoreNulls()
    .to(UserDto.class);  // Returns UserDto instance

// [English] Can also instantiate target class
// [한국어] 대상 클래스 인스턴스 생성도 가능

Map<String, Object> map = S2Copier.from(source)
    .deep()
    .to(new HashMap<>());  // Provides target instance
```

---

## 6. Common Patterns (일반적인 패턴)

### 6-1. Safe Cache/Buffer (안전한 캐시/버퍼)

```java
// [English] Ensure modifications to returned copy don't affect original
// [한국어] 반환된 복사본 수정이 원본에 영향을 주지 않도록 보장

public class UserService {
    private User cachedUser;

    public User getUserCopy() {
        return S2Copier.from(cachedUser)
            .deepOnly("preferences", "settings")
            .to(User.class);
    }
}
```

### 6-2. Snapshot for Auditing (감사용 스냅샷)

```java
// [English] Create complete, independent snapshot for audit trail
// [한국어] 감사 추적을 위한 완전하고 독립적인 스냅샷 생성

public void auditUserChange(User original, User modified) {
    User snapshot = S2Copier.from(original)
        .deep()
        .to(User.class);

    auditLog.record(snapshot, modified);
}
```

### 6-3. Form Submission Safety (폼 제출 안전성)

```java
// [English] Prevent accidental modifications to original entity
// [한국어] 원본 엔티티의 의도하지 않은 수정 방지

@PostMapping("/users/{id}")
public ResponseEntity<?> updateUser(
        @PathVariable Long id,
        @RequestBody UserDto dto) {

    User entity = userRepository.findById(id).orElseThrow();

    // Create a safe copy before applying changes
    User originalState = S2Copier.from(entity)
        .deepOnly("roles", "permissions")
        .to(User.class);

    // Apply updates
    S2Copier.from(dto).to(entity);

    // Log original state
    auditLog.record(originalState, entity);

    return ResponseEntity.ok(userRepository.save(entity));
}
```

### 6-4. PATCH Operations (부분 업데이트)

```java
// [English] PATCH: Only update provided fields
// [한국어] PATCH: 제공된 필드만 업데이트

@PatchMapping("/users/{id}")
public ResponseEntity<?> patchUser(
        @PathVariable Long id,
        @RequestBody UserPatchDto patchDto) {

    User entity = userRepository.findById(id).orElseThrow();

    // Apply PATCH semantics: null values are ignored
    S2Copier.from(patchDto)
        .ignoreNulls()
        .to(entity);

    return ResponseEntity.ok(userRepository.save(entity));
}
```

---

## 7. Performance Considerations (성능 고려사항)

### 7-1. When to Use Deep Copy (깊은 복사를 사용해야 할 때)

```
✅ Independent copies of nested objects needed
✅ Modifications to copy shouldn't affect original
✅ Working with temporary copies for calculations
✅ Creating snapshots for auditing

✅ 중첩 객체의 독립적인 복사본 필요
✅ 복사본 수정이 원본에 영향을 주면 안 될 때
✅ 계산용 임시 복사본 작업
✅ 감사용 스냅샷 생성
```

### 7-2. When to Avoid Deep Copy (깊은 복사를 피해야 할 때)

```
❌ Shallow references are sufficient (얕은 참조로 충분할 때)
❌ Dealing with extremely large object graphs (매우 큰 객체 그래프)
❌ In tight loops or high-frequency operations (타이트 루프, 고빈도 작업)
❌ When circular references are very deep (> 100 levels) (순환참조가 매우 깊을 때)
```

### 7-3. Optimization Tips (최적화 팁)

```java
// [English] ✓ Better: Only deep copy what you need
// [한국어] ✓ 좋음: 필요한 것만 깊게 복사

copy = S2Copier.from(source)
    .deepOnly("collectionField1", "collectionField2")  // Selective
    .to(Target.class);

// [English] ✗ Less optimal: Deep copy everything
// [한국어] ✗ 덜 최적: 모든 필드 깊게 복사

copy = S2Copier.from(source)
    .deep()  // All fields, including simple strings
    .to(Target.class);

// [English] ✓ Better: Don't deep copy in loops
// [한국어] ✓ 좋음: 루프에서 깊은 복사하지 않기

for (int i = 0; i < 1000; i++) {
    // Shallow copy is much faster
    Result temp = S2Copier.from(template).to(Result.class);
}

// [English] ✗ Not recommended: Deep copy in loops
// [한국어] ✗ 권장하지 않음: 루프에서 깊은 복사

for (int i = 0; i < 1000; i++) {
    // Deep copy overhead multiplied 1000 times!
    Result temp = S2Copier.from(template).deep().to(Result.class);
}
```

---

## 8. Troubleshooting (문제 해결)

### 8-1. "Unable to instantiate target class"

```
Error: RuntimeException - Failed to instantiate target class

[English] Cause: The class doesn't have a no-argument constructor
[한국어] 원인: no-argument 생성자가 없음

Solution / 해결책:
Add a public no-arg constructor
public MyClass() {}
```

### 8-2. "Maximum deep copy depth exceeded"

```
Error: RuntimeException - Maximum deep copy depth exceeded (MAX_DEPTH=100)

[English] Cause: Object nesting exceeds 100 levels
[한국어] 원인: 객체 중첩이 100 레벨 초과

Solution / 해결책:
Use deepOnly() instead of deep()
copy = S2Copier.from(source)
    .deepOnly("otherField")  // Skip the deeply nested one
    .to(Target.class);
```

### 8-3. Circular references causing issues

```
Error: Various errors during deep copy

[English] Cause: Circular reference not properly handled
[한국어] 원인: 순환참조가 제대로 처리되지 않음

Solution / 해결책:
This should be automatic, but check for:
1. Ensure IdentityHashMap is being used
2. Check MAX_DEPTH protection is in place
3. Consider using deepOnly() for critical fields

자동으로 처리되어야 하지만 확인사항:
1. IdentityHashMap 사용 확인
2. MAX_DEPTH 보호 확인
3. 중요 필드에만 deepOnly() 사용 고려
```

---

## 9. Best Practices (모범 사례)

```
1. ✅ Use deepOnly() for performance
   성능을 위해 deepOnly() 사용

2. ✅ Test with circular references
   순환참조로 테스트

3. ✅ Monitor performance impact
   성능 영향 모니터링

4. ✅ Document mutable fields
   변경 가능한 필드 문서화

5. ❌ Avoid deep copy in hot paths
   핫 경로에서 깊은 복사 회피

6. ❌ Don't deep copy in tight loops
   타이트 루프에서 깊은 복사 금지

7. ✅ Chain methods for readability
   가독성을 위해 메서드 체이닝

8. ✅ Use field mapping for flexibility
   유연성을 위해 필드 매핑 사용
```

---

## 10. Comparison Matrix (기능 비교 매트릭스)

| Feature            |  Shallow Copy  |    Deep Copy    | Deep Copy (Selective) |
| ------------------ | :------------: | :-------------: | :-------------------: |
| **Speed**          |     ⚡⚡⚡     |      ⚡⚡       |        ⚡⚡⚡         |
| **Memory**         |       🟢       |       🟡        |          🟢           |
| **Nested Objects** |   🔗 Shared    | ✅ Independent  |   ✅ Independent\*    |
| **Collections**    |   🔗 Shared    | ✅ Independent  |   ✅ Independent\*    |
| **Circular Refs**  | ⚠️ Not handled | ✅ Auto handled |    ✅ Auto handled    |
| **Field Mapping**  |       ✅       |       ✅        |          ✅           |
| **Use Case**       |  Simple copy   | Safety critical |       Balanced        |

\* = Only specified fields

| 기능          |  얕은 복사   |  깊은 복사   | 선택적 깊은 복사 |
| ------------- | :----------: | :----------: | :--------------: |
| **속도**      |    ⚡⚡⚡    |     ⚡⚡     |      ⚡⚡⚡      |
| **메모리**    |      🟢      |      🟡      |        🟢        |
| **중첩 객체** |   🔗 공유    |   ✅ 독립    |    ✅ 독립\*     |
| **컬렉션**    |   🔗 공유    |   ✅ 독립    |    ✅ 독립\*     |
| **순환참조**  | ⚠️ 처리 안함 | ✅ 자동 처리 |   ✅ 자동 처리   |
| **필드 매핑** |      ✅      |      ✅      |        ✅        |
| **사용 사례** | 간단한 복사  | 안전성 중요  |  균형 잡힌 방식  |

\* = 지정된 필드만
