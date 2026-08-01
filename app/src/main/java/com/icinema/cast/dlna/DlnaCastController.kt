package com.icinema.cast.dlna

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.icinema.cast.CastController
import com.icinema.cast.CastMedia
import com.icinema.cast.CastState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class DlnaCastController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : CastController {

    private val _state = MutableStateFlow(CastState())
    override val state: StateFlow<CastState> = _state.asStateFlow()

    private val devices = mutableMapOf<String, DlnaDevice>()
    private var connectedDeviceId: String? = null

    override suspend fun startDiscovery(timeoutMs: Long) {
        Log.d(TAG, "startDiscovery timeoutMs=$timeoutMs")
        _state.update { it.copy(isSearching = true, errorMessage = null) }
        val discoveredDevices = withContext(Dispatchers.IO) {
            discoverDevices(timeoutMs)
        }
        Log.d(TAG, "startDiscovery completed discovered=${discoveredDevices.size}")
        val connectedDeviceBeforeRefresh = connectedDeviceId?.let { devices[it] }
        val nextDevices = (discoveredDevices + listOfNotNull(connectedDeviceBeforeRefresh))
            .distinctBy { it.id }
        devices.clear()
        nextDevices.forEach { devices[it.id] = it }
        val connectedDevice = connectedDeviceId?.let { devices[it]?.toCastDevice() }
        _state.update {
            it.copy(
                isSearching = false,
                devices = nextDevices.map { device -> device.toCastDevice() },
                connectedDevice = connectedDevice,
                errorMessage = null
            )
        }
    }

    override suspend fun cast(deviceId: String, media: CastMedia): Result<Unit> {
        val device = devices[deviceId] ?: return Result.failure(IllegalArgumentException("未找到投屏设备"))
        val castDevice = device.toCastDevice()
        _state.update {
            it.copy(
                isConnecting = true,
                connectedDevice = castDevice,
                errorMessage = null
            )
        }

        return runCatching {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "cast SetAVTransportURI device=${device.friendlyName} url=${media.url}")
                sendAction(device, "SetAVTransportURI", DlnaSoap.setUriArguments(media))
                sendAction(device, "Play", DlnaSoap.playArguments())
                if (media.positionMs > 0L) {
                    sendAction(device, "Seek", DlnaSoap.seekArguments(media.positionMs))
                }
            }
            connectedDeviceId = device.id
            _state.update {
                it.copy(
                    isConnecting = false,
                    connectedDevice = castDevice,
                    isCasting = true,
                    isPlaying = true,
                    currentMediaTitle = media.title,
                    currentMediaSubtitle = media.subtitle,
                    currentMediaImageUrl = media.imageUrl,
                    currentPositionMs = media.positionMs.coerceAtLeast(0L),
                    durationMs = media.durationMs.coerceAtLeast(0L),
                    errorMessage = null
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isConnecting = false,
                    isCasting = false,
                    isPlaying = false,
                    errorMessage = error.message ?: "投屏失败"
                )
            }
        }
    }

    override suspend fun play(): Result<Unit> {
        return sendConnectedAction("Play", DlnaSoap.playArguments()) {
            it.copy(isPlaying = true, errorMessage = null)
        }
    }

    override suspend fun pause(): Result<Unit> {
        return sendConnectedAction("Pause", DlnaSoap.instanceArguments()) {
            it.copy(isPlaying = false, errorMessage = null)
        }
    }

    override suspend fun seekTo(positionMs: Long): Result<Unit> {
        return sendConnectedAction("Seek", DlnaSoap.seekArguments(positionMs)) {
            it.copy(currentPositionMs = positionMs.coerceAtLeast(0L), errorMessage = null)
        }
    }

    override suspend fun refreshPlaybackPosition(): Result<Unit> {
        val device = connectedDevice() ?: return Result.failure(IllegalStateException("未连接投屏设备"))
        return runCatching {
            val response = withContext(Dispatchers.IO) {
                sendAction(device, "GetPositionInfo", DlnaSoap.instanceArguments())
            }
            val position = DlnaSoap.parsePositionInfo(response) ?: return@runCatching
            _state.update {
                it.copy(
                    currentPositionMs = position.currentPositionMs,
                    durationMs = position.durationMs,
                    errorMessage = null
                )
            }
        }.onFailure { error ->
            _state.update { it.copy(errorMessage = error.message ?: "获取投屏进度失败") }
        }
    }

    override suspend fun stopCasting(): Result<Long?> {
        val device = connectedDevice()
        val positionResult = refreshPlaybackPosition()
        val lastPosition = _state.value.currentPositionMs.takeIf { it > 0L }

        return runCatching {
            if (device != null) {
                withContext(Dispatchers.IO) {
                    sendAction(device, "Stop", DlnaSoap.instanceArguments())
                }
            }
            connectedDeviceId = null
            _state.update {
                it.copy(
                    connectedDevice = null,
                    isCasting = false,
                    isPlaying = false,
                    currentMediaTitle = "",
                    currentMediaSubtitle = "",
                    currentMediaImageUrl = "",
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    errorMessage = positionResult.exceptionOrNull()?.message
                )
            }
            lastPosition
        }.onFailure { error ->
            _state.update { it.copy(errorMessage = error.message ?: "停止投屏失败") }
        }
    }

    override fun release() {
        devices.clear()
        connectedDeviceId = null
        _state.value = CastState()
    }

    private suspend fun sendConnectedAction(
        action: String,
        arguments: String,
        reduce: (CastState) -> CastState
    ): Result<Unit> {
        val device = connectedDevice() ?: return Result.failure(IllegalStateException("未连接投屏设备"))
        return runCatching {
            withContext(Dispatchers.IO) {
                sendAction(device, action, arguments)
            }
            _state.update(reduce)
        }.onFailure { error ->
            _state.update { it.copy(errorMessage = error.message ?: "投屏控制失败") }
        }
    }

    private fun connectedDevice(): DlnaDevice? {
        return connectedDeviceId?.let { devices[it] }
    }

    private suspend fun discoverDevices(timeoutMs: Long): List<DlnaDevice> {
        val locations = linkedSetOf<String>()
        acquireMulticastLock().use {
            DatagramSocket().use { socket ->
                socket.soTimeout = SOCKET_READ_TIMEOUT_MS
                val targetAddress = InetAddress.getByName(SSDP_ADDRESS)
                repeat(SEARCH_BURST_COUNT) {
                    SEARCH_TARGETS.forEach { searchTarget ->
                        Log.d(TAG, "send M-SEARCH st=$searchTarget")
                        val requestBytes = buildSearchRequest(searchTarget).toByteArray(Charsets.UTF_8)
                        socket.send(
                            DatagramPacket(
                                requestBytes,
                                requestBytes.size,
                                targetAddress,
                                SSDP_PORT
                            )
                        )
                    }
                }

                val buffer = ByteArray(RESPONSE_BUFFER_SIZE)
                val deadline = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < deadline && currentCoroutineContext().isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                        val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val location = DlnaDiscoveryParser.parseLocation(response)
                        Log.d(
                            TAG,
                            "receive SSDP from=${packet.address.hostAddress}:${packet.port} location=$location"
                        )
                        if (location == null) {
                            Log.d(TAG, "ignore SSDP without LOCATION: ${response.lineSequence().firstOrNull()}")
                        } else {
                            locations.add(location)
                        }
                    } catch (_: SocketTimeoutException) {
                        // Keep waiting until the outer discovery window expires.
                    }
                }
            }
        }

        Log.d(TAG, "fetch device descriptions locations=${locations.size}")
        return locations
            .mapNotNull { locationUrl -> fetchDevice(locationUrl) }
            .distinctBy { it.id }
    }

    private fun fetchDevice(locationUrl: String): DlnaDevice? {
        Log.d(TAG, "fetchDevice location=$locationUrl")
        val request = Request.Builder().url(locationUrl).get().build()
        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "fetchDevice failed code=${response.code} location=$locationUrl")
                    return null
                }
                val body = response.body?.string().orEmpty()
                val device = DlnaDeviceDescriptionParser.parse(locationUrl, body)
                if (device == null) {
                    Log.d(TAG, "fetchDevice ignored no MediaRenderer/AVTransport location=$locationUrl")
                } else {
                    Log.d(
                        TAG,
                        "fetchDevice accepted id=${device.id} name=${device.friendlyName} model=${device.modelName} control=${device.avTransportControlUrl}"
                    )
                }
                device
            }
        }.onFailure { error ->
            Log.d(TAG, "fetchDevice error location=$locationUrl error=${error.message}", error)
        }.getOrNull()
    }

    private fun sendAction(device: DlnaDevice, action: String, arguments: String): String {
        val body = DlnaSoap.envelope(action, arguments)
            .toRequestBody(XML_MEDIA_TYPE)
        val request = Request.Builder()
            .url(device.avTransportControlUrl)
            .post(body)
            .header("Content-Type", "text/xml; charset=\"utf-8\"")
            .header("SOAPACTION", "\"${DlnaSoap.AV_TRANSPORT_SERVICE}#$action\"")
            .header("User-Agent", USER_AGENT)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("投屏设备响应异常：${response.code}")
            }
            return responseBody
        }
    }

    private fun acquireMulticastLock(): MulticastLockHandle {
        val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)
        val lock = wifiManager?.createMulticastLock(MULTICAST_LOCK_TAG)?.apply {
            setReferenceCounted(false)
            acquire()
        }
        return MulticastLockHandle(lock)
    }

    private class MulticastLockHandle(
        private val lock: WifiManager.MulticastLock?
    ) : AutoCloseable {
        override fun close() {
            if (lock?.isHeld == true) {
                lock.release()
            }
        }
    }

    private companion object {
        private const val TAG = "iCinemaDlna"
        private const val SSDP_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SOCKET_READ_TIMEOUT_MS = 700
        private const val RESPONSE_BUFFER_SIZE = 16 * 1024
        private const val SEARCH_BURST_COUNT = 3
        private const val MULTICAST_LOCK_TAG = "iCinemaDlnaDiscovery"
        private const val USER_AGENT = "iCinema/1.0 UPnP/1.0 DLNADOC/1.50"
        private val XML_MEDIA_TYPE = "text/xml; charset=utf-8".toMediaType()
        private val SEARCH_TARGETS = listOf(
            "urn:schemas-upnp-org:device:MediaRenderer:1",
            "urn:schemas-upnp-org:service:AVTransport:1",
            "upnp:rootdevice",
            "ssdp:all"
        )

        private fun buildSearchRequest(searchTarget: String): String {
            return """
            M-SEARCH * HTTP/1.1
            HOST: 239.255.255.250:1900
            MAN: "ssdp:discover"
            MX: 2
            ST: $searchTarget
            USER-AGENT: $USER_AGENT
            """.trimIndent().replace("\n", "\r\n") + "\r\n\r\n"
        }
    }
}
