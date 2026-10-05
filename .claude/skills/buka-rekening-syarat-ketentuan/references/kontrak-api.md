# Kontrak API — Syarat & Ketentuan Buka Rekening

Dibaca langsung dari `internal/domain/onboarding/entity.go` (`TNCDocument` dkk)
dan `internal/handler/onboarding_handler.go` (`GetTNC`) di repo
`bca-mobile-api`, bukan dari contoh di dokumen. Kalau ada selisih antara berkas
ini dan `docs/06-BUKA-REKENING-API-SPEC.md` §0b, yang menang adalah handler —
laporkan selisihnya, jangan pilih salah satu diam-diam.

Spec backend: `docs/06-BUKA-REKENING-API-SPEC.md` §0b.
Skema tabel: `docs/02-DATABASE-SCHEMA.md`, bagian S&K (migrasi `000025`).

---

## `GET /v1/onboarding/tnc`

Tanpa `Authorization`, tanpa `session_id`. `X-Device-ID` opsional — kalau
dikirim, jatah rate limit dihitung per perangkat alih-alih per IP.

| Query | Wajib | Arti |
|---|---|---|
| `version` | tidak | Kosong = versi yang **sedang** berlaku. Diisi = versi tertentu, termasuk yang sudah dicabut |

Rate limit 30 / 5 menit. Jatah terpisah dari katalog kartu.

### `200 OK`

```json
{
  "status": "success",
  "data": {
    "version": "2026-09-01",
    "heading": "Syarat & Ketentuan Pembukaan Rekening",
    "subtitle": "Mohon baca dan pahami syarat dan ketentuan pembukaan rekening digital BCA sebelum melanjutkan.",
    "trust_banner": {
      "title": "Persetujuan Resmi Nasabah",
      "subtitle": "Terdaftar dan diawasi oleh Otoritas Jasa Keuangan (OJK)"
    },
    "sections": [
      {
        "icon_key": "ACCOUNT_BOX",
        "title": "1. Ketentuan Umum Pembukaan Rekening Digital",
        "body": "Calon nasabah merupakan Warga Negara Indonesia (WNI) dengan usia minimal 17 tahun …"
      },
      {
        "icon_key": "VERIFIED_USER",
        "title": "2. Kebijakan Privasi & Penggunaan Data",
        "body": "Data pribadi Anda dilindungi kerahasiaannya …"
      },
      {
        "icon_key": "VIDEO_CALL",
        "title": "3. Ketentuan eKYC & Video Call",
        "body": "Verifikasi tatap muka digital wajib dilakukan …"
      },
      {
        "icon_key": "SAVINGS",
        "title": "4. Komitmen Setoran Awal",
        "body": "Setoran awal minimal sesuai jenis rekening yang dipilih …"
      },
      {
        "icon_key": "LOCK",
        "title": "5. Penggunaan Fasilitas m-BCA",
        "body": "Nasabah bertanggung jawab penuh menjaga kerahasiaan Kode Akses, PIN transaksi, dan kode OTP …"
      }
    ],
    "notice": {
      "label": "PENTING",
      "body": "Pastikan Anda berada di tempat tenang dan pencahayaan cukup untuk verifikasi video call pada tahap berikutnya."
    },
    "consent": {
      "prefix": "Saya telah membaca, memahami, dan menyetujui seluruh ",
      "link": "Syarat & Ketentuan Pembukaan Rekening BCA",
      "suffix": "."
    },
    "agree_cta": "Setuju & Lanjutkan",
    "effective_from": "2026-08-31T17:00:00Z",
    "is_active": true
  },
  "meta": { "request_id": "...", "timestamp": "..." }
}
```

### Catatan per field

| Field | Catatan integrasi |
|---|---|
| `version` | **Wajib** dikirim kembali apa adanya sebagai `accepted_tnc_version`. Maks 20 karakter (dibatasi kolom di sisi sesi) |
| `trust_banner` | Mengisi `TrustBanner` yang sekarang memakai `_trust_title` / `_trust_subtitle` |
| `sections` | Selalu ≥ 1 pasal; server menolak dokumen tanpa pasal dengan `503`. Urutan sudah benar dari server — **jangan** disortir ulang di client |
| `sections[].icon_key` | Kunci, bukan path. Kunci baru harus jatuh ke ikon cadangan, bukan melewati pasalnya |
| `notice` | Kotak PENTING. `label` ikut dari server, jadi "PENTING" tidak lagi hardcode |
| `consent` | Tiga potongan karena `link` dicetak tebal + berwarna oleh `buildAnnotatedString`. **Jangan** disatukan lalu dicari substring-nya — substring itu pecah pada setiap perbaikan kata |
| `agree_cta` | Label tombol. Mengganti "Setuju & Lanjutkan" tidak lagi menuntut rilis |
| `effective_from` | ISO-8601 UTC. Tidak dipakai layar ini; ada untuk tampilan arsip |
| `is_active` | `false` hanya mungkin pada respons `?version=`. Dokumen ber-`is_active: false` **tidak boleh** menawarkan tombol setuju — persetujuannya pasti ditolak `409` |

Semua field skalar bertipe string non-null di server. Tetap beri nilai default
pada DTO (`= ""`) mengikuti pola `CardDto`, supaya penambahan field di server
tidak memecah deserialisasi.

### Caching

