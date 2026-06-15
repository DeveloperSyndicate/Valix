package io.valix.core

import kotlin.reflect.KClass

interface ValixValidator<T> {
    fun validate(value: T, vararg groups: KClass<*>): ValidationResult
}
