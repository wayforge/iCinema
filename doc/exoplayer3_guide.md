# 在 Jetpack Compose 中使用 ExoPlayer 3 的最佳实践

## 1. 添加依赖

在 `build.gradle` 中添加 Compose 和 Media3 的依赖：

```gradle
dependencies {
    implementation "androidx.media3:media3-exoplayer:1.2.0"
    implementation "androidx.media3:media3-ui:1.2.0"
    implementation "androidx.media3:media3-session:1.2.0"  // 如果需要后台播放
    
    // Compose 相关依赖
    implementation "androidx.compose.ui:ui:$compose_version"
    implementation "androidx.compose.ui:ui-tooling-preview:$compose_version"
    implementation "androidx.activity:activity-compose:$activity_compose_version"
}
```

## 2. 创建 ExoPlayer 的 Compose 可组合函数

由于 ExoPlayer 需要 View 系统，我们需要使用 `AndroidView` 将原生的 `PlayerView` 集成到 Compose UI 中：

```kotlin
@Composable
fun ExoPlayerView(
    modifier: Modifier = Modifier,
    player: ExoPlayer,
    onVideoSizeChanged: (videoWidth: Int, videoHeight: Int) -> Unit = { _, _ -> }
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                // 设置播放器视图
                useController = true  // 启用默认控制器
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)  // 总是显示缓冲
                
                // 添加视频尺寸变化监听器
                player.addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        onVideoSizeChanged(videoSize.width, videoSize.height)
                    }
                })
                
                // 绑定播放器
                this.player = player
            }
        },
        update = { playerView ->
            // 更新播放器视图属性
            playerView.player = player
        },
        modifier = modifier
    )
}
```

## 3. 创建播放器管理器

```kotlin
@Composable
fun rememberExoPlayer(
    uri: Uri,
    autoplay: Boolean = false,
    onPlayerReady: (ExoPlayer) -> Unit = {}
): ExoPlayer {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(), 
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    DisposableEffect(player) {
        // 准备媒体项
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = autoplay
        
        onPlayerReady(player)
        
        // 添加错误监听器
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlayer", "播放错误: ${error.message}", error)
            }
        }
        player.addListener(listener)
        
        // 清理资源
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    return player
}
```

## 4. 在 Compose Screen 中使用

```kotlin
@OptimizeForComposePreview
@Composable
fun VideoPlayerScreen(
    videoUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = rememberExoPlayer(
        uri = videoUri,
        autoplay = false
    )

    // 状态变量
    var isPlaying by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(1f) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 视频播放器
        ExoPlayerView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f),
            player = exoPlayer,
            onVideoSizeChanged = { width, height ->
                // 处理视频尺寸变化
            }
        )

        // 控制面板
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 播放/暂停按钮
            IconButton(onClick = {
                if (isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
                isPlaying = !isPlaying
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            // 音量控制
            Slider(
                value = volume,
                onValueChange = { newValue ->
                    volume = newValue
                    exoPlayer.volume = newValue
                },
                valueRange = 0f..1f
            )

            // 播放速度选择
            DropdownMenu(
                expanded = false,
                onDismissRequest = { /* 处理下拉菜单状态 */ }
            ) {
                // 实现播放速度选择逻辑
            }
        }
    }
}
```

## 5. 处理生命周期

在 Compose 中处理播放器生命周期，可以使用 `DisposableEffect` 如上面的 `rememberExoPlayer` 函数所示。当 Composable 离开组合时，会自动释放播放器资源。

## 6. 高级功能实现

### 监听播放状态

```kotlin
@Composable
fun rememberPlayerState(player: ExoPlayer): PlayerState {
    val state = remember { mutableStateOf(PlayerState()) }
    
    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                state.value = state.value.copy(
                    playbackState = playbackState,
                    isPlaying = playbackState == Player.STATE_READY && player.isPlaying
                )
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.value = state.value.copy(isPlaying = isPlaying)
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                state.value = state.value.copy(position = newPosition.positionMs)
            }
        }
        
        player.addListener(listener)
        awaitDispose {
            player.removeListener(listener)
        }
    }
    
    return state.value
}
```

### 后台播放支持

如果需要后台播放功能，可以结合 `MediaSessionConnector`：

```kotlin
class PlayerService : Service() {
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var mediaSessionConnector: MediaSessionConnector

    override fun onCreate() {
        super.onCreate()
        
        exoPlayer = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
        mediaSessionConnector = MediaSessionConnector(mediaSession).also {
            it.setPlayer(exoPlayer)
        }
    }
    
    // ... 实现 Service 方法
}
```

