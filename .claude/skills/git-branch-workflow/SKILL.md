---
name: git-branch-workflow
description: Alur branch dan commit wajib untuk bca_mobile — menentukan branch mana yang dibuat sebelum satu baris kode pun ditulis, dari mana ia bercabang, dan lewat mana ia sampai ke `main`. Gunakan SEBELUM mulai mengerjakan feature baru, perbaikan bug, atau fase lanjutan dari pekerjaan besar, dan saat diminta commit, merge, atau menyiapkan rilis. Trigger juga pada "buka PR", "pull request", "PR ke main", "tambah feature", "kerjakan feature", "fitur baru", "fix bug ini", "bikin branch", "branch apa", "commit ini", "merge ke main", "siap rilis", "mulai fase berikutnya", "pengerjaan panjang", "working branch", "feature branch", "branch development", "staging", "langsung di main saja", "hotfix", "urgent", "mendesak", "production bermasalah", "perbaiki yang sudah rilis", dan setiap permintaan yang akan menghasilkan commit. `main` tidak boleh disentuh langsung untuk alasan apa pun — termasuk perbaikan satu baris dan keadaan mendesak — jadi baca skill ini lebih dulu bahkan saat permintaannya terdengar sepele atau terburu-buru. Berlaku juga saat pengguna tidak menyebut git sama sekali — kalau hasil pekerjaannya berupa commit, alur ini yang menentukan tempatnya. JANGAN dipakai untuk memilih arsitektur kode (itu `compose-architecture` atau `android-architecture-patterns`), isi pesan rilis ke nasabah, atau konfigurasi CI.
---

# Alur Branch & Commit — bca_mobile

Skill ini menjawab satu pertanyaan yang harus dijawab **sebelum** mengedit berkas:
pekerjaan ini tinggal di branch mana, dan lewat jalan mana ia sampai ke `main`?

Menjawabnya di akhir pekerjaan sudah terlambat — memindahkan commit yang sudah jadi
ke branch yang benar jauh lebih mahal daripada membuat branch lebih dulu.

## Aturan yang tidak berubah

**`main` tidak pernah disentuh langsung — tanpa pengecualian.** Jangan mengubah,
membuat, atau menghapus berkas apa pun sementara branch aktif adalah `main`. Ini
berlaku untuk **semua** berkas, bukan hanya kode: CLAUDE.md, isi `.claude/skills/`,
`docs/`, README, berkas Gradle. Tidak ada perbaikan yang cukup kecil untuk
dikecualikan, dan tidak ada keadaan yang cukup mendesak — yang mendesak dapat
branch-nya sendiri, lihat §Hotfix.

Jadi sebelum mengedit berkas pertama, periksa:

```bash
git branch --show-current      # kalau menjawab "main", berhenti dan buat branch
```

Satu-satunya hal yang boleh terjadi pada `main` adalah menerima merge dari
`feature/` atau `hotfix/`. `main` hanya bertambah lewat merge, tidak pernah lewat
editor.

Yang tetap berlaku dari CLAUDE.md §Aturan #6: **`git commit`, `git push`, dan membuat
branch hanya saat diminta eksplisit.** Skill ini mengatur *bentuk* branch-nya, bukan
memberi izin membuatnya sendiri. Jadi urutannya: pengguna meminta pekerjaan → Anda
sebutkan branch apa yang perlu dibuat → pengguna setuju → baru dibuat.

Kalau pengguna meminta pekerjaan yang jelas akan mengubah berkas sementara Anda di
`main`, sebutkan branch yang dibutuhkan **sebelum** mulai, bukan setelah berkasnya
berubah. Menanyakannya di tengah pekerjaan memaksa pengguna memilih antara
menyetujui branch atau membuang pekerjaan yang sudah jalan.

## Pilih bentuknya dulu

Satu pertanyaan yang memutuskan: **pekerjaan ini selesai dalam sekali duduk, atau
terbagi fase yang jalan berminggu-minggu?**

