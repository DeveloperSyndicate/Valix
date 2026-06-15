package io.valix.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import io.valix.runtime.FormState
import io.valix.runtime.ValidationMode
import kotlin.reflect.KClass

class ComposeFormState<T>(
    private val initialValue: T,
    val validator: ValixValidator<T>,
    val validationMode: ValidationMode = ValidationMode.OnChange
) {
    private val delegate = FormState(initialValue, validator, validationMode)

    var value by mutableStateOf(initialValue)
        private set

    var validationResult by mutableStateOf(ValidationResult(true, emptyList()))
        private set

    var isDirty by mutableStateOf(false)
        private set

    var isTouched by mutableStateOf(false)
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    var isSubmitted by mutableStateOf(false)
        private set

    var dirtyFields by mutableStateOf(emptySet<String>())
        private set

    var touchedFields by mutableStateOf(emptySet<String>())
        private set

    val isValid: Boolean get() = validationResult.valid
    val errors: List<ValidationError> get() = validationResult.errors
    val fieldErrors: Map<String, ValidationError> get() = errors.associateBy { it.field }

    fun errorFor(field: String): ValidationError? {
        return errors.find { it.field == field }
    }

    fun onFieldChange(field: String, newValue: T) {
        value = newValue
        delegate.onFieldChange(field, newValue)
        syncState()
    }

    fun onFieldBlur(field: String) {
        delegate.onFieldBlur(field)
        syncState()
    }

    fun validate(vararg groups: KClass<*>): ValidationResult {
        val res = delegate.validate(*groups)
        syncState()
        return res
    }

    suspend fun submit(vararg groups: KClass<*>, onExecute: suspend (T) -> Unit) {
        isSubmitted = true
        delegate.submit(*groups) {
            isSubmitting = true
            try {
                onExecute(it)
            } finally {
                isSubmitting = false
            }
        }
        syncState()
    }

    fun reset(newValue: T = initialValue) {
        delegate.reset(newValue)
        syncState()
    }

    fun clearErrors() {
        delegate.clearErrors()
        syncState()
    }

    private fun syncState() {
        value = delegate.value
        validationResult = delegate.validationResult
        isDirty = delegate.isDirty
        isTouched = delegate.isTouched
        isSubmitting = delegate.isSubmitting
        isSubmitted = delegate.isSubmitted
        dirtyFields = delegate.dirtyFields.toSet()
        touchedFields = delegate.touchedFields.toSet()
    }
}

class ValidationState {
    var errors by mutableStateOf(emptyList<ValidationError>())
    var isDirty by mutableStateOf(false)
    var isTouched by mutableStateOf(false)
    var isSubmitting by mutableStateOf(false)

    val isValid: Boolean get() = errors.isEmpty()
    val fieldErrors: Map<String, ValidationError> get() = errors.associateBy { it.field }

    fun errorFor(field: String): ValidationError? {
        return errors.find { it.field == field }
    }
}

@Composable
fun <T> rememberValixForm(
    initialValue: T,
    validator: ValixValidator<T>,
    validationMode: ValidationMode = ValidationMode.OnChange
): ComposeFormState<T> {
    return remember(initialValue, validator, validationMode) {
        ComposeFormState(initialValue, validator, validationMode)
    }
}

@Composable
fun rememberValidationState(): ValidationState {
    return remember { ValidationState() }
}
