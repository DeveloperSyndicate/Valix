package io.valix.ksp.rules

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import io.valix.ksp.ConstraintGenerator

private fun isStringType(property: KSPropertyDeclaration): Boolean {
    val resolved = property.type.resolve()
    val qName = resolved.declaration.qualifiedName?.asString()
    return qName == "kotlin.String"
}

private fun validateStringProperty(
    target: KSDeclaration,
    annotation: KSAnnotation,
    logger: KSPLogger
): Boolean {
    val property = target as? KSPropertyDeclaration
    if (property == null || !isStringType(property)) {
        logger.error(
            "@${annotation.shortName.asString()} can only be applied to String or String? properties",
            target
        )
        return false
    }
    return true
}

object NotBlankRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.NotBlank"
    override val errorCode = "NOT_BLANK"
    override val defaultMessage = "must not be blank"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateStringProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        return CodeBlock.of("%L.trim().isEmpty()", valName)
    }
}

object EmailRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.Email"
    override val errorCode = "EMAIL_INVALID"
    override val defaultMessage = "invalid email"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateStringProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        return CodeBlock.of("!EMAIL_REGEX.matches(%L)", valName)
    }

    override fun generateAuxiliaryProperties(target: KSDeclaration, annotation: KSAnnotation, propName: String): List<PropertySpec> {
        return listOf(
            PropertySpec.builder("EMAIL_REGEX", Regex::class)
                .addModifiers(KModifier.PUBLIC)
                .initializer("Regex(%S)", "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
                .build()
        )
    }
}

object MinLengthRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.MinLength"
    override val errorCode = "MIN_LENGTH"
    override val defaultMessage = "minimum length check failed"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val limit = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? Int ?: 0
        return "minimum length is $limit"
    }

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateStringProperty(target, annotation, logger)) return false
        val value = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? Int
        if (value == null || value < 0) {
            logger.error("@MinLength value must be a non-negative integer", target)
            return false
        }
        return true
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val limit = annotation.arguments.first { it.name?.asString() == "value" }.value as Int
        return CodeBlock.of("%L.length < %L", valName, limit)
    }
}

object MaxLengthRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.MaxLength"
    override val errorCode = "MAX_LENGTH"
    override val defaultMessage = "maximum length check failed"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val limit = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? Int ?: 0
        return "maximum length is $limit"
    }

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateStringProperty(target, annotation, logger)) return false
        val value = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? Int
        if (value == null || value < 0) {
            logger.error("@MaxLength value must be a non-negative integer", target)
            return false
        }
        return true
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val limit = annotation.arguments.first { it.name?.asString() == "value" }.value as Int
        return CodeBlock.of("%L.length > %L", valName, limit)
    }
}

object PatternRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.Pattern"
    override val errorCode = "PATTERN_MISMATCH"
    override val defaultMessage = "pattern mismatch"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateStringProperty(target, annotation, logger)) return false
        val regexp = annotation.arguments.firstOrNull { it.name?.asString() == "regexp" || it.name?.asString() == "value" }?.value as? String
        if (regexp == null) {
            logger.error("@Pattern must specify a regular expression", target)
            return false
        }
        return true
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val propNameUpper = target.simpleName.asString().replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
        return CodeBlock.of("!PATTERN_%L.matches(%L)", propNameUpper, valName)
    }

    override fun generateAuxiliaryProperties(target: KSDeclaration, annotation: KSAnnotation, propName: String): List<PropertySpec> {
        val regexp = annotation.arguments.first { it.name?.asString() == "regexp" || it.name?.asString() == "value" }.value as String
        val propNameUpper = propName.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
        return listOf(
            PropertySpec.builder("PATTERN_$propNameUpper", Regex::class)
                .addModifiers(KModifier.PUBLIC)
                .initializer("Regex(%S)", regexp)
                .build()
        )
    }
}

object UrlRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.Url"
    override val errorCode = "URL_INVALID"
    override val defaultMessage = "invalid URL"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateStringProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        return CodeBlock.of("!URL_REGEX.matches(%L)", valName)
    }

    override fun generateAuxiliaryProperties(target: KSDeclaration, annotation: KSAnnotation, propName: String): List<PropertySpec> {
        return listOf(
            PropertySpec.builder("URL_REGEX", Regex::class)
                .addModifiers(KModifier.PUBLIC)
                .initializer("Regex(%S)", "^(https?|ftp)://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?\$")
                .build()
        )
    }
}

object PhoneNumberRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.PhoneNumber"
    override val errorCode = "PHONE_INVALID"
    override val defaultMessage = "invalid phone number"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateStringProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        return CodeBlock.of("!PHONE_REGEX.matches(%L)", valName)
    }

    override fun generateAuxiliaryProperties(target: KSDeclaration, annotation: KSAnnotation, propName: String): List<PropertySpec> {
        return listOf(
            PropertySpec.builder("PHONE_REGEX", Regex::class)
                .addModifiers(KModifier.PUBLIC)
                .initializer("Regex(%S)", "^\\+?[0-9\\s\\-()]{7,20}\$")
                .build()
        )
    }
}

object AlphaRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.Alpha"
    override val errorCode = "ALPHA_INVALID"
    override val defaultMessage = "must contain only alphabetic characters"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateStringProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        return CodeBlock.of("!ALPHA_REGEX.matches(%L)", valName)
    }

    override fun generateAuxiliaryProperties(target: KSDeclaration, annotation: KSAnnotation, propName: String): List<PropertySpec> {
        return listOf(
            PropertySpec.builder("ALPHA_REGEX", Regex::class)
                .addModifiers(KModifier.PUBLIC)
                .initializer("Regex(%S)", "^[a-zA-Z]+\$")
                .build()
        )
    }
}

object AlphaNumericRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.AlphaNumeric"
    override val errorCode = "ALPHANUMERIC_INVALID"
    override val defaultMessage = "must contain only alphanumeric characters"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateStringProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        return CodeBlock.of("!ALPHANUMERIC_REGEX.matches(%L)", valName)
    }

    override fun generateAuxiliaryProperties(target: KSDeclaration, annotation: KSAnnotation, propName: String): List<PropertySpec> {
        return listOf(
            PropertySpec.builder("ALPHANUMERIC_REGEX", Regex::class)
                .addModifiers(KModifier.PUBLIC)
                .initializer("Regex(%S)", "^[a-zA-Z0-9]+\$")
                .build()
        )
    }
}

object LowerCaseRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.LowerCase"
    override val errorCode = "LOWERCASE_INVALID"
    override val defaultMessage = "must be lowercase"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateStringProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        return CodeBlock.of("%L != %L.lowercase()", valName, valName)
    }
}

object UpperCaseRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.UpperCase"
    override val errorCode = "UPPERCASE_INVALID"
    override val defaultMessage = "must be uppercase"

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return validateStringProperty(target, annotation, logger)
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        return CodeBlock.of("%L != %L.uppercase()", valName, valName)
    }
}

object ContainsRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.Contains"
    override val errorCode = "CONTAINS_INVALID"
    override val defaultMessage = "must contain specified value"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val target = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? String ?: ""
        return "must contain '$target'"
    }

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateStringProperty(target, annotation, logger)) return false
        val value = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? String
        if (value == null) {
            logger.error("@Contains must specify a value", target)
            return false
        }
        return true
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val targetStr = annotation.arguments.first { it.name?.asString() == "value" }.value as String
        return CodeBlock.of("!%L.contains(%S)", valName, targetStr)
    }
}

object StartsWithRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.StartsWith"
    override val errorCode = "STARTS_WITH_INVALID"
    override val defaultMessage = "must start with specified value"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val target = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? String ?: ""
        return "must start with '$target'"
    }

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateStringProperty(target, annotation, logger)) return false
        val value = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? String
        if (value == null) {
            logger.error("@StartsWith must specify a value", target)
            return false
        }
        return true
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val targetStr = annotation.arguments.first { it.name?.asString() == "value" }.value as String
        return CodeBlock.of("!%L.startsWith(%S)", valName, targetStr)
    }
}

object EndsWithRule : ConstraintGenerator {
    override val annotationFqName = "io.valix.annotations.EndsWith"
    override val errorCode = "ENDS_WITH_INVALID"
    override val defaultMessage = "must end with specified value"

    override fun getDefaultMessage(annotation: KSAnnotation): String {
        val target = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? String ?: ""
        return "must end with '$target'"
    }

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        if (!validateStringProperty(target, annotation, logger)) return false
        val value = annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? String
        if (value == null) {
            logger.error("@EndsWith must specify a value", target)
            return false
        }
        return true
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val targetStr = annotation.arguments.first { it.name?.asString() == "value" }.value as String
        return CodeBlock.of("!%L.endsWith(%S)", valName, targetStr)
    }
}
