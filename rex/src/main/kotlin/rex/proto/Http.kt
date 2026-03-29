package rex.proto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Coroutine-friendly HTTP helper backed by OkHttp.
 * OkHttp is available on Android API 21+ and works on JVM.
 */
object Http {
    data class Response(
        val code: Int,
        val body: String,
        val headers: Map<String, String>,
        val isSuccess: Boolean = code in 200..299
    )

    private fun buildClient(timeoutMs: Long, followRedirects: Boolean): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .followRedirects(followRedirects)
            .build()

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 10_000,
        followRedirects: Boolean = true
    ): Response = withContext(Dispatchers.IO) {
        val client = buildClient(timeoutMs, followRedirects)
        val req = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> addHeader(k, v) }
        }.build()
        client.newCall(req).execute().use { resp ->
            Response(
                code = resp.code,
                body = resp.body?.string() ?: "",
                headers = resp.headers.toMultimap().mapValues { (_, v) -> v.first() }
            )
        }
    }

    suspend fun post(
        url: String,
        body: String,
        contentType: String = "application/x-www-form-urlencoded",
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 10_000
    ): Response = withContext(Dispatchers.IO) {
        val client = buildClient(timeoutMs, true)
        val reqBody = body.toRequestBody(contentType.toMediaType())
        val req = Request.Builder().url(url).post(reqBody).apply {
            headers.forEach { (k, v) -> addHeader(k, v) }
        }.build()
        client.newCall(req).execute().use { resp ->
            Response(
                code = resp.code,
                body = resp.body?.string() ?: "",
                headers = resp.headers.toMultimap().mapValues { (_, v) -> v.first() }
            )
        }
    }

    suspend fun banner(host: String, port: Int, ssl: Boolean = false): String? = runCatching {
        val scheme = if (ssl) "https" else "http"
        val resp = get("$scheme://$host:$port/", timeoutMs = 5_000, followRedirects = false)
        resp.headers["Server"] ?: resp.headers["server"]
    }.getOrNull()
}
