package io.valix.spring

import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidValix(
    val groups: Array<KClass<*>> = []
)
