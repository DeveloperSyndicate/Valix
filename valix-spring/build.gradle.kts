plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":valix-core"))
    implementation(project(":valix-localization"))

    compileOnly("org.springframework:spring-web:7.0.8")
    compileOnly("org.springframework:spring-webmvc:7.0.8")
    compileOnly("org.springframework:spring-context:7.0.8")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:4.1.0")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework:spring-test:7.0.8")
    testImplementation("org.springframework:spring-webmvc:7.0.8")
}

tasks.test {
    useJUnitPlatform()
}
