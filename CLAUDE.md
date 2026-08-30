# CLAUDE.md

Konteks project untuk AI coding agent. File ini **selalu** masuk konteks di setiap sesi,
jadi isinya dibatasi pada hal yang berlaku universal. Detail per-domain ada di `.claude/skills/`.

Jaga file ini tetap pendek. Panduan yang terlalu panjang cenderung terabaikan.

## Project

`bca_mobile` — aplikasi Android, Kotlin, 100% Jetpack Compose + Material3. Tanpa XML layout.

- Package: `id.bca.bcamobile` <!-- verifikasi terhadap namespace di app/build.gradle.kts -->
- Module: single-module (`:app`)
- minSdk 24, targetSdk 36, Compose BOM 2026.02.01, Kotlin 2.2.10
- Theme composable: `BcaMobileTheme` — pertahankan namanya, jangan bikin theme kedua
- `dynamicColor` harus **mati**; menyalakannya membuat seluruh token warna tidak terpakai
- Skema gelap belum didefinisikan desain — jangan dibuat dengan nilai tebakan

## Aturan yang selalu berlaku

1. **Tidak ada nilai visual hardcoded.** Warna, spacing, radius, stroke, dan ukuran font
   selalu lewat token di `ui/theme/`. Dilarang `Color(0xFF…)`, `.dp`/`.sp` telanjang, dan
   `RoundedCornerShape(n.dp)` inline di luar `Color.kt`, `Dimens.kt`, `Shape.kt`, `Type.kt`.
2. **Token tidak ada → berhenti dan lapor.** Jangan membuat token baru sendiri, jangan
   memakai hex langsung, jangan membulatkan ke token terdekat diam-diam.
3. **String selalu ke `strings.xml`, additive only.** Tidak menghapus atau mengubah entri
   yang sudah ada tanpa diminta.
4. **Perubahan pada file XML layout dan resource yang sudah ada bersifat penambahan saja.**
5. **Composable UI stateless.** Tanpa ViewModel, repository, atau pemanggilan network
   di dalamnya.
6. **Jangan menjalankan `git commit`, `git push`, atau membuat branch** kecuali diminta
   eksplisit.

## Dua skill, dua wilayah

| Wilayah | Skill |
|---|---|
| Token, layout, penampilan visual dari desain | `stitch-to-compose` |
| Navigasi, state, batas ViewModel, keamanan sesi | `compose-architecture` |

Keduanya di `.claude/skills/`. Aturannya jangan disalin ke sini — cukup rujukan ini supaya
tidak ada dua sumber kebenaran yang bisa berbeda.

Struktur navigasi di `compose-architecture/references/navigation.md` bersifat **mengikat**.
Menambah route atau graph berarti memperbarui file itu di commit yang sama.

## Perintah

```bash
./gradlew assembleDebug              # build
./gradlew testDebugUnitTest          # unit test
./gradlew lintDebug                  # android lint
./scripts/check-hardcoded-ui.sh      # cek literal visual di luar file token
```

Jalankan `check-hardcoded-ui.sh` sebelum melaporkan pekerjaan UI selesai.

## Git

<!-- Isi sesuai konvensi tim; hapus bagian ini kalau project personal. -->
- Alur branch:
- Format pesan commit:
- Bahasa PR:

## Batasan

- Jangan menambah dependency baru tanpa persetujuan.
- Jangan mengubah `build.gradle.kts`, `settings.gradle.kts`, atau konfigurasi Gradle
  tanpa diminta.
- Jangan menyimpan API key, token, atau kredensial di file yang ter-commit.
- Jangan menaruh aset referensi desain di `res/` — tempatnya `docs/design/`.
