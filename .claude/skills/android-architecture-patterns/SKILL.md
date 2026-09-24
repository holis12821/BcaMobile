---
name: android-architecture-patterns
description: Android Implementation Guide — Clean Architecture + MVI + Dagger Hilt. Gunakan saat setup project structure, membuat layer architecture (data/domain/presentation), implementasi MVI pattern, Hilt DI module, UseCase, Repository, ApiService, DTO/Domain model mapping, atau phase-based implementation planning. Trigger juga pada "setup Clean Architecture", "buat UseCase", "buat Repository", "Hilt module", "Retrofit API", "MVI pattern", "unidirectional data flow", dan "project structure". JANGAN dipakai untuk navigasi/state Compose (itu `compose-architecture`) atau visual/token (itu `stitch-to-compose`).
---

# Android Implementation Guide — Clean Architecture + MVI

> Panduan untuk AI agent dan developer: arsitektur, pattern, prioritas implementasi, dan best practice perbankan digital.

---

## Arsitektur: Clean Architecture + MVI + Dagger Hilt

### Tech Stack

| Layer | Library | Fungsi |
|-------|---------|--------|
| UI | Jetpack Compose + Material 3 | Declarative UI |
| Pattern | MVI (Model-View-Intent) | Unidirectional data flow |
| Navigation | Compose Navigation | Single activity routing |
| DI | Dagger Hilt | Compile-time DI |
| Network | Retrofit + OkHttp + kotlinx-serialization | HTTP client + JSON |
| Storage | EncryptedSharedPreferences | Secure token storage |
| Security | AndroidKeyStore, BiometricPrompt | Biometric + key management |
| Camera | CameraX + ML Kit Barcode | QRIS scanner |
| Image | Coil | Image loading |
| Async | Kotlin Coroutines + Flow | Async operations |

### MVI dengan ViewModel

Pattern MVI tetap menggunakan `ViewModel` sebagai host. Perbedaan dengan MVVM biasa:

| Aspek | MVVM | MVI (yang kita pakai) |
|-------|------|----------------------|
| Input ke ViewModel | Banyak method publik | Satu method `onEvent(Event)` |
| Output ke UI | Multiple LiveData/StateFlow | Satu `StateFlow<UiState>` + `Channel<SideEffect>` |
| State mutation | Langsung update field | `_state.update { it.copy(...) }` — selalu immutable |
| Side effects | Campur di state | Terpisah via `Channel` (navigasi, snackbar, dll) |
| Testability | Mock ViewModel methods | Kirim Event, assert State |

**Per screen, buat 4 file:**
```
presentation/home/
├── HomeScreen.kt          // @Composable, observe state, dispatch events
├── HomeViewModel.kt       // @HiltViewModel, onEvent(), state + sideEffect
├── HomeUiState.kt         // data class, immutable, single source of truth
├── HomeContract.kt        // sealed interface Event + sealed interface SideEffect
```

### Layer Rules

| Rule | Detail |
|------|--------|
| Presentation → Domain | ViewModel inject UseCase, BUKAN Repository |
| Domain → Data | Repository interface di domain, implementation di data |
| Data → Remote | ApiService (Retrofit), DTO models (dengan @Json) |
| Domain model | TIDAK punya @Json annotation, pure Kotlin data class |
| UseCase | Single responsibility, `operator fun invoke(...)`, constructor @Inject |
| ViewModel | TIDAK import `data.*` package, hanya `domain.*` |

---

## Prinsip Implementasi

