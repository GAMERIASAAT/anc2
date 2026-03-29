package anc.core.session

import anc.base.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import rex.socket.TcpSocket
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

/**
 * Phantom Agent session — uses a custom TLV (Type-Length-Value) binary protocol.
 *
 * Packet format (big-endian):
 *   [4 bytes: type] [4 bytes: length] [N bytes: value]
 *
 * Types:
 *   0x00000001  COMMAND_EXEC   — run a shell command, value = UTF-8 string
 *   0x00000002  COMMAND_RESULT — response from agent, value = UTF-8 string
 *   0x00000003  FILE_READ      — read a remote file path
 *   0x00000004  FILE_DATA      — file bytes response
 *   0x00000005  SYSINFO        — request system info (empty value)
 *   0x00000006  SYSINFO_RESULT — system info response, value = JSON UTF-8
 *   0xFFFFFFFF  CLOSE          — graceful close
 */
class PhantomSession(
    private val socket: TcpSocket,
    override val remoteAddress: String
) : Session() {
    override val type = "phantom"
    override val isAlive: Boolean get() = socket.isConnected

    private val _output = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 256)
    override val output: SharedFlow<String> = _output.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val din = DataInputStream(socket.inputStream)
    private val dout = DataOutputStream(socket.outputStream)

    companion object {
        const val TYPE_COMMAND_EXEC   = 0x00000001
        const val TYPE_COMMAND_RESULT = 0x00000002
        const val TYPE_FILE_READ      = 0x00000003
        const val TYPE_FILE_DATA      = 0x00000004
        const val TYPE_SYSINFO        = 0x00000005
        const val TYPE_SYSINFO_RESULT = 0x00000006
        const val TYPE_CLOSE          = -1 // 0xFFFFFFFF as signed int
    }

    init {
        scope.launch {
            try {
                while (socket.isConnected) {
                    val pktType = din.readInt()
                    val pktLen  = din.readInt()
                    val value   = ByteArray(pktLen).also { din.readFully(it) }
                    when (pktType) {
                        TYPE_COMMAND_RESULT, TYPE_SYSINFO_RESULT ->
                            _output.emit(String(value, Charsets.UTF_8))
                        TYPE_FILE_DATA ->
                            _output.emit("[file data: ${value.size} bytes]")
                        TYPE_CLOSE -> {
                            _output.emit("[phantom: agent closed connection]")
                            break
                        }
                        else -> Logging.d("phantom: unknown packet type 0x%08x".format(pktType))
                    }
                }
            } catch (e: Exception) {
                Logging.d("phantom read loop ended: ${e.message}")
            }
        }
    }

    private fun writePacket(type: Int, value: ByteArray) {
        val buf = ByteBuffer.allocate(8 + value.size)
        buf.putInt(type)
        buf.putInt(value.size)
        buf.put(value)
        dout.write(buf.array())
        dout.flush()
    }

    override suspend fun send(command: String) {
        writePacket(TYPE_COMMAND_EXEC, command.toByteArray(Charsets.UTF_8))
    }

    override suspend fun execute(command: String, timeoutMs: Long): String {
        val result = StringBuilder()
        val job = scope.launch {
            output.collect { line -> result.appendLine(line) }
        }
        send(command)
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            kotlinx.coroutines.delay(timeoutMs)
        }
        job.cancel()
        return result.toString().trimEnd()
    }

    fun requestSysinfo() = writePacket(TYPE_SYSINFO, ByteArray(0))

    fun readFile(remotePath: String) =
        writePacket(TYPE_FILE_READ, remotePath.toByteArray(Charsets.UTF_8))

    override fun close() {
        runCatching { writePacket(TYPE_CLOSE, ByteArray(0)) }
        socket.close()
    }
}
