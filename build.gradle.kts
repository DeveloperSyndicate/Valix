plugins {
    kotlin("jvm") version "2.3.21" apply false
    kotlin("android") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
    id("com.android.application") version "9.1.1" apply false
    id("com.android.library") version "9.1.1" apply false
}

allprojects {
    group = "io.valix"
    version = "1.0.0"
}