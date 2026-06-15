package io.valix.flow

import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data class FlowUser(val name: String)

object FlowUserValidator : ValixValidator<FlowUser> {
    override fun validate(value: FlowUser, vararg groups: KClass<*>): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        if (value.name.isBlank()) {
            errors.add(ValidationError("name", "NOT_BLANK", "Name must not be blank", value.name))
        }
        return ValidationResult(errors.isEmpty(), errors)
    }
}

class FlowTest {

    @Test
    fun testFlowValidation() = runBlocking {
        val userStream = flowOf(
            FlowUser(""),
            FlowUser("Alice"),
            FlowUser("  ")
        )

        val results = userStream.validateWith(FlowUserValidator).toList()

        assertEquals(3, results.size)
        assertFalse(results[0].valid)
        assertTrue(results[1].valid)
        assertFalse(results[2].valid)
    }
}
