package com.example.jvm

import com.example.jvm.generated.RegisterRequestValidator
import io.valix.flow.validateWith
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("--- Testing Valix Validator ---")

    val validRequest = RegisterRequest(
        email = "test@example.com",
        password = "strongpassword123",
        username = "validUser"
    )
    val result1 = RegisterRequestValidator.validate(validRequest)
    println("Valid request is valid: ${result1.valid}, errors: ${result1.errors}")

    val invalidRequest = RegisterRequest(
        email = null,
        password = "short",
        username = "invalid_user_123"
    )
    val result2 = RegisterRequestValidator.validate(invalidRequest)
    println("Invalid request is valid: ${result2.valid}")
    result2.errors.forEach { println(" - Field: ${it.field}, Code: ${it.code}, Message: ${it.message}") }

    println("\n--- Testing Valix Flow Integration ---")
    val requestFlow = flowOf(validRequest, invalidRequest)
    requestFlow.validateWith(RegisterRequestValidator).collect { result ->
        println("Flow validation emission - is valid: ${result.valid}, errors count: ${result.errors.size}")
    }
}
