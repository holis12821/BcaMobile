---
name: frontend-push-notification
description: Integrasi sisi client (Android `bca_mobile`) untuk push notification FCM — memasang Firebase Messaging dari nol, mendapatkan token perangkat, mendaftarkannya lewat `POST /v1/account/device/push-token`, menerima pesan di foreground maupun dari system tray, membuka `deep_link` `bcamobile://`, membuat notification channel, meminta izin `POST_NOTIFICATIONS`, dan memetakan `403 AUTH_DEVICE_NOT_RECOGNIZED`. Gunakan saat diminta "integrasikan push notification", "notifikasi tidak muncul di HP", "pasang Firebase", "daftarkan FCM token", "kenapa notifikasi hanya ada di dalam aplikasi", "buka layar dari notifikasi", atau saat mengerjakan apa pun yang menyentuh `FirebaseMessagingService`, `onNewToken`, atau `registerPushToken`. Trigger juga pada "FCM", "Firebase", "push token", "push-token", "google-services.json", "onMessageReceived", "notification channel", "POST_NOTIFICATIONS", "deep link notifikasi", "badge notifikasi", "unread_count", "push_notification_enabled", dan "notifikasi keamanan tetap masuk". JANGAN gunakan untuk implementasi backend push (transport FCM, pembersihan token mati, sakelar di `ListPushTokens` — itu skill `push-notification-api` di repo backend), untuk daftar/filter/mark-read notifikasi dalam aplikasi yang sudah jalan (itu `frontend-integrasi-api-baru`), untuk OTP buka rekening (`frontend-otp-verification`), atau untuk enkripsi PIN (`frontend-pin-encryption`).
---

# Frontend — Push Notification (FCM)

Dua hal yang sering dikira satu:

| | Sumbernya | Keadaan di repo ini |
|---|---|---|
| **Notifikasi dalam aplikasi** — layar Notifikasi, lencana belum dibaca | `GET /v1/notifications` | **Sudah jalan penuh** |
| **Push ke perangkat** — muncul di notification tray, HP bergetar | FCM | **Belum ada sama sekali** |

Skill ini soal yang kedua. Yang pertama sudah selesai dan jangan diubah dari
sini — `NotificationRepository`, `NotifikasiViewModel`, filter `type`, mark-read.

Kontrak yang mengikat: `bca-mobile-api/docs/01-API-SPECIFICATION.md` §3 (push-token) dan
§7 (notifications). Sisi server ada di skill `push-notification-api` pada repo
`bca-mobile-api`. **Kalau dokumen ini berbeda dengan spec, spec yang menang** —
laporkan selisihnya, jangan diam-diam ikut dokumen ini.

---

## 1. Keadaan repo — apa yang sudah ada, apa yang belum

Setengah jalurnya sudah ditulis dan **tidak pernah dipanggil**. Periksa dulu
sebelum membuat yang baru; membuat ulang lapisan ini adalah kesalahan paling
mahal di pekerjaan ini.

**Sudah ada:**

| Berkas | Isi |
|---|---|
| `data/account/remote/AccountApi.kt` | `@POST("account/device/push-token")` `registerPushToken` |
| `data/account/remote/dto/AccountDto.kt` | `PushTokenRequest(push_token)` |
| `domain/account/AccountRepository.kt` | `registerPushToken(pushToken): DataResult<Unit>` |
| `data/account/AccountRepositoryImpl.kt` | implementasinya |
| `domain/notification/model/NotificationModels.kt` | `NotificationType` (5 nilai + `UNKNOWN`), `AppNotification.deepLink` |

**Belum ada — ini pekerjaannya:**

- Dependency `firebase-messaging` dan plugin `google-services` (tidak ada di
  `gradle/libs.versions.toml` maupun `app/build.gradle.kts`)
- `app/google-services.json`
- Izin `POST_NOTIFICATIONS` di `AndroidManifest.xml`
- Turunan `FirebaseMessagingService` beserta deklarasinya di manifest
- Notification channel
- **Pemanggil `registerPushToken`** — nol pemanggil hari ini, jadi endpoint yang
  sudah ada itu praktis kode mati
