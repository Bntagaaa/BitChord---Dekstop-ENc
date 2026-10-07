# Spotify Android — implementasi M2/M3 dan validasi

Baseline: `78cde0f17f41b6aa8d6e9e5aca3a931ecd383f62` pada `feature/mobile-spotify-stability`. Diagnosis dan batas bukti ada di `SPOTIFY_M1_FINDINGS.md`. Perubahan ini hanya menargetkan login WebView dan validasi sesi/library Android; UA desktop dan CSS override baseline tetap karena eksperimen perangkat terkontrol belum ada. Tidak ada migrasi OAuth atau perubahan Canvas API, playback, desktop, shared, workflow, signing, atau versi.

## Perubahan

- `SpotifyLibraryScreen.kt`: callback login diabaikan setelah layar ditutup, navigasi utama yang tidak diizinkan memberi pesan, serta error halaman dan renderer memberi retry. Back/cancel menutup login. Cookie yang tertangkap masuk tahap validasi library sebelum status Connected; kegagalan mendapat retry atau reauth. Reauth membersihkan sesi Spotify lama sebelum membuka login lagi.
- `SpotifyConnection.kt`, `AppSettings.kt`, `AccountAndScrobblingScreen.kt`, `SpotifyLibrary.kt`: status sesi bersama (`Disconnected`, `Connecting`, `Validating`, `Connected`, `NeedsReauth`, `TemporaryError`), validasi menggunakan operasi playlist yang sudah ada, HTTP 401 dibedakan dari 403/429/jaringan, dan hasil request akun lama tidak boleh mengubah status/playlist akun baru.
- `SpotifyToken.kt`: invalidasi cache diikat pada cookie/generasi sesi, harvest aktif dihentikan saat akun berubah, cancellation dipropagasikan, kegagalan renderer ditangani, dan penghapusan WebStorage dibatasi ke origin `open.spotify.com` menggantikan `deleteAllData()`.
- Enam pesan baru diterjemahkan untuk semua 15 locale yang didukung. `SpotifyConnectionTest.kt` menambah enam tes sesi sintetis tanpa kredensial.

## Hasil perintah aktual

| Perintah | Hasil |
| --- | --- |
| `./gradlew :app:testDevDebugUnitTest --tests 'com.music.bitchord.SpotifyConnectionTest' --console=plain` | PASS; versi awal 5 tes, tanpa gagal. |
| `./gradlew :app:testDevDebugUnitTest :app:assembleDevDebug --console=plain` | Tes: 978 dijalankan, 16 gagal (15 locale baru belum diterjemahkan, 1 tes live Genius). Build tidak dijalankan karena task tes gagal. Locale kemudian dilengkapi. |
| `./gradlew :app:testDevDebugUnitTest --console=plain` dengan proxy HTTPS lingkungan | 978 dijalankan, 977 lulus, 1 gagal: `GeniusTest.live genius search and scraping test with noisy titles` pada `GeniusTest.kt:111` karena hasil live pertama `null`. Tes ini di luar Spotify; tidak dimatikan atau diubah. Semua tes locale lulus. |
| `./gradlew :app:testDevDebugUnitTest --tests 'com.music.bitchord.Spotify*' :app:assembleDevDebug --console=plain` | PASS: 8 tes Spotify (6 sesi, 2 parser), dan `assembleDevDebug` berhasil. |
| Perintah Spotify+assemble yang sama setelah review race akun | PASS lagi: 8 tes Spotify; APK DevDebug dibangun dari logika akhir. |
| `git diff --check` | PASS, tidak ada whitespace error. |

SDK dipasang di `/workspace/.local/android-sdk`, di luar checkout. AGP 8.10.1 mencari direktori `android-37`, sementara paket platform terpasang sebagai `android-37.0`; alias lokal SDK digunakan untuk build. Tidak ada perubahan `compileSdk`, plugin, atau file konfigurasi repository. AGP memberi peringatan bahwa versi ini diuji sampai SDK 36; build Android 37 tetap selesai.

APK universal DevDebug: `app/build/outputs/apk/dev/debug/app-dev-universal-debug.apk` (186,989,515 byte), SHA-256 `0f1f4cc677ece263cd65195151fd42f0d71e7ca1c79b86afd6ee0b730184fd32`. ABI lain (`arm64-v8a`, `armeabi-v7a`, `x86_64`) juga dihasilkan. APK ini **belum** diuji pada perangkat.

## Checklist perangkat — NOT RUN

Tidak tersedia emulator/GUI atau bukti perangkat Android terhubung. `adb devices` tidak dapat berjalan dalam sandbox karena mencoba membuat `/home/agent/.android` yang read-only. Karena itu login nyata, perbandingan UA versus CSS, flicker, provider Google, Back/cancel, rotasi, background/resume, renderer termination, switch akun nyata, sesi YouTube, library panjang, dan Canvas semuanya **NOT RUN**. Keberhasilan compile dan unit test tidak membuktikan masalah flicker/crash sudah hilang pada perangkat.

Untuk pengujian berikutnya: gunakan APK DevDebug di atas; catat model/versi Android/WebView dan jalur login. Ambil logcat tersanitasi untuk crash, kode/kategori HTTP untuk library/Canvas, serta hasil perbandingan UA dan CSS secara terpisah. Jangan mencatat atau mengirim cookie, password, OTP, token, header Authorization, HTML akun, atau callback URL lengkap.

`git diff` perlu tetap terbatas pada `app/**` dan `docs/mobile/**`. Kegagalan live Genius yang tidak terkait Spotify tetap terbuka; tidak ada test yang dinonaktifkan agar suite tampak hijau.
