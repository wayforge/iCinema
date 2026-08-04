package com.icinema.pages.player.adfilter

import androidx.lifecycle.ViewModel
import com.icinema.pages.player.core.hls.HlsAdMatchScope
import com.icinema.pages.player.core.hls.HlsDetectedAdSegment
import com.icinema.pages.player.core.hls.HlsAdRule
import com.icinema.pages.player.core.hls.HlsAdRuleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

@HiltViewModel
class HlsAdFilterViewModel @Inject constructor(
    private val adRuleStore: HlsAdRuleStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(HlsAdFilterContract.UiState())
    val uiState: StateFlow<HlsAdFilterContract.UiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HlsAdFilterContract.UiEffect>()
    val uiEffect: Flow<HlsAdFilterContract.UiEffect> = _uiEffect.receiveAsFlow()

    fun handleIntent(intent: HlsAdFilterContract.UiIntent) {
        when (intent) {
            HlsAdFilterContract.UiIntent.Load -> loadRules()
            is HlsAdFilterContract.UiIntent.DeleteRule -> deleteRule(intent.ruleId)
            is HlsAdFilterContract.UiIntent.SetRuleEnabled -> setRuleEnabled(intent.ruleId, intent.enabled)
            HlsAdFilterContract.UiIntent.ClearAll -> clearAll()
            is HlsAdFilterContract.UiIntent.PreviewRule -> previewRule(intent.ruleId)
            is HlsAdFilterContract.UiIntent.PreviewDetectedSegment -> previewDetectedSegment(intent.segmentId)
            is HlsAdFilterContract.UiIntent.SaveRule -> saveRule(intent)
            is HlsAdFilterContract.UiIntent.ValidateRule -> validateRule(intent)
        }
    }

    private fun loadRules() {
        _uiState.value = HlsAdFilterContract.UiState(
            rules = adRuleStore.loadRules().map { it.toUiItem() },
            detectedSegments = adRuleStore.loadDetectedSegments().map { it.toUiItem() }
        )
    }

    private fun deleteRule(ruleId: String) {
        adRuleStore.deleteRule(ruleId)
        loadRules()
        emitEffect(HlsAdFilterContract.UiEffect.ShowMessage("已删除广告规则"))
    }

    private fun setRuleEnabled(ruleId: String, enabled: Boolean) {
        val ok = adRuleStore.setRuleEnabled(ruleId, enabled)
        if (!ok) {
            emitEffect(HlsAdFilterContract.UiEffect.ShowMessage("广告规则不存在"))
            return
        }
        loadRules()
        emitEffect(
            HlsAdFilterContract.UiEffect.ShowMessage(
                if (enabled) "已启用规则识别" else "已停用规则识别"
            )
        )
    }

    private fun clearAll() {
        adRuleStore.clearRules()
        loadRules()
        emitEffect(HlsAdFilterContract.UiEffect.ShowMessage("已清空广告规则"))
    }

    private fun previewRule(ruleId: String) {
        val item = _uiState.value.rules.firstOrNull { it.id == ruleId } ?: return
        emitEffect(HlsAdFilterContract.UiEffect.OpenPreview(item.segmentUrl, item.title))
    }

    private fun previewDetectedSegment(segmentId: String) {
        val item = _uiState.value.detectedSegments.firstOrNull { it.id == segmentId } ?: return
        emitEffect(HlsAdFilterContract.UiEffect.OpenPreview(item.segmentUrl, item.title))
    }

    private fun saveRule(intent: HlsAdFilterContract.UiIntent.SaveRule) {
        adRuleStore.saveRule(
            ruleId = intent.ruleId,
            playlistUrl = intent.playlistUrl,
            segmentUrl = intent.segmentUrl,
            urlPattern = intent.urlPattern
        ).onSuccess {
            loadRules()
            emitEffect(HlsAdFilterContract.UiEffect.ShowMessage("已保存广告规则"))
        }.onFailure { error ->
            emitEffect(HlsAdFilterContract.UiEffect.ShowMessage(error.message ?: "保存广告规则失败"))
        }
    }

    private fun validateRule(intent: HlsAdFilterContract.UiIntent.ValidateRule) {
        adRuleStore.validateRule(
            ruleId = intent.ruleId,
            playlistUrl = intent.playlistUrl,
            segmentUrl = intent.segmentUrl
        ).onSuccess { result ->
            val message = when {
                result.matches -> "校验通过：开启后会删片或跳过该广告"
                !result.playlistMatches -> "校验未通过：播放清单不匹配"
                else -> "校验未通过：片段地址不匹配"
            }
            emitEffect(HlsAdFilterContract.UiEffect.ShowMessage(message))
        }.onFailure { error ->
            emitEffect(HlsAdFilterContract.UiEffect.ShowMessage(error.message ?: "校验广告规则失败"))
        }
    }

    private fun HlsAdRule.toUiItem(): HlsAdFilterContract.AdRuleItem {
        val title = listOf(videoTitle, episodeTitle)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { "广告片段" }
        val durationText = durationSeconds?.let { "约 ${"%.1f".format(Locale.US, it)} 秒" }
        val subtitle = listOfNotNull(durationText, playlistUrl.hostText())
            .joinToString(" / ")
            .ifBlank { "本地标记规则" }
        return HlsAdFilterContract.AdRuleItem(
            id = id,
            title = title,
            subtitle = subtitle,
            playlistUrl = playlistUrl,
            segmentUrl = segmentUrl,
            urlPattern = urlPattern,
            matchText = matchText.ifBlank { segmentUrl.substringAfterLast('/') },
            matchType = when {
                matchScope == HlsAdMatchScope.GlobalFingerprint && contentSha256 != null -> "全局内容指纹"
                contentSha256 != null -> "内容指纹"
                urlPattern == null -> "精确片段"
                else -> "同类片段"
            },
            createdAtText = DATE_FORMAT.format(Date(createdAtMs)),
            hitCount = hitCount,
            lastHitAtText = lastHitAtMs?.let { DATE_FORMAT.format(Date(it)) },
            enabled = enabled,
            scopeLabel = when {
                matchScope == HlsAdMatchScope.GlobalFingerprint && contentSha256 != null -> "可跨视频"
                else -> "仅本片"
            },
            fingerprintShort = contentSha256?.take(8)
        )
    }

    private fun HlsDetectedAdSegment.toUiItem(): HlsAdFilterContract.DetectedSegmentItem {
        val title = listOf(videoTitle, episodeTitle)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { "识别广告分片" }
        val durationText = durationSeconds?.let { "约 ${"%.1f".format(Locale.US, it)} 秒" }
        val hostText = playlistUrl.hostText()
        val subtitle = listOfNotNull(detectedBy.takeIf { it.isNotBlank() }, durationText, hostText)
            .joinToString(" / ")
            .ifBlank { "广告 TS 分片" }
        return HlsAdFilterContract.DetectedSegmentItem(
            id = id,
            title = title,
            subtitle = subtitle,
            segmentUrl = segmentUrl,
            matchText = segmentUrl.substringBefore('?').substringAfterLast('/'),
            timeRangeText = formatTimeRange(segmentStartPositionMs, segmentEndPositionMs),
            detectedBy = detectedBy,
            detectedCount = detectedCount,
            lastDetectedAtText = DATE_FORMAT.format(Date(lastDetectedAtMs))
        )
    }

    private fun formatTimeRange(startMs: Long?, endMs: Long?): String {
        return when {
            startMs != null && endMs != null -> "${formatDuration(startMs)} - ${formatDuration(endMs)}"
            endMs != null -> "结束 ${formatDuration(endMs)}"
            else -> "时间线待捕获"
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }

    private fun String.hostText(): String? {
        return runCatching { java.net.URI(this).host }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun emitEffect(effect: HlsAdFilterContract.UiEffect) {
        _uiEffect.trySend(effect)
    }

    private companion object {
        private val DATE_FORMAT = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
    }
}
