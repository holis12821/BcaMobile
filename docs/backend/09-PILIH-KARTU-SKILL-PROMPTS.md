# Skill & Prompt — API Pilih Jenis Kartu Paspor

Dokumen ini punya dua bagian yang dipakai berbeda:

1. **Skill Definition** (Bagian 1) — disalin ke project backend sebagai
   `.claude/skills/buka-rekening-kartu/SKILL.md`.
2. **Prompt Instructions** (Bagian 3) — disalin ke
   `.claude/skills/buka-rekening-kartu/references/prompts.md`, atau ditempel satu per satu
   ke AI agent saat mengerjakan tiap fase.

Kontrak yang dirujuk keduanya: `08-PILIH-KARTU-API-SPEC.md`.

---

# Bagian 1 — Skill Definition

## Penempatan berkas

```text
.claude/skills/
└── buka-rekening-kartu/        ← nama direktori = nilai `name` di frontmatter
    ├── SKILL.md                ← Bagian 1 dokumen ini
    └── references/
        └── prompts.md          ← Bagian 3 dokumen ini
```

Perintah `/buka-rekening-kartu` berasal dari **nama direktori**, bukan dari `name`.
Samakan keduanya supaya tidak ada dua sebutan untuk satu skill.

## Frontmatter

Salin apa adanya ke baris pertama `SKILL.md`:

```markdown
---
name: buka-rekening-kartu
description: Katalog dan pemilihan jenis kartu Paspor BCA pada flow buka rekening — endpoint GET /v1/onboarding/products/{type}/cards, step CARD_SELECTION, field card_type pada create session, PUT /sessions/{id}/card, tabel card_products + product_card_options, cache Redis berbasis catalog_version, feature flag, dan audit trail pemilihan kartu. Gunakan saat membuat atau memodifikasi katalog kartu, biaya/limit kartu debit, stok kartu per wilayah, atau propagasi pilihan kartu ke submit dan core banking. Trigger juga pada "katalog kartu", "kartu Paspor", "card_type", "CARD_SELECTION", "stok kartu per wilayah", dan "biaya administrasi kartu". JANGAN gunakan untuk endpoint OCR/biometrik/video call (itu skill `buka-rekening-onboarding`), endpoint auth/login/PIN (itu skill `auth`), atau UI Android (project terpisah).
---
```

Hanya `name` dan `description` yang wajib. Bidang opsional — `allowed-tools`,
`disable-model-invocation`, `model`, `paths` — sengaja tidak dipakai: skill ini murni
instruksi, tidak membawa skrip, dan tidak perlu pra-persetujuan tool.

## Konvensi penamaan yang dipatuhi

| Aturan konvensi skill Claude | Penerapan di sini |
|---|---|
| `name` hanya huruf kecil, angka, dan tanda hubung; maksimum 64 karakter | `buka-rekening-kartu` — 19 karakter |
| `name` tidak memuat kata terlarang (`claude`, `anthropic`) dan tanpa tag XML | terpenuhi |
| `name` sama persis dengan nama direktori skill | terpenuhi |
| `description` tidak kosong, maksimum 1.024 karakter, tanpa tag XML | 808 karakter |
| `description` menyebut **apa** yang dikerjakan **dan kapan** dipakai | "Katalog dan pemilihan …" + "Gunakan saat …" + "JANGAN gunakan untuk …" |
| `description` ditulis sebagai orang ketiga, bukan "saya bisa membantu…" | terpenuhi |
| Badan `SKILL.md` di bawah 500 baris | Bagian 1 ± 110 baris; prompt panjang pindah ke `references/prompts.md` |
| Rujukan berkas cukup satu tingkat dari `SKILL.md` | `references/prompts.md` dirujuk langsung, tidak lewat perantara |
| Berkas rujukan di atas 100 baris punya daftar isi | Bagian 3 dibuka dengan daftar isi |
| Nama konsisten dengan keluarga skill yang sudah ada | sejajar `buka-rekening-api`, `buka-rekening-native-android`, `buka-rekening-video-call`, `buka-rekening-onboarding` |
| Path memakai garis miring maju | semua path di dokumen ini |