## 7. 性能优化建议

- 使用 `remember` 和 `DisposableEffect` 来管理 ExoPlayer 生命周期
- 避免不必要的重组影响播放器性能
- 合理使用 `LaunchedEffect` 和 `produceState` 来响应播放器状态变化
- 根据屏幕方向调整视频比例

这种方法将 ExoPlayer 与 Compose 的声明式 UI 结合起来，既保持了 ExoPlayer 的强大功能，又利用了 Compose 的灵活性和简洁性。

## 8. 播放 m3u8 格式（HLS 流）

ExoPlayer 支持 HLS (HTTP Live Streaming) 协议，而 .m3u8 文件正是 HLS 流的播放列表格式。要播放 m3u8 链接，只需要在创建 MediaItem 时使用正确的 URI 即可。

### 8.1 基本实现

播放 m3u8 链接非常简单，只需要将 m3u8 URL 作为 URI 传递给 ExoPlayer：

```kotlin
// 示例：使用 m3u8 链接创建 MediaItem
val m3u8Url = "https://example.com/playlist.m3u8"  // 替换为实际的 m3u8 链接
val mediaItem = MediaItem.fromUri(m3u8Url)
player.setMediaItem(mediaItem)
```

在 Compose 中使用时，只需将 m3u8 链接作为 Uri 传递给您的播放器管理器函数即可：

```kotlin
// 使用示例
val m3u8Uri = Uri.parse("https://example.com/playlist.m3u8")
val exoPlayer = rememberExoPlayer(uri = m3u8Uri, autoplay = true)
```

### 8.2 高级配置（可选）

如果需要对 HLS 播放进行更精细的控制，可以配置 HlsMediaSource：

```kotlin
val hlsMediaSourceFactory = HlsMediaSource.Factory(
    DefaultHttpDataSource.Factory().apply {
        // 设置适当的安全User-Agent，避免暴露敏感信息
        setUserAgent("iCinema Player/1.0")
    }
)

val hlsMediaSource = hlsMediaSourceFactory.createMediaSource(
    MediaItem.fromUri(m3u8Url),
    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
)

player.setMediaSource(hlsMediaSource)
```

### 8.3 注意事项

- 确保您的应用具有必要的权限：
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />
  ```
  
- m3u8 链接通常需要网络连接，因此请确保设备已连接到网络
  
- 某些 m3u8 流可能受到 DRM 保护，这需要额外的配置来处理

- 对于 HTTPS m3u8 链接，确保服务器证书有效

在 Compose 集成中，之前的实现无需修改，只需传入 m3u8 链接作为 URI 参数即可。ExoPlayer 会自动识别这是一个 HLS 流并相应地处理它。

## 9. 分离下载和播放的策略

为了获得更流畅的播放体验，可以将下载和播放分离，实现边下边播的功能。这种策略可以让播放器优先从缓存中读取数据，而不必等待网络请求，从而大大提高播放流畅度。

### 9.1 实现智能缓存播放器

创建一个使用缓存数据源的播放器，优先从缓存读取数据：

```kotlin
class CachedExoPlayer(private val context: Context) {
    private val cache: Cache
    private val player: ExoPlayer
    private val downloadManager: DownloadManager
    
