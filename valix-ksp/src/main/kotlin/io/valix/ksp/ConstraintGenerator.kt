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
}
