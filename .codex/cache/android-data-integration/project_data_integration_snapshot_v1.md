# Project Data Integration Snapshot

## Summary

- 当前项目继续沿用统一真实数据主链：`ViewModel -> BizPort -> Repository-backed BizPort -> ICmsRepository -> CmsRepositoryImpl -> CmsApiService / Room DAO`。
- `detail` 现已从仅依赖远端详情数据，升级为 `mixed local+remote real` 的真实接入路径：详情基础数据来自 `ICmsRepository.getVideoDetail(videoId)`，最近播放上下文来自 `PlaybackHistoryDao.getLatestHistoryForVideo(videoId)`，两者统一收口到 `DetailBizPort`。
- 详情页的恢复逻辑没有绕过 BizPort 或直接访问 DAO，仍符合当前仓库的数据边界约定。

## How Detail Resume Context Is Wired

1. `DetailViewModel.loadVideo(videoId)` 先调用 `DetailBizPort.loadVideo(videoId)` 获取详情。
2. 同一加载链路中再调用 `DetailBizPort.loadLatestPlayback(videoId)` 获取该视频最近播放记录。
3. `RepositoryDetailBizPort` 将“最近播放记录”继续下沉给 `ICmsRepository.getLatestPlaybackForVideo(videoId)`。
4. `CmsRepositoryImpl` 通过 `PlaybackHistoryDao.getLatestHistoryForVideo(videoId)` 读取本地最近记录，并映射为 `WatchHistoryItem`。
5. `DetailViewModel` 根据已确认业务规则做恢复：
   - 记录有效 -> 恢复对应 source/episode
   - 已看完 -> 若有下一集则跳下一集
   - source/episode 失效 -> 静默回退到首个可用源的首集
6. 恢复结果最终只落在 `UiState`，不使用额外提示 effect。

## Verified Facts

- DAO 新增：`PlaybackHistoryDao.getLatestHistoryForVideo(videoId)`
- Repository 新增：`ICmsRepository.getLatestPlaybackForVideo(videoId)`
- DetailBizPort 新增：`loadLatestPlayback(videoId)`
- DetailViewModel 已在加载详情时消费最近播放上下文，并计算 `preferredSource / preferredEpisode / preferredRange`
- 编译验证通过：`:app:compileDebugKotlin`

## Remaining Validation

- 仍需设备侧确认三条运行时路径：
  - 正常恢复最近 source/episode
  - 已看完后跳下一集
  - source/episode 失效后的静默回退
