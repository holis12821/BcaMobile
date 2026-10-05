# Prompt Android — Menyambungkan Layar Pilih Kartu ke API

Sisipan langkah **Pilih Kartu Paspor** di antara *Pilih Jenis Rekening* dan
*Syarat & Ketentuan*, dari sisi client Android.

Kontrak backend: `bca-mobile-api/docs/08-PILIH-KARTU-API-SPEC.md`.
Skill & prompt backend: `docs/backend-prompts/09-PILIH-KARTU-SKILL-PROMPTS.md`.

---

## 1. Keadaan Sekarang

Layarnya sudah ada dan sudah terpasang di navigasi — yang belum ada hanya datanya.

| Berkas | Keadaan |
|---|---|
| `ui/screen/buka_rekening/BukaRekeningPilihKartuScreen.kt` | Lengkap: tiga kartu, badge, limit, state loading/error sudah ada |
| `BukaRekeningPilihKartuScreen.kt:80` `defaultKartuPasporList()` | Data kartu dibaca dari `strings.xml` — biaya dan limit **tertanam di APK** |
| `BukaRekeningUiStates.kt:32` `toPilihKartuUiState()` | Selalu mengembalikan `defaultKartuPasporList()`, mengabaikan state |
| `BukaRekeningFlowContract.kt` `CardTypeSelected(index)` | Hanya menyimpan `selectedCardIndex`, tidak pernah dikirim ke server |
| `ui/navigation/AuthGraph.kt:163` | Komentar di sana sudah menyatakan "Pilihan kartu belum menyentuh API" |
| `data/onboarding/remote/OnboardingApi.kt` | Belum punya endpoint kartu |
| `domain/onboarding/model/OnboardingModels.kt:22` `OnboardingStep` | Belum punya `CARD_SELECTION` |

Yang dituju: kartu, biaya, limit, dan ketersediaan datang dari server; pilihan nasabah ikut
terkirim saat sesi dibuat; daftar bawaan `strings.xml` turun pangkat menjadi **fallback offline**.

---

## 2. Aturan yang Mengikat Pekerjaan Ini

Dari `CLAUDE.md` dan skill `stitch-to-compose`:

1. **Warna kartu tetap dari token.** API mengirim `style: "BLUE" | "GOLD" | "PLATINUM"`;
   pemetaannya ke `CardArt` sudah ada di `BukaRekeningPilihKartuScreen.kt:657` dan seterusnya.
   Jangan menambah jalur yang menerima warna dari jaringan.
2. **`style` tidak dikenal → jangan menebak.** Petakan ke `KartuPasporGaya.BLUE` sebagai
   nilai aman dan laporkan; jangan membuat token baru sendiri.
3. **Label baru masuk `strings.xml`, additive.** Entri lama tidak dihapus — ia menjadi fallback.
4. **Composable tetap stateless.** Pemanggilan API ada di ViewModel/repository, bukan di layar.
5. **Tidak ada `.dp`/`.sp` telanjang** pada perubahan apa pun di layar ini.
6. `./scripts/check-hardcoded-ui.sh` wajib bersih sebelum pekerjaan dilaporkan selesai.

---

## 3. Urutan Panggilan yang Benar

```
BukaRekening (Pilih Jenis)
   └─ ProductSelected(index)
        └─ GET /products/{product_type}/cards        ← dipanggil di sini, bukan di layar kartu
BukaRekeningPilihKartu
   └─ CardTypeSelected(index)                        ← lokal saja, belum ada sesi
BukaRekeningSyaratKetentuan
   └─ TncAccepted
        └─ POST /sessions { product_type, card_type, card_catalog_version }
```

Katalog diambil saat produk dipilih supaya layar kartu terbuka dengan data sudah siap.
Kalau katalog gagal dimuat, layar kartu tetap terbuka memakai fallback `strings.xml`, dan
`card_type` tetap dikirim saat membuat sesi — server yang memutuskan kartu itu sah atau tidak.

---

## Prompt 1: DTO & Endpoint

