package io.valix.runtime

object ValixDslConstants {
    val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    
    const val KEY_PREFIX = "valix."
    const val DSL_CONSTRAINT_PREFIX = "io.valix.dsl."
}
