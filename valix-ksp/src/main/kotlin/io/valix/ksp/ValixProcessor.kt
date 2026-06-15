package io.valix.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class ValixProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotations = listOf(
            "io.valix.annotations.NotNull",
            "io.valix.annotations.NotBlank",
            "io.valix.annotations.Email",
            "io.valix.annotations.MinLength",
            "io.valix.annotations.MaxLength",
            "io.valix.annotations.Pattern"
        )

        val classesToValidate = mutableMapOf<KSClassDeclaration, MutableList<KSPropertyDeclaration>>()

        for (annotationName in annotations) {
            val symbols = resolver.getSymbolsWithAnnotation(annotationName)
            for (symbol in symbols) {
                if (symbol is KSPropertyDeclaration) {
                    val classDecl = symbol.parentDeclaration as? KSClassDeclaration
                    if (classDecl != null) {
                        classesToValidate.getOrPut(classDecl) { mutableListOf() }.add(symbol)
                    }
                }
            }
        }

        for ((classDecl, properties) in classesToValidate) {
            val uniqueProperties = properties.distinctBy { it.simpleName.asString() }
            generateValidator(classDecl, uniqueProperties)
        }

        return emptyList()
    }

    private fun generateValidator(classDecl: KSClassDeclaration, properties: List<KSPropertyDeclaration>) {
        val packageName = classDecl.packageName.asString()
        val generatedPackageName = "$packageName.generated"
        val className = classDecl.simpleName.asString()
        val validatorName = "${className}ValixValidator"

        val validatorObject = TypeSpec.objectBuilder(validatorName)

        val validateFun = FunSpec.builder("validate")
            .addParameter("value", classDecl.toClassName())
            .returns(ClassName("io.valix.core", "ValidationResult"))

        validateFun.addStatement("val errors = mutableListOf<%T>()", ClassName("io.valix.core", "ValidationError"))

        var hasEmailRegex = false
        val patternsToGenerate = mutableMapOf<String, String>() // propertyName to regexPattern

        for (property in properties) {
            val propName = property.simpleName.asString()
            val type = property.type.resolve()
            val isNullable = type.isMarkedNullable

            var hasNotNull = false
            var hasNotBlank = false
            var hasEmail = false
            var minLengthValue: Int? = null
            var maxLengthValue: Int? = null
            var patternValue: String? = null

            for (annotation in property.annotations) {
                val qName = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
                when (qName) {
                    "io.valix.annotations.NotNull" -> hasNotNull = true
                    "io.valix.annotations.NotBlank" -> hasNotBlank = true
                    "io.valix.annotations.Email" -> hasEmail = true
                    "io.valix.annotations.MinLength" -> {
                        val arg = annotation.arguments.firstOrNull { it.name?.asString() == "value" } ?: annotation.arguments.firstOrNull()
                        minLengthValue = arg?.value as? Int
                    }
                    "io.valix.annotations.MaxLength" -> {
                        val arg = annotation.arguments.firstOrNull { it.name?.asString() == "value" } ?: annotation.arguments.firstOrNull()
                        maxLengthValue = arg?.value as? Int
                    }
                    "io.valix.annotations.Pattern" -> {
                        val arg = annotation.arguments.firstOrNull { it.name?.asString() == "regexp" } ?: annotation.arguments.firstOrNull()
                        patternValue = arg?.value as? String
                    }
                }
            }

            if (hasEmail) {
                hasEmailRegex = true
            }
            if (patternValue != null) {
                patternsToGenerate[propName] = patternValue
            }

            validateFun.addComment("Validation for %L", propName)
            val valName = "${propName}Val"
            validateFun.addStatement("val %L = value.%L", valName, propName)

            val bodyBuilder = FunSpec.builder("temp") // temporary builder for body blocks
            generateStringChecks(
                bodyBuilder,
                valName,
                propName,
                hasNotBlank,
                hasEmail,
                minLengthValue,
                maxLengthValue,
                patternValue != null
            )
            val checksBody = bodyBuilder.build().body.toString()

            if (isNullable) {
                if (hasNotNull) {
                    validateFun.beginControlFlow("if (%L == null)", valName)
                    validateFun.addStatement(
                        "errors.add(%T(%S, %S, %S))",
                        ClassName("io.valix.core", "ValidationError"),
                        propName, "NOT_NULL", "must not be null"
                    )
                    validateFun.nextControlFlow("else")
                    validateFun.addCode(checksBody)
                    validateFun.endControlFlow()
                } else {
                    validateFun.beginControlFlow("if (%L != null)", valName)
                    validateFun.addCode(checksBody)
                    validateFun.endControlFlow()
                }
            } else {
                validateFun.addCode(checksBody)
            }
        }

        validateFun.addStatement("return %T(errors.isEmpty(), errors)", ClassName("io.valix.core", "ValidationResult"))

        // Add regular expression properties
        if (hasEmailRegex) {
            validatorObject.addProperty(
                PropertySpec.builder("EMAIL_REGEX", Regex::class)
                    .initializer("%T(%S)", Regex::class, "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                    .build()
            )
        }
        for ((propName, pattern) in patternsToGenerate) {
            val propNameUpper = propName.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
            validatorObject.addProperty(
                PropertySpec.builder("PATTERN_${propNameUpper}", Regex::class)
                    .initializer("%T(%S)", Regex::class, pattern)
                    .build()
            )
        }

        validatorObject.addFunction(validateFun.build())

        val fileSpec = FileSpec.builder(generatedPackageName, validatorName)
            .addType(validatorObject.build())
            .build()

        fileSpec.writeTo(codeGenerator, aggregating = false)
    }

    private fun generateStringChecks(
        builder: FunSpec.Builder,
        valName: String,
        propName: String,
        hasNotBlank: Boolean,
        hasEmail: Boolean,
        minLengthValue: Int?,
        maxLengthValue: Int?,
        hasPattern: Boolean
    ) {
        if (hasNotBlank) {
            builder.beginControlFlow("if (%L.trim().isEmpty())", valName)
            builder.addStatement(
                "errors.add(%T(%S, %S, %S))",
                ClassName("io.valix.core", "ValidationError"),
                propName, "NOT_BLANK", "must not be blank"
            )
            builder.endControlFlow()
        }
        if (hasEmail) {
            builder.beginControlFlow("if (!EMAIL_REGEX.matches(%L))", valName)
            builder.addStatement(
                "errors.add(%T(%S, %S, %S))",
                ClassName("io.valix.core", "ValidationError"),
                propName, "EMAIL_INVALID", "invalid email"
            )
            builder.endControlFlow()
        }
        if (minLengthValue != null) {
            builder.beginControlFlow("if (%L.length < %L)", valName, minLengthValue)
            builder.addStatement(
                "errors.add(%T(%S, %S, %S))",
                ClassName("io.valix.core", "ValidationError"),
                propName, "MIN_LENGTH", "minimum length is $minLengthValue"
            )
            builder.endControlFlow()
        }
        if (maxLengthValue != null) {
            builder.beginControlFlow("if (%L.length > %L)", valName, maxLengthValue)
            builder.addStatement(
                "errors.add(%T(%S, %S, %S))",
                ClassName("io.valix.core", "ValidationError"),
                propName, "MAX_LENGTH", "maximum length is $maxLengthValue"
            )
            builder.endControlFlow()
        }
        if (hasPattern) {
            val propNameUpper = propName.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
            builder.beginControlFlow("if (!PATTERN_${propNameUpper}.matches(%L))", valName)
            builder.addStatement(
                "errors.add(%T(%S, %S, %S))",
                ClassName("io.valix.core", "ValidationError"),
                propName, "PATTERN_MISMATCH", "pattern mismatch"
            )
            builder.endControlFlow()
        }
    }
}
