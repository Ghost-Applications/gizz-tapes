import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.google.gms.googleservices.GoogleServicesTask

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)

    id("signing-config")
    id("build-number")

    alias(libs.plugins.paparazzi)
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

    defaultConfig {
        val buildNumber: String by project
        minSdk = 23
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
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        val release by getting {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
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
        viewBinding = false
        aidl = false
        buildConfig = false
        compose = true
        prefab = false
        renderScript = false
        resValues = false
        shaders = false
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(projects.networking)
    implementation(kotlin("stdlib"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

    "fullImplementation"(platform(libs.firebase.bom))
    "fullImplementation"(libs.bundles.firebase)
    "fullImplementation"(libs.media3.cast)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.navigation)

    implementation(libs.bundles.hilt)
    ksp(libs.hilt.android.compiler)
    ksp(libs.kotlin.metadata.jvm)

    implementation(libs.android.material)

    implementation(libs.bundles.media3)
    implementation(libs.androidx.mediarouter)

    implementation(libs.bundles.androidx)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.navigation)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.coil)
    implementation(libs.coil.svg)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.timber)
    implementation(libs.byteunits)
    implementation(libs.okio)

    implementation(libs.markwon)
    implementation(libs.bundles.arrow)

    debugImplementation(libs.bundles.android.debug.libs)

    testImplementation(libs.bundles.android.test.libs)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.android.compiler)
}

tasks.named("build") {
    dependsOn("verifyPaparazziFullRelease")
}

tasks.withType<GoogleServicesTask> {
    enabled = name.contains("full", ignoreCase = true)
}
