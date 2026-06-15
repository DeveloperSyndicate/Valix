package io.valix.core

import java.util.Locale

interface MessageResolver {
    fun resolve(key: String, locale: Locale, params: Map<String, Any> = emptyMap()): String
}

object NoOpMessageResolver : MessageResolver {
    override fun resolve(key: String, locale: Locale, params: Map<String, Any>): String = key
}
