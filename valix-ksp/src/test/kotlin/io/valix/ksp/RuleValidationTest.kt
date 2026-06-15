package io.valix.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import io.valix.ksp.rules.AllowedValuesRule
import io.valix.ksp.rules.MinLengthRule
import io.valix.ksp.rules.PastRule
import io.valix.ksp.rules.PositiveRule
import io.valix.ksp.rules.RangeRule
import io.valix.ksp.rules.SizeRule
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuleValidationTest {

    private val logger: KSPLogger = mock()

    private fun mockProperty(typeFqn: String): KSPropertyDeclaration {
        val name = mockName(typeFqn)
        val typeDeclaration: KSClassDeclaration = mock()
        org.mockito.kotlin.whenever(typeDeclaration.qualifiedName).doReturn(name)

        val type: KSType = mock()
        org.mockito.kotlin.whenever(type.declaration).doReturn(typeDeclaration)

        val property: KSPropertyDeclaration = mock()
        val typeReference: com.google.devtools.ksp.symbol.KSTypeReference = mock()
        org.mockito.kotlin.whenever(typeReference.resolve()).doReturn(type)
        org.mockito.kotlin.whenever(property.type).doReturn(typeReference)
        return property
    }

    private fun mockName(fqn: String): KSName {
        val m: KSName = mock()
        org.mockito.kotlin.whenever(m.asString()).doReturn(fqn)
        return m
    }

    private fun mockAnnotation(annotationName: String, args: Map<String, Any?> = emptyMap()): KSAnnotation {
        val valueArguments = args.map { (k, v) ->
            val argName = mockName(k)
            val arg: KSValueArgument = mock()
            org.mockito.kotlin.whenever(arg.name).doReturn(argName)
            org.mockito.kotlin.whenever(arg.value).doReturn(v)
            arg
        }
        val annName = mockName(annotationName.substringAfterLast('.'))
        val ann: KSAnnotation = mock()
        org.mockito.kotlin.whenever(ann.shortName).doReturn(annName)
        org.mockito.kotlin.whenever(ann.arguments).doReturn(valueArguments)
        return ann
    }

    @Test
    fun testMinLengthOnIntFails() {
        val prop = mockProperty("kotlin.Int")
        val ann = mockAnnotation("io.valix.annotations.MinLength", mapOf("value" to 5))
        assertFalse(MinLengthRule.validate(prop, ann, logger))
        verify(logger).error(eq("@MinLength can only be applied to String or String? properties"), eq(prop))
    }

    @Test
    fun testMinLengthOnStringPasses() {
        val prop = mockProperty("kotlin.String")
        val ann = mockAnnotation("io.valix.annotations.MinLength", mapOf("value" to 5))
        assertTrue(MinLengthRule.validate(prop, ann, logger))
    }

    @Test
    fun testPositiveOnStringFails() {
        val prop = mockProperty("kotlin.String")
        val ann = mockAnnotation("io.valix.annotations.Positive")
        assertFalse(PositiveRule.validate(prop, ann, logger))
        verify(logger).error(eq("@Positive can only be applied to Int, Long, Float, Double, or Short properties"), eq(prop))
    }

    @Test
    fun testPositiveOnIntPasses() {
        val prop = mockProperty("kotlin.Int")
        val ann = mockAnnotation("io.valix.annotations.Positive")
        assertTrue(PositiveRule.validate(prop, ann, logger))
    }

    @Test
    fun testPastOnBooleanFails() {
        val prop = mockProperty("kotlin.Boolean")
        val ann = mockAnnotation("io.valix.annotations.Past")
        assertFalse(PastRule.validate(prop, ann, logger))
        verify(logger).error(eq("@Past can only be applied to LocalDate, LocalDateTime, Instant, or OffsetDateTime properties"), eq(prop))
    }

    @Test
    fun testRangeWithMinGreaterThanMaxFails() {
        val prop = mockProperty("kotlin.Int")
        val ann = mockAnnotation("io.valix.annotations.Range", mapOf("min" to 100L, "max" to 10L))
        assertFalse(RangeRule.validate(prop, ann, logger))
        verify(logger).error(eq("@Range min value cannot be greater than max value"), eq(prop))
    }

    @Test
    fun testRangeWithValidParametersPasses() {
        val prop = mockProperty("kotlin.Int")
        val ann = mockAnnotation("io.valix.annotations.Range", mapOf("min" to 10L, "max" to 100L))
        assertTrue(RangeRule.validate(prop, ann, logger))
    }

    @Test
    fun testSizeWithMinGreaterThanMaxFails() {
        val prop = mockProperty("kotlin.collections.List")
        val ann = mockAnnotation("io.valix.annotations.Size", mapOf("min" to 10, "max" to 2))
        assertFalse(SizeRule.validate(prop, ann, logger))
        verify(logger).error(eq("@Size max value cannot be less than min value"), eq(prop))
    }

    @Test
    fun testSizeWithNegativeMinFails() {
        val prop = mockProperty("kotlin.collections.List")
        val ann = mockAnnotation("io.valix.annotations.Size", mapOf("min" to -1, "max" to 10))
        assertFalse(SizeRule.validate(prop, ann, logger))
        verify(logger).error(eq("@Size min value must be non-negative"), eq(prop))
    }

    @Test
    fun testAllowedValuesOnInvalidTypeFails() {
        val prop = mockProperty("kotlin.Boolean")
        val ann = mockAnnotation("io.valix.annotations.AllowedValues", mapOf("value" to listOf("YES", "NO")))
        assertFalse(AllowedValuesRule.validate(prop, ann, logger))
        verify(logger).error(eq("@AllowedValues can only be applied to Enum or String/String? properties"), eq(prop))
    }
}
