package io.valix.core

interface ConstraintValidator<T> {
    fun validate(value: T, context: ValidationContext): Boolean
}
