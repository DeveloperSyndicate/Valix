package io.valix.core

import java.util.Locale

/**
 * Interface for resolving localized validation error messages.
 */
interface MessageResolver {
    /**
     * Resolves a message template or key into a localized error message string.
     *
     * @param key The message key or default message template.
     * @param locale Target locale requested for interpolation.
     * @param params Key-value pairs available for placeholder substitution.
     * @return The formatted and localized message string.
     */
    fun resolve(key: String, locale: Locale, params: Map<String, Any> = emptyMap()): String
}

/**
 * Default pass-through implementation of [MessageResolver] returning raw message keys unmodified.
 */
object NoOpMessageResolver : MessageResolver {
    override fun resolve(key: String, locale: Locale, params: Map<String, Any>): String = key
}
