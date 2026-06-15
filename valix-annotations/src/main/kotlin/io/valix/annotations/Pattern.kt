package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.ANNOTATION_CLASS)
annotation class Pattern(
    val regexp: String,
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
