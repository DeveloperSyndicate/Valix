# Valix Spring Boot Integration Guide (`valix-spring`)

## Overview

The `valix-spring` adapter provides seamless integration with Spring Boot applications, auto-registering message resolvers and controller parameter validation.

---

## Installation

Add dependency to `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.developersyndicate.valix:valix-spring:1.0.2")
}
```

---

## 1. Auto-Configuration

`ValixSpringAutoConfiguration` automatically detects Spring's `MessageSource` bean and registers `SpringMessageResolver`. Validation error keys (e.g. `valix.notblank` or custom keys) are translated using standard Spring `messages.properties` files.

---

## 2. Controller Parameter Validation (`@ValidValix`)

Use `@ValidValix` on `@RequestBody` parameters inside `@RestController` endpoints:

```kotlin
@RestController
@RequestMapping("/api/users")
class UserController {

    @PostMapping
    fun createUser(@ValidValix @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userService.create(request))
    }
}
```

If validation fails, `ValixSpringAutoConfiguration` interceptors automatically convert errors into Spring's standard error response or throw `ValixValidationException`.
