# API Specification — Pilih Jenis Kartu Paspor (Sisipan Flow Buka Rekening)

> Sisipan langkah **Pilih Kartu** di antara *Pilih Jenis Rekening* dan *Syarat & Ketentuan*.
> Mengikuti konvensi envelope, auth header, dan error code di `01-API-SPECIFICATION.md`,
> serta melanjutkan kontrak onboarding di `06-BUKA-REKENING-API-SPEC.md`.
>
> Layar client yang dilayani: `BukaRekeningPilihKartuScreen.kt`.

---

## 1. Kenapa sisipan ini butuh endpoint sendiri

Sampai hari ini layar pilih kartu memakai data lokal: `defaultKartuPasporList()` membaca
biaya administrasi dan limit dari `strings.xml` (`buka_rekening_kartu_blue_biaya`,
`..._tarik_tunai`, dst). Akibatnya:

- Mengubah biaya admin Rp14.000 → Rp15.000 berarti **rilis ulang APK**.
- Kartu yang stoknya habis di satu wilayah tetap terlihat bisa dipilih.
- Server tidak pernah tahu kartu mana yang dipilih nasabah, jadi `POST /onboarding/submit`
  tidak bisa membuat permintaan cetak kartu.
- Tidak ada jejak audit "nasabah melihat biaya berapa saat memilih" — ini yang diminta
  saat sengketa biaya.

Empat masalah itu yang diselesaikan dokumen ini.

---

## 2. Posisi dalam flow

```
Pilih Jenis Rekening            → GET  /v1/onboarding/products/{product_type}/cards
        ↓                          (tanpa session — session belum ada)
Pilih Kartu Paspor       ←── SISIPAN BARU
        ↓
Syarat & Ketentuan              → POST /v1/onboarding/sessions   { product_type, card_type }
        ↓
Panduan Foto → OCR → ... → Ringkasan → Berhasil
```

**Yang penting dan mudah salah:** di client, sesi onboarding baru dibuat di layar S&K
(`AuthGraph.kt`, `composable<BukaRekeningSyaratKetentuan>`), yaitu **sesudah** kartu dipilih.
Jadi endpoint katalog **tidak boleh** mensyaratkan `session_id`. Pilihan kartu ikut terkirim
saat sesi dibuat.

Tetap sediakan jalur kedua (`PUT .../card`) untuk kasus nasabah menekan Back dari S&K,
melanjutkan draf, atau mengubah kartu dari layar Ringkasan.

---

## 3. Daftar Endpoint

```
GET  /v1/onboarding/products/{product_type}/cards   ← katalog kartu per produk (publik, cacheable)
POST /v1/onboarding/sessions                        ← DIUBAH: terima field `card_type`
PUT  /v1/onboarding/sessions/{session_id}/card      ← set/ubah kartu pada sesi berjalan
GET  /v1/onboarding/sessions/{session_id}           ← DIUBAH: balas objek `card`
POST /v1/onboarding/submit                          ← DIUBAH: balas `card` + estimasi kirim
```

---

## 4. GET Katalog Kartu

```
GET /v1/onboarding/products/{product_type}/cards
```

### Path & Query

| Nama | Wajib | Keterangan |
|---|---|---|
| `product_type` (path) | ya | `TAHAPAN_BCA` \| `TAHAPAN_XPRESI` \| `TABUNGANKU` |
| `region_code` (query) | tidak | Kode wilayah untuk cek stok kartu fisik, mis. `DKI` |

### Header

| Header | Keterangan |
|---|---|
| `X-Device-Id` | Wajib. Dipakai untuk rate limit karena belum ada session/token |
| `If-None-Match` | Opsional. Isi `ETag` dari respons sebelumnya |

Endpoint ini **tidak** butuh `Authorization` maupun `session_id`.

### Response `200 OK`

