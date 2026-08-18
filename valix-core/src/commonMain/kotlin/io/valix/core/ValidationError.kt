package io.valix.core

/**
 * Represents a single constraint violation error resulting from validation evaluation.
 *
 * @property field The simple name of the target property or field that failed validation.
 * @property code The unique error code identifier for the failed constraint (e.g. `"EMAIL"`, `"NOT_BLANK"`).
 * @property message The human-readable error description message.
 * @property rejectedValue The invalid value that triggered the validation error, or `null` if unavailable.
 * @property constraint The qualified constraint name or annotation descriptor.
 * @property path The full property navigation path (e.g. `"user.address.zipCode"` or `"items[0].id"`).
 * @property messageKey Optional translation key for localized error message resolution.
 */
data class ValidationError(
    val field: String,
    val code: String,
    val message: String,
    val rejectedValue: Any? = null,
    val constraint: String? = null,
    val path: String = field,
    val messageKey: String = ""
) {
    /** The resolved type-safe [ValixErrorCode] representation of the error code, if it matches a standard code. */
    val errorCode: ValixErrorCode? get() = ValixErrorCode.fromCode(code)
}

