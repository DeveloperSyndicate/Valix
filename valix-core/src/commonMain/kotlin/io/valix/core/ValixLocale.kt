package io.valix.core

/**
 * Multiplatform representation of a language/country locale descriptor.
 */
data class ValixLocale(
    val language: String = "en",
    val country: String = "US"
) {
    companion object {
        val ENGLISH = ValixLocale("en", "US")
    }
}