```json
{
  "status": "success",
  "data": {
    "catalog_version": "2026-09-22.1",
    "product_type": "TAHAPAN_BCA",
    "default_card_type": "PASPOR_BLUE",
    "currency": "IDR",
    "cards": [
      {
        "card_type": "PASPOR_BLUE",
        "name": "Blue Mastercard",
        "network": "MASTERCARD",
        "tier_key": "DEBIT",
        "style": "BLUE",
        "badge_key": "RECOMMENDED_BEGINNER",
        "is_popular": true,
        "display_order": 1,
        "fees": {
          "monthly_admin": 14000,
          "card_issuance": 0,
          "card_replacement": 15000
        },
        "limits": {
          "cash_withdrawal": 10000000,
          "transfer_bca": 50000000,
          "transfer_interbank": 15000000,
          "debit_purchase": 50000000
        },
        "availability": {
          "status": "AVAILABLE",
          "reason_key": null
        },
        "delivery": {
          "physical_card_available": true,
          "estimated_days_min": 3,
          "estimated_days_max": 7,
          "branch_pickup_available": true
        },
        "eligibility": {
          "min_age": 17,
          "min_initial_deposit": 500000
        }
      },
      {
        "card_type": "PASPOR_GOLD",
        "name": "Gold Mastercard",
        "network": "MASTERCARD",
        "tier_key": "DEBIT",
        "style": "GOLD",
        "badge_key": "FLEXIBLE_TRANSACTION",
        "is_popular": false,
        "display_order": 2,
        "fees": { "monthly_admin": 16000, "card_issuance": 0, "card_replacement": 15000 },
        "limits": {
          "cash_withdrawal": 15000000,
          "transfer_bca": 75000000,
          "transfer_interbank": 20000000,
          "debit_purchase": 75000000
        },
        "availability": { "status": "AVAILABLE", "reason_key": null },
        "delivery": {
          "physical_card_available": true,
          "estimated_days_min": 3,
          "estimated_days_max": 7,
          "branch_pickup_available": true
        },
        "eligibility": { "min_age": 17, "min_initial_deposit": 500000 }
      },
      {
        "card_type": "PASPOR_PLATINUM",
        "name": "Platinum Mastercard",
        "network": "MASTERCARD",
        "tier_key": "PLATINUM_DEBIT",
        "style": "PLATINUM",
        "badge_key": "MAX_LIMIT",
        "is_popular": false,
        "display_order": 3,
        "fees": { "monthly_admin": 19000, "card_issuance": 0, "card_replacement": 15000 },
        "limits": {
          "cash_withdrawal": 20000000,
          "transfer_bca": 100000000,
          "transfer_interbank": 25000000,
          "debit_purchase": 100000000
        },
        "availability": { "status": "OUT_OF_STOCK", "reason_key": "STOCK_EMPTY_IN_REGION" },
        "delivery": {
          "physical_card_available": false,
          "estimated_days_min": null,
          "estimated_days_max": null,
          "branch_pickup_available": true
        },
        "eligibility": { "min_age": 17, "min_initial_deposit": 500000 }
      }
    ]
  },
  "meta": { "request_id": "req_...", "timestamp": "2026-09-22T09:00:00Z" }
}
```

Header respons: `ETag: "2026-09-22.1"` dan `Cache-Control: public, max-age=900`.
Bila `If-None-Match` cocok → `304 Not Modified` tanpa body.

### Urutan dan default

- Server mengirim `cards` **sudah terurut** sesuai `display_order`. Client tidak menyortir ulang.
- `default_card_type` adalah kartu yang terpilih saat layar pertama kali dibuka. Kalau nilainya
  menunjuk kartu yang `availability.status != "AVAILABLE"`, server wajib menurunkannya ke kartu
  tersedia pertama — jangan biarkan client menebak.

### Error Codes

| Code | HTTP | Keterangan |
|---|---|---|
| `ONBOARDING_PRODUCT_UNKNOWN` | 404 | `product_type` di luar enum |
| `ONBOARDING_PRODUCT_UNAVAILABLE` | 422 | Produk sedang maintenance |
| `CARD_CATALOG_EMPTY` | 404 | Produk valid tapi belum punya satu pun kartu aktif |
| `RATE_LIMIT_EXCEEDED` | 429 | Sertakan `details.retry_after_seconds` |

---

