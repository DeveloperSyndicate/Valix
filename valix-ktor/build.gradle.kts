plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":valix-core"))
    implementation(project(":valix-localization"))

    compileOnly("io.ktor:ktor-server-core:3.5.2")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:3.5.2")
}

tasks.test {
    useJUnitPlatform()
}
