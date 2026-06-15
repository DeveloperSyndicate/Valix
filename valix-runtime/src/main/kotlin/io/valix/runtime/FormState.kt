package io.valix.runtime

import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import kotlin.reflect.KClass

class FormState<T>(
    initialValue: T,
    val validator: ValixValidator<T>,
    val validationMode: ValidationMode = ValidationMode.OnChange
) {
    var value: T = initialValue
        private set

    var validationResult: ValidationResult = ValidationResult(true, emptyList())
        private set

    var isDirty: Boolean = false
        private set

    var isTouched: Boolean = false
        private set

    var isSubmitting: Boolean = false
        private set

    var isSubmitted: Boolean = false
        private set

    val dirtyFields = mutableSetOf<String>()
    val touchedFields = mutableSetOf<String>()

    val isValid: Boolean get() = validationResult.valid
    val errors: List<ValidationError> get() = validationResult.errors
    val fieldErrors: Map<String, ValidationError> get() = errors.associateBy { it.field }

    fun errorFor(field: String): ValidationError? {
        return errors.find { it.field == field }
    }

    fun onFieldChange(field: String, newValue: T) {
        value = newValue
        dirtyFields.add(field)
        isDirty = true
        if (validationMode == ValidationMode.OnChange) {
            validate()
        }
    }

    fun onFieldBlur(field: String) {
        touchedFields.add(field)
        isTouched = true
        if (validationMode == ValidationMode.OnBlur) {
            validate()
        }
    }

    fun validate(vararg groups: KClass<*>): ValidationResult {
        validationResult = validator.validate(value, *groups)
        return validationResult
    }

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

    fun clearErrors() {
        validationResult = ValidationResult(true, emptyList())
    }
}
