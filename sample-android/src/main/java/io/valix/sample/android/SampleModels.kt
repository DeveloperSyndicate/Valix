package io.valix.sample.android

import io.valix.annotations.*

data class LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    val email: String,

    @NotBlank(message = "Password is required")
    @MinLength(8, message = "Password must be at least 8 characters")
    val password: String
)

@FieldsMatch(
    first = "password",
    second = "confirmPassword",
    message = "Passwords must match"
)
data class RegisterRequest(
    @NotBlank(message = "Username is required")
    @MinLength(4, message = "Username must be at least 4 characters")
    val username: String,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    val email: String,

    @NotBlank(message = "Password is required")
    @MinLength(8, message = "Password must be at least 8 characters")
    val password: String,

    val confirmPassword: String
)

data class MultiStepRequest(
    // Step 1: Personal Info
    @NotBlank(message = "First name is required")
    val firstName: String,

    @NotBlank(message = "Last name is required")
    val lastName: String,

    // Step 2: Contact Info
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    val email: String,

    @NotBlank(message = "Phone number is required")
    val phone: String
)
