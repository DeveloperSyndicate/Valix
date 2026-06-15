package io.valix.compose

import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import io.valix.runtime.ValidationMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass

data class ComposeUser(val name: String)

object ComposeUserValidator : ValixValidator<ComposeUser> {
    override fun validate(value: ComposeUser, vararg groups: KClass<*>): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        if (value.name.isBlank()) {
            errors.add(ValidationError("name", "NOT_BLANK", "Empty name", value.name))
        }
        return ValidationResult(errors.isEmpty(), errors)
    }
}

class ComposeFormStateTest {

    @Test
    fun testComposeFormStateUpdates() = runBlocking {
        val form = ComposeFormState(ComposeUser(""), ComposeUserValidator, ValidationMode.OnChange)

        // Initial checks
        assertEquals("", form.value.name)
        assertTrue(form.isValid)
        assertFalse(form.isDirty)

        // Modify value -> triggers onChange
        form.onFieldChange("name", ComposeUser("Alice"))
        assertEquals("Alice", form.value.name)
        assertTrue(form.isValid)
        assertTrue(form.isDirty)
        assertTrue(form.dirtyFields.contains("name"))

        // Modify to invalid -> invalid
        form.onFieldChange("name", ComposeUser(""))
        assertFalse(form.isValid)
        assertEquals("name", form.errorFor("name")?.field)
    }
}
