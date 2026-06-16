plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":valix-core"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
