# Valix

<p align="center">
  <img src="images/logo.png" alt="Valix Logo" width="160px">
</p>

Compile-time generated validation logic for Kotlin. Zero reflection. Generated Kotlin code.

[![Build Status](https://github.com/developersyndicate/valix/actions/workflows/publish.yml/badge.svg)](https://github.com/developersyndicate/valix/actions/workflows/publish.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.developersyndicate.valix/valix-core.svg)](https://search.maven.org/artifact/com.developersyndicate.valix/valix-core)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

Valix uses **Kotlin Symbol Processing (KSP)** to generate type-safe validators at compile time—delivering reflection-free validation with zero runtime overhead and zero cold-start delay.

* **Kotlin-Native**: Built specifically for Kotlin types, nullability (`T?`), and data classes.
* **KSP Powered**: Generates clean, human-readable procedural Kotlin code at build time.
* **Zero Reflection**: Avoids expensive reflection calls and custom Proguard/R8 reflection rules.
* **Multiplatform (KMP)**: Supports JVM, Android, iOS (`iosArm64`, `iosX64`, `iosSimulatorArm64`), Web (JS), and WebAssembly (Wasm).
* **Framework Ready**: First-class integrations for Spring Boot, Ktor, Micronaut, Jetpack Compose, and Coroutines Flow.
* **Schema Generation**: Exports OpenAPI 3.1 YAML descriptors and JSON Schema (Draft-07).

---

## 30-Second Overview

### 1. Annotate Data Model
```kotlin
package com.example.user

import io.valix.annotations.*

data class CreateUserRequest(
    @NotBlank
    val username: String,

    @Email
    val email: String,

    @Min(18)
    val age: Int,

    @Sensitive(mask = "[REDACTED]")
    @MinLength(8)
    val password: String
)
```

### 2. Execute Validation
```kotlin
val request = CreateUserRequest(username = "john", email = "invalid", age = 15, password = "123")
val result = CreateUserRequestValidator.validate(request)

if (!result.valid) {
    result.errors.forEach { error ->
        println("${error.field}: ${error.message} (Rejected: ${error.rejectedValue})")
    }
}
```

### 3. Generated Code Under the Hood
KSP generates standard, human-readable Kotlin procedural code in your `build/generated/ksp/` folder:

```kotlin
public object CreateUserRequestValidator : ValixValidator<CreateUserRequest> {
    override fun validate(value: CreateUserRequest, vararg groups: KClass<out Any>, failFast: Boolean): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        val usernameVal = value.username
        if (usernameVal.trim().isEmpty()) {
            errors.add(ValidationError(field = "username", code = "NOT_BLANK", message = "must not be blank", path = "username"))
            if (failFast) return ValidationResult(false, errors)
        }

        val emailVal = value.email
        if (!emailVal.matches(EMAIL_REGEX)) {
            errors.add(ValidationError(field = "email", code = "EMAIL_INVALID", message = "invalid email", path = "email"))
            if (failFast) return ValidationResult(false, errors)
        }

        val ageVal = value.age
        if (ageVal < 18) {
            errors.add(ValidationError(field = "age", code = "MIN_VALUE", message = "must be at least 18", path = "age"))
            if (failFast) return ValidationResult(false, errors)
        }

        val passwordVal = value.password
        if (passwordVal.length < 8) {
            errors.add(ValidationError(field = "password", code = "MIN_LENGTH", message = "minimum length is 8", rejectedValue = "[REDACTED]", path = "password"))
            if (failFast) return ValidationResult(false, errors)
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}
```

---

## Comparison Matrix

| Feature | Valix | Bean Validation (JSR 380) | Valiktor | Konform |
| :--- | :--- | :--- | :--- | :--- |
| **Kotlin-First Design** | Yes | No (Java-centric) | Yes | Yes |
| **Execution Model** | KSP Codegen | Runtime Reflection | Runtime Reflection | Type-safe DSL |
| **Reflection-Free** | Yes | No | No | Yes |
| **Kotlin Multiplatform (KMP)** | Yes (JVM, iOS, JS, Wasm) | No | No | Yes |
| **Spring Boot / Ktor / Micronaut** | Yes (Dedicated Adapters) | Yes (Spring default) | Manual | Manual |
| **Jetpack Compose Integration** | Yes | No | No | No |
| **OpenAPI / JSON Schema Export** | Yes (Built-in Generator) | Ecosystem Addons | No | No |
| **Fail-Fast Execution Mode** | Yes | No | No | No |
| **Async Validator Codegen** | Yes | No | No | No |

---

## Performance Benchmark (JMH)

Microbenchmarks executed via Java Microbenchmark Harness (JMH) comparing Valix against Hibernate Validator (the reference JSR-380 implementation):

| Case | Hibernate Validator (JSR-380) | Valix | Throughput Speedup |
| :--- | :--- | :--- | :--- |
| **Invalid Payload Validation** | 874,809 ops/sec | **7,866,714 ops/sec** | **~9.0x speedup** |
| **Valid Payload Validation** | 905,822 ops/sec | **8,511,063 ops/sec** | **~9.4x speedup** |

*Bypassing runtime reflection and annotation introspection yields ~9.4x higher operational throughput with zero cold-start latency.*

---

## Examples & Benchmarks

For complete integration projects and real-time validation execution benchmarks of Valix across Spring Boot, Micronaut, Ktor, Kotlin Multiplatform (KMP), and Android Jetpack Compose, check out the dedicated examples repository:

👉 **[DeveloperSyndicate/Valix-Examples](https://github.com/DeveloperSyndicate/Valix-Examples)**

---

## Installation

### 1. Apply KSP Plugin (`build.gradle.kts`)
```kotlin
plugins {
    kotlin("jvm") version "2.3.21"
    id("com.google.devtools.ksp") version "2.3.9"
}
```

### 2. Add Dependencies
```kotlin
dependencies {
    // Core annotations and runtime
    implementation("com.developersyndicate.valix:valix-core:1.0.4")
    implementation("com.developersyndicate.valix:valix-runtime:1.0.4")

    // KSP annotation processor
    ksp("com.developersyndicate.valix:valix-ksp:1.0.4")
}
```

---

## Ecosystem & Framework Adapters

* **Spring Boot (`valix-spring`)**: Auto-configures `SpringMessageResolver` to translate message keys using native Spring `MessageSource` localizations and handles controller parameter validation.
* **Ktor (`valix-ktor`)**: Pipeline interceptor validating incoming call request payloads automatically.
* **Micronaut (`valix-micronaut`)**: AOP advice (`@ValixValidated`) and method interceptor for parameter validation.
* **Jetpack Compose (`valix-compose`)**: State management via `rememberValixForm()` and `ValidatedTextField`.
* **Coroutines Flow (`valix-flow`)**: Reactive stream validation operator (`validateWith`).
* **Architecture Components (`valix-viewmodel`)**: ViewModel state binding via `ValixFormViewModel`.

---

## Key Features

### 1. Fail-Fast Execution (`failFast = true`)
Terminate validation execution immediately on the first encountered error to reduce unnecessary processing:

```kotlin
val result = CreateUserRequestValidator.validate(request, failFast = true)
```

### 2. Sensitive Data Masking (`@Sensitive`)
Redact sensitive inputs from error reporting:

```kotlin
data class LoginRequest(
    @NotBlank
    val username: String,

    @Sensitive(mask = "[REDACTED]")
    @MinLength(8)
    val password: String
)
```

### 3. Conditional Validation (`@ValidateIf`)
Evaluate constraints on a property only when sibling properties satisfy condition checks:

```kotlin
data class PaymentRequest(
    val paymentType: String,

    @ValidateIf(field = "paymentType", equals = "CARD")
    @NotBlank(message = "Card number is required for card payments")
    val cardNumber: String?
)
```

### 4. Dynamic Parameter Interpolation
Expose constraint parameters (`min`, `max`, `value`) directly inside error message templates:

```kotlin
data class Account(
    @MinLength(value = 8, message = "Minimum length is {min}")
    val username: String
)
```

### 5. Programmatic Builder DSL (`valixDsl`)
Validate third-party or domain models without adding annotations:

```kotlin
val UserValidator = valixDsl<DomainUser> {
    field("email", DomainUser::email) {
        notBlank()
        email()
    }
    field("age", DomainUser::age) {
        min(18)
    }
}
```

---

## Supported Constraints

### String Constraints
`@NotNull`, `@NotBlank`, `@Email`, `@MinLength(val)`, `@MaxLength(val)`, `@Pattern(regex)`, `@Url`, `@PhoneNumber`, `@Alpha`, `@AlphaNumeric`, `@LowerCase`, `@UpperCase`, `@Contains(val)`, `@StartsWith(val)`, `@EndsWith(val)`.

### Numeric Constraints
`@Min(val)`, `@Max(val)`, `@Range(min, max)`, `@Positive`, `@PositiveOrZero`, `@Negative`, `@NegativeOrZero`.

### Collection & Enum Constraints
`@NotEmpty`, `@Size(min, max)`, `@AllowedValues(array)`.

---

## Documentation & AI Context

* **[API Reference Documentation (Dokka Pages)](https://developersyndicate.github.io/Valix/)**
* **[Medium Article: Building a Zero-Reflection Validation Engine in Kotlin](https://medium.com/@imsaba16/building-a-zero-reflection-validation-engine-in-kotlin-using-ksp-91a538badb65)**
* **[Medium Article: Can Compile-Time Generated Validation Really Be Faster?](https://medium.com/@imsaba16/can-compile-time-generated-validation-really-be-faster-f2d659c06040)**

Comprehensive documentation is available in the [`docs/`](docs/) directory:

* **[Architecture & Internals](docs/ARCHITECTURE.md)**: KSP processing pipeline, generated code structure, and multiplatform design.
* **[Developer Cheatsheet](docs/CHEATSHEET.md)**: Annotation quick reference and feature summary.
* **[Advanced Features](docs/ADVANCED.md)**: `@Sensitive`, `@ValidateIf`, `failFast`, schema export, and `valixDsl`.
* **Framework Guides**:
  * [Spring Boot Integration](docs/framework-guides/SPRING.md)
  * [Ktor Integration](docs/framework-guides/KTOR.md)
  * [Micronaut Integration](docs/framework-guides/MICRONAUT.md)
  * [Jetpack Compose Integration](docs/framework-guides/COMPOSE.md)
  * [ViewModel & Flow Integration](docs/framework-guides/VIEWMODEL_FLOW.md)
* **AI & LLM Context Prompting**:
  * **[`docs/LLMS.txt`](docs/LLMS.txt)**: High-density context summary for AI coding assistants.
  * **[`docs/LLMS_FULL.txt`](docs/LLMS_FULL.txt)**: Complete API and integration reference for LLM context ingestion.

---

## License

```text
Copyright 2026 Developer Syndicate

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
