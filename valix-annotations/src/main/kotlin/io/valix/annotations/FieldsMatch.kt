package io.valix.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class FieldsMatch(
    val first: String,
    val second: String,
    val message: String = "",
    val groups: Array<KClass<*>> = []
)
