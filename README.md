# Salat DND — prototipe toggle Android

Proyek Kotlin minimal untuk Android 6+ (termasuk Samsung Galaxy A11 Android 10). Saat ini hanya menguji izin dan toggle DND manual; belum ada jadwal salat otomatis.

## Build APK
1. Buka folder `SalatDND` dengan Android Studio.
2. Tunggu Gradle Sync selesai.
3. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Instal APK hasil build di Galaxy A11.
5. Buka aplikasi, tekan **Beri Izin DND**, lalu aktifkan akses untuk **Salat DND** di pengaturan Android.
6. Kembali ke aplikasi dan coba tombol Aktifkan/Matikan DND.

## Catatan
- Tombol membaca status DND aktual dari NotificationManager.
- Mode ON menggunakan `INTERRUPTION_FILTER_NONE` (membisukan semua interupsi, kecuali pengecualian sistem seperti panggilan telepon tertentu).
- Mode OFF menggunakan `INTERRUPTION_FILTER_ALL`.
- Versi awal ini tidak mengubah DND otomatis berdasarkan jadwal salat.
