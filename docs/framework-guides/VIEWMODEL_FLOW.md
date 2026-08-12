# Valix ViewModel & Coroutines Flow Integration Guide

## Overview

Integrates Valix validation state with Android Architecture Components `ViewModel` (`valix-viewmodel`) and reactive `Flow` streams (`valix-flow`).

---

## 1. ViewModel Integration (`valix-viewmodel`)

```kotlin
class RegistrationViewModel : ValixFormViewModel<RegisterForm>(
    initialState = RegisterForm(email = "", password = "")
) {
    fun onEmailChanged(email: String) {
        updateState { copy(email = email) }
    }

    fun submit() {
        if (validate()) {
            // State is valid!
        }
    }
}
```

---

## 2. Coroutines Flow Integration (`valix-flow`)

Use `validateWith` operator on Kotlin `Flow` streams:

```kotlin
userRegistrationFlow
    .validateWith()
    .collect { (user, validationResult) ->
        if (validationResult.valid) {
            processRegistration(user)
        } else {
            handleErrors(validationResult.errors)
        }
    }
```