| Perkiraan | Bentuk |
|---|---|
| Satu-dua fase, selesai dalam hitungan hari sampai ~2 minggu | **Jalur pendek** — satu `working/` langsung dari `main` |
| Banyak fase, perkiraan lebih dari 2 minggu sampai sebulan | **Jalur panjang** — `feature/` dari `main`, lalu `working/` per fase dari `feature/` |

Kalau ragu, tanyakan ke pengguna berapa lama dan berapa fase. Menebak ke arah yang
salah mahal di kedua sisi: jalur panjang untuk pekerjaan sehari menambah dua merge
tanpa guna, sementara jalur pendek untuk pekerjaan sebulan menyimpan satu branch
raksasa yang tidak pernah bisa dites sebagian.

### Jalur pendek

```text
main
 └── working/ambil-ulang-foto-ktp      ← seluruh pekerjaan di sini
```

### Jalur panjang

Satu `feature/` menampung seluruh pekerjaan besar; tiap fase dapat `working/`-nya
sendiri yang bercabang dari sana, lalu kembali ke sana setelah selesai.

```text
main
 └── feature/buka-rekening
      ├── working/fase-1-ocr-ktp       ← selesai → merge ke feature/buka-rekening
      ├── working/fase-2-biometrik
      └── working/fase-3-video-call
```

Alasan fase tidak bercabang langsung dari `main`: fase 2 hampir selalu butuh hasil
fase 1. Dari `main`, fase 2 mulai tanpa pekerjaan fase 1 dan konfliknya baru
ketahuan di akhir. Dari `feature/`, fase 2 mulai di atas fase 1 — seperti keadaan
sebenarnya nanti di produksi.

## Jalan menuju main

`development` adalah branch **berumur panjang** — staging tempat pengujian. Dibuat
sekali dari `main`; kalau belum ada, buat dan katakan Anda membuatnya. Jangan
membuat `development` baru per feature.

```text
1. working/<fase>      --merge-->  feature/<nama>     (jalur panjang; jalur pendek lewati)
2. feature/<nama>      --merge-->  development        ← diuji di sini
3. feature/<nama>      --merge-->  main               ← rilis, setelah pengujian lulus
```

Perhatikan langkah 3: yang masuk `main` adalah **`feature/`**, bukan `development`.

Ini bagian yang paling sering salah, jadi alasannya perlu dipegang: `development`
bercampur. Ia memuat beberapa feature sekaligus, termasuk yang pengujiannya gagal
dan yang belum diputuskan ikut rilis. Me-merge `development` ke `main` berarti
menyeret semuanya — termasuk yang belum siap. `feature/` memuat tepat satu pekerjaan
yang batasnya jelas, jadi itulah yang pantas jadi isi rilis.

`development` lulus uji membuktikan *feature itu* sehat bersama feature lain. Ia
tidak membuat `development` jadi kandidat rilis.

## Pull request

Setiap merge di atas terjadi lewat pull request, dan **arah PR-nya tidak bebas**.

| Dari | PR-nya ke | Catatan |
|---|---|---|
| `working/`, `fix/` | `feature/<nama>` (jalur panjang) atau `development` (jalur pendek) | tidak pernah ke `main` |
| `feature/<nama>` | `development` lebih dulu, lalu `main` | PR ke `main` hanya setelah commit yang sama ada di `development` |
| `hotfix/<nama>` | `main`, lalu merge balik ke `development` dan tiap `feature/` yang hidup | satu-satunya jalan pintas |

**Jangan pernah membuka PR ke `main` dari branch yang belum lewat `development`.**
Bukan karena formalitas: `main` adalah yang ada di tangan nasabah, dan satu-satunya
tempat perubahan diuji bersama feature lain adalah `development`. PR `working/` →
`main` melewati pengujian itu sepenuhnya, dan yang lolos dari sana tidak pernah
ketahuan sebelum nasabah yang menemukannya.

Yang diuji di `development` adalah **commit**, bukan niat. Kalau `feature/` mendapat
commit baru setelah lulus uji, ia harus kembali ke `development` dulu sebelum PR ke
`main` dibuka lagi — kalau tidak, yang dirilis bukan yang diuji.

