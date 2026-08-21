package io.valix.ksp

object ValixAnnotationNames {
    const val CONSTRAINT = "io.valix.annotations.Constraint"
    const val VALID = "io.valix.annotations.Valid"
    const val NOT_NULL = "io.valix.annotations.NotNull"
    const val SENSITIVE = "io.valix.annotations.Sensitive"
    const val VALIDATE_IF = "io.valix.annotations.ValidateIf"
    const val VALIX_DOC = "io.valix.annotations.ValixDoc"

    // Core Validator interfaces
    const val CONSTRAINT_VALIDATOR = "io.valix.core.ConstraintValidator"
    const val OBJECT_CONSTRAINT_VALIDATOR = "io.valix.core.ObjectConstraintValidator"
    const val ASYNC_CONSTRAINT_VALIDATOR = "io.valix.runtime.AsyncConstraintValidator"

    // Key annotation parameters
    const val PARAM_MESSAGE = "message"
    const val PARAM_MESSAGE_KEY = "messageKey"
    const val PARAM_GROUPS = "groups"
    const val PARAM_VALIDATOR = "validator"
    const val PARAM_FIELD = "field"
    const val PARAM_EQUALS = "equals"

    // Default values
    const val DEFAULT_SENSITIVE_MASK = "********"
    const val DEFAULT_METADATA_VERSION = "1.0.0"

    // Built-in constraint annotations
    const val MIN_LENGTH = "io.valix.annotations.MinLength"
    const val MAX_LENGTH = "io.valix.annotations.MaxLength"
    const val PATTERN = "io.valix.annotations.Pattern"
    const val EMAIL = "io.valix.annotations.Email"
    const val URL = "io.valix.annotations.Url"
    const val MIN = "io.valix.annotations.Min"
    const val MAX = "io.valix.annotations.Max"
    const val NOT_EMPTY = "io.valix.annotations.NotEmpty"
    const val SIZE = "io.valix.annotations.Size"
    const val ALLOWED_VALUES = "io.valix.annotations.AllowedValues"
}
