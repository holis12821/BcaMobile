---
name: buka-rekening-syarat-ketentuan
description: >-
  Integrasi layar Syarat & Ketentuan buka rekening (Android `BcaMobile`) ke API
  `GET /v1/onboarding/tnc` — memindahkan lima pasal S&K dari `strings.xml` ke
  server, menghapus konstanta `TNC_VERSION`, memetakan `icon_key` ke drawable,
  dan menangani `409 TNC_VERSION_OUTDATED` saat `POST /v1/onboarding/sessions`.
  Gunakan saat mengerjakan `BukaRekeningSyaratKetentuanScreen`, `...ViewModel`,
  `...UiState`, `...Contract`, DTO/mapper S&K, atau saat teks S&K perlu bisa
  diubah tanpa rilis aplikasi. Trigger juga pada "layar S&K", "syarat dan
  ketentuan", "buka_rekening_sk", "TNC_VERSION", "accepted_tnc_version",
  "TNC_VERSION_OUTDATED", "TNC_VERSION_UNKNOWN", "TNC_UNAVAILABLE", "icon_key",
  "tncSections", "checkbox persetujuan", "Setuju & Lanjutkan". JANGAN dipakai
  untuk implementasi backend-nya (itu `buka-rekening-backend` di repo
  `bca-mobile-api`), katalog kartu Paspor (`buka-rekening-api`), layar OTP buka
  rekening (`frontend-otp-verification`), atau token visual
  (`stitch-to-compose`).
---

# Integrasi API Syarat & Ketentuan — Buka Rekening

Layar `BukaRekeningSyaratKetentuanScreen` menampilkan lima pasal S&K yang
di-hardcode di `strings.xml`, dan mengirim nomor versi dari konstanta Kotlin.
Backend sekarang melayani teks itu dan **memvalidasi** nomor versinya.

Status: **API sudah siap di backend** (migrasi `000025`). Yang belum ada adalah
pemanggilnya di aplikasi.

---

## ATURAN #0 — Jangan hapus `strings.xml` S&K sebelum DTO terbukti jalan

Lima belas entri `buka_rekening_sk_*` di `app/src/main/res/values/strings.xml`
adalah satu-satunya salinan teks ini di aplikasi. Hapus dulu, lalu DTO ternyata
salah bentuk, dan layar pertama buka rekening jadi kosong tanpa jalan kembali.

Urutannya: tambah lapisan data → render dari state → **jalankan dan lihat
layarnya** → baru hapus entri yang sudah tidak dirujuk.

Yang **tetap** tinggal di `strings.xml` meski API sudah jalan:

| Key | Kenapa tetap lokal |
|---|---|
| `buka_rekening_baru_title` | judul `AppTopBar`, bukan isi S&K |
| `buka_rekening_sk_step_label` | label `StepProgressIndicator`, milik komponen progres |

Yang **pindah** ke server: `buka_rekening_sk_heading`, `_subtitle`,
`_trust_title`, `_trust_subtitle`, `_section_1..5_title`, `_section_1..5_body`,
`_penting`, `_penting_desc`, `_checkbox_prefix`, `_checkbox_link`,
`_checkbox_suffix`, `_setuju`.

Sisakan satu teks cadangan untuk keadaan gagal — lihat **Keadaan kosong** di
bawah.

---

## ATURAN #1 — Hapus `TNC_VERSION`, jangan sekadar membacanya dari server

`BukaRekeningStepViewModel.kt:95` memegang:

```kotlin
protected companion object {
    const val TNC_VERSION = "2026-09-01"
    const val AGREEMENT_VERSION = "2026-09-01"
}
```

`TNC_VERSION` **harus hilang**. Selama konstanta itu ada, seseorang akan
memakainya lagi, dan nilainya akan membeku saat server sudah pindah versi —
yang sekarang berarti `409`, bukan sekadar catatan yang keliru.

Versi yang dikirim ke `createSession` wajib berasal dari respons
`GET /onboarding/tnc` **pada sesi tampil ini**, bukan dari konstanta, bukan dari
`DataStore`, dan bukan dari versi yang di-cache dari pembukaan layar sebelumnya.
Itu inti kontraknya: yang tercatat sebagai disetujui harus teks yang
benar-benar terpampang saat nasabah menekan tombol.

