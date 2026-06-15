package io.valix.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
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

class ValixProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private var processed = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (processed) return emptyList()
        processed = true

        val allClassDecls = mutableListOf<KSClassDeclaration>()
        for (file in resolver.getNewFiles()) {
            for (decl in file.declarations) {
                findClassDeclarations(decl, allClassDecls)
            }
        }

        val generatedClasses = mutableListOf<KSClassDeclaration>()

        for (classDecl in allClassDecls) {
            if (classDecl.classKind == com.google.devtools.ksp.symbol.ClassKind.ANNOTATION_CLASS ||
                classDecl.classKind == com.google.devtools.ksp.symbol.ClassKind.INTERFACE) {
                continue
            }
            val classDescriptors = ConstraintResolver.resolveClassConstraints(classDecl, resolver, logger)
            val propertyDescriptors = mutableMapOf<KSPropertyDeclaration, List<ConstraintDescriptor>>()

            for (property in classDecl.getAllProperties()) {
                val descriptors = ConstraintResolver.resolvePropertyConstraints(property, resolver, logger)
                val hasNotNull = property.annotations.any {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.NotNull"
                }
                val hasValid = property.annotations.any {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.Valid"
                }
                if (descriptors.isNotEmpty() || hasNotNull || hasValid) {
                    propertyDescriptors[property] = descriptors
                }
            }

            if (classDescriptors.isNotEmpty() || propertyDescriptors.isNotEmpty()) {
                generateValidator(resolver, classDecl, classDescriptors, propertyDescriptors)
                generateMetadata(classDecl, classDescriptors, propertyDescriptors)
                generatedClasses.add(classDecl)
            }
        }

        if (generatedClasses.isNotEmpty()) {
            generateRegistry(generatedClasses)
        }

        return emptyList()
    }

    private fun findClassDeclarations(decl: KSDeclaration, result: MutableList<KSClassDeclaration>) {
        if (decl is KSClassDeclaration) {
            result.add(decl)
            for (subDecl in decl.declarations) {
                findClassDeclarations(subDecl, result)
            }
        }
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
        classDescriptors: List<ConstraintDescriptor>,
        propertyDescriptors: Map<KSPropertyDeclaration, List<ConstraintDescriptor>>
    ) {
        val packageName = classDecl.packageName.asString()
        val generatedPackageName = "$packageName.generated"
        val className = classDecl.simpleName.asString()
        val validatorName = "${className}Validator"
        val valixValidatorName = "${className}ValixValidator"

        val validatorObject = TypeSpec.objectBuilder(validatorName)
            .addSuperinterface(ClassName("io.valix.core", "ValixValidator").parameterizedBy(classDecl.toClassName()))

        val kclassType = ClassName("kotlin.reflect", "KClass").parameterizedBy(WildcardTypeName.producerOf(ANY))
        val validateFun = FunSpec.builder("validate")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("value", classDecl.toClassName())
            .addParameter(
                ParameterSpec.builder("groups", kclassType, KModifier.VARARG).build()
            )
            .returns(ClassName("io.valix.core", "ValidationResult"))

        validateFun.addStatement("val errors = mutableListOf<%T>()", ClassName("io.valix.core", "ValidationError"))

        // Class-level validation
        for (desc in classDescriptors) {
            val fqName = desc.annotationFqName
            val generator = if (desc.validatorFqName != null) {
                CustomValidatorGenerator
            } else {
                PluginRegistry.getGenerator(fqName)
            }

            if (generator != null) {
                if (!generator.validate(classDecl, desc.annotation, logger)) {
                    continue
                }

                // Add auxiliary properties
                val auxProps = generator.generateAuxiliaryProperties(classDecl, desc.annotation, className)
                for (p in auxProps) {
                    if (validatorObject.propertySpecs.none { it.name == p.name }) {
                        validatorObject.addProperty(p)
                    }
                }

                val finalMsg = if (desc.message.isNotEmpty()) desc.message else generator.getDefaultMessage(desc.annotation)

                val groupCheck = if (desc.groups.isEmpty()) {
                    CodeBlock.of("groups.isEmpty()")
                } else {
                    val groupMatch = desc.groups.map { CodeBlock.of("it == %T::class", ClassName.bestGuess(it)) }.joinToCode(" || ")
                    CodeBlock.of("groups.isEmpty() || groups.any { %L }", groupMatch)
                }

                val condition = generator.generateCondition(classDecl, desc.annotation, "value")
                val errorField = generator.getErrorField(classDecl, desc.annotation, "")
                val rejectedValueExpr = generator.getRejectedValueExpression(classDecl, desc.annotation, "value")

                validateFun.addComment("Class-level validation: %L", fqName)
                validateFun.beginControlFlow("if (%L)", groupCheck)
                validateFun.beginControlFlow("if (%L)", condition)
                val resolvedMessageKey = desc.messageKey.ifEmpty {
                    val shortName = desc.annotation.annotationType.resolve().declaration.simpleName.asString().lowercase()
                    "valix.$shortName"
                }
                validateFun.addStatement(
                    "errors.add(%T(field = %S, code = %S, message = %S, messageKey = %S, rejectedValue = %L, constraint = %S, path = %S))",
                    ClassName("io.valix.core", "ValidationError"),
                    errorField, generator.errorCode, finalMsg, resolvedMessageKey, rejectedValueExpr, fqName, errorField
                )
                validateFun.endControlFlow()
                validateFun.endControlFlow()
            }
        }

        // Property-level validation
        for ((property, descriptors) in propertyDescriptors) {
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

            // Run constraint descriptors validations and code generation
            for (desc in descriptors) {
                val fqName = desc.annotationFqName
                val generator = if (desc.validatorFqName != null) {
                    CustomValidatorGenerator
                } else {
                    PluginRegistry.getGenerator(fqName)
                }

                if (generator != null) {
                    if (!generator.validate(property, desc.annotation, logger)) {
                        continue
                    }

                    // Collect auxiliary fields (e.g. regex patterns, allowed value sets)
                    val auxProps = generator.generateAuxiliaryProperties(property, desc.annotation, propName)
                    for (p in auxProps) {
                        if (validatorObject.propertySpecs.none { it.name == p.name }) {
                            validatorObject.addProperty(p)
                        }
                    }

                    val finalMsg = if (desc.message.isNotEmpty()) desc.message else generator.getDefaultMessage(desc.annotation)

                    val groupCheck = if (desc.groups.isEmpty()) {
                        CodeBlock.of("groups.isEmpty()")
                    } else {
                        val groupMatch = desc.groups.map { CodeBlock.of("it == %T::class", ClassName.bestGuess(it)) }.joinToCode(" || ")
                        CodeBlock.of("groups.isEmpty() || groups.any { %L }", groupMatch)
                    }

                    val condition = generator.generateCondition(property, desc.annotation, valName)

                    val errorField = generator.getErrorField(property, desc.annotation, propName)
                    val rejectedValueExpr = generator.getRejectedValueExpression(property, desc.annotation, valName)

                    checksBuilder.beginControlFlow("if (%L)", groupCheck)
                    checksBuilder.beginControlFlow("if (%L)", condition)
                    val resolvedMessageKey = desc.messageKey.ifEmpty {
                        val shortName = desc.annotation.annotationType.resolve().declaration.simpleName.asString().lowercase()
                        "valix.$shortName"
                    }
                    checksBuilder.addStatement(
                        "errors.add(%T(field = %S, code = %S, message = %S, messageKey = %S, rejectedValue = %L, constraint = %S, path = %S))",
                        ClassName("io.valix.core", "ValidationError"),
                        errorField, generator.errorCode, finalMsg, resolvedMessageKey, rejectedValueExpr, fqName, errorField
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
                            "errors.add(%T(field = %S + index + %S + error.field, code = error.code, message = error.message, rejectedValue = error.rejectedValue, constraint = error.constraint, path = %S + index + %S + error.path))",
                            ClassName("io.valix.core", "ValidationError"),
                            "$propName[", "].",
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
                            "errors.add(%T(field = %S + error.field, code = error.code, message = error.message, rejectedValue = error.rejectedValue, constraint = error.constraint, path = %S + error.path))",
                            ClassName("io.valix.core", "ValidationError"),
                            "$propName.",
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
                val groupsArg = notNullAnn.arguments.firstOrNull { notNullAnn.arguments.isNotEmpty() && it.name?.asString() == "groups" }?.value as? List<*>
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
                    val resolvedMessageKey = notNullAnn!!.arguments.firstOrNull { it.name?.asString() == "messageKey" }?.value as? String ?: ""
                    val finalMessageKey = resolvedMessageKey.ifEmpty { "valix.notnull" }
                    validateFun.addStatement(
                        "errors.add(%T(field = %S, code = %S, message = %S, messageKey = %S, rejectedValue = null, constraint = %S, path = %S))",
                        ClassName("io.valix.core", "ValidationError"),
                        propName, "NOT_NULL", finalMsg, finalMessageKey, "io.valix.annotations.NotNull", propName
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

        val originatingFiles = classDecl.containingFile?.let { arrayOf(it) } ?: emptyArray()
        val dependencies = Dependencies(aggregating = false, *originatingFiles)

        // File 1: classNameValidator.kt
        val fileSpec1 = FileSpec.builder(generatedPackageName, validatorName)
            .addType(validatorObject.build())
            .build()
        fileSpec1.writeTo(codeGenerator, dependencies)

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
        fileSpec2.writeTo(codeGenerator, dependencies)
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

        // Metadata registry auto-registration in ValixRegistry init block
        val initBlock = CodeBlock.builder()
        for (clazz in classes) {
            val metadataCN = ClassName("${clazz.packageName.asString()}.generated", "${clazz.simpleName.asString()}ValidationMetadata")
            initBlock.addStatement("%T.register(%T)", ClassName("io.valix.metadata", "MetadataRegistry"), metadataCN)
        }
        registryObject.addInitializerBlock(initBlock.build())

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

        val originatingFiles = classes.mapNotNull { it.containingFile }.toTypedArray()
        val dependencies = Dependencies(aggregating = true, *originatingFiles)
        fileSpec.writeTo(codeGenerator, dependencies)
    }

    private fun generateMetadata(
        classDecl: KSClassDeclaration,
        classDescriptors: List<ConstraintDescriptor>,
        propertyDescriptors: Map<KSPropertyDeclaration, List<ConstraintDescriptor>>
    ) {
        val packageName = classDecl.packageName.asString()
        val generatedPackageName = "$packageName.generated"
        val className = classDecl.simpleName.asString()
        val metadataObjectName = "${className}ValidationMetadata"

        val metadataObject = TypeSpec.objectBuilder(metadataObjectName)
            .addSuperinterface(ClassName("io.valix.metadata", "ValixModelMetadata"))

        metadataObject.addProperty(
            PropertySpec.builder("modelFqName", String::class, KModifier.OVERRIDE)
                .initializer("%S", classDecl.qualifiedName?.asString() ?: "")
                .build()
        )

        metadataObject.addProperty(
            PropertySpec.builder("modelSimpleName", String::class, KModifier.OVERRIDE)
                .initializer("%S", className)
                .build()
        )

        metadataObject.addProperty(
            PropertySpec.builder("schemaVersion", Int::class, KModifier.OVERRIDE)
                .initializer("1")
                .build()
        )

        metadataObject.addProperty(
            PropertySpec.builder("metadataVersion", String::class, KModifier.OVERRIDE)
                .initializer("%S", "1.0.0")
                .build()
        )

        val fieldsCodeBlock = CodeBlock.builder()
        fieldsCodeBlock.add("listOf(\n").indent()

        val properties = classDecl.getAllProperties().toList()
        for (i in properties.indices) {
            val property = properties[i]
            val propName = property.simpleName.asString()
            val type = property.type.resolve()
            val typeFqn = type.declaration.qualifiedName?.asString() ?: "kotlin.Any"
            val nullable = type.isMarkedNullable

            val notNullAnn = property.annotations.firstOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.NotNull"
            }
            val hasNotNull = notNullAnn != null
            val required = !nullable || hasNotNull

            val docAnn = property.annotations.firstOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.ValixDoc"
            }
            val displayName = docAnn?.arguments?.firstOrNull { it.name?.asString() == "displayName" }?.value as? String ?: ""
            val description = docAnn?.arguments?.firstOrNull { it.name?.asString() == "description" }?.value as? String ?: ""
            val finalDisplayName = displayName.ifEmpty { propName }

            val descriptors = propertyDescriptors[property] ?: emptyList()
            val constraintsBlocks = mutableListOf<CodeBlock>()

            if (hasNotNull) {
                val message = notNullAnn.arguments.firstOrNull { it.name?.asString() == "message" }?.value as? String ?: ""
                val messageKey = notNullAnn.arguments.firstOrNull { it.name?.asString() == "messageKey" }?.value as? String ?: ""
                val resolvedMessageKey = messageKey.ifEmpty { "valix.notnull" }
                val notNullGroups = mutableListOf<String>()
                val groupsArg = notNullAnn.arguments.firstOrNull { it.name?.asString() == "groups" }?.value as? List<*>
                if (groupsArg != null) {
                    for (g in groupsArg) {
                        if (g is KSType) {
                            g.declaration.qualifiedName?.asString()?.let { notNullGroups.add(it) }
                        }
                    }
                }
                val groupsCode = if (notNullGroups.isEmpty()) {
                    CodeBlock.of("emptyList()")
                } else {
                    val groupItems = notNullGroups.map { CodeBlock.of("%S", it) }.joinToCode(", ")
                    CodeBlock.of("listOf(%L)", groupItems)
                }
                constraintsBlocks.add(
                    CodeBlock.of(
                        "%T(annotationFqName = %S, constraintCode = %S, messageKey = %S, defaultMessage = %S, params = emptyMap(), groups = %L, isCustom = false, schemaKeyword = %T.REQUIRED)",
                        ClassName("io.valix.metadata", "ConstraintMetadata"),
                        "io.valix.annotations.NotNull",
                        "NOT_NULL",
                        resolvedMessageKey,
                        message.ifEmpty { "must not be null" },
                        groupsCode,
                        ClassName("io.valix.metadata", "SchemaKeyword")
                    )
                )
            }

            for (desc in descriptors) {
                constraintsBlocks.add(generateConstraintMetadataCode(desc))
            }

            val constraintsListCode = if (constraintsBlocks.isEmpty()) {
                CodeBlock.of("emptyList()")
            } else {
                CodeBlock.builder().add("listOf(\n").indent()
                    .add(constraintsBlocks.joinToCode(",\n"))
                    .unindent().add("\n)").build()
            }

            fieldsCodeBlock.add(
                "%T(name = %S, type = %S, nullable = %L, required = %L, constraints = %L, displayName = %S, description = %S)",
                ClassName("io.valix.metadata", "FieldMetadata"),
                propName,
                typeFqn,
                nullable,
                required,
                constraintsListCode,
                finalDisplayName,
                description
            )
            if (i < properties.size - 1) {
                fieldsCodeBlock.add(",\n")
            }
        }
        fieldsCodeBlock.unindent().add("\n)")

        metadataObject.addProperty(
            PropertySpec.builder("fields", ClassName("kotlin.collections", "List").parameterizedBy(ClassName("io.valix.metadata", "FieldMetadata")), KModifier.OVERRIDE)
                .initializer(fieldsCodeBlock.build())
                .build()
        )

        val classConstraintsBlocks = mutableListOf<CodeBlock>()
        for (desc in classDescriptors) {
            classConstraintsBlocks.add(generateConstraintMetadataCode(desc))
        }
        val classConstraintsCode = if (classConstraintsBlocks.isEmpty()) {
            CodeBlock.of("emptyList()")
        } else {
            CodeBlock.builder().add("listOf(\n").indent()
                .add(classConstraintsBlocks.joinToCode(",\n"))
                .unindent().add("\n)").build()
        }

        metadataObject.addProperty(
            PropertySpec.builder("classConstraints", ClassName("kotlin.collections", "List").parameterizedBy(ClassName("io.valix.metadata", "ConstraintMetadata")), KModifier.OVERRIDE)
                .initializer(classConstraintsCode)
                .build()
        )

        val allGroups = mutableSetOf<String>()
        for (desc in classDescriptors) {
            allGroups.addAll(desc.groups)
        }
        for (descs in propertyDescriptors.values) {
            for (desc in descs) {
                allGroups.addAll(desc.groups)
            }
        }
        val groupsListCode = if (allGroups.isEmpty()) {
            CodeBlock.of("emptyList()")
        } else {
            val groupItems = allGroups.map { CodeBlock.of("%S", it) }.joinToCode(", ")
            CodeBlock.of("listOf(%L)", groupItems)
        }

        metadataObject.addProperty(
            PropertySpec.builder("groups", ClassName("kotlin.collections", "List").parameterizedBy(ClassName("kotlin", "String")), KModifier.OVERRIDE)
                .initializer(groupsListCode)
                .build()
        )

        val fileSpec = FileSpec.builder(generatedPackageName, metadataObjectName)
            .addType(metadataObject.build())
            .build()

        val originatingFiles = classDecl.containingFile?.let { arrayOf(it) } ?: emptyArray()
        val dependencies = Dependencies(aggregating = false, *originatingFiles)
        fileSpec.writeTo(codeGenerator, dependencies)
    }

    private fun generateConstraintMetadataCode(desc: ConstraintDescriptor): CodeBlock {
        val fqName = desc.annotationFqName
        val generator = if (desc.validatorFqName != null) {
            CustomValidatorGenerator
        } else {
            PluginRegistry.getGenerator(fqName)
        }

        val errorCode = generator?.errorCode ?: "CUSTOM"
        val defaultMessage = generator?.defaultMessage ?: ""
        val isCustom = desc.validatorFqName != null

        val schemaKeywordVal = when (fqName) {
            "io.valix.annotations.MinLength" -> "MIN_LENGTH"
            "io.valix.annotations.MaxLength" -> "MAX_LENGTH"
            "io.valix.annotations.Pattern" -> "PATTERN"
            "io.valix.annotations.Email" -> "FORMAT_EMAIL"
            "io.valix.annotations.Url" -> "FORMAT_URI"
            "io.valix.annotations.Min" -> "MINIMUM"
            "io.valix.annotations.Max" -> "MAXIMUM"
            "io.valix.annotations.NotEmpty" -> "NOT_EMPTY"
            "io.valix.annotations.Size" -> "CUSTOM"
            "io.valix.annotations.AllowedValues" -> "ENUM_VALUES"
            else -> "NONE"
        }

        val paramsCode = CodeBlock.builder().add("mapOf(")
        val args = desc.annotation.arguments.filter {
            val name = it.name?.asString()
            name != "message" && name != "messageKey" && name != "groups"
        }
        for (i in args.indices) {
            val arg = args[i]
            val name = arg.name?.asString() ?: ""
            val value = arg.value
            if (value != null) {
                val valExpr = if (value is String) {
                    CodeBlock.of("%S", value)
                } else if (value is Number || value is Boolean) {
                    CodeBlock.of("%L", value)
                } else if (value is Collection<*>) {
                    val items = value.map { if (it is String) CodeBlock.of("%S", it) else CodeBlock.of("%L", it) }.joinToCode(", ")
                    CodeBlock.of("listOf(%L)", items)
                } else if (value is Array<*>) {
                    val items = value.map { if (it is String) CodeBlock.of("%S", it) else CodeBlock.of("%L", it) }.joinToCode(", ")
                    CodeBlock.of("listOf(%L)", items)
                } else {
                    CodeBlock.of("%S", value.toString())
                }
                paramsCode.add("%S to %L", name, valExpr)
                if (i < args.size - 1) {
                    paramsCode.add(", ")
                }
            }
        }
        paramsCode.add(")")

        val groupsCode = if (desc.groups.isEmpty()) {
            CodeBlock.of("emptyList()")
        } else {
            val groupItems = desc.groups.map { CodeBlock.of("%S", it) }.joinToCode(", ")
            CodeBlock.of("listOf(%L)", groupItems)
        }

        val resolvedMessageKey = desc.messageKey.ifEmpty {
            val shortName = desc.annotation.annotationType.resolve().declaration.simpleName.asString().lowercase()
            "valix.$shortName"
        }

        return CodeBlock.of(
            "%T(annotationFqName = %S, constraintCode = %S, messageKey = %S, defaultMessage = %S, params = %L, groups = %L, isCustom = %L, schemaKeyword = %T.%L)",
            ClassName("io.valix.metadata", "ConstraintMetadata"),
            fqName,
            errorCode,
            resolvedMessageKey,
            desc.message.ifEmpty { defaultMessage },
            paramsCode.build(),
            groupsCode,
            isCustom,
            ClassName("io.valix.metadata", "SchemaKeyword"),
            schemaKeywordVal
        )
    }
}
