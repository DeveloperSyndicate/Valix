package io.valix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import io.valix.runtime.FormState
import io.valix.runtime.ValidationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Android Architecture Components [ViewModel] base class for managing form state and validation using Kotlin Coroutines `StateFlow`.
 *
 * @param T The form value or model class type.
 * @param initialValue The starting model value.
 * @param validator The [ValixValidator] instance used to validate model data.
 * @param validationMode Strategy determining when validation executes automatically.
 */
open class ValixFormViewModel<T>(
    private val initialValue: T,
    val validator: ValixValidator<T>,
    val validationMode: ValidationMode = ValidationMode.OnChange
) : ViewModel() {

    private val _formState = FormState(initialValue, validator, validationMode)

    private val _value = MutableStateFlow(initialValue)

    /** Reactive [StateFlow] holding current model value. */
    val value: StateFlow<T> = _value.asStateFlow()

    private val _errors = MutableStateFlow<List<ValidationError>>(emptyList())

    /** Reactive [StateFlow] holding active validation errors. */
    val errors: StateFlow<List<ValidationError>> = _errors.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)

    /** Reactive [StateFlow] indicating whether async submission is currently executing. */
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isSubmitted = MutableStateFlow(false)

    /** Reactive [StateFlow] indicating whether form submission was attempted. */
    val isSubmitted: StateFlow<Boolean> = _isSubmitted.asStateFlow()

    /** Reactive [StateFlow] evaluating to `true` when zero validation errors exist. */
    val isValid: StateFlow<Boolean> by lazy {
        _errors.map { it.isEmpty() }.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    }

    /** Updates the entire model value and evaluates validation rules. */
    fun updateValue(newValue: T) {
        _value.value = newValue
        _formState.onFieldChange("", newValue)
        if (validationMode == ValidationMode.OnChange) {
            validate()
        }
    }

    /** Updates property value for field and updates error state. */
    fun onFieldChange(field: String, newValue: T) {
        _value.value = newValue
        _formState.onFieldChange(field, newValue)
        _errors.value = _formState.errors
    }

    /** Handles focus blur events for a field. */
    fun onFieldBlur(field: String) {
        _formState.onFieldBlur(field)
        _errors.value = _formState.errors
    }

    /** Manually triggers validation for active groups. */
    fun validate(vararg groups: KClass<*>): ValidationResult {
        val result = _formState.validate(*groups)
        _errors.value = result.errors
        return result
    }

    /** Validates form and launches [onExecute] within [viewModelScope] if valid. */
    fun submit(vararg groups: KClass<*>, onExecute: suspend (T) -> Unit) {
        _isSubmitted.value = true
        val result = validate(*groups)
        if (result.valid) {
            _isSubmitting.value = true
            viewModelScope.launch {
                try {
                    onExecute(_value.value)
                } finally {
                    _isSubmitting.value = false
                }
            }
        }
    }

    /** Resets ViewModel form state back to initial values. */
    fun reset() {
        _formState.reset(initialValue)
        _value.value = initialValue
        _errors.value = emptyList()
        _isSubmitting.value = false
        _isSubmitted.value = false
    }

    /** Clears active errors. */
    fun clearErrors() {
        _formState.clearErrors()
        _errors.value = emptyList()
    }

    /** Returns [StateFlow] of validation errors. */
    fun observeValidation(): StateFlow<List<ValidationError>> = errors
}