```
ETag: "tnc-2026-09-01"
Cache-Control: public, max-age=300
```

`ETag` memuat versi yang **benar-benar dilayani**, bukan nilai query — jadi saat
versi aktif berganti, ETag-nya berganti sendiri. `If-None-Match` dengan ETag yang
sama menjawab `304` tanpa body. OkHttp menangani ini sendiri bila `Cache` sudah
dipasang di `OkHttpClient`; tidak ada yang perlu ditulis tangan.

### Error

| Code | HTTP | Arti | Penanganan client |
|---|---|---|---|
| `TNC_VERSION_UNKNOWN` | 422 | `?version=` tidak ada di database | Hanya mungkin bila client mengirim `?version=` karangan. Error umum |
| `TNC_UNAVAILABLE` | 503 | Tidak ada versi aktif di server | Salah konfigurasi server (migrasi `000025` belum jalan), bukan salah request. Jatuh ke `ApiFailure.Server` dan diulang 2×; tampilkan keadaan gagal + "Coba Lagi" |
| `RATE_LIMIT_EXCEEDED` | 429 | Lewat 30 / 5 menit | `ApiFailure.RateLimited`, sudah tertangani `ApiCaller` |

---

## `POST /v1/onboarding/sessions` — yang berubah

Hanya satu field yang perilakunya berubah; sisanya sama.

```json
{
  "product_type": "TAHAPAN_BCA",
  "device_id": "d_abc123",
  "accepted_tnc_version": "2026-09-01",
  "card_type": "PASPOR_BLUE",
  "card_catalog_version": "2026-09-23.1"
}
```

`accepted_tnc_version` sekarang **diperiksa ke database**, bukan sekadar dicek
tidak kosong. Nilainya harus sama dengan `version` yang dilayani
`GET /onboarding/tnc` saat layar itu tampil.

Diperiksa **sebelum** batas 3 sesi per perangkat, jadi versi yang salah tidak
menghabiskan jatah sesi — percobaan ulang setelah memuat ulang S&K tidak akan
kena `ONBOARDING_SESSION_LIMIT`.

### Error baru

| Code | HTTP | `details` | Penanganan client |
|---|---|---|---|
| `TNC_VERSION_OUTDATED` | 409 | `sent_version`, `current_version` | **Muat ulang S&K, reset checkbox, beri tahu nasabah.** Bukan "coba lagi": mengulang dengan nilai yang sama dijamin gagal |
| `TNC_VERSION_UNKNOWN` | 422 | — | Bug client (versi karangan atau konstanta yang tertinggal). Error umum |
| `TNC_UNAVAILABLE` | 503 | — | Server tanpa versi aktif |

Contoh `409`:

```json
{
  "status": "error",
  "error": {
    "code": "TNC_VERSION_OUTDATED",
    "message": "Syarat & Ketentuan telah diperbarui. Mohon baca dan setujui versi terbaru.",
    "details": {
      "sent_version": "2026-09-01",
      "current_version": "2026-12-01"
    }
  },
  "meta": { "request_id": "...", "timestamp": "..." }
}
```

`message` sudah berbahasa Indonesia dan lebih spesifik daripada teks cadangan di
aplikasi — pakai itu, seperti yang sudah dilakukan `ApiFailure.RateLimited`.

---

## Bentuk DTO

Ikuti pola `CardDto.kt`: `@Serializable`, `@SerialName` untuk setiap nama
`snake_case`, default pada semua field.

```kotlin
package id.bca.bcamobile.data.onboarding.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Syarat & Ketentuan buka rekening.
 * Kontrak: `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md` §0b.
 */
@Serializable
data class TncTrustBannerDto(
    val title: String = "",
    val subtitle: String = "",
)

@Serializable
data class TncSectionDto(
    @SerialName("icon_key") val iconKey: String = "",
    val title: String = "",
    val body: String = "",
)

@Serializable
data class TncNoticeDto(
    val label: String = "",
    val body: String = "",
)

@Serializable
data class TncConsentDto(
    val prefix: String = "",
    val link: String = "",
    val suffix: String = "",
)

@Serializable
data class TncResponse(
    val version: String = "",
    val heading: String = "",
    val subtitle: String = "",
    @SerialName("trust_banner") val trustBanner: TncTrustBannerDto? = null,
    val sections: List<TncSectionDto> = emptyList(),
    val notice: TncNoticeDto? = null,
    val consent: TncConsentDto? = null,
    @SerialName("agree_cta") val agreeCta: String = "",
    @SerialName("effective_from") val effectiveFrom: String = "",
    @SerialName("is_active") val isActive: Boolean = false,
)
```

Objek bersarang `nullable` dengan default `null` mengikuti `CardDto` (`fees`,
`limits`, `availability` semuanya begitu): respons yang cacat jadi keadaan yang
bisa ditangani, bukan `SerializationException` di layar pertama buka rekening.

Mapper mengubahnya jadi nilai non-null dengan default kosong — lihat
`CardCatalogResponse.toDomain()` sebagai contoh. Satu-satunya yang **tidak**
boleh di-default jadi "kosong lalu lanjut" adalah `version` dan `sections`:
dokumen tanpa versi tidak bisa disetujui, dan dokumen tanpa pasal tidak boleh
menampilkan tombol setuju. Perlakukan keduanya sebagai kegagalan muat.
