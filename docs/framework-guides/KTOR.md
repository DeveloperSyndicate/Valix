# How do I validate JSON request bodies in Ktor without reflection?

This guide demonstrates how to integrate compile-time generated validation logic into your Ktor server application using a lightweight pipeline plugin.

---

### TL;DR: Quick Example

Install the `ValixKtorPlugin` inside your Ktor application configuration:

```kotlin
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.valix.ktor.ValixKtorPlugin

fun Application.module() {
    install(ValixKtorPlugin) {
        onValidationError { call, errors ->
            // Customize error response payloads
            call.respond(HttpStatusCode.BadRequest, mapOf("validationErrors" to errors))
        }
    }

    routing {
        post("/users") {
            // Incoming request payload is automatically validated before block execution!
            val request = call.receive<CreateUserRequest>()
            call.respond(HttpStatusCode.Created, userService.create(request))
        }
    }
}
```

---

## Step-by-Step Integration

### 1. Add Dependencies

Add the Ktor adapter and the KSP annotation processor to your `build.gradle.kts` configuration:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.9"
}

dependencies {
    // Core runtime and Ktor adapter dependencies
    implementation("com.developersyndicate.valix:valix-core:1.0.5")
    implementation("com.developersyndicate.valix:valix-runtime:1.0.5")
    implementation("com.developersyndicate.valix:valix-ktor:1.0.5")
    
    // KSP generator
    ksp("com.developersyndicate.valix:valix-ksp:1.0.5")
}
```

### 2. Execution Behavior

When Ktor deserializes incoming JSON payloads via ContentNegotiation, `ValixKtorPlugin` interceptors:
* Locate the generated validator class dynamically using the global `ValixRegistry` mapper.
* Run the validator's `validate(value)` logic.
* Skip route execution and invoke the `onValidationError` fallback if any schema violations are detected, preventing invalid payloads from reaching controller blocks.
