package io.valix.ksp.rules

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec

interface ConstraintRule {
    val annotationFqName: String
    val errorCode: String
    val defaultMessage: String

    fun getDefaultMessage(annotation: KSAnnotation): String = defaultMessage

    /**
     * Performs compile-time check of target type and annotation arguments.
     * Logs error via logger.error if validation fails, returning false.
     */
    fun validate(
        property: KSPropertyDeclaration,
        annotation: KSAnnotation,
        logger: KSPLogger
    ): Boolean

    /**
     * Generates the boolean condition that triggers a validation failure (evaluates to true on failure).
     */
    fun generateCondition(
        property: KSPropertyDeclaration,
        annotation: KSAnnotation,
        valName: String
    ): CodeBlock

    /**
     * Generates any companion/validator object auxiliary fields (e.g. Regex patterns, allowed values sets).
     */
    fun generateAuxiliaryProperties(
        property: KSPropertyDeclaration,
        annotation: KSAnnotation,
        propName: String
    ): List<PropertySpec> = emptyList()
}
