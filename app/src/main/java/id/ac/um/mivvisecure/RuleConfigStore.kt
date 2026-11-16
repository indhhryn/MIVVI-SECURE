package id.ac.um.mivvisecure

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RuleConfigStore {
    private const val PREFS = "mivvi_settings"
    private const val CONFIG_KEY = "remote_rule_config"

    fun load(context: Context): RuleConfig {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CONFIG_KEY, null)
            ?: return RuleConfig()
        return try { fromJson(JSONObject(json)) } catch (_: Exception) { RuleConfig() }
    }

    fun save(context: Context, json: JSONObject) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(CONFIG_KEY, json.toString()).apply()
    }

    fun fromJson(json: JSONObject): RuleConfig {
        val defaults = RuleConfig()
        val weightsObject = json.optJSONObject("weights") ?: JSONObject()
        val weights = defaults.weights.mapValues { (key, value) -> weightsObject.optInt(key, value) }
        return RuleConfig(
            weights = weights,
            suspiciousKeywords = json.optStringArray("keywords", defaults.suspiciousKeywords),
            urgencyPatterns = json.optStringArray("urgency_patterns", defaults.urgencyPatterns),
            sensitivePatterns = json.optStringArray("sensitive_patterns", defaults.sensitivePatterns),
            officialDomains = json.optStringArray("official_domains", defaults.officialDomains.toList()).map { it.lowercase() }.toSet(),
            shortlinkDomains = json.optStringArray("shortlink_domains", defaults.shortlinkDomains.toList()).map { it.lowercase() }.toSet(),
            safeUpperBound = json.optJSONObject("thresholds")?.optInt("suspicious_from", defaults.safeUpperBound) ?: defaults.safeUpperBound,
            phishingThreshold = json.optJSONObject("thresholds")?.optInt("phishing_from", defaults.phishingThreshold) ?: defaults.phishingThreshold,
            version = json.optString("version", defaults.version)
        )
    }

    private fun JSONObject.optStringArray(key: String, fallback: List<String>): List<String> {
        val array: JSONArray = optJSONArray(key) ?: return fallback
        return buildList { for (i in 0 until array.length()) array.optString(i).takeIf { it.isNotBlank() }?.let(::add) }
            .ifEmpty { fallback }
    }
}
