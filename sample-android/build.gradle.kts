plugins {
    id("com.android.library")
    id("com.google.devtools.ksp")
}

android {
    namespace = "io.valix.sample.android"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":valix-annotations"))
    implementation(project(":valix-core"))
    ksp(project(":valix-ksp"))
}
