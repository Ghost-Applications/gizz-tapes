package gizz.tapes

sealed interface OperatingSystem {
    data object MacOs : OperatingSystem
    data object Windows : OperatingSystem
    data object Linux : OperatingSystem
    data class Other(val name: String) : OperatingSystem

    companion object {
        val current: OperatingSystem by lazy { fromName(System.getProperty("os.name")) }

        /** Maps a JVM `os.name` value, e.g. "Mac OS X", "Windows 11" or "Linux". */
        fun fromName(name: String): OperatingSystem = when {
            name.startsWith("Mac") -> MacOs
            name.startsWith("Windows") -> Windows
            name.startsWith("Linux") -> Linux
            else -> Other(name)
        }
    }
}