## 5. Aturan Penyajian Data untuk Client

Aturan di `CLAUDE.md` project Android mengunci hal ini, dan backend yang melanggarnya akan
memaksa client menanam nilai visual. **Wajib dipatuhi di sisi server:**

1. **Tidak ada warna di payload.** Tidak ada hex, tidak ada gradient stop, tidak ada URL
   gambar kartu. Yang dikirim hanya `style` (`BLUE` \| `GOLD` \| `PLATINUM`), dipetakan client
   ke token `CardArt` di `ui/theme/Color.kt`. Menambah gaya baru berarti menambah token
   di client lebih dulu — koordinasikan, jangan kirim nilai yang belum dikenal.
2. **Nominal dikirim sebagai integer rupiah penuh**, bukan string terformat.
   `14000`, bukan `"Rp14.000"`. Pemformatan milik client.
3. **Label statis dikirim sebagai key, bukan kalimat.** `badge_key: "RECOMMENDED_BEGINNER"`,
   bukan `"Rekomendasi Pemula"`. Client memetakannya ke `strings.xml`. Key yang tidak dikenal
   client → badge tidak ditampilkan, bukan crash.
4. **Nama produk boleh berupa teks apa adanya.** `name: "Blue Mastercard"` adalah data
   produk, bukan label antarmuka — ini pengecualian sah dari aturan §3 `strings.xml`.
5. **Semua field opsional wajib punya nilai default yang aman** supaya client versi lama
   tetap jalan saat field baru ditambahkan.

---

## 6. Enum Values

### card_type
```
PASPOR_BLUE | PASPOR_GOLD | PASPOR_PLATINUM
```

### style (pemetaan visual di client)
```
BLUE | GOLD | PLATINUM
```

### tier_key
```
DEBIT | PLATINUM_DEBIT
```

### badge_key
```
RECOMMENDED_BEGINNER | FLEXIBLE_TRANSACTION | MAX_LIMIT
```

### availability.status
```
AVAILABLE | OUT_OF_STOCK | DISABLED | NOT_ELIGIBLE
```

### availability.reason_key
```
STOCK_EMPTY_IN_REGION | TEMPORARILY_DISABLED | PRODUCT_MISMATCH | AGE_REQUIREMENT
```

### current_step — urutan baru

```
TNC → CARD_SELECTION → OCR → PERSONAL_DATA → OTP_VERIFY → BIOMETRIC
    → VIDEO_CALL → CREDENTIALS → REVIEW → COMPLETED
```

`CARD_SELECTION` disisipkan tepat setelah `TNC`. Lihat §7 untuk kapan step ini dilewati.

---

## 7. POST Sessions — Perubahan

```
POST /v1/onboarding/sessions
```

### Request

```json
{
  "product_type": "TAHAPAN_BCA",
  "card_type": "PASPOR_BLUE",
  "card_catalog_version": "2026-09-22.1",
  "device_id": "d_abc123",
  "accepted_tnc_version": "2026-09-01"
}
```

| Field | Wajib | Keterangan |
|---|---|---|
| `card_type` | tidak | Kosongkan bila client belum menampilkan layar kartu |
| `card_catalog_version` | tidak | Versi katalog yang dilihat nasabah; dicatat untuk audit biaya |

### Perilaku

| Kondisi | `current_step` hasil | `steps_completed.card_selected` |
|---|---|---|
| `card_type` valid & tersedia | `OCR` | `true` |
| `card_type` kosong | `CARD_SELECTION` | `false` |
| `card_type` tidak dikenal | tolak `422 CARD_TYPE_INVALID` | — |
| `card_type` tidak tersedia | tolak `409 CARD_TYPE_UNAVAILABLE` | — |

Cabang "kosong → `CARD_SELECTION`" itu yang membuat client versi lama tetap jalan, dan yang
membuat client baru bisa memilih kartu setelah sesi ada.

Bila `card_catalog_version` yang dikirim sudah bukan versi terkini, sesi tetap dibuat, tetapi
server mencatat selisihnya di audit log dan menyertakan `meta.catalog_outdated: true` supaya
client bisa menyegarkan tampilan biaya sebelum layar Ringkasan.

