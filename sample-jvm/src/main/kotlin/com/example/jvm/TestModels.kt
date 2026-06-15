package com.example.jvm

import io.valix.annotations.*

// Validation Groups
interface Create
interface Update

// Nested Validation Models
data class Country(
    @NotBlank(message = "Country name must not be blank")
    val name: String
)

data class Address(
    @NotBlank
    val city: String,

    @Valid
    val country: Country?
)

data class Nickname(
    @MinLength(value = 3, message = "Nickname too short")
    val name: String
)

data class User(
    @Email(groups = [Create::class], message = "Custom email invalid")
    val email: String,

    @Valid
    val address: Address,

    @Valid
    val nicknames: List<Nickname>?
)

// Collection (Set) Validation Models
data class Member(
    @NotBlank
    val name: String
)

data class Project(
    @Valid
    val members: Set<Member>
)

// Phase 3 Test Models
enum class UserRole {
    ADMIN, USER, GUEST
}

data class Phase3TestModel(
    // Strings
    @Url val website: String?,
    @PhoneNumber val phone: String?,
    @Alpha val alphabetic: String?,
    @AlphaNumeric val alphanumeric: String?,
    @LowerCase val lowercase: String?,
    @UpperCase val uppercase: String?,
    @Contains("valix") val tag: String?,
    @StartsWith("PREFIX_") val code: String?,
    @EndsWith("_suffix") val suffix: String?,

    // Numbers
    @Min(10) val minVal: Int?,
    @Max(100) val maxVal: Long?,
    @Positive val positiveVal: Double?,
    @PositiveOrZero val positiveOrZeroVal: Float?,
    @Negative val negativeVal: Int?,
    @NegativeOrZero val negativeOrZeroVal: Long?,
    @Range(min = 5, max = 50) val rangeVal: Short?,

    // Collections
    @NotEmpty val items: List<String>?,
    @Size(min = 2, max = 5) val names: Set<String>?,

    // Enums
    @AllowedValues(["ADMIN", "USER"]) val stringRole: String?,
    @AllowedValues(["ADMIN", "USER"]) val enumRole: UserRole?,

    // Dates
    @Past val pastDate: java.time.LocalDate?,
    @PastOrPresent val pastOrPresentDateTime: java.time.LocalDateTime?,
    @Future val futureInstant: java.time.Instant?,
    @FutureOrPresent val futureOrPresentOffset: java.time.OffsetDateTime?
)

