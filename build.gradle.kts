plugins {
    kotlin("jvm") version "2.4.10" apply false
    kotlin("multiplatform") version "2.4.10" apply false
    kotlin("android") version "2.4.10" apply false
    id("com.google.devtools.ksp") version "2.3.11" apply false
    id("com.android.application") version "9.3.1" apply false
    id("com.android.library") version "9.3.1" apply false
    id("org.jetbrains.dokka") version "2.2.0"
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

allprojects {
    group = "com.developersyndicate.valix"
    version = "1.0.5"
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        buildUponDefaultConfig = true
        parallel = true
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        reports {
            html.required.set(true)
            xml.required.set(true)
            sarif.required.set(true)
            txt.required.set(false)
        }
    }

    val publishableModules = setOf(
        "valix-annotations",
        "valix-core",
        "valix-ksp",
        "valix-runtime",
        "valix-flow",
        "valix-viewmodel",
        "valix-compose",
        "valix-metadata",
        "valix-localization",
        "valix-schema",
        "valix-serialization",
        "valix-spring",
        "valix-ktor",
        "valix-micronaut",
        "valix-gradle-plugin"
    )

    if (name in publishableModules) {
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "com.vanniktech.maven.publish")

        tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
            pluginsMapConfiguration.set(
                mapOf(
                    "org.jetbrains.dokka.base.DokkaBase" to """{ "footerMessage": "© 2026 Copyright Developer Syndicate" }"""
                )
            )
        }

        configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
            publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
            signAllPublications()

            coordinates(group.toString(), project.name, version.toString())

            pom {
                name.set("Valix ${project.name.replace("valix-", "").replaceFirstChar { it.uppercase() }}")
                description.set("Compile-time validation framework for Kotlin - ${project.name}")
                url.set("https://github.com/developersyndicate/valix")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("sanjays")
                        name.set("Sanjay S")
                        email.set("dev.sanjayofficial@outlook.com")
                        organization.set("Developer Syndicate")
                        organizationUrl.set("https://developersyndicate.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/developersyndicate/valix.git")
                    developerConnection.set("scm:git:ssh://github.com/developersyndicate/valix.git")
                    url.set("https://github.com/developersyndicate/valix")
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """{ "footerMessage": "© 2026 Copyright Developer Syndicate" }"""
        )
    )
}