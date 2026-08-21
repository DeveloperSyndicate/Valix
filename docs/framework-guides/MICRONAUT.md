# How do I validate method parameters in Micronaut using compile-time generated logic?

This guide demonstrates how to integrate compile-time generated validation logic into Micronaut controllers and service beans without relying on runtime reflection.

---

### TL;DR: Quick Example

Annotate your Micronaut controller class with `@ValixValidated`:

```kotlin
import io.micronaut.http.annotation.*
import io.micronaut.http.HttpResponse
import io.valix.micronaut.ValixValidated

@Controller("/users")
@ValixValidated
class UserController(private val userService: UserService) {

    @Post
    fun createUser(@Body user: CreateUserRequest): HttpResponse<UserResponse> {
        // Controller inputs are validated by Valix before execution begins
        return HttpResponse.created(userService.create(user))
    }
}
```

---

## Step-by-Step Integration

### 1. Add Dependencies

Add the Micronaut adapter and the KSP annotation processor to your `build.gradle.kts` configuration:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.9"
}

dependencies {
    // Core runtime and Micronaut adapter dependencies
    implementation("com.developersyndicate.valix:valix-core:1.0.5")
    implementation("com.developersyndicate.valix:valix-runtime:1.0.5")
    implementation("com.developersyndicate.valix:valix-micronaut:1.0.5")
    
    // KSP generator
    ksp("com.developersyndicate.valix:valix-ksp:1.0.5")
}
```

### 2. Execution Behavior

The `@ValixValidated` annotation hooks into Micronaut's AOP interceptor pipeline:
* When a validated controller action is called, `ValixValidatedInterceptor` intercepts the call parameters.
* Validates properties against compiled model structures without running runtime reflection checks.
* Throws `ValixValidationException` if constraints are violated, allowing you to catch errors centrally using a standard Micronaut `ExceptionHandler` mapping.
