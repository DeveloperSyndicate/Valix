package io.valix.core

data class ValidationError(
    val field: String,
    val code: String,
    val message: String
)
