plugins {
    kotlin("jvm") version "2.3.21" apply false
    kotlin("android") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
    id("com.android.application") version "9.1.1" apply false
    id("com.android.library") version "9.1.1" apply false
    id("org.jetbrains.dokka") version "1.9.20" apply false
    id("com.vanniktech.maven.publish") version "0.29.0" apply false
}

allprojects {
    group = "com.developersyndicate.valix"
    version = "1.0.0"
}

subprojects {
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
                        email.set("sanjay@developersyndicate.com")
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