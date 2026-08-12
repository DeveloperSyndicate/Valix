package io.valix.core

import kotlin.reflect.KClass

/**
 * Main contract for compiled Valix validators.
 *
 * Implementations of this interface are generated at compile time by KSP for classes
 * annotated with constraint annotations or custom validation logic.
 *
 * @param T The type of the target object being validated.
 */
interface ValixValidator<T> {
    /**
     * Validates the target instance against configured constraints.
     *
     * @param value The target object instance to validate.
     * @param groups Validation groups to evaluate. If empty, default constraints are evaluated.
     * @param failFast If `true`, validation terminates immediately upon encountering the first error.
     * @return [ValidationResult] containing overall validity status and any detected [ValidationError]s.
     */
    fun validate(value: T, vararg groups: KClass<*>, failFast: Boolean = false): ValidationResult

    /**
     * Asynchronously validates the target instance against configured constraints.
     *
     * @param value The target object instance to validate.
     * @param groups Validation groups to evaluate.
     * @param failFast If `true`, validation terminates immediately upon encountering the first error.
     * @return [ValidationResult] containing overall validity status and any detected [ValidationError]s.
     */
    suspend fun validateAsync(value: T, vararg groups: KClass<*>, failFast: Boolean = false): ValidationResult =
        validate(value, *groups, failFast = failFast)
}
