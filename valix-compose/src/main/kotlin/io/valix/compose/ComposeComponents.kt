package io.valix.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import io.valix.core.ValidationError

@Composable
fun ValidationMessage(
    error: ValidationError?,
    modifier: Modifier = Modifier
) {
    if (error != null) {
        Text(
            text = error.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}

@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: ValidationError?,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    onBlur: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = error != null,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        onBlur?.invoke()
                    }
                }
        )
        ValidationMessage(error = error)
    }
}
