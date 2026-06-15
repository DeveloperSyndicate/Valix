package com.example.jvm

import com.example.jvm.generated.TestUserValixValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationTest {

    @Test
    fun testValidUser() {
        val user = TestUser(
            username = "john_doe",
            displayName = "John",
            email = "john@example.com",
            shortCode = "12345",
            longCode = "12345",
            numericCode = "998877"
        )
        val result = TestUserValixValidator.validate(user)
        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun testNullValues() {
        // username is @NotNull, so it should fail.
        // Others are nullable and not @NotNull, so nulls should pass validation.
        val user = TestUser(
            username = null,
            displayName = null,
            email = null,
            shortCode = null,
            longCode = null,
            numericCode = null
        )
        val result = TestUserValixValidator.validate(user)
        assertFalse(result.valid)
        assertEquals(1, result.errors.size)
        assertEquals("username", result.errors[0].field)
        assertEquals("NOT_NULL", result.errors[0].code)
        assertEquals("must not be null", result.errors[0].message)
    }

    @Test
    fun testNotBlankValidation() {
        val user = TestUser(
            username = "user",
            displayName = "   ", // blank spaces
            email = "email@test.com",
            shortCode = "12345",
            longCode = "12345",
            numericCode = "123"
        )
        val result = TestUserValixValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "displayName" }
        assertEquals("NOT_BLANK", error?.code)
        assertEquals("must not be blank", error?.message)
    }

    @Test
    fun testEmailValidation() {
        val user = TestUser(
            username = "user",
            displayName = "display",
            email = "invalid-email-format",
            shortCode = "12345",
            longCode = "12345",
            numericCode = "123"
        )
        val result = TestUserValixValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "email" }
        assertEquals("EMAIL_INVALID", error?.code)
        assertEquals("invalid email", error?.message)
    }

    @Test
    fun testMinLengthValidation() {
        val user = TestUser(
            username = "user",
            displayName = "display",
            email = "test@email.com",
            shortCode = "123", // too short (min 5)
            longCode = "12345",
            numericCode = "123"
        )
        val result = TestUserValixValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "shortCode" }
        assertEquals("MIN_LENGTH", error?.code)
        assertEquals("minimum length is 5", error?.message)
    }

    @Test
    fun testMaxLengthValidation() {
        val user = TestUser(
            username = "user",
            displayName = "display",
            email = "test@email.com",
            shortCode = "12345",
            longCode = "12345678901", // too long (max 10)
            numericCode = "123"
        )
        val result = TestUserValixValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "longCode" }
        assertEquals("MAX_LENGTH", error?.code)
        assertEquals("maximum length is 10", error?.message)
    }

    @Test
    fun testPatternValidation() {
        val user = TestUser(
            username = "user",
            displayName = "display",
            email = "test@email.com",
            shortCode = "12345",
            longCode = "12345",
            numericCode = "123a45" // invalid pattern (contains letter)
        )
        val result = TestUserValixValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "numericCode" }
        assertEquals("PATTERN_MISMATCH", error?.code)
        assertEquals("pattern mismatch", error?.message)
    }
}
