package io.valix.core

/**
 * Functional contract for evaluating single field constraint rules.
 *
 * @param T The type of value being validated.
 */
interface ConstraintValidator<T> {
    /**
     * Evaluates the constraint rule against the given [value].
     *
     * @param value The property value under validation.
     * @param context Navigation context holding metadata regarding the property and parent root.
     * @return `true` if valid; `false` if the value violates the constraint rule.
     */
    fun validate(value: T, context: ValidationContext): Boolean
}
