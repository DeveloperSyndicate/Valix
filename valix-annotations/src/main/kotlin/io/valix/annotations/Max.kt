package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Max(
    val value: Long,
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
