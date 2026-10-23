# MU Reev - Android Music Player 🎵

**MU Reev** adalah aplikasi pemutar musik Android modern yang dirancang untuk memberikan pengalaman mendengarkan musik lokal yang personal dan cerdas. Dibangun menggunakan Kotlin dengan arsitektur yang bersih, aplikasi ini merupakan bagian dari Proyek Integrasi Sistem.

## ✨ Fitur Utama

*   **Pemutaran Musik Lokal**: Manajemen file audio (MP3, FLAC, M4A, WAV dll) yang efisien menggunakan MediaStore API.
*   **Sistem Tema Dinamis**: Mendukung mode **Light** dan **Dark** yang dapat disesuaikan melalui pengaturan.
*   **Statistik Pendengaran (Stats)**: Lacak lagu yang paling sering diputar, artis favorit, dan total waktu mendengarkan.
*   **Shake to Change**: Ubah lagu secara praktis hanya dengan menggoyangkan perangkat Anda.
*   **Filter Durasi**: Bersihkan daftar putar Anda dari file audio pendek (seperti ringtone atau pesan suara) dengan filter durasi yang dapat disesuaikan.
*   **Pencarian Cerdas**: Mendukung pencarian teks dan **Pencarian Suara** untuk menemukan lagu dengan cepat.
*   **Manajemen Playlist & Favorit**: Atur koleksi musik Anda ke dalam playlist kustom.
*   **Pencarian Lirik Online**: Integrasi otomatis dengan API lirik (**LRCLIB**) untuk menampilkan lirik lagu secara real-time, mendukung format lirik sinkron (LRC) maupun teks biasa.
*   **Keamanan dengan Firebase**: Sistem otentikasi aman menggunakan Firebase Auth (Login, Register, & Reset Password).
*   **Kustomisasi Urutan (Sorting)**: Urutkan lagu berdasarkan Tanggal Ditambahkan, Judul, atau Ukuran File.

## 🛠️ Teknologi & Library

*   **Bahasa**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: Android Jetpack (View Binding, ConstraintLayout, Material Design 3)
*   **Backend**: Firebase (Authentication & Analytics)
*   **Library Pihak Ketiga**:
    *   [Glide](https://github.com/bumptech/glide) - Pemuatan gambar & album art yang efisien.
    *   [Gson](https://github.com/google/gson) - Serialisasi data untuk penyimpanan lokal.
    *   [Jaudiotagger](http://www.jthink.net/jaudiotagger/) - Membaca dan mengedit metadata file audio.
    *   [OkHttp](https://square.github.io/okhttp/) - Koneksi jaringan untuk pengambilan lirik online via [LRCLIB](https://lrclib.net/).
    *   [VerticalSeekBar](https://github.com/h6ah4i/android-widget-verticalseekbar) - Kontrol volume/progres yang unik.

## 🚀 Versi Saat Ini
**Versi 1.210.24**

## 📋 Persyaratan Izin

Untuk menjalankan aplikasi ini dengan lancar, izin berikut diperlukan:
*   `READ_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO`: Untuk memindai musik di penyimpanan.
*   `RECORD_AUDIO`: Untuk fitur pencarian suara.
*   `INTERNET`: Untuk sinkronisasi Firebase dan pengambilan lirik online.

---
**Developer:** Arif Septiawardi
**Project:** TA Proyek Integrasi Sistem
