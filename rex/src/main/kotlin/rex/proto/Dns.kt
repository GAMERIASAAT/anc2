package rex.proto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * DNS resolution helpers using the platform's built-in resolver
 * (java.net.InetAddress — available on Android API 26+).
 *
 * For raw DNS queries (zone transfer, MX records etc.) use [RawDns].
 */
object Dns {

    /** Resolve a hostname to all of its IP addresses. Returns empty list on failure. */
    suspend fun resolveAll(hostname: String): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            InetAddress.getAllByName(hostname).map { it.hostAddress ?: "" }.filter { it.isNotEmpty() }
        }.getOrDefault(emptyList())
    }

    /** Resolve a hostname to its first IP address, or null if unresolvable. */
    suspend fun resolve(hostname: String): String? = resolveAll(hostname).firstOrNull()

    /** Reverse-lookup an IP address to its hostname, or null on failure. */
    suspend fun reverseLookup(ip: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val addr = InetAddress.getByName(ip)
            // getCanonicalHostName triggers a PTR query
            val canonical = addr.canonicalHostName
            // If it just returned the IP back, treat it as no PTR record
            if (canonical == ip) null else canonical
        }.getOrNull()
    }

    /** Returns true if [hostname] resolves to at least one address. */
    suspend fun isResolvable(hostname: String): Boolean = resolve(hostname) != null

    /**
     * Attempt to determine whether an IP is within a private RFC-1918 /
     * loopback / link-local range.
     */
    fun isPrivate(ip: String): Boolean = runCatching {
        val addr = InetAddress.getByName(ip)
        addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress
    }.getOrDefault(false)

    /** Build a simple PTR query name from an IPv4 string (e.g. "1.2.3.4" → "4.3.2.1.in-addr.arpa"). */
    fun ptrName(ipv4: String): String =
        ipv4.split(".").reversed().joinToString(".") + ".in-addr.arpa"
}
