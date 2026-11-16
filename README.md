# MIVVI SECURE

**Your messages. Your safety. — Kenali risiko. Lindungi dirimu.**

MIVVI SECURE adalah prototipe aplikasi Android untuk membantu mengenali indikasi phishing pada pesan SMS dan WhatsApp. Sistem menggunakan **Rule-Based Risk Scoring**, bukan machine learning. Pemeriksaan pesan dilakukan secara lokal di perangkat.

> Status rilis: source code dan backend API tersedia. APK belum dapat disertakan dari lingkungan pembuatan ini karena Android SDK/Build Tools dan Gradle tidak tersedia serta akses unduhan toolchain diblokir. Cara membangun APK dijelaskan di bawah. Jangan mengganti status ini dengan file ber-ekstensi `.apk` yang bukan paket Android valid.

## Fitur

- Analisis manual untuk teks SMS atau WhatsApp.
- Deteksi notifikasi SMS/WhatsApp melalui `NotificationListenerService` setelah pengguna mengaktifkan izin **Notification Access** di Android.
- Pemrosesan hanya terhadap paket aplikasi SMS yang didukung dan WhatsApp/WhatsApp Business.
- Pembacaan konten berdasarkan pratinjau notifikasi yang disediakan Android. Jika pratinjau disembunyikan, aplikasi menyimpan catatan metadata dengan status konten tidak tersedia; isi pesan tidak ditebak.
- Rule-based risk scoring dengan enam indikator dan bobot yang sudah disepakati.
- Kategori AMAN, MENCURIGAKAN, dan PHISHING.
- Riwayat analisis lokal dengan penghapusan seluruh riwayat.
- Daftar pengirim tepercaya yang dapat dikelola pengguna.
- API opsional untuk sinkronisasi keyword, bobot, threshold, whitelist domain, dan daftar shortlink.
- Halaman Login, Sign Up, dan Forgot Password sebagai **UI prototype**; autentikasi belum tersambung ke backend. Aplikasi tetap dapat digunakan melalui Continue as Guest.

## Metode deteksi

| Indikator | Bobot |
|---|---:|
| Keyword mencurigakan | +2 |
| URL di luar whitelist resmi | +3 |
| Shortlink | +2 |
| Pengirim tidak dikenal | +2 |
| Pola urgensi | +1 |
| Permintaan data sensitif | +3 |

Klasifikasi:

- **AMAN**: skor < 3
- **MENCURIGAKAN**: 3 ≤ skor < 6
- **PHISHING**: skor ≥ 6

Skor contoh maksimum keenam indikator terpenuhi adalah 13. Setiap indikator dihitung satu kali walaupun terdapat beberapa keyword atau URL sejenis di dalam satu pesan. Indikator pengirim hanya dihitung jika identitas pengirim tersedia; pengirim dibandingkan dengan daftar tepercaya yang dimasukkan pengguna.

### Pipeline

`SMS/WhatsApp notification → parsing → normalisasi → ekstraksi URL → tokenisasi/pencocokan keyword → pemeriksaan domain & shortlink → pemeriksaan pengirim → deteksi urgensi/data sensitif → weighted scoring → threshold classification → hasil`

Aho–Corasick digunakan untuk pencocokan banyak keyword. Regex digunakan untuk ekstraksi URL serta pola urgensi/data sensitif. Pemeriksaan whitelist menggunakan pencocokan domain yang membatasi kecocokan pada domain tepat atau subdomain (bukan sekadar substring).

## Batasan penting integrasi SMS dan WhatsApp

Android tidak memberikan akses bebas ke database percakapan pribadi semua aplikasi. Implementasi ini menggunakan **NotificationListenerService**. Karena itu:

1. Pengguna harus mengaktifkan Notification Access secara eksplisit dari pengaturan Android.
2. Aplikasi hanya memproses notifikasi dari paket SMS dan WhatsApp yang didukung.
3. Isi pesan hanya dapat dianalisis jika aplikasi sumber menampilkan teks dalam pratinjau notifikasi.
4. Pengaturan privasi seperti menyembunyikan isi notifikasi, notifikasi yang tidak muncul, atau pembatasan aplikasi dapat membuat konten tidak tersedia.
5. Implementasi ini tidak menjadi aplikasi SMS default, tidak memakai izin `READ_SMS`/`RECEIVE_SMS`, tidak membaca database chat WhatsApp, dan tidak memblokir pesan secara otomatis.
6. Pengirim WhatsApp yang berupa nama tampilan dapat dimasukkan manual ke daftar pengirim tepercaya. Daftar kontak Android tidak dibaca otomatis.

