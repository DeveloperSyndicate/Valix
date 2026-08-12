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

/**
 * Compose-backed reactive form state holder for managing field value changes and validation result state.
 *
 * All properties are backed by Compose [mutableStateOf] snapshot state.
 *
 * @param T The type of value or model object under validation.
 * @param initialValue The starting model value.
 * @param validator Compiled [ValixValidator] instance used to validate the model.
 * @param validationMode Strategy determining when validation is executed.
 */
class ComposeFormState<T>(
    private val initialValue: T,
    val validator: ValixValidator<T>,
    val validationMode: ValidationMode = ValidationMode.OnChange
) {
    private val delegate = FormState(initialValue, validator, validationMode)

    /** Current form model value. */
    var value by mutableStateOf(initialValue)
        private set

    /** Current Compose snapshot [ValidationResult]. */
    var validationResult by mutableStateOf(ValidationResult(true, emptyList()))
        private set

    /** `true` if form data has been modified. */
    var isDirty by mutableStateOf(false)
        private set

    /** `true` if any field has lost focus. */
    var isTouched by mutableStateOf(false)
        private set

    /** `true` while submission callback is executing. */
    var isSubmitting by mutableStateOf(false)
        private set

    /** `true` if form submission has been attempted. */
    var isSubmitted by mutableStateOf(false)
        private set

    /** Set of modified property names. */
    var dirtyFields by mutableStateOf(emptySet<String>())
        private set

    /** Set of property names that lost focus. */
    var touchedFields by mutableStateOf(emptySet<String>())
        private set

    /** Returns `true` if validation result contains zero errors. */
    val isValid: Boolean get() = validationResult.valid

    /** Returns list of current validation errors. */
    val errors: List<ValidationError> get() = validationResult.errors

    /** Returns validation errors indexed by property field name. */
    val fieldErrors: Map<String, ValidationError> get() = errors.associateBy { it.field }

    /** Returns first [ValidationError] matching the given property field name, or `null`. */
    fun errorFor(field: String): ValidationError? {
        return errors.find { it.field == field }
    }

    /** Triggers field update and re-evaluates validation if mode is [ValidationMode.OnChange]. */
    fun onFieldChange(field: String, newValue: T) {
        value = newValue
        delegate.onFieldChange(field, newValue)
        syncState()
    }

    /** Triggers field blur event and re-evaluates validation if mode is [ValidationMode.OnBlur]. */
    fun onFieldBlur(field: String) {
        delegate.onFieldBlur(field)
        syncState()
    }

    /** Re-evaluates validation rules manually for specified groups. */
    fun validate(vararg groups: KClass<*>): ValidationResult {
        val res = delegate.validate(*groups)
        syncState()
        return res
    }

    /** Triggers form submission and calls [onExecute] if valid. */
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

    /** Resets form state back to initial state or [newValue]. */
    fun reset(newValue: T = initialValue) {
        delegate.reset(newValue)
        syncState()
    }

    /** Clears active errors. */
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

/**
 * Lightweight state object for standalone validation error tracking in Compose.
 */
class ValidationState {
    /** Active list of validation errors. */
    var errors by mutableStateOf(emptyList<ValidationError>())

    /** `true` if form value is modified. */
    var isDirty by mutableStateOf(false)

    /** `true` if field has lost focus. */
    var isTouched by mutableStateOf(false)

    /** `true` while submission is pending. */
    var isSubmitting by mutableStateOf(false)

    /** Returns `true` if error list is empty. */
    val isValid: Boolean get() = errors.isEmpty()

    /** Returns map of property field names to validation errors. */
    val fieldErrors: Map<String, ValidationError> get() = errors.associateBy { it.field }

    /** Returns error for specified property name, or `null`. */
    fun errorFor(field: String): ValidationError? {
        return errors.find { it.field == field }
    }
}

/**
 * Creates and remembers a [ComposeFormState] across recompositions.
 */
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

/**
 * Creates and remembers a standalone [ValidationState] across recompositions.
 */
@Composable
fun rememberValidationState(): ValidationState {
    return remember { ValidationState() }
}
