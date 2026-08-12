package io.valix.runtime

import io.valix.core.ValidationContext

/**
 * Suspending constraint validator for executing asynchronous validation rules (e.g. database lookups, remote API checks).
 *
 * @param T The property value type under validation.
 */
interface AsyncConstraintValidator<T> {
    /**
     * Asynchronously evaluates the constraint rule.
     *
     * @param value Property value under evaluation.
     * @param context Current navigation context.
     * @return `true` if valid; `false` otherwise.
     */
    suspend fun validate(value: T, context: ValidationContext): Boolean
}
