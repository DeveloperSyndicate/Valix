plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":valix-annotations"))
    implementation(project(":valix-core"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.9")
    implementation("com.squareup:kotlinpoet:2.2.0")
    implementation("com.squareup:kotlinpoet-ksp:2.2.0")
}

kotlin {
    jvmToolchain(17)
}
