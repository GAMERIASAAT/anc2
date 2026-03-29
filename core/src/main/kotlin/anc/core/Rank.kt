package anc.core

/**
 * Module reliability rank, mirroring the established convention:
 * Manual < Low < Average < Normal < Good < Great < Excellent
 */
enum class Rank(val value: Int, val label: String) {
    MANUAL(0, "manual"),
    LOW(100, "low"),
    AVERAGE(200, "average"),
    NORMAL(300, "normal"),
    GOOD(400, "good"),
    GREAT(500, "great"),
    EXCELLENT(600, "excellent");

    override fun toString() = label

    companion object {
        fun fromLabel(label: String): Rank =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: NORMAL
    }
}
