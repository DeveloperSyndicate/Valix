plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":valix-core"))
    implementation(project(":valix-localization"))

    compileOnly("io.micronaut:micronaut-inject:4.10.30")
    compileOnly("io.micronaut:micronaut-aop:4.10.30")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