Nama lama `onboarding-card-catalog` ditinggalkan karena memutus pola keluarga
`buka-rekening-*` — konsistensi penamaan itu yang membuat satu skill bisa dirujuk dari
skill lain tanpa salah tunjuk.

> **Catatan lintas repo:** skill backend onboarding disebut `buka-rekening-onboarding` di
> `07-BUKA-REKENING-BACKEND-SKILL-PROMPTS.md`, tetapi `.claude/skills/buka-rekening-api/SKILL.md`
> di project Android menyebutnya `buka-rekening-backend`. Satu nama harus dipilih sebelum
> kedua skill saling merujuk; dokumen ini memakai `buka-rekening-onboarding` mengikuti
> definisi skill itu sendiri.

## Batas wilayah

**Trigger**: endpoint `/v1/onboarding/products/*/cards`, field `card_type` di mana pun,
tabel `card_products`/`product_card_options`, step `CARD_SELECTION`, stok kartu, biaya
administrasi kartu, estimasi pengiriman kartu fisik.

**Jangan trigger** untuk: OCR/Dukcapil, biometrik, video call, kredensial, pembuatan
rekening itu sendiri (semua ada di skill `buka-rekening-onboarding`); endpoint auth;
UI/Compose Android.

**Prompt implementasi bertahap**: lihat `references/prompts.md` — delapan fase, masing-masing
dengan *Definition of Done*.

## Aturan wajib

1. **Payload tidak memuat nilai visual.** Tidak ada hex warna, gradient, atau URL gambar
   kartu. Hanya enum `style`. Client memetakannya ke design token; mengirim hex membuat
   client melanggar aturan token dan perubahan akan ditolak di review.
2. **Nominal integer, bukan string terformat.** `14000`, bukan `"Rp14.000"`.
3. **Label statis dikirim sebagai key** (`badge_key`, `reason_key`), bukan kalimat Indonesia.
4. **Katalog tidak pernah dibaca langsung dari database di jalur panas.** Selalu lewat cache
   berbasis `catalog_version`; database hanya untuk cache miss dan admin write.
5. **Setiap pemilihan dan perubahan kartu ditulis ke audit log** beserta biaya yang ditampilkan
   saat itu. Ini syarat kepatuhan, bukan tambahan opsional.
6. **Kartu terkunci setelah submit.** Tidak ada jalur kode yang boleh mengubah `card_type`
   sesi yang sudah `submitted`.
7. **Field baru selalu nullable dengan default aman.** Client lama harus tetap jalan.
8. **Sisipan ini harus bisa dimatikan lewat feature flag** tanpa rollback deployment.
9. **Jangan mengubah urutan step yang sudah ada.** `CARD_SELECTION` disisipkan setelah `TNC`;
   step lain tidak bergeser namanya maupun artinya.

## Struktur file yang disentuh

```
onboarding-service/
├── handler/
│   ├── card_catalog.go        ← BARU: GET katalog
│   ├── card_selection.go      ← BARU: PUT kartu pada sesi
│   ├── session.go             ← UBAH: terima card_type, balas card
│   └── submit.go              ← UBAH: validasi card_selected, balas card+delivery
├── service/
│   ├── card_catalog_service.go ← BARU: baca katalog + cache + versi
│   └── card_selection_service.go ← BARU: validasi & simpan pilihan
├── repository/
│   └── card_repo.go           ← BARU
├── model/
│   └── card.go                ← BARU
└── migrations/
    └── 0xx_card_products.sql  ← BARU
```

---

# Bagian 2 — Informasi yang Wajib Dikumpulkan Lebih Dulu

Agent **tidak boleh** mulai menulis kode sebelum sembilan hal ini punya jawaban tertulis.
Kalau salah satu belum ada, tanyakan; jangan diisi dengan tebakan yang kelihatan masuk akal.