`AGREEMENT_VERSION` **tidak** disentuh — itu milik layar lain
(persetujuan data pribadi), bukan S&K ini. Jangan ikut dihapus.

---

## ATURAN #2 — `icon_key`, bukan nama drawable

Server mengirim kunci, bukan path atau URL. Petakan di satu tempat:

```kotlin
@DrawableRes
private fun tncIcon(iconKey: String): Int = when (iconKey) {
    "ACCOUNT_BOX" -> R.drawable.ic_account_box
    "VERIFIED_USER" -> R.drawable.ic_verified_user
    "VIDEO_CALL" -> R.drawable.ic_video_call
    "SAVINGS" -> R.drawable.ic_savings
    "LOCK" -> R.drawable.ic_lock
    // Kunci baru dari server TIDAK boleh menghilangkan pasalnya: teks hukum
    // yang tidak tampil jauh lebih buruk daripada ikon yang salah.
    else -> R.drawable.ic_info
}
```

Kelima drawable itu **sudah ada** di `app/src/main/res/drawable/`, begitu juga
`ic_info.xml` untuk cadangannya. Tidak ada aset baru yang perlu dibuat.

Cabang `else` bukan kerapian. Backend bisa menambah pasal keenam dengan
`icon_key` baru lewat SQL, tanpa rilis aplikasi — itu justru tujuan seluruh
perubahan ini. `when` tanpa `else` yang melempar, atau `mapOf(...)[key]!!`, akan
mengubah penambahan pasal jadi crash di layar pertama buka rekening.

---

## ATURAN #3 — `409 TNC_VERSION_OUTDATED` adalah "muat ulang", bukan "coba lagi"

Ini satu-satunya bagian yang butuh kerja di luar pemetaan DTO biasa, dan
satu-satunya yang bisa menjebak nasabah dalam lingkaran kalau dikerjakan
setengah.

`ApiCaller.classifyCode` saat ini menjatuhkan kode yang tidak dikenal ke
`ApiFailure.Business(code, message)`. `TNC_VERSION_OUTDATED` akan ikut ke sana,
dan `Business` **tidak membawa `details`** — jadi `current_version` hilang.
Yang terjadi kalau dibiarkan: nasabah melihat pesan error, menekan tombol lagi,
mengirim versi lama yang sama, dan gagal lagi. Selamanya.

Dua pilihan, dan yang pertama lebih disukai karena perilakunya eksplisit:

**(a) `ApiFailure` varian sendiri.** Tambah di
`core/network/ApiFailure.kt`:

```kotlin
/**
 * `TNC_VERSION_OUTDATED` — S&K sudah diperbarui sejak layar ini dibuka.
 *
 * [currentVersion] dari `details.current_version`. Bukan `Business`: pemulihannya
 * BUKAN mengulang request, tapi memuat ulang teks S&K dan meminta nasabah
 * menyetujui versi baru. Tanpa varian sendiri, layar akan menawarkan "coba lagi"
 * yang dijamin gagal dengan nilai yang sama.
 */
data class TncOutdated(
    val currentVersion: String,
    val message: String = "",
) : ApiFailure
```

lalu di `classifyCode`, sejajar dengan `CODE_OTP_BLOCKED`:

```kotlin
CODE_TNC_OUTDATED -> ApiFailure.TncOutdated(
    currentVersion = currentVersionFrom(error).orEmpty(),
    message = message.orEmpty(),
)
```

`currentVersionFrom` menirukan `retryAfterFrom` yang sudah ada — `runCatching`
di sekitar `details.jsonObject[...]`, karena `details` bertipe `JsonElement?`
dan bisa `null` atau bukan objek:

```kotlin
private fun currentVersionFrom(error: ApiError?): String? = runCatching {
    error?.details?.jsonObject?.get("current_version")?.jsonPrimitive?.content
}.getOrNull()
```

Perhatikan `classifyCode` dipanggil **sebelum** cabang `when (httpCode)`, jadi
status `409`-nya tidak perlu ditangani terpisah.

**(b) Tetap `Business`,** dan ViewModel memeriksa
`error.code == "TNC_VERSION_OUTDATED"`. Lebih sedikit kode, tapi
`current_version` tidak terbaca sama sekali, jadi pemulihannya harus memuat
ulang S&K tanpa tahu versi tujuannya. Bisa diterima — `GET /onboarding/tnc`
tanpa parameter memang selalu menjawab versi aktif — tapi kodenya jadi tersebar
di layar, bukan di satu tempat klasifikasi.

