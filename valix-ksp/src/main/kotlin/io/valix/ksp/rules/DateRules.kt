package io.valix.ksp.rules

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock

private fun isDateType(property: KSPropertyDeclaration): Boolean {
    val resolved = property.type.resolve()
    val qName = resolved.declaration.qualifiedName?.asString()
    return qName in setOf(
        "java.time.LocalDate",
        "java.time.LocalDateTime",
        "java.time.Instant",
        "java.time.OffsetDateTime"
    )
}

private fun validateDateProperty(
    property: KSPropertyDeclaration,
    annotation: KSAnnotation,
    logger: KSPLogger
): Boolean {
    if (!isDateType(property)) {
        logger.error(
            "@${annotation.shortName.asString()} can only be applied to LocalDate, LocalDateTime, Instant, or OffsetDateTime properties",
            property
        )
        return false
    }
    return true
}

private fun getDateTypeFqn(property: KSPropertyDeclaration): String {
    return property.type.resolve().declaration.qualifiedName?.asString() ?: ""
}

object PastRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.Past"
    override val errorCode = "PAST_REQUIRED"
    override val defaultMessage = "must be in the past"

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateDateProperty(property, annotation, logger)
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val fqn = getDateTypeFqn(property)
        return CodeBlock.of("!%L.isBefore(%L.now())", valName, fqn)
    }
}

object PastOrPresentRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.PastOrPresent"
    override val errorCode = "PAST_REQUIRED"
    override val defaultMessage = "must be in the past or present"

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateDateProperty(property, annotation, logger)
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val fqn = getDateTypeFqn(property)
        return CodeBlock.of("%L.isAfter(%L.now())", valName, fqn)
    }
}

object FutureRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.Future"
    override val errorCode = "FUTURE_REQUIRED"
    override val defaultMessage = "must be in the future"

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateDateProperty(property, annotation, logger)
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val fqn = getDateTypeFqn(property)
        return CodeBlock.of("!%L.isAfter(%L.now())", valName, fqn)
    }
}

object FutureOrPresentRule : ConstraintRule {
    override val annotationFqName = "io.valix.annotations.FutureOrPresent"
    override val errorCode = "FUTURE_REQUIRED"
    override val defaultMessage = "must be in the future or present"

    override fun validate(property: KSPropertyDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateDateProperty(property, annotation, logger)
    }

    override fun generateCondition(property: KSPropertyDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val fqn = getDateTypeFqn(property)
        return CodeBlock.of("%L.isBefore(%L.now())", valName, fqn)
    }
}
