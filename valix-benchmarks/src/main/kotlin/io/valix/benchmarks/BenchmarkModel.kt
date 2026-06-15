package io.valix.benchmarks

import io.valix.annotations.NotNull as ValixNotNull
import io.valix.annotations.NotBlank as ValixNotBlank
import io.valix.annotations.Email as ValixEmail
import io.valix.annotations.MinLength as ValixMinLength
import io.valix.annotations.MaxLength as ValixMaxLength
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class BenchmarkModel(
    @ValixNotNull
    @ValixNotBlank
    @ValixEmail
    @get:NotNull
    @get:NotBlank
    @get:Email
    val email: String,

    @ValixNotNull
    @ValixMinLength(5)
    @ValixMaxLength(10)
    @get:NotNull
    @get:Size(min = 5, max = 10)
    val username: String
)
