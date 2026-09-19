# AI Agent Prompt — Android BCA Mobile Frontend

> File ini berisi prompt yang bisa langsung diberikan ke AI agent (Claude, Cursor, dll) untuk mengimplementasikan Android frontend yang mengonsumsi BCA Mobile backend API.
> Copy prompt sesuai phase yang diinginkan.

---

## Cara Pakai

1. Copy file `09-ANDROID-API-SKILL.md` ke project Android: `.claude/skills/bca-mobile-api/SKILL.md`
2. Buka file ini, copy prompt sesuai phase
3. Paste ke AI agent (Claude Code, Cursor, dll)
4. Agent akan mengimplementasikan berdasarkan skill yang sudah di-load

---

## Prompt: Phase 0 — Project Setup + Architecture

```text
Saya sedang membangun Android app (Kotlin, Jetpack Compose, Material 3) untuk BCA Mobile banking.
Backend API sudah ready di http://localhost:8080/v1.
Referensi lengkap API ada di skill file.

ARSITEKTUR WAJIB:
- Clean Architecture: 3 layer terpisah (data → domain ← presentation)
- MVI Pattern: ViewModel sebagai MVI host, satu StateFlow<UiState> + Channel<SideEffect>, semua user action via sealed interface Event, satu method onEvent()
- Dagger Hilt: compile-time DI, @HiltAndroidApp, @HiltViewModel, @Module @InstallIn

Tech Stack:
- Kotlin 2.0+, target SDK 35, min SDK 26
- Jetpack Compose (Material 3) untuk UI
- Dagger Hilt untuk dependency injection
- Retrofit + OkHttp untuk networking
- Moshi (dengan codegen, @JsonClass) untuk JSON serialization
- Jetpack Navigation Compose untuk routing
- EncryptedSharedPreferences untuk secure storage
- Kotlin Coroutines + Flow untuk async
- CameraX + ML Kit Barcode Scanner untuk QRIS
- BiometricPrompt untuk fingerprint/face auth
- Coil untuk image loading

Project Structure:
app/src/main/java/com/bca/mobile/
├── BcaMobileApp.kt                       // @HiltAndroidApp
├── MainActivity.kt                        // @AndroidEntryPoint, single activity
├── data/
│   ├── remote/api/                        // Retrofit interfaces (AuthApi, AccountApi, TransactionApi, EWalletApi, QRISApi, NotificationApi, RegistrationApi)
│   ├── remote/dto/request/                // Request DTOs with @JsonClass
│   ├── remote/dto/response/               // Response DTOs with @JsonClass + ApiEnvelope<T>
│   ├── remote/interceptor/                // AuthInterceptor, TokenRefreshAuthenticator
│   ├── local/TokenManager.kt             // EncryptedSharedPrefs, access token in-memory
│   ├── mapper/                            // DTO ↔ Domain model mappers
│   └── repository/                        // Repository implementations
├── domain/
│   ├── model/                             // Domain models (NO @Json annotations)
│   ├── repository/                        // Repository interfaces
│   └── usecase/                           // UseCase classes (@Inject constructor, operator fun invoke)
├── presentation/
│   ├── navigation/                        // NavHost, Screen routes, NavGraphs
│   ├── common/                            // PinPad, PinVerifyBottomSheet, RupiahFormatter, ShimmerEffect
│   └── {feature}/                         // Per feature: Screen.kt, ViewModel.kt, UiState.kt, Contract.kt
├── di/                                    // NetworkModule, SecurityModule, RepositoryModule
└── util/                                  // PinEncryptor, BiometricHelper, UiError, Extensions

MVI Rules:
- Setiap screen: UiState (immutable data class), Event (sealed interface), SideEffect (sealed interface)
- ViewModel: _state MutableStateFlow, state StateFlow, _sideEffect Channel, sideEffect Flow
- UI: collectAsStateWithLifecycle() untuk state, LaunchedEffect untuk sideEffect
- Navigasi dan one-shot actions (snackbar, toast) via SideEffect, BUKAN state
- ViewModel TIDAK import data.* atau Android Context (kecuali @ApplicationContext via Hilt)

Hilt Modules:
- NetworkModule (@Singleton): OkHttpClient, Retrofit, semua ApiService
- SecurityModule (@Singleton): PinEncryptor, EncryptedSharedPrefs, BiometricHelper
- RepositoryModule: @Binds abstract fun bindXxxRepository(impl: XxxRepositoryImpl): XxxRepository

Setup yang perlu dibuat:
1. Semua file structure di atas
2. Retrofit client dengan AuthInterceptor + TokenRefreshAuthenticator
3. ApiEnvelope<T> generic wrapper + error handling
4. Semua ApiService interfaces (7 total) dengan endpoint signatures dari skill
5. Semua Request/Response DTO models dari skill
6. Domain models (terpisah dari DTO, tanpa JSON annotations)
7. Mappers (DTO → Domain)
8. Repository interfaces (domain) + implementations (data)
9. TokenManager (EncryptedSharedPrefs, in-memory access token)
10. PinEncryptor (RSA-OAEP-SHA256, load PEM dari assets)
11. UiError sealed class (map API error codes)
12. Network Security Config (cleartext hanya localhost + 10.0.2.2)
13. NavHost skeleton dengan route definitions

Jangan implementasikan screen/ViewModel dulu, fokus infrastructure layer.
```

