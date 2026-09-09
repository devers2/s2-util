# S2Util Library - Core Module (s2-core)

[English](README.md) | [한국어](README.ko.md)

---

## 📖 Overview

The **s2-core** module is the foundational library of the S2Util project, providing high-performance core utility classes optimized for Java development. It leverages advanced technologies such as Method Handles for efficient reflection, **built-in optimized lightweight cache for zero-dependency caching (with optional Caffeine support)**, and adaptive thread factories for different Java versions. Core features include optimized data access/manipulation methods (`getValue()`, `setValue()`), string utilities, date/time handling, and type conversion.

---

## ✨ Key Features

1. **High-Performance Reflection with Method Handles**
   - Eliminates performance bottlenecks of standard Java reflection (`java.lang.reflect`)
   - JIT compiler optimizes MethodHandle calls to near-native performance
   - Strategic caching of MethodHandles in ConcurrentHashMap for repeated access

2. **Intelligent Caching (Dual Mode)**
   - **Default**: Built-in `S2OptimisticCache` optimized for speed and simplicity (No external dependencies)
     - Lock-free reads & Optimistic/Atomic writes
     - Sequence-based LRU eviction strategy
   - **Optional**: Seamless integration with **Caffeine Cache** for enterprise-grade workloads
     - W-TinyLFU algorithm for maximizing hit rates in high-traffic scenarios
     - Automatically activated when Caffeine is present in the classpath
     - **Verification**: You can check if Caffeine is enabled by calling `S2Cache.isCaffeineEnabled()`.

3. **Java Version-Adaptive Thread Factory**
   - Virtual Thread support for Java 21+ environments
   - Fallback to optimized platform thread pools for earlier versions
   - Unified API for cross-version compatibility

4. **Optimized Data Access & Manipulation**
   - `getValue()`: Extracts values from nested objects with dot notation (`user.address.street`)
   - `setValue()`: Sets values in nested structures with array/collection support
   - Support for Maps, Lists, Arrays, Records, DTO/VO, and JPA Hibernate proxies
   - Bracket notation support (`users[0].name`, `matrix[1][2]`)

5. **Comprehensive Utility Modules**
   - **S2Cache**: Advanced caching with pattern-based eviction policies
   - **S2ThreadUtil**: Thread and executor management with version-aware optimization
   - **S2StringUtil**: String manipulation with character replacement, validation, encoding; Pattern caching for regex operations reduces compilation overhead
   - **S2DateUtil**: Date/time parsing, formatting, timezone handling

6. **Multi-Level Access Modes**
   - **Public Mode**: Adheres to public contracts (Getter/Setter) with maximum performance
   - **Private Mode**: Enables private member access when explicitly required

---

## ⚙️ Requirements

This project is built with **JDK 21**, but it can be used reliably in all environments running **Java 17 or higher**.

---

## 📦 Dependencies

This module has **ZERO mandatory runtime dependencies**.

- **Caffeine Cache**: Optional. Add this dependency only if you require advanced caching features for high-concurrency environments.

---

## 📜 License & Copyright

This library is provided under the **Apache License 2.0**. You are free to use, modify, and distribute this software, provided that you comply with the obligations of the license (such as copyright notice and source code disclosure requirements). For detailed terms and conditions, please refer to the **[LICENSE](./LICENSE)** file.

- **Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)**
- Contact: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**Third-party Notice:** This project uses external libraries. For detailed third-party license notices, please refer to the **[licenses/NOTICE](./licenses/NOTICE)** file.

---

s2-core Version: 1.1.7 (2026-08-12)

[//]: # 'S2_DEPS_INFO_START'

---

**To use certain functionalities (e.g., S2BindValidator), the end-user project must explicitly add the following dependencies to be available at runtime.** Failure to include these dependencies will result in a `java.lang.NoClassDefFoundError` at runtime.

**[For Gradle Users]**

```groovy
dependencies {
    // Essential runtime dependencies for optional functionalities
    implementation 'com.github.ben-manes.caffeine:caffeine:3.2.4'
}
```

[//]: # 'S2_DEPS_INFO_END'
