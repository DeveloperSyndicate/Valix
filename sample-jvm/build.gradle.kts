buildscript {
    dependencies {
        classpath(files("../valix-gradle-plugin/build/classes/kotlin/main"))
        classpath(files("../valix-gradle-plugin/build/resources/main"))
    }
}

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    application
}

apply(plugin = "io.valix.gradle")

dependencies {
    implementation(project(":valix-annotations"))
    implementation(project(":valix-core"))
    implementation(project(":valix-metadata"))
    implementation(project(":valix-localization"))
    implementation(project(":valix-schema"))
    implementation(project(":valix-serialization"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
    implementation(project(":valix-runtime"))
    implementation(project(":valix-flow"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    ksp(project(":valix-ksp"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.jvm.MainKt")
}

kotlin {
    jvmToolchain(17)
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

tasks.test {
    useJUnitPlatform()
}
