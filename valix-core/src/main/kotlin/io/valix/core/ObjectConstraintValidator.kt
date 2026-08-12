package io.valix.core

/**
 * Functional contract for evaluating multi-field or class-level object constraint rules.
 *
 * @param T The type of object instance being validated.
 */
interface ObjectConstraintValidator<T> {
    /**
     * Evaluates class-level cross-field constraints against the target object [value].
     *
     * @param value The object instance under validation.
     * @param context Navigation context holding field metadata and target context properties.
     * @return `true` if all class-level rules pass; `false` otherwise.
     */
    fun validate(value: T, context: ValidationContext): Boolean
}
