# Advanced Valix Validation Features

## 1. Sensitive Data Masking (`@Sensitive`)

Protect sensitive values (e.g. passwords, SSNs, credit cards) from being leaked in validation error logs:

```kotlin
data class UserCredentials(
    val username: String,

    @Sensitive(mask = "[REDACTED]")
    @MinLength(8)
    val password: String
)
```

When validation fails on `password`, `ValidationError.rejectedValue` contains `"[REDACTED]"` instead of the actual plaintext password.

---

## 2. Conditional Validation (`@ValidateIf`)

Validate a property only when a sibling property condition holds true:

```kotlin
data class PaymentRequest(
    val paymentType: String,

    @ValidateIf(field = "paymentType", equals = "CARD")
    @NotBlank(message = "Card number required")
    val cardNumber: String?
)
```

If `paymentType == "CASH"`, validation on `cardNumber` is skipped entirely.

---

## 3. Fail-Fast Execution Mode (`failFast = true`)

By default, Valix evaluates all constraints to collect every validation error. In high-throughput APIs, pass `failFast = true` to terminate evaluation immediately upon encountering the first error:

```kotlin
val result = CreateUserRequestValidator.validate(request, failFast = true)
```

---

## 4. Programmatic Builder DSL (`valixDsl`)

For third-party models or domain DTOs that cannot be annotated directly:

```kotlin
val ThirdPartyUserValidator = valixDsl<ThirdPartyUser> {
    field("name", ThirdPartyUser::name) {
        notBlank()
    }
    field("email", ThirdPartyUser::email) {
        email()
    }
    field("age", ThirdPartyUser::age) {
        min(18)
    }
}

val result = ThirdPartyUserValidator.validate(user)
```

---

## 5. Schema Export (OpenAPI 3.1 & JSON Schema)

Valix can automatically export draft-07 JSON Schemas and OpenAPI 3.1 YAML descriptors directly from annotated Kotlin data models using `valix-schema`:

```kotlin
val jsonSchema = ValixJsonSchemaGenerator.generate(CreateUserRequest::class)
val openApiYaml = ValixOpenApiGenerator.generateYaml(CreateUserRequest::class)
```
