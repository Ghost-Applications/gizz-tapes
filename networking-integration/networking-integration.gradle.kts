import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
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
    macosX64 {
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

            implementation(libs.arrow.fx)
        }
    }
}

tasks.register("runIntegrationTests") {
    group = "verification"
    description = "runs the api with live data on all available platforms"
    // todo figure out how to make this dynamic
    dependsOn("jvmRun", "jsNodeRun", "runReleaseExecutableMacosArm64", "runReleaseExecutableMacosX64")
}
