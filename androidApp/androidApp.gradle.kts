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
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "gizz.tapes"
    compileSdk = libs.versions.android.sdk.get().toInt()


    signingConfigs {
        val keystoreLocation: String by project
        val keystorePassword: String by project
        val storeKeyAlias: String by project
        val aliasKeyPassword: String by project

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
        val buildNumber: String by project
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.sdk.get().toInt()
        versionCode = buildNumber.toInt()
        versionName = properties["gizz.tapes.versionName"] as String
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        base.archivesName = "gizz-tapes-$versionName-$buildNumber"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        val debug by getting {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
            (this as ExtensionAware).configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        val release by getting {
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

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    testOptions.unitTests.isReturnDefaultValues = true
    buildFeatures {
        compose = true
        shaders = false
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(projects.composeApp)

    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.kermit)
    implementation(libs.metro.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.arrow.fx)

    debugImplementation(libs.compose.ui.tooling)

    "fullImplementation"(platform(libs.firebase.bom))
    "fullImplementation"(libs.bundles.firebase)
    "fullImplementation"(libs.media3.cast)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.datetime)
    testImplementation(libs.truth)
}

tasks.withType<GoogleServicesTask> {
    enabled = name.contains("full", ignoreCase = true)
}
