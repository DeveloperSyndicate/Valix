# How do I validate request bodies in Spring Boot using Valix?

This guide demonstrates how to integrate compile-time generated validation logic into your Spring Boot application without relying on runtime reflection.

---

### TL;DR: Quick Example

Annotate your `@RestController` parameters with `@ValidValix`:

```kotlin
import io.valix.spring.ValidValix
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController {

    @PostMapping
    fun createUser(@ValidValix @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        // request properties are validated using Valix before this block executes
        return ResponseEntity.ok(userService.create(request))
    }
}
```

---

## Step-by-Step Integration

### 1. Add Dependencies

Add the Spring Boot adapter and the KSP annotation processor to your `build.gradle.kts` configuration:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.9"
}

dependencies {
    // Core runtime and Spring adapter dependencies
    implementation("com.developersyndicate.valix:valix-core:1.0.5")
    implementation("com.developersyndicate.valix:valix-runtime:1.0.5")
    implementation("com.developersyndicate.valix:valix-spring:1.0.5")
    
    // KSP generator
    ksp("com.developersyndicate.valix:valix-ksp:1.0.5")
}
```

### 2. Auto-Configuration Mechanics

`ValixSpringAutoConfiguration` configures the integration automatically:
* **Localization:** Resolves Spring's active `MessageSource` bean, mapping `ValidationError.messageKey` parameters dynamically via standard `messages.properties` files.
* **Aspect Interceptors:** Intercepts endpoints annotated with `@ValidValix`. If validation fails, it collects errors and translates them, throwing a `ValixValidationException` containing structural target paths.
