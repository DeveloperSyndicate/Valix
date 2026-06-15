package io.valix.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import io.valix.ksp.rules.*

object PluginRegistry {
    private val generators = mutableMapOf<String, ConstraintGenerator>()

    init {
        register(NotBlankRule)
        register(EmailRule)
        register(MinLengthRule)
        register(MaxLengthRule)
        register(PatternRule)
        register(UrlRule)
        register(PhoneNumberRule)
        register(AlphaRule)
        register(AlphaNumericRule)
        register(LowerCaseRule)
        register(UpperCaseRule)
        register(ContainsRule)
        register(StartsWithRule)
        register(EndsWithRule)
        register(MinRule)
        register(MaxRule)
        register(PositiveRule)
        register(PositiveOrZeroRule)
        register(NegativeRule)
        register(NegativeOrZeroRule)
        register(RangeRule)
        register(NotEmptyRule)
        register(SizeRule)
        register(AllowedValuesRule)
        register(PastRule)
        register(PastOrPresentRule)
        register(FutureRule)
        register(FutureOrPresentRule)
        register(FieldsMatchGenerator)
    }

    fun register(generator: ConstraintGenerator) {
        generators[generator.annotationFqName] = generator
    }

    fun getGenerator(annotationFqName: String): ConstraintGenerator? {
        return generators[annotationFqName]
    }
}

object CustomValidatorGenerator : ConstraintGenerator {
    override val annotationFqName = ""
    override val errorCode = "CUSTOM_VALIDATION_FAILED"
    override val defaultMessage = "custom validation failed"

    private fun resolveValidatorFqName(annotation: KSAnnotation): String? {
        val annClass = annotation.annotationType.resolve().declaration as? KSClassDeclaration ?: return null
        val constraintAnn = annClass.annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.Constraint"
        } ?: return null
        val validatorType = constraintAnn.arguments.firstOrNull { it.name?.asString() == "validator" }?.value as? KSType
        return validatorType?.declaration?.qualifiedName?.asString()
    }

    private fun isValidatorObject(annotation: KSAnnotation): Boolean {
        val annClass = annotation.annotationType.resolve().declaration as? KSClassDeclaration ?: return false
        val constraintAnn = annClass.annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.Constraint"
        } ?: return false
        val validatorType = constraintAnn.arguments.firstOrNull { it.name?.asString() == "validator" }?.value as? KSType
        val validatorClassDecl = validatorType?.declaration as? KSClassDeclaration
        return validatorClassDecl?.classKind == ClassKind.OBJECT
    }

    override fun validate(target: KSDeclaration, annotation: KSAnnotation, logger: KSPLogger): Boolean {
        return true // Validation is already handled during ConstraintResolver resolution!
    }

    override fun generateCondition(target: KSDeclaration, annotation: KSAnnotation, valName: String): CodeBlock {
        val validatorFqName = resolveValidatorFqName(annotation) ?: return CodeBlock.of("false")
        val validatorCN = ClassName.bestGuess(validatorFqName)
        val validatorPropName = validatorCN.simpleName.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase() + "_INST"

        val property = target as? KSPropertyDeclaration
        val isObjectLevel = property == null

        return if (isObjectLevel) {
            CodeBlock.of(
                "!%L.validate(value, object : %T { " +
                "override val fieldName = %S; " +
                "override val path = %S; " +
                "override val rootObject = value; " +
                "override val groups = groups })",
                validatorPropName,
                ClassName("io.valix.core", "ValidationContext"),
                "", ""
            )
        } else {
            val propName = property.simpleName.asString()
            CodeBlock.of(
                "!%L.validate(%L, object : %T { " +
                "override val fieldName = %S; " +
                "override val path = %S; " +
                "override val rootObject = value; " +
                "override val groups = groups })",
                validatorPropName,
                valName,
                ClassName("io.valix.core", "ValidationContext"),
                propName, propName
            )
        }
    }

    override fun generateAuxiliaryProperties(target: KSDeclaration, annotation: KSAnnotation, propName: String): List<PropertySpec> {
        val validatorFqName = resolveValidatorFqName(annotation) ?: return emptyList()
        val validatorCN = ClassName.bestGuess(validatorFqName)
        val validatorPropName = validatorCN.simpleName.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase() + "_INST"
        val isObject = isValidatorObject(annotation)

        val propSpec = PropertySpec.builder(validatorPropName, validatorCN)
            .addModifiers(KModifier.PRIVATE)

        if (isObject) {
            propSpec.initializer("%T", validatorCN)
        } else {
            propSpec.initializer("%T()", validatorCN)
        }

        return listOf(propSpec.build())
    }
}
