# Valix Architecture & Compilation Internals

## Architectural Overview

Valix is designed around a zero-reflection, compile-time code generation model. Instead of scanning annotations at runtime via Java Reflection or Kotlin Reflection, Valix uses **Kotlin Symbol Processing (KSP)** to analyze annotated data classes during compilation and output standard, procedural Kotlin code.

```text
┌────────────────────────┐      ┌────────────────────────┐      ┌────────────────────────┐
│   Annotated Source     │ ---> │    Valix Processor     │ ---> │   Generated Kotlin     │
│   (e.g., User.kt)      │      │    (valix-ksp)         │      │   (UserValidator.kt)   │
└────────────────────────┘      └────────────────────────┘      └────────────────────────┘
                                            │
                                            v
                                ┌────────────────────────┐
                                │   Generated Registry   │
                                │   (ValixRegistry.kt)   │
                                └────────────────────────┘
```

---

## Core Modules & Design

### 1. `valix-core` (Common Multiplatform Library)
- **Target**: KMP (`commonMain`, JVM, Android, iOS, JS, Wasm).
- Contains primary constraint annotations (`@NotNull`, `@Min`, `@Email`, `@Sensitive`, `@ValidateIf`, etc.).
- Defines core contracts:
  - `ValixValidator<T>`: Primary validator interface with `validate()` and `validateAsync()`.
  - `ValidationError`: Error descriptor carrying `field`, `code`, `message`, `messageKey`, `rejectedValue`, `constraint`, and `path`.
  - `ValidationResult`: Container wrapping `valid: Boolean` and `errors: List<ValidationError>`.
  - `ConstraintValidator<T>`, `ObjectConstraintValidator<T>`, and `AsyncConstraintValidator<T>`.

### 2. `valix-ksp` (Annotation Processor)
- **Target**: JVM (KSP Processor).
- Consists of three processing stages:
  1. `ConstraintResolver`: Inspects annotations, validates target property types, resolves validation groups, and checks custom validator inheritance.
  2. `ValixProcessor`: Iterates through class & property descriptors and builds Kotlin code using KotlinPoet.
  3. Code Generation Outputs:
     - `<ClassName>Validator.kt`: Primary validator singleton object.
     - `<ClassName>ValixValidator.kt`: Backward compatibility alias.
     - `ValixRegistry.kt`: Global reflection-free class-to-validator map.
     - `<ClassName>ValidationMetadata.kt`: Compile-time model metadata registration.

### 3. `valix-runtime` (Runtime Utilities & DSL)
- **Target**: KMP (`commonMain`, JVM, Android, iOS, JS, Wasm).
- Contains `FormState<T>` for state management.
- Contains `ValixDiagnostics` profiling metrics using `TimeSource.Monotonic`.
- Contains `valixDsl` programmatic builder for dynamic or third-party data classes.

---

## Code Generation Pipeline

Given a data class:

```kotlin
data class User(@Email val email: String)
```

KSP processes `User.kt` and emits:

```kotlin
public object UserValidator : ValixValidator<User> {
    override fun validate(value: User, vararg groups: KClass<out Any>, failFast: Boolean): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val emailVal = value.email
        if (!emailVal.matches(EMAIL_REGEX)) {
            errors.add(ValidationError(field = "email", code = "EMAIL_INVALID", message = "invalid email", path = "email"))
            if (failFast) return ValidationResult(false, errors)
        }
        return ValidationResult(errors.isEmpty(), errors)
    }

    override suspend fun validateAsync(value: User, vararg groups: KClass<out Any>, failFast: Boolean): ValidationResult =
        validate(value, *groups, failFast = failFast)
}
```

---

## Global Reflection-Free Registry

`ValixRegistry` is compiled at build time into `io.valix.generated.ValixRegistry`:

```kotlin
public object ValixRegistry {
    private val validators: Map<KClass<*>, (Any, Array<out KClass<*>>, Boolean) -> ValidationResult> = mapOf(
        User::class to { value, groups, failFast -> UserValidator.validate(value as User, *groups, failFast = failFast) }
    )

    fun validate(value: Any, vararg groups: KClass<*>, failFast: Boolean = false): ValidationResult {
        val validator = validators[value::class] ?: return ValidationResult(true, emptyList())
        return validator(value, groups, failFast)
    }
}
```
