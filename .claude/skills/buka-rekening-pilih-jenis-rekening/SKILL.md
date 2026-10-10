---
name: buka-rekening-pilih-jenis-rekening
description: >-
  Integrasi layar Pilih Jenis Rekening (Android `BcaMobile`) ke API katalog
  jenis rekening tabungan `GET /v1/onboarding/products` — copy halaman dari
  server (heading, subtitle, label setoran, kotak Persiapan Dokumen, kalimat
  S&K), setoran awal minimum sebagai rupiah penuh, daftar fitur per produk,
  badge "Paling Populer" dari data bukan posisi array, status produk tutup, dan
  fallback `strings.xml` saat katalog tidak tersedia. Gunakan saat mengerjakan
  `BukaRekeningPilihJenisScreen`, `...ViewModel`, `...UiState`, `...Contract`,
  `SavingsProductDto`, mapper/model katalog produk, atau saat menambah nilai
  enum tampilan baru dari server. Trigger juga pada "layar Pilih Jenis
  Rekening", "katalog jenis rekening", "jenis rekening", "savingsProducts",
  "SavingsProductCatalog", "ProductOptionDto", "product_type",
  "min_initial_deposit", "setoran awal minimum", "icon_key", "badge_key",
  "availability_status", "is_default", "display_order",
  "ONBOARDING_CATALOG_UNAVAILABLE", "ONBOARDING_PRODUCT_UNAVAILABLE",
  "Paling Populer", "Persiapan Dokumen", "defaultJenisRekeningList", dan
  "katalog produk tidak muncul". JANGAN dipakai untuk implementasi
  backend-nya (itu `buka-rekening-produk` di repo `bca-mobile-api`), katalog
  KARTU Paspor dan layar Pilih Kartu (itu `buka-rekening-api`), isi Syarat &
  Ketentuan (itu `buka-rekening-syarat-ketentuan`), OTP buka rekening (itu
  `frontend-otp-verification`), atau token visual (itu `stitch-to-compose`).
---

# Integrasi API Katalog Jenis Rekening — Layar Pilih Jenis Rekening

Layar `BukaRekening` di `AuthGraph` — langkah **pertama** flow buka rekening,
tampil sebelum S&K, sebelum pilih kartu, sebelum sesi lahir.

Status: **sudah tersambung**. Backend terbit di `bca-mobile-api` commit
`98c0c5c` (migrasi `000039`); sisi Android sudah memanggilnya sampai ke layar.
Skill ini merekam **kontrak**, **keputusan di balik kode yang sudah ada**, dan
**tiga celah yang masih terbuka** — bukan panduan integrasi dari nol.

Envelope, retry, dan `meta.request_id` mengikuti `bca-mobile-api/docs/01-API-SPECIFICATION.md`.
Kontrak endpoint ini: `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md` §`GET /v1/onboarding/products`.
Dokumen itu yang mengikat; skill ini mengatur **cara** mengerjakannya di sisi Android.

---

## Batas wilayah

**Trigger**: endpoint `/v1/onboarding/products` (tanpa `/cards` di belakangnya),
paket `ui/screen/buka_rekening/pilih_jenis/`, `SavingsProductDto.kt`, model
`SavingsProduct*`/`Product*` di `OnboardingModels.kt`, setoran awal minimum,
teks fitur produk, badge "Paling Populer", kotak Persiapan Dokumen, status
produk tutup.

**Bukan di sini**:

| Pekerjaan | Skill |
|---|---|
| Menulis endpoint/migrasi/cache Redis-nya | `buka-rekening-produk` di repo `bca-mobile-api` |
| Katalog kartu Paspor, `products/{type}/cards`, layar Pilih Kartu | `buka-rekening-api` |
| Teks S&K dan `accepted_tnc_version` | `buka-rekening-syarat-ketentuan` |
| Token warna, bentuk kartu, spacing | `stitch-to-compose` |
| Batas ViewModel, scoping graph, MVI | `compose-architecture` |

---

## Berkas yang memegang integrasi ini

