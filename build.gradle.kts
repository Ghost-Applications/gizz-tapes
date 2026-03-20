import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import gizz.gradle.isNonStable
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.version.check)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.multiplatform.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
    source.setFrom(
        files(
            "$rootDir/androidApp/src",
            "$rootDir/composeApp/src",
            "$rootDir/networking/src",
            "$rootDir/networking-integration/src",
        )
    )
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

subprojects {
    tasks.withType<Test> {
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required = true
        xml.required = false
        txt.required = false
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
