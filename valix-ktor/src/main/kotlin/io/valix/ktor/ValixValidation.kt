package io.valix.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.respond
import io.valix.core.ValidationResult
import io.valix.localization.resolveMessages
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

class ValixKtorValidationException(val validationResult: ValidationResult) : RuntimeException("Validation failed: ${validationResult.errors.joinToString { it.message }}")

class ValixKtorConfiguration {
    var defaultGroups: List<KClass<*>> = emptyList()
    var errorHandler: suspend (ApplicationCall, ValidationResult) -> Unit = { call, result ->
        call.respond(HttpStatusCode.BadRequest, mapOf(
            "status" to HttpStatusCode.BadRequest.value,
            "error" to "Bad Request",
            "errors" to result.errors.map { error ->
                mapOf(
                    "field" to error.field,
                    "code" to error.code,
                    "message" to error.message,
                    "messageKey" to error.messageKey,
                    "rejectedValue" to error.rejectedValue,
                    "path" to error.path
                )
            }
        ))
    }
}

val ValixValidation = createApplicationPlugin(
    name = "ValixValidation",
    createConfiguration = ::ValixKtorConfiguration
) {
    val groups = pluginConfig.defaultGroups.toTypedArray()
    val errorHandler = pluginConfig.errorHandler

    onCallReceive { call ->
        transformBody { body ->
            val result = ValixFrameworkValidator.validate(body, *groups)
            if (!result.valid) {
                val resolvedResult = result.resolveMessages()
                errorHandler(call, resolvedResult)
                throw ValixKtorValidationException(resolvedResult)
            }
            body
        }
    }
}