| Berkas | Perannya |
|---|---|
| `data/onboarding/remote/OnboardingApi.kt` | `@GET("products")` → `Response<ApiEnvelope<SavingsProductCatalogResponse>>` |
| `data/onboarding/remote/dto/SavingsProductDto.kt` | `SavingsProductCatalogResponse`, `ProductOptionDto`, `ProductPageDto`, `ProductNoticeDto`, `ProductConsentDto` |
| `data/onboarding/mapper/OnboardingMapper.kt` | `SavingsProductCatalogResponse.toDomain()` — nullable, lihat ATURAN #3 |
| `domain/onboarding/model/OnboardingModels.kt` | `ProductType`, `ProductStyle`, `ProductBadge`, `ProductAvailabilityStatus`, `ProductUnavailableReason`, `SavingsProduct`, `SavingsProductCatalog` |
| `domain/onboarding/OnboardingRepository.kt` | `suspend fun savingsProducts(): DataResult<SavingsProductCatalog>` |
| `data/onboarding/OnboardingRepositoryImpl.kt` | implementasinya lewat `onboardingCall` |
| `ui/.../pilih_jenis/BukaRekeningPilihJenisViewModel.kt` | `loadCatalog()` + `defaultSelection()` |
| `ui/.../pilih_jenis/BukaRekeningPilihJenisUiState.kt` | `toPilihJenisUiState()` — katalog server **atau** `strings.xml` |
| `ui/.../pilih_jenis/BukaRekeningPilihJenisScreen.kt` | `productIconRes()`, `defaultJenisRekeningList()` |
| `ui/navigation/AuthGraph.kt` | `LaunchedEffect` → `PilihJenisEvent.ProductCatalogRequested` |
| `ui/.../common/BukaRekeningFlowState.kt` | `productCatalog`, `selectedProductType`, `selectedProduct()` |
| `test/.../BukaRekeningPilihJenisViewModelTest.kt` | enam test; lihat **Cara menguji** |

---

## Kontrak

```
GET {ONBOARDING_BASE}/products
X-Device-ID: <device_id>
```

Publik: **tanpa** `Authorization`, **tanpa** `session_id`, tanpa query parameter.
Dipanggil dari klien `@OnboardingNetwork`, bukan `@AppNetwork` — tidak ada
`AuthInterceptor` di jalur ini.

Bentuk `data`:

| Field | Tipe | Catatan |
|---|---|---|
| `catalog_version` | string | jejak; **tidak** dikirim balik saat sesi dibuat |
| `page.heading`, `.subtitle`, `.deposit_label`, `.cta_label` | string | copy layar |
| `page.notice` | objek | `icon_key`, `title`, `body` — kotak Persiapan Dokumen |
| `page.consent` | objek | `prefix`, `link`, `suffix` — **tiga potong**, lihat ATURAN #5 |
| `products[].product_type` | enum | **identitas satu-satunya** |
| `products[].name`, `.description` | string | |
| `products[].min_initial_deposit` | integer | rupiah penuh: `500000`, bukan `"Rp 500.000"` |
| `products[].currency` | string | `IDR` |
| `products[].icon_key`, `.style` | enum | bukan nilai visual, lihat ATURAN #4 |
| `products[].features` | array string | server menjamin `[]`, **tidak pernah** `null` |
| `products[].is_popular`, `.badge_key` | bool, enum\|null | badge dari data, bukan posisi |
| `products[].is_default` | bool | maksimum satu, ditegakkan unique index |
| `products[].display_order` | integer | urutan **tampilan**, bukan identitas |
| `products[].availability_status`, `.availability_reason_key` | enum, enum\|null | produk tutup tetap tampil |

### Enum yang dikenal client

| Field | Nilai sah | Pemetaan di sini |
|---|---|---|
| `icon_key` | `WALLET`, `CARD`, `SAVINGS` | `productIconRes()` di `...Screen.kt` |
| `page.notice.icon_key` | `INFO` | `productIconRes()`, konstanta `ICON_KEY_INFO` |
| `style` | `PRIMARY`, `SECONDARY`, `NEUTRAL` | `ProductStyle.fromWire` → `JenisRekeningGaya` |
| `badge_key` | `MOST_POPULAR`, `null` | `ProductBadge.fromWire` → `R.string.buka_rekening_paling_populer` |
| `availability_status` | `AVAILABLE`, `DISABLED`, `COMING_SOON` | `ProductAvailabilityStatus.fromWire` |
| `availability_reason_key` | `TEMPORARILY_DISABLED`, `MAINTENANCE`, `COMING_SOON`, `null` | `ProductUnavailableReason.fromWire` |

