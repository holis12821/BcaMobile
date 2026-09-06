# Design Brief — Halaman Rekening & Kartu

Brief untuk men-generate desain layar **Rekening & Kartu** di Stitch, menggantikan tab Mutasi
di bottom navigation.

Simpan di `docs/design/brief-rekening-kartu.md`.

---

## 1. Kenapa brief ini ketat

Audit tiga screen sebelumnya menemukan Stitch menambahkan 11 elemen yang tidak ada di desain
dan menghilangkan 5 yang ada. Pola tambahannya konsisten: efek kaca, gradient dekoratif, foto
profil di header, sapaan nama pengguna, dan tombol "Lihat Semua" yang tidak diminta.

Karena itu brief ini memuat **daftar larangan eksplisit**, bukan hanya daftar permintaan.
Generator mengisi kekosongan dengan improvisasi — jadi jangan sisakan kekosongan.

---

## 2. Konteks aplikasi

- Aplikasi mobile banking, bahasa Indonesia
- Font: **Inter** (Light 300, Regular 400, Medium 500, Bold 700)
- Layar mobile, potret, lebar acuan 360dp
- Tab ini salah satu dari empat: **Beranda · Rekening · Riwayat · Akun**, plus FAB scan di tengah
- Gaya visual: bersih, datar, banyak ruang putih. **Tanpa** efek kaca, tanpa gradient dekoratif,
  tanpa bayangan berat

### Token warna

| Peran | Hex |
|---|---|
| Primary (header, tombol utama, aksen) | `#0060AF` |
| Primary gelap (teks di atas latar terang) | `#003764` |
| Latar halaman | `#FFFFFF` |
| Latar sekunder / pembatas blok | `#E8E8E8` |
| Garis pemisah | `#D2D2D2` |
| Teks utama | `#333333` |
| Teks sekunder | `#777777` |
| Sukses / aktif | `#15BA39` |
| Danger / diblokir | `#D00416` |

### Token spacing (dp)

`2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 56` — **hanya angka ini**, tidak ada di antaranya.

### Token radius (dp)

`0, 2, 4, 6, 8, 10, 12, 16` — plus lingkaran penuh untuk avatar dan chip.

### Token stroke (dp)

`1, 2, 4, 6`

---

## 3. Tujuan halaman

Satu tempat untuk melihat **apa yang pengguna miliki**: rekening, kartu debit, dan Flazz.
Bukan tempat melakukan transaksi — transaksi ada di Beranda dan FAB.

---

## 4. Elemen wajib

Urut dari atas.

### 4.1 Top bar

- Latar `#0060AF`, tinggi standar
- Judul "Rekening & Kartu", putih, tengah, Medium
- Tanpa tombol back (ini tab, bukan layar turunan)
- Tanpa ikon lain

### 4.2 Bagian "Rekening Saya"

Label bagian, lalu daftar kartu rekening. Tampilkan **dua** rekening sebagai contoh.

Tiap kartu rekening memuat:

- Jenis rekening — contoh "Tahapan Xpres", Medium
- Nomor rekening tersamar — contoh `1234 5678`
- Saldo — contoh `Rp12.345.678`, Bold, ukuran paling besar di kartu
- Ikon mata untuk menyembunyikan/menampilkan saldo
- Dua aksi teks di bagian bawah kartu: **Mutasi** dan **Detail**

Kartu: latar putih, border `1dp #D2D2D2`, radius `12`, padding `16`.

### 4.3 Bagian "Kartu Debit"

Satu kartu contoh:

- Nama kartu — "BCA Mastercard Debit"
- Nomor tersamar — `•••• 5678`
- Chip status: "Aktif" (`#15BA39`) — sediakan juga varian "Diblokir" (`#D00416`)
- Aksi: **Blokir Kartu** dan **Atur Limit**

### 4.4 Bagian "Flazz"

- Label "Flazz"
- Nomor kartu Flazz tersamar
- Saldo terakhir + keterangan waktu pembaruan terakhir
- Aksi: **Cek Saldo** dan **Top Up**

### 4.5 Bottom navigation

Empat item: Beranda · **Rekening** (aktif) · Riwayat · Akun, dengan **FAB scan lingkaran di
tengah** memotong bar. Item aktif memakai `#0060AF`, item non-aktif `#777777`.

---

## 5. Yang DILARANG ditambahkan

Jangan buat satu pun dari ini. Kalau muncul, desainnya ditolak.

