# Frequently Asked Questions

This page addresses common developer questions about the design, execution model, and integrations of the Valix validation framework.

---

### What is Valix?
Valix is a Kotlin validation framework powered by KSP that generates type-safe validation logic at compile time, enabling reflection-free validation at runtime.

---

### Does Valix validate values at compile time?
**No.** 

* **At compile time:** Kotlin Symbol Processing (KSP) analyzes your data classes and generates plain Kotlin validator implementations (e.g. `UserValidator`).
* **At runtime:** The generated validator code executes against your actual runtime values (e.g. when processing a REST request or accepting form input).

The validation check does not run during compilation. Rather, the validation *code* is generated at build time, eliminating the need to parse annotations or inspect classes during runtime execution.

---

### Does Valix use reflection?
**No for generated validators.**

Because the validator is generated as plain Kotlin code (doing direct property accesses like `value.username` and running standard checks like `trim().isEmpty()`), Valix does not call runtime reflection APIs, parse annotations, or require reflection metadata caches. 

This makes Valix compatible with GraalVM Native Image compilation out-of-the-box without requiring complex reflection configurations (`reflect-config.json`) for your data models.

---

### How does Valix perform compared to Hibernate Validator?
In our comparative benchmarks:
* For simple flat structures, Valix typically achieves **7x to 9x the throughput** of Hibernate Validator.
* For complex nested object trees, Valix achieves **up to 15x the throughput** of Hibernate Validator.

This performance gain occurs because Hibernate Validator must recursively inspect fields and look up annotation metadata via reflection at runtime, whereas Valix executes direct, generated conditional checks.

*(For detailed configurations and reproducibility, see the [Performance Benchmarks Report](https://github.com/DeveloperSyndicate/Valix-Examples/blob/main/BENCHMARKS.md) in the examples repository).*

---

### Can I write custom constraints?
**Yes.** 

You can define custom validation rules by annotating your property and linking it to a class that implements `ConstraintValidator<T>`:

```kotlin
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validator = UsernameValidator::class)
annotation class ValidUsername(
    val message: String = "invalid username",
    val groups: Array<KClass<*>> = []
)

class UsernameValidator : ConstraintValidator<String> {
    override fun validate(value: String, context: ValidationContext): Boolean {
        return value.matches(Regex("^[a-z0-9_]+$"))
    }
}
```

---

### Does Valix support asynchronous validation (e.g. database lookups)?
**Yes.** 

Valix supports suspending validator configurations. You can implement `AsyncConstraintValidator<T>` to perform suspending executions:

```kotlin
class EmailUniquenessValidator : AsyncConstraintValidator<String> {
    override suspend fun validate(value: String, context: ValidationContext): Boolean {
        return userRepository.isEmailUnique(value) // Suspending DB call
    }
}
```
If any async constraints are present, Valix generates a suspending `validateAsync` function alongside the standard sync `validate` method.

---

### Which frameworks does Valix integrate with?
Valix provides official integration adapters for:
* **Spring Boot:** Auto-configures argument resolvers for validating `@RequestBody` parameters using `@ValidValix`.
* **Ktor:** A Ktor 3.x plugin to validate request payloads in routing paths.
* **Micronaut:** AOP method validation interceptors.
* **Jetpack Compose:** Lightweight form state tracking (`rememberValixForm`) for Android applications.
* **Kotlin Multiplatform:** Core validation execution is pure Kotlin and runs on JVM, Android, iOS, JS, and WebAssembly.
