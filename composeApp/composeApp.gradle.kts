import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.multiplatform.android.library)
    alias(libs.plugins.kotlin.native.cocoapods)

    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.sqldelight)

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

        compileSdk = libs.versions.android.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()

        withHostTest { }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "GizzTapes"
            isStatic = true
            binaryOption("bundleId", "gizz.tapes")
        }
        // The Cast SDK's cinterop def only declares `-framework GoogleCast`; Xcode's app build
        // resolves GoogleCast's own transitive framework dependencies automatically, but the
        // standalone test binary Gradle links here does not, so they must be listed explicitly.
        iosTarget.binaries.getTest("DEBUG").linkerOpts(
            "-ObjC",
            "-framework", "Network",
            "-framework", "CoreData",
            "-framework", "SystemConfiguration",
            "-framework", "MediaAccessibility",
            "-framework", "AVFoundation",
            "-framework", "AVKit",
            "-framework", "MediaPlayer",
        )
    }

    // composeApp is consumed as a CocoaPods pod by iosApp/Podfile (`pod 'composeApp', :path =>
    // '../composeApp'`) so that google-cast-sdk's framework gets embedded into the app; this
    // replaces the old manual "Compile Kotlin Framework" build phase (see iosApp.xcodeproj).
    cocoapods {
        version = "1.0.0"
        summary = "Gizz Tapes shared module"
        homepage = "https://github.com/Ghost-Applications/gizz-tapes"
        ios.deploymentTarget = "15.3"
        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "GizzTapes"
            isStatic = true
            binaryOption("bundleId", "gizz.tapes")
        }

        // Pinned to 4.8.4 (not the latest 4.8.6) because 4.8.6 requires iOS 16+ while this app's
        // deployment target is 15.3; 4.8.4 is the newest release still compatible with that.
        pod("google-cast-sdk") {
            version = "4.8.4"
            moduleName = "GoogleCast"
        }
    }

    jvm("desktop")

    sourceSets {
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
            implementation(libs.arrow.fx)

            implementation(libs.sqldelight.primitive.adapters)

            implementation(libs.html.text)

            api(libs.metro.viewmodel)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.compose.ui.test)
        }

        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.javacv)
            implementation(libs.ffmpeg.platform)
            implementation(libs.sqldelight.sqlite.driver)
        }

        getByName("desktopTest").dependencies {
            implementation(libs.ktor.client.mock)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        androidMain.dependencies {
            implementation(libs.metro.android)

            implementation(libs.compose.ui.tooling)

            implementation(libs.media3.exoplayer)
            implementation(libs.media3.session)

            implementation(libs.workmanager)
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

sqldelight {
    databases {
        register("Database") {
            packageName.set("gizz.tapes.db")
        }
    }
}