---

## Prompt: Phase 1 — Auth & Home

```text
Implementasikan Phase 1: Auth & Home screens untuk BCA Mobile Android app.
Arsitektur: Clean Architecture + MVI pattern dengan ViewModel + Dagger Hilt.
Backend API dan infrastructure layer (Retrofit, Hilt, TokenManager, PinEncryptor) sudah di-setup.

Setiap screen WAJIB mengikuti MVI pattern:
- UiState: immutable data class
- Event: sealed interface, semua user action
- SideEffect: sealed interface, navigasi dan one-shot effects
- ViewModel: @HiltViewModel, onEvent(Event), StateFlow<UiState> + Channel<SideEffect>

Screens yang perlu dibuat:

1. **SplashScreen** (SplashViewModel + SplashUiState + SplashContract)
   - Event: CheckHealth
   - State: isLoading, maintenanceMode, needsUpdate, minVersion
   - SideEffect: NavigateToLogin, NavigateToHome, NavigateToMaintenance, ShowForceUpdate
   - UseCase: CheckHealthUseCase → call GET /health/config
   - Logic: cek maintenance_mode, min_app_version, cek ada refresh token → auto-refresh → home

2. **LoginScreen** (LoginViewModel + LoginUiState + LoginContract)
   - Event: PinDigitEntered(digit), PinDeleted, LoginClicked, BiometricClicked, ErrorDismissed
   - State: pin (String, max 6), isLoading, error (UiError?), pinLength
   - SideEffect: NavigateToHome, ShowSnackbar(message)
   - UseCase: LoginUseCase → encrypt PIN via PinEncryptor, call POST /auth/login/pin, save tokens via TokenManager
   - Custom PinPad composable (6 digit, bukan system keyboard)
   - FLAG_SECURE (prevent screenshot)
   - Handle: AUTH_INVALID_PIN, AUTH_ACCOUNT_LOCKED, RATE_LIMIT_EXCEEDED, AUTH_DEVICE_NOT_RECOGNIZED

3. **HomeScreen** (HomeViewModel + HomeUiState + HomeContract)
   - Event: Load, Refresh, ToggleBalanceVisibility
   - State: dashboard (DashboardData?), isLoading, error, isBalanceVisible (default false)
   - SideEffect: NavigateToTransfer, NavigateToQRIS, NavigateToEWallet, etc
   - UseCase: GetDashboardUseCase → call GET /account/dashboard
   - UI: greeting + display_name, primary balance (tap to reveal), notification badge, quick action grid
   - Pull-to-refresh, bottom navigation bar

4. **BalanceScreen** (BalanceViewModel + BalanceUiState + BalanceContract)
   - Event: Load, Refresh
   - State: accounts (List<AccountBalance>), isLoading, error
   - UseCase: GetBalanceUseCase → call GET /account/balance
   - Format Rupiah: Rp 50.000.000,00

5. **ProfileScreen** (ProfileViewModel + ProfileUiState + ProfileContract)
   - Event: Load, LogoutClicked, LogoutConfirmed
   - State: profile (ProfileData?), isLoading, error, showLogoutDialog
   - SideEffect: NavigateToLogin, ShowSnackbar
   - UseCase: GetProfileUseCase, LogoutUseCase → POST /auth/logout + clear tokens

Shared components:
- PinPad.kt: Custom 6-digit PIN input composable
- LoadingOverlay.kt: Full-screen loading with progress indicator
- ShimmerEffect.kt: Placeholder loading animation
- ErrorDialog.kt: Bottom sheet with error message + retry button
- RupiahFormatter.kt: Format Long/String to "Rp 50.000.000,00"

Navigation: NavHost di MainActivity, routes as sealed class Screen.
Bottom Nav: Home, Transaction, QRIS (center FAB), Notification, Profile.
```

