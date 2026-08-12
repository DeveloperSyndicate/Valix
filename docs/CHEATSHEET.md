# Valix Developer Cheatsheet

## 1. Quick Reference: Annotations

### String Annotations
| Annotation | Parameters | Description |
| :--- | :--- | :--- |
| `@NotNull` | `message`, `groups` | Value must not be `null`. |
| `@NotBlank` | `message`, `groups` | String must not be empty or blank spaces. |
| `@NotEmpty` | `message`, `groups` | String length must be > 0. |
| `@Email` | `message`, `groups` | String must match standard email pattern. |
| `@MinLength` | `value: Int`, `message`, `groups` | Minimum character length. |
| `@MaxLength` | `value: Int`, `message`, `groups` | Maximum character length. |
| `@Pattern` | `regex: String`, `message`, `groups` | Regular expression match. |
| `@Url` | `message`, `groups` | Must be a valid URL string. |
| `@PhoneNumber` | `message`, `groups` | Must be a valid phone number. |
| `@Alpha` | `message`, `groups` | Must contain only alphabetic characters. |
| `@AlphaNumeric` | `message`, `groups` | Must contain only alphanumeric characters. |
| `@LowerCase` | `message`, `groups` | Must be completely lowercase. |
| `@UpperCase` | `message`, `groups` | Must be completely uppercase. |
| `@Contains` | `value: String`, `message`, `groups` | Must contain substring `value`. |
| `@StartsWith` | `value: String`, `message`, `groups` | Must start with prefix `value`. |
| `@EndsWith` | `value: String`, `message`, `groups` | Must end with suffix `value`. |

### Numeric Annotations (`Int`, `Long`, `Float`, `Double`, `Short`)
| Annotation | Parameters | Description |
| :--- | :--- | :--- |
| `@Min` | `value: Long`, `message`, `groups` | Value must be >= `value`. |
| `@Max` | `value: Long`, `message`, `groups` | Value must be <= `value`. |
| `@Range` | `min: Long`, `max: Long`, `message`, `groups` | Value must be between `min` and `max`. |
| `@Positive` | `message`, `groups` | Value must be > 0. |
| `@PositiveOrZero` | `message`, `groups` | Value must be >= 0. |
| `@Negative` | `message`, `groups` | Value must be < 0. |
| `@NegativeOrZero` | `message`, `groups` | Value must be <= 0. |

### Collection & Enum Annotations
| Annotation | Parameters | Description |
| :--- | :--- | :--- |
| `@NotEmpty` | `message`, `groups` | Collection must contain at least 1 element. |
| `@Size` | `min: Int`, `max: Int`, `message`, `groups` | Collection element count between `min` and `max`. |
| `@AllowedValues` | `value: Array<String>`, `message`, `groups` | Property must match one of allowed string values. |

### Date & Time Annotations
| Annotation | Parameters | Description |
| :--- | :--- | :--- |
| `@Past` | `message`, `groups` | Date/time must be strictly in the past. |
| `@PastOrPresent` | `message`, `groups` | Date/time must be in the past or present. |
| `@Future` | `message`, `groups` | Date/time must be strictly in the future. |
| `@FutureOrPresent` | `message`, `groups` | Date/time must be in the future or present. |

### Nested & Class-Level Annotations
| Annotation | Description |
| :--- | :--- |
| `@Valid` | Triggers recursive validation on nested object or collection items. |
| `@FieldsMatch` | Class-level annotation verifying two fields match (e.g. `password` & `confirmPassword`). |

---

## 2. Advanced Feature Cheatsheet

### Sensitive Data Redaction
```kotlin
data class UserCredentials(
    @Sensitive(mask = "[REDACTED]")
    @MinLength(8)
    val password: String
)
```

### Conditional Validation
```kotlin
data class Payment(
    val type: String,
    @ValidateIf(field = "type", equals = "CARD")
    @NotBlank
    val cardNumber: String?
)
```

### Fail-Fast Execution
```kotlin
val result = UserValidator.validate(user, failFast = true)
```

### Dynamic Parameter Interpolation
```kotlin
data class Profile(
    @MinLength(value = 8, message = "Minimum required length is {min}")
    val username: String
)
```

### Programmatic Builder DSL (`valixDsl`)
```kotlin
val validator = valixDsl<DomainUser> {
    field("email", DomainUser::email) {
        notBlank()
        email()
    }
    field("age", DomainUser::age) {
        min(18)
    }
}
```
