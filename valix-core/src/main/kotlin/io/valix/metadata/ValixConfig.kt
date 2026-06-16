package io.valix.metadata

import io.valix.core.MessageResolver
import io.valix.core.NoOpMessageResolver
import java.util.Locale

object ValixConfig {
    var defaultLocale: Locale = Locale.ENGLISH
    var messageResolver: MessageResolver = NoOpMessageResolver
    var schemaVersion: Int = 1
}
