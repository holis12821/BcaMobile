# START HERE

Panduan memulai, urut dari nol, untuk project `bca_mobile` — Android Studio, 100% Compose,
desain tersimpan di project Stitch.

> Dokumen ini menggantikan versi sebelumnya. Kalau kamu masih menyimpan
> `docs/design-system-setup.md` atau `docs/navigation-architecture.md`, **hapus keduanya** —
> isinya sudah pindah dan dua dokumen berbeda akan menyesatkan.
> Simpan file ini di `docs/START-HERE.md`.

**Total: sekitar 3 jam**, terbagi 7 fase. Tiap fase punya checkpoint. Jangan lanjut kalau
checkpoint belum hijau.

Kalau hari ini cuma ada 30 menit: kerjakan **Fase 0 dan Fase 1**. Keduanya berdiri sendiri
dan tidak meninggalkan project setengah jadi.

---

## Peta file

Dua skill, dua wilayah. Ini yang paling sering membingungkan, jadi baca tabelnya dulu.

| File | Dibaca oleh | Kapan | Taruh di |
|---|---|---|---|
| `START-HERE.md` | **kamu** | sekali, saat setup | `docs/` |
| `CLAUDE.md` | agent | **selalu**, tiap sesi | root repo |
| `stitch-to-compose/SKILL.md` | agent | saat kerja desain → Compose | `.claude/skills/` |
| ├ `references/design-tokens.md` | agent | saat butuh nilai token | idem |
| ├ `references/mapping-html-to-compose.md` | agent | saat menerjemahkan layout | idem |
| └ `references/screen-inventory.md` | agent | saat menggarap screen | idem |
| `compose-architecture/SKILL.md` | agent | saat kerja navigasi / state | `.claude/skills/` |
| └ `references/navigation.md` | agent | saat menyentuh graph / back stack | idem |
| `.mcp.json` | Claude Code | saat startup | root repo |
| `check-hardcoded-ui.sh` | shell | tiap commit | `scripts/` |

**Pembagian wilayah kedua skill:**

| Pertanyaan | Skill |
|---|---|
| Warna, spacing, tipografi, susunan elemen | `stitch-to-compose` |
| Elemen apa yang ada di sebuah screen | `stitch-to-compose` |
| Screen ini route atau dialog? Masuk graph mana? | `compose-architecture` |
| State screen tinggal di mana, siapa yang navigate | `compose-architecture` |
| Back stack, logout, auto-lock, deep link | `compose-architecture` |

`stitch-to-compose` tetap **inti alur kerja harian** — itu yang kamu pakai tiap kali menggarap
screen. `compose-architecture` adalah pendamping yang menyala saat pekerjaan menyentuh
struktur, dan isi skill Stitch tidak berubah selain satu paragraf penunjuk (§6b).

**Kenapa navigasi jadi skill terpisah, bukan ditambahkan ke dalam `stitch-to-compose`:**
karena pekerjaan navigasi sering muncul di sesi yang tidak menyebut desain sama sekali —
"kenapa Back kembali ke Login", "tambah ViewModel", "sesi timeout". Kalau aturan navigasi
tinggal di dalam skill Stitch, sesi-sesi itu tidak akan pernah memuatnya, dan agent akan
mengarang polanya sendiri. Selain itu `stitch-to-compose/SKILL.md` sudah 220+ baris;
menambah navigasi ke situ mendorongnya ke ukuran yang cenderung diabaikan.

Yang perlu **kamu** baca cuma baris pertama tabel. Sisanya untuk mesin, dan tidak perlu kamu
sebut ke agent — masing-masing skill menyala sendiri lewat `description`-nya.

---

## Kenapa urutannya begini

Disusun berdasarkan **ketergantungan**, bukan urutan dokumen.

Token, font, dan theme tidak membutuhkan Stitch — jadi dikerjakan lebih dulu supaya ada
kemajuan pasti sebelum menyentuh hal yang bisa tersendat. Rangka navigasi dipasang **sebelum**
screen pertama, karena memasang screen ke dalam graph yang salah berarti membongkarnya lagi
nanti. MCP Stitch di Fase 5, tepat sebelum benar-benar dipakai untuk menarik HTML layout.

