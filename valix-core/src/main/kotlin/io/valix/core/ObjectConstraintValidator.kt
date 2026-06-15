package io.valix.core

interface ObjectConstraintValidator<T> {
    fun validate(value: T, context: ValidationContext): Boolean
}
