plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":valix-core"))
    implementation(project(":valix-metadata"))
    implementation(project(":valix-localization"))

    compileOnly("org.springframework:spring-web:6.1.5")
    compileOnly("org.springframework:spring-webmvc:6.1.5")
    compileOnly("org.springframework:spring-context:6.1.5")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.2.4")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework:spring-test:6.1.5")
    testImplementation("org.springframework:spring-webmvc:6.1.5")
}

tasks.test {
    useJUnitPlatform()
}
