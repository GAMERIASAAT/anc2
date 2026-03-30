package rex.socket

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class UdpSocketTest : FunSpec({

    test("open assigns a local port") {
        UdpSocket.open().use { sock ->
            sock.localPort shouldNotBe 0
        }
    }

    test("open with specific port binds to that port") {
        val port = 19876
        UdpSocket.open(localPort = port).use { sock ->
            sock.localPort shouldBe port
        }
    }

    test("isClosed returns true after close") {
        val sock = UdpSocket.open()
        sock.isClosed shouldBe false
        sock.close()
        sock.isClosed shouldBe true
    }

    test("loopback send and receive") {
        val server = UdpSocket.open(timeoutMs = 2_000)
        val serverPort = server.localPort
        val message = "ping".toByteArray()

        // Launch server receive in background
        val job = kotlinx.coroutines.GlobalScope.launch {
            val (data, _) = server.receive()
            data.toString(Charsets.UTF_8) shouldBe "ping"
            server.close()
        }

        UdpSocket.sendOnce(message, "127.0.0.1", serverPort)
        job.join()
    }

    test("sendReceive loopback echo") {
        // Spin up a simple echo server
        val echoServer = UdpSocket.open(timeoutMs = 3_000)
        val echoPort = echoServer.localPort

        val echoJob = kotlinx.coroutines.GlobalScope.launch {
            val (data, sender) = echoServer.receive()
            echoServer.send(data, sender.address.hostAddress ?: "127.0.0.1", sender.port)
            echoServer.close()
        }

        val response = UdpSocket.sendReceive(
            data = "hello udp".toByteArray(),
            host = "127.0.0.1",
            port = echoPort,
            timeoutMs = 3_000
        )

        response.toString(Charsets.UTF_8) shouldBe "hello udp"
        echoJob.join()
    }

    test("receive with timeout throws SocketTimeoutException") {
        val sock = UdpSocket.open(timeoutMs = 100)
        val result = withTimeoutOrNull(500) {
            runCatching { sock.receive() }.exceptionOrNull()
        }
        result shouldNotBe null
        sock.close()
    }
})