### Response `201 Created` (tambahan pada payload lama)

```json
{
  "status": "success",
  "data": {
    "session_id": "onb_9f8e7d6c5b4a",
    "product": { "type": "TAHAPAN_BCA", "name": "Tahapan BCA", "currency": "IDR",
                 "min_initial_deposit": 500000,
                 "features": ["Paspor BCA Mastercard Debit", "m-BCA", "KlikBCA"] },
    "card": {
      "card_type": "PASPOR_BLUE",
      "name": "Blue Mastercard",
      "style": "BLUE",
      "fees": { "monthly_admin": 14000 },
      "catalog_version": "2026-09-22.1"
    },
    "current_step": "OCR",
    "expires_at": "2026-09-23T10:30:00Z"
  }
}
```

---

## 8. PUT Kartu pada Sesi Berjalan

```
PUT /v1/onboarding/sessions/{session_id}/card
```

Dipakai saat: nasabah menekan Back dari S&K lalu ganti kartu, melanjutkan draf yang berhenti
di `CARD_SELECTION`, atau mengubah kartu dari layar Ringkasan.

### Request

```json
{
  "card_type": "PASPOR_GOLD",
  "card_catalog_version": "2026-09-22.1"
}
```

### Response `200 OK`

```json
{
  "status": "success",
  "data": {
    "card": {
      "card_type": "PASPOR_GOLD",
      "name": "Gold Mastercard",
      "style": "GOLD",
      "fees": { "monthly_admin": 16000 },
      "catalog_version": "2026-09-22.1"
    },
    "current_step": "OCR",
    "steps_completed": { "card_selected": true }
  }
}
```

`current_step` yang dibalas adalah step berjalan sesi, **bukan** selalu `OCR` — bila nasabah
mengubah kartu dari Ringkasan, nilainya tetap `REVIEW`. Client menavigasi mengikuti nilai ini.

### Kapan boleh diubah

Boleh kapan saja selama `steps_completed.submitted == false`. Setelah submit, kartu terkunci:
permintaan cetak sudah masuk ke core banking.

### Error Codes

| Code | HTTP | Keterangan |
|---|---|---|
| `ONBOARDING_NOT_FOUND` | 404 | Sesi tidak dikenal |
| `ONBOARDING_SESSION_EXPIRED` | 422 | Sesi kedaluwarsa |
| `CARD_TYPE_INVALID` | 422 | `card_type` tidak ada di katalog produk sesi ini |
| `CARD_TYPE_UNAVAILABLE` | 409 | Stok habis atau kartu dinonaktifkan |
| `CARD_NOT_ELIGIBLE` | 422 | Umur/setoran awal tidak memenuhi; sertakan `details.reason_key` |
| `CARD_LOCKED` | 409 | Pengajuan sudah disubmit |

---

## 9. GET Session — Perubahan

Tambahan pada payload `GET /v1/onboarding/sessions/{session_id}`:

```json
{
  "card": {
    "card_type": "PASPOR_GOLD",
    "name": "Gold Mastercard",
    "style": "GOLD",
    "fees": { "monthly_admin": 16000 },
    "catalog_version": "2026-09-22.1"
  },
  "steps_completed": {
    "tnc_accepted": true,
    "card_selected": true,
    "ocr_verified": false
  }
}
```

`card` bernilai `null` bila sesi berhenti sebelum kartu dipilih. `steps_completed.card_selected`
adalah field **baru** — default `false` di client lama, jadi penambahan ini aman.

---

## 10. POST Submit — Perubahan

Tambahan pada payload respons `POST /v1/onboarding/submit`:

```json
{
  "card": {
    "card_type": "PASPOR_GOLD",
    "name": "Gold Mastercard",
    "masked_number": "•••• 5678",
    "status": "REQUESTED",
    "delivery": {
      "method": "COURIER",
      "estimated_arrival_from": "2026-09-25",
      "estimated_arrival_to": "2026-09-29",
      "tracking_number": null
    }
  }
}
```

