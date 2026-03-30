package anc.core

import anc.core.encoder.Encoder

/**
 * Manages encoder modules and applies them to raw payload bytes.
 *
 * Supports single-pass and chained (multi-encoder) pipelines. Uses the
 * chain-of-responsibility pattern: each encoder in the chain transforms the
 * output of the previous one.
 */
class EncoderManager(private val framework: Framework) {

    /** All registered encoder modules as metadata. */
    fun all(): List<ModuleMetadata> =
        framework.moduleManager.byType(ModuleType.ENCODER)

    /**
     * Instantiate an encoder by its full path
     * (e.g. "encoders/x86/xor_additive") or short form ("x86/xor_additive").
     */
    fun create(path: String): Encoder? {
        val normalised = if (path.startsWith("encoders/")) path else "encoders/$path"
        return framework.moduleManager.create(normalised) as? Encoder
    }

    /**
     * Encode [raw] bytes using the encoder at [encoderPath].
     * Returns the encoded bytes, or the original bytes if the encoder is not found.
     */
    suspend fun encode(raw: ByteArray, encoderPath: String): ByteArray {
        val encoder = create(encoderPath) ?: return raw
        return encoder.encode(raw)
    }

    /**
     * Apply a chain of encoders in order.
     * Each encoder receives the output of the previous one.
     */
    suspend fun encodeChain(raw: ByteArray, encoderPaths: List<String>): ByteArray =
        encoderPaths.fold(raw) { bytes, path -> encode(bytes, path) }

    /**
     * Encode [raw] with the first encoder whose [Encoder.badChars] list does NOT
     * appear in [raw] and whose output passes [Encoder.verify].
     * Returns null if no suitable encoder is found.
     */
    suspend fun autoEncode(raw: ByteArray, badChars: ByteArray): ByteArray? {
        for (meta in all()) {
            val encoder = create(meta.fullName) ?: continue
            if (encoder.badChars.any { raw.contains(it) }) continue
            val encoded = encoder.encode(raw)
            if (encoder.verify(encoded)) return encoded
        }
        return null
    }

    val count: Int get() = all().size
}
