package io.valix.spring

import io.valix.core.ValidationResult

class ValixValidationException(
    val validationResult: ValidationResult
) : RuntimeException("Validation failed: ${validationResult.errors.joinToString { it.message }}")
