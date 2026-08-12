package io.valix.core

/**
 * Container holding the outcome of a validation operation performed by a [ValixValidator].
 *
 * @property valid `true` if the target object satisfied all evaluated constraints; `false` otherwise.
 * @property errors List of [ValidationError] instances detailing any detected constraint violations.
 */
data class ValidationResult(
    val valid: Boolean,
    val errors: List<ValidationError>
)
