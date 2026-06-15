package io.valix.spring

import org.springframework.context.support.StaticMessageSource
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class SpringMessageResolverTest {

    @Test
    fun `test resolve from spring message source with interpolation`() {
        val messageSource = StaticMessageSource()
        messageSource.addMessage("valix.min", Locale.ENGLISH, "Value must be at least {min}")
        messageSource.addMessage("valix.min", Locale.FRENCH, "La valeur doit être au moins {min}")

        val resolver = SpringMessageResolver(messageSource)

        val params = mapOf("min" to 5, "defaultMessage" to "Default")
        
        val englishMessage = resolver.resolve("valix.min", Locale.ENGLISH, params)
        assertEquals("Value must be at least 5", englishMessage)

        val frenchMessage = resolver.resolve("valix.min", Locale.FRENCH, params)
        assertEquals("La valeur doit être au moins 5", frenchMessage)
    }

    @Test
    fun `test resolve falls back to defaultMessage on missing key`() {
        val messageSource = StaticMessageSource()
        val resolver = SpringMessageResolver(messageSource)

        val params = mapOf("min" to 5, "defaultMessage" to "Default message with {min}")
        val result = resolver.resolve("missing.key", Locale.ENGLISH, params)
        assertEquals("Default message with 5", result)
    }
}
