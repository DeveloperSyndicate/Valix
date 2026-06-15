package io.valix.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class ValixDoc(
    val displayName: String = "",
    val description: String = "",
    val example: String = ""
)
