package io.valix.ksp

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType

data class ConstraintDescriptor(
    val annotationFqName: String,
    val validatorFqName: String?, // FQN of the custom validator class (if any)
    val message: String,
    val groups: List<String>,
    val targetType: KSType,
    val annotation: KSAnnotation,
    val isObjectLevel: Boolean = false
)