| # | Yang harus diketahui | Kenapa penting | Sumber |
|---|---|---|---|
| 1 | Daftar kartu per produk tabungan — apakah `TAHAPAN_XPRESI` dan `TABUNGANKU` menawarkan tiga kartu yang sama seperti `TAHAPAN_BCA`? | Menentukan isi `product_card_options`; salah di sini membuat nasabah ditawari kartu yang tidak bisa diterbitkan | Product owner |
| 2 | Angka resmi biaya administrasi, biaya penerbitan, dan biaya penggantian per kartu | Nilai di `strings.xml` client (Rp14.000/16.000/19.000) adalah **data desain**, bukan angka resmi produk | Product owner / tarif resmi |
| 3 | Angka resmi keempat limit per kartu | Sama seperti di atas; limit salah berujung sengketa | Product owner |
| 4 | Aturan eligibility — umur minimum, setoran awal minimum, apakah Platinum butuk syarat tambahan | Menentukan `NOT_ELIGIBLE` dan kapan dicek (sebelum OCR, data umur belum ada) | Risk / compliance |
| 5 | Apakah stok kartu dipantau per wilayah, dan dari sistem mana angkanya | Menentukan perlu-tidaknya `region_code` dan integrasi inventaris | Operasional kartu |
| 6 | SLA pengiriman kartu fisik dan apakah ambil di cabang tersedia untuk semua kartu | Mengisi objek `delivery` | Operasional kartu |
| 7 | Siapa yang boleh mengubah katalog dan lewat antarmuka apa | Menentukan admin endpoint + otorisasi | Engineering manager |
| 8 | Format `card_type` yang dipakai core banking saat permintaan cetak kartu | Kalau berbeda dengan enum kita, butuh tabel pemetaan | Tim core banking |
| 9 | Berapa lama jejak audit pemilihan kartu harus disimpan | Menentukan retensi `onboarding_card_selection_log` | Compliance |

Jawaban 2 dan 3 yang paling sering dilewati. **Jangan** menyalin angka dari `strings.xml`
client ke database produksi tanpa konfirmasi — angka itu dibuat untuk desain layar.

Konteks teknis yang sudah pasti dan tidak perlu ditanyakan:

- Sesi onboarding dibuat **setelah** kartu dipilih (client membuatnya di layar S&K), jadi
  endpoint katalog tanpa session.
- Envelope, error shape, dan `meta.request_id` mengikuti `01-API-SPECIFICATION.md`.
- Client memetakan `style` ke token `CardArt`; hanya `BLUE`, `GOLD`, `PLATINUM` yang dikenal.

---

# Bagian 3 — Prompt Instructions

## Daftar isi

- Prompt 1: Migrasi Database & Seed Katalog
- Prompt 2: Repository & Service Katalog + Cache
- Prompt 3: Handler GET Katalog
- Prompt 4: card_type pada Create Session & State Machine
- Prompt 5: PUT Kartu pada Sesi Berjalan
- Prompt 6: Propagasi ke Submit & Core Banking
- Prompt 7: Admin API & Feature Flag
- Prompt 8: Observability & Audit

Kerjakan berurutan. Setiap prompt punya *Definition of Done*; jangan lanjut sebelum terpenuhi.

---

## Prompt 1: Migrasi Database & Seed Katalog

```
Buat migrasi database untuk katalog kartu Paspor sesuai §11 di
docs/08-PILIH-KARTU-API-SPEC.md.

Yang dibuat:
1. Tabel card_products — identitas kartu, fee, empat limit, delivery, eligibility,
   is_active.
2. Tabel product_card_options — pemetaan produk↔kartu, display_order, is_default,
   is_popular, badge_key, availability_status, availability_reason_key, region_code.
3. Unique index parsial: satu default per (product_type, region_code).
4. ALTER onboarding_sessions: card_type, card_selected_at, card_catalog_version.
5. Tabel onboarding_card_selection_log untuk audit, termasuk kolom
   monthly_admin_fee_shown.
6. Migrasi down yang benar-benar mengembalikan skema.

Seed hanya untuk lingkungan dev: tiga kartu (PASPOR_BLUE, PASPOR_GOLD,
PASPOR_PLATINUM) dipetakan ke TAHAPAN_BCA dengan PASPOR_BLUE sebagai default dan
is_popular. Beri komentar di file seed bahwa angka fee dan limit adalah placeholder
dev dan wajib diganti angka resmi sebelum staging.

JANGAN menanam angka fee/limit produksi. JANGAN menyimpan warna atau path gambar
kartu di tabel mana pun.
```

