# Valix

Valix is a production-grade, compile-time validation library for Kotlin (JVM & Android), built on top of **KSP (Kotlin Symbol Processing)** and **KotlinPoet**.

Inspired by NestJS's `class-validator` and Java's Bean Validation (JSR 380), Valix focuses on delivering high-performance validations without runtime overhead.

---

## 🚀 Key Features

- **Zero Reflection**: All validators are generated as pure Kotlin code at compile time. No performance hit or startup delays.
- **Android & JVM Friendly**: Works seamlessly on Android and JVM without requiring custom Proguard/R8 reflection rules.
- **Strict Compile-Time Safety**: Generates clean, human-readable Kotlin code that is type-safe. Unsupported uses (e.g. `@MinLength` on `Int`) fail compilation immediately.
- **Low Allocation Overhead**: Minimizes garbage collection allocations by using a simple procedural check sequence.
- **Nullability Aware**: Avoids unnecessary checks and compiler warnings by treating Kotlin's nullable (`T?`) and non-nullable (`T`) types correctly.
- **Nested Object & Collection Validation**: Validate complex nested objects and collection elements using `@Valid` with deep path and index propagation (e.g. `address.city` or `nicknames[1].name`).
- **Validation Groups**: JSR-380 style validation groups (marker interfaces) to selectively execute validation checks.
- **Custom Messages**: Supply custom validation error messages directly on annotations.
- **Reflection-Free Registry**: A global generated `ValixRegistry` to validate any registered class dynamically.
- **Fully Backward Compatible**: Keeps supporting older validation interfaces (`<ClassName>ValixValidator`).

---

## 📦 Module Structure

- **`valix-annotations`**: Lightweight SOURCE-retention annotation definitions. 
- **`valix-core`**: Defines the shared models (`ValidationError`, `ValidationResult`) and core contracts.
- **`valix-ksp`**: The KSP symbol processor that reads annotations, validates correctness, and generates validator code.
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
```

---

## 🏷️ Supported Annotations

All validation constraints support:
- `message: String = ""` to customize error messages.
- `groups: Array<KClass<*>> = []` to specify validation groups.

### String Constraints (Applicable only to `String` and `String?`)

| Annotation | Description | Error Code | Default Message |
| :--- | :--- | :--- | :--- |
| `@NotNull` | Must not be null (only relevant for nullable types) | `NOT_NULL` | `must not be null` |
| `@NotBlank` | Must not be empty or blank space | `NOT_BLANK` | `must not be blank` |
| `@Email` | Must match a standard email regex | `EMAIL_INVALID` | `invalid email` |
| `@MinLength(val)`| Minimum character length | `MIN_LENGTH` | `minimum length is X` |
| `@MaxLength(val)`| Maximum character length | `MAX_LENGTH` | `maximum length is X` |
| `@Pattern(regex)`| Must match the specified regular expression | `PATTERN_MISMATCH`| `pattern mismatch` |
| `@Url` | Must be a valid URL | `URL_INVALID` | `invalid URL` |
| `@PhoneNumber` | Must be a valid phone number | `PHONE_INVALID` | `invalid phone number` |
| `@Alpha` | Must contain only alphabetic characters | `ALPHA_INVALID` | `must contain only alphabetic characters` |
| `@AlphaNumeric`| Must contain only alphanumeric characters | `ALPHANUMERIC_INVALID`| `must contain only alphanumeric characters` |
| `@LowerCase` | Must be completely lowercase | `LOWERCASE_INVALID` | `must be lowercase` |
| `@UpperCase` | Must be completely uppercase | `UPPERCASE_INVALID` | `must be uppercase` |
| `@Contains(val)`| Must contain the specified substring | `CONTAINS_INVALID` | `must contain 'X'` |
| `@StartsWith(val)`| Must start with the specified prefix | `STARTS_WITH_INVALID`| `must start with 'X'` |
| `@EndsWith(val)`| Must end with the specified suffix | `ENDS_WITH_INVALID` | `must end with 'X'` |

### Numeric Constraints (Applicable to `Int`, `Long`, `Float`, `Double`, `Short` and nullable equivalents)

| Annotation | Description | Error Code | Default Message |
| :--- | :--- | :--- | :--- |
| `@Min(val)` | Must be greater than or equal to `val` | `MIN_VALUE` | `must be at least X` |
| `@Max(val)` | Must be less than or equal to `val` | `MAX_VALUE` | `must be at most X` |
| `@Range(min, max)`| Must be between `min` and `max` inclusive | `RANGE_INVALID` | `must be between min and max` |
| `@Positive` | Must be strictly greater than 0 | `POSITIVE_REQUIRED` | `must be positive` |
| `@PositiveOrZero`| Must be greater than or equal to 0 | `POSITIVE_REQUIRED` | `must be positive or zero` |
| `@Negative` | Must be strictly less than 0 | `NEGATIVE_REQUIRED` | `must be negative` |
| `@NegativeOrZero`| Must be less than or equal to 0 | `NEGATIVE_REQUIRED` | `must be negative or zero` |

### Collection Constraints (Applicable to `List`, `Set`, `Collection`, `Iterable` and nullable equivalents)

| Annotation | Description | Error Code | Default Message |
| :--- | :--- | :--- | :--- |
| `@NotEmpty` | Collection must contain at least one element | `NOT_EMPTY` | `must not be empty` |
| `@Size(min, max)`| Element count must be between `min` and `max` inclusive| `SIZE_INVALID` | `size must be between min and max` |

### Enum Constraints (Applicable to Enum class properties or String properties)

| Annotation | Description | Error Code | Default Message |
| :--- | :--- | :--- | :--- |
| `@AllowedValues(val)`| Value must match one of the specified allowed values | `INVALID_ENUM_VALUE` | `value must be one of allowed values` |

### Date & Time Constraints (Applicable to `LocalDate`, `LocalDateTime`, `Instant`, `OffsetDateTime`)

| Annotation | Description | Error Code | Default Message |
| :--- | :--- | :--- | :--- |
| `@Past` | Date/time must be strictly in the past | `PAST_REQUIRED` | `must be in the past` |
| `@PastOrPresent`| Date/time must be in the past or present | `PAST_REQUIRED` | `must be in the past or present` |
| `@Future` | Date/time must be strictly in the future | `FUTURE_REQUIRED` | `must be in the future` |
| `@FutureOrPresent`| Date/time must be in the future or present | `FUTURE_REQUIRED` | `must be in the future or present` |

---

## ⚡ Strict Compile-Time Type Safety

Valix validates all annotations and their parameters during compilation. The compiler will immediately fail with a compilation error if:
- A constraint is applied to an unsupported type (e.g. `@MinLength` on `Int` or `@Past` on `Boolean`).
- Annotation arguments are invalid (e.g. `@Range(min = 100, max = 1)` or `@Size(min = 10, max = 2)`).

---

## 💡 Usage Example

### 1. Define your Data Classes

```kotlin
package com.example.user

