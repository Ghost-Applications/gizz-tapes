plugins {
    `kotlin-dsl`
    alias(libs.plugins.version.check)
}

dependencies {
    implementation(libs.java.poet)
}
