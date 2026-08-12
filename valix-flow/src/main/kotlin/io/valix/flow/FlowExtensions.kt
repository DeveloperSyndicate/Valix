package io.valix.flow

import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

/**
 * Transforms a reactive [Flow] of model values into a [Flow] emitting [ValidationResult] evaluations.
 *
 * @param T The type of data value contained in the flow.
 * @param validator The [ValixValidator] compiled validator instance.
 * @param groups Optional validation groups to evaluate.
 * @return A [Flow] emitting a [ValidationResult] for each incoming item.
 */
fun <T> Flow<T>.validateWith(
    validator: ValixValidator<T>,
    vararg groups: KClass<*>
): Flow<ValidationResult> {
    return this.map { validator.validate(it, *groups) }
}
