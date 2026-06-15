package io.valix.sample.android

import io.valix.runtime.ValidationMode
import io.valix.viewmodel.ValixFormViewModel
import io.valix.sample.android.generated.LoginRequestValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel : ValixFormViewModel<LoginRequest>(
    initialValue = LoginRequest("", ""),
    validator = LoginRequestValidator,
    validationMode = ValidationMode.OnChange
) {
    private val _loginStatus = MutableStateFlow<String>("")
    val loginStatus: StateFlow<String> = _loginStatus.asStateFlow()

    fun performLogin() {
        submit { request ->
            _loginStatus.value = "Login successful for ${request.email}"
        }
    }
}
