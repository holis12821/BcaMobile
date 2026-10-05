package id.bca.bcamobile.ui.components

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier

/**
 * Jarak aman untuk isi bar yang menempel di tepi bawah layar.
 *
 * `Scaffold` meletakkan slot `bottomBar` persis di tepi window tanpa inset apa pun
 * (Material3 1.4.0, `Scaffold.kt`: `bottomBarPlaceable.place(0, layoutHeight - height)`) —
 * komponen bawaan seperti `NavigationBar` mengurusnya sendiri, bar buatan sendiri tidak.
 * Tanpa ini tombol utama duduk di bawah navigation bar, jadi di perangkat dengan navigasi
 * on-screen tombolnya tertimpa Back/Home. `enableEdgeToEdge()` di `MainActivity` juga
 * membuat window tidak lagi menyusut saat papan ketik muncul, jadi IME ikut diurus di sini.
 *
 * Dipasang pada **isi** bar, bukan pada `Surface` pembungkusnya: latar dan shadow bar tetap
 * menutup sampai tepi layar, yang naik hanya tombolnya.
 *
 * Urutan kedua modifier ini mengikat. `navigationBarsPadding()` mengonsumsi inset navigation
 * bar lebih dulu, jadi `imePadding()` sesudahnya hanya menambahkan selisihnya — hasilnya
 * `max(navigation bar, papan ketik)`. Dibalik, keduanya tertumpuk dan muncul celah setinggi
 * navigation bar setiap papan ketik terbuka.
 *
 * Efek sampingnya yang disengaja: tinggi bar bertambah saat papan ketik muncul, dan karena
 * `Scaffold` menghitung `innerPadding.bottom` konten dari tinggi bar itu, area gulir di
 * atasnya ikut menyusut. Form tetap terjangkau seluruhnya tanpa modifier tambahan.
 */
fun Modifier.bottomBarSafePadding(): Modifier = navigationBarsPadding().imePadding()
