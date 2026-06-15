package com.example.jvm

import com.example.jvm.generated.RegisterRequestValixValidator

fun main() {
    println("--- Testing Valix Validator ---")

    // Test Case 1: Valid request
    val validRequest = RegisterRequest(
        email = "test@example.com",
        password = "strongpassword123",
        username = "validUser"
    )
    val result1 = RegisterRequestValixValidator.validate(validRequest)
    println("Valid request is valid: ${result1.valid}, errors: ${result1.errors}")

    // Test Case 2: Invalid request (Null email, password too short, username invalid pattern)
    val invalidRequest = RegisterRequest(
        email = null,
        password = "short",
        username = "invalid_user_123"
    )
    val result2 = RegisterRequestValixValidator.validate(invalidRequest)
    println("Invalid request is valid: ${result2.valid}")
    result2.errors.forEach { println(" - Field: ${it.field}, Code: ${it.code}, Message: ${it.message}") }
}
