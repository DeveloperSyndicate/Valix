package io.valix.core

/**
 * Standard validation error codes supported natively by Valix.
 */
enum class ValixErrorCode(val code: String) {
    NOT_NULL("NOT_NULL"),
    NOT_BLANK("NOT_BLANK"),
    MIN("MIN"),
    MAX("MAX"),
    MIN_LENGTH("MIN_LENGTH"),
    MAX_LENGTH("MAX_LENGTH"),
    PATTERN("PATTERN"),
    EMAIL("EMAIL"),
    URL("URL"),
    PHONE_NUMBER("PHONE_NUMBER"),
    ALPHA("ALPHA"),
    ALPHA_NUMERIC("ALPHA_NUMERIC"),
    LOWER_CASE("LOWER_CASE"),
    UPPER_CASE("UPPER_CASE"),
    CONTAINS("CONTAINS"),
    STARTS_WITH("STARTS_WITH"),
    ENDS_WITH("ENDS_WITH"),
    SIZE("SIZE"),
    NOT_EMPTY("NOT_EMPTY"),
    PAST("PAST"),
    PAST_OR_PRESENT("PAST_OR_PRESENT"),
    FUTURE("FUTURE"),
    FUTURE_OR_PRESENT("FUTURE_OR_PRESENT"),
    ALLOWED_VALUES("ALLOWED_VALUES"),
    ASYNC_INVALID("ASYNC_INVALID");

    companion object {
        private val map = values().associateBy { it.code }

        /**
         * Resolves the [ValixErrorCode] matching the specified string [code], or `null` if none.
         */
        fun fromCode(code: String): ValixErrorCode? = map[code]
    }
}
