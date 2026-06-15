package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Size(
    val min: Int,
    val max: Int,
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