```
Tambahkan endpoint katalog kartu ke client onboarding.

1. Buat data/onboarding/remote/dto/CardDto.kt dengan @Serializable:
   - CardCatalogResponse(catalog_version, product_type, default_card_type, currency,
     cards: List<CardOptionDto>)
   - CardOptionDto(card_type, name, network, tier_key, style, badge_key, is_popular,
     display_order, fees: CardFeesDto, limits: CardLimitsDto,
     availability: CardAvailabilityDto, delivery: CardDeliveryDto)
   - CardFeesDto(monthly_admin, card_issuance, card_replacement)
   - CardLimitsDto(cash_withdrawal, transfer_bca, transfer_interbank, debit_purchase)
   - CardAvailabilityDto(status, reason_key)
   - CardDeliveryDto(physical_card_available, estimated_days_min, estimated_days_max,
     branch_pickup_available)
   Nominal bertipe Long. Setiap field punya nilai default supaya field baru dari server
   tidak memecah parsing. Ikuti gaya @SerialName di dto yang sudah ada.

2. Tambahkan ke data/onboarding/remote/OnboardingApi.kt:
   @GET("products/{product_type}/cards")
   suspend fun getCardCatalog(
       @Path("product_type") productType: String,
       @Query("region_code") regionCode: String? = null,
   ): Response<ApiEnvelope<CardCatalogResponse>>

   @PUT("sessions/{session_id}/card")
   suspend fun setSessionCard(
       @Path("session_id") sessionId: String,
       @Body request: SetCardRequest,
   ): Response<ApiEnvelope<SetCardResponse>>

3. Tambahkan card_type dan card_catalog_version (keduanya nullable) ke
   CreateSessionRequest, dan objek card (nullable) ke CreateSessionResponse serta
   GetSessionResponse di SessionDto.kt. Tambahkan card_selected ke StepsCompletedDto
   dengan default false.

JANGAN menambahkan field warna dalam bentuk apa pun ke DTO.
```

**Selesai bila**: proyek dikompilasi dan DTO lama tetap terparsing dengan payload lama.

---

## Prompt 2: Domain Model, Mapper, Repository

```
1. Tambahkan ke domain/onboarding/model/OnboardingModels.kt:
   - enum class CardType(val wireValue: String) { PASPOR_BLUE, PASPOR_GOLD,
     PASPOR_PLATINUM } dengan fromWire()
   - enum class CardStyle { BLUE, GOLD, PLATINUM } dengan fromWire() yang
     mengembalikan BLUE untuk nilai tidak dikenal
   - enum class CardAvailability { AVAILABLE, OUT_OF_STOCK, DISABLED, NOT_ELIGIBLE }
   - data class PasporCard(type, name, tierKey, style, badgeKey, isPopular,
     monthlyAdminFee: Long, limits: CardLimits, availability, deliveryDaysMin/Max)
   - data class CardCatalog(version, defaultCardType, cards: List<PasporCard>)
   - Sisipkan CARD_SELECTION("CARD_SELECTION") ke OnboardingStep, tepat setelah TNC.
     Jangan mengubah urutan atau wireValue step lain.

2. Perluas data/onboarding/mapper/OnboardingMapper.kt dengan pemetaan DTO→domain.
   Kartu yang card_type-nya tidak dikenal dibuang dari daftar, bukan membuat seluruh
   respons gagal.

3. Perluas domain/onboarding/OnboardingRepository.kt:
   suspend fun getCardCatalog(productType: ProductType): DataResult<CardCatalog>
   suspend fun setSessionCard(cardType: CardType): DataResult<OnboardingStep>
   dan ubah createSession() menjadi menerima cardType: CardType? dan
   catalogVersion: String?.

4. Implementasikan di OnboardingRepositoryImpl memakai ApiCaller yang sudah ada.
   Simpan catalog_version terakhir di memory repository supaya bisa dikirim ulang
   saat createSession — jangan ditulis ke OnboardingSessionStore, ini bukan data sesi.

5. Katalog boleh di-cache di memory selama proses hidup; jangan ditulis ke disk.
```

**Selesai bila**: `createSession` lama masih punya pemanggil yang benar, dan unit test mapper
menutup kasus `card_type` tidak dikenal serta `style` tidak dikenal.

---

## Prompt 3: State & ViewModel

```
1. BukaRekeningFlowContract.kt — tambahkan ke BukaRekeningFlowState:
   - cardCatalog: CardCatalog? = null
   - selectedCardType: CardType? = null
   - isCatalogLoading: Boolean = false
   Pertahankan selectedCardIndex sebagai turunan tampilan, atau hapus dan ganti
   seluruh pemakaiannya dengan selectedCardType — pilih satu, jangan menyimpan dua
   sumber kebenaran.

2. Tambahkan event CardCatalogRequested. CardTypeSelected(index) tetap bertanda tangan
   sama supaya layar tidak berubah: ViewModel menerjemahkan index ke CardType lewat
   urutan cardCatalog.cards.

3. BukaRekeningFlowViewModel:
   - ProductSelected memicu pengambilan katalog untuk produk itu.
   - Katalog berhasil → set cardCatalog, dan selectedCardType = defaultCardType.
   - Katalog gagal → biarkan cardCatalog null; layar memakai fallback bawaan.
     Jangan menampilkan layar error penuh untuk kegagalan katalog: nasabah masih
     bisa meneruskan flow.
   - CardTypeSelected menolak kartu yang availability != AVAILABLE, dan memancarkan
     ShowMessage alih-alih mengubah pilihan.
   - TncAccepted mengirim selectedCardType dan catalog version ke createSession.
   - Sesi yang balik dengan current_step CARD_SELECTION diperlakukan seperti step lain:
     navigasi mengikuti nilai server, bukan tebakan lokal.

4. Tambahkan penanganan side effect AdvanceTo(CARD_SELECTION) → BukaRekeningPilihKartu.
```