---

# FASE 0 — Kenali project (15 menit)

### 0.1 Buka project di Android Studio

### 0.2 Ganti tampilan panel Project

Dropdown pojok kiri atas panel Project: **Android** → **Project**.

Tanpa ini `.claude/`, `CLAUDE.md`, `docs/`, dan `scripts/` **tidak terlihat sama sekali**.
View "Android" menyembunyikan apa pun di luar module.

### 0.3 Verifikasi namespace

`app/build.gradle.kts`:

```kotlin
android {
    namespace = "id.bca.bcamobile"   // ← nilai yang diharapkan
}
```

Semua contoh di dokumen ini, `CLAUDE.md`, dan `design-tokens.md` §6 sudah memakai
`id.bca.bcamobile`. Kalau berbeda, cari-ganti di kedua file itu **sebelum** Fase 2 —
mengganti package setelah file Kotlin ditulis berarti menyisir ulang seluruh import.

### 0.4 Baseline hijau

```bash
./gradlew assembleDebug
```

Harus sukses sebelum menambah apa pun. Kalau sudah merah dari awal, perbaiki dulu — nanti
kamu tidak bisa membedakan error lama dari akibat perubahan baru.

### ✅ Checkpoint Fase 0

- [x] Panel Project mode **Project**, folder di luar `app/` terlihat
- [x] `namespace` = `id.bca.bcamobile` (atau sudah dicari-ganti)
- [x] `./gradlew assembleDebug` sukses

---

# FASE 1 — Pasang aturan (10 menit)

### 1.1 Pasang kedua skill

```bash
cd /path/ke/bca_mobile          # root repo, berisi settings.gradle.kts
mkdir -p .claude/skills
unzip ~/Downloads/skills-bundle.zip -d .claude/skills/
```

Verifikasi:

```bash
find .claude/skills -type f
```

Harus keluar **tepat 6 file**:

```
.claude/skills/stitch-to-compose/SKILL.md
.claude/skills/stitch-to-compose/references/design-tokens.md
.claude/skills/stitch-to-compose/references/mapping-html-to-compose.md
.claude/skills/stitch-to-compose/references/screen-inventory.md
.claude/skills/compose-architecture/SKILL.md
.claude/skills/compose-architecture/references/navigation.md
```

Kalau muncul folder bersarang ganda, naikkan isinya satu level. Skill tidak terbaca dengan
nesting ganda. Skill **harus di root repo**, bukan di dalam `app/`.

### 1.2 Pasang `CLAUDE.md` dan panduan

```bash
cp ~/Downloads/CLAUDE.md .
mkdir -p docs && cp ~/Downloads/START-HERE.md docs/
rm -f docs/design-system-setup.md docs/navigation-architecture.md   # kalau ada
```

Package, module, dan versi SDK di `CLAUDE.md` sudah terisi sesuai hasil audit. Yang masih
kosong hanya bagian **`## Git`** — isi konvensi tim, atau hapus kalau project personal.

### 1.3 `.gitignore`

```gitignore
.claude/settings.local.json
```

**Jangan** ignore `.claude/` seluruhnya. Skill harus ter-commit supaya seluruh tim dan CI
memakai aturan yang sama.

### 1.4 Verifikasi kedua skill hidup

Restart Claude Code, lalu:

```
/skills
```

**Dua-duanya** harus muncul: `stitch-to-compose` dan `compose-architecture`.

Uji trigger masing-masing, tanpa menulis kode:

```
Token warna apa yang dipakai untuk nominal transaksi negatif di layar Mutasi?
```

Benar kalau merujuk ramp `Danger` — itu `stitch-to-compose`.

```
Setelah login berhasil, siapa yang memanggil navigate ke Beranda?
```

Benar kalau menjawab **bukan LoginScreen** — melainkan state sesi yang diamati di level
aplikasi. Itu `compose-architecture`. Kalau agent menjawab "LoginScreen memanggil
navController", skill kedua belum aktif.

### ✅ Checkpoint Fase 1

