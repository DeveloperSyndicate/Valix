package io.valix.ksp.rules

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import io.valix.ksp.ConstraintGenerator

private fun isCollection(type: KSType): Boolean {
    val collectionNames = setOf(
        "kotlin.collections.List",
        "kotlin.collections.Set",
        "kotlin.collections.Collection",
        "kotlin.collections.Iterable",
        "kotlin.collections.MutableList",
        "kotlin.collections.MutableSet",
        "kotlin.collections.MutableCollection",
        "kotlin.collections.MutableIterable"
    )
    val qName = type.declaration.qualifiedName?.asString()
    if (qName in collectionNames) return true

    val classDecl = type.declaration as? KSClassDeclaration ?: return false
    return classDecl.superTypes.any {
        val resolved = it.resolve()
        isCollection(resolved)
    }
}

private fun isSubtypeOfCollection(type: KSType): Boolean {
    val collectionNames = setOf(
        "kotlin.collections.Collection",
        "kotlin.collections.List",
        "kotlin.collections.Set",
        "kotlin.collections.MutableCollection",
        "kotlin.collections.MutableList",
        "kotlin.collections.MutableSet"
    )
    val qName = type.declaration.qualifiedName?.asString()
    if (qName in collectionNames) return true

    val classDecl = type.declaration as? KSClassDeclaration ?: return false
    return classDecl.superTypes.any {
        val resolved = it.resolve()
        isSubtypeOfCollection(resolved)
    }
}

private fun validateCollectionProperty(
    target: KSDeclaration,
    annotation: KSAnnotation,
    logger: KSPLogger
): Boolean {
    val property = target as? KSPropertyDeclaration
    if (property == null) {
        logger.error(
            "@${annotation.shortName.asString()} can only be applied to List, Set, Collection, or Iterable properties",
            target
        )
        return false
    }
    val type = property.type.resolve()
    if (!isCollection(type)) {
        logger.error(
            "@${annotation.shortName.asString()} can only be applied to List, Set, Collection, or Iterable properties",
            target
        )
        return false
    }
    return true
}

object NotEmptyRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.NotEmpty"
    override val errorCode = "NOT_EMPTY"
    override val defaultMessage = "must not be empty"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateCollectionProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val property = target as KSPropertyDeclaration
        val type = property.type.resolve()
        return if (isSubtypeOfCollection(type)) {
            CodeBlock.of("%L.isEmpty()", valName)
        } else {
            CodeBlock.of("!%L.any()", valName)
        }
    }
}

object SizeRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.Size"
    override val errorCode = "SIZE_INVALID"
    override val defaultMessage = "size must be in range"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val min = annotation.arguments.firstOrNull { it.name?.asString() == "min" }?.value as? Int ?: 0
        val max = annotation.arguments.firstOrNull { it.name?.asString() == "max" }?.value as? Int ?: 0
        return "size must be between $min and $max"
    }

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateCollectionProperty(target, annotation, logger)) return false
        val min = annotation.arguments.firstOrNull { it.name?.asString() == "min" }?.value as? Int
        val max = annotation.arguments.firstOrNull { it.name?.asString() == "max" }?.value as? Int
        if (min == null || max == null) {
            logger.error("@Size must specify min and max parameters", target)
            return false
        }
        if (min < 0) {
            logger.error("@Size min value must be non-negative", target)
            return false
        }
        if (max < min) {
            logger.error("@Size max value cannot be less than min value", target)
            return false
        }
        return true
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val min = annotation.arguments.first { it.name?.asString() == "min" }.value as Int
        val max = annotation.arguments.first { it.name?.asString() == "max" }.value as Int
        val property = target as KSPropertyDeclaration
        val type = property.type.resolve()
        return if (isSubtypeOfCollection(type)) {
            CodeBlock.of("%L.size < %L || %L.size > %L", valName, min, valName, max)
        } else {
            CodeBlock.of("run { val count = %L.count(); count < %L || count > %L }", valName, min, max)
        }
    }
}
