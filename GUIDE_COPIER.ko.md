# S2Copier: 고성능 객체 복사 가이드 📋

🌐 [English](GUIDE_COPIER.md) | **한국어**

> **`MethodHandle`로 최적화되어 리플렉션 고유의 병목 없이 데이터를 매핑합니다.**

---

## 1. 빠른 시작

### 1-1. 기본 복사

```java
// DTO와 Entity 간의 간단한 복사

User entity = userRepository.findById(1L).orElseThrow();
UserDto dto = S2Copier.from(entity).to(UserDto.class);
```

### 1-2. 고급 기능

```java
// 고급 매핑, 제외, 부분 업데이트

S2Copier.from(requestDto)
    .exclude("id", "secret")              // 필드 제외
    .map("nickName", "displayName")       // 필드명 매핑
    .ignoreNulls()                        // null 무시, PATCH 지원
    .to(existingEntity);                  // JPA Dirty Checking 자동 트리거
```

---

## 2. 핵심 기능

### 2-1. 필드 제외

```java
// 민감한 필드나 시스템 필드 제외

User user = S2Copier.from(sourceUser)
    .exclude("id", "password", "secret")
    .to(User.class);

// 복사되지 않음: id, password, secret
```

### 2-2. 필드명 매핑

```java
// 원본 필드명과 대상 필드명이 다를 때 매핑

UserDto dto = S2Copier.from(entity)
    .map("id", "userId")                 // entity.id → dto.userId
    .map("name", "fullName")             // entity.name → dto.fullName
    .map("address.city", "location")     // entity.address.city → dto.location
    .to(UserDto.class);

// entity.id는 dto.userId로 매핑됨
```

### 2-3. null 무시

```java
// 부분 업데이트 지원 (PATCH 의미론)

User existingUser = userRepository.findById(1L).orElseThrow();
UserUpdateDto updateDto = new UserUpdateDto("NewName", null, "newemail@example.com");

S2Copier.from(updateDto)
    .ignoreNulls()        // Only non-null values are copied
    .to(existingUser);    // Null values are NOT copied

// 결과: 이름만 변경, 나이는 유지 (null이었음), 이메일 변경
```

### 2-4. Map과 DTO 상호 변환

```java
// DTO에서 Map으로 복사

UserDto dto = new UserDto("user001", "Alice", 30);

Map<String, Object> map = S2Copier.from(dto)
    .map("id", "userId")              // Field mapping still works
    .to(new HashMap<>());

// map: {userId: "user001", name: "Alice", age: 30}

// Map에서 DTO로 복사

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

## 3. 깊은 복사와 순환참조 방지

S2Copier는 중첩 객체와 컬렉션의 **깊은 복사**를 지원하여 독립적인 복사본을 생성합니다. 순환참조는 자동으로 감지되어 무한 루프를 방지합니다.

### 3-1. 모든 필드 깊은 복사

```java
// 중첩 객체를 포함한 모든 필드 깊은 복사

User userCopy = S2Copier.from(originalUser)
    .deep()                           // Enable deep copy for all fields
    .to(User.class);

originalUser.getAddress().setCity("NewCity");
assert userCopy.getAddress().getCity().equals("Seoul"); // ✓ 복사본 영향 없음
// 원본의 중첩 객체 수정이 복사본에 영향을 주지 않음
```

### 3-2. 특정 필드만 깊은 복사

```java
// 혼합 모드 (성능 최적화)

UserDetail userDetail = S2Copier.from(source)
    .deepOnly("tags", "addresses")    // Only these fields are deeply copied
    .to(UserDetail.class);

// 단순 문자열은 얕은 복사, 컬렉션은 깊은 복사
```

### 3-3. 깊은 복사 동작 방식

#### 얕은 복사

```java
// 기본: 얕은 복사는 참조를 공유

User original = new User("Alice", new Address("Seoul"));
User copy = S2Copier.from(original).to(User.class);

// 얕은 복사: Address 객체 공유됨
assert copy.getAddress() == original.getAddress();

original.getAddress().setCity("Busan");
assert copy.getAddress().getCity().equals("Busan"); // ✗ 복사본이 영향을 받음
```

#### 깊은 복사

```java
// deep() 사용: 중첩 객체 독립적 복사

