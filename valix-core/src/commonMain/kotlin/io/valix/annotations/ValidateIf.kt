package io.valix.annotations

/**
 * Specifies a conditional check determining whether constraints on the annotated property should be evaluated.
 *
 * Constraints on the annotated property are evaluated ONLY if property [field] on the target object equals [equals].
 *
 * @property field The sibling property name to inspect.
 * @property equals The string representation of the value required to trigger validation.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class ValidateIf(
    val field: String,
    val equals: String = ""
)
