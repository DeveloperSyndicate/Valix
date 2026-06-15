package io.valix.micronaut

import io.micronaut.aop.Around
import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.valix.core.ValidationResult
import io.valix.localization.resolveMessages
import jakarta.inject.Singleton
import kotlin.reflect.KClass

@Around
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValixValidated(
    val groups: Array<KClass<*>> = []
)

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

class ValixMicronautValidationException(val validationResult: ValidationResult) : RuntimeException("Validation failed: ${validationResult.errors.joinToString { it.message }}")

@Singleton
@InterceptorBean(ValixValidated::class)
class ValixInterceptor : MethodInterceptor<Any, Any> {
    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        val valixValidated = context.findAnnotation(ValixValidated::class.java).orElse(null)
        val groups = valixValidated?.values?.get("groups") as? Array<KClass<*>> ?: emptyArray()

        for (arg in context.parameterValues) {
            if (arg != null) {
                val result = ValixFrameworkValidator.validate(arg, *groups)
                if (!result.valid) {
                    val resolvedResult = result.resolveMessages()
                    throw ValixMicronautValidationException(resolvedResult)
                }
            }
        }
        return context.proceed()
    }
}