**Definition of Done**: migrasi up/down jalan bersih dua kali berturut-turut; unique index
menolak dua default pada produk yang sama; seed hanya aktif di dev.

---

## Prompt 2: Repository & Service Katalog + Cache

```
Buat card_repo.go, model/card.go, dan service/card_catalog_service.go.

Repository:
- ListCards(ctx, productType, regionCode) ([]CardOption, error) — join card_products
  dengan product_card_options, filter is_active, urut display_order. Baris dengan
  region_code cocok menang atas baris nasional (region_code NULL).
- GetCard(ctx, productType, cardType) (CardOption, error)
- BumpCatalogVersion(ctx) (string, error) — format YYYY-MM-DD.n

Service:
- GetCatalog(ctx, productType, regionCode) (Catalog, error) dengan urutan:
  baca onb:cards:version → coba key onb:cards:{product}:{region}:{version} →
  cache miss: baca DB, susun payload, tulis Redis TTL 15 menit.
- Redis error bukan kegagalan request: fallback ke DB, log warning, tetap balas 200.
- default_card_type diambil dari is_default; bila kartu itu tidak AVAILABLE,
  turunkan ke kartu AVAILABLE pertama. Jangan serahkan keputusan ini ke client.
- Katalog kosong → error domain CardCatalogEmpty.

Tulis unit test untuk: cache hit, cache miss, Redis down, default tidak tersedia,
override per wilayah, dan katalog kosong.
```

**Definition of Done**: enam test di atas hijau; Redis dimatikan, endpoint tetap melayani.

---

## Prompt 3: Handler GET Katalog

```
Buat handler/card_catalog.go untuk
GET /v1/onboarding/products/{product_type}/cards.

Ketentuan:
- Tanpa Authorization, tanpa session_id. Wajib header X-Device-Id.
- Query opsional region_code.
- Validasi product_type terhadap enum; di luar enum → 404 ONBOARDING_PRODUCT_UNKNOWN.
- Produk maintenance → 422 ONBOARDING_PRODUCT_UNAVAILABLE.
- Katalog kosong → 404 CARD_CATALOG_EMPTY.
- Rate limit 60/jam per device → 429 RATE_LIMIT_EXCEEDED dengan
  details.retry_after_seconds dan header Retry-After.
- Set ETag: "<catalog_version>" dan Cache-Control: public, max-age=900.
  If-None-Match cocok → 304 tanpa body.
- Bentuk payload persis §4 docs/08-PILIH-KARTU-API-SPEC.md.
- Feature flag onboarding.card_selection.enabled = false → balas CARD_CATALOG_EMPTY.

Tambahkan integration test yang membandingkan JSON hasil dengan contoh di §4,
dan satu test yang gagal bila payload memuat pola hex warna (#RRGGBB) atau string
nominal berformat "Rp".
```

**Definition of Done**: test perbandingan payload hijau; test penjaga hex/"Rp" benar-benar
gagal saat sengaja disisipkan.

---

## Prompt 4: card_type pada Create Session & State Machine

