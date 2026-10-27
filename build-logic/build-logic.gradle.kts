plugins {
    `kotlin-dsl`
}

dependencies {
    // compileOnly: the app modules already put the Kotlin plugin on the classpath.
    compileOnly(libs.kotlin.gradle.plugin)
}
