package com.icinema.pages.player.core.hls

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
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
        val rules = loadRules().filter { it.enabled }
        val playlistRules = rules.filter { HlsAdRuleMatcher.appliesToPlaylist(it, playlistUrl) }
        val knownUrls = synchronized(lock) { decodeKnownAdUrls() }
            .associateBy { normalizeUrlKey(it.segmentUrl) }

        val matchedRuleCounts = mutableMapOf<String, Long>()
        val matchedUrls = linkedSetOf<String>()
        segmentUrls.forEach { segmentUrl ->
            val byPlaylist = playlistRules.filter { HlsAdRuleMatcher.matches(it, segmentUrl) }
            byPlaylist.forEach { rule ->
                matchedRuleCounts[rule.id] = (matchedRuleCounts[rule.id] ?: 0L) + 1L
            }
            val known = knownUrls[normalizeUrlKey(segmentUrl)]
            val knownRuleEnabled = known?.let { entry ->
                rules.any { it.id == entry.ruleId && it.enabled }
            } == true
            if (byPlaylist.isNotEmpty() || knownRuleEnabled) {
                matchedUrls.add(segmentUrl)
                known?.ruleId?.let { id ->
                    matchedRuleCounts[id] = (matchedRuleCounts[id] ?: 0L) + 1L
                }
            }
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
        val rules = loadRules().filter { it.enabled }
        val playlistMatched = rules
            .filter { HlsAdRuleMatcher.appliesToPlaylist(it, playlistUrl) }
            .filter { HlsAdRuleMatcher.matches(it, segmentUrl) || it.matchesContent(contentFingerprint) }

        val knownMatched = synchronized(lock) {
            val known = decodeKnownAdUrls().firstOrNull {
                normalizeUrlKey(it.segmentUrl) == normalizeUrlKey(segmentUrl)
            } ?: return@synchronized emptyList()
            rules.filter { it.id == known.ruleId }
        }

        val fingerprintMatched = if (contentFingerprint == null) {
            emptyList()
        } else {
            rules.filter {
                it.matchScope == HlsAdMatchScope.GlobalFingerprint &&
                    it.matchesContent(contentFingerprint)
            }
        }

        val matchedRules = (playlistMatched + knownMatched + fingerprintMatched)
            .distinctBy { it.id }

        if (matchedRules.isNotEmpty()) {
            // Learn this URL for future proxy stripping across playlists.
            if (
                contentFingerprint != null ||
                matchedRules.any { it.matchScope == HlsAdMatchScope.GlobalFingerprint }
            ) {
                rememberKnownAdUrl(
                    segmentUrl = segmentUrl,
                    rule = matchedRules.first(),
                    contentFingerprint = contentFingerprint
                        ?: matchedRules.firstOrNull { it.hasContentFingerprint }?.let {
                            HlsContentFingerprint(it.contentSha256!!, it.contentLength!!)
                        }
                )
            }
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
            val fingerprint = contentFingerprint
                ?: existing?.takeIf { it.hasContentFingerprint }?.let {
                    HlsContentFingerprint(it.contentSha256!!, it.contentLength!!)
                }
            val scope = when {
                fingerprint != null -> HlsAdMatchScope.GlobalFingerprint
                existing?.matchScope == HlsAdMatchScope.GlobalFingerprint && existing.hasContentFingerprint ->
                    HlsAdMatchScope.GlobalFingerprint
                else -> HlsAdMatchScope.Playlist
            }
            val rule = HlsAdRule(
                id = existing?.id ?: UUID.randomUUID().toString(),
                playlistUrl = playlistUrl,
                segmentUrl = segmentUrl,
                urlPattern = existing?.urlPattern ?: buildConservativePattern(segmentUrl),
                matchText = buildMatchText(segmentUrl),
                durationSeconds = durationSeconds ?: existing?.durationSeconds,
                contentSha256 = fingerprint?.sha256 ?: existing?.contentSha256,
                contentLength = fingerprint?.length ?: existing?.contentLength,
                videoTitle = videoTitle.ifBlank { existing?.videoTitle.orEmpty() },
                episodeTitle = episodeTitle.ifBlank { existing?.episodeTitle.orEmpty() },
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now,
                hitCount = existing?.hitCount ?: 0L,
                lastHitAtMs = existing?.lastHitAtMs,
                enabled = existing?.enabled ?: true,
                matchScope = scope
            )
            if (existingIndex >= 0) {
                rules[existingIndex] = rule
            } else {
                rules.add(0, rule)
            }
            encodeRules(rules)
            if (rule.hasContentFingerprint) {
                rememberKnownAdUrlLocked(
                    segmentUrl = segmentUrl,
                    ruleId = rule.id,
                    contentFingerprint = HlsContentFingerprint(rule.contentSha256!!, rule.contentLength!!)
                )
            }
            rule
        }
    }

    fun rememberKnownAdUrl(
        segmentUrl: String,
        rule: HlsAdRule,
        contentFingerprint: HlsContentFingerprint?
    ) {
        synchronized(lock) {
            rememberKnownAdUrlLocked(segmentUrl, rule.id, contentFingerprint)
        }
    }

    fun deleteRule(ruleId: String) {
        synchronized(lock) {
            encodeRules(decodeRules().filterNot { it.id == ruleId })
            encodeKnownAdUrls(decodeKnownAdUrls().filterNot { it.ruleId == ruleId })
        }
    }

    fun setRuleEnabled(ruleId: String, enabled: Boolean): Boolean {
        return synchronized(lock) {
            val rules = decodeRules().toMutableList()
            val index = rules.indexOfFirst { it.id == ruleId }
            if (index < 0) return@synchronized false
            val existing = rules[index]
            if (existing.enabled == enabled) return@synchronized true
            rules[index] = existing.copy(
                enabled = enabled,
                updatedAtMs = System.currentTimeMillis()
            )
            encodeRules(rules)
            true
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
                    lastHitAtMs = if (matcherChanged) null else existing?.lastHitAtMs,
                    enabled = existing?.enabled ?: true,
                    matchScope = when {
                        existing?.hasContentFingerprint == true ->
                            existing.matchScope.takeIf { it == HlsAdMatchScope.GlobalFingerprint }
                                ?: HlsAdMatchScope.GlobalFingerprint
                        else -> HlsAdMatchScope.Playlist
                    }
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
                playlistMatches = HlsAdRuleMatcher.appliesToPlaylist(rule, playlistUrl.trim()) ||
                    rule.matchScope == HlsAdMatchScope.GlobalFingerprint,
                segmentMatches = HlsAdRuleMatcher.matches(rule, segmentUrl.trim()) ||
                    (rule.matchScope == HlsAdMatchScope.GlobalFingerprint && rule.hasContentFingerprint)
            )
        }
    }

    fun clearRules() {
        synchronized(lock) {
            prefs.edit()
                .remove(KEY_RULES)
                .remove(KEY_DETECTED_SEGMENTS)
                .remove(KEY_KNOWN_AD_URLS)
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
            if (rule.matchScope == HlsAdMatchScope.GlobalFingerprint || contentFingerprint != null) {
                rememberKnownAdUrlLocked(
                    segmentUrl = segmentUrl,
                    ruleId = rule.id,
                    contentFingerprint = contentFingerprint
                        ?: rule.takeIf { it.hasContentFingerprint }?.let {
                            HlsContentFingerprint(it.contentSha256!!, it.contentLength!!)
                        }
                )
            }
        }
    }

    private fun rememberKnownAdUrlLocked(
        segmentUrl: String,
        ruleId: String,
        contentFingerprint: HlsContentFingerprint?
    ) {
        val key = normalizeUrlKey(segmentUrl)
        if (key.isBlank()) return
        val now = System.currentTimeMillis()
        val current = decodeKnownAdUrls().toMutableList()
        val index = current.indexOfFirst { normalizeUrlKey(it.segmentUrl) == key }
        val entry = HlsKnownAdUrl(
            segmentUrl = segmentUrl.substringBefore('?'),
            ruleId = ruleId,
            contentSha256 = contentFingerprint?.sha256,
            contentLength = contentFingerprint?.length,
            updatedAtMs = now
        )
        if (index >= 0) {
            current[index] = entry
        } else {
            current.add(0, entry)
        }
        encodeKnownAdUrls(
            current
                .sortedByDescending { it.updatedAtMs }
                .take(MAX_KNOWN_AD_URLS)
        )
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

    private fun normalizeUrlKey(url: String): String {
        return url.trim().substringBefore('?')
    }

    private fun decodeRules(): List<HlsAdRule> {
        val json = prefs.getString(KEY_RULES, null).orEmpty()
        if (json.isBlank()) return emptyList()
        return runCatching {
            @Suppress("DEPRECATION")
            val root = JsonParser().parse(json)
            if (!root.isJsonArray) return@runCatching emptyList()
            root.asJsonArray.mapNotNull { element ->
                if (!element.isJsonObject) return@mapNotNull null
                val obj = element.asJsonObject
                val rule = gson.fromJson(obj, HlsAdRule::class.java) ?: return@mapNotNull null
                var normalized = rule
                if (!obj.has("enabled")) {
                    normalized = normalized.copy(enabled = true)
                }
                if (!obj.has("matchScope")) {
                    normalized = normalized.copy(
                        matchScope = if (normalized.hasContentFingerprint) {
                            HlsAdMatchScope.GlobalFingerprint
                        } else {
                            HlsAdMatchScope.Playlist
                        }
                    )
                }
                normalized
            }
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

    private fun decodeKnownAdUrls(): List<HlsKnownAdUrl> {
        val json = prefs.getString(KEY_KNOWN_AD_URLS, null).orEmpty()
        if (json.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<HlsKnownAdUrl>::class.java)
                ?.toList()
                .orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun encodeKnownAdUrls(urls: List<HlsKnownAdUrl>) {
        prefs.edit().putString(KEY_KNOWN_AD_URLS, gson.toJson(urls)).apply()
    }

    private companion object {
        private const val PREFS_NAME = "hls_ad_rules"
        private const val KEY_RULES = "rules"
        private const val KEY_DETECTED_SEGMENTS = "detected_segments"
        private const val KEY_KNOWN_AD_URLS = "known_ad_urls"
        private const val MAX_DETECTED_SEGMENTS = 100
        private const val MAX_KNOWN_AD_URLS = 500
        private val AD_TOKEN_REGEX = Regex(
            """(?i)(^|[_.=-])(ad|ads|adv|advert|vast|vmap|preroll|midroll|postroll|sponsor|commercial)([_.=-]|$)"""
        )
        private val NUMBER_REGEX = Regex("""\d+""")
    }
}
