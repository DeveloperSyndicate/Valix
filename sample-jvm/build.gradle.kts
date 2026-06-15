plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    application
}

dependencies {
    implementation(project(":valix-annotations"))
    implementation(project(":valix-core"))
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
