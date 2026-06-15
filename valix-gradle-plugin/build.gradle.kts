plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("valixPlugin") {
            id = "io.valix.gradle"
            implementationClass = "io.valix.gradle.ValixGradlePlugin"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
}
