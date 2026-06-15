package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class NotBlank(
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
