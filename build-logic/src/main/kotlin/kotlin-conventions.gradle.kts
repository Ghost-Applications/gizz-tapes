import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Shared Kotlin setup for every module: JDK 21 toolchain and full exception output in tests.
 */

listOf("org.jetbrains.kotlin.multiplatform", "com.android.application").forEach { id ->
    pluginManager.withPlugin(id) {
        extensions.configure<KotlinProjectExtension> { jvmToolchain(21) }
    }
}

tasks.withType<Test>().configureEach {
    testLogging.exceptionFormat = TestExceptionFormat.FULL
}