```
Ubah handler/session.go dan service/session_service.go.

1. CreateSessionRequest menerima card_type dan card_catalog_version, keduanya opsional.
2. Sisipkan CARD_SELECTION ke state machine tepat setelah TNC. Step lain tidak bergeser.
3. Perilaku sesuai §7 docs/08-PILIH-KARTU-API-SPEC.md:
   - card_type valid & AVAILABLE → simpan, card_selected=true, current_step=OCR
   - card_type kosong → current_step=CARD_SELECTION
   - card_type tidak dikenal → 422 CARD_TYPE_INVALID
   - card_type tidak tersedia → 409 CARD_TYPE_UNAVAILABLE
4. Validasi card_type terhadap katalog produk sesi, bukan terhadap daftar global.
5. card_catalog_version berbeda dari versi terkini → sesi tetap dibuat,
   sertakan meta.catalog_outdated=true, catat di audit.
6. Fallback client lama: bila X-App-Version di bawah ambang yang dikonfigurasi dan
   card_type kosong, pakai kartu default produk dan lanjut ke OCR. Tulis eksplisit,
   beri komentar kenapa.
7. Respons memuat objek card seperti contoh spesifikasi.
8. Tulis onboarding_card_selection_log saat kartu tersimpan, termasuk
   monthly_admin_fee_shown dari katalog versi yang dikirim client.

Perbarui test state machine yang ada supaya urutan baru terbaca, tanpa mengubah
ekspektasi step setelah OCR.
```

**Definition of Done**: keempat cabang §7 punya test; test flow lama (tanpa `card_type`,
client lama) tetap sampai `COMPLETED`.

---

## Prompt 5: PUT Kartu pada Sesi Berjalan

```
Buat handler/card_selection.go dan service/card_selection_service.go untuk
PUT /v1/onboarding/sessions/{session_id}/card sesuai §8 spesifikasi.

Ketentuan:
- Boleh dipanggil selama steps_completed.submitted == false; setelah itu 409 CARD_LOCKED.
- Validasi kartu terhadap katalog produk sesi: CARD_TYPE_INVALID,
  CARD_TYPE_UNAVAILABLE, CARD_NOT_ELIGIBLE (sertakan details.reason_key).
- Sesi tidak ada → 404 ONBOARDING_NOT_FOUND; kedaluwarsa → 422
  ONBOARDING_SESSION_EXPIRED.
- Respons mengembalikan current_step sesi yang sebenarnya, bukan selalu OCR.
  Bila sesi berada di CARD_SELECTION, majukan ke OCR.
- Setiap perubahan menulis onboarding_card_selection_log dengan from_card_type dan
  to_card_type.
- Rate limit 10 per session.
- Perubahan kartu tidak boleh menyentuh data step lain (OCR, biometrik, kredensial).

Test: ubah kartu di CARD_SELECTION, ubah dari REVIEW (current_step tetap REVIEW),
tolak setelah submit, tolak kartu milik produk lain, dan pastikan panggilan
berturut-turut idempoten untuk card_type yang sama.
```

**Definition of Done**: kelima test di atas hijau; tidak ada jalur kode yang mengubah kartu
setelah submit.

---

## Prompt 6: Propagasi ke Submit & Core Banking

```
Ubah handler/submit.go dan service/account_service.go.

1. Tolak submit bila steps_completed.card_selected == false:
   422 ONBOARDING_INCOMPLETE dengan details.missing_step = "CARD_SELECTION".
2. Saat membuat rekening, kirim permintaan penerbitan kartu ke core banking
   memakai pemetaan card_type → kode kartu core banking. Simpan pemetaan di
   konfigurasi, bukan di kode.
3. Respons submit memuat objek card sesuai §10: masked_number, status
   (REQUESTED | PRINTING | SHIPPED), dan delivery dengan estimasi tanggal dihitung
   dari delivery_days_min/max katalog.
4. Penerbitan kartu gagal sementara core banking rekeningnya sudah jadi → rekening
   tetap ACTIVE, card.status = REQUESTED, dan kegagalan masuk antrean retry.
   Jangan menggagalkan seluruh submit karena kartu.
5. Hormati X-Idempotency-Key yang sudah ada: submit ulang tidak boleh menghasilkan
   dua permintaan cetak kartu.

Test: submit tanpa kartu ditolak, submit normal menghasilkan tepat satu permintaan
cetak, submit ulang dengan idempotency key sama tidak menambah permintaan cetak,
dan kegagalan penerbitan kartu tidak membatalkan rekening.
```