### Perilaku layar yang benar

Terima `TncOutdated` → **muat ulang S&K, reset checkbox, beri tahu nasabah.**

Reset checkbox-nya wajib. Nasabah yang sudah mencentang lalu teksnya berganti di
bawahnya belum menyetujui apa pun — membiarkan centang itu hidup berarti
tombolnya langsung aktif atas teks yang baru saja berubah dan belum dibaca. Itu
persis kerusakan yang backend tolak dengan `409`; mengulanginya di UI
menghilangkan gunanya.

Karena `isChecked` sekarang hidup sebagai `remember { mutableStateOf(false) }`
**di dalam** `BukaRekeningSyaratKetentuanScreen`, ViewModel tidak bisa
me-resetnya. Angkat ke `BukaRekeningSyaratKetentuanUiState` dan jadikan
`SyaratKetentuanEvent.CheckChanged(Boolean)`, atau kunci ulang `remember`
dengan versi dokumen (`remember(doc.version) { ... }`). Yang kedua lebih sedikit
kode dan cukup: versi berganti → centang kembali kosong dengan sendirinya.

`TNC_VERSION_UNKNOWN` (422) berarti aplikasi mengirim versi yang tidak pernah
ada — itu **bug client**, bukan keadaan yang bisa dipulihkan nasabah. Jangan
tangani seperti `409`; biarkan jatuh ke penanganan error umum.

`TNC_UNAVAILABLE` (503) adalah server tanpa versi aktif. `ApiCaller` akan
mengklasifikasinya `ApiFailure.Server` dan mengulang dua kali — tidak berbahaya,
tapi juga tidak menolong. Tidak perlu diapa-apakan kecuali tester melaporkan
keluhan soal lamanya; kalau iya, tambahkan kodenya ke `FINAL_5XX_CODES` dengan
alasan yang sama seperti `OTP_DELIVERY_FAILED`.

---

## Berkas yang disentuh

Kontrak lengkap + contoh JSON: `references/kontrak-api.md`.

| Berkas | Perubahan |
|---|---|
| `data/onboarding/remote/dto/TncDto.kt` | **baru** — DTO; bentuknya di `references/kontrak-api.md` |
| `data/onboarding/remote/OnboardingApi.kt` | `@GET("tnc") suspend fun tnc(@Query("version") version: String? = null)` |
| `domain/onboarding/model/OnboardingModels.kt` | model domain `TncDocument`, `TncSection`, `TncConsent` |
| `domain/onboarding/OnboardingRepository.kt` | `suspend fun tnc(): DataResult<TncDocument>` |
| `data/onboarding/OnboardingRepositoryImpl.kt` | implementasinya, lewat `ApiCaller` seperti `cardCatalog` |
| `data/onboarding/mapper/OnboardingMapper.kt` | `fun TncResponse.toDomain()` — ikuti pola `CardCatalogResponse.toDomain()` |
| `ui/.../syarat_ketentuan/BukaRekeningSyaratKetentuanUiState.kt` | muat dokumen + `isChecked` bila diangkat dari Screen |
| `ui/.../syarat_ketentuan/BukaRekeningSyaratKetentuanViewModel.kt` | muat S&K di `init`, kirim `doc.version` ke `createSession`, tangani `TncOutdated` |
| `ui/.../syarat_ketentuan/BukaRekeningSyaratKetentuanContract.kt` | tambah event muat ulang (+ `CheckChanged` bila dipilih) |
| `ui/.../syarat_ketentuan/BukaRekeningSyaratKetentuanScreen.kt` | render dari state; hapus `private fun tncSections()` |
| `ui/.../common/BukaRekeningStepViewModel.kt` | **hapus** `TNC_VERSION` (biarkan `AGREEMENT_VERSION`) |
| `core/network/ApiFailure.kt` + `ApiCaller.kt` | varian `TncOutdated` (opsi a) |
| `res/values/strings.xml` | hapus `buka_rekening_sk_*` yang sudah tidak dirujuk — **paling akhir** |

