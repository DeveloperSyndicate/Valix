package io.valix.core

import kotlin.reflect.KClass

/**
 * Provides contextual metadata during validation execution.
 */
interface ValidationContext {
    /** The target field simple property name. */
    val fieldName: String

    /** The fully qualified navigation property path (e.g. `"user.address.street"`). */
    val path: String

    /** The root target object instance currently undergoing validation. */
    val rootObject: Any

    /** Active validation groups for conditional execution scoping. */
    val groups: Array<out KClass<*>>
}
