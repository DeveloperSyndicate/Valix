plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    js(IR) {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            // Pure Kotlin stdlib annotations
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