---

## Prompt: Phase 2 — Transactions & Transfer

```text
Implementasikan Phase 2: Transaction listing dan Transfer flow.
Arsitektur: Clean Architecture + MVI + Dagger Hilt. Phase 1 (Auth, Home) sudah selesai.

1. **MutasiScreen** (MutasiViewModel + MutasiUiState + MutasiContract)
   - Event: LoadInitial, LoadMore, Refresh, ChangePeriod(period), ChangeAccount(accountId)
   - State: mutations (List), cursor, hasMore, isLoading, isLoadingMore, selectedPeriod, selectedAccountId, accounts, error
   - UseCase: GetMutationsUseCase → GET /transactions/mutations (cursor pagination)
   - Infinite scroll via LazyColumn + detectEndReached
   - Period filter chips: 7 Hari, 30 Hari, Bulan Ini, Custom
   - DEBIT = merah, CREDIT = hijau

2. **RiwayatScreen** (RiwayatViewModel + RiwayatUiState + RiwayatContract)
   - Event: LoadInitial, LoadMore, Refresh, ChangeFilter(type)
   - State: transactions (List), cursor, hasMore, isLoading, isLoadingMore, selectedType, error
   - UseCase: GetHistoryUseCase → GET /transactions/history (cursor pagination)
   - Filter chips: All, Transfer, E-Wallet, QRIS
   - Tap item → SideEffect: NavigateToReceipt(transactionId)

3. **ReceiptScreen** (ReceiptViewModel + ReceiptUiState + ReceiptContract)
   - Event: Load(transactionId), Share
   - State: receipt (ReceiptData?), isLoading, error
   - UseCase: GetReceiptUseCase → GET /transactions/{id}/receipt
   - Share button: capture composable as bitmap

4. **TransferFlow** — shared TransferViewModel across sub-screens:

   TransferViewModel: @HiltViewModel, scoped ke navigation graph "transfer"
   - State: TransferUiState (step, recentTransfers, inquiryResult, executeResult, isLoading, error, idempotencyKey, verificationToken)
   - Events: LoadRecent, InputChanged(...), SubmitInquiry, ConfirmTransfer, PinVerified(token), ExecuteRetry, Reset
   - SideEffects: NavigateToConfirm, OpenPinSheet, NavigateToSuccess, NavigateBack

   a. TransferInputScreen: recent transfers list + input form (destination, bank, amount, notes)
   b. TransferConfirmScreen: show inquiry result, "Transfer Sekarang" button → SideEffect: OpenPinSheet
   c. PinVerifyBottomSheet: reusable, purpose="TRANSFER", onVerified → TransferEvent.PinVerified(token)
   d. TransferSuccessScreen: show result, "Lihat Receipt" / "Transfer Lagi" / "Beranda"

   Idempotency: generate UUID saat inquiry success, simpan di state, kirim sebagai X-Idempotency-Key header
   UseCases: GetRecentTransfersUseCase, TransferInquiryUseCase, VerifyPinUseCase, ExecuteTransferUseCase

5. **PinVerifyBottomSheet** — reusable MVI component:
   - Input: purpose (String), onVerified (String) -> Unit, onDismiss () -> Unit
   - Internal: PinVerifyViewModel atau stateless (encrypt + call /auth/pin/verify)
   - 6-digit PinPad, loading state, error handling
   - Will be reused by E-Wallet, QRIS, Change Limit
```