- Penanganan tap notifikasi → `deep_link`

`minSdk = 26`, `targetSdk = 36`. Dua angka itu menentukan dua hal di §3.4:
channel **selalu** wajib (API 26+), dan izin notifikasi **selalu** diminta saat
runtime (API 33+).

---

## 2. Yang dikirim backend

Backend mengirim `notification` **dan** `data`:

```json
{
  "message": {
    "token": "…",
    "notification": { "title": "Transfer berhasil", "body": "Transfer Rp100.000 ke …" },
    "data": { "type": "TRANSACTION", "deep_link": "bcamobile://transaction/3f2a…" }
  }
}
```

- `type` selalu ada, salah satu dari lima nilai `NotificationType.wireValue`.
  Nilai tak dikenal harus jatuh ke `UNKNOWN` dan **tetap ditampilkan** — pernah
  ada bug di mana `SYSTEM` tidak dikenal client lalu hilang dari setiap tab.
- `deep_link` **opsional**. Ada untuk notifikasi yang punya tujuan
  (`bcamobile://transaction/{id}`), tidak ada untuk notifikasi keamanan.
  Client **meneruskan** nilainya; jangan merakit deep link sendiri dari `type` —
  skemanya milik kesepakatan dengan backend, bukan tebakan client.

### 2.1 Jebakan paling penting di seluruh skill ini

Karena muatannya memuat blok `notification`, perilakunya **berbeda tergantung
aplikasi sedang di depan atau tidak**:

| Keadaan aplikasi | Siapa menampilkan | `onMessageReceived` |
|---|---|---|
| Foreground | kode Anda | **dipanggil** |
| Background / mati | SDK FCM → system tray | **TIDAK dipanggil** |

Akibatnya, dan ini yang biasanya salah dikerjakan: **penanganan tap tidak boleh
ditulis di `onMessageReceived`.** Notifikasi yang ditap nasabah hampir selalu
yang muncul saat aplikasi di background — yaitu justru kasus di mana metode itu
tidak pernah jalan. Tap membuka launcher activity, dan `data` sampai sebagai
**extras di Intent**. Baca dari sana (§6.2).

Kalau nanti backend berganti ke pesan data-only, `onMessageReceived` akan selalu
dipanggil dan kode Anda yang menampilkan notifikasinya. Jangan berasumsi itu
sudah terjadi: periksa muatannya.

---

## 3. Setup Firebase — sekali saja

### 3.1 `google-services.json`

Firebase Console → tambah aplikasi Android dengan package **`id.bca.bcamobile`**
(harus sama persis dengan `namespace` di `app/build.gradle.kts`, kalau tidak
token tidak akan pernah terbit) → unduh `google-services.json` → taruh di
`app/google-services.json`.

Berkas itu **bukan rahasia** — isinya identitas proyek yang juga ikut di dalam
APK. Ia tetap tidak boleh diganti sembarangan: satu berkas per proyek Firebase,
dan proyek debug berbeda dari produksi. Periksa kebijakan `.gitignore` repo ini
sebelum memutuskan ikut commit atau tidak; jangan mengubah kebijakannya sendiri.

### 3.2 Version catalog

Tambahkan ke `gradle/libs.versions.toml`, mengikuti pola yang sudah dipakai
(semua versi dipusatkan di situ, tidak ada angka versi telanjang di
`build.gradle.kts`):

```toml
[versions]
firebaseBom = "…"        # periksa versi terbaru; JANGAN percaya angka yang ditulis di sini
googleServices = "…"     # plugin com.google.gms.google-services

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

`firebase-messaging` sengaja **tanpa versi**: BoM yang menentukan, supaya seluruh
pustaka Firebase tidak pernah berbeda generasi. Versi BoM dan plugin harus
diperiksa saat mengerjakan — angka yang ditulis di dokumen cepat basi, dan
menebaknya berarti build gagal dengan pesan yang membingungkan.

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)   // tambahan
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}
```

