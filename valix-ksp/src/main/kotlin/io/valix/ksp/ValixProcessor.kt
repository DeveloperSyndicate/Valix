package io.valix.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class ValixProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var processed = false

    data class ConstraintInfo(
        val annotationName: String,
        val message: String,
        val groups: List<String>, // fully qualified group names
        val valueInt: Int? = null,
        val regexpStr: String? = null
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) return emptyList()
        processed = true

        val annotations = listOf(
            "io.valix.annotations.NotNull",
            "io.valix.annotations.NotBlank",
            "io.valix.annotations.Email",
            "io.valix.annotations.MinLength",
            "io.valix.annotations.MaxLength",
            "io.valix.annotations.Pattern",
            "io.valix.annotations.Valid"
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

        val generatedClasses = mutableListOf<KSClassDeclaration>()

        for ((classDecl, properties) in classesToValidate) {
            val uniqueProperties = properties.distinctBy { it.simpleName.asString() }
            generateValidator(resolver, classDecl, uniqueProperties)
            generatedClasses.add(classDecl)
        }

        if (generatedClasses.isNotEmpty()) {
            generateRegistry(generatedClasses)
        }

        return emptyList()
    }

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

    private fun parseAnnotation(annotation: KSAnnotation): ConstraintInfo {
        val qName = annotation.annotationType.resolve().declaration.qualifiedName?.asString() ?: ""
        var message = ""
        val groups = mutableListOf<String>()
        var valueInt: Int? = null
        var regexpStr: String? = null

        for (arg in annotation.arguments) {
            val name = arg.name?.asString()
            when (name) {
                "message" -> message = arg.value as? String ?: ""
                "groups" -> {
                    val groupList = arg.value as? List<*>
                    if (groupList != null) {
                        for (g in groupList) {
                            if (g is KSType) {
                                val gn = g.declaration.qualifiedName?.asString()
                                if (gn != null) {
                                    groups.add(gn)
                                }
                            }
                        }
                    }
                }
                "value" -> valueInt = arg.value as? Int
                "regexp" -> regexpStr = arg.value as? String
            }
        }

        return ConstraintInfo(qName, message, groups, valueInt, regexpStr)
    }

    private fun generateValidator(
        resolver: Resolver,
        classDecl: KSClassDeclaration,
        properties: List<KSPropertyDeclaration>
    ) {
        val packageName = classDecl.packageName.asString()
        val generatedPackageName = "$packageName.generated"
        val className = classDecl.simpleName.asString()
        val validatorName = "${className}Validator"
        val valixValidatorName = "${className}ValixValidator"

        val validatorObject = TypeSpec.objectBuilder(validatorName)

        val kclassType = ClassName("kotlin.reflect", "KClass").parameterizedBy(WildcardTypeName.producerOf(ANY))
        val validateFun = FunSpec.builder("validate")
            .addParameter("value", classDecl.toClassName())
            .addParameter(
                ParameterSpec.builder("groups", kclassType, KModifier.VARARG).build()
            )
            .returns(ClassName("io.valix.core", "ValidationResult"))

        validateFun.addStatement("val errors = mutableListOf<%T>()", ClassName("io.valix.core", "ValidationError"))

        var hasEmailRegex = false
        val patternsToGenerate = mutableMapOf<String, String>() // propertyName to regexPattern

        for (property in properties) {
            val propName = property.simpleName.asString()
            val type = property.type.resolve()
            val isNullable = type.isMarkedNullable

            val constraints = property.annotations
                .filter { it.annotationType.resolve().declaration.qualifiedName?.asString() != "io.valix.annotations.Valid" }
                .map { parseAnnotation(it) }

            val hasValid = property.annotations.any {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.Valid"
            }

            // Check if collection type
            val isCollectionType = isCollection(type)

            validateFun.addComment("Validation for %L", propName)
            val valName = "${propName}Val"
            validateFun.addStatement("val %L = value.%L", valName, propName)

            val checksBuilder = CodeBlock.builder()

            // Generate direct string/constraint checks
            for (constraint in constraints) {
                if (constraint.annotationName == "io.valix.annotations.NotNull") continue

                val defaultMsg = when (constraint.annotationName) {
                    "io.valix.annotations.NotBlank" -> "must not be blank"
                    "io.valix.annotations.Email" -> "invalid email"
                    "io.valix.annotations.MinLength" -> "minimum length is ${constraint.valueInt}"
                    "io.valix.annotations.MaxLength" -> "maximum length is ${constraint.valueInt}"
                    "io.valix.annotations.Pattern" -> "pattern mismatch"
                    else -> ""
                }
                val finalMsg = if (constraint.message.isNotEmpty()) constraint.message else defaultMsg

                val code = when (constraint.annotationName) {
                    "io.valix.annotations.NotBlank" -> "NOT_BLANK"
                    "io.valix.annotations.Email" -> "EMAIL_INVALID"
                    "io.valix.annotations.MinLength" -> "MIN_LENGTH"
                    "io.valix.annotations.MaxLength" -> "MAX_LENGTH"
                    "io.valix.annotations.Pattern" -> "PATTERN_MISMATCH"
                    else -> ""
                }

                if (constraint.annotationName == "io.valix.annotations.Email") {
                    hasEmailRegex = true
                }
                if (constraint.annotationName == "io.valix.annotations.Pattern" && constraint.regexpStr != null) {
                    patternsToGenerate[propName] = constraint.regexpStr
                }

                val checkCondition = when (constraint.annotationName) {
                    "io.valix.annotations.NotBlank" -> CodeBlock.of("%L.trim().isEmpty()", valName)
                    "io.valix.annotations.Email" -> CodeBlock.of("!EMAIL_REGEX.matches(%L)", valName)
                    "io.valix.annotations.MinLength" -> CodeBlock.of("%L.length < %L", valName, constraint.valueInt)
                    "io.valix.annotations.MaxLength" -> CodeBlock.of("%L.length > %L", valName, constraint.valueInt)
                    "io.valix.annotations.Pattern" -> {
                        val propNameUpper = propName.replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
                        CodeBlock.of("!PATTERN_%L.matches(%L)", propNameUpper, valName)
                    }
                    else -> CodeBlock.of("false")
                }

                val groupCheck = if (constraint.groups.isEmpty()) {
                    CodeBlock.of("groups.isEmpty()")
                } else {
                    val groupMatch = constraint.groups.map { CodeBlock.of("it == %T::class", ClassName.bestGuess(it)) }.joinToCode(" || ")
                    CodeBlock.of("groups.isEmpty() || groups.any { %L }", groupMatch)
                }

                checksBuilder.beginControlFlow("if (%L)", groupCheck)
                checksBuilder.beginControlFlow("if (%L)", checkCondition)
                checksBuilder.addStatement(
                    "errors.add(%T(%S, %S, %S, %L))",
                    ClassName("io.valix.core", "ValidationError"),
                    propName, code, finalMsg, valName
                )
                checksBuilder.endControlFlow()
                checksBuilder.endControlFlow()
            }

            // Generate nested or collection @Valid checks
            if (hasValid) {
                if (isCollectionType) {
                    val elementType = type.arguments.firstOrNull()?.type?.resolve()
                    val elementClass = elementType?.declaration as? KSClassDeclaration
                    if (elementClass != null) {
                        val elementValidatorCN = ClassName("${elementClass.packageName.asString()}.generated", "${elementClass.simpleName.asString()}Validator")
                        val isElementNullable = elementType.isMarkedNullable
                        checksBuilder.beginControlFlow("%L.forEachIndexed { index, item ->", valName)
                        if (isElementNullable) {
                            checksBuilder.beginControlFlow("if (item != null)")
                        }
                        checksBuilder.addStatement("val itemResult = %T.validate(item, *groups)", elementValidatorCN)
                        checksBuilder.beginControlFlow("itemResult.errors.forEach { error ->")
                        checksBuilder.addStatement(
                            "errors.add(%T(%S + index + %S + error.field, error.code, error.message, error.rejectedValue))",
                            ClassName("io.valix.core", "ValidationError"),
                            "$propName[", "]."
                        )
                        checksBuilder.endControlFlow()
                        if (isElementNullable) {
                            checksBuilder.endControlFlow()
                        }
                        checksBuilder.endControlFlow()
                    }
                } else {
                    val nestedClass = type.declaration as? KSClassDeclaration
                    if (nestedClass != null) {
                        val nestedValidatorCN = ClassName("${nestedClass.packageName.asString()}.generated", "${nestedClass.simpleName.asString()}Validator")
                        checksBuilder.addStatement("val nestedResult = %T.validate(%L, *groups)", nestedValidatorCN, valName)
                        checksBuilder.beginControlFlow("nestedResult.errors.forEach { error ->")
                        checksBuilder.addStatement(
                            "errors.add(%T(%S + error.field, error.code, error.message, error.rejectedValue))",
                            ClassName("io.valix.core", "ValidationError"),
                            "$propName."
                        )
                        checksBuilder.endControlFlow()
                    }
                }
            }

            val checksBody = checksBuilder.build()

            // Handle NotNull / Nullability
            val notNullConstraint = constraints.firstOrNull { it.annotationName == "io.valix.annotations.NotNull" }
            if (isNullable) {
                if (notNullConstraint != null) {
                    val groupCheck = if (notNullConstraint.groups.isEmpty()) {
                        CodeBlock.of("groups.isEmpty()")
                    } else {
                        val groupMatch = notNullConstraint.groups.map { CodeBlock.of("it == %T::class", ClassName.bestGuess(it)) }.joinToCode(" || ")
                        CodeBlock.of("groups.isEmpty() || groups.any { %L }", groupMatch)
                    }
                    val notNullMsg = if (notNullConstraint.message.isNotEmpty()) notNullConstraint.message else "must not be null"

                    validateFun.beginControlFlow("if (%L == null)", valName)
                    validateFun.beginControlFlow("if (%L)", groupCheck)
                    validateFun.addStatement(
                        "errors.add(%T(%S, %S, %S, null))",
                        ClassName("io.valix.core", "ValidationError"),
                        propName, "NOT_NULL", notNullMsg
                    )
                    validateFun.endControlFlow()
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
                    .initializer("%T(%S)", Regex::class, "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}${'$'}")
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

        // File 1: classNameValidator.kt
        val fileSpec1 = FileSpec.builder(generatedPackageName, validatorName)
            .addType(validatorObject.build())
            .build()
        fileSpec1.writeTo(codeGenerator, aggregating = false)

        // File 2: classNameValixValidator.kt (Backward Compatibility)
        val valixValidatorObject = TypeSpec.objectBuilder(valixValidatorName)
        val valixValidateFun = FunSpec.builder("validate")
            .addParameter("value", classDecl.toClassName())
            .addParameter(
                ParameterSpec.builder("groups", kclassType, KModifier.VARARG).build()
            )
            .returns(ClassName("io.valix.core", "ValidationResult"))
            .addStatement("return %T.validate(value, *groups)", ClassName(generatedPackageName, validatorName))
            .build()

        valixValidatorObject.addFunction(valixValidateFun)

        val fileSpec2 = FileSpec.builder(generatedPackageName, valixValidatorName)
            .addType(valixValidatorObject.build())
            .build()
        fileSpec2.writeTo(codeGenerator, aggregating = false)
    }

    private fun generateRegistry(classes: List<KSClassDeclaration>) {
        val generatedPackageName = "io.valix.generated"
        val registryName = "ValixRegistry"

        val registryObject = TypeSpec.objectBuilder(registryName)

        val kclassCN = ClassName("kotlin.reflect", "KClass").parameterizedBy(WildcardTypeName.producerOf(ANY))
        val arrayKclass = ClassName("kotlin", "Array").parameterizedBy(WildcardTypeName.producerOf(kclassCN))
        val lambdaCN = LambdaTypeName.get(
            parameters = listOf(ANY, arrayKclass).map { ParameterSpec.unnamed(it) },
            returnType = ClassName("io.valix.core", "ValidationResult")
        )
        val mapCN = ClassName("kotlin.collections", "Map").parameterizedBy(kclassCN, lambdaCN)

        val mapInitializer = CodeBlock.builder()
        mapInitializer.add("mapOf(\n")
        for (i in classes.indices) {
            val clazz = classes[i]
            val classCN = clazz.toClassName()
            val validatorCN = ClassName("${clazz.packageName.asString()}.generated", "${clazz.simpleName.asString()}Validator")

            mapInitializer.add(
                "  %T::class to { value, groups -> (value as %T); %T.validate(value, *groups) }",
                classCN, classCN, validatorCN
            )
            if (i < classes.size - 1) {
                mapInitializer.add(",\n")
            }
        }
        mapInitializer.add("\n)")

        val validatorsProp = PropertySpec.builder("validators", mapCN)
            .addModifiers(KModifier.PRIVATE)
            .initializer(mapInitializer.build())
            .build()

        registryObject.addProperty(validatorsProp)

        val validateFun = FunSpec.builder("validate")
            .addParameter("value", ANY)
            .addParameter(
                ParameterSpec.builder("groups", kclassCN, KModifier.VARARG).build()
            )
            .returns(ClassName("io.valix.core", "ValidationResult"))
            .addStatement("val validator = validators[value::class] ?: return %T(true, emptyList())", ClassName("io.valix.core", "ValidationResult"))
            .addStatement("return validator(value, groups)")
            .build()

        registryObject.addFunction(validateFun)

        val fileSpec = FileSpec.builder(generatedPackageName, registryName)
            .addType(registryObject.build())
            .build()

        fileSpec.writeTo(codeGenerator, aggregating = false)
    }
}
