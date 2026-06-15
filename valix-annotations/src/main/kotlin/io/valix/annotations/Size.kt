package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
annotation class Size(
    val min: Int,
    val max: Int,
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
