plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":valix-core"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.11")
    implementation("com.squareup:kotlinpoet:2.3.0")
    implementation("com.squareup:kotlinpoet-ksp:2.3.0")
    
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