Bila `steps_completed.card_selected == false` saat submit, server menolak dengan
`ONBOARDING_INCOMPLETE` dan `details.missing_step: "CARD_SELECTION"`.

---

## 11. Skema Database

Mengikuti gaya `02-DATABASE-SCHEMA.md`.

```sql
-- Katalog kartu. Satu baris per jenis kartu, lintas produk.
CREATE TABLE card_products (
    card_type                TEXT PRIMARY KEY,
    name                     TEXT        NOT NULL,
    network                  TEXT        NOT NULL DEFAULT 'MASTERCARD',
    tier_key                 TEXT        NOT NULL,
    style                    TEXT        NOT NULL,
    currency                 CHAR(3)     NOT NULL DEFAULT 'IDR',

    fee_monthly_admin        BIGINT      NOT NULL,
    fee_card_issuance        BIGINT      NOT NULL DEFAULT 0,
    fee_card_replacement     BIGINT      NOT NULL DEFAULT 0,

    limit_cash_withdrawal    BIGINT      NOT NULL,
    limit_transfer_bca       BIGINT      NOT NULL,
    limit_transfer_interbank BIGINT      NOT NULL,
    limit_debit_purchase     BIGINT      NOT NULL,

    physical_card_available  BOOLEAN     NOT NULL DEFAULT TRUE,
    delivery_days_min        INT,
    delivery_days_max        INT,
    branch_pickup_available  BOOLEAN     NOT NULL DEFAULT TRUE,

    min_age                  INT         NOT NULL DEFAULT 17,
    min_initial_deposit      BIGINT      NOT NULL DEFAULT 0,

    is_active                BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Kartu mana yang ditawarkan untuk produk mana, beserta urutan dan status stok.
CREATE TABLE product_card_options (
    product_type            TEXT    NOT NULL,
    card_type               TEXT    NOT NULL REFERENCES card_products(card_type),
    display_order           INT     NOT NULL,
    is_default              BOOLEAN NOT NULL DEFAULT FALSE,
    is_popular              BOOLEAN NOT NULL DEFAULT FALSE,
    badge_key               TEXT,
    availability_status     TEXT    NOT NULL DEFAULT 'AVAILABLE',
    availability_reason_key TEXT,
    region_code             TEXT,   -- NULL = berlaku nasional
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_type, card_type, COALESCE(region_code, ''))
);

CREATE UNIQUE INDEX idx_product_card_default
    ON product_card_options (product_type, COALESCE(region_code, ''))
    WHERE is_default;

-- Kolom tambahan di sesi onboarding.
ALTER TABLE onboarding_sessions
    ADD COLUMN card_type            TEXT REFERENCES card_products(card_type),
    ADD COLUMN card_selected_at     TIMESTAMPTZ,
    ADD COLUMN card_catalog_version TEXT;

-- Jejak audit perubahan pilihan kartu.
CREATE TABLE onboarding_card_selection_log (
    id              BIGSERIAL PRIMARY KEY,
    session_id      TEXT        NOT NULL,
    from_card_type  TEXT,
    to_card_type    TEXT        NOT NULL,
    catalog_version TEXT        NOT NULL,
    monthly_admin_fee_shown BIGINT NOT NULL,
    actor           TEXT        NOT NULL DEFAULT 'CUSTOMER',
    ip_address      INET,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`monthly_admin_fee_shown` sengaja disalin, bukan di-join: yang perlu dibuktikan saat sengketa
adalah biaya **yang dilihat nasabah saat itu**, bukan biaya hari ini.

`UNIQUE INDEX` parsial memastikan satu produk hanya punya satu kartu default per wilayah —
penjaga terhadap kesalahan konfigurasi admin yang paling sering terjadi.

---

## 12. Strategi Cache

Mengikuti `03-REDIS-STRATEGY.md`.

| Key | Isi | TTL |
|---|---|---|
| `onb:cards:version` | Versi katalog aktif, mis. `2026-09-22.1` | tanpa TTL |
| `onb:cards:{product_type}:{region}:{version}` | JSON katalog siap kirim | 15 menit |

Aturan:

- **Invalidasi berbasis versi, bukan hapus key.** Setiap tulis admin ke `card_products` atau
  `product_card_options` menaikkan `onb:cards:version` (format `YYYY-MM-DD.n`). Key lama
  kedaluwarsa sendiri. Tidak ada jendela kosong yang memukul database.
- `ETag` respons = nilai `catalog_version`. Client menyimpannya dan mengirim `If-None-Match`.
- Cache miss → baca PostgreSQL → tulis Redis → balas. Redis mati bukan alasan gagal: fallback
  langsung ke database, catat sebagai warning.
- Katalog **tidak boleh** dicache per-session. Tidak ada PII di dalamnya; per-session hanya
  memperbanyak key tanpa manfaat.

---

## 13. Konfigurasi & Kendali Operasional

Sumber kebenaran runtime adalah **PostgreSQL**; YAML hanya untuk seed di lingkungan dev.

| Kendali | Cara | Efek |
|---|---|---|
| Ubah biaya admin / limit | `UPDATE card_products` lewat admin API | Naikkan `catalog_version` |
| Kartu habis stok di wilayah | `availability_status = 'OUT_OF_STOCK'` + `region_code` | Client menampilkan kartu tapi tidak bisa dipilih |
| Tarik kartu dari penawaran | `is_active = FALSE` | Kartu hilang dari katalog |
| Ganti kartu default | `is_default` di `product_card_options` | Pilihan awal di layar berubah |
| Matikan sisipan ini sementara | Feature flag `onboarding.card_selection.enabled = false` | Katalog balas `CARD_CATALOG_EMPTY`; sesi dibuat tanpa `card_type` dan langsung ke `OCR` |

Feature flag terakhir itu jalan keluar bila sisipan bermasalah di produksi: flow lama kembali
utuh tanpa rollback APK.

Semua perubahan konfigurasi wajib masuk audit trail (`08-Audit` di skill backend): siapa,
kapan, nilai lama, nilai baru.

---

## 14. Rate Limiting

| Endpoint | Limit |
|---|---|
| `GET /onboarding/products/{type}/cards` | 60/jam per device (respons cacheable, ini hanya penjaga) |
| `PUT /onboarding/sessions/{id}/card` | 10/session |

---

## 15. Kompatibilitas

- Client lama tidak mengirim `card_type` → sesi dibuat dengan `current_step: CARD_SELECTION`.
  Supaya client lama tidak tersangkut di step yang tidak dikenalnya, server memakai
  `User-Agent`/`X-App-Version` untuk mem-fallback ke `OCR` dan memakai kartu default produk.
  Tulis fallback ini eksplisit di kode, jangan andalkan perilaku kebetulan.
- Field `card` pada `GET sessions` dan `POST submit` bersifat tambahan dan nullable.
- `CARD_SELECTION` adalah nilai enum baru pada `current_step`. Client yang belum mengenalnya
  memetakan step tidak dikenal ke `null` (`OnboardingStep.fromWire`), jadi tidak crash —
  tapi tetap butuh fallback di atas supaya tidak berhenti diam.

---

## 16. Checklist Selesai

- [ ] `GET .../cards` balas 3 kartu terurut untuk ketiga `product_type`
- [ ] `ETag` + `304` bekerja; `Cache-Control` terpasang
- [ ] Payload tidak memuat satu pun hex warna atau string nominal terformat
- [ ] `default_card_type` selalu menunjuk kartu `AVAILABLE`
- [ ] `POST sessions` dengan `card_type` → `current_step: OCR`
- [ ] `POST sessions` tanpa `card_type` → `current_step: CARD_SELECTION`
- [ ] `PUT .../card` menolak setelah submit dengan `CARD_LOCKED`
- [ ] `POST submit` menolak sesi tanpa kartu dengan `details.missing_step: "CARD_SELECTION"`
- [ ] Setiap pilihan/perubahan kartu tercatat di `onboarding_card_selection_log`
- [ ] Menaikkan `catalog_version` benar-benar menyegarkan respons
- [ ] Feature flag mati → flow lama jalan utuh