---

## Prompt: Phase 3 — E-Wallet & QRIS

```text
Implementasikan Phase 3: E-Wallet Top-Up dan QRIS Payment.
Arsitektur: Clean Architecture + MVI + Dagger Hilt.
PinVerifyBottomSheet sudah ada dan reusable dari Phase 2.

1. **EWalletFlow** — shared EWalletViewModel scoped ke nav graph "ewallet":
   - State: EWalletUiState(step, providers, selectedProvider, inquiryResult, executeResult, isLoading, error, idempotencyKey)
   - Events: LoadProviders, SelectProvider(id), InputChanged(phone, amount), SubmitInquiry, PinVerified(token), Retry
   - SideEffects: NavigateToInput, NavigateToConfirm, OpenPinSheet, NavigateToSuccess

   a. EWalletProviderScreen: grid providers, call GET /ewallet/providers
   b. EWalletInputScreen: phone input + preset amount chips + custom amount
   c. EWalletConfirmScreen: inquiry result, "Top Up" → PIN sheet
   d. EWalletSuccessScreen: transaction result

   UseCases: GetProvidersUseCase, EWalletInquiryUseCase, ExecuteTopUpUseCase

2. **QRISFlow** — shared QRISViewModel scoped ke nav graph "qris":
   - State: QRISUiState(step, decodeResult, amount, selectedAccountId, executeResult, isLoading, error, idempotencyKey)
   - Events: QRScanned(rawData), AmountChanged(amount), AccountSelected(id), PinVerified(token), Retry, Rescan
   - SideEffects: NavigateToConfirm, OpenPinSheet, NavigateToSuccess, ShowRescanPrompt

   a. QRISScanScreen: CameraX + ML Kit BarcodeScanner, call POST /qris/decode
   b. QRISConfirmScreen: merchant info, amount (input if is_amount_fixed=false), account selector, "Bayar" → PIN sheet
   c. QRISSuccessScreen: payment result

   UseCases: DecodeQRISUseCase, ExecuteQRISPayUseCase

CameraX setup:
- Request CAMERA permission with rationale
- CameraX Preview + ImageAnalysis
- ML Kit BarcodeScanner with FORMAT_QR_CODE
- Flashlight toggle
- Gallery import option (pick image → decode barcode)
```

---

## Prompt: Phase 4 — Biometric, Notification, Settings

