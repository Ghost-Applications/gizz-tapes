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


    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization)

            api(libs.ktor.client)
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
        }
    }
}
