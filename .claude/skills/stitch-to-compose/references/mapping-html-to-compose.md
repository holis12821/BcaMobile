# Mapping HTML/Tailwind → Jetpack Compose

Dibaca sebelum fase 3 (skeleton). Kolom kanan adalah padanan yang benar; kolom "Jangan"
adalah kesalahan yang paling sering muncul kalau tidak diatur.

## 1. Satuan

| Sumber | Compose | Jangan |
|---|---|---|
| `16px` | `Spacing.s4` (16dp) | `16.dp` inline |
| `1rem` | ×16 → dp, lalu petakan ke token | `1.rem` (tidak ada) |
| `font-size: 14px` | token type scale | `14.sp` inline |
| `%` lebar | `fillMaxWidth(fraction)` | lebar tetap hasil ukur |
| `vh` / `vw` | `fillMaxHeight()` / `fillMaxWidth()` | konversi ke dp |

Nilai px yang tidak jatuh tepat di skala spacing **bukan** untuk dibulatkan diam-diam —
itu kondisi STOP.

## 2. Layout

| Sumber | Compose |
|---|---|
| `flex-col` | `Column` |
| `flex-row` | `Row` |
| `flex-col gap-4` | `Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4))` |
| `flex-row gap-2` | `Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s2))` |
| `justify-between` | `Arrangement.SpaceBetween` (di sumbu utama) |
| `justify-center` | `Arrangement.Center` |
| `items-center` (row) | `verticalAlignment = Alignment.CenterVertically` |
| `items-center` (col) | `horizontalAlignment = Alignment.CenterHorizontally` |
| `flex-1` / `flex-grow` | `Modifier.weight(1f)` |
| `position: relative` + child absolute | `Box` + `Modifier.align(...)` |
| `position: sticky` (header) | `TopAppBar` di `Scaffold`, atau `stickyHeader` di `LazyColumn` |
| `position: fixed` bottom | slot `bottomBar` di `Scaffold` |
| `grid grid-cols-4` | `LazyVerticalGrid(GridCells.Fixed(4))`, atau `Row` bersarang untuk grid kecil dan tetap |
| `overflow-y: auto` (konten pendek, tetap) | `Column` + `Modifier.verticalScroll(rememberScrollState())` |
| `overflow-y: auto` (list dinamis) | `LazyColumn` dengan `key` yang stabil |
| `z-index` | urutan penulisan di dalam `Box` (yang terakhir di atas) |

## 3. Kotak, garis, bayangan

| Sumber | Compose |
|---|---|
| `p-4` | `Modifier.padding(Spacing.s4)` |
| `px-4 py-2` | `Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s2)` |
| `m-4` | padding pada parent, atau `Arrangement.spacedBy` — **bukan** margin (tidak ada di Compose) |
| `rounded-lg` | `AppShape.R6` (sesuaikan dengan nilai px aslinya) |
| `rounded-full` | `AppShape.Full` |
| `border` | `Modifier.border(StrokeWidth.w0, color, shape)` |
| `divide-y` | `HorizontalDivider()` antar item |
| `shadow-*` | `Card(elevation = …)` atau `Surface(tonalElevation = …)` |
| `bg-*` | `Modifier.background(token, shape)` — urutkan sebelum `padding` |
| `opacity-50` | `color.copy(alpha = 0.5f)`, atau `Modifier.alpha(0.5f)` untuk seluruh subtree |

Urutan modifier penting: `clip` → `background` → `border` → `clickable` → `padding`.
Padding sebelum background membuat area background ikut mengecil.

## 4. Teks

| Sumber | Compose |
|---|---|
| `font-bold` (700) | `fontWeight = FontWeight.Bold` lewat token type |
| `font-medium` (500) | `FontWeight.Medium` |
| `text-center` | `textAlign = TextAlign.Center` |
| `truncate` / `text-ellipsis` | `maxLines = 1, overflow = TextOverflow.Ellipsis` |
| `line-clamp-2` | `maxLines = 2, overflow = TextOverflow.Ellipsis` |
| `uppercase` | ubah di string resource, bukan di kode |
| teks literal | `stringResource(R.string.…)` |

