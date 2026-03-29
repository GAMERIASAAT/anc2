package anc.core.encoder

import anc.core.AncModule
import anc.core.ModuleType

abstract class Encoder : AncModule() {
    override val moduleType = ModuleType.ENCODER

    abstract val badChars: ByteArray

    /** Encode the raw payload, returning the encoded bytes. */
    abstract suspend fun encode(payload: ByteArray): ByteArray

    /** Verify the encoded payload contains none of the bad chars. */
    fun verify(encoded: ByteArray): Boolean =
        badChars.none { bad -> encoded.contains(bad) }
}
