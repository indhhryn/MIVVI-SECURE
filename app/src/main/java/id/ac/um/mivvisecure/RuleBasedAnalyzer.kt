package id.ac.um.mivvisecure

import java.net.URI
import java.util.ArrayDeque
import java.util.Locale

/** Configuration shared by the on-device analyzer and the optional Rules API. */
data class RuleConfig(
    val weights: Map<String, Int> = DEFAULT_WEIGHTS,
    val suspiciousKeywords: List<String> = DEFAULT_KEYWORDS,
    val urgencyPatterns: List<String> = DEFAULT_URGENCY,
    val sensitivePatterns: List<String> = DEFAULT_SENSITIVE,
    val officialDomains: Set<String> = DEFAULT_OFFICIAL_DOMAINS,
    val shortlinkDomains: Set<String> = DEFAULT_SHORTLINK_DOMAINS,
    val safeUpperBound: Int = 3,
    val phishingThreshold: Int = 6,
    val version: String = "1.0.0"
) {
    companion object {
        val DEFAULT_WEIGHTS = mapOf(
            "suspicious_keyword" to 2,
            "url_outside_whitelist" to 3,
            "shortlink" to 2,
            "unknown_sender" to 2,
            "urgency" to 1,
            "sensitive_data" to 3
        )
        val DEFAULT_KEYWORDS = listOf(
            "menang", "hadiah", "verifikasi akun", "klik link", "klik tautan",
            "rekening diblokir", "akun diblokir", "terblokir", "kode otp", "otp",
            "pembaruan data", "konfirmasi akun", "klaim hadiah", "akun anda"
        )
        val DEFAULT_URGENCY = listOf(
            "segera", "1x24 jam", "24 jam", "hari ini", "secepatnya",
            "segera lakukan", "batas waktu", "terakhir hari ini", "jangan sampai terlambat",
            "akan diblokir", "sebelum terlambat"
        )
        val DEFAULT_SENSITIVE = listOf(
            "otp", "pin", "password", "kata sandi", "nomor kartu", "cvv", "kode verifikasi"
        )
        // Seed examples only. Review/update this list from trusted official sources before production use.
        val DEFAULT_OFFICIAL_DOMAINS = setOf(
            "bankmandiri.co.id", "bca.co.id", "bri.co.id", "bni.co.id",
            "bankindonesia.go.id", "pajak.go.id", "bpjs-kesehatan.go.id"
        )
        val DEFAULT_SHORTLINK_DOMAINS = setOf(
            "bit.ly", "tinyurl.com", "t.co", "goo.gl", "cutt.ly", "s.id", "rb.gy", "tiny.cc"
        )
    }
}

data class FeatureResult(
    val id: String,
    val label: String,
    val detected: Boolean,
    val points: Int,
    val explanation: String
)

data class AnalysisResult(
    val score: Int,
    val category: String,
    val level: String,
    val features: List<FeatureResult>,
    val urls: List<String>,
    val ruleVersion: String
)

/** Lightweight deterministic analyzer. No network calls and no ML model are used here. */
class RuleBasedAnalyzer(private val config: RuleConfig = RuleConfig()) {
    private val keywordMatcher = AhoCorasick(config.suspiciousKeywords)
    private val urlRegex = Regex(
        "(?i)(?:https?://|www\\.)[^\\s<>\\\"']+|(?:[a-z0-9-]+\\.)+(?:com|net|org|id|co\\.id|go\\.id|xyz|site|online|info|app|me)(?:/[^\\s<>\\\"']*)?"
    )

