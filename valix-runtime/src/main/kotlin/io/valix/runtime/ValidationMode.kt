package io.valix.runtime

/**
 * Strategy controlling when form state validation is automatically triggered.
 */
enum class ValidationMode {
    /** Validate automatically whenever a field value changes. */
    OnChange,

    /** Validate automatically when a field loses focus. */
    OnBlur,

    /** Validate only when explicitly submitting or calling validate(). */
    OnSubmit
}
