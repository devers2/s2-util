# S2Copier: Zero-Reflection Copy Guide 📋

🌐 **English** | [한국어](GUIDE_COPIER.ko.md)

> **Optimized with `MethodHandle` for maximum throughput between Entities and DTOs.**

---

## 1. Quick Start

### 1-1. Basic Copy

```java
// Simple object copy between DTO and Entity

User entity = userRepository.findById(1L).orElseThrow();
UserDto dto = S2Copier.from(entity).to(UserDto.class);
```

### 1-2. Advanced Features

```java
// Advanced Mapping, Exclusion, and Partial Update

S2Copier.from(requestDto)
    .exclude("id", "secret")              // Field exclusion
    .map("nickName", "displayName")       // Property name sync
    .ignoreNulls()                        // Supports selective updates
    .to(existingEntity);                  // Naturally triggers JPA Dirty Checking
```

---

## 2. Core Features

### 2-1. Field Exclusion

```java
// Exclude sensitive or system fields from copying

User user = S2Copier.from(sourceUser)
    .exclude("id", "password", "secret")
    .to(User.class);

// User.id, password, secret are NOT copied
```

### 2-2. Field Mapping

```java
// Map source field names to different target field names

UserDto dto = S2Copier.from(entity)
    .map("id", "userId")                 // entity.id → dto.userId
    .map("name", "fullName")             // entity.name → dto.fullName
    .map("address.city", "location")     // entity.address.city → dto.location
    .to(UserDto.class);

// Now entity.id → dto.userId
```

### 2-3. Null-Aware Copying

```java
// Supports partial updates (PATCH semantics)

User existingUser = userRepository.findById(1L).orElseThrow();
UserUpdateDto updateDto = new UserUpdateDto("NewName", null, "newemail@example.com");

S2Copier.from(updateDto)
    .ignoreNulls()        // Only non-null values are copied
    .to(existingUser);    // Null values are NOT copied

// Result: name changed, age unchanged (was null), email changed
```

### 2-4. Map ↔ DTO Conversion

```java
// Copy from DTO to Map

UserDto dto = new UserDto("user001", "Alice", 30);

Map<String, Object> map = S2Copier.from(dto)
    .map("id", "userId")              // Field mapping still works
    .to(new HashMap<>());

// map: {userId: "user001", name: "Alice", age: 30}

// Copy from Map to DTO

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

## 3. Deep Copy with Circular Reference Protection

S2Copier supports **deep copy** mode to create independent copies of nested objects, collections, and maps. Circular references are automatically detected and handled to prevent infinite loops.

### 3-1. Deep Copy All Fields

```java
// Deep copy all fields including nested objects

User userCopy = S2Copier.from(originalUser)
    .deep()                           // Enable deep copy for all fields
    .to(User.class);

// Modifications to nested objects in the original don't affect the copy
originalUser.getAddress().setCity("NewCity");
assert userCopy.getAddress().getCity().equals("Seoul"); // ✓ Not affected
```

### 3-2. Deep Copy Specific Fields

```java
// Mixed shallow and deep copy mode (performance optimization)

UserDetail userDetail = S2Copier.from(source)
    .deepOnly("tags", "addresses")    // Only these fields are deeply copied
    .to(UserDetail.class);

// Simple string fields are shallow copied (reference copy)
// Collection fields are deep copied (independent copy)
```

### 3-3. How Deep Copy Works

#### Shallow Copy - Default behavior

```java
// Default: shallow copy shares references

User original = new User("Alice", new Address("Seoul"));
User copy = S2Copier.from(original).to(User.class);

// Shallow copy: Address object is shared
assert copy.getAddress() == original.getAddress(); // Same reference!

original.getAddress().setCity("Busan");
assert copy.getAddress().getCity().equals("Busan"); // ✗ Copy affected
```

#### Deep Copy - With deep() method

```java
// With deep(): independent copy of nested objects

User original = new User("Alice", new Address("Seoul"));
User copy = S2Copier.from(original).deep().to(User.class);

// Deep copy: Address object is independently copied
assert copy.getAddress() != original.getAddress(); // Different object

original.getAddress().setCity("Busan");
assert copy.getAddress().getCity().equals("Seoul"); // ✓ Copy not affected
```

### 3-4. Circular Reference Handling

#### The Problem

```java
// Circular reference creates infinite loop

ClassA a = new ClassA("A");
ClassB b = new ClassB("B");
a.setRef(b);      // A points to B
b.setRef(a);      // B points back to A (circular!)

// Without proper handling, deep copy would loop infinitely
```

#### The Solution

S2Copier uses **IdentityHashMap** to track already-copied objects:

1. **Visited Tracking**: Store copied objects in `IdentityHashMap<Object, Object>`
2. **Cycle Detection**: Before copying, check if object is already in the map
3. **Reference Reuse**: Return already-copied instance instead of re-copying

```java
// Graceful handling of circular references

ClassA aCopy = S2Copier.from(a).deep().to(ClassA.class);

// aCopy and aCopy.getRef().getRef() are properly linked
// without infinite recursion
assert aCopy != a;
assert aCopy.getRef() != b;
assert aCopy.getRef().getRef() == aCopy; // Circular reference preserved correctly

```

### 3-5. Safety Mechanism: MAX_DEPTH Limit

A **MAX_DEPTH limit (100)** provides a secondary safeguard against unexpectedly deep object graphs:

```java
// If nesting exceeds MAX_DEPTH, exception is thrown

