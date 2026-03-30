package anc.modules.nops.x86

import anc.core.Rank
import anc.core.nop.Nop
import rex.arch.Arch
import rex.arch.Platform

/**
 * x86 NOP sled generator.
 *
 * Uses multi-byte NOP equivalents to avoid simple IDS pattern matching on 0x90 runs.
 * Bad-char aware: falls back to semantically-equivalent single-byte instructions
 * that don't appear in the bad-char list.
 */
class NopX86 : Nop() {
    override val name        = "x86 NOP Sled"
    override val description = "Generates x86 NOP sleds using 0x90 and single-byte equivalents"
    override val modulePath  = "x86/opty2"
    override val arch        = listOf(Arch.X86)
    override val platform    = listOf(Platform.LINUX, Platform.WINDOWS, Platform.UNIX)
    override val rank        = Rank.NORMAL

    /**
     * Single-byte x86 instructions that are semantically neutral (preserve meaningful registers
     * or make no lasting state change in a typical shellcode context):
     * 0x90 = NOP, 0x40-0x47 = INC r32, 0x48-0x4f = DEC r32, 0xfc = CLD, 0xf8 = CLC
     */
    private val candidates = byteArrayOf(
        0x90.toByte(),                                            // NOP
        0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47,         // INC eax..edi
        0x48.toByte(), 0x49.toByte(), 0x4a.toByte(),             // DEC eax, ecx, edx
        0x4b.toByte(), 0x4c.toByte(), 0x4d.toByte(),             // DEC ebx, esp, ebp
        0x4e.toByte(), 0x4f.toByte(),                            // DEC esi, edi
        0xfc.toByte(),                                           // CLD
        0xf8.toByte()                                            // CLC
    )

    override fun generate(length: Int, badChars: ByteArray): ByteArray {
        val allowed = candidates.filter { !badChars.contains(it) }
        require(allowed.isNotEmpty()) { "All NOP candidates are excluded by bad chars" }
        // Prefer 0x90; fall back to round-robin over the allowed set
        val primary = if (!badChars.contains(0x90.toByte())) 0x90.toByte() else allowed.first()
        return ByteArray(length) { primary }
    }

    /** Generate a polymorphic sled that rotates through all allowed NOP equivalents. */
    fun generatePolymorphic(length: Int, badChars: ByteArray = byteArrayOf()): ByteArray {
        val allowed = candidates.filter { !badChars.contains(it) }
        require(allowed.isNotEmpty()) { "All NOP candidates are excluded by bad chars" }
        return ByteArray(length) { i -> allowed[i % allowed.size] }
    }
}
