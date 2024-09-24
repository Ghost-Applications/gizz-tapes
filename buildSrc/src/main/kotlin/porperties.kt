import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

// namespaces for properties passed to gradle
internal const val projectNamespace = "gizz.tapes"
internal const val systemNamespace = "GIZZ_TAPES"

/**
 * Loads properties from gradle.properties, system properties or command line.
 *
 * @see [ProjectProperties](https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties)
 */
internal fun Project.loadPropertyIntoExtra(
    extraKey: String,
    projectPropertyKey: String,
    systemPropertyKey: String,
    defaultValue: String
) {
    val namespacedProjectProperty = "$projectNamespace.$projectPropertyKey"
    val namespacedSystemProperty = "${systemNamespace}_$systemPropertyKey"

    extra[extraKey] = when {
        hasProperty(namespacedProjectProperty) -> properties[namespacedProjectProperty]
        else -> System.getProperty(namespacedSystemProperty) ?: defaultValue
    }
}
