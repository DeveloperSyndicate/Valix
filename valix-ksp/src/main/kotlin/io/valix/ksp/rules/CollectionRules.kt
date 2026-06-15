package io.valix.ksp.rules

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock

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
    property: KSPropertyDeclaration,
    annotation: KSAnnotation,
    logger: KSPLogger
): Boolean {
    val type = property.type.resolve()
    if (!isCollection(type)) {
        logger.error(
            "@${annotation.shortName.asString()} can only be applied to List, Set, Collection, or Iterable properties",
            property
        )
        return false
    }
    return true
}

object NotEmptyRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.NotEmpty"
    override val errorCode = "NOT_EMPTY"
    override val defaultMessage = "must not be empty"

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateCollectionProperty(property, annotation, logger)
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val type = property.type.resolve()
        return if (isSubtypeOfCollection(type)) {
            CodeBlock.of("%L.isEmpty()", valName)
        } else {
            CodeBlock.of("!%L.any()", valName)
        }
    }
}

object SizeRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.Size"
    override val errorCode = "SIZE_INVALID"
    override val defaultMessage = "size must be in range"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val min = annotation.arguments.firstOrNull { it.name?.asString() == "min" }?.value as? Int ?: 0
        val max = annotation.arguments.firstOrNull { it.name?.asString() == "max" }?.value as? Int ?: 0
        return "size must be between $min and $max"
    }

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateCollectionProperty(property, annotation, logger)) return false
        val min = annotation.arguments.firstOrNull { it.name?.asString() == "min" }?.value as? Int
        val max = annotation.arguments.firstOrNull { it.name?.asString() == "max" }?.value as? Int
        if (min == null || max == null) {
            logger.error("@Size must specify min and max parameters", property)
            return false
        }
        if (min < 0) {
            logger.error("@Size min value must be non-negative", property)
            return false
        }
        if (max < min) {
            logger.error("@Size max value cannot be less than min value", property)
            return false
        }
        return true
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val min = annotation.arguments.first { it.name?.asString() == "min" }.value as Int
        val max = annotation.arguments.first { it.name?.asString() == "max" }.value as Int
        val type = property.type.resolve()
        return if (isSubtypeOfCollection(type)) {
            CodeBlock.of("%L.size < %L || %L.size > %L", valName, min, valName, max)
        } else {
            CodeBlock.of("run { val count = %L.count(); count < %L || count > %L }", valName, min, max)
        }
    }
}