Aplikasi ini adalah alat bantu indikator risiko, bukan jaminan bahwa suatu pesan aman atau pasti phishing. Domain whitelist bawaan adalah contoh awal dan perlu ditinjau sebelum digunakan secara produksi.

## Privasi dan penyimpanan

- Isi pesan dan hasil analisis disimpan di SQLite lokal aplikasi agar riwayat dapat dibuka kembali.
- Tidak ada kode yang mengirim isi pesan, nomor pengirim, atau riwayat ke backend API.
- API hanya digunakan untuk mengambil konfigurasi aturan melalui `GET /api/v1/rules`.
- Implementasi ini belum mengenkripsi database lokal. Jangan gunakan pesan pribadi/sensitif sebagai data demo saat presentasi publik.
- Hapus riwayat melalui menu Riwayat atau Pengaturan.

## Struktur repository

```text
MIVVI-SECURE/
├── app/                         # Aplikasi Android Kotlin
│   └── src/main/
│       ├── java/id/ac/um/mivvisecure/
│       │   ├── MainActivity.kt
│       │   ├── NotificationMonitorService.kt
│       │   ├── RuleBasedAnalyzer.kt
│       │   ├── RuleConfigStore.kt
│       │   ├── ApiClient.kt
│       │   ├── LocalDatabase.kt
│       │   └── AppPrefs.kt
│       ├── res/
│       └── AndroidManifest.xml
├── server/                      # FastAPI Rules API
│   ├── app/main.py
│   ├── config/rules.json
│   └── requirements.txt
├── docs/                        # Dokumentasi rancangan dan API
├── tests/                       # Smoke test algoritma Kotlin
└── scripts/                     # Script pengujian dan build
```

## Menjalankan aplikasi Android

### Persyaratan

- Android Studio versi yang mendukung Android Gradle Plugin 8.7.3.
- JDK 17.
- Android SDK Platform 35 dan Android Build Tools yang sesuai.
- Gradle 8.9.
- Perangkat Android 8.0 (API 26) atau lebih baru.

### Build dengan Android Studio

1. Ekstrak repository ini.
2. Buka Android Studio → **Open** → pilih folder `MIVVI-SECURE`.
3. Pastikan Gradle JDK menggunakan JDK 17 dan SDK Platform 35 sudah terpasang.
4. Jalankan **Sync Project with Gradle Files**.
5. Hubungkan ponsel Android dengan USB debugging aktif atau gunakan emulator.
6. Pilih konfigurasi `app`, lalu tekan **Run**.
7. Untuk membuat APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
8. APK debug akan dibuat di `app/build/outputs/apk/debug/app-debug.apk`.

**Alternatif tanpa memasang Android SDK di komputer:** repository sudah menyertakan GitHub Actions workflow di `.github/workflows/android-apk.yml`. Setelah source di-push ke branch `main`, buka tab **Actions** pada repository GitHub, pilih workflow **Build MIVVI SECURE APK**, buka hasil run yang sukses, lalu unduh artifact `MIVVI-SECURE-debug-apk`. Workflow juga dapat dijalankan manual melalui **Actions → Build MIVVI SECURE APK → Run workflow**. Artifact berupa debug APK untuk pengujian.

Alternatif melalui terminal (setelah Gradle 8.9 dan Android SDK terpasang):

```bash
gradle assembleDebug
```

File debug APK akan berada di `app/build/outputs/apk/debug/app-debug.apk`. Debug APK ditandatangani dengan debug keystore dan cocok untuk instalasi/pengujian, bukan untuk distribusi Play Store.

### Mengaktifkan integrasi notifikasi

