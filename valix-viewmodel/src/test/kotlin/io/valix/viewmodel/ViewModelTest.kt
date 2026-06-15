package io.valix.viewmodel

import io.valix.core.ValidationError
import io.valix.core.ValidationResult
import io.valix.core.ValixValidator
import io.valix.runtime.ValidationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KClass

data class VMUser(val username: String)

object VMUserValidator : ValixValidator<VMUser> {
    override fun validate(value: VMUser, vararg groups: KClass<*>): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        if (value.username.length < 5) {
            errors.add(ValidationError("username", "MIN_LENGTH", "Too short", value.username))
        }
        return ValidationResult(errors.isEmpty(), errors)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testViewModelValidationAndFlows() = runBlocking {
        val viewModel = ValixFormViewModel(VMUser(""), VMUserValidator, ValidationMode.OnChange)

        // Initial check
        assertEquals("", viewModel.value.value.username)
        assertTrue(viewModel.errors.value.isEmpty())

        // Change value to invalid
        viewModel.onFieldChange("username", VMUser("abc"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("abc", viewModel.value.value.username)
        assertFalse(viewModel.errors.value.isEmpty())
        assertEquals("MIN_LENGTH", viewModel.errors.value.first().code)

        // Reset form
        viewModel.reset()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.value.value.username)
        assertTrue(viewModel.errors.value.isEmpty())
        assertFalse(viewModel.isSubmitted.value)
    }

    @Test
    fun testViewModelSubmission() = runBlocking {
        val viewModel = ValixFormViewModel(VMUser("validUser"), VMUserValidator, ValidationMode.OnChange)
        var submittedUser: VMUser? = null

        viewModel.submit { user ->
            submittedUser = user
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(submittedUser)
        assertEquals("validUser", submittedUser?.username)
        assertTrue(viewModel.isSubmitted.value)
    }

    private fun assertNotNull(value: Any?) {
        assertTrue(value != null)
    }
}
