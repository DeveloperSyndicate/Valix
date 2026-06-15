package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Range(
    val min: Long,
    val max: Long,
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
