package io.valix.core

data class ValidationResult(
    val valid: Boolean,
    val errors: List<ValidationError>
)