User original = new User("Alice", new Address("Seoul"));
User copy = S2Copier.from(original).deep().to(User.class);

// 깊은 복사: Address 객체가 독립적으로 복사됨
assert copy.getAddress() != original.getAddress();

original.getAddress().setCity("Busan");
assert copy.getAddress().getCity().equals("Seoul"); // ✓ 복사본이 영향을 받지 않음
```

### 3-4. 순환참조 처리

#### 문제점

```java
// 순환참조는 무한 루프 생성

ClassA a = new ClassA("A");
ClassB b = new ClassB("B");
a.setRef(b);      // A points to B
b.setRef(a);      // B points back to A (circular!)

// 적절한 처리 없으면 무한 루프 발생
```

#### 해결책

S2Copier는 **IdentityHashMap**을 사용하여 복사된 객체를 추적합니다:

1. **방문 추적**: 복사된 객체를 IdentityHashMap에 저장
2. **순환참조 감지**: 복사 전에 맵에 있는지 확인
3. **참조 재사용**: 이미 복사된 인스턴스 반환

```java
// 순환참조 자동 처리

ClassA aCopy = S2Copier.from(a).deep().to(ClassA.class);

// 순환참조가 올바르게 유지됨 (무한 루프 없음)
assert aCopy != a;
assert aCopy.getRef() != b;
assert aCopy.getRef().getRef() == aCopy;
```

### 3-5. 안전 메커니즘: MAX_DEPTH 제한

**MAX_DEPTH 제한(100)**은 예상치 못한 깊은 객체 그래프에 대한 2차 보호를 제공합니다:

```java
// 중첩이 MAX_DEPTH를 초과하면 예외 발생

try {
    ClassA copy = S2Copier.from(deeplyNested).deep().to(ClassA.class);
} catch (RuntimeException e) {
    if (e.getMessage().contains("Maximum deep copy depth exceeded")) {
        // MAX_DEPTH 초과 처리
        System.out.println("Object nesting too deep");
    }
}
```

---

## 4. 깊은 복사 지원 타입

### 4-1. 원시 래퍼

```java
// 불변 타입: 그대로 반환 (복사하지 않음)

Integer, Long, Double, Float, Boolean, Byte, Short, Character
String
```

### 4-2. 컬렉션

```java
// 각 요소를 재귀적으로 깊게 복사

List, ArrayList, LinkedList
Set, HashSet, LinkedHashSet
Iterable implementations
```

### 4-3. 맵

```java
// 각 값을 재귀적으로 깊게 복사

HashMap, LinkedHashMap
TreeMap, ConcurrentHashMap
Map implementations
```

### 4-4. 사용자 정의 클래스

```java
// 다음 조건을 만족하는 클래스:

// 1. 공개 필드 또는 S2Cache 지원
// 2. no-arg 생성자
// 3. S2Util 호환 getter/setter

public class User {
    private String name;
    private Address address;

    public User() {}  // 필수: no-arg 생성자
    public User(String name, Address address) { ... }

    // Getters/setters for S2Copier
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}
```

---

## 5. 고급 사용법

### 5-1. 모든 기능 결합

```java
// 깊은 복사 + 필드 매핑 + 제외 + null 처리

User copy = S2Copier.from(source)
    .deep()                       // 깊은 복사 모드
    .deepOnly("preferences")      // 이 필드만 깊은 복사
    .map("id", "userId")          // 필드명 매핑
    .exclude("secret")            // 민감한 필드 제외
    .ignoreNulls()                // null 무시
    .to(User.class);
```

### 5-2. Map 대상 깊은 복사

```java
// DTO에서 Map으로 깊은 복사

UserDto dto = new UserDto("user001", "Alice", 30, new Address("Seoul"));

Map<String, Object> deepMap = S2Copier.from(dto)
    .deep()
    .to(new HashMap<>());

// 모든 중첩 객체가 독립적으로 복사됨
```

### 5-3. 유연한 체이닝

```java
// 모든 메서드는 copier를 반환하여 체이닝 가능

UserDto result = S2Copier.from(source)
    .deep()
    .deepOnly("tags", "addresses")
    .exclude("id")
    .map("userId", "id")
    .ignoreNulls()
    .to(UserDto.class);  // UserDto 인스턴스 반환

