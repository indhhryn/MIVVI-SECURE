package id.ac.um.mivvisecure

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val cosmic = Color.rgb(8, 10, 24)
    private val navy = Color.rgb(16, 22, 45)
    private val cardColor = Color.rgb(26, 33, 70)
    private val violet = Color.rgb(104, 82, 232)
    private val cyan = Color.rgb(99, 221, 232)
    private val pearl = Color.rgb(245, 245, 252)
    private val muted = Color.rgb(168, 176, 200)
    private val green = Color.rgb(45, 212, 191)
    private val amber = Color.rgb(245, 158, 11)
    private val red = Color.rgb(244, 63, 94)
    private var currentPage = "welcome"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = cosmic
        window.navigationBarColor = cosmic
        showWelcome()
    }

    override fun onResume() {
        super.onResume()
        if (currentPage == "home") showHome()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun gradientBackground(): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TL_BR, intArrayOf(cosmic, navy, Color.rgb(22, 25, 62))
    )

    private fun createPage(title: String, showNav: Boolean = true): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradientBackground()
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(16), dp(22), dp(8))
        }
        val brand = TextView(this).apply {
            text = "✦  MIVVI SECURE"
            setTextColor(cyan)
            textSize = 13f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        top.addView(brand)
        if (title.isNotBlank()) {
            val heading = TextView(this).apply {
                text = title
                setTextColor(pearl)
                textSize = 25f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setPadding(0, dp(8), 0, dp(4))
            }
            top.addView(heading)
        }
        root.addView(top)
        currentPage = when (title) {
            "Beranda" -> "home"
            "Analisis Manual" -> "manual"
            "Aktivitas Notifikasi" -> "activities"
            "Riwayat Analisis" -> "history"
            "Edukasi Keamanan" -> "education"
            "Pengaturan" -> "settings"
            else -> currentPage
        }
        val scroll = ScrollView(this).apply { isFillViewport = false; clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), dp(22))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        if (showNav) root.addView(buildBottomNav())
        setContentView(root)
        return content
    }

    private fun buildBottomNav(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(8), dp(6), dp(8))
            setBackgroundColor(Color.rgb(10, 14, 32))
        }
        listOf("Beranda", "Aktivitas", "Riwayat", "Edukasi", "Pengaturan").forEach { label ->
            val b = TextView(this).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(if (currentPage == when (label) {
                        "Beranda" -> "home"; "Aktivitas" -> "activities"; "Riwayat" -> "history";
                        "Edukasi" -> "education"; else -> "settings"
                    }) cyan else muted)
                setPadding(dp(4), dp(8), dp(4), dp(8))
                setOnClickListener {
                    when (label) {
                        "Beranda" -> showHome()
                        "Aktivitas" -> showActivities()
                        "Riwayat" -> showHistory()
                        "Edukasi" -> showEducation()
                        else -> showSettings()
                    }
                }
            }
            bar.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        return bar
    }

    private fun text(parent: LinearLayout, value: String, size: Float = 14f, color: Int = pearl,
                     bold: Boolean = false, top: Int = 0, bottom: Int = 6): TextView {
        val view = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            setPadding(0, dp(top), 0, dp(bottom))
        }
        parent.addView(view)
        return view
    }

    private fun card(parent: LinearLayout, title: String? = null, description: String? = null): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.argb(80, 150, 160, 210))
            }
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(12)
        parent.addView(box, lp)
        if (title != null) text(box, title, 16f, pearl, true, bottom = 5)
        if (description != null) text(box, description, 13f, muted, bottom = 0)
        return box
    }

    private fun action(parent: LinearLayout, label: String, onClick: () -> Unit, primary: Boolean = true): Button {
        val b = Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (primary) cosmic else pearl)
            background = GradientDrawable().apply {
                setColor(if (primary) cyan else Color.rgb(42, 48, 83))
                cornerRadius = dp(12).toFloat()
                if (!primary) setStroke(dp(1), Color.argb(100, 160, 170, 220))
            }
            minHeight = dp(48)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { onClick() }
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(10)
        parent.addView(b, lp)
        return b
    }

    private fun input(parent: LinearLayout, hint: String, multiline: Boolean = false, password: Boolean = false): EditText {
        val edit = EditText(this).apply {
            this.hint = hint
            setHintTextColor(muted)
            setTextColor(pearl)
            textSize = 14f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.rgb(15, 22, 44))
                cornerRadius = dp(10).toFloat()
                setStroke(dp(1), Color.argb(100, 160, 170, 220))
            }
            if (password) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else inputType = if (multiline) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                else InputType.TYPE_CLASS_TEXT
            if (multiline) { minLines = 5; gravity = Gravity.TOP; setSingleLine(false) } else setSingleLine(true)
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, if (multiline) dp(140) else dp(50))
        lp.bottomMargin = dp(12)
        parent.addView(edit, lp)
        return edit
    }

    private fun showWelcome() {
        currentPage = "welcome"
        val body = createPage("Your messages. Your safety.", false)
        text(body, "Kenali risiko. Lindungi dirimu.", 15f, muted, bottom = 22)
        val hero = card(body, "Perlindungan dari indikasi phishing", "Analisis pesan SMS dan WhatsApp menggunakan Rule-Based Risk Scoring yang ringan dan berjalan di perangkat.")
        text(hero, "SMS  •  WHATSAPP  •  LOCAL ANALYSIS", 10f, cyan, true, top = 12, bottom = 0)
        text(body, "Masuk ke MIVVI SECURE", 18f, pearl, true, top = 12)
        action(body, "Sign In", { showLogin() })
        action(body, "Create Account", { showSignup() }, false)
        action(body, "Continue as Guest", { showHome() }, false)
        text(body, "Akun pada prototipe ini belum terhubung ke layanan autentikasi. Kamu tetap dapat menggunakan fitur analisis melalui mode tamu.", 12f, muted, top = 12)
        text(body, "PROTOTYPE 1.0  •  RULE-BASED RISK SCORING", 10f, muted, top = 22)
    }

    private fun showLogin() {
        val body = createPage("Sign In", false)
        text(body, "Masuk ke akun MIVVI SECURE.", 14f, muted, bottom = 18)
        input(body, "Email address")
        input(body, "Password", password = true)
        action(body, "Forgot Password?", { showForgotPassword() }, false)
        action(body, "Sign In", {
            AlertDialog.Builder(this).setTitle("Autentikasi belum tersedia")
                .setMessage("Halaman login sudah tersedia sebagai prototipe UI, tetapi layanan akun belum dihubungkan. Lanjutkan menggunakan mode tamu.")
                .setPositiveButton("Continue as Guest") { _, _ -> showHome() }.setNegativeButton("Kembali", null).show()
        })
        action(body, "Create Account", { showSignup() }, false)
        action(body, "Continue as Guest", { showHome() }, false)
    }

    private fun showSignup() {
        val body = createPage("Create Account", false)
        text(body, "Buat akun untuk pengalaman keamanan digitalmu.", 14f, muted, bottom = 18)
        input(body, "Full name")
        input(body, "Email address")
        input(body, "Password", password = true)
        input(body, "Confirm password", password = true)
        val agree = CheckBox(this).apply { text = "Saya menyetujui Terms & Privacy Policy (demo)"; setTextColor(pearl) }
        body.addView(agree)
        action(body, "Create Account", {
            if (!agree.isChecked) Toast.makeText(this, "Centang persetujuan terlebih dahulu.", Toast.LENGTH_SHORT).show()
            else AlertDialog.Builder(this).setTitle("Pendaftaran demo")
                .setMessage("Pendaftaran akun belum terhubung ke backend. Lanjutkan ke aplikasi sebagai tamu.")
                .setPositiveButton("Lanjutkan") { _, _ -> showHome() }.setNegativeButton("Batal", null).show()
        })
        action(body, "Already have an account? Sign In", { showLogin() }, false)
    }

    private fun showForgotPassword() {
        val body = createPage("Forgot Password", false)
        text(body, "Masukkan email akun untuk memulai pemulihan kata sandi.", 14f, muted, bottom = 18)
        input(body, "Email address")
        action(body, "Send Reset Link", {
            AlertDialog.Builder(this).setTitle("Layanan belum terhubung")
                .setMessage("Permintaan reset password belum dikirim karena API autentikasi belum tersedia.")
                .setPositiveButton("Mengerti", null).show()
        })
        action(body, "Back to Sign In", { showLogin() }, false)
    }

    private fun showHome() {
        val body = createPage("Beranda")
        text(body, "Kenali risiko. Lindungi dirimu.", 14f, muted, bottom = 14)
        val enabled = AppPrefs.notificationAccessEnabled(this)
        val access = card(body, if (enabled) "Akses notifikasi aktif" else "Akses notifikasi belum diaktifkan",
            if (enabled) "MIVVI SECURE dapat memproses pratinjau notifikasi dari sumber yang didukung dan dipilih." else "Aktifkan Notification Access agar aplikasi dapat membaca pratinjau notifikasi SMS dan WhatsApp yang tersedia.")
        text(access, if (enabled) "STATUS: ENABLED" else "STATUS: NOT ENABLED", 11f, if (enabled) green else amber, true, top = 10, bottom = 0)
        action(body, if (enabled) "Kelola Akses Notifikasi" else "Hubungkan Notifikasi", { openNotificationAccess() }, false)

        val total = dbCount()
        val high = dbHighRisk()
        val stats = card(body, "Ringkasan Aktivitas", "Data dihitung dari analisis yang tersimpan pada perangkat ini.")
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        stats.addView(row)
        statCell(row, "TOTAL ANALISIS", total.toString())
        statCell(row, "PHISHING", high.toString())

        action(body, "Analisis Pesan Manual", { showManual() })
        action(body, "Lihat Aktivitas Notifikasi", { showActivities() }, false)

        text(body, "Aktivitas Terbaru", 17f, pearl, true, top = 14)
        val latest = dbRecent(3)
        if (latest.isEmpty()) text(body, "Belum ada aktivitas. Coba analisis manual atau aktifkan akses notifikasi.", 13f, muted)
        latest.forEach { record -> recordCard(body, record) }
        text(body, "Pemrosesan lokal • Rule-Based Risk Scoring • Tidak mengirim isi pesan ke API", 11f, muted, top = 12)
    }

    private fun statCell(parent: LinearLayout, label: String, value: String) {
        val cell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, 0) }
        parent.addView(cell, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        text(cell, value, 25f, cyan, true, bottom = 1)
        text(cell, label, 10f, muted, true, bottom = 0)
    }

    private fun showManual(prefill: String? = null) {
        val body = createPage("Analisis Manual")
        text(body, "Masukkan isi pesan untuk memeriksa indikator risiko phishing.", 13f, muted, bottom = 14)
        text(body, "Sumber pesan", 12f, muted, true, bottom = 5)
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("SMS", "WhatsApp"))
        body.addView(spinner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))
        text(body, "Identitas pengirim (opsional)", 12f, muted, true, top = 10, bottom = 5)
        val sender = input(body, "Contoh: 0812xxxx atau nama kontak")
        text(body, "Isi pesan", 12f, muted, true, bottom = 5)
        val message = input(body, "Tempel isi SMS atau WhatsApp di sini", multiline = true)
        if (prefill != null) {
            message.setText(prefill)
            sender.setText("Pengirim Tidak Dikenal")
        }
        var isDemo = false
        action(body, "Isi Contoh DEMO (Skor 13)", {
            message.setText("Rekening Anda diblokir. Segera verifikasi akun hari ini melalui https://bit.ly/verifikasi-aman dan kirim kode OTP/PIN.")
            sender.setText("Pengirim Tidak Dikenal")
            isDemo = true
        }, false)
        action(body, "Analisis Pesan", {
            val content = message.text.toString().trim()
            if (content.isBlank()) {
                Toast.makeText(this, "Isi pesan belum dimasukkan.", Toast.LENGTH_SHORT).show()
                return@action
            }
            val source = spinner.selectedItem.toString().uppercase(Locale.ROOT)
            val result = RuleBasedAnalyzer(RuleConfigStore.load(this)).analyze(content, sender.text.toString(), AppPrefs.trustedSenders(this))
            val id = LocalDatabase(this).use { it.insert(source, sender.text.toString(), content, System.currentTimeMillis(), result, isDemo) }
            showResult(content, sender.text.toString(), source, result, id, isDemo)
        })
        text(body, "Catatan: isi pesan disimpan pada database lokal aplikasi. Pesan tidak dikirim ke server API.", 11f, muted, top = 10)
    }

    private fun showResult(message: String, sender: String, source: String, result: AnalysisResult, recordId: Long, demo: Boolean) {
        val body = createPage("Hasil Analisis")
        val statusColor = when (result.category) { "PHISHING" -> red; "MENCURIGAKAN" -> amber; else -> green }
        val summary = card(body, "${result.category}  •  ${result.level}", "Hasil Rule-Based Risk Scoring")
        text(summary, result.score.toString(), 44f, statusColor, true, top = 8, bottom = 0)
        text(summary, "SKOR RISIKO", 10f, muted, true, bottom = 4)
        text(summary, "Batas klasifikasi: AMAN 0–2 • MENCURIGAKAN 3–5 • PHISHING ≥6", 11f, muted, bottom = 0)
        if (demo) text(body, "DEMO — contoh pesan sintetis", 11f, amber, true, bottom = 8)
        val sourceCard = card(body, "Informasi Pesan", "Sumber: $source\nPengirim: ${sender.ifBlank { "Tidak tersedia" }}")
        text(sourceCard, message.take(500), 12f, pearl, top = 10, bottom = 0)
        text(body, "Indikator Penilaian", 17f, pearl, true, top = 10)
        result.features.forEach { feature ->
            val row = card(body, "${if (feature.detected) "●" else "○"}  ${feature.label}   ${if (feature.detected) "+${feature.points}" else "+0"}", feature.explanation)
            text(row, if (feature.detected) "TERDETEKSI" else "TIDAK TERDETEKSI", 10f, if (feature.detected) statusColor else muted, true, top = 8, bottom = 0)
        }
        val advice = when (result.category) {
            "PHISHING" -> "Jangan buka tautan atau memberikan OTP, PIN, maupun kata sandi. Verifikasi melalui aplikasi atau kanal resmi yang kamu buka sendiri."
            "MENCURIGAKAN" -> "Periksa identitas pengirim dan domain melalui kanal resmi. Jangan terburu-buru mengikuti instruksi pesan."
            else -> "Tidak ditemukan indikator kuat berdasarkan aturan saat ini. Tetap periksa konteks dan identitas pengirim sebelum bertindak."
        }
        card(body, "Rekomendasi", advice)
        action(body, "Hapus Catatan Analisis", {
            AlertDialog.Builder(this).setTitle("Hapus catatan ini?")
                .setMessage("Catatan analisis ini akan dihapus dari penyimpanan lokal.")
                .setNegativeButton("Batal", null).setPositiveButton("Hapus") { _, _ ->
                    LocalDatabase(this).use { it.delete(recordId) }
                    showHistory()
                }.show()
        }, false)
        action(body, "Kembali ke Beranda", { showHome() }, false)
        action(body, "Lihat Riwayat Analisis", { showHistory() })
    }

    private fun showActivities() = showRecordList("Aktivitas Notifikasi", null)
    private fun showHistory() = showRecordList("Riwayat Analisis", null)

    private fun showRecordList(title: String, filter: String?) {
        val body = createPage(title)
        text(body, "Riwayat yang tersimpan secara lokal di perangkat ini.", 13f, muted, bottom = 12)
        val records = dbRecent(150).filter { filter == null || it.source == filter }
        if (records.isEmpty()) {
            val empty = card(body, "Belum ada aktivitas", "Aktivitas akan muncul setelah analisis manual atau notifikasi SMS/WhatsApp tersedia.")
            text(empty, "MODE LOKAL", 10f, cyan, true, top = 8, bottom = 0)
            action(body, "Mulai Analisis Manual", { showManual() })
        } else records.forEach { recordCard(body, record) }
        action(body, "Hapus Semua Riwayat", {
            AlertDialog.Builder(this).setTitle("Hapus seluruh riwayat?")
                .setMessage("Tindakan ini menghapus data analisis dari database lokal dan tidak dapat dibatalkan.")
                .setNegativeButton("Batal", null).setPositiveButton("Hapus") { _, _ ->
                    LocalDatabase(this).use { it.clear() }
                    Toast.makeText(this, "Riwayat dihapus.", Toast.LENGTH_SHORT).show()
                    showRecordList(title, filter)
                }.show()
        }, false)
    }

    private fun recordCard(parent: LinearLayout, record: AnalysisRecord) {
        val color = when (record.category) { "PHISHING" -> red; "MENCURIGAKAN" -> amber; else -> green }
        val box = card(parent, "${record.source}  •  ${record.category}", record.sender.ifBlank { "Pengirim tidak tersedia" })
        val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(record.receivedAt))
        text(box, if (record.body.isBlank()) "Konten notifikasi tidak tersedia" else record.body.take(130), 12f, muted, top = 6)
        text(box, "Skor ${record.score}  •  $stamp${if (record.isDemo) "  •  DEMO" else ""}", 10f, color, true, bottom = 0)
        box.setOnClickListener { showRecordDetail(record) }
        box.isClickable = true
    }

    private fun showRecordDetail(record: AnalysisRecord) {
        val body = createPage("Detail Riwayat")
        if (record.body.isBlank()) {
            card(body, "Konten notifikasi tidak tersedia", "Android atau aplikasi sumber tidak menampilkan pratinjau isi pesan. MIVVI SECURE tidak membuat atau menebak isi pesan.")
            card(body, "Metadata", "Sumber: ${record.source}\nPengirim: ${record.sender.ifBlank { "Tidak tersedia" }}\nWaktu: ${formatTime(record.receivedAt)}")
        } else {
            val result = RuleBasedAnalyzer(RuleConfigStore.load(this)).analyze(record.body, record.sender, AppPrefs.trustedSenders(this))
            showResult(record.body, record.sender, record.source, result, record.id, record.isDemo)
            return
        }
        action(body, "Hapus Catatan Ini", {
            AlertDialog.Builder(this).setTitle("Hapus catatan?").setMessage("Catatan ini akan dihapus dari riwayat lokal.")
                .setNegativeButton("Batal", null).setPositiveButton("Hapus") { _, _ ->
                    LocalDatabase(this).use { it.delete(record.id) }
                    showHistory()
                }.show()
        }, false)
        action(body, "Kembali", { showHistory() })
    }

    private fun formatTime(time: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(time))

    private fun showEducation() {
        val body = createPage("Edukasi Keamanan")
        text(body, "Pelajari cara mengenali pesan mencurigakan dan melindungi diri dari phishing.", 13f, muted, bottom = 14)
        val featured = card(body, "Kenali Phishing Sebelum Terlambat", "Pelajari tanda-tanda pesan yang mencoba menipu, mencuri informasi pribadi, atau mengarahkan pengguna ke situs berbahaya.")
        text(featured, "ARTIKEL UNGGULAN  →", 10f, cyan, true, top = 12, bottom = 0)
        featured.setOnClickListener { showArticle("Kenali Phishing Sebelum Terlambat") }
        val topics = listOf(
            "Ciri-Ciri Pesan Phishing" to "Kenali tautan mencurigakan, permintaan data sensitif, dan informasi yang tidak wajar.",
            "Waspadai Pesan Mendesak" to "Penipu dapat memakai ancaman, hadiah, atau batas waktu agar pengguna terburu-buru.",
            "Periksa Tautan Sebelum Membuka" to "Periksa ejaan domain dan waspadai shortlink atau URL yang disamarkan.",
            "Lindungi Data Pribadi" to "Jangan berikan PIN, kata sandi, OTP, atau data pembayaran kepada pihak yang tidak tepercaya.",
            "Langkah Saat Menemukan Pesan Mencurigakan" to "Jangan klik tautan. Verifikasi melalui kanal resmi dan laporkan bila diperlukan."
        )
        topics.forEach { (title, desc) ->
            val c = card(body, title, desc)
            text(c, "BACA SELENGKAPNYA  →", 10f, cyan, true, top = 10, bottom = 0)
            c.setOnClickListener { showArticle(title) }
        }
        action(body, "Pusat Bantuan & FAQ", {
            AlertDialog.Builder(this).setTitle("Pusat Bantuan & FAQ")
                .setMessage("MIVVI SECURE menggunakan Rule-Based Risk Scoring. Akses notifikasi membutuhkan izin Android. Hanya pratinjau notifikasi yang tersedia yang dapat diproses. Hasil analisis merupakan indikator, bukan kepastian bahwa pesan aman atau berbahaya.")
                .setPositiveButton("Mengerti", null).show()
        }, false)
    }

    private fun showArticle(title: String) {
        AlertDialog.Builder(this).setTitle(title)
            .setMessage("Berhenti sejenak sebelum bertindak. Periksa alamat pengirim dan domain tautan dengan teliti. Jangan pernah memberikan OTP, PIN, atau kata sandi melalui pesan. Bila pesan mengaku berasal dari bank atau instansi, buka aplikasi atau situs resmi secara mandiri untuk melakukan verifikasi.\n\nContoh pada halaman edukasi ini bersifat informatif dan bukan hasil intersepsi pesan.")
            .setPositiveButton("Tutup", null).show()
    }

    private fun showSettings() {
        val body = createPage("Pengaturan")
        val enabled = AppPrefs.notificationAccessEnabled(this)
        card(body, "Akses Notifikasi", if (enabled) "Izin Notification Access aktif." else "Izin Notification Access belum diberikan.")
        action(body, "Buka Pengaturan Akses Android", { openNotificationAccess() }, false)

        text(body, "Sumber Notifikasi", 16f, pearl, true, top = 10)
        val prefs = getSharedPreferences(AppPrefs.PREFS, MODE_PRIVATE)
        val wa = CheckBox(this).apply { text = "WhatsApp"; setTextColor(pearl); isChecked = prefs.getBoolean(AppPrefs.KEY_INCLUDE_WHATSAPP, true) }
        val sms = CheckBox(this).apply { text = "Aplikasi SMS"; setTextColor(pearl); isChecked = prefs.getBoolean(AppPrefs.KEY_INCLUDE_SMS, true) }
        body.addView(wa); body.addView(sms)
        text(body, "Pengirim Tepercaya", 16f, pearl, true, top = 12, bottom = 5)
        text(body, "Masukkan satu identitas pengirim per baris. Pengirim yang tidak ada di daftar akan mendapat indikator +2 jika identitasnya tersedia.", 12f, muted)
        val trusted = input(body, "Contoh:\n08123456789\nNama Kontak", multiline = true)
        trusted.setText(prefs.getString(AppPrefs.KEY_TRUSTED_SENDERS, ""))

        text(body, "Integrasi Data (API)", 16f, pearl, true, top = 12, bottom = 5)
        text(body, "Masukkan base URL backend untuk mengambil pembaruan aturan dan whitelist. Isi pesan tidak dikirim ke API.", 12f, muted)
        val apiUrl = input(body, "Contoh: http://192.168.1.10:8000")
        apiUrl.setText(prefs.getString(AppPrefs.KEY_API_URL, ""))
        action(body, "Simpan Pengaturan & Sinkronkan Aturan", {
            prefs.edit()
                .putBoolean(AppPrefs.KEY_INCLUDE_WHATSAPP, wa.isChecked)
                .putBoolean(AppPrefs.KEY_INCLUDE_SMS, sms.isChecked)
                .putString(AppPrefs.KEY_TRUSTED_SENDERS, trusted.text.toString())
                .putString(AppPrefs.KEY_API_URL, apiUrl.text.toString().trim())
                .apply()
            val url = apiUrl.text.toString().trim()
            if (url.isBlank()) {
                Toast.makeText(this, "Pengaturan tersimpan. API belum dikonfigurasi; aturan lokal digunakan.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Menghubungkan ke API...", Toast.LENGTH_SHORT).show()
                Thread {
                    try {
                        val response = ApiClient.fetchRules(url)
                        RuleConfigStore.save(this, response.raw)
                        runOnUiThread { Toast.makeText(this, "Aturan API ${response.config.version} berhasil disinkronkan.", Toast.LENGTH_LONG).show() }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this, "Sinkronisasi gagal: ${e.message}. Aturan lokal tetap digunakan.", Toast.LENGTH_LONG).show() }
                    }
                }.start()
            }
        })
        action(body, "Hapus Semua Riwayat Lokal", {
            AlertDialog.Builder(this).setTitle("Hapus seluruh riwayat?")
                .setMessage("Isi pesan dan hasil analisis yang tersimpan akan dihapus dari database aplikasi.")
                .setNegativeButton("Batal", null).setPositiveButton("Hapus") { _, _ ->
                    LocalDatabase(this).use { it.clear() }
                    Toast.makeText(this, "Riwayat lokal dihapus.", Toast.LENGTH_SHORT).show()
                }.show()
        }, false)
        text(body, "Privasi: data analisis disimpan di database lokal aplikasi. Tidak ada sinkronisasi isi pesan atau pengirim ke server. Penyimpanan lokal tidak dienkripsi oleh implementasi ini.", 11f, muted, top = 8)
        text(body, "Tentang MIVVI SECURE • Rule-Based Risk Scoring • Prototype 1.0", 10f, cyan, top = 16)
    }

    private fun openNotificationAccess() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, "Pengaturan Notification Access tidak tersedia pada perangkat ini.", Toast.LENGTH_LONG).show()
        }
    }

    private fun dbCount(): Int = LocalDatabase(this).use { it.count() }
    private fun dbHighRisk(): Int = LocalDatabase(this).use { it.highRiskCount() }
    private fun dbRecent(limit: Int): List<AnalysisRecord> = LocalDatabase(this).use { it.recent(limit) }
}
