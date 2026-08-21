plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":valix-core"))
    implementation(project(":valix-localization"))

    compileOnly("io.micronaut:micronaut-inject:5.1.11")
    compileOnly("io.micronaut:micronaut-aop:5.1.11")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
