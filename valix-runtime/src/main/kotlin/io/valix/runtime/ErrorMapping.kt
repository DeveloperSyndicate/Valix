package io.valix.runtime

import io.valix.core.ValidationError
import io.valix.core.ValidationResult

/** Groups validation errors by target field name. */
fun ValidationResult.errorsByField(): Map<String, List<ValidationError>> = errors.errorsByField()

/** Returns map of target field names to their first [ValidationError]. */
fun ValidationResult.fieldErrors(): Map<String, ValidationError> = errors.fieldErrors()

/** Returns the first error in the result, or `null`. */
fun ValidationResult.firstError(): ValidationError? = errors.firstError()

/** Returns list of all error messages in the result. */
fun ValidationResult.allMessages(): List<String> = errors.allMessages()

/** Groups validation errors in the list by target field name. */
fun List<ValidationError>.errorsByField(): Map<String, List<ValidationError>> {
    return groupBy { it.field }
}

/** Associates validation errors in the list by target field name. */
fun List<ValidationError>.fieldErrors(): Map<String, ValidationError> {
    return associateBy { it.field }
}

/** Returns the first error in the list, or `null`. */
fun List<ValidationError>.firstError(): ValidationError? {
    return firstOrNull()
}

/** Returns list of error message strings from the list of validation errors. */
fun List<ValidationError>.allMessages(): List<String> {
    return map { it.message }
}

/** Filters validation result errors matching the given field paths. */
fun ValidationResult.filterFields(vararg fields: String): ValidationResult {
    val filteredErrors = errors.filterFields(*fields)
    return ValidationResult(filteredErrors.isEmpty(), filteredErrors)
}

/** Filters error list for errors matching the target field names or subpaths. */
fun List<ValidationError>.filterFields(vararg fields: String): List<ValidationError> {
    val fieldSet = fields.toSet()
    return filter { error ->
        fieldSet.any { field ->
            error.field == field || error.field.startsWith("$field.") || error.field.startsWith("$field[")
        }
    }
}
