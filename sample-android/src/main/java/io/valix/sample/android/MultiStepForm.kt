package io.valix.sample.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.valix.compose.ValidatedTextField
import io.valix.compose.rememberValixForm
import io.valix.runtime.ValidationMode
import io.valix.runtime.filterFields
import io.valix.sample.android.generated.MultiStepRequestValidator

@Composable
fun MultiStepForm(
    onFormComplete: (MultiStepRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) }
    val formState = rememberValixForm(
        initialValue = MultiStepRequest("", "", "", ""),
        validator = MultiStepRequestValidator,
        validationMode = ValidationMode.OnChange
    )

    Column(modifier = modifier.padding(16.dp)) {
        Text("Multi-step Form - Step $step of 2")
        Spacer(modifier = Modifier.height(16.dp))

        if (step == 1) {
            // Step 1: Personal Info
            ValidatedTextField(
                value = formState.value.firstName,
                onValueChange = { formState.onFieldChange("firstName", formState.value.copy(firstName = it)) },
                error = formState.errorFor("firstName"),
                label = "First Name"
            )
            Spacer(modifier = Modifier.height(8.dp))
            ValidatedTextField(
                value = formState.value.lastName,
                onValueChange = { formState.onFieldChange("lastName", formState.value.copy(lastName = it)) },
                error = formState.errorFor("lastName"),
                label = "Last Name"
            )
        } else {
            // Step 2: Contact Info
            ValidatedTextField(
                value = formState.value.email,
                onValueChange = { formState.onFieldChange("email", formState.value.copy(email = it)) },
                error = formState.errorFor("email"),
                label = "Email"
            )
            Spacer(modifier = Modifier.height(8.dp))
            ValidatedTextField(
                value = formState.value.phone,
                onValueChange = { formState.onFieldChange("phone", formState.value.copy(phone = it)) },
                error = formState.errorFor("phone"),
                label = "Phone"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (step == 1) {
                    // Validate subset of fields for step 1
                    val validation = formState.validate()
                    val step1Result = validation.filterFields("firstName", "lastName")
                    if (step1Result.valid) {
                        step = 2
                    }
                } else {
                    // Full form validation
                    val validation = formState.validate()
                    if (validation.valid) {
                        onFormComplete(formState.value)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (step == 1) "Next" else "Submit")
        }
    }
}
