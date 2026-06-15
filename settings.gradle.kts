pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "valix"

include(":valix-annotations")
include(":valix-core")
include(":valix-ksp")
include(":valix-runtime")
include(":valix-flow")
include(":valix-viewmodel")
include(":valix-compose")
include(":sample-jvm")
include(":sample-android")