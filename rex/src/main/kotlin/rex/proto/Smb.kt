package rex.proto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rex.socket.TcpSocket
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * SMB protocol primitives — NetBIOS session + SMBv1 negotiate.
 *
 * Useful for banner grabbing and basic recon:
 * - Identify SMB dialect and server OS/version
 * - Detect SMBv1 (EternalBlue prerequisite)
 * - Grab domain/workgroup information
 */
object Smb {

    const val DEFAULT_PORT = 445
    const val NETBIOS_PORT = 139

    data class NegotiateResult(
        val dialect: String,
        val osVersion: String,
        val serverName: String,
        val domainName: String,
        val smbV1Supported: Boolean,
        val signingRequired: Boolean
    )

    /** Connect to [host]:[port], send an SMBv1 Negotiate, return parsed result. */
    suspend fun negotiate(
        host: String,
        port: Int = DEFAULT_PORT,
        timeoutMs: Int = 10_000
    ): NegotiateResult? = withContext(Dispatchers.IO) {
        runCatching {
            val sock = TcpSocket.connect(host, port, timeoutMs)
            try {
                sock.send(buildNegotiateRequest())
                val resp = receiveNetBiosFrame(sock)
                parseNegotiateResponse(resp)
            } finally {
                sock.close()
            }
        }.getOrNull()
    }

    /** Returns true if the target responds to SMB negotiate on port 445. */
    suspend fun isOpen(host: String, timeoutMs: Int = 3_000): Boolean =
        negotiate(host, DEFAULT_PORT, timeoutMs) != null

    // ── Packet builders ────────────────────────────────────────────────────────

    /**
     * Build a minimal SMBv1 Negotiate Protocol Request.
     * Advertises both NT LM 0.12 (SMBv1) and SMB 2.002 so we can fingerprint
     * whether the server downgrades to v1.
     */
    fun buildNegotiateRequest(): ByteArray {
        val dialects = listOf("PC NETWORK PROGRAM 1.0", "LANMAN1.0", "NT LM 0.12", "SMB 2.002")
        val dialectBytes = dialects.flatMap { d ->
            listOf(0x02.toByte()) + d.toByteArray(Charsets.US_ASCII).toList() + listOf(0x00.toByte())
        }.toByteArray()

        // SMBv1 header (32 bytes) + Negotiate params
        val smb = ByteBuffer.allocate(32 + 3 + dialectBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        // Protocol magic
        smb.put(byteArrayOf(0xFF.toByte(), 0x53, 0x4D, 0x42))
        smb.put(0x72)                          // Command: Negotiate
        smb.putInt(0)                          // NT Status
        smb.put(0x18)                          // Flags
        smb.putShort(0x0001)                   // Flags2: unicode
        smb.putShort(0)                        // PID high
        repeat(8) { smb.put(0) }               // Signature
        smb.putShort(0)                        // Reserved
        smb.putShort(0)                        // TreeID
        smb.putShort(0xFF.toShort())           // ProcessID
        smb.putShort(0)                        // UserID
        smb.putShort(0)                        // MultiplexID
        // Parameters: WordCount=0
        smb.put(0)
        // Byte count + dialect buffer
        smb.putShort(dialectBytes.size.toShort())
        smb.put(dialectBytes)

        val payload = smb.array()

        // NetBIOS Session Message wrapper (4-byte header)
        val nb = ByteBuffer.allocate(4 + payload.size).order(ByteOrder.BIG_ENDIAN)
        nb.put(0x00)                           // Session message type
        nb.put(0x00)
        nb.putShort(payload.size.toShort())
        nb.put(payload)
        return nb.array()
    }

    // ── Response parsers ───────────────────────────────────────────────────────

    private suspend fun receiveNetBiosFrame(sock: TcpSocket): ByteArray {
        val header = readExact(sock, 4)
        val length = ((header[1].toInt() and 0xFF) shl 16) or
                     ((header[2].toInt() and 0xFF) shl 8)  or
                      (header[3].toInt() and 0xFF)
        return readExact(sock, length)
    }

    private suspend fun readExact(sock: TcpSocket, n: Int): ByteArray {
        val buf = ByteArray(n)
        var offset = 0
        while (offset < n) {
            val chunk = sock.receive(n - offset)
            if (chunk.isEmpty()) break
            chunk.copyInto(buf, offset)
            offset += chunk.size
        }
        return buf
    }

    private fun parseNegotiateResponse(data: ByteArray): NegotiateResult {
        if (data.size < 32) return emptyResult()
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        // Verify SMB magic
        val magic = ByteArray(4).also { buf.get(it) }
        val isSmb = magic.contentEquals(byteArrayOf(0xFF.toByte(), 0x53, 0x4D, 0x42))
        val isSm2 = magic.contentEquals(byteArrayOf(0xFE.toByte(), 0x53, 0x4D, 0x42))

        if (isSm2) {
            // Server responded with SMBv2 — v1 not supported
            return NegotiateResult(
                dialect = "SMB 2.x",
                osVersion = "",
                serverName = "",
                domainName = "",
                smbV1Supported = false,
                signingRequired = false
            )
        }

        if (!isSmb || data.size < 36) return emptyResult()

        // Skip to SecurityMode at offset 39 in SMBv1 negotiate response
        val signingRequired = if (data.size > 39) (data[39].toInt() and 0x08) != 0 else false

        // Dialect index at byte 33-34 (word)
        val dialectIndex = if (data.size > 35) {
            ((data[34].toInt() and 0xFF) shl 8) or (data[33].toInt() and 0xFF)
        } else -1

        val dialect = when (dialectIndex) {
            0    -> "PC NETWORK PROGRAM 1.0"
            1    -> "LANMAN1.0"
            2    -> "NT LM 0.12"
            3    -> "SMB 2.002"
            else -> "Unknown ($dialectIndex)"
        }

        return NegotiateResult(
            dialect = dialect,
            osVersion = extractString(data, 0x40),
            serverName = "",
            domainName = "",
            smbV1Supported = dialectIndex == 2,
            signingRequired = signingRequired
        )
    }

    private fun extractString(data: ByteArray, offset: Int): String {
        if (offset >= data.size) return ""
        return runCatching {
            var end = offset
            while (end + 1 < data.size && !(data[end] == 0.toByte() && data[end + 1] == 0.toByte())) end++
            String(data, offset, end - offset, Charsets.UTF_16LE)
        }.getOrDefault("")
    }

    private fun emptyResult() = NegotiateResult("", "", "", "", smbV1Supported = false, signingRequired = false)
}