Berdasarkan best practice perbankan digital (BCA Mobile, Jago, Jenius, Livin' by Mandiri):

1. **Security first** — Auth dan device binding harus solid sebelum fitur lain
2. **Core before convenience** — Saldo dan transfer dulu, baru QRIS dan e-wallet
3. **Read before write** — Tampilkan data dulu, baru fitur transaksi
4. **Progressive disclosure** — Buka fitur bertahap, bukan sekaligus
5. **Unidirectional** — Data mengalir satu arah: Event → ViewModel → State → UI

---

## Implementasi Wajib (Semua API)

**Ya, semua 35 endpoint harus diimplementasikan.** Tidak ada endpoint opsional karena:

- Endpoint auth (7) — wajib untuk security flow
- Endpoint account (6) — wajib untuk core banking
- Endpoint transaksi (9) — wajib untuk core value proposition
- Endpoint e-wallet (3) — wajib (fitur standar mobile banking)
- Endpoint QRIS (2) — wajib (regulasi BI, semua bank wajib support QRIS)
- Endpoint notifikasi (3) — wajib (compliance: notifikasi transaksi real-time)
- Endpoint registrasi (4) — wajib (onboarding nasabah baru)
- Endpoint health (3) — wajib (maintenance mode, force update, feature flags)

---

## Phase 0: Project Setup (Hari 1-3)

> Goal: Project structure, DI, network layer, security utilities — belum ada screen.

### Yang dibuat

| Komponen | File | Layer |
|----------|------|-------|
| Application | `BcaMobileApp.kt` (@HiltAndroidApp) | - |
| Activity | `MainActivity.kt` (@AndroidEntryPoint) | - |
| NetworkModule | `di/NetworkModule.kt` | DI |
| SecurityModule | `di/SecurityModule.kt` | DI |
| RepositoryModule | `di/RepositoryModule.kt` | DI |
| Retrofit + OkHttp | `data/remote/interceptor/` | Data |
| ApiService x7 | `data/remote/api/` | Data |
| DTO models | `data/remote/dto/request/`, `dto/response/` | Data |
| Envelope wrapper | `data/remote/dto/response/ApiEnvelope.kt` | Data |
| TokenManager | `data/local/TokenManager.kt` | Data |
| PinEncryptor | `util/PinEncryptor.kt` | Util |
| UiError | `util/UiError.kt` | Util |
| RupiahFormatter | `presentation/common/RupiahFormatter.kt` | Presentation |
| NavHost skeleton | `presentation/navigation/` | Presentation |

---

## Phase 1: Foundation (Minggu 1-2)

> Goal: User bisa login, lihat saldo, dan logout.

### MVI Components per Screen

| Screen | ViewModel | UiState | Events | UseCases |
|--------|-----------|---------|--------|----------|
| Splash | SplashViewModel | `SplashUiState(isLoading, maintenanceMode, needsUpdate)` | `CheckHealth` | CheckHealthUseCase |
| Login | LoginViewModel | `LoginUiState(pin, isLoading, error, pinLength)` | `PinDigitEntered, PinDeleted, Login, BiometricClicked, ErrorDismissed` | LoginUseCase |
| Home | HomeViewModel | `HomeUiState(dashboard, isLoading, error, isBalanceVisible)` | `Load, Refresh, ToggleBalance` | GetDashboardUseCase |
| Balance | BalanceViewModel | `BalanceUiState(accounts, isLoading, error)` | `Load, Refresh` | GetBalanceUseCase |
| Profile | ProfileViewModel | `ProfileUiState(profile, isLoading, error)` | `Load, Logout` | GetProfileUseCase, LogoutUseCase |

### Endpoints

| # | Endpoint | UseCase |
|---|----------|---------|
| 1 | `GET /health/config` | CheckHealthUseCase |
| 2 | `POST /auth/login/pin` | LoginUseCase |
| 3 | `POST /auth/token/refresh` | RefreshTokenUseCase (via Authenticator) |
| 4 | `POST /auth/logout` | LogoutUseCase |
| 5 | `GET /account/dashboard` | GetDashboardUseCase |
| 6 | `GET /account/balance` | GetBalanceUseCase |
| 7 | `GET /account/profile` | GetProfileUseCase |

---

## Phase 2: Core Transactions (Minggu 3-4)

> Goal: User bisa lihat mutasi dan transfer antar rekening.

### MVI Components

| Screen | ViewModel | Key Events | UseCases |
|--------|-----------|------------|----------|
| Mutasi | MutasiViewModel | `Load, LoadMore, Refresh, ChangePeriod, ChangeAccount` | GetMutationsUseCase |
| Riwayat | RiwayatViewModel | `Load, LoadMore, Refresh, ChangeFilter` | GetHistoryUseCase |
| Receipt | ReceiptViewModel | `Load, Share` | GetReceiptUseCase |
| Transfer | TransferViewModel (shared) | `SubmitInquiry, ConfirmTransfer, PinVerified(token), Retry` | TransferInquiryUseCase, VerifyPinUseCase, ExecuteTransferUseCase |

**TransferViewModel** di-share across 4 sub-screens (Input, Confirm, PIN, Success) via Hilt `NavBackStackEntry` scoping:

```kotlin
// In NavGraph
composable("transfer/input") {
    val parentEntry = remember(it) { navController.getBackStackEntry("transfer") }
    val viewModel: TransferViewModel = hiltViewModel(parentEntry)
    TransferInputScreen(viewModel)
}
```

### Reusable Component: PinVerifyBottomSheet

```kotlin
// Standalone MVI component, bisa dipanggil dari screen manapun
@Composable
fun PinVerifyBottomSheet(
    purpose: String,                    // "TRANSFER", "EWALLET_TOPUP", etc
    onVerified: (verificationToken: String) -> Unit,
    onDismiss: () -> Unit,
)
```

### Endpoints

| # | Endpoint |
|---|----------|
| 8 | `GET /transactions/mutations` |
| 9 | `GET /transactions/history` |
| 10 | `GET /transactions/{id}/receipt` |
| 11 | `GET /transfer/recent` |
| 12 | `POST /transfer/inquiry` |
| 13 | `POST /auth/pin/verify` |
| 14 | `POST /transfer/execute` |

---

## Phase 3: E-Wallet & QRIS (Minggu 5-6)

> Goal: User bisa top-up e-wallet dan bayar QRIS.

### MVI Components

| Screen | ViewModel | Key Events |
|--------|-----------|------------|
| E-Wallet | EWalletViewModel (shared) | `LoadProviders, SelectProvider, SubmitInquiry, PinVerified, Retry` |
| QRIS | QRISViewModel (shared) | `QRScanned, AmountEntered, PinVerified, Retry` |

### Endpoints

| # | Endpoint |
|---|----------|
| 15 | `GET /ewallet/providers` |
| 16 | `POST /ewallet/inquiry` |
| 17 | `POST /ewallet/topup` |
| 18 | `POST /qris/decode` |
| 19 | `POST /qris/pay` |

---

## Phase 4: Secondary Features (Minggu 7-8)

> Goal: Biometric login, notifikasi, pengaturan.

### MVI Components

| Screen | ViewModel | Key Events |
|--------|-----------|------------|
| Notification | NotificationViewModel | `Load, LoadMore, MarkRead(id), MarkAllRead` |
| Settings | SettingsViewModel | `ToggleBiometric, TogglePushNotif, Logout, LogoutAll` |
| Change PIN | ChangePinViewModel | `OldPinEntered, NewPinEntered, ConfirmPinEntered, Submit` |
| Limit | LimitViewModel | `Load, UpdateLimit(type, value), PinVerified, Submit` |

### Endpoints

| # | Endpoint |
|---|----------|
| 20-21 | Biometric challenge + login |
| 22 | Biometric register |
| 23 | Change PIN |
| 24-26 | Notifications (list, mark read, mark all) |
| 27 | Update profile |
| 28 | Update settings |
| 29 | Update transaction limit |

---

## Phase 5: Onboarding (Minggu 9-10)

> Goal: User baru bisa registrasi dari app.

### MVI Components

| Step | ViewModel | Key Events |
|------|-----------|------------|
| All steps | RegistrationViewModel (shared) | `SubmitData, SubmitOtp, ResendOtp, UploadDoc, SubmitPin, ConfirmPin` |

**UiState tracks wizard step:**
```kotlin
data class RegistrationUiState(
    val step: RegistrationStep = RegistrationStep.DATA_INPUT,
    val isLoading: Boolean = false,
    val error: UiError? = null,
    val registrationId: String? = null,
    val registrationToken: String? = null,
    val otpDestination: String? = null,
    // form fields...
)

enum class RegistrationStep { DATA_INPUT, OTP, DOCUMENTS, CREATE_PIN, COMPLETE }
```

### Endpoints

| # | Endpoint |
|---|----------|
| 30 | Registration initiate |
| 31 | Verify OTP |
| 32 | Upload document |
| 33 | Complete registration |

---

## Phase 6: Polish (Minggu 11-12)

| Item | Detail |
|------|--------|
| Health endpoints | `GET /health`, `GET /health/live` for debug screen |
| Pull-to-refresh | Swipe refresh on all list screens |
| Deep linking | Notification → receipt/transaction screen |
| Share receipt | Screenshot composable as bitmap |
| Animations | Lottie for transaction success |
| Dark mode | Material 3 dynamic colors |
| Offline | Show cached data when no network |

---

## Checklist Keamanan Android

| # | Item | Wajib |
|---|------|-------|
| 1 | PIN encrypted RSA-OAEP-SHA256, NEVER plaintext | Ya |
| 2 | Access token in-memory, refresh token di EncryptedSharedPrefs | Ya |
| 3 | Biometric key di AndroidKeyStore (hardware-backed) | Ya |
| 4 | Certificate pinning (OkHttp CertificatePinner) | Ya (prod) |
| 5 | Root/jailbreak detection | Ya |
| 6 | `FLAG_SECURE` di screen sensitif (login, PIN, transfer confirm) | Ya |
| 7 | ProGuard/R8 obfuscation | Ya |
| 8 | No sensitive data in logs (token, PIN, account number) | Ya |
| 9 | Network Security Config (cleartext hanya localhost) | Ya |
| 10 | App integrity check (Play Integrity API) | Rekomendasi |
| 11 | Auto-logout setelah 15 menit inaktif | Ya |
| 12 | Clipboard clear setelah paste account number | Rekomendasi |