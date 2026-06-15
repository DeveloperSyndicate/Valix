package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class EndsWith(
    val value: String,
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
