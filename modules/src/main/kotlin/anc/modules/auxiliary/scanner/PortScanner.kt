package anc.modules.auxiliary.scanner

import anc.core.ModuleType
import anc.core.Rank
import anc.core.auxiliary.Auxiliary
import anc.core.optional
import anc.core.required
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import rex.arch.Platform
import rex.socket.TcpSocket

class PortScanner : Auxiliary() {
    override val name = "TCP Port Scanner"
    override val description = "Scans a range of TCP ports on one or more hosts"
    override val modulePath = "scanner/portscan/tcp"
    override val rank = Rank.NORMAL
    override val authors = listOf("AncKit")
    override val platform = listOf(Platform.ANY)

    override val options = anc.core.Options().apply {
        required<String>("RHOSTS",  "Target host(s), e.g. 192.168.1.1 or 192.168.1.0/24")
        required<String>("PORTS",   "Port range, e.g. 1-1024 or 80,443,8080", "1-1024")
        optional<Int>   ("THREADS", "Number of concurrent scan threads", 100)
        optional<Int>   ("TIMEOUT", "Connect timeout per port in ms", 1000)
    }

    override suspend fun run() {
        datastore.applyDefaults(options)
        val hosts = parseHosts(datastore.getString("RHOSTS"))
        val ports = parsePorts(datastore.getString("PORTS"))
        val timeout = datastore.getInt("TIMEOUT", 1000)
        val threads = datastore.getInt("THREADS", 100)

        printStatus("Scanning ${hosts.size} host(s), ${ports.size} port(s) each...")

        coroutineScope {
            hosts.forEach { host ->
                ports.chunked(threads).forEach { chunk ->
                    chunk.map { port ->
                        async {
                            if (TcpSocket.isOpen(host, port, timeout)) {
                                printGood("$host:$port open")
                            }
                        }
                    }.awaitAll()
                }
            }
        }
        printStatus("Scan complete.")
    }

    private fun parseHosts(raw: String): List<String> {
        return raw.split(",", " ").map { it.trim() }.filter { it.isNotBlank() }.flatMap { token ->
            if (token.contains("/")) expandCidr(token) else listOf(token)
        }
    }

    private fun parsePorts(raw: String): List<Int> = buildList {
        raw.split(",").forEach { part ->
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val (start, end) = trimmed.split("-").map { it.trim().toInt() }
                addAll(start..end)
            } else {
                trimmed.toIntOrNull()?.let { add(it) }
            }
        }
    }

    private fun expandCidr(cidr: String): List<String> {
        // Basic /24 CIDR expansion for demonstration
        val (base, prefix) = cidr.split("/")
        if (prefix.toInt() != 24) return listOf(base)
        val parts = base.split(".").take(3).joinToString(".")
        return (1..254).map { "$parts.$it" }
    }
}
