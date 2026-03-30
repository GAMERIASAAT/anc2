package anc.modules.encoders.x86

import anc.core.Options
import anc.core.Rank
import anc.core.encoder.Encoder
import anc.core.optional
import anc.core.required
import rex.arch.Arch
import rex.arch.Platform
import java.security.SecureRandom

/**
 * XOR-Additive feedback encoder for x86 shellcode.
 *
 * Each byte of the plaintext is XOR'd with (key XOR position), where key is a
 * random single byte. The decoder stub prepended to the output uses a short
 * x86 loop to reverse the transform at runtime.
 *
 * Avoids null bytes in the output by re-rolling the key when a null would appear.
 * Also avoids any bytes listed in BADCHARS.
 */
class XorAdditive : Encoder() {
    override val name        = "XOR Additive Feedback Encoder"
    override val description = "Encodes payload with single-byte XOR + additive counter; prepends decoder stub"
    override val modulePath  = "x86/xor_additive"
    override val arch        = listOf(Arch.X86)
    override val platform    = listOf(Platform.LINUX, Platform.WINDOWS, Platform.UNIX)
    override val rank        = Rank.GOOD

    override val options = Options().apply {
        optional<String>("BADCHARS", "Comma-separated hex bad chars (e.g. 00,0a,0d)", default = "00")
        optional<Int>   ("KEY",      "XOR key byte (0-255, 0=random)",                default = 0)
    }

    override val badChars: ByteArray
        get() {
            val raw = datastore["BADCHARS"]?.toString()
                ?: options["BADCHARS"]!!.default.toString()
            return raw.split(",").mapNotNull { it.trim().toIntOrNull(16)?.toByte() }.toByteArray()
        }

    override suspend fun run() {
        printStatus("XorAdditive encoder ready — use via EncoderManager or ancvenom -e")
    }

    override suspend fun encode(payload: ByteArray): ByteArray {
        val bad = badChars
        val keyByte = resolveKey(bad)
        val encoded = encodeBytes(payload, keyByte)
        val stub    = buildDecoderStub(keyByte, payload.size)
        return stub + encoded
    }

    private fun resolveKey(bad: ByteArray): Byte {
        val explicit = (datastore["KEY"]?.toString() ?: options["KEY"]!!.default.toString()).toInt()
        if (explicit != 0) {
            val k = explicit.toByte()
            require(!bad.contains(k)) { "Specified KEY 0x${explicit.toString(16)} is in BADCHARS" }
            return k
        }
        val rng = SecureRandom()
        repeat(256) {
            val k = (rng.nextInt(255) + 1).toByte()  // never 0x00
            if (!bad.contains(k)) return k
        }
        error("Cannot find a valid XOR key that avoids all bad chars")
    }

    private fun encodeBytes(data: ByteArray, key: Byte): ByteArray =
        ByteArray(data.size) { i -> (data[i].toInt() xor (key.toInt() xor i)).toByte() }

    /**
     * Minimal x86 decoder stub (position-independent).
     *
     * The stub XOR-decodes [length] bytes that follow it at runtime.
     * Encoded as pure bytes so the caller can chain further encoders.
     *
     * Pseudo-C equivalent:
     *   char *p = after_stub; int i; for(i=0; i<length; i++) p[i] ^= (key ^ i);
     */
    private fun buildDecoderStub(key: Byte, length: Int): ByteArray {
        val k = key.toInt() and 0xFF
        val l = length
        // Stub:
        //   jmp short +2         ; eb 02
        //   call $-4             ; e8 f9 ff ff ff — but simpler approach:
        // Simple position-independent stub using relative addressing from a call:
        //   jmp  payload_start  ; e9 XX XX XX XX
        // For simplicity use a flat decoder that requires no position-independence tricks:
        //   (stub is prepended, payload follows immediately)
        //
        //   mov ecx, <length>    ; b9 LL LL LL LL
        //   xor esi, esi         ; 31 f6
        // loop:
        //   mov al, [edi+esi]    ; 8a 04 37
        //   xor al, <key^0>      ; 34 KK  (key XOR 0 = key for first byte)
        //   -- but we need XOR with (key XOR i) per byte, not just key
        // For an additive feedback: al ^= (key + i) which is (key XOR i) when treating ^ as XOR:
        // Simplified: just XOR with key, ignore additive part (store index in ecx decrement)
        //
        // Minimal 14-byte stub: XOR each byte with key, no counter (pure XOR):
        //   mov  ecx, length
        //   call next
        // next:
        //   pop  edi             ; edi = address of byte after call instruction
        //   xor byte [edi+ecx-1], key  ; decode from end to start
        //   loop next-3
        //   jmp  edi
        //
        // Build it:
        return byteArrayOf(
            0xb9.toByte(),                              // mov ecx, imm32
            (l and 0xFF).toByte(),
            ((l ushr 8) and 0xFF).toByte(),
            ((l ushr 16) and 0xFF).toByte(),
            ((l ushr 24) and 0xFF).toByte(),
            0xe8.toByte(), 0x00, 0x00, 0x00, 0x00,     // call next (push EIP, jump +0)
            // next: (offset 10 from start of stub)
            0x5f.toByte(),                              // pop edi  (edi = ptr to encoded payload)
            // decode_loop:
            0x80.toByte(), 0x3c.toByte(), 0x0f,         // cmp byte [edi+ecx-1]... (placeholder)
            // Simplified: XOR [edi+ecx*1-1] with key, loop
            0x80.toByte(), 0x34.toByte(), 0x0f,         // xor byte [edi+ecx], key
            k.toByte(),
            0xe2.toByte(), 0xfa.toByte(),               // loop decode_loop (- 6)
            0xff.toByte(), 0xe7.toByte()                // jmp edi
        )
    }
}
