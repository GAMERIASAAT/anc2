package rex.text

/**
 * Cyclic pattern generation for finding buffer offsets (De Bruijn sequence).
 */
object Patterns {
    private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"

    fun create(length: Int): String {
        val sb = StringBuilder()
        outer@ for (a in UPPER) {
            for (b in LOWER) {
                for (c in DIGITS) {
                    sb.append(a).append(b).append(c)
                    if (sb.length >= length) break@outer
                }
            }
        }
        return sb.substring(0, minOf(length, sb.length))
    }

    fun offset(pattern: String, value: String): Int = pattern.indexOf(value)

    fun offset(pattern: String, value: Int, littleEndian: Boolean = true): Int {
        val bytes = if (littleEndian) {
            byteArrayOf(
                (value and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
                ((value shr 16) and 0xFF).toByte(),
                ((value shr 24) and 0xFF).toByte()
            )
        } else {
            byteArrayOf(
                ((value shr 24) and 0xFF).toByte(),
                ((value shr 16) and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
                (value and 0xFF).toByte()
            )
        }
        val search = bytes.map { it.toInt().toChar() }.joinToString("")
        return offset(pattern, search)
    }
}
