package io.valix.annotations

/**
 * Marks a property as containing sensitive data (e.g. passwords, SSNs, credit card numbers).
 *
 * When validation fails on a sensitive property, the [mask] string will be recorded in
 * [io.valix.core.ValidationError.rejectedValue] instead of the actual sensitive value.
 *
 * @property mask The string replacement used to redact sensitive values in error logs (defaults to `"********"`).
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Sensitive(
    val mask: String = "********"
)