    fun analyze(message: String, sender: String = "", trustedSenders: Set<String> = emptySet()): AnalysisResult {
        val raw = message.trim()
        val normalized = normalizeText(raw)
        val tokens = normalized.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        val tokenStream = tokens.joinToString(" ")
        val urls = extractUrls(raw)
        val hosts = urls.mapNotNull(::hostOf)
        val keywordHits = keywordMatcher.find(tokenStream).distinct().sorted()
        val urgencyHits = config.urgencyPatterns.filter { normalized.contains(normalizeText(it)) }
        val sensitiveHits = config.sensitivePatterns.filter { normalized.contains(normalizeText(it)) }

        val outsideHosts = hosts.filterNot { host -> isWhitelisted(host, config.officialDomains) }
        val shortHosts = hosts.filter { host -> isDomainInList(host, config.shortlinkDomains) }
        val normalizedSender = normalizeSender(sender)
        val trusted = trustedSenders.map(::normalizeSender).filter { it.isNotBlank() }.toSet()
        val senderKnown = normalizedSender.isNotBlank() && trusted.contains(normalizedSender)
        val senderUnknown = normalizedSender.isNotBlank() && !senderKnown

        val f = listOf(
            FeatureResult("suspicious_keyword", "Kata kunci mencurigakan", keywordHits.isNotEmpty(), config.weights["suspicious_keyword"] ?: 2,
                if (keywordHits.isEmpty()) "Tidak ditemukan keyword dari daftar deteksi." else "Ditemukan: ${keywordHits.joinToString(", ")}"),
            FeatureResult("url_outside_whitelist", "URL di luar whitelist", outsideHosts.isNotEmpty(), config.weights["url_outside_whitelist"] ?: 3,
                when {
                    urls.isEmpty() -> "Tidak ditemukan URL pada pesan."
                    outsideHosts.isEmpty() -> "Domain yang ditemukan cocok dengan whitelist lokal."
                    else -> "Domain di luar whitelist: ${outsideHosts.distinct().joinToString(", ")}"
                }),
            FeatureResult("shortlink", "Penggunaan shortlink", shortHosts.isNotEmpty(), config.weights["shortlink"] ?: 2,
                if (shortHosts.isEmpty()) "Tidak ditemukan domain shortlink yang dikenal." else "Shortlink terdeteksi: ${shortHosts.distinct().joinToString(", ")}"),
            FeatureResult("unknown_sender", "Pengirim tidak dikenal", senderUnknown, config.weights["unknown_sender"] ?: 2,
                when {
                    sender.isBlank() -> "Identitas pengirim tidak tersedia; indikator tidak dihitung."
                    senderKnown -> "Pengirim tercantum dalam daftar tepercaya lokal."
                    else -> "Pengirim belum ada dalam daftar tepercaya lokal."
                }),
            FeatureResult("urgency", "Pola urgensi", urgencyHits.isNotEmpty(), config.weights["urgency"] ?: 1,
                if (urgencyHits.isEmpty()) "Tidak ditemukan pola urgensi." else "Pola terdeteksi: ${urgencyHits.joinToString(", ")}"),
            FeatureResult("sensitive_data", "Permintaan data sensitif", sensitiveHits.isNotEmpty(), config.weights["sensitive_data"] ?: 3,
                if (sensitiveHits.isEmpty()) "Tidak ditemukan permintaan data sensitif." else "Pola terdeteksi: ${sensitiveHits.joinToString(", ")}")
        )

        val score = f.filter { it.detected }.sumOf { it.points }
        val (category, level) = when {
            score >= config.phishingThreshold -> "PHISHING" to "Risiko Tinggi"
            score >= config.safeUpperBound -> "MENCURIGAKAN" to "Risiko Sedang"
            else -> "AMAN" to "Risiko Rendah"
        }
        return AnalysisResult(score, category, level, f, urls, config.version)
    }

    private fun normalizeText(input: String): String {
        var text = input.lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()
        // Handle common letter substitutions only when surrounded by letters (e.g. kl1k -> klik).
        text = text.replace(Regex("(?<=\\p{L})1(?=\\p{L})"), "i")
            .replace(Regex("(?<=\\p{L})0(?=\\p{L})"), "o")
            .replace(Regex("(?<=\\p{L})@(?=\\p{L})"), "a")
        return text
    }

    private fun extractUrls(text: String): List<String> = urlRegex.findAll(text)
        .map { it.value.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '}', '\u2019', '\u201d') }
        .distinct().toList()

    private fun hostOf(value: String): String? = try {
        val candidate = when {
            value.startsWith("www.", true) -> "https://$value"
            value.startsWith("http://", true) || value.startsWith("https://", true) -> value
            else -> "https://$value"
        }
        URI(candidate).host?.lowercase(Locale.ROOT)?.removePrefix("www.")
    } catch (_: Exception) { null }

    private fun isWhitelisted(host: String, domains: Set<String>): Boolean = isDomainInList(host, domains)

    private fun isDomainInList(host: String, domains: Set<String>): Boolean {
        val h = host.lowercase(Locale.ROOT).removePrefix("www.")
        return domains.any { d ->
            val domain = d.lowercase(Locale.ROOT).removePrefix("www.").trim('.')
            h == domain || h.endsWith(".$domain")
        }
    }

    private fun normalizeSender(sender: String): String = sender.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9+]"), "")
        .let { if (it.startsWith("+62")) "0${it.removePrefix("+62")}" else if (it.startsWith("62")) "0${it.removePrefix("62")}" else it }
}

/** Aho-Corasick multi-pattern matcher for the keyword feature. */
private class AhoCorasick(patterns: List<String>) {
    private class Node {
        val next = HashMap<Char, Node>()
        var failure: Node? = null
        val outputs = mutableListOf<String>()
    }
    private val root = Node()

    init {
        patterns.map { it.lowercase(Locale.ROOT).trim() }.filter { it.isNotEmpty() }.distinct().forEach { pattern ->
            var node = root
            pattern.forEach { ch -> node = node.next.getOrPut(ch) { Node() } }
            node.outputs += pattern
        }
        val queue = ArrayDeque<Node>()
        root.next.values.forEach { it.failure = root; queue.add(it) }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            current.next.forEach { (ch, child) ->
                var fallback = current.failure
                while (fallback != null && fallback !== root && !fallback.next.containsKey(ch)) fallback = fallback.failure
                child.failure = fallback?.next?.get(ch) ?: root
                child.outputs += child.failure?.outputs.orEmpty()
                queue.add(child)
            }
        }
    }

    fun find(text: String): List<String> {
        val found = mutableListOf<String>()
        var state = root
        text.lowercase(Locale.ROOT).forEach { ch ->
            while (state !== root && !state.next.containsKey(ch)) state = state.failure ?: root
            state = state.next[ch] ?: root
            found += state.outputs
        }
        return found
    }
}
