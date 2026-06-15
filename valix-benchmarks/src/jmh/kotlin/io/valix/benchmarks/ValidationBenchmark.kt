package io.valix.benchmarks

import io.valix.generated.ValixRegistry
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
open class ValidationBenchmark {

    private lateinit var hibernateValidator: Validator
    private lateinit var validModel: BenchmarkModel
    private lateinit var invalidModel: BenchmarkModel

    @Setup
    fun setup() {
        val factory = Validation.buildDefaultValidatorFactory()
        hibernateValidator = factory.validator

        // Trigger ValixRegistry init
        val _trigger = ValixRegistry

        validModel = BenchmarkModel("test@valix.io", "john_doe")
        invalidModel = BenchmarkModel("invalid-email", "jo")
    }

    @Benchmark
    fun benchmarkValixValid(): Boolean {
        val result = ValixRegistry.validate(validModel)
        return result.valid
    }

    @Benchmark
    fun benchmarkValixInvalid(): Boolean {
        val result = ValixRegistry.validate(invalidModel)
        return result.valid
    }

    @Benchmark
    fun benchmarkHibernateValid(): Boolean {
        val violations = hibernateValidator.validate(validModel)
        return violations.isEmpty()
    }

    @Benchmark
    fun benchmarkHibernateInvalid(): Boolean {
        val violations = hibernateValidator.validate(invalidModel)
        return violations.isEmpty()
    }
}
