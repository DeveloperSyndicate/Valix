plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":valix-core"))
    implementation(project(":valix-metadata"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
