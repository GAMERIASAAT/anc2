package anc.modules.nops.arm

import anc.core.Rank
import anc.core.nop.Nop
import rex.arch.Arch
import rex.arch.Platform

/**
 * ARM / Thumb NOP sled generator.
 *
 * ARM mode NOP  : e3 20 f0 00  (MOV r0, #0 hint / NOP hint, little-endian)
 * Thumb mode NOP: 00 bf         (NOP, Thumb-2 encoding)
 */
class NopArm : Nop() {
    override val name        = "ARM NOP Sled"
    override val description = "Generates ARM/Thumb NOP sleds"
    override val modulePath  = "arm/simple"
    override val arch        = listOf(Arch.ARM, Arch.ARM64)
    override val platform    = listOf(Platform.ANDROID, Platform.LINUX)
    override val rank        = Rank.NORMAL

    enum class Mode { ARM, THUMB }

    /** Default mode is THUMB (most common in Android/modern ARM). */
    var mode: Mode = Mode.THUMB

    /** ARM-mode NOP (4 bytes, little-endian): e320f000 */
    private val armNop   = byteArrayOf(0x00.toByte(), 0xf0.toByte(), 0x20.toByte(), 0xe3.toByte())

    /** Thumb-mode NOP (2 bytes): bf00 */
    private val thumbNop = byteArrayOf(0x00.toByte(), 0xbf.toByte())

    override fun generate(length: Int, badChars: ByteArray): ByteArray {
        val pattern = if (mode == Mode.ARM) armNop else thumbNop
        val unit    = pattern.size
        require(length % unit == 0) {
            "NOP sled length must be a multiple of $unit bytes for ${mode.name} mode"
        }
        require(pattern.none { badChars.contains(it) }) {
            "NOP pattern contains bad chars — consider switching ARM/THUMB mode"
        }
        val result = ByteArray(length)
        for (i in 0 until length / unit) pattern.copyInto(result, i * unit)
        return result
    }

    fun generateArm(length: Int, badChars: ByteArray = byteArrayOf()): ByteArray {
        mode = Mode.ARM; return generate(length, badChars)
    }

    fun generateThumb(length: Int, badChars: ByteArray = byteArrayOf()): ByteArray {
        mode = Mode.THUMB; return generate(length, badChars)
    }
}