```text
Implementasikan Phase 4: Biometric auth, Notifications, dan Settings.
Arsitektur: Clean Architecture + MVI + Dagger Hilt. Phase 1-3 sudah selesai.

1. **BiometricLogin** — extend LoginViewModel:
   - Tambah Event: BiometricClicked, BiometricResult(signature)
   - Flow: POST /auth/biometric/challenge → BiometricPrompt.authenticate() → sign challenge → POST /auth/login/biometric
   - BiometricHelper class (@Inject): wrap AndroidKeyStore + BiometricPrompt
   - Fallback: "Gunakan PIN" button → switch ke PIN input

2. **BiometricSetup** — di SettingsViewModel:
   - Event: EnableBiometric, DisableBiometric
   - Generate EC key pair di AndroidKeyStore (setUserAuthenticationRequired=true)
   - Export public key as PEM
   - Call POST /auth/biometric/register

3. **NotificationScreen** (NotificationViewModel + NotificationUiState + NotificationContract)
   - Event: Load, LoadMore, MarkRead(id), MarkAllRead, Refresh
   - State: notifications (List), cursor, hasMore, isLoading, isLoadingMore, error
   - SideEffect: NavigateToDetail(deepLink)
   - UseCases: GetNotificationsUseCase, MarkNotificationReadUseCase, MarkAllReadUseCase
   - Badge count: dari HomeViewModel dashboard data

4. **ChangePinScreen** (ChangePinViewModel + ChangePinUiState + ChangePinContract)
   - Event: OldPinChanged(pin), NewPinChanged(pin), ConfirmPinChanged(pin), Submit, ErrorDismissed
   - State: oldPin, newPin, confirmPin, step (OLD|NEW|CONFIRM), isLoading, error
   - SideEffect: NavigateToLogin (auto-logout after success)
   - UseCase: ChangePinUseCase

5. **SettingsScreen** (SettingsViewModel + SettingsUiState + SettingsContract)
   - Event: ToggleBiometric, TogglePushNotif, ToggleEmailStatement, Logout, LogoutAllDevices, LogoutConfirmed
   - State: biometricEnabled, pushNotifEnabled, emailStatementEnabled, isLoading, showLogoutDialog
   - UseCases: UpdateSettingsUseCase, LogoutUseCase

6. **TransactionLimitScreen** (LimitViewModel + LimitUiState + LimitContract)
   - Event: Load, UpdateLimit(type, field, value), PinVerified(token), Submit
   - State: limits (Map<String, LimitData>), isLoading, error
   - UseCase: UpdateLimitUseCase (requires PIN verify with purpose CHANGE_LIMIT)

7. **EditProfileScreen** (EditProfileViewModel)
   - Event: EmailChanged(email), OtpChanged(otp), Submit
   - UseCase: UpdateProfileUseCase → PUT /account/profile
```

---

## Prompt: Phase 5 — Registration

```text
Implementasikan Phase 5: Registration flow (Buka Rekening) untuk user baru.
Arsitektur: Clean Architecture + MVI + Dagger Hilt.

Shared RegistrationViewModel scoped ke RegistrationNavGraph:
- State: RegistrationUiState(step, isLoading, error, registrationId, registrationToken, otpDestination, formData, documentsUploaded, pin, confirmPin)
- Events: DataSubmitted(name, nik, phone, email), OtpSubmitted(code), ResendOtp, DocumentUploaded(type, uri), PinEntered(pin), PinConfirmed(pin), ErrorDismissed
- SideEffects: NavigateToOtp, NavigateToDocuments, NavigateToPin, NavigateToSuccess, NavigateToLogin, ShowSnackbar
- Step enum: DATA_INPUT, OTP, DOCUMENTS, CREATE_PIN, COMPLETE

Screens (multi-step wizard with progress indicator):

1. RegistrationDataScreen (Step 1/4):
   - Input: full_name, nik (16 digit), phone_number, email
   - Client-side validation: NIK 16 digits, phone starts 08 (10-13 digits), email format
   - Call POST /registration/initiate
   - Handle REGISTRATION_DUPLICATE → show "Sudah terdaftar" + login link

2. RegistrationOTPScreen (Step 2/4):
   - Show masked phone (otp_destination)
   - 6-digit OTP input (auto-focus, auto-submit)
   - Call POST /registration/verify-otp
   - Handle REGISTRATION_OTP_INVALID, REGISTRATION_OTP_EXPIRED
   - Resend OTP countdown 60s

3. RegistrationDocumentScreen (Step 3/4):
   - Upload KTP: CameraX capture → compress → POST /registration/upload-document (multipart, type=KTP)
   - Upload Selfie: front camera → POST /registration/upload-document (type=SELFIE)
   - Auth: Bearer <registration_token>
   - Validate: JPEG/PNG, max 5MB

4. RegistrationPINScreen (Step 4/4):
   - Create PIN: 6-digit custom PinPad
   - Confirm PIN: re-enter, must match
   - Encrypt PIN via PinEncryptor
   - Call POST /registration/complete
   - Success → show account number, navigate to Login

Navigation: RegistrationNavGraph terpisah dari MainNavGraph.
Back handling: confirmation dialog "Batalkan pendaftaran?" saat back dari step 2+.
Registration token: scoped di ViewModel state, hilang saat process death → restart.

UseCases: InitiateRegistrationUseCase, VerifyOtpUseCase, UploadDocumentUseCase, CompleteRegistrationUseCase
```

