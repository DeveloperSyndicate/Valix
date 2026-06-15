package io.valix.runtime

import io.valix.core.ValidationError
import io.valix.core.ValidationResult

fun ValidationResult.errorsByField(): Map<String, List<ValidationError>> = errors.errorsByField()
fun ValidationResult.fieldErrors(): Map<String, ValidationError> = errors.fieldErrors()
fun ValidationResult.firstError(): ValidationError? = errors.firstError()
fun ValidationResult.allMessages(): List<String> = errors.allMessages()

fun List<ValidationError>.errorsByField(): Map<String, List<ValidationError>> {
    return groupBy { it.field }
}

fun List<ValidationError>.fieldErrors(): Map<String, ValidationError> {
    return associateBy { it.field }
}

fun List<ValidationError>.firstError(): ValidationError? {
    return firstOrNull()
}

fun List<ValidationError>.allMessages(): List<String> {
    return map { it.message }
}

fun ValidationResult.filterFields(vararg fields: String): ValidationResult {
    val filteredErrors = errors.filterFields(*fields)
    return ValidationResult(filteredErrors.isEmpty(), filteredErrors)
}

fun List<ValidationError>.filterFields(vararg fields: String): List<ValidationError> {
    val fieldSet = fields.toSet()
    return filter { error ->
        fieldSet.any { field ->
            error.field == field || error.field.startsWith("$field.") || error.field.startsWith("$field[")
        }
    }
}
