package io.valix.flow

import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

fun <T> Flow<T>.validateWith(
    validator: ValixValidator<T>,
    vararg groups: KClass<*>
): Flow<ValidationResult> {
    return this.map { validator.validate(it, *groups) }
}
