package rex.proto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking

class SshTest : FunSpec({

    test("banner grab from closed port returns null gracefully") {
        // Port 22222 should not be listening — must not throw, just return null
        val result = runBlocking {
            SshClient.banner("127.0.0.1", port = 22222, timeoutMs = 500)
        }
        result shouldBe null
    }

    test("connectPassword to closed port throws or returns null") {
        val result = runCatching {
            runBlocking {
                SshClient.connectPassword(
                    host = "127.0.0.1",
                    port = 22222,
                    username = "user",
                    password = "pass",
                    timeoutMs = 500
                )
            }
        }
        result.isFailure shouldBe true
    }

    test("banner grab from local SSH if available") {
        // If sshd is running on port 22, grab its banner
        val banner = runCatching {
            runBlocking { SshClient.banner("127.0.0.1", port = 22, timeoutMs = 2_000) }
        }.getOrNull()

        // Either null (sshd not running) or a non-empty SSH version string
        if (banner != null) {
            banner.startsWith("SSH-") shouldBe true
        }
    }
})
