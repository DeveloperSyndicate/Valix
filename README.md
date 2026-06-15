# Valix

Valix is a production-grade, compile-time validation library for Kotlin (JVM & Android), built on top of **KSP (Kotlin Symbol Processing)** and **KotlinPoet**.

Inspired by NestJS's `class-validator` and Java's Bean Validation (JSR 380), Valix focuses on delivering high-performance validations without runtime overhead.

---

## 🚀 Key Features

- **Zero Reflection**: All validators are generated as pure Kotlin code at compile time. No performance hit or startup delays.
- **Android & JVM Friendly**: Works seamlessly on Android and JVM without requiring custom Proguard/R8 reflection rules.
- **Strict Compile-Time Safety**: Generates clean, human-readable Kotlin code that is type-safe.
- **Low Allocation Overhead**: Minimizes garbage collection allocations by using a simple procedural check sequence.
- **Nullability Aware**: Avoids unnecessary checks and compiler warnings by treating Kotlin's nullable (`String?`) and non-nullable (`String`) types correctly.
- **Nested Object Validation**: Validate complex nested objects using `@Valid` with deep path propagation (e.g. `address.city` or `address.country.name`).
- **Collection Validation**: Validate elements of `List`, `Set`, and `Iterable` types using `@Valid` (with index propagation, e.g. `nicknames[1].name`).
- **Validation Groups**: JSR-380 style validation groups (marker interfaces) to selectively execute validation checks.
- **Custom Messages**: Supply custom validation error messages directly on annotations.
- **Reflection-Free Registry**: A global generated `ValixRegistry` to validate any registered class dynamically.
- **Fully Backward Compatible**: Keeps supporting Phase 1 validators (`<ClassName>ValixValidator`) and constructors.

---

## 📦 Module Structure

- **`valix-annotations`**: Lightweight SOURCE-retention annotation definitions. 
- **`valix-core`**: Defines the shared models (`ValidationError`, `ValidationResult`) and core contracts.
- **`valix-ksp`**: The KSP symbol processor that reads annotations and generates validator code.
- **`sample-jvm`**: Demonstrates the integration and validates usage.
- **`sample-android`**: Showcases Android module compatibility.

---

## 🛠️ Getting Started

To use Valix in your Kotlin project, apply the KSP plugin and add Valix dependencies.

### 1. Root Configuration (`build.gradle.kts`)

```kotlin
plugins {
    kotlin("jvm") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
}
```

### 2. Module Configuration (`build.gradle.kts`)

```kotlin
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    // Annotations to use in your classes
    implementation("io.valix:valix-annotations:1.0.0")
    // Core runtime models
    implementation("io.valix:valix-core:1.0.0")
    // KSP annotation processor
    ksp("io.valix:valix-ksp:1.0.0")
}

// Optionally make Gradle aware of generated KSP source directory:
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
```

---

## 🏷️ Supported Annotations

All validation constraints support:
- `message: String = ""` to customize error messages.
- `groups: Array<KClass<*>> = []` to specify validation groups.

| Annotation | Description | Error Code | Default Message |
| :--- | :--- | :--- | :--- |
| `@NotNull` | Must not be null (only relevant for nullable types) | `NOT_NULL` | `must not be null` |
| `@NotBlank` | Must not be empty or blank space | `NOT_BLANK` | `must not be blank` |
| `@Email` | Must match a lightweight email regex | `EMAIL_INVALID` | `invalid email` |
| `@MinLength(val)`| Minimum character length | `MIN_LENGTH` | `minimum length is X` |
| `@MaxLength(val)`| Maximum character length | `MAX_LENGTH` | `maximum length is X` |
| `@Pattern(regex)`| Must match the specified regular expression | `PATTERN_MISMATCH`| `pattern mismatch` |
| `@Valid` | Instructs Valix to perform nested or collection validation | *Propagated* | *Propagated* |

---

## 💡 Usage Example