Menambah nilai baru di salah satu baris berarti memperbarui tabel ini **dan**
`bca-mobile-api/.claude/skills/buka-rekening-produk/references/verification.md`
di commit yang sama.

### Caching & rate limit sisi server

| Hal | Nilai |
|---|---|
| `ETag` | `"products-<catalog_version>"` |
| `Cache-Control` | `public, max-age=300` |
| `If-None-Match` cocok | `304` **tanpa body**, termasuk tanpa envelope |
| Rate limit | 30 / 5 menit per `X-Device-ID`, berlapis 600/jam per IP |

Dua baris pertama **belum dipakai client** — lihat **Celah #1**.

### Error

| Kondisi | Respons | Sikap layar |
|---|---|---|
| `X-Device-ID` kosong | `400 VALIDATION_ERROR`, `details.missing_header` | tidak boleh terjadi; lihat ATURAN #6 |
| Tidak ada produk aktif, atau `FEATURE_ONBOARDING_PRODUCT_CATALOG=false` | `503 ONBOARDING_CATALOG_UNAVAILABLE` | **keadaan normal** → fallback, lihat ATURAN #3 |
| Melewati rate limit | `429 RATE_LIMIT_EXCEEDED` + `Retry-After` | fallback |
| Produk tutup dipilih lalu sesi dibuat | `422 ONBOARDING_PRODUCT_UNAVAILABLE` di `POST /sessions` | tidak boleh sampai ke sini; lihat ATURAN #2 |

---

## ATURAN #1 — `product_type`, bukan indeks baris

`PilihJenisEvent.ProductSelected` membawa `ProductType`. Pernah membawa `Int`
dan dipetakan ke ordinal enum, dan itu **harus tetap tidak ada**: urutan tampil
datang dari `display_order` server dan produk bisa ditutup, jadi baris ke-N
bukan produk ke-N. Satu perubahan urutan di server membuat pilihan nasabah
menunjuk produk lain — nasabah membuka rekening yang bukan pilihannya, tanpa
error di mana pun.

Yang menegakkannya:

- `ProductType` tidak punya `fromIndex`. Jangan menambahkannya.
- `defaultJenisRekeningList()` pun menyebut `productType` tiap entri, jadi jalur
  fallback juga tidak bergantung pada urutan baris.
- `selectedIndex` di `UiState` **diturunkan** dari `selectedProductType`
  (`indexOfFirst`), bukan disimpan sendiri. Jangan dibalik arahnya.
- Urutan tampil: `SavingsProductCatalog.sortedProducts` (`sortedBy { displayOrder }`).
  Jangan mengurutkan ulang di layar, dan jangan memindahkan produk tutup ke
  bawah — urutan adalah keputusan product owner.

## ATURAN #2 — enum asing jatuh ke yang aman, dan "aman" berbeda per field

`fromWire` tiap enum sudah memilih arah jatuhnya, dan arahnya **tidak seragam**:

| Enum | Nilai asing jatuh ke | Kenapa arah itu |
|---|---|---|
| `ProductStyle` | `NEUTRAL` | netral tidak menonjolkan produk yang salah |
| `ProductBadge` | `NONE` | jangan mengarang label badge |
| `ProductAvailabilityStatus` | `DISABLED` | menebak "tersedia" berarti nasabah memilih produk yang lalu ditolak `422` **dua layar kemudian** |
| `ProductUnavailableReason` | `UNKNOWN` (dan `null` bila kosong) | ada kalimat umum di `strings.xml` |
| `productIconRes()` | `ic_savings` | ikon salah jauh lebih baik daripada kartu hilang |
| `ProductType` | `null` → produk **dibuang** mapper | identitas tidak boleh ditebak |

Jangan menukar `DISABLED` jadi `AVAILABLE` "supaya tidak ada produk yang hilang":
produk yang muncul lalu ditolak saat `POST /sessions` memaksa nasabah mundur dua
layar tanpa tahu kenapa.

