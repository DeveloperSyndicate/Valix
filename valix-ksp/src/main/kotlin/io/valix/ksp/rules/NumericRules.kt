package io.valix.ksp.rules

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock

private fun isNumericType(property: KSPropertyDeclaration): Boolean {
    val resolved = property.type.resolve()
    val qName = resolved.declaration.qualifiedName?.asString()
    return qName in setOf(
        "kotlin.Int",
        "kotlin.Long",
        "kotlin.Float",
        "kotlin.Double",
        "kotlin.Short"
    )
}

private fun validateNumericProperty(
    property: KSPropertyDeclaration,
    annotation: KSAnnotation,
    logger: KSPLogger
): Boolean {
    if (!isNumericType(property)) {
        logger.error(
            "@${annotation.shortName.asString()} can only be applied to Int, Long, Float, Double, or Short properties",
            property
        )
        return false
    }
    return true
}

private fun getCastSuffix(property: KSPropertyDeclaration): String {
    val resolved = property.type.resolve()
    return when (resolved.declaration.qualifiedName?.asString()) {
        "kotlin.Double" -> ".toDouble()"
        "kotlin.Float" -> ".toFloat()"
        "kotlin.Short" -> ".toShort()"
        else -> ""
    }
}

private fun getZeroString(property: KSPropertyDeclaration): String {
    val resolved = property.type.resolve()
    val qName = resolved.declaration.qualifiedName?.asString()
    return if (qName == "kotlin.Double" || qName == "kotlin.Float") "0.0" else "0"
}

object MinRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.Min"
    override val errorCode = "MIN_VALUE"
    override val defaultMessage = "must be at least min value"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val limit = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? Long ?: 0L
        return "must be at least $limit"
    }

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateNumericProperty(property, annotation, logger)) return false
        val value = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? Long
        if (value == null) {
            logger.error("@Min must specify a value parameter", property)
            return false
        }
        return true
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val limit = annotation.arguments.first { it.name?.asString() == "value" }.value as Long
        val resolved = property.type.resolve()
        val qName = resolved.declaration.qualifiedName?.asString()
        return if (qName == "kotlin.Int") {
            CodeBlock.of("%L < %L", valName, limit.toInt())
        } else if (qName == "kotlin.Long") {
            CodeBlock.of("%L < %LL", valName, limit)
        } else {
            val suffix = getCastSuffix(property)
            CodeBlock.of("%L < %L%L", valName, limit, suffix)
        }
    }
}

object MaxRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.Max"
    override val errorCode = "MAX_VALUE"
    override val defaultMessage = "must be at most max value"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val limit = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? Long ?: 0L
        return "must be at most $limit"
    }

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateNumericProperty(property, annotation, logger)) return false
        val value = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? Long
        if (value == null) {
            logger.error("@Max must specify a value parameter", property)
            return false
        }
        return true
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val limit = annotation.arguments.first { it.name?.asString() == "value" }.value as Long
        val resolved = property.type.resolve()
        val qName = resolved.declaration.qualifiedName?.asString()
        return if (qName == "kotlin.Int") {
            CodeBlock.of("%L > %L", valName, limit.toInt())
        } else if (qName == "kotlin.Long") {
            CodeBlock.of("%L > %LL", valName, limit)
        } else {
            val suffix = getCastSuffix(property)
            CodeBlock.of("%L > %L%L", valName, limit, suffix)
        }
    }
}

object PositiveRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.Positive"
    override val errorCode = "POSITIVE_REQUIRED"
    override val defaultMessage = "must be positive"

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateNumericProperty(property, annotation, logger)
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val zero = getZeroString(property)
        return CodeBlock.of("%L <= %L", valName, zero)
    }
}

object PositiveOrZeroRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.PositiveOrZero"
    override val errorCode = "POSITIVE_REQUIRED"
    override val defaultMessage = "must be positive or zero"

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateNumericProperty(property, annotation, logger)
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val zero = getZeroString(property)
        return CodeBlock.of("%L < %L", valName, zero)
    }
}

object NegativeRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.Negative"
    override val errorCode = "NEGATIVE_REQUIRED"
    override val defaultMessage = "must be negative"

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateNumericProperty(property, annotation, logger)
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val zero = getZeroString(property)
        return CodeBlock.of("%L >= %L", valName, zero)
    }
}

object NegativeOrZeroRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.NegativeOrZero"
    override val errorCode = "NEGATIVE_REQUIRED"
    override val defaultMessage = "must be negative or zero"

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateNumericProperty(property, annotation, logger)
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val zero = getZeroString(property)
        return CodeBlock.of("%L > %L", valName, zero)
    }
}

object RangeRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.Range"
    override val errorCode = "RANGE_INVALID"
    override val defaultMessage = "must be in range"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val min = annotation.arguments.firstOrNull { it.name?.asString() == "min" }?.value as? Long ?: 0L
        val max = annotation.arguments.firstOrNull { it.name?.asString() == "max" }?.value as? Long ?: 0L
        return "must be between $min and $max"
    }

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateNumericProperty(property, annotation, logger)) return false
        val min = annotation.arguments.firstOrNull { it.name?.asString() == "min" }?.value as? Long
        val max = annotation.arguments.firstOrNull { it.name?.asString() == "max" }?.value as? Long
        if (min == null || max == null) {
            logger.error("@Range must specify min and max parameters", property)
            return false
        }
        if (min > max) {
            logger.error("@Range min value cannot be greater than max value", property)
            return false
        }
        return true
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val min = annotation.arguments.first { it.name?.asString() == "min" }.value as Long
        val max = annotation.arguments.first { it.name?.asString() == "max" }.value as Long
        val resolved = property.type.resolve()
        val qName = resolved.declaration.qualifiedName?.asString()
        return if (qName == "kotlin.Int") {
            CodeBlock.of("%L < %L || %L > %L", valName, min.toInt(), valName, max.toInt())
        } else if (qName == "kotlin.Long") {
            CodeBlock.of("%L < %LL || %L > %LL", valName, min, valName, max)
        } else {
            val suffix = getCastSuffix(property)
            CodeBlock.of("%L < %L%L || %L > %L%L", valName, min, suffix, valName, max, suffix)
        }
    }
}
