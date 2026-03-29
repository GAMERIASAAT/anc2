package anc.core.nop

import anc.core.AncModule
import anc.core.ModuleType

abstract class Nop : AncModule() {
    override val moduleType = ModuleType.NOP
    override suspend fun run() {} // NOPs don't run standalone

    /** Generate [length] bytes of NOP sled. */
    abstract fun generate(length: Int, badChars: ByteArray = byteArrayOf()): ByteArray
}
