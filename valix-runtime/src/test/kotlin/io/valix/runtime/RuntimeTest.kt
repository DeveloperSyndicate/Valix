package io.valix.runtime

import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

data class MockUser(val email: String, val age: Int)

object MockUserValidator : ValixValidator<MockUser> {
    override fun validate(value: MockUser, vararg groups: KClass<*>): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        if (value.email.isBlank()) {
            errors.add(ValidationError("email", "NOT_BLANK", "Email must not be blank", value.email))
        }
        if (value.age < 18) {
            errors.add(ValidationError("age", "MIN_VALUE", "Age must be at least 18", value.age))
        }
        return ValidationResult(errors.isEmpty(), errors)
    }
}

class RuntimeTest {

    @Test
    fun testErrorMappingExtensions() {
        val errors = listOf(
            ValidationError("email", "NOT_BLANK", "Email must not be blank", ""),
            ValidationError("age", "MIN_VALUE", "Age must be at least 18", 15)
        )
        val result = ValidationResult(false, errors)

        val byField = result.errorsByField()
        assertEquals(2, byField.size)
        assertEquals("NOT_BLANK", byField["email"]?.first()?.code)

        val fieldErrors = result.fieldErrors()
        assertEquals(2, fieldErrors.size)
        assertEquals("MIN_VALUE", fieldErrors["age"]?.code)

        assertEquals("email", result.firstError()?.field)
        assertEquals(listOf("Email must not be blank", "Age must be at least 18"), result.allMessages())
    }

    @Test
    fun testPartialValidationFiltering() {
        val errors = listOf(
            ValidationError("email", "NOT_BLANK", "Email must not be blank", ""),
            ValidationError("profile.username", "MIN_LENGTH", "Too short", "ab"),
            ValidationError("nicknames[0].name", "NOT_BLANK", "Name empty", "")
        )
        val result = ValidationResult(false, errors)

        val filteredEmail = result.filterFields("email")
        assertEquals(1, filteredEmail.errors.size)
        assertEquals("email", filteredEmail.errors.first().field)

        val filteredProfile = result.filterFields("profile")
        assertEquals(1, filteredProfile.errors.size)
        assertEquals("profile.username", filteredProfile.errors.first().field)

        val filteredNicknames = result.filterFields("nicknames")
        assertEquals(1, filteredNicknames.errors.size)
        assertEquals("nicknames[0].name", filteredNicknames.errors.first().field)
    }

    @Test
    fun testFormStateOnChange() {
        val initialUser = MockUser("", 20)
        val formState = FormState(initialUser, MockUserValidator, ValidationMode.OnChange)

        // Initial state should be clean/valid until validated or modified
        assertTrue(formState.isValid)
        assertFalse(formState.isDirty)

        // Update email to blank -> triggers onChange validation -> invalid
        formState.onFieldChange("email", MockUser(" ", 20))
        assertFalse(formState.isValid)
        assertTrue(formState.isDirty)
        assertTrue(formState.dirtyFields.contains("email"))
        assertNotNull(formState.errorFor("email"))

        // Update email to valid -> triggers onChange validation -> valid
        formState.onFieldChange("email", MockUser("john@example.com", 20))
        assertTrue(formState.isValid)
    }

    @Test
    fun testFormStateOnBlur() {
        val initialUser = MockUser("", 20)
        val formState = FormState(initialUser, MockUserValidator, ValidationMode.OnBlur)

        // Update email -> dirty but no validation run yet since mode is OnBlur
        formState.onFieldChange("email", MockUser(" ", 20))
        assertTrue(formState.isValid)

        // Trigger focus blur on email -> runs validation -> invalid
        formState.onFieldBlur("email")
        assertFalse(formState.isValid)
        assertTrue(formState.isTouched)
        assertTrue(formState.touchedFields.contains("email"))
    }

    @Test
    fun testFormStateDiagnostics() {
        ValixDiagnostics.enabled = true
        ValixDiagnostics.metrics.reset()

        val user = MockUser("john@example.com", 20)
        val result = MockUserValidator.validateWithMetrics(user)
        assertTrue(result.valid)

        val stats = ValixDiagnostics.metrics.getStats()
        assertEquals(1, stats.size)
        val entry = stats.values.first()
        assertEquals("MockUserValidator", entry.name)
        assertEquals(1L, entry.count)
        assertTrue(entry.totalDurationNs > 0)
    }
}
