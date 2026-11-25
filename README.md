# 🐤 Kenari Tasikmalaya

**Kenari Tasikmalaya** adalah aplikasi Android untuk membantu peternak mengelola data burung kenari, silsilah indukan, pasangan breeding, produksi telur, proses pengeraman, kesehatan, dan perawatan harian dalam satu workspace kandang (*aviary*).

Aplikasi dirancang untuk pencatatan peternakan yang terstruktur, pemantauan produksi, kolaborasi tim, serta sinkronisasi data melalui Firebase Cloud Firestore.

> Repository: [diycraftchannel83-create/Canary](https://github.com/diycraftchannel83-create/Canary)  
> Nama aplikasi: **Kenari Tasikmalaya**  
> Platform: **Android**  
> Application ID: `com.winsmediatama.kenaritasik`  
> Versi konfigurasi saat ini: **1.0 (versionCode 1)**

---

## Daftar Isi

- [Gambaran Aplikasi](#-gambaran-aplikasi)
- [Fitur Aplikasi](#-fitur-aplikasi)
- [Alur Pengelolaan Breeding](#-alur-pengelolaan-breeding)
- [Teknologi](#-teknologi)
- [Struktur Proyek](#-struktur-proyek)
- [Persyaratan](#-persyaratan)
- [Konfigurasi Firebase](#-konfigurasi-firebase)
- [Build dan Menjalankan Aplikasi](#-build-dan-menjalankan-aplikasi)
- [Keamanan Data](#-keamanan-data)
- [Catatan Implementasi](#-catatan-implementasi)
- [Kontribusi](#-kontribusi)

## 🌿 Gambaran Aplikasi

Kenari Tasikmalaya merupakan aplikasi manajemen breeding kenari yang menggabungkan pencatatan individu burung dan hubungan genetiknya dengan manajemen pasangan, periode produksi, penetasan, kesehatan, serta jadwal perawatan.

Data dikelompokkan ke dalam **Aviary** (workspace peternakan). Pemilik dapat mengelola aviary sendiri dan mengundang anggota tim menggunakan kode undangan. Data aviary disinkronkan melalui Firestore sehingga anggota yang memiliki akses dapat bekerja pada data kandang yang sama.

Antarmuka dibangun menggunakan Jetpack Compose dan Material 3. Navigasi utama menyediakan lima area: Dasbor, Silsilah, Produksi, Perawatan, dan Setting.

## ✨ Fitur Aplikasi

Fitur-fitur berikut dirangkum dari layar, model data, repository, utilitas, dan konfigurasi yang tersedia di source code repository.

### 1. Akun dan akses pengguna

- Autentikasi pengguna menggunakan Firebase Authentication.
- Alur masuk menggunakan akun Google melalui Android Credential Manager dan Google Identity.
- Status autentikasi dipantau untuk menampilkan halaman masuk atau area aplikasi.
- Profil pengguna disimpan di Firestore.
- Pengaturan aviary aktif dan preferensi pengingat tersimpan pada profil.

### 2. Dasbor peternakan

Dasbor menjadi ringkasan kondisi dan produktivitas aviary, antara lain:

- Jumlah kenari jantan dan betina.
- Jumlah pasangan breeding aktif.
- Ringkasan jumlah telur dan telur yang berhasil menetas.
- Persentase keberhasilan penetasan berdasarkan catatan produksi.
- Daftar proses inkubasi yang sedang berjalan.
- Ringkasan burung dengan status kesehatan kritis dan peringatan terbaru.
- Grafik produktivitas bulanan.
- Akses cepat menuju pencatatan perawatan dan produksi.
- Ekspor laporan PDF dan Excel.
- Pengaturan identitas peternakan: nama farm, alamat, dan nomor telepon.

### 3. Data individu kenari

Setiap burung dapat dicatat sebagai individu dengan informasi terstruktur:

- Nama burung dan kode ring.
- Jenis kelamin: jantan atau betina.
- Jenis/trah (breed) dan warna bulu.
- Tanggal menetas.
- Kode kandang.
- Referensi induk jantan (*sire*) dan induk betina (*dam*).
- Catatan garis keturunan atau trah.
- Status breeding: aktif, istirahat, atau pensiun.
- Status kesehatan dan skor kesehatan.
- Catatan tambahan.
- Riwayat jumlah periode produksi, telur, telur menetas, dan anakan yang berhasil dibesarkan.
- Catatan riwayat produksi.
- Foto individu melalui kamera.

Foto individu yang sudah tersimpan dilindungi oleh aturan Firestore agar tidak dapat diganti atau dihapus melalui pembaruan dokumen biasa. Data foto pada implementasi ini disimpan sebagai string Base64 terkompresi di dokumen Firestore.

### 4. Silsilah dan pedigree

- Pencatatan hubungan induk jantan dan induk betina.
- Referensi induk pada masing-masing data kenari.
- Catatan garis keturunan (*lineage notes*).
- Tampilan pohon silsilah melalui komponen pedigree.
- Informasi ring dan identitas individu membantu membedakan burung dalam garis keturunan.

### 5. Manajemen pasangan breeding

- Membuat pasangan kenari jantan dan betina.
- Menghubungkan pasangan dengan data indukan yang sudah terdaftar.
- Menyimpan nama dan kode ring kedua indukan.
- Mencatat tanggal mulai pasangan.
- Mengaitkan pasangan dengan kode kandang.
- Status pasangan: aktif, istirahat, atau diarsipkan.
- Menyimpan catatan pasangan dan riwayat pasangan.

### 6. Produksi telur dan inkubasi

- Mencatat periode produksi (*clutch*).
- Menghubungkan catatan produksi dengan pasangan dan kandang.
- Mencatat nomor periode produksi.
- Mencatat jumlah telur, telur fertil, telur menetas, dan anakan yang berhasil dibesarkan.
- Mencatat tanggal mulai inkubasi.
- Menghitung estimasi tanggal menetas berdasarkan siklus **21 hari**.
- Menyimpan tanggal menetas aktual.
- Memantau status: bertelur, inkubasi, menetas, selesai, atau gagal.
- Menyediakan tahapan pemantauan hari ke-7, hari ke-14, hari ke-20, dan estimasi hari ke-21.

### 7. Perawatan dan pemantauan kesehatan

- Mencatat pemeriksaan kesehatan setiap individu.
- Memberi skor kesehatan dan status: sehat, perlu pemantauan, atau kritis.
- Mencatat gejala dan tindakan yang dilakukan.
- Menyimpan riwayat pemeriksaan kesehatan.
- Mendeteksi penurunan skor kesehatan untuk memicu peringatan dini.
- Menyediakan pencatatan jadwal suplemen harian.
- Mencatat dosis, kelompok sasaran, waktu pemberian, dan tanggal penyelesaian tugas.
- Menandai tugas suplemen yang sudah diselesaikan.

### 8. Notifikasi dan pengingat otomatis

Aplikasi memiliki helper notifikasi Android dan penerima alarm untuk pengingat:

- Pengingat suplemen harian pada waktu yang diatur pengguna.
- Pengingat pemeriksaan telur pada hari ke-7.
- Pengingat pemantauan embrio pada hari ke-14.
- Pengingat persiapan menetas pada hari ke-20.
- Pengingat estimasi penetasan pada hari ke-21.
- Peringatan dini ketika skor kesehatan burung turun secara drastis.
- Kanal notifikasi terpisah untuk pengingat perawatan, inkubasi, dan kesehatan.
- Receiver boot untuk menangani penerimaan event saat perangkat selesai menyala.

Notifikasi Android 13 ke atas memerlukan izin `POST_NOTIFICATIONS` dari pengguna. Jadwal alarm dapat dipengaruhi oleh pengaturan baterai dan kebijakan sistem Android.

### 9. Kolaborasi tim dan aviary

- Membuat workspace aviary untuk peternakan.
- Menyimpan nama dan informasi kontak peternakan.
- Membuat kode undangan unik untuk aviary.
- Bergabung ke aviary menggunakan kode undangan.
- Menyimpan daftar UID anggota dan label anggota.
- Berbagi data burung, pasangan, produksi, kesehatan, dan suplemen di dalam workspace.
- Sinkronisasi perubahan melalui Cloud Firestore.
- Pembatasan akses berdasarkan status autentikasi dan keanggotaan aviary.

### 10. Laporan dan ekspor

- Ekspor laporan dalam format PDF.
- Ekspor data dalam format Excel.
- Ringkasan produktivitas bulanan.
- Data produksi telur dan penetasan sebagai bahan evaluasi breeding.

Ketersediaan isi laporan mengikuti data yang tersimpan pada aviary dan modul ekspor yang diimplementasikan.

### 11. Antarmuka dan navigasi

- Dibangun dengan Jetpack Compose.
- Menggunakan Material Design 3.
- Navigasi lima bagian: Dasbor, Silsilah, Produksi, Perawatan, dan Setting.
- Navigasi bawah pada layar lebih sempit.
- Navigation Rail pada layar lebar.
- Layout konten dengan lebar maksimum untuk menjaga keterbacaan.
- Tema warna khusus Kenari Tasikmalaya.

---

## 🔄 Alur Pengelolaan Breeding

Alur penggunaan yang disarankan:

1. Masuk menggunakan akun Google.
2. Buat aviary atau bergabung ke aviary menggunakan kode undangan.
3. Lengkapi informasi peternakan.
4. Daftarkan kenari jantan dan betina beserta identitas ring dan data induknya.
5. Susun pasangan breeding dan tentukan kode kandang.
6. Catat periode produksi dan jumlah telur.
7. Mulai pencatatan inkubasi untuk mendapatkan estimasi penetasan 21 hari.
8. Pantau tahapan pengeraman serta catat hasil penetasan.
9. Catat kesehatan dan jadwal suplemen secara berkala.
10. Tinjau statistik dan ekspor laporan untuk evaluasi.

## 🧰 Teknologi

| Komponen | Teknologi |
|---|---|
| Platform | Android Native |
| Bahasa | Kotlin |
| UI | Jetpack Compose |
| Design System | Material 3 |
| Navigasi | Navigation Compose |
| State & lifecycle | AndroidX Lifecycle, Kotlin Flow |
| Asynchronous | Kotlin Coroutines |
| Autentikasi | Firebase Authentication |
| Login Google | Android Credential Manager, Google Identity |
| Database cloud | Firebase Cloud Firestore |
| Build | Gradle Kotlin DSL |
| Dependency catalog | Gradle Version Catalog |
| Unit test | JUnit, Kotlin Coroutines Test, Robolectric |
| UI test | AndroidX Compose UI Test, Espresso |

### Konfigurasi Android saat ini

- `applicationId`: `com.winsmediatama.kenaritasik`
- `minSdk`: 24 (Android 7.0)
- `targetSdk`: 36
- `compileSdk`: 36 (minor API level 1)
- `versionName`: 1.0
- `versionCode`: 1
- Java source/target compatibility: 11

## 📁 Struktur Proyek

```text
Canary/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── firebase/
│       │   │   │   ├── model/
│       │   │   │   └── repository/
│       │   │   ├── export/
│       │   │   ├── notifications/
│       │   │   ├── ui/
│       │   │   │   ├── auth/
│       │   │   │   ├── components/
│       │   │   │   ├── screens/
│       │   │   │   ├── theme/
│       │   │   │   └── viewmodel/
│       │   │   └── utils/
│       │   └── res/
│       ├── test/
│       └── androidTest/
├── gradle/
│   └── libs.versions.toml
├── .env.example
├── firebase-blueprint.json
├── firebase.json
├── firestore.rules
├── firestore.test.js
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## 📋 Persyaratan

- Android Studio versi yang mendukung Android Gradle Plugin dan Kotlin pada proyek ini.
- JDK 11 atau toolchain yang kompatibel dengan Gradle/Android Gradle Plugin yang digunakan.
- Android SDK dengan platform API 36.
- Perangkat/emulator Android 7.0 (API 24) atau lebih baru.
- Project Firebase dengan Authentication dan Cloud Firestore yang sudah disiapkan.
- Konfigurasi aplikasi Android pada Firebase harus menggunakan package name `com.winsmediatama.kenaritasik`.

> Catatan: versi plugin dan library dapat berubah mengikuti konfigurasi di `gradle/libs.versions.toml`. Gunakan konfigurasi repository sebagai acuan utama.

## 🔥 Konfigurasi Firebase

### 1. Buat atau pilih Firebase project

Aktifkan layanan:

- **Authentication** — aktifkan metode masuk Google.
- **Cloud Firestore** — siapkan database yang digunakan aplikasi.

### 2. Daftarkan aplikasi Android

Daftarkan Android app menggunakan package name:

```text
com.winsmediatama.kenaritasik
```

Unduh `google-services.json` dari Firebase Console dan letakkan di:

```text
app/google-services.json
```

Pastikan konfigurasi OAuth dan SHA fingerprint aplikasi debug/release telah sesuai dengan keystore yang digunakan untuk build.

### 3. Database Firestore

Repository menyertakan:

- `firebase.json` — konfigurasi Firebase CLI dan emulator Firestore.
- `firestore.rules` — aturan validasi dan kontrol akses dokumen.
- `firebase-blueprint.json` — blueprint skema data aplikasi.

Konfigurasi `firebase.json` saat ini menunjuk database Firestore bernama:

```text
ai-studio-android-kenaripe-62004774-6ed3-4939-83d3-a67475913af9
```

Pastikan database tersebut tersedia pada Firebase project yang digunakan, atau sesuaikan konfigurasi database sesuai lingkungan deployment.

### 4. Secrets

File `.env.example` disediakan sebagai contoh konfigurasi secret untuk Gradle Secrets Plugin. Saat ini variabel `GEMINI_API_KEY` masih berupa contoh/komentar. Jangan memasukkan API key, service account, atau kredensial rahasia ke repository publik.

## 🏗️ Build dan Menjalankan Aplikasi

### Clone repository

```bash
git clone https://github.com/diycraftchannel83-create/Canary.git
cd Canary
```

### Siapkan Firebase

Tempatkan `google-services.json` yang cocok di folder `app/`, lalu periksa konfigurasi Firebase dan database ID sebelum menjalankan aplikasi.

### Build debug APK

Di macOS/Linux:

```bash
./gradlew assembleDebug
```

Di Windows:

```bat
gradlew.bat assembleDebug
```

Hasil APK debug umumnya berada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Build Android App Bundle

Di macOS/Linux:

```bash
./gradlew bundleRelease
```

Di Windows:

```bat
gradlew.bat bundleRelease
```

Hasil AAB umumnya berada di:

```text
app/build/outputs/bundle/release/app-release.aab
```

Build release memerlukan konfigurasi signing yang valid. Proyek membaca lokasi dan password keystore melalui environment variable:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

Alias signing release yang dikonfigurasi saat ini adalah `upload`. Simpan file keystore dan password secara privat; jangan commit ke repository.

## 🔐 Keamanan Data

Aturan Firestore menerapkan prinsip *default deny*: akses tidak diberikan kecuali dokumen dan operasi memenuhi aturan yang didefinisikan.

Perlindungan yang tersedia pada rules meliputi:

- Pengguna harus terautentikasi untuk mengakses data.
- Profil pengguna dibatasi pada UID pemiliknya.
- Data aviary dan subkoleksi dibatasi berdasarkan keanggotaan.
- Payload divalidasi menurut tipe data, panjang teks, rentang angka, dan nilai status yang diizinkan.
- Query koleksi harus mengikuti batasan keanggotaan yang diharapkan rules.
- Kode undangan memiliki format dan panjang yang divalidasi.
- Foto kenari yang telah diunggah dibuat tidak dapat diubah atau dihapus melalui update biasa.
- Catatan produksi, pasangan, kesehatan, dan suplemen memiliki validasi skema.

Sebelum rilis produksi, tinjau kembali rules terhadap semua alur aplikasi dan jalankan pengujian menggunakan Firebase Emulator Suite.

## 🧪 Pengujian

Repository menyertakan pengujian aturan Firestore pada `firestore.test.js` dengan Node.js test runner dan `@firebase/rules-unit-testing`.

Pengujian mencakup skenario seperti:

- Pengguna tanpa autentikasi ditolak.
- Isolasi profil antar pengguna.
- Pembuatan aviary dan penggunaan kode undangan.
- Akses anggota tim terhadap data bersama.
- Penolakan akses pengguna di luar tim.
- Validasi data kenari, pasangan, periode produksi, kesehatan, dan suplemen.
- Pembatasan perubahan foto kenari.

Konfigurasi emulator Firestore pada `firebase.json` menggunakan host `127.0.0.1` dan port `8085`.

Contoh menjalankan pengujian (setelah dependency Node.js dan Firebase Emulator Suite disiapkan):

```bash
firebase emulators:exec --only firestore --project demo-no-project "node --test firestore.test.js"
```

Jalankan juga pengujian Android dari Gradle:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## 📝 Catatan Implementasi

- Nama repository adalah **Canary**, sedangkan nama produk di metadata dan konfigurasi proyek adalah **Kenari Tasikmalaya**.
- Namespace Android saat ini adalah `com.example`, sedangkan `applicationId` adalah `com.winsmediatama.kenaritasik`. Jangan menyamakan keduanya saat mengatur Firebase.
- Konfigurasi Gradle menyebut beberapa library tambahan yang belum semuanya dipakai sebagai dependency aktif. Daftar teknologi di atas membedakan komponen yang benar-benar dipasang dengan library yang hanya disiapkan di version catalog.
- Metadata proyek mencantumkan kemampuan Gemini server-side, tetapi `.env.example` saat ini hanya menyediakan placeholder `GEMINI_API_KEY`. README ini tidak mengklaim fitur AI sudah tersedia pada alur aplikasi.
- Ekspor PDF/Excel dan sinkronisasi cloud tercantum pada implementasi aplikasi dan struktur kode. Hasil build, perilaku runtime, serta penerbitan Play Store perlu diverifikasi terpisah.
- Estimasi penetasan 21 hari merupakan alat bantu pencatatan. Kondisi dan waktu menetas aktual tetap dapat berbeda bergantung kondisi telur dan perawatan.

## 🤝 Kontribusi

Kontribusi untuk perbaikan bug, peningkatan UI/UX, kualitas data, keamanan Firestore, dokumentasi, dan pengujian sangat dipersilakan.

1. Fork repository.
2. Buat branch fitur atau perbaikan.
3. Lakukan perubahan dan pengujian.
4. Ajukan Pull Request dengan ringkasan perubahan dan bukti pengujian.

## 📄 Lisensi

Belum ditemukan file lisensi pada root repository. Hak penggunaan dan distribusi kode mengikuti ketentuan pemilik repository sampai file lisensi resmi ditambahkan.

---

**Kenari Tasikmalaya** — satu tempat untuk mengelola data indukan, silsilah, produksi, kesehatan, dan aktivitas breeding kenari.
