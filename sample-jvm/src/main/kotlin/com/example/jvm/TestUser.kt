package com.example.jvm

import io.valix.annotations.*

data class TestUser(
    @NotNull
    val username: String?,

    @NotBlank
    val displayName: String?,

    @Email
    val email: String?,

    @MinLength(5)
    val shortCode: String?,

    @MaxLength(10)
    val longCode: String?,

    @Pattern("^[0-9]+$")
    val numericCode: String?
)
