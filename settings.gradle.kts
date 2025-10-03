@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("com.gradle.develocity") version "4.2.1"
}

include(
    ":android",
    ":composeApp",
    ":networking",
    ":networking-integration",
    ":utils"
)

rootProject.name = "gizz-tapes"

rootProject.children.forEach {
    it.buildFileName = "${it.name}.gradle.kts"
}

develocity {
    buildScan {
        publishing.onlyIf { System.getProperty("GIZZ_TAPES_ACCEPT_BUILD_SCAN_AGREEMENT") != null }
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set(System.getProperty("GIZZ_TAPES_ACCEPT_BUILD_SCAN_AGREEMENT", "no"))
    }
}
