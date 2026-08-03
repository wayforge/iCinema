package com.icinema.pages.player.core.hls

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HlsAdRuleStore @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    fun loadRules(): List<HlsAdRule> {
        return synchronized(lock) {
            decodeRules()
        }
    }

    fun loadDetectedSegments(): List<HlsDetectedAdSegment> {
        return synchronized(lock) {
            decodeDetectedSegments()
        }
    }

    fun matchingAdUrls(
        playlistUrl: String,
        segmentUrls: List<String>,
        recordHits: Boolean = false
    ): Set<String> {
        val rules = loadRules().filter { HlsAdRuleMatcher.appliesToPlaylist(it, playlistUrl) }
        if (rules.isEmpty()) return emptySet()

        val matchedRuleCounts = mutableMapOf<String, Long>()
        val matchedUrls = segmentUrls.filterTo(linkedSetOf()) { segmentUrl ->
            val matchingRules = rules.filter { rule -> HlsAdRuleMatcher.matches(rule, segmentUrl) }
            matchingRules.forEach { rule ->
                matchedRuleCounts[rule.id] = (matchedRuleCounts[rule.id] ?: 0L) + 1L
            }
            matchingRules.isNotEmpty()
        }
        if (recordHits) {
            recordHits(matchedRuleCounts)
        }
        return matchedUrls
    }

    fun matchingAdRules(
        playlistUrl: String,
        segmentUrl: String,
        contentFingerprint: HlsContentFingerprint? = null,
        recordHits: Boolean = false
    ): List<HlsAdRule> {
        val matchedRules = loadRules()
            .filter { HlsAdRuleMatcher.appliesToPlaylist(it, playlistUrl) }
            .filter { rule ->
                HlsAdRuleMatcher.matches(rule, segmentUrl) ||
                    rule.matchesContent(contentFingerprint)
            }
        if (recordHits) {
            recordHits(matchedRules.associate { it.id to 1L })
        }
        return matchedRules
    }

    fun upsertMarkedSegment(
        playlistUrl: String,
        segmentUrl: String,
        durationSeconds: Double?,
        contentFingerprint: HlsContentFingerprint?,
        videoTitle: String,
        episodeTitle: String
    ): HlsAdRule {
        return synchronized(lock) {
            val now = System.currentTimeMillis()
            val rules = decodeRules().toMutableList()
            val existingIndex = rules.indexOfFirst {
                it.playlistUrl == playlistUrl && it.segmentUrl == segmentUrl
            }
            val existing = rules.getOrNull(existingIndex)
            val rule = HlsAdRule(
                id = existing?.id ?: UUID.randomUUID().toString(),
                playlistUrl = playlistUrl,
                segmentUrl = segmentUrl,
                urlPattern = existing?.urlPattern ?: buildConservativePattern(segmentUrl),
                matchText = buildMatchText(segmentUrl),
                durationSeconds = durationSeconds ?: existing?.durationSeconds,
                contentSha256 = contentFingerprint?.sha256 ?: existing?.contentSha256,
                contentLength = contentFingerprint?.length ?: existing?.contentLength,
                videoTitle = videoTitle.ifBlank { existing?.videoTitle.orEmpty() },
                episodeTitle = episodeTitle.ifBlank { existing?.episodeTitle.orEmpty() },
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now,
                hitCount = existing?.hitCount ?: 0L,
                lastHitAtMs = existing?.lastHitAtMs
            )
            if (existingIndex >= 0) {
                rules[existingIndex] = rule
            } else {
                rules.add(0, rule)
            }
            encodeRules(rules)
            rule
        }
    }

    fun deleteRule(ruleId: String) {
        synchronized(lock) {
            encodeRules(decodeRules().filterNot { it.id == ruleId })
        }
    }

    fun saveRule(
        ruleId: String?,
        playlistUrl: String,
        segmentUrl: String,
        urlPattern: String?
    ): Result<HlsAdRule> {
        return runCatching {
            val normalizedPlaylistUrl = playlistUrl.trim().also {
                require(it.isNotBlank()) { "请输入播放清单地址" }
            }
            val normalizedSegmentUrl = segmentUrl.trim().also {
                require(it.isNotBlank()) { "请输入广告片段地址" }
            }
            val normalizedPattern = urlPattern?.trim()?.takeIf { it.isNotBlank() }
            normalizedPattern?.let(::Regex)

            synchronized(lock) {
                val now = System.currentTimeMillis()
                val rules = decodeRules().toMutableList()
                val existingIndex = ruleId?.let { id -> rules.indexOfFirst { it.id == id } } ?: -1
                val existing = rules.getOrNull(existingIndex)
                val matcherChanged = existing == null ||
                    existing.playlistUrl != normalizedPlaylistUrl ||
                    existing.segmentUrl != normalizedSegmentUrl ||
                    existing.urlPattern != normalizedPattern
                val rule = HlsAdRule(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    playlistUrl = normalizedPlaylistUrl,
                    segmentUrl = normalizedSegmentUrl,
                    urlPattern = normalizedPattern,
                    matchText = buildMatchText(normalizedSegmentUrl),
                    durationSeconds = existing?.durationSeconds,
                    contentSha256 = existing?.contentSha256,
                    contentLength = existing?.contentLength,
                    videoTitle = existing?.videoTitle.orEmpty().ifBlank { "自定义规则" },
                    episodeTitle = existing?.episodeTitle.orEmpty(),
                    createdAtMs = existing?.createdAtMs ?: now,
                    updatedAtMs = now,
                    hitCount = if (matcherChanged) 0L else existing?.hitCount ?: 0L,
                    lastHitAtMs = if (matcherChanged) null else existing?.lastHitAtMs
                )
                if (existingIndex >= 0) {
                    rules[existingIndex] = rule
                } else {
                    rules.add(0, rule)
                }
                encodeRules(rules)
                rule
            }
        }
    }

    private fun HlsAdRule.matchesContent(fingerprint: HlsContentFingerprint?): Boolean {
        if (fingerprint == null) return false
        val ruleHash = contentSha256?.takeIf { it.isNotBlank() } ?: return false
        val ruleLength = contentLength ?: return false
        return ruleHash == fingerprint.sha256 && ruleLength == fingerprint.length
    }

    fun validateRule(
        ruleId: String,
        playlistUrl: String,
        segmentUrl: String
    ): Result<HlsAdRuleValidation> {
        return runCatching {
            val rule = loadRules().firstOrNull { it.id == ruleId }
                ?: throw IllegalArgumentException("广告规则不存在")
            HlsAdRuleValidation(
                playlistMatches = HlsAdRuleMatcher.appliesToPlaylist(rule, playlistUrl.trim()),
                segmentMatches = HlsAdRuleMatcher.matches(rule, segmentUrl.trim())
            )
        }
    }

    fun clearRules() {
        synchronized(lock) {
            prefs.edit()
                .remove(KEY_RULES)
                .remove(KEY_DETECTED_SEGMENTS)
                .apply()
        }
    }

    fun recordDetectedSegment(
        rule: HlsAdRule,
        playlistUrl: String,
        segmentUrl: String,
        segmentStartPositionMs: Long?,
        segmentEndPositionMs: Long?,
        durationSeconds: Double?,
        contentFingerprint: HlsContentFingerprint?,
        videoTitle: String,
        episodeTitle: String,
        detectedBy: String
    ) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val segments = decodeDetectedSegments().toMutableList()
            val existingIndex = segments.indexOfFirst {
                it.playlistUrl == playlistUrl && it.segmentUrl == segmentUrl
            }
            val existing = segments.getOrNull(existingIndex)
            val detectedSegment = HlsDetectedAdSegment(
                id = existing?.id ?: UUID.randomUUID().toString(),
                ruleId = rule.id,
                playlistUrl = playlistUrl,
                segmentUrl = segmentUrl,
                segmentStartPositionMs = segmentStartPositionMs ?: existing?.segmentStartPositionMs,
                segmentEndPositionMs = segmentEndPositionMs ?: existing?.segmentEndPositionMs,
                durationSeconds = durationSeconds ?: existing?.durationSeconds,
                contentSha256 = contentFingerprint?.sha256 ?: existing?.contentSha256,
                contentLength = contentFingerprint?.length ?: existing?.contentLength,
                videoTitle = videoTitle.ifBlank { existing?.videoTitle.orEmpty() },
                episodeTitle = episodeTitle.ifBlank { existing?.episodeTitle.orEmpty() },
                detectedBy = detectedBy.ifBlank { existing?.detectedBy.orEmpty() },
                detectedCount = (existing?.detectedCount ?: 0L) + 1L,
                firstDetectedAtMs = existing?.firstDetectedAtMs ?: now,
                lastDetectedAtMs = now
            )
            if (existingIndex >= 0) {
                segments[existingIndex] = detectedSegment
            } else {
                segments.add(0, detectedSegment)
            }
            encodeDetectedSegments(
                segments
                    .sortedByDescending { it.lastDetectedAtMs }
                    .take(MAX_DETECTED_SEGMENTS)
            )
        }
    }

    private fun recordHits(hitCounts: Map<String, Long>) {
        if (hitCounts.isEmpty()) return
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val rules = decodeRules()
            val updatedRules = rules.map { rule ->
                val count = hitCounts[rule.id] ?: return@map rule
                rule.copy(
                    hitCount = rule.hitCount + count,
                    lastHitAtMs = now
                )
            }
            encodeRules(updatedRules)
        }
    }

    private fun buildConservativePattern(segmentUrl: String): String? {
        val matchText = buildMatchText(segmentUrl).lowercase()
        if (!AD_TOKEN_REGEX.containsMatchIn(matchText)) return null
        val builder = StringBuilder()
        var cursor = 0
        NUMBER_REGEX.findAll(segmentUrl).forEach { match ->
            builder.append(Regex.escape(segmentUrl.substring(cursor, match.range.first)))
            builder.append("""\d+""")
            cursor = match.range.last + 1
        }
        builder.append(Regex.escape(segmentUrl.substring(cursor)))
        return builder.toString()
    }

    private fun buildMatchText(segmentUrl: String): String {
        return segmentUrl.substringBefore('?').substringAfterLast('/')
    }

    private fun decodeRules(): List<HlsAdRule> {
        val json = prefs.getString(KEY_RULES, null).orEmpty()
        if (json.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<HlsAdRule>::class.java)
                ?.toList()
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun encodeRules(rules: List<HlsAdRule>) {
        prefs.edit().putString(KEY_RULES, gson.toJson(rules)).apply()
    }

    private fun decodeDetectedSegments(): List<HlsDetectedAdSegment> {
        val json = prefs.getString(KEY_DETECTED_SEGMENTS, null).orEmpty()
        if (json.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<HlsDetectedAdSegment>::class.java)
                ?.toList()
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun encodeDetectedSegments(segments: List<HlsDetectedAdSegment>) {
        prefs.edit().putString(KEY_DETECTED_SEGMENTS, gson.toJson(segments)).apply()
    }

    private companion object {
        private const val PREFS_NAME = "hls_ad_rules"
        private const val KEY_RULES = "rules"
        private const val KEY_DETECTED_SEGMENTS = "detected_segments"
        private const val MAX_DETECTED_SEGMENTS = 100
        private val AD_TOKEN_REGEX = Regex(
            """(?i)(^|[_.=-])(ad|ads|adv|advert|vast|vmap|preroll|midroll|postroll|sponsor|commercial)([_.=-]|$)"""
        )
        private val NUMBER_REGEX = Regex("""\d+""")
    }
}
