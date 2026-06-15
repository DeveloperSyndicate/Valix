package io.valix.core

data class ValidationError(
    val field: String,
    val code: String,
    val message: String,
    val rejectedValue: Any? = null,
    val constraint: String? = null,
    val path: String = field,
    val messageKey: String = ""
)