### 1. Define your Validation Groups & Data Classes

```kotlin
package com.example.auth

import io.valix.annotations.*

// Validation Group marker interfaces
interface Create
interface Update

data class Country(
    @NotBlank(message = "Country name must not be blank")
    val name: String
)

data class Address(
    @NotBlank
    val city: String,

    @Valid
    val country: Country?
)

data class Nickname(
    @MinLength(value = 3, message = "Nickname too short")
    val name: String
)

data class RegisterRequest(
    @Email(groups = [Create::class], message = "Custom email invalid")
    val email: String,

    @Valid
    val address: Address,

    @Valid
    val nicknames: List<Nickname>?
)
```

### 2. Run Validation

During compilation, Valix generates a validator class called `RegisterRequestValidator` (and a backward-compatible alias `RegisterRequestValixValidator`) in the `com.example.auth.generated` package.

```kotlin
package com.example.auth

import com.example.auth.generated.RegisterRequestValidator

fun handleRegistration(request: RegisterRequest) {
    // Validate request using 'Create' group
    val result = RegisterRequestValidator.validate(request, Create::class)
    
    if (result.valid) {
        println("Request is valid! Processing...")
    } else {
        println("Validation failed with errors:")
        result.errors.forEach { error ->
            println(" - Field: ${error.field}, Code: ${error.code}, Message: ${error.message}, Rejected: ${error.rejectedValue}")
        }
    }
}
```

If we execute this with invalid data:
- `address.city = " "`
- `address.country.name = " "`
- `nicknames = listOf(Nickname("Al"))`

The error fields will propagate paths:
- `address.city`
- `address.country.name` (Message: `Country name must not be blank`)
- `nicknames[0].name` (Message: `Nickname too short`, Rejected: `Al`)

---

## 🗃️ Global ValixRegistry

Valix compiles a reflection-free global registry `io.valix.generated.ValixRegistry` containing all compile-time registered validator mappings. This is extremely useful for running validation dynamically on generic inputs (e.g. at an HTTP router or gateway controller layer):

```kotlin
import io.valix.generated.ValixRegistry

fun validatePayload(payload: Any) {
    val result = ValixRegistry.validate(payload)
    if (!result.valid) {
        // Handle validation errors...
    }
}
```

---

## 🔍 How Code Generation Works Under the Hood

For the nested data classes above, KSP generates standard, easy-to-read Kotlin files:

```kotlin
package com.example.auth.generated

import com.example.auth.RegisterRequest
import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import kotlin.reflect.KClass

public object RegisterRequestValidator {
  public fun validate(value: RegisterRequest, vararg groups: KClass<*>): ValidationResult {
    val errors = mutableListOf<ValidationError>()
    
    // Validate email with group check
    val emailVal = value.email
    val groupCheckEmail = groups.isEmpty() || groups.any { it == com.example.auth.Create::class }
    if (groupCheckEmail) {
      if (!EMAIL_REGEX.matches(emailVal)) {
        errors.add(ValidationError("email", "EMAIL_INVALID", "Custom email invalid", emailVal))
      }
    }
    
    // Validate address nested object
    val addressVal = value.address
    val addressResult = com.example.auth.generated.AddressValidator.validate(addressVal, *groups)
    addressResult.errors.forEach { error ->
      errors.add(ValidationError("address." + error.field, error.code, error.message, error.rejectedValue))
    }
    
    // Validate nicknames list
    val nicknamesVal = value.nicknames
    if (nicknamesVal != null) {
      nicknamesVal.forEachIndexed { index, item ->
        val itemResult = com.example.auth.generated.NicknameValidator.validate(item, *groups)
        itemResult.errors.forEach { error ->
          errors.add(ValidationError("nicknames[" + index + "]." + error.field, error.code, error.message, error.rejectedValue))
        }
      }
    }
    
    return ValidationResult(errors.isEmpty(), errors)
  }
}
```
