package io.valix.runtime

import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import kotlin.reflect.KClass

/**
 * Diagnostics controller for enabling validation metrics and profiling.
 */
object ValixDiagnostics {
    /** Toggle metrics collection on/off. */
    var enabled: Boolean = false

    /** Recorded execution metrics. */
    val metrics = ValidationMetrics()

    /** Measures duration of the validation block. */
    inline fun <T> measure(validator: ValixValidator<*>, block: () -> T): T {
        if (!enabled) return block()
        val start = System.nanoTime()
        return try {
            block()
        } finally {
            val duration = System.nanoTime() - start
            metrics.record(validator::class, duration)
        }
    }
}

/** Total validation counts and timing metrics. */
class ValidationMetrics {
    var totalValidations: Long = 0L
    var totalDurationNs: Long = 0L
    private val statsMap = mutableMapOf<KClass<*>, ConstraintExecutionStats>()

    fun record(validatorClass: KClass<*>, durationNs: Long) {
        totalValidations++
        totalDurationNs += durationNs
        val stats = statsMap.getOrPut(validatorClass) { ConstraintExecutionStats(validatorClass.simpleName ?: "Unknown") }
        stats.record(durationNs)
    }

    fun getStats(): Map<KClass<*>, ConstraintExecutionStats> = statsMap.toMap()

    fun reset() {
        totalValidations = 0L
        totalDurationNs = 0L
        statsMap.clear()
    }
}

/** Execution metrics per validator class. */
class ConstraintExecutionStats(val name: String) {
    var count: Long = 0L
    var totalDurationNs: Long = 0L
    var maxDurationNs: Long = 0L

    fun record(durationNs: Long) {
        count++
        totalDurationNs += durationNs
        if (durationNs > maxDurationNs) {
            maxDurationNs = durationNs
        }
    }
}

/** Helper extension executing validation with diagnostics timing. */
fun <T> ValixValidator<T>.validateWithMetrics(value: T, vararg groups: KClass<*>): ValidationResult {
    return ValixDiagnostics.measure(this) {
        validate(value, *groups)
    }
}
