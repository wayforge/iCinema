package com.icinema.pages.player.core.hls

import com.icinema.pages.player.core.PlaybackCacheManager
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HlsPersistentCache @Inject constructor(
    cacheManager: PlaybackCacheManager
) {
    private val rootDir: File = cacheManager.hlsCacheDir
    private val maxBytes: Long = cacheManager.maxCacheBytes

    fun cachedResource(url: String, contentType: String? = null): CachedResource? {
        val file = dataFile(url)
        if (!file.exists() || file.length() <= 0L) return null
        file.setLastModified(System.currentTimeMillis())
        return CachedResource(
            file = file,
            contentType = contentType ?: readMeta(url).contentType ?: guessContentType(url),
            length = file.length()
        )
    }

    fun isCached(url: String): Boolean {
        val file = dataFile(url)
        return file.exists() && file.length() > 0L
    }

    fun cachedText(url: String): String? {
        val file = dataFile(url)
        if (!file.exists() || file.length() <= 0L) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    fun contentFingerprint(url: String): HlsContentFingerprint? {
        val file = dataFile(url)
        if (!file.exists() || file.length() <= 0L) return null
        val meta = readMeta(url)
        if (
            !meta.sha256.isNullOrBlank() &&
            meta.length != null &&
            meta.length == file.length()
        ) {
            return HlsContentFingerprint(sha256 = meta.sha256, length = meta.length)
        }
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val fingerprint = HlsContentFingerprint(
                sha256 = digest.digest().toHexString(),
                length = file.length()
            )
            writeMeta(
                url = url,
                contentType = meta.contentType ?: guessContentType(url),
                sha256 = fingerprint.sha256,
                length = fingerprint.length
            )
            fingerprint
        }.getOrNull()
    }

    fun write(url: String, contentType: String?, writer: (OutputStream) -> Unit): CachedResource {
        ensureRoot()
        val targetFile = dataFile(url)
        val tempFile = tempFile(targetFile)
        return runCatching {
            tempFile.outputStream().use(writer)
            finalizeWrite(url, contentType, targetFile, tempFile)
        }.getOrElse { error ->
            tempFile.delete()
            throw error
        }
    }

    fun writeFromStream(
        url: String,
        contentType: String?,
        input: InputStream,
        output: OutputStream
    ): CachedResource {
        ensureRoot()
        val targetFile = dataFile(url)
        val tempFile = tempFile(targetFile)
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            tempFile.outputStream().use { fileOutput ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                    fileOutput.write(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
                fileOutput.flush()
                output.flush()
            }
            finalizeWrite(
                url = url,
                contentType = contentType,
                targetFile = targetFile,
                tempFile = tempFile,
                sha256 = digest.digest().toHexString()
            )
        }.getOrElse { error ->
            tempFile.delete()
            throw error
        }
    }

    fun writeFromStream(
        url: String,
        contentType: String?,
        input: InputStream
    ): CachedResource {
        return writeFromStream(
            url = url,
            contentType = contentType,
            input = input,
            output = DEV_NULL_OUTPUT_STREAM
        )
    }

    fun writeText(url: String, contentType: String?, text: String): CachedResource {
        return write(url, contentType) { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        }
    }

    fun guessContentType(url: String): String {
        val lower = url.substringBefore('?').lowercase()
        return when {
            lower.endsWith(".m3u8") -> "application/vnd.apple.mpegurl"
            lower.endsWith(".ts") -> "video/mp2t"
            lower.endsWith(".m4s") || lower.endsWith(".mp4") -> "video/mp4"
            lower.endsWith(".aac") -> "audio/aac"
            lower.endsWith(".vtt") -> "text/vtt"
            else -> "application/octet-stream"
        }
    }

    private fun ensureRoot() {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
    }

    private fun trimToSize() {
        val dataFiles = rootDir.listFiles { file -> file.name.endsWith(DATA_SUFFIX) }
            ?.sortedBy { it.lastModified() }
            ?: return
        var totalBytes = dataFiles.sumOf { it.length() }
        for (file in dataFiles) {
            if (totalBytes <= maxBytes) return
            totalBytes -= file.length()
            file.delete()
            File(rootDir, "${file.name.removeSuffix(DATA_SUFFIX)}$META_SUFFIX").delete()
        }
    }

    private fun dataFile(url: String): File {
        ensureRoot()
        return File(rootDir, "${hash(url)}$DATA_SUFFIX")
    }

    private fun metaFile(url: String): File {
        ensureRoot()
        return File(rootDir, "${hash(url)}$META_SUFFIX")
    }

    private fun tempFile(targetFile: File): File {
        return File(rootDir, "${targetFile.name}.${System.nanoTime()}.${TEMP_FILE_COUNTER.incrementAndGet()}.tmp")
    }

    private fun finalizeWrite(
        url: String,
        contentType: String?,
        targetFile: File,
        tempFile: File,
        sha256: String? = null
    ): CachedResource {
        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
        val resolvedContentType = contentType ?: guessContentType(url)
        val fingerprintSha = sha256 ?: runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            targetFile.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().toHexString()
        }.getOrNull()
        writeMeta(
            url = url,
            contentType = resolvedContentType,
            sha256 = fingerprintSha,
            length = targetFile.length()
        )
        trimToSize()
        return CachedResource(
            file = targetFile,
            contentType = resolvedContentType,
            length = targetFile.length()
        )
    }

    private fun writeMeta(
        url: String,
        contentType: String,
        sha256: String?,
        length: Long?
    ) {
        val body = buildString {
            append("contentType=").append(contentType).append('\n')
            if (!sha256.isNullOrBlank()) {
                append("sha256=").append(sha256).append('\n')
            }
            if (length != null && length > 0L) {
                append("length=").append(length).append('\n')
            }
        }
        metaFile(url).writeText(body, Charsets.UTF_8)
    }

    private fun readMeta(url: String): CacheMeta {
        val file = metaFile(url)
        if (!file.exists()) return CacheMeta()
        val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
        if (text.isBlank()) return CacheMeta()
        // Backward compatible: plain content-type string without keys.
        if (!text.contains('=') && !text.contains('\n')) {
            return CacheMeta(contentType = text.trim().takeIf { it.isNotBlank() })
        }
        var contentType: String? = null
        var sha256: String? = null
        var length: Long? = null
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val key = line.substringBefore('=', missingDelimiterValue = "").trim()
            val value = line.substringAfter('=', missingDelimiterValue = "").trim()
            when (key) {
                "contentType" -> contentType = value.takeIf { it.isNotBlank() }
                "sha256" -> sha256 = value.takeIf { it.isNotBlank() }
                "length" -> length = value.toLongOrNull()
                "" -> if (contentType == null) contentType = line.takeIf { it.isNotBlank() }
            }
        }
        return CacheMeta(contentType = contentType, sha256 = sha256, length = length)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.toHexString()
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    data class CachedResource(
        val file: File,
        val contentType: String,
        val length: Long
    )

    private data class CacheMeta(
        val contentType: String? = null,
        val sha256: String? = null,
        val length: Long? = null
    )

    private companion object {
        private const val DATA_SUFFIX = ".data"
        private const val META_SUFFIX = ".meta"
        private val TEMP_FILE_COUNTER = AtomicLong()
        private val DEV_NULL_OUTPUT_STREAM = object : OutputStream() {
            override fun write(b: Int) = Unit
        }
    }
}
