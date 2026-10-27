val defaultBuildNumber: String = property("gizz.tapes.defaultBuildNumber") as String

loadPropertyIntoExtra(
    extraKey = "buildNumber",
    projectPropertyKey = "buildNumber",
    systemPropertyKey = "BUILD_NUMBER",
    defaultValue = defaultBuildNumber
)
