package rex.text

object Hex {
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        bytes.forEach { b ->
            val i = b.toInt() and 0xFF
            sb.append(HEX_CHARS[i shr 4])
            sb.append(HEX_CHARS[i and 0x0F])
        }
        return sb.toString()
    }

    fun decode(hex: String): ByteArray {
        val clean = hex.replace("\\s".toRegex(), "").replace("0x", "").replace(":", "")
        require(clean.length % 2 == 0) { "Hex string length must be even" }
        return ByteArray(clean.length / 2) { i ->
            ((clean[i * 2].digitToInt(16) shl 4) + clean[i * 2 + 1].digitToInt(16)).toByte()
        }
    }

    /** Pretty hex dump like 00 11 22 33 ... */
    fun dump(bytes: ByteArray, bytesPerLine: Int = 16): String {
        val sb = StringBuilder()
        bytes.toList().chunked(bytesPerLine).forEachIndexed { idx, chunk ->
            val offset = "%08x".format(idx * bytesPerLine)
            val hex = chunk.joinToString(" ") { "%02x".format(it.toInt() and 0xFF) }
                .padEnd(bytesPerLine * 3 - 1)
            val ascii = chunk.map { if (it.toInt() and 0xFF in 0x20..0x7e) it.toInt().toChar() else '.' }
                .joinToString("")
            sb.appendLine("$offset  $hex  |$ascii|")
        }
        return sb.toString()
    }
}