**Selesai bila**: memutus jaringan lalu membuka layar kartu tetap menampilkan tiga kartu
bawaan dan flow bisa diteruskan sampai S&K.

---

## Prompt 4: UI State Mapper & Layar

```
1. Buat core/format/CurrencyFormatter.kt: fungsi memformat Long rupiah menjadi
   "Rp14.000" memakai Locale("id","ID") dan NumberFormat, tanpa desimal.
   Belum ada formatter serupa di project — jangan menyalin logika ke beberapa tempat.

2. BukaRekeningUiStates.kt — ubah toPilihKartuUiState() supaya memetakan
   cardCatalog.cards ke List<KartuPaspor> bila katalog ada, dan
   defaultKartuPasporList() bila null. Nominal diformat di sini, bukan di composable.

3. BukaRekeningPilihKartuScreen.kt — tambahkan pada data class KartuPaspor:
   - isAvailable: Boolean = true
   - unavailableLabel: String? = null
   Kartu tidak tersedia tetap tampil, tetapi selectable dimatikan dan diberi label
   ketidaktersediaan. JANGAN menyembunyikan kartunya.

4. Badge: petakan badge_key ke string resource yang sudah ada
   (RECOMMENDED_BEGINNER → buka_rekening_kartu_blue_badge,
    FLEXIBLE_TRANSACTION → buka_rekening_kartu_gold_badge,
    MAX_LIMIT → buka_rekening_kartu_platinum_badge).
   Key tidak dikenal → badge tidak ditampilkan.

5. Tambahkan string baru ke strings.xml, additive saja. Kalau sebuah label butuh
   token warna/spacing/radius yang belum ada di ui/theme/, BERHENTI dan laporkan —
   jangan membuat token baru dan jangan membulatkan ke token terdekat.

6. Pertahankan keempat preview yang sudah ada, dan tambahkan satu preview untuk
   kartu tidak tersedia.
```

**Selesai bila**: `./scripts/check-hardcoded-ui.sh` bersih dan kelima preview merender.

---

## Prompt 5: Navigasi & Resume Draf

```
1. AuthGraph.kt composable<BukaRekening>: ProductSelected tetap diikuti
   navigate(BukaRekeningPilihKartu). Katalog dimuat oleh ViewModel, layar tidak menunggu.

2. composable<BukaRekeningPilihKartu>: hapus komentar "Pilihan kartu belum menyentuh
   API" dan ganti dengan keterangan bahwa pilihan dikirim saat sesi dibuat di layar S&K.
   onLanjutClick tetap navigate(BukaRekeningSyaratKetentuan).

3. Resume draf: sesi yang kembali dengan current_step CARD_SELECTION harus mendarat
   di BukaRekeningPilihKartu, lalu PUT /sessions/{id}/card dipanggil saat nasabah
   menekan Lanjut — bukan createSession, karena sesinya sudah ada.
   Bedakan kedua jalur ini eksplisit di ViewModel: sessionId null → createSession,
   sessionId ada → setSessionCard.

4. Back dari S&K ke layar kartu lalu ganti kartu: bila sesi sudah terlanjur dibuat,
   panggil setSessionCard; bila belum, cukup ubah state lokal.

5. Perbarui .claude/skills/compose-architecture/references/navigation.md pada commit
   yang sama — struktur navigasi di berkas itu bersifat mengikat.
```

**Selesai bila**: tiga skenario diuji manual — flow baru, resume dari `CARD_SELECTION`, dan
ganti kartu setelah sesi dibuat.

---

## Prompt 6: Uji & Pemeriksaan Akhir

```
Unit test:
- Mapper: card_type tidak dikenal dibuang; style tidak dikenal jatuh ke BLUE.
- ViewModel: katalog gagal → fallback dipakai, flow tetap bisa lanjut.
- ViewModel: memilih kartu OUT_OF_STOCK tidak mengubah selectedCardType dan
  memancarkan ShowMessage.
- ViewModel: TncAccepted mengirim card_type dan catalog_version yang benar.
- ViewModel: sessionId ada → setSessionCard, sessionId null → createSession.

Lalu jalankan:
  ./gradlew testDebugUnitTest
  ./gradlew lintDebug
  ./scripts/check-hardcoded-ui.sh
  ./gradlew assembleDebug
```

---

## 4. Yang Perlu Disepakati dengan Backend Sebelum Mulai

- Nilai `badge_key` yang benar-benar dikirim server, supaya pemetaan ke `strings.xml` tidak meleset.
- Apakah `region_code` dipakai; kalau ya, dari mana client mengambilnya.
- Ambang `X-App-Version` untuk fallback client lama (§15 spesifikasi backend).
- Apakah `TAHAPAN_XPRESI` dan `TABUNGANKU` menawarkan kartu yang sama — memengaruhi jumlah
  kartu yang harus dirender dan apakah layar butuh state "hanya satu kartu".

Empat hal ini yang paling sering membuat integrasi diulang. Tanyakan di depan.