`development` juga tidak pernah menerima commit langsung. Sama seperti `main`, ia
hanya bertambah lewat merge; bedanya ia menerima dari lebih banyak arah.

Gerbangnya tidak bergantung ingatan siapa pun:

```bash
./scripts/check-pr-branch-policy.sh <branch-asal> <branch-tujuan>
```

Skrip itu dijalankan job **Arah branch** di `.github/workflows/ai-pr-review.yml` pada
setiap PR, dan ia menolak PR ke `main` yang commit-nya belum ada di `development`.
Auto-merge AI pun tidak pernah menyentuh `main`: PR ke `main` yang lulus seluruh
review hanya diberi label `ai-review/siap-rilis`, dan tombol merge-nya tetap milik
manusia.

## Penamaan

Prefix dengan garis miring, sisanya kebab-case, bahasa apa pun yang paling jelas.

| Prefix | Isi | Contoh |
|---|---|---|
| `feature/` | Payung pekerjaan besar, satu per feature | `feature/buka-rekening` |
| `working/` | Satu fase atau satu pekerjaan pendek | `working/fase-1-ocr-ktp` |
| `fix/` | Perbaikan bug yang tidak mendesak | `fix/ambil-ulang-foto-ktp` |
| `hotfix/` | Perbaikan mendesak untuk yang sudah rilis | `hotfix/pin-gagal-setelah-rotasi` |
| `development` | Staging, tanpa akhiran | `development` |

Nama menyebut **apa yang dikerjakan**, bukan nomor tiket sendirian: `working/BCA-412`
tidak memberi tahu siapa pun isinya saat daftar branch dibaca enam bulan kemudian.
Nomor tiket boleh ikut setelah namanya — `working/fase-2-biometrik-BCA-412`.

## Hotfix

Keadaan mendesak **bukan** izin menyentuh `main` langsung. Ia hanya memperpendek
jalannya, dan jalannya tetap lewat branch.

```text
main
 └── hotfix/<nama>   --merge-->  main          (rilis perbaikan)
                     --merge-->  development   (dan setiap feature/ yang masih hidup)
```

Bercabang dari **`main`**, bukan dari `development` atau `feature/`: yang diperbaiki
adalah keadaan yang sudah ada di tangan nasabah, dan itulah yang ada di `main`.
Bercabang dari `development` akan menyeret pekerjaan yang belum dirilis ke dalam
perbaikan mendesak — tepat yang paling tidak diinginkan saat sedang terdesak.

Boleh melewati singgah di `development` sebelum masuk `main`; itu makna "mendesak" di
sini. Tapi **gerbang tiga perintah di bawah tidak boleh dilewati** — justru saat
terdesak, perbaikan yang merusak hal lain paling mahal harganya.

Setelah masuk `main`, **merge balik** `hotfix/` ke `development` dan ke setiap
`feature/` yang masih berjalan. Tanpa ini perbaikannya hilang dari pekerjaan yang
sedang jalan, lalu kambuh begitu feature berikutnya dirilis — dan kambuhnya sulit
dipercaya, karena semua orang ingat bug itu sudah pernah ditutup.

## Sebelum merge ke mana pun

Jangan mengusulkan merge sebelum keempatnya hijau — angkanya disebut apa adanya, dan
kegagalan dilaporkan, bukan dibulatkan jadi "sudah lewat":

```bash
./scripts/pr-gates.sh                # keempat gerbang sekaligus, isinya sama dengan CI
```

Isinya — jalankan sendiri-sendiri kalau butuh satu saja:

```bash
./gradlew testLocalDebugUnitTest     # jumlah test & kegagalan, sebutkan angkanya
./gradlew lintLocalDebug             # 0 error
./scripts/check-hardcoded-ui.sh      # tidak ada pelanggaran visual baru
./scripts/check-anr-patterns.sh      # tidak ada pola ANR/bug laten baru
```

