import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.google.gms.googleservices.GoogleServicesTask

plugins {
    id("com.android.application")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)

    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)

    alias(libs.plugins.metro)

    id("signing-config")
    id("build-number")
    id("kotlin-conventions")
}

android {
    namespace = "gizz.tapes"
    compileSdk = libs.versions.android.sdk.get().toInt()


    signingConfigs {
        val keystoreLocation = project.property("keystoreLocation") as String
        val keystorePassword = project.property("keystorePassword") as String
        val storeKeyAlias = project.property("storeKeyAlias") as String
        val aliasKeyPassword = project.property("aliasKeyPassword") as String

        getByName("debug") {
            storeFile = rootProject.file("keys/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = rootProject.file(keystoreLocation)
            storePassword = keystorePassword
            keyAlias = storeKeyAlias
            keyPassword = aliasKeyPassword
        }
    }

    // done
    defaultConfig {
        val buildNumber = project.property("buildNumber") as String
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.sdk.get().toInt()
        versionCode = buildNumber.toInt()
        versionName = project.property("gizz.tapes.versionName") as String
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        base.archivesName = "gizz-tapes-$versionName-$buildNumber"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            (this as ExtensionAware).configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "version"
    productFlavors {
        // no firebase and no cast framework because these are no foss
        // this release is sent to f-droid
        create("foss") {
            applicationIdSuffix = ".foss"
            dimension = "version"
            (this as ExtensionAware).configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }

        // contains cast and firebase
        create("full") {
            applicationIdSuffix = ".full"
            dimension = "version"
        }
    }

    testOptions.unitTests.isReturnDefaultValues = true
    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.all {
        // Robolectric instruments internal java.*/jdk.* classes, which JPMS blocks by default
        // on JDK 17+ (project targets JDK 21 via jvmToolchain).
        it.jvmArgs(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.base/java.security=ALL-UNNAMED",
            "--add-opens=java.base/java.text=ALL-UNNAMED",
            "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
            "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        )
    }
    buildFeatures {
        compose = true
        shaders = false
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    bundle {
        // Ship all densities in the base APK. Installs missing the density split (e.g. sideloaded
        // base.apk) crash loading mediarouter's PNG-only cast button drawables.
        density {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(projects.composeApp)

    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.workmanager)

    implementation(libs.kermit)
    implementation(libs.metro.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.arrow.fx)
    implementation(libs.sqldelight.android.driver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    debugImplementation(libs.compose.ui.tooling)

    "fullImplementation"(platform(libs.firebase.bom))
    "fullImplementation"(libs.bundles.firebase)
    "fullImplementation"(libs.media3.cast)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
}

tasks.withType<GoogleServicesTask> {
    enabled = name.contains("full", ignoreCase = true)
}

// Crashlytics injects a random mapping file id into every variant. Keep it out of foss so the
// GitHub foss APK matches F-Droid's build (which strips the Firebase plugins).
tasks.configureEach {
    if (name.startsWith("injectCrashlytics") && name.contains("foss", ignoreCase = true)) {
        enabled = false
    }
}
