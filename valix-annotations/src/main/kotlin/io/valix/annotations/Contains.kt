package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
annotation class Contains(
    val value: String,
    val message: String = "",
    val messageKey: String = "",
    val groups: Array<KClass<*>> = []
)
