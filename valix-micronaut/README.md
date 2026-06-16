# Valix Micronaut Integration

This module provides reflection-free validation for Micronaut applications using Micronaut AOP.

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
implementation("com.developersyndicate.valix:valix-micronaut:1.0.1")
```

## Usage

Annotate your controller or service classes/methods with `@ValixValidated`:

```kotlin
import io.valix.micronaut.ValixValidated
import jakarta.inject.Singleton

@Singleton
open class UserService {

    @ValixValidated
    open fun createUser(request: CreateUserRequest) {
        // request is automatically validated before this body executes
    }
}
```

If validation fails, a `ValixMicronautValidationException` is thrown, which contains the detailed validation errors.
You can catch and handle this using a Micronaut `ExceptionHandler`.