## 5. Interaksi

| Sumber | Compose |
|---|---|
| `<button>` | `Button` / `TextButton` / `OutlinedButton` / `IconButton` |
| `<input type="text">` | `TextField` / `OutlinedTextField` |
| `<select>` | `ExposedDropdownMenuBox` |
| `<input type="checkbox">` | `Checkbox` |
| toggle switch | `Switch` |
| `hover:*` | tidak ada padanan di mobile — abaikan |
| `active:*` / `:pressed` | `interactionSource` + `collectIsPressedAsState()` |
| `disabled` | parameter `enabled = false` |
| `cursor-pointer` pada div | `Modifier.clickable` + pastikan target ≥ 48dp |

## 6. Gambar & ikon

| Sumber | Compose |
|---|---|
| `<img>` lokal | `Image(painterResource(...), contentDescription = ...)` |
| `<img>` remote | Coil `AsyncImage`, dengan placeholder dan error |
| `object-fit: cover` | `contentScale = ContentScale.Crop` |
| `object-fit: contain` | `contentScale = ContentScale.Fit` |
| ikon SVG | vector drawable + `Icon(painterResource(...), contentDescription = ...)` |

Ikon dekoratif: `contentDescription = null` secara eksplisit. Jangan dikosongkan begitu saja.

---

## 7. Anti-pattern

### Hardcode nilai visual

```kotlin
// SALAH
Text(
    text = "Total Saldo",
    color = Color(0xFF0060AF),
    fontSize = 14.sp,
    modifier = Modifier.padding(16.dp),
)

// BENAR
Text(
    text = stringResource(R.string.total_saldo),
    style = MaterialTheme.typography.bodyMedium,
    color = AppColor.Primary700,
    modifier = Modifier.padding(Spacing.s4),
)
```

### Spacing lewat padding per anak

```kotlin
// SALAH — jarak tidak konsisten, item terakhir ikut kena padding bawah
Column {
    items.forEach { ItemRow(it, Modifier.padding(bottom = Spacing.s4)) }
}

// BENAR
Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
    items.forEach { ItemRow(it) }
}
```

### List dinamis pakai Column

```kotlin
// SALAH — semua item dikomposisi sekaligus
Column(Modifier.verticalScroll(rememberScrollState())) {
    mutations.forEach { MutationRow(it) }
}

// BENAR
LazyColumn {
    items(mutations, key = { it.id }) { MutationRow(it) }
}
```

### Absolute positioning jadi offset

```kotlin
// SALAH — pecah di ukuran layar lain
Box {
    Fab(Modifier.offset(x = 150.dp, y = 600.dp))
}

// BENAR
Box(Modifier.fillMaxSize()) {
    Fab(Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.s6))
}
```

### Composable menyentuh ViewModel

```kotlin
// SALAH
@Composable
fun MutasiScreen(viewModel: MutasiViewModel = hiltViewModel()) {
    val state = viewModel.state.collectAsState()
    ...
}

// BENAR — screen stateless, wiring di layer atas
@Composable
fun MutasiScreen(
    state: MutasiUiState,
    onFilterClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) { ... }
```

### Touch target di bawah 48dp

```kotlin
// SALAH
Icon(..., modifier = Modifier.size(20.dp).clickable(onClick = onClose))

// BENAR
IconButton(onClick = onClose, modifier = Modifier.size(AppSize.MinTouchTarget)) {
    Icon(..., modifier = Modifier.size(20.dp))
}
```

### Lebar tetap hasil ukur screenshot

```kotlin
// SALAH
Button(onClick = ..., modifier = Modifier.width(343.dp))

// BENAR
Button(onClick = ..., modifier = Modifier.fillMaxWidth())
```
