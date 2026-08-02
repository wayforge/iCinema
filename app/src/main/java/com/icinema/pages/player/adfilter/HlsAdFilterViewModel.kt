package com.icinema.pages.player.adfilter

import androidx.lifecycle.ViewModel
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
            HlsAdFilterContract.UiIntent.ClearAll -> clearAll()
            is HlsAdFilterContract.UiIntent.PreviewRule -> previewRule(intent.ruleId)
        }
    }

    private fun loadRules() {
        _uiState.value = HlsAdFilterContract.UiState(
            rules = adRuleStore.loadRules().map { it.toUiItem() }
        )
    }

    private fun deleteRule(ruleId: String) {
        adRuleStore.deleteRule(ruleId)
        loadRules()
        emitEffect(HlsAdFilterContract.UiEffect.ShowMessage("已删除广告规则"))
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
            segmentUrl = segmentUrl,
            matchText = matchText.ifBlank { segmentUrl.substringAfterLast('/') },
            matchType = if (urlPattern == null) "精确片段" else "同类片段",
            createdAtText = DATE_FORMAT.format(Date(createdAtMs))
        )
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
