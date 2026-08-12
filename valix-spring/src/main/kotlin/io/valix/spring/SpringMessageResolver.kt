package io.valix.spring

import io.valix.core.MessageResolver
import org.springframework.context.MessageSource
import java.util.Locale

class SpringMessageResolver(
    private val messageSource: MessageSource
) : MessageResolver {

    override fun resolve(key: String, locale: io.valix.core.ValixLocale, params: Map<String, Any>): String {
        val javaLocale = Locale(locale.language, locale.country)
        val defaultMessage = params["defaultMessage"] as? String ?: key
        
        val template = try {
            messageSource.getMessage(key, null, javaLocale)
        } catch (e: Exception) {
            defaultMessage
        }

        return interpolate(template, params)
    }

    fun resolve(key: String, locale: Locale, params: Map<String, Any>): String {
        return resolve(key, io.valix.core.ValixLocale(locale.language, locale.country), params)
    }

    private fun interpolate(template: String, params: Map<String, Any>): String {
        var result = template
        for ((name, value) in params) {
            result = result.replace("{$name}", value.toString())
        }
        return result
    }
}