Satu test yang gagal adalah alasan cukup untuk tidak merge. Laporkan keluarannya ke
pengguna dan biarkan mereka yang memutuskan, alih-alih memilih sendiri untuk lanjut.

## Commit

Ruang lingkup satu commit = satu perubahan yang bisa dijelaskan dalam satu kalimat.
Kalau butuh kata "dan" dua kali, itu dua commit.

**Yang tidak ikut commit:** state mesin lokal. Di repo ini yang sering muncul adalah
`.idea/deploymentTargetSelector.xml` (pilihan emulator) dan `.idea/misc.xml` (nama
JDK lokal). Keduanya ter-track, jadi gampang terbawa `git add -A` — dan rekan yang
menariknya mendapat konfigurasi mesin orang lain. Stage berkas yang Anda sentuh,
jangan `-A`, lalu sebutkan apa yang sengaja ditinggalkan.

Pesan commit menjelaskan **kenapa**, karena *apa*-nya sudah ada di diff. Untuk
perbaikan bug, sebut gejala yang dialami nasabah lebih dulu — itu yang dicari orang
saat menelusuri kambuhnya setahun kemudian. Akhiri dengan baris atribusi yang
diminta harness.

```text
fix ambil ulang foto ktp tidak lagi memusnahkan sesi buka rekening

Menekan "Ambil Ulang" memulangkan nasabah ke Pilih Jenis Rekening dengan state
terbuang, lalu memaksa sesi baru. POST /onboarding/sessions hanya 3/jam per
device, jadi tiga kali ambil ulang memblokir perangkat sejam.

Penyebabnya: backend memakai satu kode ONBOARDING_NOT_FOUND untuk "sesi tidak
dikenal" dan "belum ada hasil OCR"; client memetakan keduanya ke fatal.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
```

## Yang wajib ikut di commit yang sama

CLAUDE.md §Memperbarui file ini mengikat di sini: menambah route, paket, endpoint,
dependency, atau skill berarti memperbarui CLAUDE.md **di commit yang sama** —
ditambah `references/navigation.md` kalau yang berubah adalah navigasi.

CLAUDE.md dibatasi 200 baris dan saat ini sudah penuh. Jadi menambah sesuatu ke sana
berarti memangkas yang lain, dan pemangkasan itu keputusan pengguna. Pola yang
berhasil: rapatkan kalimat yang bertele-tele atau lipat subheading yang isinya cuma
beberapa baris — jangan buang fakta untuk memberi ruang bagi fakta baru.

## Kalau pekerjaan sudah dimulai di main

Terjadi. Yang menentukan jalan keluarnya: perubahannya **sudah di-commit atau belum?**
Periksa dengan `git status` dan `git log origin/main..HEAD` sebelum memilih.

**Belum di-commit** — kasus paling umum, dan tidak ada yang perlu dihapus. Perubahan
yang belum di-commit ikut berpindah saat branch dibuat, dan `main` tidak tersentuh:

```bash
git switch -c working/<nama>       # perubahan terbawa; main tetap bersih
```

**Sudah di-commit tapi belum di-push** — commit-nya dipindahkan, lalu `main`
dikembalikan ke posisi remote:

```bash
git branch working/<nama>          # tandai posisi sekarang, commit ikut tersimpan
git reset --hard origin/main       # MENGHAPUS perubahan yang belum di-commit
git switch working/<nama>
```

`reset --hard` membuang perubahan yang belum di-commit tanpa bisa dibatalkan. Jalankan
`git status` lebih dulu, tunjukkan ke pengguna apa yang akan hilang, dan minta
persetujuan — jangan jalankan atas inisiatif sendiri. Kalau ada perubahan belum
di-commit yang bercampur, selamatkan dulu dengan `git stash` sebelum reset.

**Sudah di-push ke `main`** — berhenti dan serahkan keputusannya ke pengguna. Jangan
pernah menawarkan `push --force` ke `main`: orang lain mungkin sudah menariknya, dan
menulis ulang riwayat bersama merusak salinan mereka. Perbaikannya maju ke depan
lewat `hotfix/`, bukan dengan menghapus jejak.
