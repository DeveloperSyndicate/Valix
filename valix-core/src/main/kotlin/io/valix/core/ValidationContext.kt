package io.valix.core

import kotlin.reflect.KClass

interface ValidationContext {
    val fieldName: String
    val path: String
    val rootObject: Any
    val groups: Array<out KClass<*>>
}
