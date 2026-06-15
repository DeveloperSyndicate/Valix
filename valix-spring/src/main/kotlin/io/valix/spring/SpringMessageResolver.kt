package io.valix.spring

import io.valix.core.MessageResolver
import org.springframework.context.MessageSource
import java.util.Locale

class SpringMessageResolver(
    private val messageSource: MessageSource
) : MessageResolver {

    override fun resolve(key: String, locale: Locale, params: Map<String, Any>): String {
        val defaultMessage = params["defaultMessage"] as? String ?: key
        
        val template = try {
            messageSource.getMessage(key, null, locale)
        } catch (e: Exception) {
            defaultMessage
        }

        return interpolate(template, params)
    }

    private fun interpolate(template: String, params: Map<String, Any>): String {
        var result = template
        for ((name, value) in params) {
            result = result.replace("{$name}", value.toString())
        }
        return result
    }
}
