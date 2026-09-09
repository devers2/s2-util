# S2 Validator Plugin - Gradle Build Plugin (s2-validator-plugin)

[English](README.md) | [한국어](README.ko.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.devers2/s2-validator-plugin?color=brightgreen&label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.devers2/s2-validator-plugin)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](../LICENSE)

> 📦 Part of the **[S2Util Suite](../README.md)**.

---

## 📖 Overview

The **s2-validator-plugin** is a Gradle build plugin that performs static source code analysis to validate field names used in S2Validator configurations. While S2Validator leverages dot notation and array indexing for powerful nested object validation, it cannot verify at compile-time whether specified field names actually exist in target DTO classes. This plugin fills that gap by detecting typos, non-existent fields, and incorrect field references **before runtime**, preventing misconfiguration errors and enabling early error detection during the build process.

---

## ✨ Key Features

1. **Static Source Code Analysis with JavaParser**
   - AST (Abstract Syntax Tree) parsing for accurate code analysis
   - Detects all `.field("fieldName")` calls in S2Validator configurations
   - Zero runtime overhead: Analysis happens only at build-time

2. **Compile-Time Field Validation**
   - Verifies that specified field names actually exist in target DTO classes
   - Identifies typos in field names before they cause runtime errors
   - Supports inheritance: Validates fields from parent classes as well

3. **Multi-Project Support**
   - Scans all subprojects within the root Gradle project
   - Finds DTO classes across multiple modules
   - Unified validation across complex multi-module builds

4. **Zero Configuration Required**
   - Automatically applies validation during standard Gradle build tasks
   - Executes before `compileJava` task: Prevents compilation of invalid code
   - Integrates with `check` task for CI/CD pipelines
   - Supports `bootRun` and other JavaExec tasks

5. **Field Caching for Performance**
   - Caches analyzed DTO field information in memory
   - Reuses cached data for repeated validation checks
   - Minimizes repeated file I/O and parsing operations

6. **Smart Validation Skipping**
   - Skips validation for generic types with `?` or `Object`
   - Gracefully handles cases where complete type information is unavailable
   - Prevents false positives from incomplete generic type parameters

7. **Detailed Error Reporting**
   - Clear error messages with color-coded output
   - Shows file paths, line numbers, and problematic field names
   - Identifies which target DTO class lacks the specified field
   - Example: `'address' 필드가 UserDTO에 없습니다` (Field 'address' not found in UserDTO)

8. **Build Failure on Errors**
   - Strict validation mode: Build fails immediately if errors detected
   - Prevents invalid code from progressing through the build pipeline
   - Ensures only properly configured validators reach production

---

## 🔧 Installation

### settings.gradle

```groovy
pluginManagement {
    repositories {
        mavenCentral()
    }
}
```

### build.gradle

```groovy
plugins {
    id 'io.github.devers2.validator' version '1.1.2'
}
```

---

## ⚙️ Requirements

- This project is built with **JDK 21**, but it can be used reliably in all environments running **Java 17 or higher**.
- **Gradle 8.0 or higher** is recommended.

---

## 🔄 Compatibility

This plugin supports **s2-validator version 1.1.0 or higher** to ensure optimal functionality and compatibility with the latest features.

---

## 📜 License & Copyright

This library is provided under the **Apache License 2.0**. You are free to use, modify, and distribute this software, provided that you comply with the obligations of the license (such as copyright notice and source code disclosure requirements). For detailed terms and conditions, please refer to the **[LICENSE](./LICENSE)** file.

- **Copyright 2020 - 2026 devers2 (이승수, Daejeon, Korea)**
- Contact: [eseungsu.dev@gmail.com](mailto:eseungsu.dev@gmail.com)

**Third-party Notice:** This project uses external libraries. For detailed third-party license notices, please refer to the **[licenses/NOTICE](./licenses/NOTICE)** file.

---

s2-validator-plugin Version: 1.1.2 (2026-08-12)
