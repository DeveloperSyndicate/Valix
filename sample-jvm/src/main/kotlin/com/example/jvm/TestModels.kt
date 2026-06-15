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
