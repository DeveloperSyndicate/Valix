# Contributing to Valix

Thank you for your interest in contributing to Valix! This document outlines the guidelines and steps to set up, build, and contribute to the Valix project.

---

## 1. Project Architecture Overview

Valix is a modular Kotlin Multiplatform project. Here is a breakdown of the primary modules:

* **`valix-annotations`**: Declares standard constraint annotations (e.g., `@NotNull`, `@NotBlank`, `@Size`).
* **`valix-core`**: Core validation engine interfaces, metadata schemas, and validation context.
* **`valix-runtime`**: Common KMP runtime rules, standard constraints implementations, and the reflection-free validator registry.
* **`valix-ksp`**: Kotlin Symbol Processing (KSP) annotation processor that generates validator source files during build compilation.
* **`valix-spring` / `valix-ktor` / `valix-micronaut`**: Framework adapters providing parameter validation resolvers and plugins.

---

## 2. Local Setup Requirements

To set up the development environment:
* **JDK Version:** Eclipse Adoptium OpenJDK 17.0.20+8 or higher.
* **Kotlin Version:** Compatible with Kotlin 2.x and KSP 2.x.
* **IDE:** IntelliJ IDEA (Recommended) or Android Studio.

---

## 3. How to Build and Run Tests Locally

Valix uses standard Gradle tasks to compile and verify all modules:

### Build the Project
Compile the core library and annotations code:
```bash
./gradlew compileKotlin
```

### Run Unit Tests
To run all tests across the core library and compiler processor:
```bash
./gradlew test
```

To run compiler rules validation tests specifically:
```bash
./gradlew :valix-ksp:test
```

### Generate Documentation
Verify that the multi-module API documentation builds without errors:
```bash
./gradlew dokkaHtmlMultiModule
```

---

## 4. Code Style & Design Guidelines

When submitting code changes:
* **Zero Reflection:** Ensure that new validator rules and generated files do not invoke runtime Java/Kotlin reflection APIs. Use build-time KSP parsing and direct property access.
* **KMP Compatibility:** Core libraries must target Kotlin Multiplatform. Do not import Java-specific dependencies (such as `java.util.*`) inside the `valix-core` or `valix-runtime` modules unless they are placed in JVM-specific source sets.
* **Keep KDocs Updated:** Document public interfaces and classes using standard Kotlin KDoc structures, specifying clear compile-time and runtime execution scopes.
