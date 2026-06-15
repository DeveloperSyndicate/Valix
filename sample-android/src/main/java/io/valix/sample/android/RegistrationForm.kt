package io.valix.sample.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.valix.compose.ValidatedTextField
import io.valix.compose.rememberValixForm
import io.valix.runtime.ValidationMode
import io.valix.sample.android.generated.RegisterRequestValidator
import kotlinx.coroutines.launch

@Composable
fun RegistrationForm(
    onRegisterSuccess: (RegisterRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val formState = rememberValixForm(
        initialValue = RegisterRequest("", "", "", ""),
        validator = RegisterRequestValidator,
        validationMode = ValidationMode.OnBlur
    )

    Column(modifier = modifier.padding(16.dp)) {
        Text("Registration Form (Validates on Blur)")
        Spacer(modifier = Modifier.height(8.dp))

        ValidatedTextField(
            value = formState.value.username,
            onValueChange = { username ->
                formState.onFieldChange("username", formState.value.copy(username = username))
            },
            onBlur = { formState.onFieldBlur("username") },
            error = formState.errorFor("username"),
            label = "Username"
        )

        Spacer(modifier = Modifier.height(8.dp))

        ValidatedTextField(
            value = formState.value.email,
            onValueChange = { email ->
                formState.onFieldChange("email", formState.value.copy(email = email))
            },
            onBlur = { formState.onFieldBlur("email") },
            error = formState.errorFor("email"),
            label = "Email"
        )

        Spacer(modifier = Modifier.height(8.dp))

        ValidatedTextField(
            value = formState.value.password,
            onValueChange = { password ->
                formState.onFieldChange("password", formState.value.copy(password = password))
            },
            onBlur = { formState.onFieldBlur("password") },
            error = formState.errorFor("password"),
            label = "Password"
        )

        Spacer(modifier = Modifier.height(8.dp))

        ValidatedTextField(
            value = formState.value.confirmPassword,
            onValueChange = { confirmPassword ->
                formState.onFieldChange("confirmPassword", formState.value.copy(confirmPassword = confirmPassword))
            },
            onBlur = { formState.onFieldBlur("confirmPassword") },
            error = formState.errorFor("confirmPassword"),
            label = "Confirm Password"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    formState.submit { request ->
                        onRegisterSuccess(request)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (formState.isSubmitting) "Registering..." else "Register")
        }
    }
}
