package io.valix.runtime

import io.valix.core.ValidationContext

interface AsyncConstraintValidator<T> {
    suspend fun validate(value: T, context: ValidationContext): Boolean
}
