package rex.arch

enum class Arch(val label: String) {
    X86("x86"),
    X64("x86_64"),
    ARM("arm"),
    ARM64("arm64"),
    MIPS("mips"),
    MIPS64("mips64"),
    JAVA("java"),
    DALVIK("dalvik"),
    ANY("any");

    override fun toString() = label

    companion object {
        fun fromLabel(label: String): Arch =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: ANY
    }
}

enum class Platform(val label: String) {
    ANDROID("android"),
    LINUX("linux"),
    WINDOWS("windows"),
    OSX("osx"),
    UNIX("unix"),
    JAVA("java"),
    PHP("php"),
    PYTHON("python"),
    RUBY("ruby"),
    ANY("any");

    override fun toString() = label

    companion object {
        fun fromLabel(label: String): Platform =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: ANY
    }
}
