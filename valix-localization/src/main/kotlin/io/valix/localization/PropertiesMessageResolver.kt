package io.valix.localization

import io.valix.core.MessageResolver
import io.valix.core.ValidationResult
import io.valix.metadata.ValixConfig
import java.util.Locale
import java.util.ResourceBundle
import java.util.MissingResourceException

class PropertiesMessageResolver(
    private val baseName: String = "valix-messages"
) : MessageResolver {

    override fun resolve(key: String, locale: Locale, params: Map<String, Any>): String {
        val bundle = try {
            ResourceBundle.getBundle(baseName, locale)
        } catch (e: MissingResourceException) {
            try {
                ResourceBundle.getBundle(baseName, Locale.ENGLISH)
            } catch (ex: MissingResourceException) {
                null
            }
        }

        val template = if (bundle != null && bundle.containsKey(key)) {
            bundle.getString(key)
        } else {
            params["defaultMessage"] as? String ?: key
        }

        return interpolate(template, params)
    }

    private fun interpolate(template: String, params: Map<String, Any>): String {
        var result = template
        for ((name, value) in params) {
            result = result.replace("{$name}", value.toString())
        }
        return result
    }
}

fun ValidationResult.resolveMessages(
    locale: Locale = ValixConfig.defaultLocale,
    resolver: MessageResolver = ValixConfig.messageResolver
): ValidationResult {
    val resolvedErrors = errors.map { error ->
        if (error.messageKey.isNotEmpty()) {
            val params = mutableMapOf<String, Any>()
            error.rejectedValue?.let { params["rejectedValue"] = it }
            params["field"] = error.field
            params["path"] = error.path
            params["defaultMessage"] = error.message

            if (error.constraint != null) {
                val constraintParams = findConstraintParams(error.constraint!!, error.path)
                if (constraintParams != null) {
                    params.putAll(constraintParams)
                }
            }

            val resolvedMsg = resolver.resolve(error.messageKey, locale, params)
            error.copy(message = resolvedMsg)
        } else {
            error
        }
    }
    return ValidationResult(valid, resolvedErrors)
}

private fun findConstraintParams(constraintFqName: String, fieldPath: String): Map<String, Any>? {
    val cleanField = fieldPath.substringBefore("[").substringAfterLast(".")
    for (modelMetadata in io.valix.metadata.MetadataRegistry.getAll()) {
        val field = modelMetadata.fields.find { it.name == cleanField }
        if (field != null) {
            val constraint = field.constraints.find { it.annotationFqName == constraintFqName }
            if (constraint != null) {
                return constraint.params
            }
        }
        val classConstraint = modelMetadata.classConstraints.find { it.annotationFqName == constraintFqName }
        if (classConstraint != null) {
            return classConstraint.params
        }
    }
    return null
}