Jangan memakai `when` tanpa `else` yang melempar, atau `mapOf(...)[key]!!`.
Backend bisa menambah nilai lewat SQL tanpa rilis aplikasi — itu justru tujuan
endpoint ini.

### Produk baru tetap butuh rilis aplikasi

`ProductType` adalah enum tertutup, jadi produk keempat dari server akan dibuang
`mapNotNull` di mapper dan **tidak** muncul di layar. Ini disengaja — identitas
produk mengalir ke `POST /sessions`, submit, dan provisioning, jadi client tidak
boleh meneruskan kode yang tidak dikenalnya. Konsekuensinya harus diingat saat
backend menambah produk: `product_type` baru = rilis APK, berbeda dari teks,
setoran awal, urutan, badge, dan status yang semuanya bisa berubah tanpa rilis.

## ATURAN #3 — katalog gagal BUKAN layar error

`loadCatalog()` membuang `DataResult.Failure` tanpa menulis `error`, dan itu
disengaja:

- Layar punya daftar bawaan `strings.xml`.
- `POST /sessions` **tidak** menuntut katalog ini pernah terbaca.
- `503 ONBOARDING_CATALOG_UNAVAILABLE` bahkan bukan kerusakan — itu jawaban
  server saat `FEATURE_ONBOARDING_PRODUCT_CATALOG=false`.

Menampilkan layar error di langkah **pertama** menghentikan pendaftaran yang
sebenarnya masih bisa jalan. Jangan "memperbaiki" ini dengan menulis `error`.

Konsekuensinya yang wajib dijaga:

- **Angka bawaan di `strings.xml` harus sama dengan isi katalog server.** Yang
  tampil itulah yang dicatat server sebagai `min_initial_deposit_shown`.
- Entri bawaan yang tetap lokal selamanya: `buka_rekening_pilih_jenis_heading`,
  `_pilih_subtitle`, `_setoran_awal`, `_lanjut`, `_persiapan_dokumen`,
  `_persiapan_dokumen_desc`, `_syarat_prefix`, `_syarat_link`, `_syarat_suffix`,
  `_paling_populer`, `_jenis_setoran_format`, `_jenis_tidak_tersedia_*`.
  **Jangan hapus** — tanpa mereka layar pertama jadi kosong saat katalog mati.
- `orBlankRes()` di `UiState`: string **kosong** dari server juga jatuh ke
  `strings.xml`, bukan hanya `null`. Pertahankan.
- Mapper mengembalikan `null` saat tidak ada satu pun produk yang bisa dipetakan
  (`mapped.isEmpty()`), dan repo menerjemahkannya ke `ApiFailure.Unknown` →
  fallback. Jangan diubah jadi `Success` berisi daftar kosong.

## ATURAN #4 — server mengirim kunci, bukan nilai visual

Tidak ada hex, gradient, nama drawable, atau URL gambar di payload — hanya
`icon_key` dan `style`. Pemetaannya ada di **satu** tempat per jenis:
`productIconRes()` untuk drawable, `ProductStyle.toGaya()` →
`JenisRekeningGaya` untuk token warna.

Kalau suatu saat backend mengusulkan mengirim hex atau URL ikon: **tolak di
review**. Itu membuat layar melanggar aturan design token repo ini, dan menaruh
keputusan visual di tabel database yang tidak pernah dilihat desainer.

## ATURAN #5 — `consent` tetap tiga potong sampai ke layar

`prefix` / `link` / `suffix` dibiarkan terpisah sampai `buildAnnotatedString` di
layar, karena `link` dicetak tebal dan berwarna. Jangan menggabungnya jadi satu
string lalu mencari substring — substring itu pecah pada setiap perbaikan kata di
server, dan pecahnya tidak terlihat sampai ada yang membaca layarnya.

Alasan dan bentuknya sama dengan `TncConsentDto` di `buka-rekening-syarat-ketentuan`.

## ATURAN #6 — `X-Device-ID` datang dari interceptor, jangan dikirim manual

Endpoint ini **mewajibkan** header itu (`400 VALIDATION_ERROR` +
`details.missing_header` tanpanya), berbeda dari `GET /onboarding/tnc` yang
membolehkannya kosong. Alasannya rate limit per device: tanpa header, semua
nasabah di belakang satu NAT operator berbagi satu jatah.

