package rex.crypto

import org.bouncycastle.crypto.StreamCipher
import org.bouncycastle.crypto.engines.RC4Engine
import org.bouncycastle.crypto.params.KeyParameter

/**
 * RC4 (ARCFOUR) stream cipher using Bouncy Castle.
 * Same key stream is used for both encrypt and decrypt.
 */
object Rc4 {

    /**
     * Encrypt or decrypt [data] with [key].
     * RC4 is symmetric — call crypt() for both directions.
     */
    fun crypt(data: ByteArray, key: ByteArray): ByteArray {
        require(key.isNotEmpty()) { "RC4 key must not be empty" }
        val engine: StreamCipher = RC4Engine()
        engine.init(true, KeyParameter(key))
        val out = ByteArray(data.size)
        engine.processBytes(data, 0, data.size, out, 0)
        return out
    }

    /** Convenience: XOR a hex-encoded key string against [data]. */
    fun crypt(data: ByteArray, hexKey: String): ByteArray =
        crypt(data, hexKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray())

    /**
     * Returns a stateful encryptor/decryptor that maintains key-stream position
     * across multiple calls — useful for streaming data.
     */
    fun stateful(key: ByteArray): StatefulRc4 = StatefulRc4(key)

    class StatefulRc4(key: ByteArray) {
        private val engine: StreamCipher = RC4Engine().also {
            it.init(true, KeyParameter(key))
        }

        fun process(data: ByteArray): ByteArray {
            val out = ByteArray(data.size)
            engine.processBytes(data, 0, data.size, out, 0)
            return out
        }

        fun reset(key: ByteArray) = engine.init(true, KeyParameter(key))
    }
}
