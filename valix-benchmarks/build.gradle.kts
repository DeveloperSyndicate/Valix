plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("me.champeau.jmh") version "0.7.2"
}

kotlin {
    jvmToolchain(17)
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

dependencies {
    implementation(project(":valix-core"))
    implementation(project(":valix-runtime"))
    ksp(project(":valix-ksp"))

    implementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
    implementation("org.glassfish:jakarta.el:4.0.2")

    // JMH dependencies
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

jmh {
    fork.set(1)
    warmupIterations.set(2)
    iterations.set(3)
    benchmarkMode.set(listOf("thrpt")) // Throughput (ops/sec)
}
