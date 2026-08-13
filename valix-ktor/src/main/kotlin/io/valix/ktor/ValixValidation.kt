package io.valix.ktor

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.response.respond
import io.valix.core.ValidationResult
import io.valix.core.ValixFrameworkConstants
import io.valix.localization.resolveMessages
import kotlin.reflect.KClass

/** Reflection bridge locating compiled `ValixRegistry` for dynamic framework dispatch. */
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

    /** Validates generic request payload [value] against compiled Valix rules. */
    fun validate(value: Any, vararg groups: KClass<*>): ValidationResult {
        return validateFunction(value, groups)
    }
}

/** Exception thrown when Ktor request payload validation fails. */
class ValixKtorValidationException(val validationResult: ValidationResult) : RuntimeException("Validation failed: ${validationResult.errors.joinToString { it.message }}")

/** Configuration class for Ktor [ValixValidation] plugin. */
class ValixKtorConfiguration {
    /** Default validation groups. */
    var defaultGroups: List<KClass<*>> = emptyList()

    /** Custom error response handler block. */
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

/** Ktor Server Application Plugin intercepting incoming request bodies for Valix validation. */
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
