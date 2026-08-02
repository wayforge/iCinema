package com.icinema.pages.player.core.hls

import android.util.Log
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class HlsProxyServer @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val manifestRewriter: HlsManifestRewriter,
    private val cache: HlsPersistentCache,
    private val adRuleStore: HlsAdRuleStore,
    private val prefetchCoordinator: HlsPrefetchCoordinator
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val recentResources = ArrayDeque<RecentHlsResource>()
    private var serverSocket: ServerSocket? = null
    private var port: Int = 0

    fun proxyUrl(
        originUrl: String,
        type: HlsProxyResourceType,
        target: HlsProxyTarget,
        parentPlaylistUrl: String? = null
    ): String {
        ensureStarted()
        val host = when (target) {
            HlsProxyTarget.Loopback -> LOOPBACK_HOST
            HlsProxyTarget.Lan -> localIpv4Address() ?: LOOPBACK_HOST
        }
        val path = when (type) {
            HlsProxyResourceType.Manifest -> MANIFEST_PATH
            HlsProxyResourceType.Resource -> RESOURCE_PATH
        }
        val query = buildString {
            append("u=").append(URLEncoder.encode(originUrl, Charsets.UTF_8.name()))
            if (!parentPlaylistUrl.isNullOrBlank()) {
                append("&p=").append(URLEncoder.encode(parentPlaylistUrl, Charsets.UTF_8.name()))
            }
        }
        return "http://$host:$port$path?$query"
    }

    fun resourceUrl(originUrl: String, target: HlsProxyTarget): String {
        return proxyUrl(
            originUrl = originUrl,
            type = HlsProxyResourceType.Resource,
            target = target
        )
    }

    fun markCurrentSegmentAsAd(
        playbackPositionMs: Long,
        videoTitle: String,
        episodeTitle: String
    ): Result<MarkedHlsAdSegment> {
        return runCatching {
            val target = resolveCurrentResource(playbackPositionMs)
                ?: throw IllegalStateException("还没有捕获到当前播放片段，请播放几秒后再标记")
            val rule = adRuleStore.upsertMarkedSegment(
                playlistUrl = target.playlistUrl,
                segmentUrl = target.url,
                durationSeconds = target.durationSeconds,
                videoTitle = videoTitle,
                episodeTitle = episodeTitle
            )
            MarkedHlsAdSegment(
                rule = rule,
                message = if (rule.urlPattern == null) {
                    "已标记当前广告片段"
                } else {
                    "已标记广告，并生成同类片段过滤规则"
                }
            )
        }
    }

    private fun ensureStarted() {
        if (serverSocket != null) return
        synchronized(lock) {
            if (serverSocket != null) return
            val socket = ServerSocket(0, BACKLOG, InetAddress.getByName(BIND_HOST))
            serverSocket = socket
            port = socket.localPort
            scope.launch {
                acceptLoop(socket)
            }
        }
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            runCatching {
                val client = socket.accept()
                scope.launch {
                    runCatching { handleClient(client) }
                        .onFailure { error ->
                            if (!error.isClientDisconnect()) {
                                Log.d(TAG, "client handler failed error=${error.message}", error)
                            }
                        }
                }
            }.onFailure { error ->
                if (!socket.isClosed) {
                    Log.d(TAG, "accept failed error=${error.message}")
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = SOCKET_TIMEOUT_MS
            val request = parseRequest(client.getInputStream()) ?: return
            runCatching {
                when (request.path) {
                    MANIFEST_PATH -> serveManifest(client, request)
                    RESOURCE_PATH -> serveResource(client, request)
                    else -> writeTextResponse(client.getOutputStream(), 404, "text/plain", "Not found", request.method == "HEAD")
                }
            }.onFailure { error ->
                if (!error.isClientDisconnect()) {
                    Log.d(TAG, "request failed path=${request.path} error=${error.message}", error)
                    runCatching {
                        writeTextResponse(
                            output = client.getOutputStream(),
                            statusCode = 502,
                            contentType = "text/plain",
                            body = error.message ?: "Proxy error",
                            headersOnly = request.method == "HEAD"
                        )
                    }.onFailure { writeError ->
                        if (!writeError.isClientDisconnect()) {
                            Log.d(TAG, "failed to write error response path=${request.path} writeError=${writeError.message}", writeError)
                        }
                    }
                }
            }
        }
    }

    private fun serveManifest(socket: Socket, request: ProxyRequest) {
        val originUrl = request.query["u"] ?: throw IllegalArgumentException("missing url")
        val cachedPlaylist = cache.cachedText(originUrl)
        val upstreamRequest = Request.Builder().url(originUrl).get().build()
        val upstreamResult = runCatching {
            okHttpClient.newCall(upstreamRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use UpstreamManifestResult.Failed(response.code)
                }
                UpstreamManifestResult.Succeeded(response.body?.string().orEmpty())
            }
        }
        val upstreamManifest = upstreamResult.getOrNull() as? UpstreamManifestResult.Succeeded
        val playlist = upstreamManifest?.playlist ?: cachedPlaylist
        if (playlist == null) {
            val statusCode = (upstreamResult.getOrNull() as? UpstreamManifestResult.Failed)?.statusCode ?: 503
            writeTextResponse(socket.getOutputStream(), statusCode, "text/plain", "Manifest temporarily unavailable", request.method == "HEAD")
            return
        }

        if (upstreamManifest != null) {
            runCatching { cache.writeText(originUrl, HLS_CONTENT_TYPE, playlist) }
        }
        val target = request.target
        val mediaSegments = manifestRewriter.extractMediaSegments(originUrl, playlist)
        val knownAdUrls = buildSet {
            cachedPlaylist?.let { addAll(manifestRewriter.extractAdResourceUrls(originUrl, it)) }
            addAll(manifestRewriter.extractAdResourceUrls(originUrl, playlist))
            addAll(adRuleStore.matchingAdUrls(originUrl, mediaSegments.map { it.url }))
        }
        val rewritten = manifestRewriter.rewrite(originUrl, playlist, knownAdUrls) { url, type ->
            proxyUrl(
                originUrl = url,
                type = type,
                target = target,
                parentPlaylistUrl = originUrl.takeIf { type == HlsProxyResourceType.Resource }
            )
        }
        prefetchCoordinator.prefetchResources(rewritten.prefetchUrls)
        writeTextResponse(
            output = socket.getOutputStream(),
            statusCode = 200,
            contentType = HLS_CONTENT_TYPE,
            body = rewritten.playlist,
            headersOnly = request.method == "HEAD"
        )
    }

    private fun serveResource(socket: Socket, request: ProxyRequest) {
        val originUrl = request.query["u"] ?: throw IllegalArgumentException("missing url")
        request.query["p"]?.takeIf { isMediaResourceUrl(originUrl) }?.let { playlistUrl ->
            recordRecentResource(originUrl, playlistUrl)
        }
        val range = request.headers["range"]?.let(::parseRangeHeader)
        val cached = cache.cachedResource(originUrl)
        if (cached != null) {
            writeCachedResource(socket.getOutputStream(), cached, range, request.method == "HEAD")
            return
        }
        fetchResource(socket.getOutputStream(), originUrl, request.method == "HEAD", range)
    }

    private fun fetchResource(
        output: OutputStream,
        originUrl: String,
        headersOnly: Boolean,
        range: ByteRange?
    ) {
        val requestBuilder = Request.Builder().url(originUrl)
        if (headersOnly) {
            requestBuilder.head()
        }
        if (range != null) {
            requestBuilder.header("Range", range.toHeaderValue())
        }
        okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                writeTextResponse(output, response.code, "text/plain", "Resource request failed", headersOnly)
                return
            }
            val responseBody = response.body
            val contentType = responseBody?.contentType()?.toString() ?: cache.guessContentType(originUrl)
            val contentLength = responseBody?.contentLength()?.takeIf { it >= 0L }
            val statusCode = response.code

            if (range != null || headersOnly || responseBody == null) {
                writeBinaryHeaders(
                    output = output,
                    statusCode = statusCode,
                    contentType = contentType,
                    contentLength = contentLength,
                    extraHeaders = response.header("Content-Range")?.let { mapOf("Content-Range" to it) }.orEmpty(),
                    headersOnly = headersOnly
                )
                if (!headersOnly && responseBody != null) {
                    responseBody.byteStream().use { input -> input.copyTo(output) }
                }
                return
            }

            writeBinaryHeaders(
                output = output,
                statusCode = 200,
                contentType = contentType,
                contentLength = responseBody.contentLength().takeIf { it >= 0L },
                headersOnly = false
            )
            responseBody.byteStream().use { input ->
                cache.writeFromStream(originUrl, contentType, input, output)
            }
        }
    }

    private fun writeCachedResource(
        output: OutputStream,
        resource: HlsPersistentCache.CachedResource,
        range: ByteRange?,
        headersOnly: Boolean
    ) {
        val resolvedRange = range?.resolve(resource.length)
        if (range != null && resolvedRange == null) {
            writeBinaryHeaders(
                output = output,
                statusCode = 416,
                contentType = "text/plain",
                contentLength = 0L,
                extraHeaders = mapOf("Content-Range" to "bytes */${resource.length}"),
                headersOnly = true
            )
            return
        }
        val statusCode = if (resolvedRange == null) 200 else 206
        val contentLength = resolvedRange?.length ?: resource.length
        val headers = resolvedRange?.let {
            mapOf("Content-Range" to "bytes ${it.start}-${it.end}/${resource.length}")
        }.orEmpty()
        writeBinaryHeaders(
            output = output,
            statusCode = statusCode,
            contentType = resource.contentType,
            contentLength = contentLength,
            extraHeaders = headers,
            headersOnly = headersOnly
        )
        if (headersOnly) return

        if (resolvedRange == null) {
            FileInputStream(resource.file).use { input -> input.copyTo(output) }
        } else {
            RandomAccessFile(resource.file, "r").use { file ->
                file.seek(resolvedRange.start)
                copyRange(file, output, resolvedRange.length)
            }
        }
        output.flush()
    }

    private fun parseRequest(input: InputStream): ProxyRequest? {
        val reader = input.bufferedReader(Charsets.ISO_8859_1)
        val requestLine = reader.readLine() ?: return null
        val requestParts = requestLine.split(" ")
        if (requestParts.size < 2) return null
        val method = requestParts[0].uppercase(Locale.US)
        val rawPath = requestParts[1]
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            val separator = line.indexOf(':')
            if (separator > 0) {
                headers[line.substring(0, separator).trim().lowercase(Locale.US)] =
                    line.substring(separator + 1).trim()
            }
        }

        val path = rawPath.substringBefore('?')
        val query = rawPath.substringAfter('?', missingDelimiterValue = "")
            .split('&')
            .filter { it.isNotBlank() && it.contains('=') }
            .associate { value ->
                val key = value.substringBefore('=')
                val encodedValue = value.substringAfter('=')
                key to URLDecoder.decode(encodedValue, Charsets.UTF_8.name())
            }
        val hostHeader = headers["host"].orEmpty()
        val target = if (hostHeader.startsWith(LOOPBACK_HOST) || hostHeader.startsWith("localhost")) {
            HlsProxyTarget.Loopback
        } else {
            HlsProxyTarget.Lan
        }
        return ProxyRequest(method, path, query, headers, target)
    }

    private fun writeTextResponse(
        output: OutputStream,
        statusCode: Int,
        contentType: String,
        body: String,
        headersOnly: Boolean
    ) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        writeBinaryHeaders(
            output = output,
            statusCode = statusCode,
            contentType = contentType,
            contentLength = bytes.size.toLong(),
            headersOnly = headersOnly
        )
        if (!headersOnly) {
            output.write(bytes)
        }
        output.flush()
    }

    private fun writeBinaryHeaders(
        output: OutputStream,
        statusCode: Int,
        contentType: String,
        contentLength: Long?,
        extraHeaders: Map<String, String> = emptyMap(),
        headersOnly: Boolean
    ) {
        val headers = buildString {
            append("HTTP/1.1 ")
            append(statusCode)
            append(' ')
            append(reasonPhrase(statusCode))
            append("\r\n")
            append("Content-Type: ").append(contentType).append("\r\n")
            append("Accept-Ranges: bytes\r\n")
            contentLength?.let { append("Content-Length: ").append(it).append("\r\n") }
            extraHeaders.forEach { (name, value) -> append(name).append(": ").append(value).append("\r\n") }
            append("Connection: close\r\n")
            append("\r\n")
        }
        output.write(headers.toByteArray(Charsets.ISO_8859_1))
        if (headersOnly) output.flush()
    }

    private fun parseRangeHeader(header: String): ByteRange? {
        if (!header.startsWith("bytes=", ignoreCase = true)) return null
        val range = header.removePrefix("bytes=").substringBefore(',')
        val start = range.substringBefore('-', missingDelimiterValue = "").toLongOrNull() ?: return null
        val end = range.substringAfter('-', missingDelimiterValue = "").toLongOrNull()
        return ByteRange(start, end)
    }

    private fun copyRange(file: RandomAccessFile, output: OutputStream, length: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var remaining = length
        while (remaining > 0L) {
            val read = file.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read <= 0) return
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun reasonPhrase(statusCode: Int): String {
        return when (statusCode) {
            200 -> "OK"
            206 -> "Partial Content"
            400 -> "Bad Request"
            404 -> "Not Found"
            416 -> "Range Not Satisfiable"
            502 -> "Bad Gateway"
            else -> "Proxy Response"
        }
    }

    private fun localIpv4Address(): String? {
        return runCatching {
            NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { networkInterface ->
                    val name = networkInterface.name.lowercase(Locale.US)
                    if (name.startsWith("wlan") || name.startsWith("eth")) 0 else 1
                }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        }.getOrNull()
    }

    private fun recordRecentResource(resourceUrl: String, playlistUrl: String) {
        synchronized(recentResources) {
            val now = System.currentTimeMillis()
            recentResources.removeAll { it.url == resourceUrl && it.playlistUrl == playlistUrl }
            recentResources.addLast(
                RecentHlsResource(
                    url = resourceUrl,
                    playlistUrl = playlistUrl,
                    requestedAtMs = now
                )
            )
            while (recentResources.size > RECENT_RESOURCE_LIMIT) {
                recentResources.removeFirst()
            }
        }
    }

    private fun resolveCurrentResource(playbackPositionMs: Long): ResolvedAdCandidate? {
        val recent = synchronized(recentResources) {
            recentResources.toList()
        }.asReversed()
            .firstOrNull { System.currentTimeMillis() - it.requestedAtMs <= RECENT_RESOURCE_WINDOW_MS }
            ?: return null

        val playlistText = cache.cachedText(recent.playlistUrl)
        val mediaSegments = playlistText
            ?.let { manifestRewriter.extractMediaSegments(recent.playlistUrl, it) }
            .orEmpty()
        val byPosition = findSegmentAtPosition(mediaSegments, playbackPositionMs)
        val target = byPosition ?: mediaSegments.firstOrNull { it.url == recent.url }
        return ResolvedAdCandidate(
            url = target?.url ?: recent.url,
            playlistUrl = recent.playlistUrl,
            durationSeconds = target?.durationSeconds
        )
    }

    private fun findSegmentAtPosition(
        segments: List<HlsMediaSegment>,
        playbackPositionMs: Long
    ): HlsMediaSegment? {
        if (segments.isEmpty() || playbackPositionMs <= 0L) return null
        val positionSeconds = playbackPositionMs / 1000.0
        return segments.firstOrNull { segment ->
            val start = segment.startSeconds ?: return@firstOrNull false
            val end = start + (segment.durationSeconds ?: 0.0)
            positionSeconds >= start && positionSeconds < end
        }
    }

    private fun isMediaResourceUrl(url: String): Boolean {
        val lower = url.substringBefore('?').lowercase(Locale.US)
        return lower.endsWith(".ts") ||
            lower.endsWith(".m4s") ||
            lower.endsWith(".mp4") ||
            lower.endsWith(".aac")
    }

    private fun Throwable.isClientDisconnect(): Boolean {
        val message = message.orEmpty().lowercase(Locale.US)
        return this is java.net.SocketException &&
            (message.contains("broken pipe") || message.contains("connection reset") || message.contains("socket closed"))
    }

    private data class ProxyRequest(
        val method: String,
        val path: String,
        val query: Map<String, String>,
        val headers: Map<String, String>,
        val target: HlsProxyTarget
    )

    private data class RecentHlsResource(
        val url: String,
        val playlistUrl: String,
        val requestedAtMs: Long
    )

    private data class ResolvedAdCandidate(
        val url: String,
        val playlistUrl: String,
        val durationSeconds: Double?
    )

    private sealed interface UpstreamManifestResult {
        data class Succeeded(val playlist: String) : UpstreamManifestResult
        data class Failed(val statusCode: Int) : UpstreamManifestResult
    }

    private data class ByteRange(
        val start: Long,
        val requestedEnd: Long?
    ) {
        fun resolve(totalLength: Long): ResolvedByteRange? {
            if (totalLength <= 0L || start >= totalLength) return null
            val end = requestedEnd?.coerceAtMost(totalLength - 1) ?: totalLength - 1
            if (end < start) return null
            return ResolvedByteRange(start, end)
        }

        fun toHeaderValue(): String {
            return "bytes=$start-${requestedEnd ?: ""}"
        }
    }

    private data class ResolvedByteRange(
        val start: Long,
        val end: Long
    ) {
        val length: Long = end - start + 1
    }

    private companion object {
        private const val TAG = "iCinemaHlsProxy"
        private const val LOOPBACK_HOST = "127.0.0.1"
        private const val BIND_HOST = "0.0.0.0"
        private const val MANIFEST_PATH = "/hls/manifest"
        private const val RESOURCE_PATH = "/hls/resource"
        private const val HLS_CONTENT_TYPE = "application/vnd.apple.mpegurl"
        private const val SOCKET_TIMEOUT_MS = 30_000
        private const val BACKLOG = 32
        private const val RECENT_RESOURCE_LIMIT = 30
        private const val RECENT_RESOURCE_WINDOW_MS = 120_000L
    }
}
