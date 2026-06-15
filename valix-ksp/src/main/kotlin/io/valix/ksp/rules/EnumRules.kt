package io.valix.ksp.rules

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import io.valix.ksp.ConstraintGenerator

object AllowedValuesRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.AllowedValues"
    override val errorCode = "INVALID_ENUM_VALUE"
    override val defaultMessage = "value must be one of allowed values"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        val property = target as? KSPropertyDeclaration
        if (property == null) {
            logger.error(
                "@AllowedValues can only be applied to Enum or String/String? properties",
                target
            )
            return false
        }
        val type = property.type.resolve()
        val classDecl = type.declaration as? KSClassDeclaration
        val isEnum = classDecl?.classKind == ClassKind.ENUM_CLASS
        val isString = type.declaration.qualifiedName?.asString() == "kotlin.String"

        if (!isEnum && !isString) {
            logger.error(
                "@AllowedValues can only be applied to Enum or String/String? properties",
                target
            )
            return false
        }

        val values = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? List<*>
        if (values == null || values.isEmpty()) {
            logger.error("@AllowedValues must specify at least one allowed value", target)
            return false
        }

        if (isEnum && classDecl != null) {
            val enumConstants = classDecl.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.classKind == ClassKind.ENUM_ENTRY }
                .map { it.simpleName.asString() }
                .toSet()

            for (v in values) {
                val vStr = v as? String
                if (vStr == null || vStr !in enumConstants) {
                    logger.error(
                        "Value '$vStr' is not a valid entry of the enum ${classDecl.qualifiedName?.asString()}",
                        target
                    )
                    return false
                }
            }
        }

        return true
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val property = target as KSPropertyDeclaration
        val propNameUpper = property.simpleName.asString().replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
        val type = property.type.resolve()
        val classDecl = type.declaration as? KSClassDeclaration
        val isEnum = classDecl?.classKind == ClassKind.ENUM_CLASS

        return if (isEnum) {
            CodeBlock.of("!ALLOWED_VALUES_%L.contains(%L.name)", propNameUpper, valName)
        } else {
            CodeBlock.of("!ALLOWED_VALUES_%L.contains(%L)", propNameUpper, valName)
        }
    }

    override fun generateAuxiliaryProperties(target: KSDeclaration, annotation: KSAnnotation, propName: String): List<PropertySpec> {
        val values = annotation.arguments.first { it.name?.asString() == "value" }.value as List<*>
        val allowedValues = values.map { it as String }
        val propNameUpper = propName.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

        val setCN = ClassName("kotlin.collections", "Set")
        val stringCN = ClassName("kotlin", "String")
        val setOfString = setCN.parameterizedBy(stringCN)

        return listOf(
            PropertySpec.builder("ALLOWED_VALUES_$propNameUpper", setOfString)
                .addModifiers(KModifier.PRIVATE)
                .initializer("setOf(" + allowedValues.joinToString(", ") { "%S" } + ")", *allowedValues.toTypedArray())
                .build()
        )
    }
}
