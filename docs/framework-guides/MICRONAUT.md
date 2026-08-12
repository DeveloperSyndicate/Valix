# Valix Micronaut Integration Guide (`valix-micronaut`)

## Overview

`valix-micronaut` provides AOP interceptor advice (`@ValixValidated`) for Micronaut service beans and controllers.

---

## Installation

Add dependency to `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.developersyndicate.valix:valix-micronaut:1.0.3")
}
```

---

## Usage

Annotate your Micronaut controller or service class with `@ValixValidated`:

```kotlin
@Controller("/users")
@ValixValidated
class UserController(private val userService: UserService) {

    @Post
    fun createUser(@Body user: CreateUserRequest): HttpResponse<UserResponse> {
        return HttpResponse.created(userService.create(user))
    }
}
```
