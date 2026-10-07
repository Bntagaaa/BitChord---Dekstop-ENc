# Mobile - Spotify Integration

Status: PLAN ONLY. Belum ada implementasi, build APK, atau pengujian perangkat untuk workstream ini.
Disusun: 7 Oktober 2026.

## 1. Identitas dan pemisahan pekerjaan

| Bagian | Keputusan |
| --- | --- |
| Repository | `Bntagaaa/BitChord---Dekstop-ENc` |
| Platform | Mobile Android, bukan desktop dan bukan iOS |
| Branch kerja | `feature/mobile-spotify-stability` |
| Dasar branch | `main` pada commit `d12f4ebdef9308324fc57d68617fce81a584c3ec` |
| Branch desktop existing | `feature/desktop-ux-shortcuts-search-layout` - tidak diubah |
| Pekerjaan sekarang | Membuat branch dan dokumen rencana saja |
| Langkah berikutnya | Menunggu persetujuan pengguna sebelum perubahan kode atau build |

Branch berasal dari `main` milik fork, bukan dari ujung branch desktop dan bukan dari upstream terbaru yang belum ditinjau. Source mobile Spotify sudah ada pada baseline tersebut. Ini memisahkan perubahan baru mobile dari rangkaian patch desktop; bukan berarti repository menjadi Android-only atau baseline tidak memiliki kode desktop.

Jangan merge branch desktop ke branch ini hanya untuk menyamakan commit. Jangan mengubah `main`, upstream, release, tag, workflow, atau nomor versi dalam tahap persiapan. Saat implementasi nanti gunakan checkout/worktree atau environment terpisah dari pekerjaan desktop.

## 2. Tujuan dan batas scope

Tujuan: memperbaiki kestabilan koneksi Spotify di Android dengan membedakan kegagalan halaman login, autentikasi/sesi, library, dan Canvas. Perbaikan harus bisa diuji dan tidak boleh sekadar menyembunyikan pesan error.

Dalam scope investigasi:

- Halaman Spotify sign-in: flicker, layout, navigasi, Back/cancel, lifecycle WebView, renderer/process errors.
- Validasi koneksi, expired session, retry, disconnect, dan pergantian akun.
- Library playlist/Liked Songs serta kegagalan pembacaan atau pagination yang teridentifikasi.
- Canvas sebagai kemampuan terpisah dari library: tidak ada Canvas berbeda dengan gagal autentikasi atau gagal request.
- Pengujian kompatibilitas Android/WebView dan perlindungan kredensial.

Di luar scope:

- `desktopApp/**`, shortcut desktop, flyout desktop, title bar Windows, MPRIS, AppImage/DEB, dan fix decoder desktop.
- Mengganti mesin playback, YouTube resolver, queue, atau identitas aplikasi.
- Menambahkan Spotify Connect atau menjanjikan streaming audio langsung dari Spotify.
- Mengakali pembatasan penyedia identitas, CAPTCHA, rate limit, atau kuota API.
- Rilis APK publik, merge, workflow baru, dan migrasi OAuth besar tanpa keputusan terpisah.

`shared/**` dan `sharedUi/**` tidak diubah secara default. Kebutuhan perubahan bersama harus dijelaskan dahulu, disetujui, dan memiliki regression coverage desktop.

## 3. Bukti awal dan hal yang belum terbukti

Sumber awal: `SpotifyLibraryScreen.kt`, `SpotifyToken.kt`, `SpotifyLibrary.kt`, `SpotifyCanvas.kt`, dan laporan upstream yang sudah ditinjau dalam investigasi sebelumnya. Path lengkap ada di bagian 7.

Fakta source pada baseline:

- Login memakai Android WebView, tetapi `LOGIN_USER_AGENT` menyebut Windows/Chrome desktop.
- `LOGIN_LAYOUT_FIX` menyisipkan CSS yang mengubah tinggi, overflow, dan posisi elemen halaman Spotify.
- Saat `sp_dc` ditemukan, callback menyimpan cookie dan menutup layar login; keberadaan cookie saja belum membuktikan library berhasil dibaca.
- Token provider memakai WebView tersembunyi serta hook respons web-player; ini bukan alur OAuth PKCE milik aplikasi.
- `SpotifyToken` memanggil `WebStorage.getInstance().deleteAllData()` sebelum pemuatan web-player. Efeknya terhadap storage WebView lain perlu diaudit; jangan menambah penghapusan data global sebagai workaround.

