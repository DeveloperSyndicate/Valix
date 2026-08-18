package io.valix.spring

import io.valix.core.ValidationResult
import io.valix.core.ValixFrameworkConstants
import kotlin.reflect.KClass

object ValixFrameworkValidator {
    private val validateFunction: (Any, Array<out KClass<*>>) -> ValidationResult = run {
        try {
            val registryClass = Class.forName(ValixFrameworkConstants.REGISTRY_CLASS_NAME)
            val instance = registryClass.getField(ValixFrameworkConstants.INSTANCE_FIELD_NAME).get(null)
            val method = registryClass.getMethod(ValixFrameworkConstants.VALIDATE_METHOD_NAME, Any::class.java, Array::class.java)
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
