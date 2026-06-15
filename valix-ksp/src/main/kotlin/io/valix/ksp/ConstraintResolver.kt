package io.valix.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

object ConstraintResolver {

    data class ValidatorInterfaceInfo(
        val isObjectValidator: Boolean,
        val expectedType: KSType
    )

    private fun findValidatorInterface(classDecl: KSClassDeclaration): ValidatorInterfaceInfo? {
        for (superTypeRef in classDecl.superTypes) {
            val superType = superTypeRef.resolve()
            val decl = superType.declaration as? KSClassDeclaration ?: continue
            val qName = decl.qualifiedName?.asString()
            if (qName == "io.valix.core.ConstraintValidator") {
                val expected = superType.arguments.firstOrNull()?.type?.resolve()
                if (expected != null) {
                    return ValidatorInterfaceInfo(isObjectValidator = false, expectedType = expected)
                }
            } else if (qName == "io.valix.core.ObjectConstraintValidator") {
                val expected = superType.arguments.firstOrNull()?.type?.resolve()
                if (expected != null) {
                    return ValidatorInterfaceInfo(isObjectValidator = true, expectedType = expected)
                }
            } else {
                val nested = findValidatorInterface(decl)
                if (nested != null) return nested
            }
        }
        return null
    }

    fun resolvePropertyConstraints(
        property: KSPropertyDeclaration,
        resolver: Resolver,
        logger: KSPLogger
    ): List<ConstraintDescriptor> {
        val targetType = property.type.resolve()
        val directAnnotations = property.annotations.filter {
            val fqName = it.annotationType.resolve().declaration.qualifiedName?.asString()
            fqName != "io.valix.annotations.Valid" && fqName != "io.valix.annotations.NotNull"
        }.toList()

        return resolveRecursive(directAnnotations, targetType, property, resolver, logger, isObjectLevel = false)
    }

    fun resolveClassConstraints(
        classDecl: KSClassDeclaration,
        resolver: Resolver,
        logger: KSPLogger
    ): List<ConstraintDescriptor> {
        val targetType = classDecl.asStarProjectedType()
        val directAnnotations = classDecl.annotations.filter {
            val fqName = it.annotationType.resolve().declaration.qualifiedName?.asString()
            // Exclude common standard class annotations (like Metadata, Target, Retention, Repeatable)
            fqName != "kotlin.Metadata" && fqName != "kotlin.annotation.Target" &&
            fqName != "kotlin.annotation.Retention" && fqName != "kotlin.annotation.Repeatable"
        }.toList()
        return resolveRecursive(directAnnotations, targetType, classDecl, resolver, logger, isObjectLevel = true)
    }

    private fun resolveRecursive(
        annotations: List<KSAnnotation>,
        targetType: KSType,
        errorNode: com.google.devtools.ksp.symbol.KSNode,
        resolver: Resolver,
        logger: KSPLogger,
        isObjectLevel: Boolean
    ): List<ConstraintDescriptor> {
        val descriptors = mutableListOf<ConstraintDescriptor>()
        for (ann in annotations) {
            val fqName = ann.annotationType.resolve().declaration.qualifiedName?.asString() ?: ""
            if (fqName.startsWith("kotlin.annotation.") || fqName.startsWith("java.lang.annotation.")) continue

            val annClass = ann.annotationType.resolve().declaration as? KSClassDeclaration ?: continue

            // Check if it is annotated with @Constraint (custom validator)
            val constraintAnn = annClass.annotations.firstOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.valix.annotations.Constraint"
            }

            val validatorFqName = if (constraintAnn != null) {
                val validatorType = constraintAnn.arguments.firstOrNull { it.name?.asString() == "validator" }?.value as? KSType
                validatorType?.declaration?.qualifiedName?.asString()
            } else {
                null
            }

            // Perform KSP custom validator compile-time checks
            if (validatorFqName != null) {
                val validatorClassDecl = resolver.getClassDeclarationByName(resolver.getKSNameFromString(validatorFqName))
                if (validatorClassDecl == null) {
                    logger.error("Validator class '$validatorFqName' not found", errorNode)
                    continue
                }

                val interfaceInfo = findValidatorInterface(validatorClassDecl)
                if (interfaceInfo == null) {
                    logger.error("Validator class '$validatorFqName' must implement ConstraintValidator or ObjectConstraintValidator", errorNode)
                    continue
                }

                if (interfaceInfo.isObjectValidator && !isObjectLevel) {
                    logger.error("Object-level validator '$validatorFqName' cannot be applied to a property", errorNode)
                    continue
                }
                if (!interfaceInfo.isObjectValidator && isObjectLevel) {
                    logger.error("Property validator '$validatorFqName' cannot be applied to a class", errorNode)
                    continue
                }

                // Check type compatibility
                val expectedNotNullable = interfaceInfo.expectedType.makeNotNullable()
                val targetNotNullable = targetType.makeNotNullable()
                if (!expectedNotNullable.isAssignableFrom(targetNotNullable)) {
                    if (isObjectLevel) {
                        logger.error("Validator '$validatorFqName' expects type '${interfaceInfo.expectedType}' but was applied to class '$targetType'", errorNode)
                    } else {
                        logger.error("Validator '$validatorFqName' expects type '${interfaceInfo.expectedType}' but was applied to property of type '$targetType'", errorNode)
                    }
                    continue
                }
            }

            // Parse common parameters
            val message = ann.arguments.firstOrNull { it.name?.asString() == "message" }?.value as? String ?: ""
            val messageKey = ann.arguments.firstOrNull { it.name?.asString() == "messageKey" }?.value as? String ?: ""

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

            val metaAnnotations = annClass.annotations.filter {
                val name = it.annotationType.resolve().declaration.qualifiedName?.asString() ?: ""
                name != "io.valix.annotations.Constraint" &&
                !name.startsWith("kotlin.annotation.") &&
                !name.startsWith("java.lang.annotation.")
            }.toList()

            val isBuiltIn = fqName.startsWith("io.valix.annotations.")
            val isCustom = validatorFqName != null

            val composed = if (metaAnnotations.isNotEmpty()) {
                resolveRecursive(metaAnnotations, targetType, errorNode, resolver, logger, isObjectLevel)
            } else {
                emptyList()
            }

            if (isBuiltIn || isCustom) {
                descriptors.add(
                    ConstraintDescriptor(
                        annotationFqName = fqName,
                        validatorFqName = validatorFqName,
                        message = message,
                        messageKey = messageKey,
                        groups = groups,
                        targetType = targetType,
                        annotation = ann,
                        isObjectLevel = isObjectLevel
                    )
                )
            }

            if (composed.isNotEmpty()) {
                val mappedComposed = composed.map { comp ->
                    comp.copy(
                        groups = if (comp.groups.isEmpty()) groups else comp.groups,
                        message = if (comp.message.isEmpty()) message else comp.message,
                        messageKey = if (comp.messageKey.isEmpty()) messageKey else comp.messageKey
                    )
                }
                descriptors.addAll(mappedComposed)
            }
        }
        return descriptors
    }
}
