# Valix Jetpack Compose Integration Guide (`valix-compose`)

## Overview

`valix-compose` binds Valix validation state directly into Jetpack Compose UI components.

---

## Installation

Add dependency to `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.developersyndicate.valix:valix-compose:1.0.2")
}
```

---

## Usage

```kotlin
@Composable
fun RegistrationFormScreen() {
    val formState = rememberValixForm(initialValue = RegisterForm(email = "", password = ""))

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = formState.value.email,
            onValueChange = { newEmail -> formState.update { copy(email = newEmail) } },
            label = { Text("Email") },
            isError = formState.hasFieldError("email")
        )
        formState.getFieldError("email")?.let { error ->
            Text(text = error.message, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (formState.validate()) {
                    // Submit valid form
                }
            }
        ) {
            Text("Submit")
        }
    }
}
```
