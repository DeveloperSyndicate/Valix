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
import io.valix.sample.android.generated.LoginRequestValidator
import kotlinx.coroutines.launch

@Composable
fun LoginForm(
    onLoginSuccess: (LoginRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val formState = rememberValixForm(
        initialValue = LoginRequest("", ""),
        validator = LoginRequestValidator,
        validationMode = ValidationMode.OnChange
    )

    Column(modifier = modifier.padding(16.dp)) {
        Text("Login Form")
        Spacer(modifier = Modifier.height(8.dp))

        ValidatedTextField(
            value = formState.value.email,
            onValueChange = { email ->
                formState.onFieldChange("email", formState.value.copy(email = email))
            },
            error = formState.errorFor("email"),
            label = "Email",
            placeholder = "Enter your email"
        )

        Spacer(modifier = Modifier.height(8.dp))

        ValidatedTextField(
            value = formState.value.password,
            onValueChange = { password ->
                formState.onFieldChange("password", formState.value.copy(password = password))
            },
            error = formState.errorFor("password"),
            label = "Password",
            placeholder = "Enter your password"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    formState.submit { request ->
                        onLoginSuccess(request)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (formState.isSubmitting) "Logging in..." else "Login")
        }
    }
}