1. Buka MIVVI SECURE.
2. Pilih **Hubungkan Notifikasi** atau masuk ke Pengaturan.
3. Tekan **Buka Pengaturan Akses Android**.
4. Cari **MIVVI SECURE Notification Access**, lalu aktifkan izin.
5. Kembali ke aplikasi. Aktifkan/ nonaktifkan sumber WhatsApp dan Aplikasi SMS sesuai kebutuhan.
6. Kirim pesan uji ke perangkat. Hanya pratinjau yang tampil di notifikasi yang dapat diperiksa.

## Menjalankan Rules API

API bersifat opsional. Aplikasi tetap melakukan analisis dengan aturan lokal jika API tidak dikonfigurasi atau sedang tidak tersedia.

### Persiapan backend

```bash
cd server
python -m venv .venv

# Windows PowerShell
.venv\Scripts\Activate.ps1

# macOS/Linux
source .venv/bin/activate

pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Cek API:

```bash
curl http://127.0.0.1:8000/api/v1/health
curl http://127.0.0.1:8000/api/v1/rules
```

Dokumentasi interaktif tersedia di `http://127.0.0.1:8000/docs`.

### Hubungkan Android ke API

1. Pastikan ponsel dan komputer berada di jaringan Wi-Fi/LAN yang sama.
2. Cari alamat IP lokal komputer, misalnya `192.168.1.10`.
3. Jalankan API dengan `--host 0.0.0.0` dan izinkan port 8000 pada firewall lokal.
4. Di MIVVI SECURE → Pengaturan → Integrasi Data (API), masukkan `http://192.168.1.10:8000`.
5. Tekan **Simpan Pengaturan & Sinkronkan Aturan**.
6. Jika sinkronisasi gagal, aplikasi tetap memakai aturan lokal bawaan.

Alamat `10.0.2.2` digunakan emulator Android untuk mengakses host komputer; pada ponsel fisik gunakan IP LAN komputer. HTTP hanya untuk pengembangan di jaringan lokal. Gunakan HTTPS, autentikasi, dan kontrol perubahan konfigurasi sebelum deployment publik.

## Dokumentasi API

- `GET /api/v1/health` — status layanan.
- `GET /api/v1/rules` — konfigurasi versioned yang berisi `weights`, `thresholds`, `keywords`, `urgency_patterns`, `sensitive_patterns`, `official_domains`, dan `shortlink_domains`.

Contoh response tersedia di `server/config/rules.json`. Untuk memperbarui aturan, edit file tersebut lalu restart/reload server. Aplikasi mengambil konfigurasi dan menyimpannya pada perangkat; pesan pengguna tetap dianalisis lokal.

## Pengujian aturan

Dengan Kotlin compiler terpasang:

```bash
./scripts/run-rule-tests.sh
```

Skrip memverifikasi contoh skor 13 (PHISHING) serta ambang AMAN dan MENCURIGAKAN.

## Mengunggah ke GitHub

Buat repository kosong, lalu jalankan dari folder proyek:

```bash
git init
git add .
git commit -m "Initial MIVVI SECURE Android prototype"
git branch -M main
git remote add origin https://github.com/USERNAME/MIVVI-SECURE.git
git push -u origin main
```

Ganti `USERNAME` dan URL dengan repository GitHub milikmu. Jangan unggah data pesan asli, keystore pribadi, password, token, atau file konfigurasi rahasia.

## Catatan status implementasi

| Komponen | Status |
|---|---|
| Rule-based analyzer Kotlin | Implemented; smoke test pass |
| Analisis manual | Implemented |
| Database riwayat lokal | Implemented |
| NotificationListenerService SMS/WhatsApp | Implemented, requires Android access and visible notification preview |
| Sinkronisasi konfigurasi Rules API | Implemented |
| Login/Sign Up/Forgot Password UI | Prototype UI only; no authentication backend |
| Enkripsi database lokal | Not implemented |
| Auto-block / auto-delete / background interception of private chats | Not implemented |
| APK binary | Not built in this environment; local build steps and GitHub Actions workflow provided |

## Lisensi dan penggunaan

Prototype akademik. Tinjau domain whitelist, pola deteksi, privasi, keamanan API, dan kompatibilitas perangkat sebelum penggunaan di luar pengujian.