- Foto profil atau avatar pengguna di header
- Sapaan "Selamat datang" atau nama pengguna
- Efek kaca, `backdrop-blur`, atau overlay transparan
- Gradient dekoratif di kartu mana pun
- Foto latar
- Tombol "Lihat Semua" atau "Selengkapnya"
- Grafik, chart, atau ringkasan pengeluaran
- Banner promo atau penawaran
- Ikon dekoratif yang tidak punya fungsi
- Warna di luar daftar token di §2
- Spacing di luar skala token di §2

---

## 6. State yang harus ikut didesain

Buat sebagai varian layar terpisah, jangan digabung:

| State | Isi |
|---|---|
| **Loading** | Placeholder abu (`#E8E8E8`) berbentuk kartu, tanpa spinner di tengah layar |
| **Empty — belum ada kartu debit** | Teks singkat + aksi "Ajukan Kartu" |
| **Error — saldo gagal dimuat** | Kartu tetap tampil, saldo diganti teks "Gagal memuat" + tautan "Coba Lagi" |
| **Saldo tersembunyi** | Saldo diganti titik-titik, ikon mata berubah |

State error **tidak boleh** mengosongkan seluruh layar — rekening dan nomor kartu tetap
terlihat, hanya saldonya yang gagal.

---

## 7. Prompt siap tempel untuk Stitch

```
Design a mobile banking screen in Indonesian titled "Rekening & Kartu".

Style: clean, flat, plenty of white space. NO glass effects, NO backdrop blur,
NO decorative gradients, NO background photos, NO shadows beyond a subtle card border.
Font: Inter.

Colors, use ONLY these:
primary #0060AF, dark primary #003764, page background #FFFFFF,
secondary surface #E8E8E8, divider #D2D2D2, primary text #333333,
secondary text #777777, success #15BA39, danger #D00416.

Spacing, use ONLY: 2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 56 px.
Corner radius, use ONLY: 0, 2, 4, 6, 8, 10, 12, 16 px, or full circle.

Layout top to bottom:
1. Top app bar, background #0060AF, white centered title "Rekening & Kartu".
   No back button, no other icons.
2. Section label "Rekening Saya". Two account cards. Each card: white background,
   1px #D2D2D2 border, 12px radius, 16px padding. Inside: account type
   ("Tahapan Xpres"), masked account number ("1234 5678"), balance
   ("Rp12.345.678") as the largest bold text, an eye icon to hide the balance,
   and two text actions at the bottom: "Mutasi" and "Detail".
3. Section label "Kartu Debit". One card: card name "BCA Mastercard Debit",
   masked number "•••• 5678", a status chip "Aktif" in #15BA39, and two text
   actions: "Blokir Kartu" and "Atur Limit".
4. Section label "Flazz". Card with masked Flazz number, last known balance,
   last-updated text, and two actions: "Cek Saldo" and "Top Up".
5. Bottom navigation with exactly four items: Beranda, Rekening (active),
   Riwayat, Akun — plus a circular scan FAB in the center overlapping the bar.
   Active item #0060AF, inactive #777777.

Do NOT add: user avatar, greeting text, user name, "Lihat Semua" buttons,
charts, spending summaries, promo banners, or any element not listed above.
```

---

## 8. Verifikasi setelah generate

Jangan langsung dipakai. Periksa satu per satu:

- [ ] Tidak ada elemen dari daftar larangan §5
- [ ] Bottom nav tepat 4 item **dan** FAB scan di tengah
- [ ] Tab aktif adalah Rekening
- [ ] Tidak ada tombol back di top bar
- [ ] Semua warna ada di daftar token §2 — periksa hex-nya, jangan percaya kemiripan
- [ ] Semua jarak jatuh di skala spacing §2
- [ ] Radius kartu `12`, border `1dp`
- [ ] Empat varian state §6 ada

Kalau ada yang meleset, perbaiki di Stitch dulu — jangan dikoreksi belakangan di Compose.
Artefak dan desain harus cocok sebelum masuk `screen-inventory.md`.

---

## 9. Setelah desain disetujui

1. Ekspor screenshot ke `docs/design/`
2. Tambahkan entri screen ke
   `.claude/skills/stitch-to-compose/references/screen-inventory.md` — daftar screen §2,
   komponen bersama §1, dan state wajib §5
3. Tambahkan route `Main.Rekening` ke
   `.claude/skills/compose-architecture/references/navigation.md` §3, dan **hapus**
   `Main.Mutasi` dari daftar tab (Mutasi tetap ada sebagai layar, dibuka dari Beranda
   dan dari aksi "Mutasi" di kartu rekening)
4. Perbarui skenario uji navigasi §13 untuk tab yang baru

Langkah 2 dan 3 wajib di commit yang sama dengan perubahan kodenya.
