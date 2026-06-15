# Valix

Valix is a production-grade, compile-time validation library for Kotlin (JVM & Android), built on top of **KSP (Kotlin Symbol Processing)** and **KotlinPoet**.

Inspired by NestJS's `class-validator` and Java's Bean Validation (JSR 380), Valix focuses on delivering high-performance validations without runtime overhead.

---

## 🚀 Key Features

- **Zero Reflection**: All validators are generated as pure Kotlin code at compile time. No performance hit or startup delays.
- **Android & JVM Friendly**: Works seamlessly on Android and JVM without requiring custom Proguard/R8 reflection rules.
- **Strict Compile-Time Safety**: Generates clean, human-readable Kotlin code that is type-safe.
- **Low Allocation overhead**: Minimizes garbage collection allocations by using a simple procedural check sequence.
- **Nullability Aware**: Avoids unnecessary checks and compiler warnings by treating Kotlin's nullable (`String?`) and non-nullable (`String`) types correctly.

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

Phase 1 supports validation constraints on `String` and `String?` types:

| Annotation | Description | Error Code | Default Message |
| :--- | :--- | :--- | :--- |
| `@NotNull` | Must not be null (only relevant for `String?`) | `NOT_NULL` | `must not be null` |
| `@NotBlank` | Must not be empty or blank space | `NOT_BLANK` | `must not be blank` |
| `@Email` | Must match a lightweight email regex | `EMAIL_INVALID` | `invalid email` |
| `@MinLength(val)`| Minimum character length | `MIN_LENGTH` | `minimum length is X` |
| `@MaxLength(val)`| Maximum character length | `MAX_LENGTH` | `maximum length is X` |
| `@Pattern(regex)`| Must match the specified regular expression | `PATTERN_MISMATCH`| `pattern mismatch` |

---

## 💡 Usage Example

### 1. Define your Data Class

```kotlin
package com.example.auth

import io.valix.annotations.*

data class RegisterRequest(
    @NotNull
    @NotBlank
    @Email
    val email: String?,

    @MinLength(8)
    val password: String,

    @Pattern("^[a-zA-Z0-9]+$")
    val username: String
)
```

### 2. Run Validation

During compilation, Valix generates a validator class called `RegisterRequestValixValidator` in the `com.example.auth.generated` package.

```kotlin
package com.example.auth

import com.example.auth.generated.RegisterRequestValixValidator

fun handleRegistration(request: RegisterRequest) {
    val result = RegisterRequestValixValidator.validate(request)
    
    if (result.valid) {
        println("Request is valid! Processing...")
    } else {
        println("Validation failed with errors:")
        result.errors.forEach { error ->
            println(" - Field: ${error.field}, Code: ${error.code}, Message: ${error.message}")
        }
    }
}
```

---

## 🔍 How Code Generation Works Under the Hood

For the `RegisterRequest` class above, KSP generates the following companion object:

```kotlin
package com.example.auth.generated

import com.example.auth.RegisterRequest
import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import kotlin.text.Regex

public object RegisterRequestValixValidator {
  public val EMAIL_REGEX: Regex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}${'$'}")
  public val PATTERN_USERNAME: Regex = Regex("^[a-zA-Z0-9]+${'$'}")

  public fun validate(value: RegisterRequest): ValidationResult {
    val errors = mutableListOf<ValidationError>()
    
    // Validation for email
    val emailVal = value.email
    if (emailVal == null) {
      errors.add(ValidationError("email", "NOT_NULL", "must not be null"))
    } else {
      if (emailVal.trim().isEmpty()) {
        errors.add(ValidationError("email", "NOT_BLANK", "must not be blank"))
      }
      if (!EMAIL_REGEX.matches(emailVal)) {
        errors.add(ValidationError("email", "EMAIL_INVALID", "invalid email"))
      }
    }
    
    // Validation for password
    val passwordVal = value.password
    if (passwordVal.length < 8) {
      errors.add(ValidationError("password", "MIN_LENGTH", "minimum length is 8"))
    }
    
    // Validation for username
    val usernameVal = value.username
    if (!PATTERN_USERNAME.matches(usernameVal)) {
      errors.add(ValidationError("username", "PATTERN_MISMATCH", "pattern mismatch"))
    }
    
    return ValidationResult(errors.isEmpty(), errors)
  }
}
```
