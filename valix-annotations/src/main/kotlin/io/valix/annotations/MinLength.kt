package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class MinLength(
    val value: Int,
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