`HeaderInterceptor` sudah memasangnya di **semua** request dari
`@OnboardingNetwork`, bersumber dari `DeviceIdProvider`. Jangan menambahkan
`@Header("X-Device-Id")` di `OnboardingApi` — dua sumber nilai yang sama adalah
dua nilai yang akan berbeda, dan `device_id` di body `POST /sessions` **wajib
sama** dengan header ini.

Beda ejaan `X-Device-ID` (client) vs `X-Device-Id` (dokumen backend) **tidak**
masalah: `http.Header.Get` di Go mengkanonkan nama header. Jangan "memperbaiki"
salah satunya.

## ATURAN #7 — jangan kirim balik `catalog_version` atau setoran awal

Berbeda dari `card_catalog_version` yang ikut di body `POST /sessions`,
`catalog_version` dan `min_initial_deposit` **tidak** dikirim client. Server
mencatat `product_catalog_version` dan `min_initial_deposit_shown` sendiri dari
katalog yang sedang berlaku.

Alasannya ada di sisi backend dan mengikat kita: angka itu adalah **bukti apa
yang server tampilkan** saat sengketa. Menerimanya dari client berarti yang
disengketakan menentukan bukti sengketanya. Kalau ada permintaan menambah dua
field itu ke request, jawabannya tidak.

---

## Tiga celah yang masih terbuka

Semuanya kecil dan tidak memblokir apa pun, tapi masing-masing sudah punya
bentuk perbaikannya. Kerjakan satu-satu, jangan digabung.

### Celah #1 — `ETag` dan `max-age=300` dari server diabaikan

`NetworkModule` tidak memasang `Cache` OkHttp sama sekali, jadi `If-None-Match`
tidak pernah dikirim dan `304` tidak pernah terjadi. Cache client yang
sebenarnya ada adalah guard in-memory di `loadCatalog()`:

```kotlin
if (store.current.productCatalog != null) return
```

Umurnya = umur `BukaRekeningSessionStore` (scope flow), **tanpa TTL**. Akibatnya:
flow yang dibiarkan terbuka satu jam menampilkan setoran awal yang sudah berubah,
padahal server menandainya kadaluwarsa setelah 5 menit.

Perbaikan, dari yang paling ringan:

1. Simpan `fetchedAtMillis` di `BukaRekeningFlowState` dan tarik ulang katalog
   kalau umurnya > 5 menit. Cukup untuk masalah nyatanya.
2. Pasang `Cache` OkHttp pada klien `@OnboardingNetwork`. Kalau memilih ini:
   **uji `304` sampai ke layar**. OkHttp memang mengubah `304` jadi respons
   cache ber-status `200`, tapi `ApiCaller.call` memeriksa `response.isSuccessful`
   — dan `304` yang sampai ke Retrofit **tanpa** cache terpasang akan jatuh ke
   `classifyHttp(304, …)`, yaitu kegagalan. Jangan setengah jalan.

### Celah #2 — `503` di-retry dua kali sebelum fallback

`ONBOARDING_CATALOG_UNAVAILABLE` tidak ada di `ApiCaller.FINAL_5XX_CODES`
(isinya baru `CODE_OTP_DELIVERY_FAILED`), jadi `ApiCaller` memperlakukannya
sebagai kegagalan transport: dua percobaan ulang dengan jeda 1s lalu 3s. Layar
**pertama** buka rekening menahan `isLoading` ±4 detik sebelum menampilkan
daftar bawaan yang sejak awal sudah ada di APK.

Padahal `503` di sini adalah jawaban **final** — katalog dimatikan feature flag;
mengulanginya tidak mengubah apa pun.

Perbaikan: tambahkan konstanta kodenya dan masukkan ke `FINAL_5XX_CODES`,
sejajar `CODE_OTP_DELIVERY_FAILED`. Setelah itu `savingsProducts()` mengembalikan
`ApiFailure.Business("ONBOARDING_CATALOG_UNAVAILABLE", …)` sekali jalan, dan
`loadCatalog()` tetap membuangnya seperti sekarang — ATURAN #3 tidak berubah.

