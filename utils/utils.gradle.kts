plugins {
    kotlin("multiplatform")
    alias(libs.plugins.serialization)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    macosX64()
    js {
        browser()
        nodejs()
    }

    jvmToolchain(21)

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
