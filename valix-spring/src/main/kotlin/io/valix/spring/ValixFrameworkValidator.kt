package io.valix.spring

import io.valix.core.ValidationResult
import kotlin.reflect.KClass

object ValixFrameworkValidator {
    private val validateFunction: (Any, Array<out KClass<*>>) -> ValidationResult = run {
        try {
            val registryClass = Class.forName("io.valix.generated.ValixRegistry")
            val instance = registryClass.getField("INSTANCE").get(null)
            val method = registryClass.getMethod("validate", Any::class.java, Array::class.java)
            val fn = { value: Any, groups: Array<out KClass<*>> ->
                method.invoke(instance, value, groups) as ValidationResult
            }
            fn
        } catch (e: Exception) {
            { _, _ -> ValidationResult(true, emptyList()) }
        }
    }

    fun validate(value: Any, vararg groups: KClass<*>): ValidationResult {
        return validateFunction(value, groups)
    }
}
