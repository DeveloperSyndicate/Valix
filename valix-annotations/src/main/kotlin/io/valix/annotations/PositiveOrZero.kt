package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
annotation class PositiveOrZero(
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