Hipotesis, bukan diagnosis final:

- Kombinasi UA desktop, viewport ponsel, dan CSS override dapat berkontribusi pada flicker. Perlu perbandingan terkontrol, bukan langsung menyatakan penyebabnya sudah pasti.
- Crash login dapat berasal dari renderer, lifecycle, navigasi, atau jalur lain. Laporan teks tidak cukup untuk memilih salah satunya: perlu stack trace/logcat dan versi WebView.
- Kegagalan sesudah login dapat berada pada validasi sesi, pengambilan token, atau API private; jangan memberi label semua kasus sebagai cookie salah.
- `429` tidak otomatis membuktikan penolakan token. Bedakan rate/quota limit dan kegagalan lain dari respons yang benar-benar diamati.

Laporan awal untuk reproduksi:

- [#583 - crash saat login](https://github.com/kushagrasinghx/BitChord/issues/583)
- [#597 - crash integrasi](https://github.com/kushagrasinghx/BitChord/issues/597)
- [#598 - halaman login flicker](https://github.com/kushagrasinghx/BitChord/issues/598)
- [#612 - flicker dan verifikasi Google tidak lanjut](https://github.com/kushagrasinghx/BitChord/issues/612)

Status issue dapat berubah. Penutupan sebagai duplicate bukan bukti bug telah diperbaiki. Baca kembali issue/komentar terkait sebelum implementasi.

## 4. Keputusan autentikasi sebelum perombakan

### Library dan Canvas bukan kontrak autentikasi yang sama

Rencana memisahkan status kemampuan library dari Canvas. Jangan menganggap token OAuth publik pasti dapat dipakai pada endpoint Canvas/private web-player, atau menjadikan kegagalan Canvas sebagai kegagalan semua integrasi.

Untuk library, OAuth Authorization Code + PKCE melalui browser adalah kandidat jangka panjang. Spotify merekomendasikan PKCE ketika client secret tidak dapat disimpan aman pada aplikasi mobile. Namun kelayakannya harus diperiksa untuk app yang benar-benar didaftarkan: scope, redirect URI, akses endpoint, allowlist/kuota, serta target distribusi. Tidak ada keputusan bahwa migrasi ini otomatis layak untuk seluruh pengguna BitChord.

Google tidak mendukung sign-in dalam embedded WebView. Menambah domain whitelist atau mengganti UA bukan penyelesaian kebijakan tersebut. Jangan mencoba melewati pembatasan ini.

Custom Tabs bukan pengganti langsung bagi alur pembacaan `sp_dc`: sesi browser dan cookie jar WebView tidak sama. Membuka Spotify di browser saja tidak otomatis menyerahkan cookie ke BitChord. Jalur browser harus memiliki callback otorisasi yang didukung dan divalidasi, bukan janji membaca cookie browser.

**Gate keputusan setelah reproduksi:** pilih patch kompatibilitas/lifecycle terbatas atau usulkan migrasi OAuth terpisah berdasarkan bukti dan akses API yang tersedia. Perubahan arsitektur besar perlu persetujuan pengguna sebelum dikerjakan. Selama gate belum diputuskan, jangan menulis konektor OAuth baru atau mengganti seluruh sistem sesi.

Referensi resmi yang diperiksa pada penyusunan rencana:

- [Spotify Authorization Code with PKCE](https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow)
- [Spotify quota modes](https://developer.spotify.com/documentation/web-api/concepts/quota-modes)
- [Google Sign in best practices: larangan embedded WebView](https://developers.google.com/identity/siwg/best-practices)
- [Chrome Custom Tabs: perbedaan state browser dan WebView](https://developer.chrome.com/docs/android/custom-tabs)

## 5. Tahapan pengerjaan setelah disetujui

### M1 - Reproduksi dan diagnosis

Catat versi APK/source commit, Android, model perangkat, provider/versi WebView, metode login, dan langkah reproduksi. Pisahkan minimal tiga kasus: halaman flicker, proses aplikasi crash, dan login terlihat berhasil tetapi library/Canvas gagal.

Ambil logcat/stack trace tersanitasi, HTTP status, waktu tiap tahap, dan host navigasi yang relevan. Jangan mencatat password, OTP, cookie, Authorization header, token, HTML akun, atau URL callback lengkap dengan query sensitif. Jangan mengunggah kredensial ke issue publik.

Bandingkan baseline dengan eksperimen UA dan CSS secara terpisah. Audit lifecycle WebView dan callback error/renderer terlebih dahulu. Hasil M1 harus menyatakan apa yang berhasil direproduksi, bukti akar masalah, serta hipotesis yang belum terbukti.

### M2 - Stabilitas halaman login dan navigasi

Setelah gate autentikasi diputuskan, hilangkan atau batasi hanya override UA/CSS yang terbukti bermasalah. Pertahankan navigasi aman: validasi scheme dan host secara tepat, jangan melebarkan allowlist secara bebas, serta jangan menelan navigasi yang gagal tanpa pesan.

Sediakan loading/error/cancel/retry yang jelas. Tangani lifecycle, pembatalan, rotasi, background/resume, dan renderer termination tanpa loop reload atau crash aplikasi. Provider yang tidak mendukung embedded sign-in harus mendapat penjelasan/jalur yang sah; jangan menjanjikan semua provider otomatis bekerja.

### M3 - Validasi sesi dan status koneksi

Rancang state eksplisit: `Disconnected`, `Connecting`, `Validating`, `Connected`, `NeedsReauth`, `TemporaryError`. Simpan data dengan aman, tetapi jangan menampilkan Connected hanya karena field cookie terisi.

Validasi sesi melalui operasi yang memang dibutuhkan. Bedakan invalid credentials, token acquisition timeout, pembatasan API, dan masalah jaringan. Callback lama tidak boleh mengembalikan sesi setelah pengguna disconnect atau berganti akun. Audit pembatalan coroutine dan pemakaian cache agar akun lama tidak bocor ke akun baru.

Disconnect hanya membersihkan sesi/cache Spotify terkait. Login dan storage YouTube Music tidak boleh ikut dihapus. Jangan menggunakan reset data seluruh aplikasi sebagai alur pemulihan normal.

### M4 - Library dan Canvas

Perbaiki hanya kegagalan yang dibuktikan: pagination, parsing, metadata, status HTTP/GraphQL, atau cache sesi. Bedakan library kosong dari request gagal dan no-Canvas dari kesalahan autentikasi.

Recovery harus dibatasi, menghormati server backoff/Retry-After jika tersedia, dan tidak mengulang endpoint private tanpa batas. Jangan menyatakan hash/endpoint lama sebagai penyebab pasti tanpa bukti respons. Usulkan pemisahan komponen bila dibutuhkan; jangan menyamarkannya sebagai fitur baru yang sudah dibuat.

Pertahankan impor playlist yang sudah bekerja serta pencocokan ke YouTube Music. Kegagalan Spotify tidak boleh menghentikan playback biasa atau merusak playlist lokal.

### M5 - Tes, APK uji, dan review

Tambahkan tes state/session, navigation validation, parsing/pagination, cancellation, cache invalidation, serta error/retry menggunakan data sintetis atau fixture tersanitasi. Tentukan task Gradle Android dari build configuration yang aktual; jangan menyalin task packaging desktop.

Setelah kode dan tes siap, build APK uji Android dari branch mobile ini saja. Catat commit SHA pada hasil uji. Pengujian unit atau compile tidak menggantikan tes login nyata. Jangan klaim pengujian perangkat berhasil tanpa benar-benar menjalankannya.

## 6. Kriteria selesai

| Area | Bukti yang diperlukan |
| --- | --- |
| Flicker/crash | Reproduksi awal dan hasil setelah patch pada perangkat/versi WebView yang dicatat; tidak mengasumsikan semua perangkat terwakili |
| Login | Jalur yang dinyatakan didukung berhasil; jalur tidak didukung memiliki pesan yang benar, bukan layar mati |
| Session | Connected hanya setelah validasi; expired/offline/rate-limited dibedakan; retry tidak perlu restart aplikasi |
| Lifecycle | Back/cancel, rotasi, background/resume, dan renderer failure ditangani dengan aman |
| Akun | Pergantian akun/disconnect tidak menggunakan hasil lama dan tidak menghapus sesi YouTube Music |
| Library | Playlist kosong, playlist panjang, Liked Songs, serta lagu yang tidak ditemukan ditangani tanpa crash |
| Canvas | Lagu dengan/tanpa Canvas dan keadaan online/offline dibedakan dari kegagalan autentikasi |
| Regresi | Playback normal dan impor existing tetap berjalan; source desktop tidak berubah |
| Privasi | Log/fixture/commit bebas cookie, token, password, OTP, dan data akun pribadi |

Matriks perangkat akan ditentukan saat M1 berdasarkan laporan nyata dan minSdk/targetSdk project, bukan menebak versi Android yang harus didukung. Sertakan sesi baru dan existing, dua akun uji yang sah, Wi-Fi/data seluler, koneksi putus, serta versi WebView yang tercatat.

## 7. Peta source yang mungkin disentuh nanti

Path berikut adalah calon scope, bukan daftar file yang sudah diubah:

- `app/src/main/java/com/music/bitchord/ui/screens/SpotifyLibraryScreen.kt`: login UI, navigasi, loading/error/retry.
- `app/src/main/java/com/music/bitchord/data/canvas/SpotifyToken.kt`: lifecycle token dan isolasi sesi.
- `app/src/main/java/com/music/bitchord/data/spotify/SpotifyLibrary.kt`: library, pagination, dan klasifikasi error.
- `app/src/main/java/com/music/bitchord/data/canvas/SpotifyCanvas.kt`: kegagalan Canvas yang berhasil dibuktikan.
- `app/src/main/java/com/music/bitchord/ui/screens/SpotifyCanvasAuthScreen.kt`: status/validasi pengaturan jika diperlukan.
- `app/src/main/java/com/music/bitchord/ui/screens/AccountAndScrobblingScreen.kt` serta `app/src/main/java/com/music/bitchord/data/settings/AppSettings.kt`: status koneksi dan disconnect.
- `app/src/test/**` dan bila tersedia `app/src/androidTest/**`: regression tests.
- `app/src/main/java/com/music/bitchord/BitChordApplication.kt`, `MainActivity.kt`, manifest, atau dependencies hanya jika diagnosis memerlukan perubahan lifecycle/auth yang disetujui.

Baseline contoh source:

- [SpotifyLibraryScreen pada commit dasar](https://github.com/Bntagaaa/BitChord---Dekstop-ENc/blob/d12f4ebdef9308324fc57d68617fce81a584c3ec/app/src/main/java/com/music/bitchord/ui/screens/SpotifyLibraryScreen.kt)
- [SpotifyToken pada commit dasar](https://github.com/Bntagaaa/BitChord---Dekstop-ENc/blob/d12f4ebdef9308324fc57d68617fce81a584c3ec/app/src/main/java/com/music/bitchord/data/canvas/SpotifyToken.kt)

## 8. Aturan pelaksanaan

Saat membuka task coding berikutnya, baca dokumen ini dan verifikasi branch dahulu. Tahap persiapan hanya menambahkan dokumen ini. Jangan menganggap persetujuan membuat plan sebagai persetujuan implementasi, build, merge, atau release.

Setelah izin implementasi diberikan, pecah perubahan menjadi commit kecil sesuai tahap: diagnosis/tests, login/lifecycle, session validation, lalu perbaikan library/Canvas yang terbukti perlu. Setiap laporan hasil harus memisahkan source review, tes yang benar-benar dijalankan, tes perangkat, dan pekerjaan yang belum dilakukan.
