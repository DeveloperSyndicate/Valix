package io.valix.ksp.rules

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock
import io.valix.ksp.ConstraintGenerator

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
    target: KSDeclaration,
    annotation: KSAnnotation,
    logger: KSPLogger
): Boolean {
    val property = target as? KSPropertyDeclaration
    if (property == null || !isDateType(property)) {
        logger.error(
            "@${annotation.shortName.asString()} can only be applied to LocalDate, LocalDateTime, Instant, or OffsetDateTime properties",
            target
        )
        return false
    }
    return true
}

private fun getDateTypeFqn(target: KSDeclaration): String {
    val property = target as KSPropertyDeclaration
    return property.type.resolve().declaration.qualifiedName?.asString() ?: ""
}

object PastRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.Past"
    override val errorCode = "PAST_REQUIRED"
    override val defaultMessage = "must be in the past"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateDateProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val fqn = getDateTypeFqn(target)
        return CodeBlock.of("!%L.isBefore(%L.now())", valName, fqn)
    }
}

object PastOrPresentRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.PastOrPresent"
    override val errorCode = "PAST_REQUIRED"
    override val defaultMessage = "must be in the past or present"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateDateProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val fqn = getDateTypeFqn(target)
        return CodeBlock.of("%L.isAfter(%L.now())", valName, fqn)
    }
}

object FutureRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.Future"
    override val errorCode = "FUTURE_REQUIRED"
    override val defaultMessage = "must be in the future"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateDateProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val fqn = getDateTypeFqn(target)
        return CodeBlock.of("!%L.isAfter(%L.now())", valName, fqn)
    }
}

object FutureOrPresentRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.FutureOrPresent"
    override val errorCode = "FUTURE_REQUIRED"
    override val defaultMessage = "must be in the future or present"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateDateProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val fqn = getDateTypeFqn(target)
        return CodeBlock.of("%L.isBefore(%L.now())", valName, fqn)
    }
}