// 대상 클래스 인스턴스 생성도 가능

Map<String, Object> map = S2Copier.from(source)
    .deep()
    .to(new HashMap<>());  // 대상 인스턴스 제공
```

---

## 6. 일반적인 패턴

### 6-1. 안전한 캐시/버퍼

```java
// 반환된 복사본 수정이 원본에 영향을 주지 않도록 보장

public class UserService {
    private User cachedUser;

    public User getUserCopy() {
        return S2Copier.from(cachedUser)
            .deepOnly("preferences", "settings")
            .to(User.class);
    }
}
```

### 6-2. 감사용 스냅샷

```java
// 감사 추적을 위한 완전하고 독립적인 스냅샷 생성

public void auditUserChange(User original, User modified) {
    User snapshot = S2Copier.from(original)
        .deep()
        .to(User.class);

    auditLog.record(snapshot, modified);
}
```

### 6-3. 폼 제출 안전성

```java
// 원본 엔티티의 의도하지 않은 수정 방지

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

### 6-4. 부분 업데이트

```java
// PATCH: 제공된 필드만 업데이트

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

## 7. 성능 고려사항

### 7-1. 깊은 복사를 사용해야 할 때

```
✅ 중첩 객체의 독립적인 복사본 필요
✅ 복사본 수정이 원본에 영향을 주면 안 될 때
✅ 계산용 임시 복사본 작업
✅ 감사용 스냅샷 생성
```

### 7-2. 깊은 복사를 피해야 할 때

```
❌ 얕은 참조로 충분할 때
❌ 매우 큰 객체 그래프를 다룰 때
❌ 타이트 루프 또는 고빈도 작업
❌ 순환참조가 매우 깊을 때 (> 100 레벨)
```

### 7-3. 최적화 팁

```java
// ✓ 좋음: 필요한 것만 깊게 복사

copy = S2Copier.from(source)
    .deepOnly("collectionField1", "collectionField2")  // 선택적 깊은 복사
    .to(Target.class);

// ✗ 덜 최적: 모든 필드 깊게 복사

copy = S2Copier.from(source)
    .deep()  // 단순 문자열 포함 모든 필드
    .to(Target.class);

// ✓ 좋음: 루프에서 깊은 복사하지 않기

for (int i = 0; i < 1000; i++) {
    // 얕은 복사가 훨씬 빠름
    Result temp = S2Copier.from(template).to(Result.class);
}

// ✗ 권장하지 않음: 루프에서 깊은 복사

for (int i = 0; i < 1000; i++) {
    // 깊은 복사 오버헤드가 1000배로 증폭!
    Result temp = S2Copier.from(template).deep().to(Result.class);
}
```

---

## 8. 문제 해결

### 8-1. "Unable to instantiate target class"

```
Error: RuntimeException - Failed to instantiate target class

원인: no-argument 생성자가 없음

해결책:
public no-arg 생성자 추가
public MyClass() {}
```

### 8-2. "Maximum deep copy depth exceeded"

```
Error: RuntimeException - Maximum deep copy depth exceeded (MAX_DEPTH=100)

[한국어] 원인: 객체 중첩이 100 레벨 초과

Solution / 해결책:
Use deepOnly() instead of deep()
copy = S2Copier.from(source)
    .deepOnly("otherField")  // 깊게 중첩된 필드 건너뜀
    .to(Target.class);
```

### 8-3. 순환참조 문제

```
Error: Various errors during deep copy

원인: 순환참조가 제대로 처리되지 않음

자동으로 처리되어야 하지만 다음 사항을 확인하세요:
1. IdentityHashMap 사용 확인
2. MAX_DEPTH 보호 확인
3. 중요 필드에만 deepOnly() 사용 고려
```

---

## 9. 모범 사례

```
1. ✅ 성능을 위해 deepOnly() 사용
2. ✅ 순환참조로 테스트
3. ✅ 성능 영향 모니터링
4. ✅ 변경 가능한 필드 문서화
5. ❌ 핫 경로에서 깊은 복사 회피
6. ❌ 타이트 루프에서 깊은 복사 금지
7. ✅ 가독성을 위해 메서드 체이닝
8. ✅ 유연성을 위해 필드 매핑 사용
```

---

## 10. 기능 비교 매트릭스

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
