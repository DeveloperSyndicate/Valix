package io.valix.metadata

import io.valix.core.MessageResolver
import io.valix.core.NoOpMessageResolver
import io.valix.core.ValixLocale

/**
 * Global configuration settings for the Valix validation engine.
 */
object ValixConfig {
    /** Default locale used when resolving validation messages. */
    var defaultLocale: ValixLocale = ValixLocale.ENGLISH

    /** Active [MessageResolver] for error message template localization. */
    var messageResolver: MessageResolver = NoOpMessageResolver

    /** Metadata schema format version. */
    var schemaVersion: Int = 1
}