import io.valix.annotations.*
import java.time.LocalDate

enum class UserRole {
    ADMIN, USER
}

data class Profile(
    @Url
    val website: String?,

    @Past
    val birthDate: LocalDate?
)

data class User(
    @Email
    val email: String,

    @Min(18)
    val age: Int,

    @AllowedValues(["ADMIN", "USER"])
    val role: UserRole,

    @NotEmpty
    @Size(min = 1, max = 5)
    val hobbies: List<String>?,

    @Valid
    val profile: Profile
)
```

### 2. Run Validation

```kotlin
package com.example.user

import com.example.user.generated.UserValidator

fun validateUser(user: User) {
    val result = UserValidator.validate(user)
    
    if (result.valid) {
        println("User is valid!")
    } else {
        result.errors.forEach { error ->
            println("Field: ${error.field}, Error: ${error.message}, Rejected: ${error.rejectedValue}")
        }
    }
}
```

---

## 🗃️ Global ValixRegistry

Valix compiles a reflection-free global registry `io.valix.generated.ValixRegistry` containing all compile-time registered validator mappings. This is extremely useful for running validation dynamically on generic inputs:

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

## 🚀 Migration Notes

- **Naming evolution**: `Valix` now generates `<ClassName>Validator` as the primary validator class.
- **Backward compatibility**: The older suffix `<ClassName>ValixValidator` continues to be generated and works as an alias delegating directly to `<ClassName>Validator`.
- **`ValidationError` upgrade**: `ValidationError` now contains `rejectedValue: Any?` to retrieve the invalid value, `constraint: String?` representing the FQN of the failed annotation, and `path: String` representing the nested property path. Legacy constructions continue to work seamlessly.

---

## 🛠️ Advanced Platform Features (Phase 4)

Valix Phase 4 introduces robust customization capabilities transforming it into a full validation platform.

### 1. Custom Property Validators
Create your own custom property annotations by applying the `@Constraint` meta-annotation pointing to a class implementing `ConstraintValidator<T>`:

```kotlin
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@Constraint(validator = UsernameValidator::class)
annotation class Username(
    val message: String = "invalid username",
    val groups: Array<KClass<*>> = []
)

class UsernameValidator : ConstraintValidator<String> {
    override fun validate(value: String, context: ValidationContext): Boolean {
        return value.matches(Regex("^[a-z0-9_]+$"))
    }
}
```

### 2. Validation Context
Custom validators receive a `ValidationContext` containing runtime information:
- `context.fieldName`: The simple name of the validated property.
- `context.path`: The fully qualified nested property path (e.g. `user.profile.website`).
- `context.rootObject`: The top-level class object being validated.
- `context.groups`: The active validation groups list.

### 3. Composed Meta-Annotations
Build reusable validation bundles (constraint composition) from existing annotations. The generator recursively expands composed meta-annotations and propagates message and groups:

```kotlin
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
@NotBlank
@MinLength(8)
@Pattern("^(?=.*[A-Z])(?=.*[0-9]).*$")
annotation class StrongPassword(
    val message: String = "must be a strong password",
    val groups: Array<KClass<*>> = []
)
```

### 4. Object-Level & Cross-Field Validation
Perform validations on class declarations using `ObjectConstraintValidator<T>`:

```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Constraint(validator = DateRangeValidator::class)
annotation class ValidDateRange(
    val message: String = "invalid date range",
    val groups: Array<KClass<*>> = []
)

class DateRangeValidator : ObjectConstraintValidator<Event> {
    override fun validate(value: Event, context: ValidationContext): Boolean {
        return !value.startDate.isAfter(value.endDate)
    }
}
```

### 5. Cross-Field Equality (`@FieldsMatch`)
Use the built-in `@FieldsMatch` class-level annotation to assert that two properties match:

```kotlin
@FieldsMatch(
    first = "password",
    second = "confirmPassword",
    message = "Passwords must match"
)
data class RegisterRequest(
    val email: String,
    val password: String,
    val confirmPassword: String
)
```

