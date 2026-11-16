import id.ac.um.mivvisecure.RuleBasedAnalyzer

fun main() {
    val analyzer = RuleBasedAnalyzer()
    val fullRiskSample = "Rekening Anda diblokir. Segera verifikasi akun hari ini melalui https://bit.ly/verifikasi-aman dan kirim kode OTP/PIN."
    val fullRisk = analyzer.analyze(fullRiskSample, "Pengirim Tidak Dikenal")
    check(fullRisk.score == 13) { "Expected 13, got ${fullRisk.score}: ${fullRisk.features}" }
    check(fullRisk.category == "PHISHING")

    val safe = analyzer.analyze("Informasi layanan tersedia di https://bca.co.id", "08123456789", setOf("08123456789"))
    check(safe.score == 0 && safe.category == "AMAN") { "Expected safe 0, got ${safe.score}: ${safe.features}" }

    val suspicious = analyzer.analyze("Mohon segera verifikasi akun", "0812", setOf("0812"))
    check(suspicious.score == 3 && suspicious.category == "MENCURIGAKAN") {
        "Expected suspicious 3, got ${suspicious.score}: ${suspicious.features}"
    }

    val obfuscated = analyzer.analyze("Silakan kl1k link sekarang", "", emptySet())
    check(obfuscated.features.first { it.id == "suspicious_keyword" }.detected) {
        "Expected obfuscated keyword normalization to detect kl1k"
    }

    println("PASS: six indicators produce score=13, category=PHISHING")
    println("PASS: trusted sender + whitelisted domain produces AMAN")
    println("PASS: score=3 produces MENCURIGAKAN")
    println("PASS: common obfuscated keyword normalization")
}