try {
    ClassA copy = S2Copier.from(deeplyNested).deep().to(ClassA.class);
} catch (RuntimeException e) {
    if (e.getMessage().contains("Maximum deep copy depth exceeded")) {
        // Handle MAX_DEPTH exceeded
        System.out.println("Object nesting too deep");
    }
}
```

---

## 4. Supported Types for Deep Copy

### 4-1. Primitive Wrappers

```java
// Immutable types: returned as-is

Integer, Long, Double, Float, Boolean, Byte, Short, Character
String
```

### 4-2. Collections

```java
// Recursively deep copy each element

List, ArrayList, LinkedList
Set, HashSet, LinkedHashSet
Iterable implementations
```

### 4-3. Maps

```java
// Recursively deep copy each value

HashMap, LinkedHashMap
TreeMap, ConcurrentHashMap
Map implementations
```

### 4-4. Custom Objects

```java
// Any class with:

// 1. Accessible fields
// 2. A no-argument constructor
// 3. Proper getters/setters

public class User {
    private String name;
    private Address address;

    public User() {}  // Required: no-arg constructor
    public User(String name, Address address) { ... }

    // Getters/setters for S2Copier
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
}
```

---

## 5. Advanced Usage

### 5-1. Combining All Features

```java
// Deep copy + field mapping + exclusion + null handling

User copy = S2Copier.from(source)
    .deep()                       // Deep copy mode
    .deepOnly("preferences")      // Only deep copy this field
    .map("id", "userId")          // Field name mapping
    .exclude("secret")            // Exclude sensitive fields
    .ignoreNulls()                // Ignore null values
    .to(User.class);
```

### 5-2. Map Target with Deep Copy

```java
// Deep copy from DTO to Map

UserDto dto = new UserDto("user001", "Alice", 30, new Address("Seoul"));

Map<String, Object> deepMap = S2Copier.from(dto)
    .deep()
    .to(new HashMap<>());

// All nested objects are independently copied
```

### 5-3. Fluent Chaining

```java
// All methods return the copier for easy chaining

UserDto result = S2Copier.from(source)
    .deep()
    .deepOnly("tags", "addresses")
    .exclude("id")
    .map("userId", "id")
    .ignoreNulls()
    .to(UserDto.class);  // Returns UserDto instance

// Can also instantiate target class

Map<String, Object> map = S2Copier.from(source)
    .deep()
    .to(new HashMap<>());  // Provides target instance
```

---

## 6. Common Patterns

### 6-1. Safe Cache/Buffer

```java
// Ensure modifications to returned copy don't affect original

public class UserService {
    private User cachedUser;

    public User getUserCopy() {
        return S2Copier.from(cachedUser)
            .deepOnly("preferences", "settings")
            .to(User.class);
    }
}
```

### 6-2. Snapshot for Auditing

```java
// Create complete, independent snapshot for audit trail

public void auditUserChange(User original, User modified) {
    User snapshot = S2Copier.from(original)
        .deep()
        .to(User.class);

    auditLog.record(snapshot, modified);
}
```

### 6-3. Form Submission Safety

```java
// Prevent accidental modifications to original entity

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

### 6-4. PATCH Operations

```java
// PATCH: Only update provided fields

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

## 7. Performance Considerations

### 7-1. When to Use Deep Copy

```
✅ Independent copies of nested objects needed
✅ Modifications to copy shouldn't affect original
✅ Working with temporary copies for calculations
✅ Creating snapshots for auditing
```

### 7-2. When to Avoid Deep Copy

```
❌ Shallow references are sufficient
❌ Dealing with extremely large object graphs
❌ In tight loops or high-frequency operations
❌ When circular references are very deep (> 100 levels)
```

### 7-3. Optimization Tips

```java
// ✓ Better: Only deep copy what you need

copy = S2Copier.from(source)
    .deepOnly("collectionField1", "collectionField2")  // Selective
    .to(Target.class);

// ✗ Less optimal: Deep copy everything

copy = S2Copier.from(source)
    .deep()  // All fields, including simple strings
    .to(Target.class);

// ✓ Better: Don't deep copy in loops

for (int i = 0; i < 1000; i++) {
    // Shallow copy is much faster
    Result temp = S2Copier.from(template).to(Result.class);
}

// ✗ Not recommended: Deep copy in loops

for (int i = 0; i < 1000; i++) {
    // Deep copy overhead multiplied 1000 times!
    Result temp = S2Copier.from(template).deep().to(Result.class);
}
```

---

## 8. Troubleshooting

### 8-1. "Unable to instantiate target class"

```
Error: RuntimeException - Failed to instantiate target class

Cause: The class doesn't have a no-argument constructor

Solution:
Add a public no-arg constructor
public MyClass() {}
```

### 8-2. "Maximum deep copy depth exceeded"

```
Error: RuntimeException - Maximum deep copy depth exceeded (MAX_DEPTH=100)

Cause: Object nesting exceeds 100 levels

Solution:
Use deepOnly() instead of deep()
copy = S2Copier.from(source)
    .deepOnly("otherField")  // Skip the deeply nested one
    .to(Target.class);
```

### 8-3. Circular references causing issues

```
Error: Various errors during deep copy

Cause: Circular reference not properly handled

Solution:
This should be automatic, but check for:
1. Ensure IdentityHashMap is being used
2. Check MAX_DEPTH protection is in place
3. Consider using deepOnly() for critical fields
```

---

## 9. Best Practices

```
1. ✅ Use deepOnly() for performance

2. ✅ Test with circular references

3. ✅ Monitor performance impact

4. ✅ Document mutable fields

5. ❌ Avoid deep copy in hot paths

6. ❌ Don't deep copy in tight loops

7. ✅ Chain methods for readability

8. ✅ Use field mapping for flexibility
```

---

## 10. Comparison Matrix

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
