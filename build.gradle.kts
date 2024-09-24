import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import gizz.gradle.isNonStable
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.version.check)
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.serialization) apply false
}

subprojects {
    tasks.withType<Test> {
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
