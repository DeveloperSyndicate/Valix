package io.valix.runtime

import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Fluent programmatic validation builder for dynamic or unannotated data classes.
 */
class ValixDslBuilder<T> {
    private val fieldValidators = mutableListOf<(T, MutableList<ValidationError>, Boolean) -> Boolean>()

    /**
     * Registers validation constraints on property extracted by [property].
     */
    fun <P> field(
        property: KProperty1<T, P>,
        builder: PropertyValidationBuilder<P>.() -> Unit
    ) {
        field(property.name, { property.get(it) }, builder)
    }

    /**
     * Registers validation constraints on property extracted by [propertyGetter].
     */
    fun <P> field(
        name: String,
        propertyGetter: (T) -> P,
        builder: PropertyValidationBuilder<P>.() -> Unit
    ) {
        val propBuilder = PropertyValidationBuilder<P>(name)
        propBuilder.builder()
        val rules = propBuilder.buildRules()

        fieldValidators.add { instance, errors, failFast ->
            val value = propertyGetter(instance)
            for (rule in rules) {
                if (!rule.predicate(value)) {
                    errors.add(
                        ValidationError(
                            field = name,
                            code = rule.code,
                            message = rule.message,
                            messageKey = "valix.${rule.code.lowercase()}",
                            rejectedValue = value,
                            constraint = "io.valix.dsl.${rule.code}",
                            path = name
                        )
                    )
                    if (failFast) return@add true
                }
            }
            false
        }
    }

    internal fun buildValidator(): ValixValidator<T> {
        return object : ValixValidator<T> {
            override fun validate(value: T, vararg groups: KClass<*>, failFast: Boolean): ValidationResult {
                val errors = mutableListOf<ValidationError>()
                for (fv in fieldValidators) {
                    val stop = fv(value, errors, failFast)
                    if (stop && failFast) break
                }
                return ValidationResult(errors.isEmpty(), errors)
            }
        }
    }
}

/** Rule evaluation definition for DSL field properties. */
data class ValidationRule<P>(
    val code: String,
    val message: String,
    val predicate: (P) -> Boolean
)

/** Builder accumulating property rules. */
class PropertyValidationBuilder<P>(private val fieldName: String) {
    private val rules = mutableListOf<ValidationRule<P>>()

    fun notNull(message: String = "$fieldName must not be null") {
        rules.add(ValidationRule("NOT_NULL", message) { it != null })
    }

    fun notBlank(message: String = "$fieldName must not be blank") {
        rules.add(ValidationRule("NOT_BLANK", message) { (it as? String)?.isNotBlank() == true })
    }

    fun min(minValue: Number, message: String = "$fieldName must be at least $minValue") {
        rules.add(ValidationRule("MIN", message) { (it as? Number)?.toDouble() ?: 0.0 >= minValue.toDouble() })
    }

    fun max(maxValue: Number, message: String = "$fieldName must be at most $maxValue") {
        rules.add(ValidationRule("MAX", message) { (it as? Number)?.toDouble() ?: 0.0 <= maxValue.toDouble() })
    }

    fun minLength(minLen: Int, message: String = "$fieldName length must be at least $minLen") {
        rules.add(ValidationRule("MIN_LENGTH", message) { (it as? String)?.length ?: 0 >= minLen })
    }

    fun email(message: String = "$fieldName must be a valid email") {
        rules.add(ValidationRule("EMAIL", message) {
            val str = it as? String ?: return@ValidationRule false
            str.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
        })
    }

    fun rule(code: String, message: String, predicate: (P) -> Boolean) {
        rules.add(ValidationRule(code, message, predicate))
    }

    internal fun buildRules(): List<ValidationRule<P>> = rules
}

/**
 * Construct a programmatic [ValixValidator] for type [T] using Kotlin DSL syntax.
 */
fun <T> valixDsl(init: ValixDslBuilder<T>.() -> Unit): ValixValidator<T> {
    val builder = ValixDslBuilder<T>()
    builder.init()
    return builder.buildValidator()
}
