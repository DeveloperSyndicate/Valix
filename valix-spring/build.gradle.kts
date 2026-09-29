plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":valix-core"))
    implementation(project(":valix-localization"))

    compileOnly("org.springframework:spring-web:6.2.19")
    compileOnly("org.springframework:spring-webmvc:6.2.19")
    compileOnly("org.springframework:spring-context:6.2.19")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.2.4")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework:spring-test:6.2.19")
    testImplementation("org.springframework:spring-webmvc:6.2.19")
}

tasks.test {
    useJUnitPlatform()
}
