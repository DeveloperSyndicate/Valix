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
            id = "com.developersyndicate.valix"
            implementationClass = "io.valix.gradle.ValixGradlePlugin"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
}