    init {
        cache = getDownloadCache()
        downloadManager = createDownloadManager()
        
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
        
        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                ProgressiveMediaSource.Factory(cacheDataSourceFactory)
            )
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                        30000, // 最大缓冲时间，可根据需要调整
                        2500,  // 开始播放前需要的缓冲时间
                        5000   // 重新缓冲前需要的缓冲时间
                    )
                    .build()
            )
            .build()
    }
    
    private fun getDownloadCache(): Cache {
        val downloadDirectory = File(context.getExternalFilesDir(null), "downloads")
        return SimpleCache(downloadDirectory, NoOpCacheEvictor())
    }
    
    private fun createDownloadManager(): DownloadManager {
        val databaseProvider = StandaloneDatabaseProvider(context)
        val factory = DefaultHttpDataSource.Factory()
        
        return DownloadManager(
            context,
            databaseProvider,
            cache,
            factory
        )
    }
    
    fun prepareAndPlay(uri: Uri, startDownload: Boolean = true) {
        // 检查是否已有部分下载
        val isPartiallyCached = isUriCached(uri.toString())
        
        if (startDownload && !isPartiallyCached) {
            // 如果没有缓存，则开始下载
            startDownload(uri)
        }
        
        // 准备播放，播放器会自动从缓存读取可用部分
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }
    
    private fun startDownload(uri: Uri) {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .build()
        
        val downloadRequest = DownloadRequest.Builder(
            uri.toString(), 
            uri
        )
            .setMediaItem(mediaItem)
            .setCustomCacheKey(uri.lastPathSegment ?: uri.toString())
            .build()
        
        downloadManager.addDownload(downloadRequest)
    }
    
    private fun isUriCached(uri: String): Boolean {
        // 检查 URI 是否已在缓存中
        val key = Util.toUtf8Bytes(uri)
        return cache.isCached(key, 0, C.LENGTH_UNSET)
    }
    
    fun getPlayer(): ExoPlayer = player
    
    fun release() {
        player.release()
        downloadManager.release()
    }
}
```

### 9.2 在 Compose 中实现预下载播放器

使用 Compose 实现一个支持预下载的播放器：

```kotlin
@Composable
fun PreDownloadVideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    autoStartDownload: Boolean = true
) {
    val context = LocalContext.current
    val cachedPlayer = remember {
        CachedExoPlayer(context)
    }
    
    // 使用 AndroidView 将 ExoPlayer 集成到 Compose
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                
                // 绑定播放器
                player = cachedPlayer.getPlayer()
                
                // 预先开始下载并准备播放
                cachedPlayer.prepareAndPlay(videoUri, autoStartDownload)
            }
        },
        update = { playerView ->
            playerView.player = cachedPlayer.getPlayer()
        },
        modifier = modifier
    )
    
    // 清理资源
    DisposableEffect(cachedPlayer) {
        onDispose {
            cachedPlayer.release()
        }
    }
}
```

### 9.3 高级缓存预加载策略

实现一个更高级的缓存管理器，可以在用户观看视频前预加载部分内容：

```kotlin
class AdvancedVideoCacheManager(private val context: Context) {
    private val cache: Cache
    private val downloadManager: DownloadManager
    private val cacheDataSourceFactory: CacheDataSource.Factory
    
    init {
        cache = getDownloadCache()
        downloadManager = createDownloadManager()
        
        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
    }
    
    fun prepareVideoWithCache(
        uri: Uri,
        preloadPercentage: Float = 0.3f, // 预加载前30%
        onPreloadComplete: (() -> Unit)? = null
    ) {
        // 检查缓存状态
        if (!isUriFullyCached(uri.toString())) {
            // 开始下载整个视频或至少预加载部分
            startDownload(uri, preloadPercentage, onPreloadComplete)
        }
    }
    
    private fun startDownload(
        uri: Uri, 
        preloadPercentage: Float, 
        onPreloadComplete: (() -> Unit)?
    ) {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .build()
        
        val downloadRequest = DownloadRequest.Builder(
            uri.toString(), 
            uri
        )
            .setMediaItem(mediaItem)
            .setCustomCacheKey(uri.lastPathSegment ?: uri.toString())
            .build()
        
        downloadManager.addDownload(downloadRequest)
        
        // 监听下载进度
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?
            ) {
                if (download.request.uri == uri && 
                    download.percentDownloaded >= preloadPercentage * 100) {
                    onPreloadComplete?.invoke()
                    
                    // 移除监听器以避免重复调用
                    downloadManager.removeListener(this)
                }
            }
        })
    }
    
    private fun isUriFullyCached(uri: String): Boolean {
        val key = Util.toUtf8Bytes(uri)
        // 检查是否整个 URI 都已缓存
        return cache.getCachedBytes(key, 0, C.LENGTH_UNSET) != 0
    }
    
    fun createMediaSource(uri: Uri): MediaSource {
        return ProgressiveMediaSource.Factory(cacheDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))
    }
    
    private fun getDownloadCache(): Cache {
        val downloadDirectory = File(context.getExternalFilesDir(null), "downloads")
        return SimpleCache(downloadDirectory, NoOpCacheEvictor())
    }
    
    private fun createDownloadManager(): DownloadManager {
        val databaseProvider = StandaloneDatabaseProvider(context)
        val factory = DefaultHttpDataSource.Factory()
        
        return DownloadManager(
            context,
            databaseProvider,
            cache,
            factory
        )
    }
}
```

### 9.4 分离策略的优势

这种分离下载和播放的策略具有以下优点：

1. **播放流畅性**：播放器直接从本地缓存读取数据，不受网络波动影响
2. **用户体验**：即使在网络较差的情况下也能流畅播放
3. **资源利用**：可以利用空闲时段预下载内容
4. **节省流量**：避免重复下载相同内容
5. **后台操作**：下载过程不影响用户其他操作

通过这种设计，您可以实现一个高效且流畅的视频播放系统，其中下载和播放是两个独立的过程，但又能很好地协同工作。