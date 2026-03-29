package anc.modules.auxiliary.scanner

import anc.core.Rank
import anc.core.auxiliary.Auxiliary
import anc.core.required
import anc.core.optional
import rex.arch.Platform
import rex.proto.Http

class HttpVersion : Auxiliary() {
    override val name = "HTTP Version Scanner"
    override val description = "Grabs the Server banner from one or more HTTP/HTTPS endpoints"
    override val modulePath = "scanner/http/http_version"
    override val rank = Rank.NORMAL
    override val authors = listOf("AncKit")
    override val platform = listOf(Platform.ANY)

    override val options = anc.core.Options().apply {
        required<String>("RHOSTS",  "Target host(s)")
        optional<Int>   ("RPORT",   "Target port", 80)
        optional<Boolean>("SSL",    "Use HTTPS", false)
        optional<String>("VHOST",   "HTTP Host header override", "")
        optional<Int>   ("TIMEOUT", "Timeout in ms", 8000)
    }

    override suspend fun run() {
        datastore.applyDefaults(options)
        val hosts = datastore.getString("RHOSTS").split(",", " ").map { it.trim() }.filter { it.isNotBlank() }
        val port = datastore.getInt("RPORT", 80)
        val ssl = datastore.getBoolean("SSL")
        val timeout = datastore.getLong("TIMEOUT", 8000)
        val vhost = datastore.getString("VHOST")

        hosts.forEach { host ->
            try {
                val scheme = if (ssl) "https" else "http"
                val url = "$scheme://$host:$port/"
                val headers = buildMap<String, String> {
                    if (vhost.isNotBlank()) put("Host", vhost)
                }
                val resp = Http.get(url, headers = headers, timeoutMs = timeout, followRedirects = false)
                val server = resp.headers["Server"] ?: resp.headers["server"] ?: "unknown"
                printGood("$host:$port (${resp.code}) Server: $server")
            } catch (e: Exception) {
                printBad("$host:$port — ${e.message}")
            }
        }
    }
}
