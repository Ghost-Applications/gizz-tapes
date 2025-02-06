plugins {
    kotlin("multiplatform")
    alias(libs.plugins.serialization)
}

kotlin {
    // jvm and iOS targets will be priority for apps
    // with plans / abilities to support the others in the future.
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

    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client)
            api(libs.kotlinx.datetime)
            api(libs.arrow.core)

            implementation(libs.kotlinx.serialization)
            implementation(libs.arrow.core.serialization)
            implementation(libs.ktor.json)
            implementation(libs.ktor.content.negotiation)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        macosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            // temp work around for coroutines, remove later
            implementation("org.jetbrains.kotlin:kotlinx-atomicfu-runtime:2.1.10")
        }
    }
}
