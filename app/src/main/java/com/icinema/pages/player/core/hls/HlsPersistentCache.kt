package com.icinema.pages.player.core.hls

import com.icinema.pages.player.core.PlaybackCacheManager
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
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
            contentType = contentType ?: readContentType(url) ?: guessContentType(url),
            length = file.length()
        )
    }

    fun isCached(url: String): Boolean {
        val file = dataFile(url)
        return file.exists() && file.length() > 0L
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
            tempFile.outputStream().use { fileOutput ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    fileOutput.write(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
                fileOutput.flush()
                output.flush()
            }
            finalizeWrite(url, contentType, targetFile, tempFile)
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
        return File(rootDir, "${targetFile.name}.${Thread.currentThread().id}.${System.nanoTime()}.tmp")
    }

    private fun writeContentType(url: String, contentType: String) {
        metaFile(url).writeText(contentType, Charsets.UTF_8)
    }

    private fun finalizeWrite(
        url: String,
        contentType: String?,
        targetFile: File,
        tempFile: File
    ): CachedResource {
        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
        val resolvedContentType = contentType ?: guessContentType(url)
        writeContentType(url, resolvedContentType)
        trimToSize()
        return CachedResource(
            file = targetFile,
            contentType = resolvedContentType,
            length = targetFile.length()
        )
    }

    private fun readContentType(url: String): String? {
        val file = metaFile(url)
        return file.takeIf { it.exists() }?.readText(Charsets.UTF_8)?.takeIf { it.isNotBlank() }
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    data class CachedResource(
        val file: File,
        val contentType: String,
        val length: Long
    )

    private companion object {
        private const val DATA_SUFFIX = ".data"
        private const val META_SUFFIX = ".meta"
        private val DEV_NULL_OUTPUT_STREAM = object : OutputStream() {
            override fun write(b: Int) = Unit
        }
    }
}
