import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
    id("kotlin-conventions")
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("AppKt")
        }
    }
    macosArm64 {
        binaries.executable {
            entryPoint = "main"
        }
    }
    js {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.networking)

            implementation(libs.kotlinx.coroutines)

            implementation(libs.ktor.json)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.logging)

            implementation(libs.arrow.fx)
        }
    }
}

tasks.register("runIntegrationTests") {
    group = "verification"
    description = "runs the api with live data on all available platforms"
    // KGP has no public provider for the JVM and JS run tasks, so those are referenced by name.
    dependsOn("jvmRun", "jsNodeProductionRun")
    kotlin.macosArm64().binaries.getExecutable(NativeBuildType.RELEASE).runTaskProvider?.let { dependsOn(it) }
}
