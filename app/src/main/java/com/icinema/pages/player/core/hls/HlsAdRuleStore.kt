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

    fun matchingAdUrls(playlistUrl: String, segmentUrls: List<String>): Set<String> {
        val rules = loadRules().filter { HlsAdRuleMatcher.appliesToPlaylist(it, playlistUrl) }
        if (rules.isEmpty()) return emptySet()

        return segmentUrls.filterTo(linkedSetOf()) { segmentUrl ->
            rules.any { rule -> HlsAdRuleMatcher.matches(rule, segmentUrl) }
        }
    }

    fun upsertMarkedSegment(
        playlistUrl: String,
        segmentUrl: String,
        durationSeconds: Double?,
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
                videoTitle = videoTitle.ifBlank { existing?.videoTitle.orEmpty() },
                episodeTitle = episodeTitle.ifBlank { existing?.episodeTitle.orEmpty() },
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now
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

    fun clearRules() {
        synchronized(lock) {
            prefs.edit().remove(KEY_RULES).apply()
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

    private companion object {
        private const val PREFS_NAME = "hls_ad_rules"
        private const val KEY_RULES = "rules"
        private val AD_TOKEN_REGEX = Regex(
            """(?i)(^|[_.=-])(ad|ads|adv|advert|vast|vmap|preroll|midroll|postroll|sponsor|commercial)([_.=-]|$)"""
        )
        private val NUMBER_REGEX = Regex("""\d+""")
    }
}
