package io.valix.runtime

import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import kotlin.reflect.KClass

/**
 * Manages mutable form state and validation status for a target value object [T].
 *
 * Provides fields tracking (`dirtyFields`, `touchedFields`), validation triggers (`onFieldChange`, `onFieldBlur`),
 * and async form submission helpers.
 *
 * @param T The type of value or data class held by the form state.
 * @param initialValue The starting value of the form.
 * @param validator The [ValixValidator] compiled instance used to validate form data.
 * @param validationMode Strategy determining when validation is triggered ([ValidationMode.OnChange], [ValidationMode.OnBlur], etc.).
 * @param failFast If `true`, validation terminates immediately upon encountering the first error.
 */
class FormState<T>(
    initialValue: T,
    val validator: ValixValidator<T>,
    val validationMode: ValidationMode = ValidationMode.OnChange,
    val failFast: Boolean = false
) {
    /** The current value instance. */
    var value: T = initialValue
        private set

    /** The current [ValidationResult]. */
    var validationResult: ValidationResult = ValidationResult(true, emptyList())
        private set

    /** `true` if any field value has been modified since initialization or reset. */
    var isDirty: Boolean = false
        private set

    /** `true` if any field has lost focus. */
    var isTouched: Boolean = false
        private set

    /** `true` while the form submission callback is currently executing. */
    var isSubmitting: Boolean = false
        private set

    /** `true` if the form submission has been triggered. */
    var isSubmitted: Boolean = false
        private set

    /** Set of field names that have been modified. */
    val dirtyFields = mutableSetOf<String>()

    /** Set of field names that have lost focus. */
    val touchedFields = mutableSetOf<String>()

    /** Returns `true` if current validation result is valid. */
    val isValid: Boolean get() = validationResult.valid

    /** Returns list of current validation errors. */
    val errors: List<ValidationError> get() = validationResult.errors

    /** Returns current errors indexed by property field name. */
    val fieldErrors: Map<String, ValidationError> get() = errors.associateBy { it.field }

    /** Returns the first [ValidationError] for the specified field, or `null`. */
    fun errorFor(field: String): ValidationError? {
        return errors.find { it.field == field }
    }

    /** Triggers field update and executes validation if mode is [ValidationMode.OnChange]. */
    fun onFieldChange(field: String, newValue: T) {
        value = newValue
        dirtyFields.add(field)
        isDirty = true
        if (validationMode == ValidationMode.OnChange) {
            validate()
        }
    }

    /** Triggers field blur tracking and executes validation if mode is [ValidationMode.OnBlur]. */
    fun onFieldBlur(field: String) {
        touchedFields.add(field)
        isTouched = true
        if (validationMode == ValidationMode.OnBlur) {
            validate()
        }
    }

    /** Manually triggers validation for the specified groups. */
    fun validate(vararg groups: KClass<*>): ValidationResult {
        validationResult = validator.validate(value, *groups, failFast = failFast)
        return validationResult
    }

    /** Validates and executes [onExecute] if validation passes. */
    suspend fun submit(vararg groups: KClass<*>, onExecute: suspend (T) -> Unit) {
        isSubmitted = true
        validate(*groups)
        if (isValid) {
            isSubmitting = true
            try {
                onExecute(value)
            } finally {
                isSubmitting = false
            }
        }
    }

    /** Resets the form state to [initialValue] and clears all error/dirty state. */
    fun reset(initialValue: T) {
        value = initialValue
        validationResult = ValidationResult(true, emptyList())
        isDirty = false
        isTouched = false
        isSubmitting = false
        isSubmitted = false
        dirtyFields.clear()
        touchedFields.clear()
    }

    /** Clears all validation errors while keeping values intact. */
    fun clearErrors() {
        validationResult = ValidationResult(true, emptyList())
    }
}
