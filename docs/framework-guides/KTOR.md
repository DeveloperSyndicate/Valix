# Valix Ktor Integration Guide (`valix-ktor`)

## Overview

`valix-ktor` provides a lightweight plugin pipeline interceptor to validate request payloads in Ktor HTTP server routes.

---

## Installation

Add dependency to `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.developersyndicate.valix:valix-ktor:1.0.3")
}
```

---

## Usage

Install `ValixKtorPlugin` in your Ktor application:

```kotlin
fun Application.module() {
    install(ValixKtorPlugin) {
        onValidationError { call, errors ->
            call.respond(HttpStatusCode.BadRequest, mapOf("validationErrors" to errors))
        }
    }

    routing {
        post("/users") {
            val request = call.receive<CreateUserRequest>()
            // Payload is automatically validated before route execution!
            call.respond(HttpStatusCode.Created, userService.create(request))
        }
    }
}
```
