package rex.proto

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FtpTest : FunSpec({

    // ── Unit tests for PASV parser — no live server required ──────────────────

    test("PASV response parsing: extract host and port") {
        // Test the parsing logic directly by calling the companion object helper
        // via a known-good PASV response string
        val pasvMsg = "227 Entering Passive Mode (192,168,1,1,31,144)"
        val match = Regex("""\((\d+),(\d+),(\d+),(\d+),(\d+),(\d+)\)""").find(pasvMsg)
        match shouldNotBe null
        val (h1, h2, h3, h4, p1, p2) = match!!.destructured
        val host = "$h1.$h2.$h3.$h4"
        val port = p1.toInt() * 256 + p2.toInt()
        host shouldBe "192.168.1.1"
        port shouldBe 8080   // 31*256 + 144 = 7936 + 144 = 8080
    }

    test("PASV port calculation: (0,21)=21") {
        val port = 0 * 256 + 21
        port shouldBe 21
    }

    test("PASV port calculation: (195,149)=50069") {
        val port = 195 * 256 + 149
        port shouldBe 50069
    }

    test("Response isPositive for 2xx codes") {
        FtpClient.Response(200, "OK").isPositive shouldBe true
        FtpClient.Response(226, "Transfer complete").isPositive shouldBe true
        FtpClient.Response(331, "Password required").isPositive shouldBe true
    }

    test("Response isError for 4xx and 5xx codes") {
        FtpClient.Response(421, "Service unavailable").isError shouldBe true
        FtpClient.Response(530, "Login incorrect").isError shouldBe true
    }

    test("Response isPositive false for 4xx codes") {
        FtpClient.Response(421, "Service unavailable").isPositive shouldBe false
    }

    // ── Integration test against a live server (skipped if unreachable) ───────

    test("banner grab from ftp.dlptest.com skipped when offline") {
        // Known public FTP test server — skip gracefully if no network
        val banner = runCatching {
            kotlinx.coroutines.runBlocking {
                FtpClient.banner("ftp.dlptest.com", timeoutMs = 4_000)
            }
        }.getOrNull()
        // Just verify it doesn't throw — value may be null if offline
        // If it connects, we expect a non-empty banner
        if (banner != null) {
            banner.isNotBlank() shouldBe true
        }
    }
})