---

## Prompt: One-Shot (Semua Sekaligus)

> Gunakan prompt ini jika ingin membangun semuanya dalam satu sesi.

```text
Bangun Android app BCA Mobile (Kotlin, Jetpack Compose, Material 3) yang mengonsumsi backend API.
Referensi lengkap API ada di skill file 09-ANDROID-API-SKILL.md.

ARSITEKTUR WAJIB:
- Clean Architecture: data layer (Retrofit, DTO, Repository impl) → domain layer (model, repository interface, usecase) ← presentation layer (Compose, ViewModel, MVI state)
- MVI Pattern dengan ViewModel: satu StateFlow<UiState> + Channel<SideEffect>, semua user action via sealed interface Event, satu method onEvent(Event)
- Dagger Hilt: @HiltAndroidApp, @HiltViewModel, @Module @InstallIn, @Binds untuk repository, @Provides untuk network/security
- Retrofit + OkHttp + Moshi: AuthInterceptor, TokenRefreshAuthenticator, ApiEnvelope<T> wrapper
- Kotlin Coroutines + Flow: async operations, StateFlow untuk state

MVI Rules:
- UiState: immutable data class, satu-satunya source of truth
- Event: sealed interface per screen, satu event per user action
- SideEffect: sealed interface, untuk navigasi dan one-shot effects (Channel)
- ViewModel: onEvent() method, _state.update { it.copy(...) }, viewModelScope.launch
- Screen: collectAsStateWithLifecycle(), LaunchedEffect for sideEffect

Implementasikan SEMUA fitur:
1. Infrastructure: Hilt modules (Network, Security, Repository), Retrofit client, 7 ApiService interfaces, DTO models, domain models, mappers, repository interfaces + impls, TokenManager, PinEncryptor
2. Auth: Login PIN (MVI), Biometric login (challenge-response), Token refresh (OkHttp Authenticator), Logout, Change PIN
3. Home: Dashboard (MVI), Balance detail (MVI)
4. Profile: User info (MVI), Edit email, Settings, Transaction limits
5. Transactions: Mutasi (MVI, cursor pagination, period filter), Riwayat (MVI, cursor pagination), Receipt detail
6. Transfer: Recent list → Inquiry → Confirm → PIN verify (reusable bottom sheet) → Execute (idempotent) — shared ViewModel, nav graph scoped
7. E-Wallet: Provider list → Input → Inquiry → Confirm → PIN verify → TopUp (idempotent) — shared ViewModel
8. QRIS: CameraX scan → Decode → Confirm → PIN verify → Pay (idempotent) — shared ViewModel
9. Notifications: List (MVI, cursor pagination), Mark read, Mark all read
10. Registration: Initiate → OTP → Upload docs (multipart) → Complete (create PIN) — shared ViewModel, wizard steps
11. Health: Maintenance mode check on splash, Force update check

Transaction pattern: Inquiry → PIN Verify → Execute dengan X-Idempotency-Key header.
Cursor pagination: LazyColumn + detectEndReached, opaque cursor string.
Amount: int64 minor units di request, "string decimal" di response, display as "Rp 50.000.000,00".
Seed data: user NURHOLIS (PIN 123456, device device-nurholis-001, account 1234567890).
```