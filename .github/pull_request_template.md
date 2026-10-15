## Apa yang berubah

<!-- Satu paragraf. Reviewer AI membaca bagian ini untuk tahu maksud Anda,
     lalu memeriksa apakah kodenya benar-benar melakukan itu. -->

## Kenapa

<!-- Gejala yang dialami nasabah kalau ini perbaikan bug; kebutuhan yang
     mendorongnya kalau ini feature. -->

## Arah branch

<!-- Centang satu. PR ke `main` hanya dari feature/ yang commit-nya SUDAH
     diuji di development, atau dari hotfix/. -->

- [ ] `working/` atau `fix/` → `feature/<nama>`
- [ ] `working/`, `fix/`, atau `feature/` → `development`
- [ ] `feature/` → `main` (sudah lulus uji di `development`)
- [ ] `hotfix/` → `main` (mendesak; akan di-merge balik ke `development`)

## Yang perlu diperiksa manusia

<!-- Sebut eksplisit kalau PR ini menyentuh: uang/nominal, PII (foto KTP, NIK,
     PIN, kode akses), kriptografi, token/sesi, thread utama, atau siklus hidup
     kamera/WebRTC. Auto-merge tetap jalan kalau review bersih, jadi bagian ini
     yang memberi tahu kapan Anda ingin menahannya. Tambahkan label
     `do-not-merge` kalau PR ini tidak boleh di-merge otomatis. -->

## Yang wajib ikut di PR yang sama

- [ ] Route/graph baru → `references/navigation.md` + CLAUDE.md diperbarui
- [ ] Endpoint baru → §Integrasi API + tabel status di CLAUDE.md diperbarui
- [ ] Paket, dependency, permission, atau `buildConfigField` baru → disebut di CLAUDE.md
- [ ] Teks UI baru ada di `strings.xml` (additive, tidak menghapus entri lama)
- [ ] Nilai visual baru lewat token di `ui/theme/`, bukan literal

## Gerbang lokal

```bash
./scripts/pr-gates.sh
```

- [ ] Sudah dijalankan dan lulus (atau kegagalannya dijelaskan di atas)
