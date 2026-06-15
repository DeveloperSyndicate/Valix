package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
annotation class Min(
    val value: Long,
    val message: String = "",
    val messageKey: String = "",
    val groups: Array<KClass<*>> = []
)
