package io.valix.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec

interface ConstraintGenerator {
    val annotationFqName: String
    val errorCode: String
    val defaultMessage: String

    fun getDefaultMessage(annotation: KSAnnotation): String = defaultMessage

    /**
     * Customizes the field name reported in the ValidationError.
     */
    fun getErrorField(target: KSDeclaration, annotation: KSAnnotation, defaultField: String): String = defaultField

    /**
     * Customizes the expression used to capture the rejected value in the ValidationError.
     */
    fun getRejectedValueExpression(target: KSDeclaration, annotation: KSAnnotation, defaultExpression: String): String = defaultExpression

    /**
     * Performs compile-time check of target type and annotation arguments.
     * Logs error via logger.error if validation fails, returning false.
     */
    fun validate(
        target: KSDeclaration,
        annotation: KSAnnotation,
        logger: KSPLogger
    ): Boolean

    /**
     * Generates the boolean condition that triggers a validation failure (evaluates to true on failure).
     */
    fun generateCondition(
        target: KSDeclaration,
        annotation: KSAnnotation,
        valName: String
    ): CodeBlock

    /**
     * Generates any companion/validator object auxiliary fields (e.g. Regex patterns, allowed values sets).
     */
    fun generateAuxiliaryProperties(
        target: KSDeclaration,
        annotation: KSAnnotation,
        propName: String
    ): List<PropertySpec> = emptyList()

    /**
     * Maps the annotation to a ConstraintMetadata object at compile time.
     */
    fun toConstraintMetadata(
        annotation: KSAnnotation,
        message: String,
        messageKey: String,
        groups: List<String>
    ): io.valix.metadata.ConstraintMetadata {
        val params = mutableMapOf<String, Any>()
        for (arg in annotation.arguments) {
            val name = arg.name?.asString()
            if (name != null && name != "message" && name != "messageKey" && name != "groups") {
                val value = arg.value
                if (value != null) {
                    if (value is Collection<*>) {
                        params[name] = value.map { it?.toString() ?: "" }
                    } else if (value is Array<*>) {
                        params[name] = value.map { it?.toString() ?: "" }
                    } else {
                        params[name] = value
                    }
                }
            }
        }

        val keyword = when (annotationFqName) {
            "io.valix.annotations.MinLength" -> io.valix.metadata.SchemaKeyword.MIN_LENGTH
            "io.valix.annotations.MaxLength" -> io.valix.metadata.SchemaKeyword.MAX_LENGTH
            "io.valix.annotations.Pattern" -> io.valix.metadata.SchemaKeyword.PATTERN
            "io.valix.annotations.Email" -> io.valix.metadata.SchemaKeyword.FORMAT_EMAIL
            "io.valix.annotations.Url" -> io.valix.metadata.SchemaKeyword.FORMAT_URI
            "io.valix.annotations.Min" -> io.valix.metadata.SchemaKeyword.MINIMUM
            "io.valix.annotations.Max" -> io.valix.metadata.SchemaKeyword.MAXIMUM
            "io.valix.annotations.NotEmpty" -> io.valix.metadata.SchemaKeyword.NOT_EMPTY
            "io.valix.annotations.Size" -> io.valix.metadata.SchemaKeyword.CUSTOM
            "io.valix.annotations.AllowedValues" -> io.valix.metadata.SchemaKeyword.ENUM_VALUES
            else -> io.valix.metadata.SchemaKeyword.NONE
        }

        val resolvedMessageKey = messageKey.ifEmpty {
            val shortName = annotation.annotationType.resolve().declaration.simpleName.asString().lowercase()
            "valix.$shortName"
        }

        return io.valix.metadata.ConstraintMetadata(
            annotationFqName = annotationFqName,
            constraintCode = errorCode,
            messageKey = resolvedMessageKey,
            defaultMessage = message.ifEmpty { getDefaultMessage(annotation) },
            params = params,
            groups = groups,
            isCustom = false,
            schemaKeyword = keyword
        )
    }
}
