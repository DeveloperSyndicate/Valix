package com.example.jvm

import io.valix.annotations.Email
import io.valix.annotations.MinLength
import io.valix.annotations.NotBlank
import io.valix.annotations.NotNull
import io.valix.annotations.MaxLength
import io.valix.annotations.Pattern

data class RegisterRequest(
    @NotNull
    @NotBlank
    @Email
    val email: String?,

    @MinLength(8)
    @MaxLength(20)
    val password: String,

    @Pattern("^[a-zA-Z]+$")
    val username: String
)
