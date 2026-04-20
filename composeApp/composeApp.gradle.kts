import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.multiplatform.android.library)
    kotlin("native.cocoapods")

    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.metro)

    id("signing-config")
    id("build-number")
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "gizz.tapes.compose"
        compileSdk = libs.versions.android.sdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "GizzTapes"
            isStatic = true
            binaryOption("bundleId", "gizz.tapes")
        }
    }

    cocoapods {
        version = "1.0"
        summary = "Gizz Tapes KMP framework"
        homepage = "https://github.com/Ghost-Applications/gizz-tapes"
        ios.deploymentTarget = "15.3"
        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "GizzTapes"
            isStatic = true
            binaryOption("bundleId", "gizz.tapes")
        }

        pod("FirebaseCrashlytics") { linkOnly = true }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)

            implementation(libs.ktor.client)

            api(libs.androidx.datastore)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.multiplatform.compose.navigation)
            implementation(libs.kotlinx.serialization)

            api(projects.networking)
            implementation(libs.kermit)

            implementation(libs.coil)
            implementation(libs.coil.svg)
            implementation(libs.coil.network.ktor3)

            implementation(libs.arrow.resilience)

            api(libs.metro.viewmodel)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.javacv)
            implementation(libs.ffmpeg.platform)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.gitlive.firebase.crashlytics)
        }

        androidMain.dependencies {
            implementation(libs.metro.android)

            implementation(libs.media3.exoplayer)
            implementation(libs.media3.session)
        }
    }
}

compose.desktop {
    application {
        mainClass = "gizz.tapes.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Gizz Tapes"
            packageVersion = "1.0.${project.property("gizz.tapes.defaultBuildNumber")}"
            description = project.property("gizz.tapes.versionName") as String

            macOS { iconFile.set(project.file("src/desktopMain/resources/icon.icns")) }
            windows { iconFile.set(project.file("src/desktopMain/resources/icon.ico")) }
            linux { iconFile.set(project.file("src/desktopMain/resources/icon.png")) }
        }
    }
}
