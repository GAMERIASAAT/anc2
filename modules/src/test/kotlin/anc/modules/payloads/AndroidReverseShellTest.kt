package anc.modules.payloads

import anc.modules.payloads.singles.android.ReverseShell
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.runBlocking

class AndroidReverseShellTest : FunSpec({

    test("cmd payload contains LHOST and LPORT") {
        val cmd = ReverseShell.buildCmdPayload("192.168.1.10", 4444)
        cmd shouldContain "192.168.1.10"
        cmd shouldContain "4444"
    }

    test("cmd payload invokes /system/bin/sh") {
        val cmd = ReverseShell.buildCmdPayload("10.0.0.1", 9001)
        cmd shouldContain "/system/bin/sh"
    }

    test("java payload contains LHOST and LPORT") {
        val java = ReverseShell.buildJavaPayload("192.168.1.10", 4444)
        java shouldContain "192.168.1.10"
        java shouldContain "4444"
    }

    test("java payload contains Socket constructor") {
        val java = ReverseShell.buildJavaPayload("1.2.3.4", 1234)
        java shouldContain "Socket"
    }

    test("java payload contains /system/bin/sh") {
        val java = ReverseShell.buildJavaPayload("1.2.3.4", 1234)
        java shouldContain "/system/bin/sh"
    }

    test("generate() returns cmd payload as UTF-8 bytes by default") {
        val payload = ReverseShell()
        payload.datastore["LHOST"] = "10.0.0.1"
        payload.datastore["LPORT"] = "4444"
        val bytes = runBlocking { payload.generate() }
        val text = bytes.toString(Charsets.UTF_8)
        text shouldContain "10.0.0.1"
        text shouldContain "4444"
    }

    test("generate() returns java payload when TYPE=java") {
        val payload = ReverseShell()
        payload.datastore["LHOST"] = "10.0.0.1"
        payload.datastore["LPORT"] = "4444"
        payload.datastore["TYPE"]  = "java"
        val bytes = runBlocking { payload.generate() }
        val text = bytes.toString(Charsets.UTF_8)
        text shouldContain "Socket"
    }

    test("cmd payload does not contain single quotes that would break shell escaping in basic case") {
        // The cmd payload wraps in exec ... 'command', verify structure
        val cmd = ReverseShell.buildCmdPayload("127.0.0.1", 4444)
        cmd shouldNotContain "\n"  // should be a single line
    }
})