`BukaRekeningFlowState` **tidak** perlu field baru. Dokumen S&K hanya hidup
selama layar itu tampil dan tidak dibaca langkah berikutnya; menaruhnya di store
bersama hanya memperpanjang umurnya tanpa pembaca.

`AuthGraph.kt` juga tidak berubah bentuknya — `composable<BukaRekeningSyaratKetentuan>`
sudah meneruskan `state` dan `onAgreeClick`. Yang berubah hanya isi state-nya.

---

## Urutan panggilan

Layar S&K sekarang punya dua panggilan, dan urutannya penting:

```
1. init         → GET  /onboarding/tnc        (muat teks + versi)
2. tombol tekan → POST /onboarding/sessions   (accepted_tnc_version = versi dari #1)
```

Muat di `init`, bukan saat tombol ditekan. Nasabah harus bisa **membaca**
teksnya sebelum menyetujui; memuatnya di tombol berarti dia menyetujui teks yang
belum diunduh.

Katalog kartu (`GET /products/{type}/cards`) punya rate limit terpisah dari S&K,
jadi memuat keduanya pada layar berurutan tidak lagi saling menghabiskan jatah.
S&K sendiri: 30 permintaan / 5 menit.

---

## Keadaan kosong & gagal

Tiga keadaan baru yang sebelumnya tidak mungkin ada, karena teksnya dulu selalu
tersedia di dalam APK:

| Keadaan | Tampilan |
|---|---|
| sedang memuat | kerangka/`CircularProgressIndicator` di area pasal; tombol setuju **mati** |
| gagal memuat | pesan + tombol "Coba Lagi"; checkbox dan tombol setuju **mati** |
| berhasil | seperti sekarang |

Tombol setuju **tidak boleh** aktif saat dokumen belum ada. `isChecked &&
!state.isLoading` yang sekarang tidak cukup — tambahkan `state.document != null`.
Tanpa itu, sesi bisa lahir dengan `accepted_tnc_version` kosong, dan backend
menolaknya `400 VALIDATION_ERROR`: error yang benar, tapi muncul di tempat yang
membingungkan.

---

## Sebelum bilang selesai

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Lalu **jalankan aplikasinya** dan periksa di layar S&K:

- kelima pasal tampil, ikonnya benar, urutannya 1–5
- kalimat di samping checkbox menebalkan bagian tengahnya (`consent.link`)
- matikan server → pesan gagal + tombol "Coba Lagi", tombol setuju mati
- tekan setuju dengan server hidup → sesi lahir, lanjut ke langkah berikutnya

Untuk menguji jalur `409`, naikkan versi di backend lalu **hapus entri cache-nya**
— tanpa `DEL`, server masih menjawab versi lama sampai 24 jam dan jalur itu tidak
akan pernah terpicu:

```bash
psql "$DB_URL" -c "BEGIN; UPDATE onboarding_tnc_documents SET is_active=FALSE WHERE is_active; \
  INSERT INTO onboarding_tnc_documents (version,heading,subtitle,trust_title,trust_subtitle, \
  notice_label,notice_body,consent_prefix,consent_link,agree_cta,is_active) \
  VALUES ('2099-01-01','S&K uji','sub','t','ts','PENTING','n','Saya setuju ','S&K BCA','Setuju',TRUE); \
  INSERT INTO onboarding_tnc_sections (document_id,section_order,icon_key,title,body) \
  SELECT id,1,'LOCK','1. Pasal uji','isi' FROM onboarding_tnc_documents WHERE version='2099-01-01'; COMMIT;"
redis-cli -a "$REDIS_CACHE_PASSWORD" DEL onboarding:tnc:v1:active
```

Lalu, **dari aplikasi yang sudah membuka layar S&K sebelum perintah di atas**,
tekan setuju. Harus muncul pemberitahuan, teks berganti ke "S&K uji", dan
checkbox kembali kosong. Membuka layar dari awal setelah perintah itu **tidak**
menguji apa pun — versinya sudah yang terbaru.

Bersihkan setelahnya:

```bash
psql "$DB_URL" -c "BEGIN; DELETE FROM onboarding_tnc_documents WHERE version='2099-01-01'; \
  UPDATE onboarding_tnc_documents SET is_active=TRUE WHERE version='2026-09-01'; COMMIT;"
redis-cli -a "$REDIS_CACHE_PASSWORD" DEL onboarding:tnc:v1:active
```
