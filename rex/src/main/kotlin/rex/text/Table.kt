package rex.text

/**
 * Simple ASCII table renderer for console output.
 */
class Table(private val columns: List<String>) {
    private val rows = mutableListOf<List<String>>()

    fun addRow(vararg cells: String): Table {
        rows += cells.toList().let { row ->
            // Pad/trim to column count
            List(columns.size) { i -> row.getOrElse(i) { "" } }
        }
        return this
    }

    fun render(): String {
        val widths = List(columns.size) { col ->
            maxOf(columns[col].length, rows.maxOfOrNull { it[col].length } ?: 0) + 2
        }
        val separator = "+" + widths.joinToString("+") { "-".repeat(it) } + "+"
        val header = "|" + columns.mapIndexed { i, h -> " ${h.padEnd(widths[i] - 2)} " }.joinToString("|") + "|"

        return buildString {
            appendLine(separator)
            appendLine(header)
            appendLine(separator)
            rows.forEach { row ->
                val line = "|" + row.mapIndexed { i, cell ->
                    " ${cell.padEnd(widths[i] - 2)} "
                }.joinToString("|") + "|"
                appendLine(line)
            }
            append(separator)
        }
    }
}
