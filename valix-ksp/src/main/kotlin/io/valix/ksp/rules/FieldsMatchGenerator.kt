package io.valix.ksp.rules

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock
import io.valix.ksp.ConstraintGenerator

object FieldsMatchGenerator : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.FieldsMatch"
    override val errorCode = "FIELDS_MATCH_INVALID"
    override val defaultMessage = "fields do not match"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val first = annotation.arguments.firstOrNull { it.name?.asString() == "first" }?.value as? String ?: ""
        val second = annotation.arguments.firstOrNull { it.name?.asString() == "second" }?.value as? String ?: ""
        return "Field '$first' and '$second' must match"
    }

    override fun getErrorField(target: KSDeclaration, annotation: KSAnnotation, defaultField: String): String {
        return annotation.arguments.firstOrNull { it.name?.asString() == "second" }?.value as? String ?: defaultField
    }

    override fun getRejectedValueExpression(target: KSDeclaration, annotation: KSAnnotation, defaultExpression: String): String {
        val second = annotation.arguments.firstOrNull { it.name?.asString() == "second" }?.value as? String
        return if (second != null) "value.$second" else defaultExpression
    }

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        val classDecl = target as? KSClassDeclaration
        if (classDecl == null) {
            logger.error("@FieldsMatch can only be applied to class declarations", target)
            return false
        }

        val first = annotation.arguments.firstOrNull { it.name?.asString() == "first" }?.value as? String
        val second = annotation.arguments.firstOrNull { it.name?.asString() == "second" }?.value as? String

        if (first.isNullOrEmpty() || second.isNullOrEmpty()) {
            logger.error("@FieldsMatch must specify both 'first' and 'second' parameters", target)
            return false
        }

        val properties = classDecl.getAllProperties().toList()
        val firstProp = properties.find { it.simpleName.asString() == first }
        val secondProp = properties.find { it.simpleName.asString() == second }

        if (firstProp == null) {
            logger.error("Field '$first' not found in class ${classDecl.simpleName.asString()}", target)
            return false
        }
        if (secondProp == null) {
            logger.error("Field '$second' not found in class ${classDecl.simpleName.asString()}", target)
            return false
        }

        val firstType = firstProp.type.resolve()
        val secondType = secondProp.type.resolve()
        val firstNotNullable = firstType.makeNotNullable()
        val secondNotNullable = secondType.makeNotNullable()

        if (!firstNotNullable.isAssignableFrom(secondNotNullable) && !secondNotNullable.isAssignableFrom(firstNotNullable)) {
            logger.error("Fields '$first' and '$second' have incompatible types", target)
            return false
        }

        return true
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val first = annotation.arguments.first { it.name?.asString() == "first" }.value as String
        val second = annotation.arguments.first { it.name?.asString() == "second" }.value as String
        return CodeBlock.of("value.%L != value.%L", first, second)
    }
}