**Definition of Done**: keempat test hijau; retry idempoten terbukti lewat test, bukan asumsi.

---

## Prompt 7: Admin API & Feature Flag

```
Buat endpoint admin internal untuk mengelola katalog, dilindungi
X-Internal-Service-Key dan peran admin:

- GET    /internal/v1/cards
- PUT    /internal/v1/cards/{card_type}              (fee, limit, delivery, is_active)
- PUT    /internal/v1/products/{product_type}/cards/{card_type}
         (display_order, is_default, is_popular, badge_key, availability_status,
          availability_reason_key, region_code)

Ketentuan:
- Setiap write menaikkan catalog_version tepat satu kali per transaksi, bukan per baris.
- Setiap write menulis audit: aktor, waktu, nilai lama, nilai baru.
- Menonaktifkan kartu yang sedang menjadi default ditolak kecuali default baru
  disertakan dalam permintaan yang sama.
- badge_key dan style divalidasi terhadap daftar nilai yang dikenal client.
  Nilai di luar daftar ditolak 422 dengan pesan yang menyebutkan nilai yang sah.
- Feature flag onboarding.card_selection.enabled dibaca dari konfigurasi runtime,
  bukan environment variable yang butuh restart.

Test: menaikkan versi menyegarkan respons katalog publik; menonaktifkan default
ditolak; style/badge tidak dikenal ditolak; mematikan flag mengembalikan flow lama.
```

**Definition of Done**: mematikan flag membuat seluruh test flow lama hijau tanpa perubahan
kode lain.

---

## Prompt 8: Observability & Audit

```
Lengkapi instrumentasi untuk sisipan ini.

Metrik:
- onboarding_card_catalog_requests_total{product_type,cache_result}
- onboarding_card_selected_total{product_type,card_type}
- onboarding_card_changed_total{from,to}
- onboarding_card_unavailable_total{card_type,reason_key}
- Histogram latensi GET katalog, dipisah cache hit dan miss.

Log terstruktur (tanpa PII): session_id, product_type, card_type, catalog_version,
request_id.

Audit query yang harus bisa dijawab dalam satu SELECT:
- "Kartu apa yang dipilih sesi X, dan biaya administrasi berapa yang ditampilkan
  saat itu?"
- "Berapa kali nasabah mengganti kartu sebelum submit, bulan ini?"

Tambahkan alert: rasio CARD_TYPE_UNAVAILABLE di atas 5% dari pemilihan dalam 15
menit — pertanda konfigurasi stok salah, bukan perilaku nasabah.
```

**Definition of Done**: kedua pertanyaan audit terjawab lewat satu query; alert diuji dengan
data sintetis.

---

# Bagian 4 — Verifikasi Lintas Tim

Sebelum menutup pekerjaan, cek tujuh hal ini bersama client Android:

- [ ] Nilai `style` yang dikirim hanya `BLUE`, `GOLD`, `PLATINUM`
- [ ] Nilai `badge_key` yang dikirim sudah punya padanan di `strings.xml` client
- [ ] Tidak ada hex warna atau string `"Rp..."` di seluruh payload
- [ ] Urutan `cards` dari server sama dengan urutan yang client tampilkan
- [ ] Client mengirim `card_catalog_version` yang ia terima, apa adanya
- [ ] `CARD_SELECTION` dikenali `OnboardingStep.fromWire` di client sebelum sisipan
      diaktifkan di produksi
- [ ] Kartu `OUT_OF_STOCK` tampil tapi tidak bisa dipilih — bukan disembunyikan

Butir terakhir keputusan produk, bukan teknis: menyembunyikan kartu membuat nasabah bertanya
ke call center kenapa Platinum hilang. Menampilkannya sebagai tidak tersedia menjawab
pertanyaan itu sebelum ditanyakan.