- [x] `find .claude/skills -type f` mengeluarkan tepat 6 file
- [x] `CLAUDE.md` di root, bagian Git sudah diisi atau dihapus
- [x] `.gitignore` memuat `settings.local.json`, **tidak** memuat `.claude/`
- [x] `/skills` menampilkan **kedua** skill
- [x] Uji trigger token dijawab dengan nama token
- [x] Uji trigger navigasi dijawab "bukan LoginScreen"

---

# FASE 2 — Fondasi visual (20 menit)

### 2.1 Font Inter

Unduh dari [rsms.me/inter](https://rsms.me/inter/) atau Google Fonts.

```bash
mkdir -p app/src/main/res/font
```

Empat file, nama **huruf kecil + underscore**:

| Weight | Nama file |
|---|---|
| Light 300 | `inter_light.ttf` |
| Regular 400 | `inter_regular.ttf` |
| Medium 500 | `inter_medium.ttf` |
| Bold 700 | `inter_bold.ttf` |

Resource Android menolak huruf besar, spasi, dan tanda hubung. `Inter-Regular.ttf` menggagalkan
build dengan *invalid resource name*. Kalau yang terunduh variable font, pakai file statis
per weight.

Lalu **File → Sync Project with Gradle Files**.

### 2.2 File token

```
Buat file token di id.bca.bcamobile/ui/theme/ sesuai design-tokens.md §6:
Color.kt, Dimens.kt, Shape.kt, Type.kt.

Color.kt dan Type.kt saat ini masih template Android Studio (Purple80/Purple40,
Typography dengan FontFamily.Default). GANTI seluruh isinya — warna template
tidak perlu dipertahankan.

Buat Dimens.kt dan Shape.kt yang belum ada.

Jangan sentuh Theme.kt dan MainActivity.kt.
Di Type.kt gunakan parameter bernama pada TextStyle (fontFamily = Inter, ...)
— parameter posisional pertama adalah color, bukan fontFamily.
```

`Theme.kt` sengaja belum disentuh: masih merujuk `Typography` template, jadi mengubah keduanya
sekaligus membuat build merah di tengah jalan dan sulit ditelusuri.

### 2.3 Build ulang

```bash
./gradlew assembleDebug
```

### ✅ Checkpoint Fase 2

- [x] Empat file font di `res/font/`, nama huruf kecil semua
- [x] `Color.kt`, `Dimens.kt`, `Shape.kt`, `Type.kt` ada, package benar
- [x] Nilai hex di `Color.kt` **sama persis** dengan tabel `design-tokens.md` §1
- [x] `./gradlew assembleDebug` sukses

---

# FASE 3 — Tutup keputusan terbuka (30 menit)

Fase yang paling sering dilewat dan paling mahal akibatnya. Selama tiga hal ini terbuka,
setiap screen berhenti di tengah jalan.

### 3.1 Type scale — blocker utama

Desain tidak menetapkan ukuran font maupun line height. Usulan di `design-tokens.md` §5
berstatus `PROPOSED`.

```
Type scale di design-tokens.md §5 masih PROPOSED.
Bantu saya tetapkan ukuran font dan line height final per peran teks,
lalu update design-tokens.md §5 dan Type.kt dalam commit yang sama.
```

Satu commit, karena kalau dokumen dan kode terpisah ada jendela waktu di mana keduanya berbeda.

### 3.2 Pemetaan `colorScheme`

Tinjau tabel di `design-tokens.md` §7, setujui atau ubah.

### 3.3 `Theme.kt`

```
Pemetaan colorScheme di design-tokens.md §7 sudah saya setujui.
Ubah Theme.kt yang sudah ada:
- LightColorScheme diisi dari pemetaan itu
- dynamicColor DIMATIKAN (default false)
- skema gelap: kunci ke LightColorScheme, jangan pakai warna template ungu
- pertahankan nama composable BcaMobileTheme
- typography diarahkan ke AppTypography
Jangan ubah MainActivity.kt.
```

**Dynamic color harus mati.** Template menyalakannya untuk Android 12+; dynamic color mengambil
warna dari wallpaper dan menimpa seluruh `colorScheme`. Semua token warna jadi tidak terlihat
di Android 12 ke atas — build hijau, preview benar, tapi di HP warnanya ikut wallpaper.

### ✅ Checkpoint Fase 3

- [x] `design-tokens.md` §5 tidak lagi bertanda PROPOSED
- [x] Pemetaan `colorScheme` disetujui
- [x] `Theme.kt` diubah, build sukses, `MainActivity` tidak ikut berubah
- [x] Ganti wallpaper → warna aplikasi tidak berubah (uji di Android 12+)
- [x] Mode gelap tidak menampilkan warna ungu template

---

# FASE 4 — Rangka navigasi (45 menit)

Dikerjakan **sebelum** screen pertama. Memasang screen ke dalam graph yang salah berarti
membongkarnya lagi nanti — dan tiap screen tambahan memperbesar biaya bongkarnya.

Stitch tidak membantu di fase ini. Ia menghasilkan screen satu per satu tanpa model
perpindahan antar screen; di project ini bahkan meng-inline filter Mutasi sehingga menghapus
satu screen dari alur. Navigasi datang dari `compose-architecture`, bukan dari artefak Stitch.

### 4.1 Audit navigasi yang ada

```
Baca arsitektur navigasi yang berlaku, lalu audit kode navigasi di project ini
terhadap arsitektur itu.

Laporkan sebagai daftar penyimpangan. Untuk tiap penyimpangan sebutkan:
- file dan baris
- aturan mana yang dilanggar
- risikonya kalau dibiarkan
- perbaikan yang diusulkan

Periksa khusus: Scaffold yang membungkus NavHost, SPLASH sebagai route,
FACE_ID dan FINGER_PRINT sebagai route, KODE_AKSES sebagai composable
bukan dialog, graph datar tanpa nesting, dan popUpTo yang hilang.

Bandingkan juga dengan task flow di screen-inventory.md — kalau ada route
yang tidak punya padanan di flow, atau flow yang tidak punya route, laporkan.

Jangan ubah kode. Jangan ubah file skill. Laporan saja.
```

**Kenapa agent dilarang mengubah file skill di sini:** kalau boleh, ia akan menyesuaikan
dokumen arsitektur agar cocok dengan kode yang salah, bukan sebaliknya.

### 4.2 Tinjau laporan, ambil keputusan

Lima hal yang perlu kamu putuskan sebelum restrukturisasi — semuanya ada di
`navigation.md` §14:

1. `core-splashscreen` — tambah dependency, atau splash sebagai state composable?
2. Route type-safe — tambah `kotlinx-serialization`, atau tetap konstanta `String`?
3. Ambang auto-lock — 1, 3, atau 5 menit di background?
4. Setelah re-auth dari `Locked` — kembali ke layar terakhir, atau selalu Beranda?
5. Cakupan `FLAG_SECURE` — hanya auth dan transaksi, atau seluruh aplikasi?

Nomor 3 dan 4 keputusan produk. Nomor 1 dan 2 menambah dependency — per `CLAUDE.md` butuh
persetujuanmu.

### 4.3 Restrukturisasi graph

- Sesi 1 -> struktur graph dan route
```
Restrukturisasi navigasi sesuai navigation.md §3 dan §12.
Perbaiki penyimpangan audit #1, #3, #4, #5, #8.

Keputusan saya: splash pakai core-splashscreen, route type-safe
(kotlinx-serialization disetujui).

- Pecah ke 6 file: Graph.kt, Route.kt, BcaNavHost.kt,
  AuthGraph.kt, MainGraph.kt, EWalletGraph.kt
- Graph bersarang: Auth, Main, EWallet
- Hapus route SPLASH, FACE_ID, FINGER_PRINT
- KODE_AKSES jadi dialog()
- Tambah route yang hilang: Auth.BukaRekening, Auth.GantiKodeAkses,
  Main.RentangWaktu, Main.Riwayat, EWallet.Pilih/Nominal/Pin/Bukti
- Hapus tab TRANSFER; Riwayat dan Mutasi jadi dua route terpisah

Screen composable boleh placeholder. Jangan sentuh Scaffold dulu.
Build harus hijau.
```
- Sesi 2 -> state 1sesi dan back stack (ini perbaikan keamanannya).

```
Perbaiki audit #6 dan #7 sesuai navigation.md §2, §4, §8.

- SessionState sealed interface: Loading, LoggedOut, Locked, Authenticated
- SessionRepository dengan StateFlow
- BcaApp menerima SessionState, LaunchedEffect memindahkan graph
- Setiap perpindahan graph pakai popUpTo(navController.graph.id) { inclusive = true }
- Auto-lock 3 menit via ProcessLifecycleOwner
- Setelah re-auth dari Locked: pulihkan layar terakhir, KECUALI flow
  transaksi yang dibuang ke Beranda
- FLAG_SECURE di Activity, seluruh aplikasi

Perhatikan jebakan di §8: LaunchedEffect ikut jalan di komposisi pertama.
Bandingkan dengan graph aktif sebelum menavigasi.
```
- Sesi 3 -> Scaffold dan bottom bar
```
Perbaiki audit #2 dan #9 sesuai navigation.md §9.

- Scaffold tidak lagi membungkus seluruh NavHost
- Visibility bottom bar dari destination.hierarchy, bukan daftar TAB_ROUTES
- AppBottomNav: 4 tab (Beranda, Mutasi, Riwayat, Akun) + FAB scan di tengah
- Hapus konstanta TAB_ROUTES
```

### 4.4 Uji back stack

Jalankan minimal empat skenario keamanan dari `navigation.md` §13:

| # | Skenario | Hasil benar |
|---|---|---|
| 1 | Login berhasil, tekan Back di Beranda | Aplikasi keluar — bukan kembali ke Login |
| 2 | Logout, tekan Back | Tetap di Login — bukan kembali ke Beranda |
| 6 | Selesai transaksi, Back di Bukti | Ke Beranda — bukan ke layar PIN |
| 8 | Proses dibunuh saat login, buka lagi | Login — bukan Beranda |

Keempatnya sifat keamanan, bukan preferensi UX. Jadikan tes otomatis
(`androidx.navigation:navigation-testing`), jangan hanya dicek manual.

### ✅ Checkpoint Fase 4

- [ ] Laporan audit sudah ditinjau, lima keputusan §14 sudah diambil
- [ ] File graph terpisah sesuai §12, bukan satu `NavHost` raksasa
- [ ] Graph bersarang: `Auth`, `Main`, `EWallet`
- [ ] Tidak ada `SPLASH`, `FACE_ID`, `FINGER_PRINT` sebagai route
- [ ] `KODE_AKSES` sebagai `dialog()`
- [ ] Bottom bar hanya muncul di tab graph Main, dan punya FAB scan
- [ ] Empat skenario 4.4 lulus
- [ ] `navigation.md` §3 sinkron dengan kode

---

# FASE 5 — Pagar pengaman (15 menit)

### 5.1 Script pemeriksa

```bash
mkdir -p scripts
```

`scripts/check-hardcoded-ui.sh`:

```bash
#!/usr/bin/env bash
# Menolak literal warna/dimensi di luar file token.
set -uo pipefail

SRC="app/src/main/java"
TOKEN_FILES='(Color|Dimens|Shape|Type)\.kt'

hits=$(grep -rnE 'Color\(0x|[^a-zA-Z0-9_][0-9]+\.dp|[^a-zA-Z0-9_][0-9]+\.sp' \
         --include='*.kt' "$SRC" | grep -vE "$TOKEN_FILES" || true)

if [ -n "$hits" ]; then
  echo "FAIL: literal visual di luar file token"
  echo "$hits"
  exit 1
fi

echo "OK: tidak ada literal visual di luar file token"
```

```bash
chmod +x scripts/check-hardcoded-ui.sh
./scripts/check-hardcoded-ui.sh
```

Project ini template segar, jadi seharusnya langsung lolos — tidak ada kode lama yang
membanjiri hasilnya. Manfaatkan itu: jadikan blocking sejak awal.

### 5.2 Pre-commit hook

```bash
cat > .git/hooks/pre-commit <<'EOF'
#!/usr/bin/env bash
exec ./scripts/check-hardcoded-ui.sh
EOF
chmod +x .git/hooks/pre-commit
```

Script di `scripts/` supaya ikut ter-commit; `.git/hooks/` tidak ikut ter-commit.

### 5.3 Aset desain

```bash
mkdir -p docs/design
```

Salin screenshot desain ke sana. Jangan taruh di `res/drawable/` — semua isi `res/` ikut
dibungkus ke APK.

### ✅ Checkpoint Fase 5

- [ ] `./scripts/check-hardcoded-ui.sh` lolos
- [ ] Pre-commit hook terpasang
- [ ] Aset desain di `docs/design/`, bukan `res/`

---

# FASE 6 — Sambungkan Stitch (30 menit)

Inti alur kerja harian. HTML dari Stitch memberi hierarki layout dan angka presisi yang tidak
bisa didapat dari screenshot.

### 6.1 Install server

```bash
node --version                      # >= 18
git clone https://github.com/oogleyskr/stitch-mcp-server.git ~/tools/stitch-mcp-server
cd ~/tools/stitch-mcp-server
npm install
npm run build
ls dist/index.js
```

Letakkan **di luar** repo Android — `node_modules` mengotori working tree.

### 6.2 Kredensial

```bash
echo 'export STITCH_API_KEY="…"' >> ~/.zshrc
source ~/.zshrc
echo "${STITCH_API_KEY:0:6}…"
```

Token berlaku sekitar 90 hari. Kalau suatu saat MCP menolak semua permintaan padahal
konfigurasi tidak berubah, regenerasi token — jangan utak-atik konfigurasi.

### 6.3 `.mcp.json` di root repo

```json
{
  "mcpServers": {
    "stitch": {
      "command": "node",
      "args": ["/Users/<user>/tools/stitch-mcp-server/dist/index.js"],
      "env": {
        "STITCH_API_KEY": "${STITCH_API_KEY}"
      }
    }
  }
}
```

`args` harus **absolute path**. **Jangan pernah** menulis key literal — file ini ikut
ter-commit. Sebelum commit pertama:

```bash
grep -i 'key\|token\|secret' .mcp.json
```

Yang boleh muncul hanya `"STITCH_API_KEY": "${STITCH_API_KEY}"`.

### 6.4 Verifikasi

Restart Claude Code (konfigurasi MCP dibaca saat startup), lalu `/mcp` — `stitch` harus
**connected**. Uji satu panggilan baca:

```
Daftar project Stitch yang bisa kamu akses apa saja?
```

### ✅ Checkpoint Fase 6

- [ ] `/mcp` menampilkan `stitch` **connected**
- [ ] Panggilan baca mengembalikan daftar project sungguhan
- [ ] `.mcp.json` tidak memuat key literal

---

# FASE 7 — Screen pertama

```
Implement screen Mutasi dari desain.
```

Sesingkat itu. Skill menyala sendiri.

**Yang akan terjadi, dan itu memang benar:** agent memeriksa konformitas artefak Stitch
terhadap desain lebih dulu (`stitch-to-compose` §3 fase 2). Kalau ada elemen tambahan atau
hilang, ia berhenti dan melapor. Jangan anggap itu hambatan — itu yang mencegah improvisasi
generator masuk permanen ke design system.

**Satu screen per sesi.** Navigation dan wiring ViewModel pekerjaan terpisah.

Urutan screen yang disarankan, dari yang paling sedikit state:

1. Splash
2. Face ID / Touch ID *(keadaan pada Login, bukan screen tersendiri)*
3. Welcome / Login
4. Bukti Transaksi
5. Riwayat
6. Mutasi *(mulai di sini butuh empty state)*
7. Beranda *(paling banyak komponen)*

Sebelum melaporkan selesai, minta agent menjalankan Definition of Done —
`stitch-to-compose` §8 untuk visual, `compose-architecture` untuk navigasi/state.

### Kalau skill tidak aktif

Ciri: agent menulis `Color(0xFF…)`, mengarang spacing, memanggil `navController` dari dalam
screen, atau menggarap satu flow sekaligus. Hentikan, lalu paksa sekali:

```
Gunakan skill stitch-to-compose.
```

atau

```
Gunakan skill compose-architecture.
```

Kalau perilakunya berubah, masalahnya di `description`. Catat kalimat yang gagal memicu,
tambahkan kata-katanya ke `description` skill terkait.

---

# Troubleshooting

| Gejala | Sebab & tindakan |
|---|---|
| `.claude/` tidak terlihat di Android Studio | Panel Project mode **Android**. Ganti ke **Project** (0.2). |
| Hanya satu skill muncul di `/skills` | Unzip menghasilkan nesting ganda. Cek `find .claude/skills -type f` — harus 6 file. |
| Skill terbaca tapi tidak pernah aktif | `description` terlalu generik. Tambahkan istilah konkret yang biasa kamu ketik. |
| Agent menulis `Color(0xFF…)` | File theme tidak ada di konteks. Agent butuh daftar nama token yang valid, bukan sekadar larangan. |
| Agent memanggil `navController` dari screen | `compose-architecture` tidak aktif. Panggil eksplisit, lalu perbaiki `description`. |
| Build gagal: *invalid resource name* | Nama file font pakai huruf besar atau tanda hubung. |
| `Unresolved reference: R.font.inter_regular` | Font belum di `res/font/`, atau perlu Sync Gradle. |
| Warna benar di preview, salah di HP | `dynamicColor` masih menyala. Lihat 3.3. |
| Back dari Beranda kembali ke Login | `popUpTo(inclusive = true)` hilang saat pindah graph. `navigation.md` §8. |
| `/mcp` tidak menampilkan `stitch` | Belum restart, atau CLI dijalankan dari luar root repo. |
| `stitch` berstatus `failed` | Cek berurutan: `node dist/index.js` jalan manual, path `args` absolute, `echo $STITCH_API_KEY` terisi di shell yang sama. |
| MCP connected tapi panggilan ditolak | Token kedaluwarsa atau kuota bulanan habis. |

---

# Struktur akhir

```
bca_mobile/
├── .mcp.json
├── .claude/
│   ├── skills/
│   │   ├── stitch-to-compose/
│   │   │   ├── SKILL.md
│   │   │   └── references/
│   │   │       ├── design-tokens.md
│   │   │       ├── mapping-html-to-compose.md
│   │   │       └── screen-inventory.md
│   │   └── compose-architecture/
│   │       ├── SKILL.md
│   │       └── references/navigation.md
│   └── settings.local.json          ← JANGAN di-commit
├── CLAUDE.md
├── docs/
│   ├── START-HERE.md
│   └── design/
├── scripts/check-hardcoded-ui.sh
├── app/src/main/
│   ├── java/id/bca/bcamobile/
│   │   ├── ui/theme/{Color,Dimens,Shape,Type,Theme}.kt
│   │   └── ui/navigation/{Graph,Route,BcaNavHost,AuthGraph,MainGraph,EWalletGraph}.kt
│   └── res/font/inter_{light,regular,medium,bold}.ttf
├── settings.gradle.kts
└── .gitignore
```

---

# Merawat skill

**Progressive disclosure.** `SKILL.md` selalu masuk konteks saat skill aktif; `references/`
hanya dibaca saat dibutuhkan. Jaga tiap `SKILL.md` di bawah ±500 baris.

**`description` adalah trigger.** Satu-satunya dasar agent memutuskan skill relevan. Tulis
dengan kata-kata yang benar-benar kamu pakai, dan sebutkan kapan skill itu **tidak** dipakai.

**Satu skill, satu tanggung jawab.** Itu alasan navigasi terpisah dari konversi desain. Butuh
aturan networking atau testing nanti? Skill ketiga, bukan menumpuk ke yang ada.

**Perlakukan seperti kode.** Token berubah → `design-tokens.md` berubah di commit yang sama.
Route berubah → `navigation.md` berubah di commit yang sama. Skill yang tidak sinkron lebih
buruk daripada tidak ada skill.

**Jangan simpan rahasia di dalam skill.** Semua isinya masuk konteks model dan ikut ter-commit.
