enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.develocity") version "3.17.6"
}

include(
    ":mobile",
    ":networking",
    ":networking-integration"
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
