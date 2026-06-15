package io.valix.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ANY
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
import io.valix.ksp.rules.*

class ValixProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var processed = false

    private val ruleRegistry = listOf(
        NotBlankRule, EmailRule, MinLengthRule, MaxLengthRule, PatternRule,
        UrlRule, PhoneNumberRule, AlphaRule, AlphaNumericRule, LowerCaseRule, UpperCaseRule, ContainsRule, StartsWithRule, EndsWithRule,
        MinRule, MaxRule, PositiveRule, PositiveOrZeroRule, NegativeRule, NegativeOrZeroRule, RangeRule,
        NotEmptyRule, SizeRule, AllowedValuesRule, PastRule, PastOrPresentRule, FutureRule, FutureOrPresentRule
    ).associateBy { it.annotationFqName }

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
            "io.valix.annotations.Valid",
            "io.valix.annotations.Url",
            "io.valix.annotations.PhoneNumber",
            "io.valix.annotations.Alpha",
            "io.valix.annotations.AlphaNumeric",
            "io.valix.annotations.LowerCase",
            "io.valix.annotations.UpperCase",
            "io.valix.annotations.Contains",
            "io.valix.annotations.StartsWith",
            "io.valix.annotations.EndsWith",
            "io.valix.annotations.Min",
            "io.valix.annotations.Max",
            "io.valix.annotations.Positive",
            "io.valix.annotations.PositiveOrZero",
            "io.valix.annotations.Negative",
            "io.valix.annotations.NegativeOrZero",
            "io.valix.annotations.Range",
            "io.valix.annotations.NotEmpty",
            "io.valix.annotations.Size",
            "io.valix.annotations.AllowedValues",
            "io.valix.annotations.Past",
            "io.valix.annotations.PastOrPresent",
            "io.valix.annotations.Future",
            "io.valix.annotations.FutureOrPresent"
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

        for (property in properties) {
            val propName = property.simpleName.asString()
            val type = property.type.resolve()
            val isNullable = type.isMarkedNullable

            val hasValid = property.annotations.any {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.Valid"
            }

            val isCollectionType = isCollection(type)

            validateFun.addComment("Validation for %L", propName)
            val valName = "${propName}Val"
            validateFun.addStatement("val %L = value.%L", valName, propName)

            val checksBuilder = CodeBlock.builder()

            // Run rule validations and code generation
            for (ann in property.annotations) {
                val fqName = ann.annotationType.resolve().declaration.qualifiedName?.asString() ?: ""
                if (fqName == "io.valix.annotations.NotNull" || fqName == "io.valix.annotations.Valid") continue

                val rule = ruleRegistry[fqName]
                if (rule != null) {
                    if (!rule.validate(property, ann, logger)) {
                        continue
                    }

                    // Collect auxiliary fields (regex patterns, allowed value sets)
                    val auxProps = rule.generateAuxiliaryProperties(property, ann, propName)
                    for (p in auxProps) {
                        if (validatorObject.propertySpecs.none { it.name == p.name }) {
                            validatorObject.addProperty(p)
                        }
                    }

                    val parsedMessage = ann.arguments.firstOrNull { it.name?.asString() == "message" }?.value as? String ?: ""
                    val finalMsg = if (parsedMessage.isNotEmpty()) parsedMessage else rule.getDefaultMessage(ann)

                    val groups = mutableListOf<String>()
                    val groupsArg = ann.arguments.firstOrNull { it.name?.asString() == "groups" }?.value as? List<*>
                    if (groupsArg != null) {
                        for (g in groupsArg) {
                            if (g is KSType) {
                                val gn = g.declaration.qualifiedName?.asString()
                                if (gn != null) {
                                    groups.add(gn)
                                }
                            }
                        }
                    }

                    val groupCheck = if (groups.isEmpty()) {
                        CodeBlock.of("groups.isEmpty()")
                    } else {
                        val groupMatch = groups.map { CodeBlock.of("it == %T::class", ClassName.bestGuess(it)) }.joinToCode(" || ")
                        CodeBlock.of("groups.isEmpty() || groups.any { %L }", groupMatch)
                    }

                    val condition = rule.generateCondition(property, ann, valName)

                    checksBuilder.beginControlFlow("if (%L)", groupCheck)
                    checksBuilder.beginControlFlow("if (%L)", condition)
                    checksBuilder.addStatement(
                        "errors.add(%T(%S, %S, %S, %L))",
                        ClassName("io.valix.core", "ValidationError"),
                        propName, rule.errorCode, finalMsg, valName
                    )
                    checksBuilder.endControlFlow()
                    checksBuilder.endControlFlow()
                }
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
            var hasNotNull = false
            var notNullMessage = ""
            val notNullGroups = mutableListOf<String>()

            val notNullAnn = property.annotations.firstOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.NotNull"
            }
            if (notNullAnn != null) {
                hasNotNull = true
                notNullMessage = notNullAnn.arguments.firstOrNull { it.name?.asString() == "message" }?.value as? String ?: ""
                val groupsArg = notNullAnn.arguments.firstOrNull { it.name?.asString() == "groups" }?.value as? List<*>
                if (groupsArg != null) {
                    for (g in groupsArg) {
                        if (g is KSType) {
                            val gn = g.declaration.qualifiedName?.asString()
                            if (gn != null) {
                                notNullGroups.add(gn)
                            }
                        }
                    }
                }
            }

            if (isNullable) {
                if (hasNotNull) {
                    val groupCheck = if (notNullGroups.isEmpty()) {
                        CodeBlock.of("groups.isEmpty()")
                    } else {
                        val groupMatch = notNullGroups.map { CodeBlock.of("it == %T::class", ClassName.bestGuess(it)) }.joinToCode(" || ")
                        CodeBlock.of("groups.isEmpty() || groups.any { %L }", groupMatch)
                    }
                    val finalMsg = if (notNullMessage.isNotEmpty()) notNullMessage else "must not be null"

                    validateFun.beginControlFlow("if (%L == null)", valName)
                    validateFun.beginControlFlow("if (%L)", groupCheck)
                    validateFun.addStatement(
                        "errors.add(%T(%S, %S, %S, null))",
                        ClassName("io.valix.core", "ValidationError"),
                        propName, "NOT_NULL", finalMsg
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
