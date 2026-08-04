package com.icinema.pages.player.adfilter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.icinema.ui.theme.iCinemaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HlsAdFilterActivity : ComponentActivity() {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, HlsAdFilterActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            iCinemaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HlsAdFilterScreen(
                        viewModel = viewModel(),
                        onBack = { finish() },
                        onOpenPreview = { segmentUrl, title ->
                            HlsAdClipPreviewActivity.start(this, segmentUrl, title)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HlsAdFilterScreen(
    viewModel: HlsAdFilterViewModel,
    onBack: () -> Unit,
    onOpenPreview: (String, String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showRuleEditor by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<HlsAdFilterContract.AdRuleItem?>(null) }
    var validatingRule by remember { mutableStateOf<HlsAdFilterContract.AdRuleItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.handleIntent(HlsAdFilterContract.UiIntent.Load)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HlsAdFilterContract.UiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is HlsAdFilterContract.UiEffect.OpenPreview -> onOpenPreview(effect.segmentUrl, effect.title)
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空广告规则") },
            text = { Text("确认清空全部本地广告过滤规则吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.handleIntent(HlsAdFilterContract.UiIntent.ClearAll)
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showRuleEditor) {
        HlsAdRuleEditorDialog(
            rule = editingRule,
            onDismiss = { showRuleEditor = false },
            onSave = { playlistUrl, segmentUrl, urlPattern ->
                viewModel.handleIntent(
                    HlsAdFilterContract.UiIntent.SaveRule(
                        ruleId = editingRule?.id,
                        playlistUrl = playlistUrl,
                        segmentUrl = segmentUrl,
                        urlPattern = urlPattern
                    )
                )
                showRuleEditor = false
            }
        )
    }

    validatingRule?.let { rule ->
        HlsAdRuleValidationDialog(
            rule = rule,
            onDismiss = { validatingRule = null },
            onValidate = { playlistUrl, segmentUrl ->
                viewModel.handleIntent(
                    HlsAdFilterContract.UiIntent.ValidateRule(
                        ruleId = rule.id,
                        playlistUrl = playlistUrl,
                        segmentUrl = segmentUrl
                    )
                )
                validatingRule = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("广告过滤") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        editingRule = null
                        showRuleEditor = true
                    }) {
                        Text("新增")
                    }
                    if (state.rules.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text("清空")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        HlsAdFilterContent(
            state = state,
            onPreview = { viewModel.handleIntent(HlsAdFilterContract.UiIntent.PreviewRule(it)) },
            onEdit = { rule ->
                editingRule = rule
                showRuleEditor = true
            },
            onValidate = { validatingRule = it },
            onPreviewDetectedSegment = {
                viewModel.handleIntent(HlsAdFilterContract.UiIntent.PreviewDetectedSegment(it))
            },
            onSetEnabled = { ruleId, enabled ->
                viewModel.handleIntent(
                    HlsAdFilterContract.UiIntent.SetRuleEnabled(ruleId, enabled)
                )
            },
            onDelete = { viewModel.handleIntent(HlsAdFilterContract.UiIntent.DeleteRule(it)) },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun HlsAdFilterContent(
    state: HlsAdFilterContract.UiState,
    onPreview: (String) -> Unit,
    onEdit: (HlsAdFilterContract.AdRuleItem) -> Unit,
    onValidate: (HlsAdFilterContract.AdRuleItem) -> Unit,
    onPreviewDetectedSegment: (String) -> Unit,
    onSetEnabled: (ruleId: String, enabled: Boolean) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.rules.isEmpty() && state.detectedSegments.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BuiltInAdFilterRulesCard()
            Text(
                text = "暂无广告过滤规则",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "播放时可标记广告，也可在右上角手动新增规则。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var detectedExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            BuiltInAdFilterRulesCard()
        }
        if (state.detectedSegments.isNotEmpty()) {
            item {
                DetectedSegmentsSectionHeader(
                    count = state.detectedSegments.size,
                    expanded = detectedExpanded,
                    onToggle = { detectedExpanded = !detectedExpanded }
                )
            }
            if (detectedExpanded) {
                items(state.detectedSegments, key = { it.id }) { item ->
                    DetectedAdSegmentCard(
                        item = item,
                        onPreview = { onPreviewDetectedSegment(item.id) }
                    )
                }
            }
        }
        if (state.rules.isNotEmpty()) {
            item {
                Text(
                    text = "广告规则",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
        items(state.rules, key = { it.id }) { item ->
            HlsAdRuleCard(
                item = item,
                onPreview = { onPreview(item.id) },
                onEdit = { onEdit(item) },
                onValidate = { onValidate(item) },
                onEnabledChange = { enabled -> onSetEnabled(item.id, enabled) },
                onDelete = { onDelete(item.id) }
            )
        }
    }
}

@Composable
private fun DetectedSegmentsSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "最近识别广告分片",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (expanded) "点击收起 · $count 条" else "点击展开 · $count 条",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BuiltInAdFilterRulesCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "内置候选识别（只读）",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "• 代理按清单顺序全量预取 TS（含广告，保障后续正片可下）\n" +
                    "• 广告须先下载识别；已缓存后播放层提前 seek 跳过\n" +
                    "• 全局内容指纹：相同 TS 可跨视频识别\n" +
                    "• 关闭的规则不参与跳过",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetectedAdSegmentCard(
    item: HlsAdFilterContract.DetectedSegmentItem,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = item.detectedBy.ifBlank { "规则" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = item.matchText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "整段 TS：${item.timeRangeText}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "已识别 ${item.detectedCount} 次 / 最近识别 ${item.lastDetectedAtText}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPreview) {
                    Text("播放分片")
                }
            }
        }
    }
}

@Composable
private fun HlsAdRuleCard(
    item: HlsAdFilterContract.AdRuleItem,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onValidate: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = if (item.enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        text = item.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (item.enabled) "生效" else "停用",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                    Switch(
                        checked = item.enabled,
                        onCheckedChange = onEnabledChange
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listOfNotNull(
                        item.matchType,
                        item.scopeLabel,
                        item.fingerprintShort?.let { "fp:$it" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
                if (!item.enabled) {
                    Text(
                        text = "播放时不参与识别",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = item.matchText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.createdAtText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "播放层已识别 ${item.hitCount} 次" +
                    (item.lastHitAtText?.let { " / 最近识别 $it" }.orEmpty()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("编辑")
                }
                TextButton(onClick = onValidate) {
                    Text("校验")
                }
                TextButton(onClick = onPreview) {
                    Text("播放片段")
                }
                TextButton(onClick = onDelete) {
                    Text("删除")
                }
            }
        }
    }
}

@Composable
private fun HlsAdRuleEditorDialog(
    rule: HlsAdFilterContract.AdRuleItem?,
    onDismiss: () -> Unit,
    onSave: (playlistUrl: String, segmentUrl: String, urlPattern: String?) -> Unit
) {
    var playlistUrl by remember(rule?.id) { mutableStateOf(rule?.playlistUrl.orEmpty()) }
    var segmentUrl by remember(rule?.id) { mutableStateOf(rule?.segmentUrl.orEmpty()) }
    var urlPattern by remember(rule?.id) { mutableStateOf(rule?.urlPattern.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rule == null) "新增广告规则" else "编辑广告规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "本清单 URL 规则仅当前 m3u8；全局指纹可跨视频。开启后：广告仍会下载，识别后播放跳过。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = playlistUrl,
                    onValueChange = { playlistUrl = it },
                    label = { Text("播放清单 URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = segmentUrl,
                    onValueChange = { segmentUrl = it },
                    label = { Text("广告片段 URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = urlPattern,
                    onValueChange = { urlPattern = it },
                    label = { Text("同类片段正则（可选）") },
                    supportingText = { Text("留空时按片段完整 URL、忽略查询参数或文件名匹配") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(playlistUrl, segmentUrl, urlPattern.takeIf { it.isNotBlank() })
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun HlsAdRuleValidationDialog(
    rule: HlsAdFilterContract.AdRuleItem,
    onDismiss: () -> Unit,
    onValidate: (playlistUrl: String, segmentUrl: String) -> Unit
) {
    var playlistUrl by remember(rule.id) { mutableStateOf(rule.playlistUrl) }
    var segmentUrl by remember(rule.id) { mutableStateOf(rule.segmentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("校验广告规则") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "校验这组 URL 是否会命中播放层识别规则。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = playlistUrl,
                    onValueChange = { playlistUrl = it },
                    label = { Text("待校验播放清单 URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = segmentUrl,
                    onValueChange = { segmentUrl = it },
                    label = { Text("待校验片段 URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onValidate(playlistUrl, segmentUrl) }) {
                Text("开始校验")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun HlsAdFilterContentPreview() {
    iCinemaTheme {
        HlsAdFilterContent(
            state = HlsAdFilterContract.UiState(
                rules = listOf(
                    HlsAdFilterContract.AdRuleItem(
                        id = "1",
                        title = "示例影片 - 第1集",
                        subtitle = "约 6.0 秒 / cdn.example.com",
                        playlistUrl = "https://cdn.example.com/ad/index.m3u8",
                        segmentUrl = "https://cdn.example.com/ad/segment_001.ts",
                        urlPattern = "https://cdn\\.example\\.com/ad/segment_\\d+\\.ts",
                        matchText = "segment_001.ts",
                        matchType = "全局内容指纹",
                        createdAtText = "08-02 15:30",
                        hitCount = 12,
                        lastHitAtText = "08-03 10:30",
                        enabled = true,
                        scopeLabel = "可跨视频",
                        fingerprintShort = "a1b2c3d4"
                    )
                )
            ),
            onPreview = {},
            onEdit = {},
            onValidate = {},
            onPreviewDetectedSegment = {},
            onSetEnabled = { _, _ -> },
            onDelete = {}
        )
    }
}
