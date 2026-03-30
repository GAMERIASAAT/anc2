package rex.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

/**
 * SHA-256 and HMAC-SHA-256 using the JVM's built-in JCA provider
 * (available on Android API 26+ and all JVM platforms).
 */
object Sha256 {

    /** Returns the raw 32-byte SHA-256 digest of [data]. */
    fun digest(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    /** Returns the SHA-256 digest of [data] as a lowercase hex string. */
    fun hex(data: ByteArray): String = digest(data).joinToString("") { "%02x".format(it) }

    /** Returns the SHA-256 digest of a UTF-8 string as a lowercase hex string. */
    fun hex(data: String): String = hex(data.toByteArray(Charsets.UTF_8))

    /** Returns the raw 32-byte HMAC-SHA-256 of [data] using [key]. */
    fun hmac(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    /** Returns HMAC-SHA-256 as a lowercase hex string. */
    fun hmacHex(data: ByteArray, key: ByteArray): String =
        hmac(data, key).joinToString("") { "%02x".format(it) }

    /** Constant-time equality check to prevent timing attacks. */
    fun verify(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
