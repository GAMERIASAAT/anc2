package rex.proto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rex.socket.TcpSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Minimal FTP client built on top of [TcpSocket].
 * Supports anonymous and authenticated login, PASV data transfers,
 * and basic recon commands (SYST, PWD, LIST, STAT).
 */
class FtpClient private constructor(private val ctrl: TcpSocket) : AutoCloseable {

    data class Response(val code: Int, val message: String) {
        val isPositive: Boolean get() = code in 100..399
        val isError: Boolean get() = code >= 400
    }

    // ── low-level ─────────────────────────────────────────────────────────────

    private suspend fun readResponse(): Response {
        val sb = StringBuilder()
        var line: String
        do {
            line = ctrl.readLine()
            sb.appendLine(line)
        } while (line.length > 3 && line[3] == '-')   // multi-line responses (RFC 959 §4.2)
        val code = line.take(3).toIntOrNull() ?: -1
        return Response(code, sb.trim().toString())
    }

    private suspend fun sendCmd(cmd: String): Response {
        ctrl.send("$cmd\r\n".toByteArray())
        return readResponse()
    }

    // ── public API ─────────────────────────────────────────────────────────────

    /** The banner returned immediately after connecting. */
    var banner: String = ""
        private set

    /** Login with username and password (use "anonymous" / "" for anonymous). */
    suspend fun login(user: String = "anonymous", pass: String = "anonymous@"): Response {
        sendCmd("USER $user")
        return sendCmd("PASS $pass")
    }

    /** Send SYST — returns server OS/type string. */
    suspend fun syst(): Response = sendCmd("SYST")

    /** Send PWD — returns current working directory. */
    suspend fun pwd(): Response = sendCmd("PWD")

    /** Change remote directory. */
    suspend fun cwd(dir: String): Response = sendCmd("CWD $dir")

    /** List remote directory contents in PASV mode. Returns raw listing string. */
    suspend fun list(path: String = ""): Pair<Response, String> {
        val dataConn = openPasv() ?: return sendCmd("LIST $path".trim()) to ""
        val cmdResp = sendCmd("LIST $path".trim())
        val data = withContext(Dispatchers.IO) {
            dataConn.use { s -> s.getInputStream().readBytes().toString(Charsets.UTF_8) }
        }
        val finalResp = readResponse()  // 226 Transfer complete
        return finalResp to data
    }

    /** STAT — server status / file info. */
    suspend fun stat(path: String = ""): Response =
        sendCmd(if (path.isBlank()) "STAT" else "STAT $path")

    /** Gracefully quit. */
    suspend fun quit(): Response = sendCmd("QUIT")

    // ── PASV helper ───────────────────────────────────────────────────────────

    private suspend fun openPasv(): Socket? {
        val resp = sendCmd("PASV")
        if (resp.isError) return null
        // Parse (h1,h2,h3,h4,p1,p2) from response message
        val match = Regex("""\((\d+),(\d+),(\d+),(\d+),(\d+),(\d+)\)""").find(resp.message)
            ?: return null
        val (h1, h2, h3, h4, p1, p2) = match.destructured
        val host = "$h1.$h2.$h3.$h4"
        val port = p1.toInt() * 256 + p2.toInt()
        return runCatching {
            withContext(Dispatchers.IO) {
                Socket().also { it.connect(InetSocketAddress(host, port), 10_000) }
            }
        }.getOrNull()
    }

    override fun close() {
        ctrl.close()
    }

    companion object {
        suspend fun connect(host: String, port: Int = 21, timeoutMs: Int = 10_000): FtpClient {
            val sock = TcpSocket.connect(host, port, timeoutMs)
            val client = FtpClient(sock)
            client.banner = client.readResponse().message
            return client
        }

        /** Grab just the FTP banner without authenticating. */
        suspend fun banner(host: String, port: Int = 21, timeoutMs: Int = 5_000): String? =
            runCatching {
                connect(host, port, timeoutMs).use { it.banner }
            }.getOrNull()
    }
}
