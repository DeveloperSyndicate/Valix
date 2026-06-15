package com.example.jvm

import com.example.jvm.generated.*
import io.valix.generated.ValixRegistry
import io.valix.localization.PropertiesMessageResolver
import io.valix.localization.resolveMessages
import io.valix.metadata.ValixConfig
import io.valix.metadata.MetadataRegistry
import io.valix.schema.JsonSchemaGenerator
import io.valix.schema.OpenApiSchemaGenerator
import io.valix.serialization.toJson
import io.valix.serialization.mergeValixMetadata
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

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
        val result = TestUserValidator.validate(user)
        assertTrue(result.valid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun testNullValues() {
        val user = TestUser(
            username = null,
            displayName = null,
            email = null,
            shortCode = null,
            longCode = null,
            numericCode = null
        )
        val result = TestUserValidator.validate(user)
        assertFalse(result.valid)
        assertEquals(1, result.errors.size)
        assertEquals("username", result.errors[0].field)
        assertEquals("NOT_NULL", result.errors[0].code)
        assertEquals("must not be null", result.errors[0].message)
        assertEquals(null, result.errors[0].rejectedValue)
    }

    @Test
    fun testNotBlankValidation() {
        val user = TestUser(
            username = "user",
            displayName = "   ",
            email = "email@test.com",
            shortCode = "12345",
            longCode = "12345",
            numericCode = "123"
        )
        val result = TestUserValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "displayName" }
        assertEquals("NOT_BLANK", error?.code)
        assertEquals("must not be blank", error?.message)
        assertEquals("   ", error?.rejectedValue)
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
        val result = TestUserValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "email" }
        assertEquals("EMAIL_INVALID", error?.code)
        assertEquals("invalid email", error?.message)
        assertEquals("invalid-email-format", error?.rejectedValue)
    }

    @Test
    fun testMinLengthValidation() {
        val user = TestUser(
            username = "user",
            displayName = "display",
            email = "test@email.com",
            shortCode = "123",
            longCode = "12345",
            numericCode = "123"
        )
        val result = TestUserValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "shortCode" }
        assertEquals("MIN_LENGTH", error?.code)
        assertEquals("minimum length is 5", error?.message)
        assertEquals("123", error?.rejectedValue)
    }

    @Test
    fun testMaxLengthValidation() {
        val user = TestUser(
            username = "user",
            displayName = "display",
            email = "test@email.com",
            shortCode = "12345",
            longCode = "12345678901",
            numericCode = "123"
        )
        val result = TestUserValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "longCode" }
        assertEquals("MAX_LENGTH", error?.code)
        assertEquals("maximum length is 10", error?.message)
        assertEquals("12345678901", error?.rejectedValue)
    }

    @Test
    fun testPatternValidation() {
        val user = TestUser(
            username = "user",
            displayName = "display",
            email = "test@email.com",
            shortCode = "12345",
            longCode = "12345",
            numericCode = "123a45"
        )
        val result = TestUserValidator.validate(user)
        assertFalse(result.valid)
        val error = result.errors.find { it.field == "numericCode" }
        assertEquals("PATTERN_MISMATCH", error?.code)
        assertEquals("pattern mismatch", error?.message)
        assertEquals("123a45", error?.rejectedValue)
    }

    @Test
    fun testNestedValidationAndPaths() {
        val nestedUser = User(
            email = "john@example.com",
            address = Address(
                city = "   ", // Blank -> Should trigger NOT_BLANK
                country = Country(
                    name = " " // Blank -> Should trigger NOT_BLANK
                )
            ),
            nicknames = listOf(
                Nickname("Joe"), // Valid
                Nickname("Al")  // Invalid -> Length 2 < 3
            )
        )

        val result = UserValidator.validate(nestedUser)
        assertFalse(result.valid)
        
        // Check errors size and specific fields
        assertEquals(3, result.errors.size)

        val cityError = result.errors.find { it.field == "address.city" }
        assertEquals("NOT_BLANK", cityError?.code)
        assertEquals("must not be blank", cityError?.message)
        assertEquals("   ", cityError?.rejectedValue)

        val countryError = result.errors.find { it.field == "address.country.name" }
        assertEquals("NOT_BLANK", countryError?.code)
        assertEquals("Country name must not be blank", countryError?.message) // custom msg
        assertEquals(" ", countryError?.rejectedValue)

        val nicknameError = result.errors.find { it.field == "nicknames[1].name" }
        assertEquals("MIN_LENGTH", nicknameError?.code)
        assertEquals("Nickname too short", nicknameError?.message) // custom msg
        assertEquals("Al", nicknameError?.rejectedValue)
    }

    @Test
    fun testCollectionSetValidation() {
        val project = Project(
            members = setOf(
                Member("Alice"),
                Member(" ") // Blank -> Should trigger NOT_BLANK
            )
        )

        val result = ProjectValidator.validate(project)
        assertFalse(result.valid)
        assertEquals(1, result.errors.size)
        assertEquals("members[1].name", result.errors[0].field)
        assertEquals("NOT_BLANK", result.errors[0].code)
        assertEquals(" ", result.errors[0].rejectedValue)
    }

    @Test
    fun testValidationGroups() {
        val nestedUser = User(
            email = "invalid-email-format",
            address = Address(
                city = "Paris",
                country = Country("France")
            ),
            nicknames = emptyList()
        )

        // Case 1: Validate with Create group.
        // Email validation matches Create group -> should trigger error
        val createResult = UserValidator.validate(nestedUser, Create::class)
        assertFalse(createResult.valid)
        assertEquals(1, createResult.errors.size)
        assertEquals("email", createResult.errors[0].field)
        assertEquals("Custom email invalid", createResult.errors[0].message)

        // Case 2: Validate with Update group.
        // Email validation does NOT match Update group -> should be valid
        val updateResult = UserValidator.validate(nestedUser, Update::class)
        assertTrue(updateResult.valid)

        // Case 3: Validate with no groups.
        // Should execute all validations -> triggers error
        val defaultResult = UserValidator.validate(nestedUser)
        assertFalse(defaultResult.valid)
        assertEquals(1, defaultResult.errors.size)
    }

    @Test
    fun testRegistryValidation() {
        val user = TestUser(
            username = null,
            displayName = "Display",
            email = "email@test.com",
            shortCode = "12345",
            longCode = "12345",
            numericCode = "123"
        )
        
        // Validate using ValixRegistry
        val result = ValixRegistry.validate(user)
        assertFalse(result.valid)
        assertEquals(1, result.errors.size)
        assertEquals("username", result.errors[0].field)
    }

    @Test
    fun testBackwardCompatibility() {
        val request = RegisterRequest(
            email = "invalid-email",
            password = "short",
            username = "validUser"
        )
        // Call the Valix suffix validator (which delegates internally to RegisterRequestValidator)
        val result = RegisterRequestValixValidator.validate(request)
        assertFalse(result.valid)
        
        val emailError = result.errors.find { it.field == "email" }
        assertEquals("EMAIL_INVALID", emailError?.code)

        val passwordError = result.errors.find { it.field == "password" }
        assertEquals("MIN_LENGTH", passwordError?.code)
    }

    @Test
    fun testPhase3StringValidators() {
        val validModel = Phase3TestModel(
            website = "https://valix.io",
            phone = "+1-555-0199",
            alphabetic = "AbcDef",
            alphanumeric = "Abc123Def",
            lowercase = "lowercaseonly",
            uppercase = "UPPERCASEONLY",
            tag = "hello valix user",
            code = "PREFIX_xyz",
            suffix = "sample_suffix",
            minVal = null, maxVal = null, positiveVal = null, positiveOrZeroVal = null,
            negativeVal = null, negativeOrZeroVal = null, rangeVal = null,
            items = null, names = null, stringRole = null, enumRole = null,
            pastDate = null, pastOrPresentDateTime = null, futureInstant = null, futureOrPresentOffset = null
        )
        val validResult = Phase3TestModelValidator.validate(validModel)
        assertTrue(validResult.valid)

        val invalidModel = Phase3TestModel(
            website = "not_a_website",
            phone = "123", 
            alphabetic = "Alpha1", 
            alphanumeric = "Alpha1#", 
            lowercase = "LowerClass",
            uppercase = "UpperClass",
            tag = "hello admin",
            code = "NOTPREFIX_",
            suffix = "no_suff",
            minVal = null, maxVal = null, positiveVal = null, positiveOrZeroVal = null,
            negativeVal = null, negativeOrZeroVal = null, rangeVal = null,
            items = null, names = null, stringRole = null, enumRole = null,
            pastDate = null, pastOrPresentDateTime = null, futureInstant = null, futureOrPresentOffset = null
        )
        val invalidResult = Phase3TestModelValidator.validate(invalidModel)
        assertFalse(invalidResult.valid)
        assertEquals(9, invalidResult.errors.size)
        
        assertEquals("URL_INVALID", invalidResult.errors.find { it.field == "website" }?.code)
        assertEquals("PHONE_INVALID", invalidResult.errors.find { it.field == "phone" }?.code)
        assertEquals("ALPHA_INVALID", invalidResult.errors.find { it.field == "alphabetic" }?.code)
        assertEquals("ALPHANUMERIC_INVALID", invalidResult.errors.find { it.field == "alphanumeric" }?.code)
        assertEquals("LOWERCASE_INVALID", invalidResult.errors.find { it.field == "lowercase" }?.code)
        assertEquals("UPPERCASE_INVALID", invalidResult.errors.find { it.field == "uppercase" }?.code)
        assertEquals("CONTAINS_INVALID", invalidResult.errors.find { it.field == "tag" }?.code)
        assertEquals("STARTS_WITH_INVALID", invalidResult.errors.find { it.field == "code" }?.code)
        assertEquals("ENDS_WITH_INVALID", invalidResult.errors.find { it.field == "suffix" }?.code)
    }

    @Test
    fun testPhase3NumericValidators() {
        val validModel = Phase3TestModel(
            website = null, phone = null, alphabetic = null, alphanumeric = null,
            lowercase = null, uppercase = null, tag = null, code = null, suffix = null,
            minVal = 10,
            maxVal = 100L,
            positiveVal = 0.1,
            positiveOrZeroVal = 0.0f,
            negativeVal = -1,
            negativeOrZeroVal = 0L,
            rangeVal = 25.toShort(),
            items = null, names = null, stringRole = null, enumRole = null,
            pastDate = null, pastOrPresentDateTime = null, futureInstant = null, futureOrPresentOffset = null
        )
        val validResult = Phase3TestModelValidator.validate(validModel)
        assertTrue(validResult.valid)

        val invalidModel = Phase3TestModel(
            website = null, phone = null, alphabetic = null, alphanumeric = null,
            lowercase = null, uppercase = null, tag = null, code = null, suffix = null,
            minVal = 9,
            maxVal = 101L,
            positiveVal = 0.0, 
            positiveOrZeroVal = -0.1f,
            negativeVal = 0, 
            negativeOrZeroVal = 1L,
            rangeVal = 4.toShort(), 
            items = null, names = null, stringRole = null, enumRole = null,
            pastDate = null, pastOrPresentDateTime = null, futureInstant = null, futureOrPresentOffset = null
        )
        val invalidResult = Phase3TestModelValidator.validate(invalidModel)
        assertFalse(invalidResult.valid)
        assertEquals(7, invalidResult.errors.size)
        
        assertEquals("MIN_VALUE", invalidResult.errors.find { it.field == "minVal" }?.code)
        assertEquals("MAX_VALUE", invalidResult.errors.find { it.field == "maxVal" }?.code)
        assertEquals("POSITIVE_REQUIRED", invalidResult.errors.find { it.field == "positiveVal" }?.code)
        assertEquals("POSITIVE_REQUIRED", invalidResult.errors.find { it.field == "positiveOrZeroVal" }?.code)
        assertEquals("NEGATIVE_REQUIRED", invalidResult.errors.find { it.field == "negativeVal" }?.code)
        assertEquals("NEGATIVE_REQUIRED", invalidResult.errors.find { it.field == "negativeOrZeroVal" }?.code)
        assertEquals("RANGE_INVALID", invalidResult.errors.find { it.field == "rangeVal" }?.code)
    }

    @Test
    fun testPhase3CollectionValidators() {
        val validModel = Phase3TestModel(
            website = null, phone = null, alphabetic = null, alphanumeric = null,
            lowercase = null, uppercase = null, tag = null, code = null, suffix = null,
            minVal = null, maxVal = null, positiveVal = null, positiveOrZeroVal = null,
            negativeVal = null, negativeOrZeroVal = null, rangeVal = null,
            items = listOf("A"),
            names = setOf("A", "B", "C"),
            stringRole = null, enumRole = null,
            pastDate = null, pastOrPresentDateTime = null, futureInstant = null, futureOrPresentOffset = null
        )
        val validResult = Phase3TestModelValidator.validate(validModel)
        assertTrue(validResult.valid)

        val invalidModel = Phase3TestModel(
            website = null, phone = null, alphabetic = null, alphanumeric = null,
            lowercase = null, uppercase = null, tag = null, code = null, suffix = null,
            minVal = null, maxVal = null, positiveVal = null, positiveOrZeroVal = null,
            negativeVal = null, negativeOrZeroVal = null, rangeVal = null,
            items = emptyList(), 
            names = setOf("A"), 
            stringRole = null, enumRole = null,
            pastDate = null, pastOrPresentDateTime = null, futureInstant = null, futureOrPresentOffset = null
        )
        val invalidResult = Phase3TestModelValidator.validate(invalidModel)
        assertFalse(invalidResult.valid)
        assertEquals(2, invalidResult.errors.size)
        
        assertEquals("NOT_EMPTY", invalidResult.errors.find { it.field == "items" }?.code)
        assertEquals("SIZE_INVALID", invalidResult.errors.find { it.field == "names" }?.code)
    }

    @Test
    fun testPhase3EnumValidators() {
        val validModel = Phase3TestModel(
            website = null, phone = null, alphabetic = null, alphanumeric = null,
            lowercase = null, uppercase = null, tag = null, code = null, suffix = null,
            minVal = null, maxVal = null, positiveVal = null, positiveOrZeroVal = null,
            negativeVal = null, negativeOrZeroVal = null, rangeVal = null,
            items = null, names = null,
            stringRole = "ADMIN",
            enumRole = UserRole.USER,
            pastDate = null, pastOrPresentDateTime = null, futureInstant = null, futureOrPresentOffset = null
        )
        val validResult = Phase3TestModelValidator.validate(validModel)
        assertTrue(validResult.valid)

        val invalidModel = Phase3TestModel(
            website = null, phone = null, alphabetic = null, alphanumeric = null,
            lowercase = null, uppercase = null, tag = null, code = null, suffix = null,
            minVal = null, maxVal = null, positiveVal = null, positiveOrZeroVal = null,
            negativeVal = null, negativeOrZeroVal = null, rangeVal = null,
            items = null, names = null,
            stringRole = "GUEST", 
            enumRole = UserRole.GUEST, 
            pastDate = null, pastOrPresentDateTime = null, futureInstant = null, futureOrPresentOffset = null
        )
        val invalidResult = Phase3TestModelValidator.validate(invalidModel)
        assertFalse(invalidResult.valid)
        assertEquals(2, invalidResult.errors.size)
        
        assertEquals("INVALID_ENUM_VALUE", invalidResult.errors.find { it.field == "stringRole" }?.code)
        assertEquals("INVALID_ENUM_VALUE", invalidResult.errors.find { it.field == "enumRole" }?.code)
    }

    @Test
    fun testPhase3DateValidators() {
        val now = java.time.LocalDate.now()
        val validModel = Phase3TestModel(
            website = null, phone = null, alphabetic = null, alphanumeric = null,
            lowercase = null, uppercase = null, tag = null, code = null, suffix = null,
            minVal = null, maxVal = null, positiveVal = null, positiveOrZeroVal = null,
            negativeVal = null, negativeOrZeroVal = null, rangeVal = null,
            items = null, names = null, stringRole = null, enumRole = null,
            pastDate = now.minusDays(1),
            pastOrPresentDateTime = java.time.LocalDateTime.now().minusSeconds(1),
            futureInstant = java.time.Instant.now().plusSeconds(3600),
            futureOrPresentOffset = java.time.OffsetDateTime.now().plusDays(1)
        )
        val validResult = Phase3TestModelValidator.validate(validModel)
        assertTrue(validResult.valid)

        val invalidModel = Phase3TestModel(
            website = null, phone = null, alphabetic = null, alphanumeric = null,
            lowercase = null, uppercase = null, tag = null, code = null, suffix = null,
            minVal = null, maxVal = null, positiveVal = null, positiveOrZeroVal = null,
            negativeVal = null, negativeOrZeroVal = null, rangeVal = null,
            items = null, names = null, stringRole = null, enumRole = null,
            pastDate = now.plusDays(1), 
            pastOrPresentDateTime = java.time.LocalDateTime.now().plusDays(1), 
            futureInstant = java.time.Instant.now().minusSeconds(3600), 
            futureOrPresentOffset = java.time.OffsetDateTime.now().minusDays(1) 
        )
        val invalidResult = Phase3TestModelValidator.validate(invalidModel)
        assertFalse(invalidResult.valid)
        assertEquals(4, invalidResult.errors.size)
        
        assertEquals("PAST_REQUIRED", invalidResult.errors.find { it.field == "pastDate" }?.code)
        assertEquals("PAST_REQUIRED", invalidResult.errors.find { it.field == "pastOrPresentDateTime" }?.code)
        assertEquals("FUTURE_REQUIRED", invalidResult.errors.find { it.field == "futureInstant" }?.code)
        assertEquals("FUTURE_REQUIRED", invalidResult.errors.find { it.field == "futureOrPresentOffset" }?.code)
    }

    @Test
    fun testCustomValidatorAndValidationContext() {
        val validAccount = Account(
            username = "valid_user_123",
            password = "StrongPassword1"
        )
        val result = AccountValidator.validate(validAccount)
        assertTrue(result.valid)

        val invalidAccount = Account(
            username = "INVALID-USERNAME",
            password = "StrongPassword1"
        )
        val resultInvalid = AccountValidator.validate(invalidAccount)
        assertFalse(resultInvalid.valid)
        assertEquals(1, resultInvalid.errors.size)
        val error = resultInvalid.errors[0]
        assertEquals("username", error.field)
        assertEquals("CUSTOM_VALIDATION_FAILED", error.code)
        assertEquals("invalid username", error.message)
        assertEquals("INVALID-USERNAME", error.rejectedValue)
        assertEquals("com.example.jvm.Username", error.constraint)
        assertEquals("username", error.path)
    }

    @Test
    fun testMetaAnnotationComposition() {
        // Test too short password (length 7)
        val accountTooShort = Account(
            username = "user",
            password = "Short1"
        )
        val result1 = AccountValidator.validate(accountTooShort)
        assertFalse(result1.valid)
        val error1 = result1.errors.find { it.code == "MIN_LENGTH" }
        assertEquals("password", error1?.field)
        assertEquals("must be a strong password", error1?.message)

        // Test missing number/uppercase character pattern
        val accountNoPattern = Account(
            username = "user",
            password = "alllowercase"
        )
        val result2 = AccountValidator.validate(accountNoPattern)
        assertFalse(result2.valid)
        val error2 = result2.errors.find { it.code == "PATTERN_MISMATCH" }
        assertEquals("password", error2?.field)
        assertEquals("must be a strong password", error2?.message)

        // Test blank password
        val accountBlank = Account(
            username = "user",
            password = "        "
        )
        val result3 = AccountValidator.validate(accountBlank)
        assertFalse(result3.valid)
        // Should trigger NOT_BLANK and PATTERN_MISMATCH (since spaces don't match pattern)
        assertTrue(result3.errors.any { it.code == "NOT_BLANK" })
    }

    @Test
    fun testObjectConstraintValidator() {
        val now = java.time.LocalDate.now()
        val validEvent = Event(
            startDate = now,
            endDate = now.plusDays(5)
        )
        val result1 = EventValidator.validate(validEvent)
        assertTrue(result1.valid)

        val invalidEvent = Event(
            startDate = now.plusDays(5),
            endDate = now
        )
        val result2 = EventValidator.validate(invalidEvent)
        assertFalse(result2.valid)
        assertEquals(1, result2.errors.size)
        val error = result2.errors[0]
        assertEquals("", error.field)
        assertEquals("CUSTOM_VALIDATION_FAILED", error.code)
        assertEquals("invalid date range", error.message)
        assertEquals(invalidEvent, error.rejectedValue)
        assertEquals("com.example.jvm.ValidDateRange", error.constraint)
        assertEquals("", error.path)
    }

    @Test
    fun testFieldsMatchValidation() {
        val validForm = RegisterForm(
            email = "test@valix.io",
            password = "superSecurePassword",
            confirmPassword = "superSecurePassword"
        )
        val result1 = RegisterFormValidator.validate(validForm)
        assertTrue(result1.valid)

        val invalidForm = RegisterForm(
            email = "test@valix.io",
            password = "superSecurePassword",
            confirmPassword = "differentPassword"
        )
        val result2 = RegisterFormValidator.validate(invalidForm)
        assertFalse(result2.valid)
        assertEquals(1, result2.errors.size)
        val error = result2.errors[0]
        assertEquals("confirmPassword", error.field)
        assertEquals("FIELDS_MATCH_INVALID", error.code)
        assertEquals("Passwords must match", error.message)
        assertEquals("differentPassword", error.rejectedValue)
        assertEquals("io.valix.annotations.FieldsMatch", error.constraint)
        assertEquals("confirmPassword", error.path)
    }

    @Test
    fun testLocalization() {
        val _triggerRegistry = io.valix.generated.ValixRegistry
        val resolver = PropertiesMessageResolver()
        ValixConfig.messageResolver = resolver
        ValixConfig.defaultLocale = Locale.ENGLISH

        val user = TestUser(
            username = "user",
            displayName = "", // violates NotBlank
            email = "invalid-email", // violates Email
            shortCode = "12", // violates MinLength(5)
            longCode = "12345",
            numericCode = "123"
        )
        val result = TestUserValidator.validate(user)
        assertFalse(result.valid)

        // Resolve messages in English (default)
        val resolvedEn = result.resolveMessages()
        val blankErrorEn = resolvedEn.errors.find { it.field == "displayName" }
        val emailErrorEn = resolvedEn.errors.find { it.field == "email" }
        val lenErrorEn = resolvedEn.errors.find { it.field == "shortCode" }

        assertEquals("must not be blank", blankErrorEn?.message)
        assertEquals("invalid email", emailErrorEn?.message)
        assertEquals("minimum length is 5", lenErrorEn?.message)

        // Resolve messages in Hindi
        val resolvedHi = result.resolveMessages(locale = Locale("hi"))
        val blankErrorHi = resolvedHi.errors.find { it.field == "displayName" }
        val emailErrorHi = resolvedHi.errors.find { it.field == "email" }
        assertEquals("खाली नहीं होना चाहिए", blankErrorHi?.message)
        assertEquals("अमान्य ईमेल", emailErrorHi?.message)

        // Resolve messages in French
        val resolvedFr = result.resolveMessages(locale = Locale.FRENCH)
        val blankErrorFr = resolvedFr.errors.find { it.field == "displayName" }
        assertEquals("ne doit pas être vide", blankErrorFr?.message)

        // Resolve messages in Spanish
        val resolvedEs = result.resolveMessages(locale = Locale("es"))
        val blankErrorEs = resolvedEs.errors.find { it.field == "displayName" }
        assertEquals("no debe estar en blanco", blankErrorEs?.message)
    }

    @Test
    fun testJsonSchemaAndOpenApiGeneration() {
        val _triggerRegistry = io.valix.generated.ValixRegistry
        val metadata = MetadataRegistry.get(TestUser::class)
        assertNotNull(metadata)

        val jsonSchema = JsonSchemaGenerator.generate(metadata)
        assertTrue(jsonSchema.contains("\"title\": \"TestUser\""))
        assertTrue(jsonSchema.contains("\"type\": \"string\""))
        assertTrue(jsonSchema.contains("\"minLength\": 5"))
        assertTrue(jsonSchema.contains("\"format\": \"email\""))

        val openApiYaml = OpenApiSchemaGenerator.generateComponent(metadata)
        assertTrue(openApiYaml.contains("TestUser:"))
        assertTrue(openApiYaml.contains("type: string"))
        assertTrue(openApiYaml.contains("minLength: 5"))
        assertTrue(openApiYaml.contains("format: email"))
    }

    @Test
    fun testSerializationBridge() {
        val _triggerRegistry = io.valix.generated.ValixRegistry
        val metadata = MetadataRegistry.get(TestUser::class)
        assertNotNull(metadata)

        val jsonMetadata = metadata.toJson()
        assertTrue(jsonMetadata.contains("\"modelFqName\": \"com.example.jvm.TestUser\""))
        assertTrue(jsonMetadata.contains("\"name\": \"email\""))

        val descriptor = PrimitiveSerialDescriptor("username", PrimitiveKind.STRING)
        val enriched = descriptor.mergeValixMetadata(metadata)
        assertEquals(metadata, enriched.metadata)
        assertEquals("username", enriched.original.serialName)
        assertEquals(metadata.fields.find { it.name == "email" }, enriched.getFieldMetadata("email"))
    }
}