### Celah #3 — `onRetry` tidak memuat ulang katalog

Di `AuthGraph.kt`, `onRetry` memetakan ke `PilihJenisEvent.ErrorDismissed`.
Konsisten dengan ATURAN #3 (kegagalan katalog tidak pernah menulis `error`, jadi
tombol itu memang tidak pernah tampil untuk kasus ini), tapi berarti **tidak ada
jalan** meminta katalog lagi dalam satu sesi layar setelah gagal — nasabah harus
keluar dan masuk ulang flow.

Perbaikan kalau nanti dibutuhkan: `ProductCatalogRequested` yang memaksa
(mengabaikan guard `productCatalog != null`), dipicu swipe-to-refresh atau
tombol kecil di kotak Persiapan Dokumen. **Jangan** dikerjakan dengan cara
menulis `error` saat katalog gagal — itu membatalkan ATURAN #3.

---

## Cara menguji

### Unit test yang sudah ada

`test/.../BukaRekeningPilihJenisViewModelTest.kt` — enam test, dan keenamnya
menjaga aturan di atas. Jangan hapus salah satunya tanpa mengganti penjaganya:

| Test | Aturan yang dijaga |
|---|---|
| katalog dimuat saat diminta dan default server yang terpilih | `is_default` dipakai |
| tanpa penanda default, produk pertama yang bisa dipilih yang terpilih | bukan "produk pertama" |
| katalog gagal tidak menulis error dan tidak memilih apa pun | ATURAN #3 |
| pilihan nasabah menang atas default server | kembali lewat Back tidak mengubah pilihan |
| katalog yang sudah ada tidak ditarik ulang | guard `productCatalog != null` |
| produk tersimpan sebagai kode | ATURAN #1 |

Katalog palsu dirakit lewat `FakeOnboardingRepository`. Tambah kasus di situ,
bukan dengan memanggil jaringan sungguhan.

### Manual, lawan server lokal

Jalankan backend (`make dev` di `bca-mobile-api`), lalu di `.env`:

| Ubah | Yang harus terlihat di layar |
|---|---|
| `FEATURE_ONBOARDING_PRODUCT_CATALOG=false` | daftar bawaan `strings.xml`, flow tetap jalan, tidak ada layar error |
| `ONBOARDING_PRODUCTS_MAINTENANCE=TABUNGANKU` | TabunganKu **tetap tampil**, redup, "sedang dalam perbaikan", **tidak pindah ke bawah**, dan tidak bisa dipilih |
| `PUT /internal/v1/onboarding/products` memindah `is_popular` | badge "Paling Populer" pindah tanpa rilis APK |
| matikan jaringan perangkat | daftar bawaan, tanpa crash |

Cek cepat bentuk responsnya:

```bash
curl -s -H 'X-Device-Id: dev-1' http://localhost:8080/v1/onboarding/products | jq '.data.products[] | {product_type, min_initial_deposit, is_default, availability_status}'
```

Tanpa header, harus `400` dengan `details.missing_header` — bukan `200`:

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/v1/onboarding/products
```

### Sebelum bilang selesai

```bash
./gradlew :app:testDebugUnitTest --tests '*PilihJenis*'
./gradlew :app:assembleDebug
```

---

## Rujukan

| Isi | Tempat — kontrak API hidup di repo `bca-mobile-api`, **tidak** disalin ke sini |
|---|---|
| Kontrak endpoint + contoh JSON lengkap | `bca-mobile-api/docs/06-BUKA-REKENING-API-SPEC.md` |
| Keputusan sisi backend, skema, cache Redis | `bca-mobile-api/.claude/skills/buka-rekening-produk/` |
| Tabel verifikasi enum (wajib ikut berubah) | `…/buka-rekening-produk/references/verification.md` |
| Envelope, retry, header standar | `bca-mobile-api/docs/01-API-SPECIFICATION.md` |
| Base URL per lingkungan | `bca-mobile-api/docs/10-BASE-URL-DAN-ENDPOINT.md` |
| Layar S&K (pola kembar: copy dari server + fallback) | `.claude/skills/buka-rekening-syarat-ketentuan/` |
| Layar Pilih Kartu (sesudah layar ini) | `.claude/skills/buka-rekening-api/` |
