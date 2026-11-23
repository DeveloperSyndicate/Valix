plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":valix-core"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.9")
    implementation("com.squareup:kotlinpoet:2.4.0")
    implementation("com.squareup:kotlinpoet-ksp:2.4.0")
    
    testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.24.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.4.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