### 3.3 Manifest

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application …>
    <service
        android:name=".core.push.BcaFirebaseMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>
</application>
```

`android:exported="false"` bukan formalitas: service yang diekspor bisa dikirimi
Intent oleh aplikasi lain.

### 3.4 Izin dan channel

**Izin.** `targetSdk = 36`, jadi di API 33+ notifikasi tidak muncul sampai
nasabah menyetujuinya. Mintanya **pada momen yang masuk akal**, bukan saat
aplikasi pertama dibuka: nasabah yang belum tahu apa gunanya cenderung menolak,
dan penolakan kedua bersifat permanen tanpa lewat Setelan sistem. Momen yang
wajar: setelah login berhasil, atau saat nasabah membuka layar Notifikasi.

Penolakan izin **bukan kegagalan**. Token tetap didaftarkan dan notifikasi tetap
tercatat di server, jadi layar Notifikasi tetap terisi. Jangan memblokir apa pun
karenanya.

**Channel.** `minSdk = 26`, jadi channel selalu wajib — tanpa channel, notifikasi
dibuang tanpa pesan error. Buat di `BcaMobileApplication.onCreate()`.

Pisahkan minimal dua channel, dipetakan dari `data["type"]`:

| Channel | Untuk | Kenapa dipisah |
|---|---|---|
| Keamanan | `SECURITY` | Nasabah tidak boleh bisa mematikannya tanpa sadar bahwa yang ia matikan adalah peringatan "kode akses Anda diubah" |
| Transaksi & info | `TRANSACTION`, `PROMO`, `SYSTEM`, `INFO`, `UNKNOWN` | Ini yang wajar dimatikan kalau terasa ramai |

Nasabah bisa mematikan channel dari Setelan sistem, dan itu di luar kendali
aplikasi maupun server — berbeda dari sakelar `push_notification_enabled` di §8.

---

## 4. Token: kapan didaftarkan

`POST /v1/account/device/push-token` butuh **bearer token**, dan perangkatnya
diambil backend dari klaim `did` di access token. Konsekuensinya langsung:
**jangan pernah mendaftarkan token sebelum login berhasil.** Sebelum itu tidak
ada `did`, dan permintaannya hanya akan dijawab 401.

Tiga titik pendaftaran, ketiganya perlu:

1. **Setelah login berhasil** — `loginWithAccessCode` dan `loginWithBiometric` di
   `AuthRepository`. Ambil token dengan `FirebaseMessaging.getInstance().token`,
   lalu kirim.
2. **`onNewToken` di service** — FCM merotasi token kapan saja. Kalau tidak
   dikirim ulang, push berhenti sampai login berikutnya, dan tidak ada gejala
   apa pun di aplikasi. Kirim hanya bila sedang login; kalau tidak, simpan dan
   kirim pada login berikutnya.
3. **Setiap aplikasi dibuka dalam keadaan sudah login.** Ini yang memulihkan
   keadaan setelah backend membersihkan token: server menghapus
   `devices.push_token` begitu FCM menjawab `UNREGISTERED`/`INVALID_ARGUMENT`,
   dan satu-satunya yang memulihkannya adalah pendaftaran ulang dari aplikasi.

Endpoint-nya **idempoten**. Jangan menambah `X-Idempotency-Key`, jangan menyimpan
"sudah pernah dikirim" untuk melewatkan panggilan — menulis nilai yang sama dua
kali tidak berbiaya, sedangkan token yang tidak terkirim membuat nasabah berhenti
menerima notifikasi tanpa satu pun tanda.

**Kegagalannya tidak boleh menghalangi apa pun.** Panggil sebagai efek samping
best-effort: login tetap berhasil, layar tetap terbuka. Pendaftaran token yang
gagal artinya push tidak sampai — bukan artinya aplikasi rusak.

Jangan mengirim `device_id` di body. Field itu tidak ada di `PushTokenRequest`
karena backend mengabaikannya dengan sengaja; kalau client bisa menyebut
perangkat, ia bisa menempelkan tokennya ke ponsel nasabah lain.

### 4.1 Logout — dan satu celah yang harus diketahui

`POST /v1/auth/logout` mencabut **sesi**, bukan baris `devices`.
`devices.push_token` tetap terisi dan `revoked_at` tetap `NULL`, jadi **server
masih akan mengirim push ke ponsel yang sudah logout**, dan tidak ada endpoint
untuk menghapus token.

Penanganan client yang benar: panggil `FirebaseMessaging.getInstance().deleteToken()`
saat logout. Token lama jadi mati, push berikutnya ke token itu dijawab
`UNREGISTERED`, dan backend membersihkan barisnya sendiri. Jadi celahnya tertutup
lewat jalur yang sudah ada, tanpa endpoint baru.

Konsekuensi yang harus diterima: login berikutnya di perangkat itu mendapat token
**baru**, jadi §4 butir 1 wajib benar-benar jalan. Kalau pendaftaran setelah
login dilewatkan, nasabah tidak akan menerima push lagi sampai aplikasi dibuka
ulang.

---

## 5. Pemetaan error

`registerPushToken` mengembalikan `DataResult<Unit>`, dan kegagalannya sudah
diklasifikasi jadi `ApiFailure`. Yang perlu diperlakukan berbeda:

| Jawaban server | `ApiFailure` | Perilaku client |
|---|---|---|
| `403 AUTH_DEVICE_NOT_RECOGNIZED` | `Business("AUTH_DEVICE_NOT_RECOGNIZED", …)` | Perangkat sudah dicabut di server. **Bersihkan sesi dan kembali ke layar masuk.** Mengulang panggilan tidak akan pernah berhasil |
| `400 VALIDATION_ERROR` | `Business("VALIDATION_ERROR", …)` | Token kosong atau > 512 karakter — ini bug client, bukan keadaan nasabah. Catat ke log, jangan tampilkan apa pun ke nasabah |
| `401 AUTH_TOKEN_INVALID` | `Business("AUTH_TOKEN_INVALID", …)` | `TokenAuthenticator` sudah mencoba refresh dan gagal, jadi sesinya benar-benar habis. Serahkan ke jalur sesi habis yang sudah ada; jangan menampilkan pesan soal notifikasi |
| jaringan / timeout / 5xx | `Network`, `Timeout`, `Server` | Diamkan. Coba lagi pada pembukaan aplikasi berikutnya; jangan retry berulang di latar belakang |

`403` itu satu-satunya yang berarti sesuatu bagi nasabah, dan artinya bukan
"notifikasi gagal" melainkan "perangkat ini sudah tidak dipercaya". Menampilkan
pesan tentang notifikasi di situ menyesatkan.

**Satu jebakan di pemeta error**, dan ini gampang membuat kode yang tidak pernah
jalan: `ApiCaller.classifyCode` diperiksa **sebelum** cabang HTTP status, dan
cabang `else`-nya menangkap kode apa pun jadi `Business`. Jadi `ApiFailure.Unauthorized`
hanya muncul untuk 401 yang **tidak** membawa `error.code` — dan backend selalu
membawanya. Mencocokkan `is ApiFailure.Unauthorized` di sini tidak akan pernah
kena. Cocokkan `Business` beserta kodenya.

---

## 6. Menerima pesan

### 6.1 Foreground — `onMessageReceived`

Yang perlu dikerjakan di sini bukan menampilkan notifikasi sistem (nasabah sedang
melihat aplikasinya), tapi **menyegarkan keadaan**: tarik ulang layar Notifikasi
kalau sedang terbuka, dan perbarui lencana belum dibaca.

Kalau ingin menampilkan sesuatu, gunakan penanda dalam aplikasi (banner/snackbar)
— bukan notifikasi tray, karena nasabah akan melihat dua hal untuk satu kejadian.

Service-nya diberi `@AndroidEntryPoint` supaya bisa menyuntik repository lewat
Hilt, sama seperti komponen Android lain di repo ini.

### 6.2 Background — dari Intent, bukan dari service

Tap pada notifikasi tray membuka `MainActivity`, dan `data` sampai sebagai extras
`Intent` (`type`, `deep_link`). Bacanya di `MainActivity`, baik pada
`onCreate(intent)` maupun `onNewIntent` — yang kedua terjadi ketika aplikasi sudah
hidup di belakang dan tidak dibuat ulang; melewatkannya membuat tap "tidak
berfungsi" hanya pada sebagian kasus, yang paling sulit dilacak.

Karena `MainActivity` satu-satunya activity dan navigasinya Compose, arahkan
tujuannya lewat nav controller setelah grafik navigasi siap. Kalau tap datang
sebelum itu, simpan dulu dan jalankan setelahnya — jangan dibuang.

### 6.3 Tujuan

- `deep_link` ada → buka tujuan itu.
- `deep_link` tidak ada, atau skemanya tidak dikenal → buka **layar Notifikasi**.
  Jangan diam di layar awal tanpa penjelasan: nasabah baru saja menekan sesuatu
  dan pantas melihat akibatnya.

Sesudah membuka layarnya, **tarik ulang dari server**. Muatan push hanya
pemberitahuan; baris notifikasi di server adalah kebenarannya, termasuk status
sudah/belum dibaca.

---

## 7. Push bukan sumber kebenaran

Backend **boleh berjalan tanpa kredensial FCM**, dan itu keadaan yang sah, bukan
kerusakan. Tanpa kredensial tidak ada satu pun push terkirim, sementara baris
notifikasinya tetap ditulis.

Yang harus benar di client karena itu:

- Layar Notifikasi **tidak boleh bergantung pada push**. Ia memuat dari
  `GET /v1/notifications`, dan itu tetap benar walau tidak ada push seumur hidup
  aplikasi.
- Jangan pernah menampilkan keadaan error atau peringatan karena push tidak
  datang. Client tidak punya cara membedakan "backend belum dikonfigurasi", "FCM
  sedang mati", dan "memang tidak ada notifikasi baru" — menebaknya berarti
  menakuti nasabah tanpa dasar.
- Jangan menyimpan notifikasi dari muatan push sebagai sumber daftar. Muatannya
  tidak punya `id` maupun status baca; menggabungkannya dengan daftar server
  menghasilkan baris ganda.

---

## 8. Sakelar `push_notification_enabled`

Sakelarnya milik `PUT /v1/account/settings` dan **ditegakkan di server**, di
lapisan token. Client tidak perlu menyaring apa pun.

Satu hal yang harus diketahui supaya tidak "diperbaiki" jadi salah: **notifikasi
`SECURITY` menembus sakelar itu.** Keputusan produk, dan alasannya masuk akal —
"kode akses Anda diubah" justru pesan yang paling dibutuhkan nasabah yang
mematikan notifikasi lain. Jadi:

- Jangan menyaring `SECURITY` di client demi "menghormati" sakelar.
- Jangan menampilkan sakelar itu sebagai "matikan semua notifikasi". Salin yang
  jujur menyebut notifikasi keamanan tetap masuk.
- Apa pun sakelarnya, baris in-app tetap ditulis dan tetap muncul di layar
  Notifikasi. Sakelar hanya meredam pengiriman ke perangkat.

---

## 9. `unread_count` — sudah ditangani, jangan "diperbaiki"

`GET /v1/notifications` **tidak mengirim** `unread_count`, walau spec §7
menjanjikannya. Client sudah tahu dan sudah menanganinya dengan sengaja — baca
komentar di `NotificationsResponse` (`data/notification/remote/dto/NotificationDto.kt`)
dan di `NotifikasiViewModel` sebelum menyentuh apa pun di sekitar ini:

- Field-nya dipertahankan dengan default `0` supaya otomatis terbaca kalau
  backend menambahkannya nanti.
- Lencana di **Beranda** memakai `unread_notifications` dari
  `GET /v1/account/dashboard`. Itu satu-satunya sumber angka belum dibaca yang
  dilayani server.
- Angka `unreadCount` di layar Notifikasi dihitung dari item yang **sudah
  termuat**, dan hanya untuk satu keperluan: menentukan tombol "Tandai Semua"
  berguna atau tidak. Ia bukan lencana, jadi tidak bersaing dengan Beranda.

Jadi tidak ada dua sumber untuk angka yang sama, dan tidak ada yang rusak di
sini. Yang harus dijaga saat mengerjakan push notification:

- **Jangan menaikkan hitungan lokal itu jadi lencana.** Ia hanya melihat halaman
  yang termuat; sebagai lencana ia akan mengecilkan jumlah sebenarnya.
- **Jangan menambah sumber kedua untuk lencana Beranda** — termasuk menghitungnya
  dari muatan push yang masuk. Muatan push tidak punya status baca.
- Setelah push diterima atau notifikasi ditandai terbaca, cara memperbarui
  lencana adalah **menarik ulang dashboard**, bukan menghitung sendiri.

Kalau backend nanti mengirim `unread_count`, satu-satunya yang berubah: hitungan
lokal itu bisa diganti nilai server, dan "Tandai Semua" mulai mencerminkan seluruh
notifikasi, bukan hanya yang termuat. Tidak ada perubahan lain yang diperlukan.

## 10. Yang jangan dilakukan

- **Jangan mencatat token push, isi notifikasi, atau PII ke log.** Badan
  notifikasi berakhir di notification tray, terlihat di layar terkunci — dan log
  client ikut terkirim saat nasabah melaporkan keluhan. Yang boleh dicatat:
  `meta.request_id`.
- **Jangan merakit `deep_link` sendiri.** Skemanya kesepakatan dengan backend.
- **Jangan menambahkan `device_id` ke body** push-token.
- **Jangan menambah `X-Idempotency-Key`.**
- **Jangan mengubah `NoopPusher`/`LoggingPusher`** — itu milik backend, dan
  keduanya sengaja tidak mengirim apa pun.
- **Jangan meminta izin notifikasi saat aplikasi pertama dibuka.**
- **Jangan menyimpulkan apa pun dari push yang tidak datang.**

---

## 11. Verifikasi

Build dan test dulu:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Lalu, dengan backend jalan (`make dev` di repo `bca-mobile-api`):

1. **Token tersimpan.** Login, lalu di sisi backend:
   ```bash
   psql "$DATABASE_URL" -c "SELECT device_id, left(push_token,12) FROM devices WHERE push_token IS NOT NULL"
   ```
   Kosong berarti §4 butir 1 tidak jalan.
2. **Rotasi token.** Hapus data aplikasi, buka lagi, login → baris di atas berisi
   token yang berbeda.
3. **Pengiriman.** Picu notifikasi (transfer, ganti PIN). Tanpa
   `FCM_CREDENTIALS_FILE` di backend, log backend mencetak
   `push (logging pusher)` dengan `devices > 0` — itu bukti jalurnya sampai ke
   tahap pengiriman. Untuk benar-benar melihat notifikasi di tray, backend harus
   dijalankan dengan kredensial FCM.
4. **Foreground vs background.** Dengan aplikasi terbuka: `onMessageReceived`
   jalan, layar Notifikasi segar. Dengan aplikasi di background: notifikasi
   muncul di tray, dan tap membuka tujuan `deep_link`.
5. **Tap saat aplikasi hidup di belakang.** Jangan dilewatkan — ini jalur
   `onNewIntent`, dan satu-satunya yang membedakannya dari kasus 4 adalah
   aplikasinya tidak dibuat ulang.
6. **Izin ditolak.** Tolak izin notifikasi, lalu pastikan aplikasi tetap berjalan
   normal dan layar Notifikasi tetap terisi.
7. **Perangkat dicabut.** Cabut perangkat di server, lalu daftarkan token →
   `403 AUTH_DEVICE_NOT_RECOGNIZED` dan aplikasi kembali ke layar masuk.

Kalau kontrak API berubah saat mengerjakan ini, `bca-mobile-api/docs/01-API-SPECIFICATION.md`
di repo ini adalah salinan — perubahannya milik repo backend, dan salinan di sini
ikut diperbarui, bukan diedit sendiri.
