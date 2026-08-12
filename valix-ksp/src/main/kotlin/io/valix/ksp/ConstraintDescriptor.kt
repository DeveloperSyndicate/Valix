package io.valix.ksp

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType

/**
 * Symbol descriptor holding metadata for a resolved constraint annotation processed by KSP.
 *
 * @property annotationFqName Fully qualified annotation class name.
 * @property validatorFqName Fully qualified custom validator class name (if applicable).
 * @property message Error message template string.
 * @property messageKey Translation key string.
 * @property groups Active validation group qualified names.
 * @property targetType Target Kotlin property type symbol.
 * @property annotation Underlying KSP annotation symbol.
 * @property isObjectLevel `true` if attached at class/object level; `false` if property level.
 */
data class ConstraintDescriptor(
    val annotationFqName: String,
    val validatorFqName: String?,
    val message: String,
    val messageKey: String,
    val groups: List<String>,
    val targetType: KSType,
    val annotation: KSAnnotation,
    val isObjectLevel: Boolean = false
)
