package anc.core.payload

import anc.core.AncModule
import anc.core.Datastore
import anc.core.ModuleType
import rex.arch.Arch
import rex.arch.Platform

sealed class PayloadType {
    object Single : PayloadType()
    object Stager : PayloadType()
    object Stage : PayloadType()
    object Adapter : PayloadType()
}

abstract class Payload : AncModule() {
    override val moduleType = ModuleType.PAYLOAD
    abstract val payloadType: PayloadType

    /** Generate the raw payload bytes from the current datastore options. */
    abstract suspend fun generate(): ByteArray

    /** Format as a byte array literal for embedding in source code. */
    fun toByteArrayLiteral(bytes: ByteArray): String =
        bytes.joinToString(", ", "byteArrayOf(", ")") { "0x%02x".format(it.toInt() and 0xFF) }
}
