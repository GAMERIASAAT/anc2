package rex.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-CBC encrypt/decrypt using Bouncy Castle (works on JVM and Android).
 */
object Aes {
    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    data class CipherResult(val iv: ByteArray, val ciphertext: ByteArray) {
        fun toBytes(): ByteArray = iv + ciphertext
    }

    fun generateKey(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }

    fun encrypt(plaintext: ByteArray, key: ByteArray): CipherResult {
        require(key.size == 32) { "AES-256 requires a 32-byte key" }
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return CipherResult(iv, cipher.doFinal(plaintext))
    }

    fun decrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(key.size == 32) { "AES-256 requires a 32-byte key" }
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(ciphertext)
    }

    /** Convenience: decrypt from a combined [iv + ciphertext] byte array. */
    fun decrypt(combined: ByteArray, key: ByteArray): ByteArray =
        decrypt(combined.drop(16).toByteArray(), key, combined.take(16).toByteArray())
}
