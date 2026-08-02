package com.icinema.pages.player.core.hls

import android.util.Log
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class HlsPrefetchCoordinator @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val cache: HlsPersistentCache,
    private val manifestRewriter: HlsManifestRewriter
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = Collections.synchronizedSet(mutableSetOf<String>())

    fun prefetchManifest(manifestUrl: String) {
        if (!inFlight.add(manifestUrl)) return
        scope.launch {
            runCatching {
                val request = Request.Builder().url(manifestUrl).get().build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string().orEmpty()
                    val result = manifestRewriter.rewrite(manifestUrl, body) { url, _ -> url }
                    prefetchResources(result.prefetchUrls.take(MANIFEST_PREFETCH_LIMIT))
                }
            }.onFailure { error ->
                Log.d(TAG, "prefetch manifest failed url=$manifestUrl error=${error.message}")
            }
            inFlight.remove(manifestUrl)
        }
    }

    fun prefetchResources(resourceUrls: List<String>) {
        resourceUrls
            .distinct()
            .take(RESOURCE_PREFETCH_LIMIT)
            .forEach { url ->
                if (cache.isCached(url) || !inFlight.add(url)) return@forEach
                scope.launch {
                    runCatching {
                        val request = Request.Builder().url(url).get().build()
                        okHttpClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) return@use
                            val body = response.body ?: return@use
                            cache.writeFromStream(
                                url = url,
                                contentType = body.contentType()?.toString(),
                                input = body.byteStream()
                            )
                        }
                    }.onFailure { error ->
                        Log.d(TAG, "prefetch resource failed url=$url error=${error.message}")
                    }
                    inFlight.remove(url)
                }
            }
    }

    companion object {
        private const val TAG = "iCinemaHlsPrefetch"
        private const val MANIFEST_PREFETCH_LIMIT = 8
        private const val RESOURCE_PREFETCH_LIMIT = 8
    }
}
