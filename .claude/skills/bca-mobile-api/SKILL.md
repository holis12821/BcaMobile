---
name: bca-mobile-api
description: Backend API integration untuk bca_mobile — endpoint reference, response envelope, PIN encryption RSA-OAEP, token management (access/refresh), error code mapping, idempotency, pagination cursor-based, dan transaction flow (inquiry → PIN verify → execute). Gunakan saat mengintegrasikan endpoint backend, membuat ApiService/DTO/Repository, setup auth interceptor/authenticator, handle error codes, implementasi transaction flow, atau seed data. Trigger juga pada "endpoint apa untuk ...", "format request/response", "error code handling", "token refresh", "idempotency key", "PIN encrypt", "screen-to-endpoint mapping", dan "API envelope". JANGAN dipakai untuk navigasi/state Compose (itu `compose-architecture`) atau visual/token (itu `stitch-to-compose`).
---

# BCA Mobile Android — Backend API Integration Skill

> Skill file untuk AI agent yang mengimplementasikan Android client (Kotlin/Jetpack Compose) untuk BCA Mobile backend API.
> Salin file ini ke project Android: `.claude/skills/bca-mobile-api/SKILL.md`

---

## 0. Konvensi Dasar

| Aspek | Aturan |
|-------|--------|
| Base URL | `http://<host>:8080/v1` (dev), `https://api.bcamobile.id/v1` (prod) |
| Content-Type | `application/json` (kecuali upload dokumen: `multipart/form-data`) |
| Auth header | `Authorization: Bearer <access_token>` |
| Idempotency header | `X-Idempotency-Key: <uuid-v4>` (wajib untuk execute/topup/pay) |
| Money format | String decimal `"1500000.00"` di response, `int64` minor units (sen) di request |
| ID format | UUID v4 string |
| Timestamp | RFC 3339 (`2026-09-11T10:30:00Z`) |
| Date | `YYYY-MM-DD` |
| Time | `HH:MM:SS` |

---

## 1. Arsitektur & Pattern

### 1.1 Clean Architecture + MVI

```
┌─────────────────────────────────────────────────────────────────┐
│                      Presentation Layer                         │
│  ┌──────────┐   ┌──────────────┐   ┌─────────────────────────┐ │
│  │  Screen   │ → │  ViewModel   │ ← │  UiState (immutable)    │ │
│  │ (Compose) │   │  (MVI host)  │   │  UiEvent (user action)  │ │
│  │           │ ← │              │ → │  SideEffect (one-shot)  │ │
│  └──────────┘   └──────────────┘   └─────────────────────────┘ │
│                         │                                       │
│                         │ inject UseCase                        │
├─────────────────────────┼───────────────────────────────────────┤
│                   Domain Layer                                  │
│  ┌──────────────────────┴──────────────────────────┐            │
│  │  UseCase (single responsibility, operator fun)  │            │
│  │  Domain Model (tidak punya JSON annotation)     │            │
│  │  Repository Interface                           │            │
│  └─────────────────────────────────────────────────┘            │
│                         │                                       │
├─────────────────────────┼───────────────────────────────────────┤
│                    Data Layer                                   │
│  ┌──────────────────────┴──────────────────────────┐            │
│  │  RepositoryImpl (implements domain interface)   │            │
│  │  ApiService (Retrofit interface)                │            │
│  │  DTO / Response models (JSON annotation)        │            │
│  │  Mapper (DTO → Domain Model)                    │            │
│  │  LocalDataSource (EncryptedSharedPrefs, Room)   │            │
│  └─────────────────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 MVI Pattern (Model-View-Intent)

Setiap screen mengikuti pola MVI:

```kotlin
// 1. UiState — immutable data class, satu-satunya source of truth untuk UI
data class LoginUiState(
    val isLoading: Boolean = false,
    val pin: String = "",
    val error: UiError? = null,
    val isLoggedIn: Boolean = false,
)

// 2. UiEvent — sealed interface, semua aksi user
sealed interface LoginEvent {
    data class PinDigitEntered(val digit: Int) : LoginEvent
    data object PinDeleted : LoginEvent
    data object LoginClicked : LoginEvent
    data object BiometricClicked : LoginEvent
    data object ErrorDismissed : LoginEvent
}

// 3. SideEffect — one-shot effects (navigasi, snackbar, dll)
sealed interface LoginSideEffect {
    data object NavigateToHome : LoginSideEffect
    data class ShowSnackbar(val message: String) : LoginSideEffect
}

// 4. ViewModel — MVI host, menerima Event → update State / emit SideEffect
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val pinEncryptor: PinEncryptor,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _sideEffect = Channel<LoginSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<LoginSideEffect> = _sideEffect.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.PinDigitEntered -> reducePinInput(event.digit)
            is LoginEvent.PinDeleted -> reducePinDelete()
            is LoginEvent.LoginClicked -> executeLogin()
            is LoginEvent.BiometricClicked -> executeBiometric()
            is LoginEvent.ErrorDismissed -> _state.update { it.copy(error = null) }
        }
    }

    private fun executeLogin() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loginUseCase(pin = pinEncryptor.encrypt(_state.value.pin))
                .onSuccess { _sideEffect.send(LoginSideEffect.NavigateToHome) }
                .onFailure { e -> _state.update { it.copy(error = e.toUiError()) } }
            _state.update { it.copy(isLoading = false) }
        }
    }
}

// 5. Screen (Compose) — observe state, dispatch events
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateHome: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is LoginSideEffect.NavigateToHome -> onNavigateHome()
                is LoginSideEffect.ShowSnackbar -> { /* show snackbar */ }
            }
        }
    }

    LoginContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}
```

**Aturan MVI:**
- UI HANYA membaca `state` dan memanggil `onEvent()` — tidak pernah memanggil repository/usecase langsung
- State SELALU immutable data class — update via `copy()`
- Navigasi dan one-shot UI actions via `SideEffect` (Channel), bukan state
- ViewModel TIDAK menyimpan reference ke Context, Activity, atau View
- Setiap user action = satu Event — tidak ada logic di Composable

### 1.3 Dagger Hilt Dependency Injection

```
┌─────────────────────────────────────┐
│         @HiltAndroidApp             │
│         BcaMobileApp                │
├─────────────────────────────────────┤
│         @Module                     │
│  ┌──────────────────────────────┐   │
│  │ NetworkModule (@Singleton)   │   │
│  │  ├ OkHttpClient              │   │
│  │  ├ Retrofit                  │   │
│  │  ├ AuthApi, AccountApi, ...  │   │
│  │  └ TokenManager              │   │
│  ├──────────────────────────────┤   │
│  │ SecurityModule (@Singleton)  │   │
│  │  ├ PinEncryptor              │   │
│  │  ├ EncryptedSharedPrefs      │   │
│  │  └ BiometricHelper           │   │
│  ├──────────────────────────────┤   │
│  │ RepositoryModule (@Singleton)│   │
│  │  ├ AuthRepository            │   │
│  │  ├ AccountRepository         │   │
│  │  ├ TransactionRepository     │   │
│  │  ├ EWalletRepository         │   │
│  │  ├ QRISRepository            │   │
│  │  ├ NotificationRepository    │   │
│  │  └ RegistrationRepository    │   │
│  └──────────────────────────────┘   │
├─────────────────────────────────────┤
│  UseCase (no @Module needed,        │
│  constructor-injected via @Inject)  │
├─────────────────────────────────────┤
│  @HiltViewModel                     │
│  ViewModel (injected UseCases)      │
└─────────────────────────────────────┘
```

**Hilt Module contoh:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenRefreshAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
```

**UseCase contoh (constructor-injected, tidak perlu @Module):**

```kotlin
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) {
    suspend operator fun invoke(pin: String): Result<LoginResponse> {
        return authRepository.loginByPin(pin)
            .onSuccess { response ->
                tokenManager.saveTokens(response.accessToken, response.refreshToken)
            }
    }
}
```

### 1.4 Project Structure

```
app/src/main/java/com/bca/mobile/
├── BcaMobileApp.kt                          // @HiltAndroidApp
├── MainActivity.kt                          // @AndroidEntryPoint, single activity
│
├── data/
│   ├── remote/
│   │   ├── api/                             // Retrofit interfaces
│   │   │   ├── AuthApi.kt
│   │   │   ├── AccountApi.kt
│   │   │   ├── TransactionApi.kt
│   │   │   ├── EWalletApi.kt
│   │   │   ├── QRISApi.kt
│   │   │   ├── NotificationApi.kt
│   │   │   └── RegistrationApi.kt
│   │   ├── dto/                             // JSON request/response models
│   │   │   ├── request/
│   │   │   │   ├── LoginRequest.kt
│   │   │   │   ├── TransferInquiryRequest.kt
│   │   │   │   └── ...
│   │   │   └── response/
│   │   │       ├── ApiEnvelope.kt           // generic envelope wrapper
│   │   │       ├── LoginResponse.kt
│   │   │       ├── DashboardResponse.kt
│   │   │       └── ...
│   │   └── interceptor/
│   │       ├── AuthInterceptor.kt
│   │       └── TokenRefreshAuthenticator.kt
│   ├── local/
│   │   └── TokenManager.kt                 // EncryptedSharedPrefs
│   ├── mapper/                              // DTO ↔ Domain mappers
│   │   ├── AuthMapper.kt
│   │   ├── AccountMapper.kt
│   │   └── ...
│   └── repository/                          // Repository implementations
│       ├── AuthRepositoryImpl.kt
│       ├── AccountRepositoryImpl.kt
│       └── ...
│
├── domain/
│   ├── model/                               // Domain models (no JSON annotations)
│   │   ├── User.kt
│   │   ├── Account.kt
│   │   ├── Transaction.kt
│   │   ├── Mutation.kt
│   │   └── ...
│   ├── repository/                          // Repository interfaces
│   │   ├── AuthRepository.kt
│   │   ├── AccountRepository.kt
│   │   └── ...
│   └── usecase/                             // Use cases
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   ├── LogoutUseCase.kt
│       │   ├── RefreshTokenUseCase.kt
│       │   ├── VerifyPinUseCase.kt
│       │   └── ...
│       ├── account/
│       │   ├── GetDashboardUseCase.kt
│       │   ├── GetBalanceUseCase.kt
│       │   └── ...
│       ├── transaction/
│       │   ├── GetMutationsUseCase.kt
│       │   ├── TransferInquiryUseCase.kt
│       │   ├── ExecuteTransferUseCase.kt
│       │   └── ...
│       └── ...
│
├── presentation/
│   ├── navigation/
│   │   ├── BcaNavHost.kt                    // Top-level NavHost
│   │   ├── Screen.kt                        // Route sealed class
│   │   ├── MainNavGraph.kt
│   │   ├── AuthNavGraph.kt
│   │   ├── TransactionNavGraph.kt
│   │   └── RegistrationNavGraph.kt
│   ├── common/                              // Shared UI components
│   │   ├── PinVerifyBottomSheet.kt          // Reusable PIN verify (MVI)
│   │   ├── PinPad.kt                        // Custom 6-digit PIN pad
│   │   ├── LoadingOverlay.kt
│   │   ├── ErrorDialog.kt
│   │   ├── RupiahFormatter.kt
│   │   └── ShimmerEffect.kt
│   ├── auth/
│   │   ├── login/
│   │   │   ├── LoginScreen.kt
│   │   │   ├── LoginViewModel.kt
│   │   │   ├── LoginUiState.kt
│   │   │   ├── LoginEvent.kt
│   │   │   └── LoginSideEffect.kt
│   │   └── register/
│   │       ├── RegistrationScreen.kt
│   │       ├── RegistrationViewModel.kt
│   │       └── ...
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   ├── HomeViewModel.kt
│   │   ├── HomeUiState.kt
│   │   ├── HomeEvent.kt
│   │   └── HomeSideEffect.kt
│   ├── transfer/
│   │   ├── input/
│   │   ├── confirm/
│   │   ├── success/
│   │   └── TransferViewModel.kt             // shared across transfer steps
│   ├── ewallet/
│   ├── qris/
│   ├── notification/
│   ├── profile/
│   └── settings/
│
├── di/                                      // Hilt modules
│   ├── NetworkModule.kt
│   ├── SecurityModule.kt
│   └── RepositoryModule.kt
│
└── util/
    ├── PinEncryptor.kt
    ├── BiometricHelper.kt
    ├── Extensions.kt
    └── UiError.kt                           // Sealed class for error mapping
```

### 1.5 Non-Negotiables

1. **UI layer TIDAK import `data.remote.*` atau `data.repository.*`** — hanya domain layer
2. **Domain layer TIDAK import Android framework** — pure Kotlin, testable tanpa Robolectric
3. **DTO dan Domain model HARUS terpisah** — DTO punya `@Json`, domain model tidak
4. **Satu ViewModel per screen** (atau per flow untuk multi-step wizard seperti Transfer)
5. **State SELALU dari `StateFlow`**, TIDAK pernah `LiveData` atau mutable state di Composable
6. **Navigasi via SideEffect**, TIDAK pernah dari Composable langsung
7. **UseCase = single public method** (`operator fun invoke(...)`)
8. **Repository interface di domain, implementation di data**

---

## 2. Response Envelope

Semua response dari server menggunakan format envelope yang sama:

```json
// Success
{
  "status": "success",
  "data": { ... },
  "pagination": {
    "cursor": "string",
    "has_more": true,
    "limit": 20
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2026-09-11T10:30:00Z"
  }
}

// Error
{
  "status": "error",
  "error": {
    "code": "AUTH_INVALID_PIN",
    "message": "Kode akses salah. Silakan coba lagi.",
    "details": null
  },
  "meta": {
    "request_id": "uuid",
    "timestamp": "2026-09-11T10:30:00Z"
  }
}
```

**Envelope DTO:**
```kotlin
@JsonClass(generateAdapter = true)
data class ApiEnvelope<T>(
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: T? = null,
    @Json(name = "error") val error: ApiError? = null,
    @Json(name = "pagination") val pagination: Pagination? = null,
    @Json(name = "meta") val meta: Meta? = null,
)

@JsonClass(generateAdapter = true)
data class ApiError(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String,
    @Json(name = "details") val details: Any? = null,
)

@JsonClass(generateAdapter = true)
data class Pagination(
    @Json(name = "cursor") val cursor: String,
    @Json(name = "has_more") val hasMore: Boolean,
    @Json(name = "limit") val limit: Int,
)
```

**Parsing rule:** Cek `status` field dulu. Jika `"error"`, baca `error.code` untuk routing logic. Jika `"success"`, parse `data` sesuai endpoint.

**Error mapping ke MVI:**
```kotlin
sealed class UiError(val code: String, val message: String) {
    data class InvalidPin(val remaining: Int? = null) : UiError("AUTH_INVALID_PIN", "Kode akses salah")
    data object AccountLocked : UiError("AUTH_ACCOUNT_LOCKED", "Akun terkunci")
    data object SessionExpired : UiError("AUTH_TOKEN_EXPIRED", "Sesi berakhir")
    data object InsufficientBalance : UiError("TRANSFER_INSUFFICIENT_BALANCE", "Saldo tidak mencukupi")
    data class Generic(val msg: String) : UiError("INTERNAL_ERROR", msg)
    // ... map setiap error code

    companion object {
        fun from(apiError: ApiError): UiError = when (apiError.code) {
            "AUTH_INVALID_PIN" -> InvalidPin()
            "AUTH_ACCOUNT_LOCKED" -> AccountLocked
            "AUTH_TOKEN_EXPIRED" -> SessionExpired
            "TRANSFER_INSUFFICIENT_BALANCE" -> InsufficientBalance
            else -> Generic(apiError.message)
        }
    }
}
```

---

## 3. PIN Encryption (RSA-2048 OAEP-SHA256)

PIN TIDAK PERNAH dikirim plaintext. Client harus mengenkripsi PIN menggunakan RSA public key dari server.

### Setup
1. Ambil `pin_public.pem` dari server/backend team
2. Simpan di Android assets (`assets/pin_public.pem`)
3. Inject via Hilt `@Provides`

### Implementasi (Hilt-injected)

```kotlin
class PinEncryptor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val publicKey: PublicKey by lazy {
        val pem = context.assets.open("pin_public.pem").bufferedReader().readText()
        val cleaned = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = Base64.decode(cleaned, Base64.NO_WRAP)
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
    }

    fun encrypt(pin: String): String {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.encodeToString(cipher.doFinal(pin.toByteArray()), Base64.NO_WRAP)
    }
}
```

### Field yang menggunakan PIN encrypted
| Endpoint | Field |
|----------|-------|
| `POST /auth/login/pin` | `pin_encrypted` |
| `POST /auth/pin/verify` | `pin_encrypted` |
| `POST /auth/pin/change` | `old_pin_encrypted`, `new_pin_encrypted` |
| `POST /registration/complete` | `pin_encrypted` |

---

## 4. Auth Token Management

### Token Types

| Token | Lifetime | Storage | Refresh |
|-------|----------|---------|---------|
| Access Token (JWT) | 15 menit | In-memory (`TokenManager`) | Via refresh token |
| Refresh Token (JWT) | 7 hari | `EncryptedSharedPreferences` | Re-login jika expired |
| Verification Token | 120 detik | ViewModel state saja | Re-verify PIN |
| Registration Token | Session-scoped | ViewModel state saja | Dari verify-otp |

### TokenManager (Hilt Singleton)

```kotlin
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "bca_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    @Volatile var accessToken: String? = null       // in-memory only
        private set

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        private set(value) = prefs.edit().putString("refresh_token", value).apply()

    fun saveTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    fun clear() {
        accessToken = null
        refreshToken = null
    }

    val isLoggedIn: Boolean get() = refreshToken != null
}
```

### Auto-Refresh (OkHttp Authenticator)

```kotlin
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApi: Lazy<AuthApi>,  // Lazy to break circular dep
) : Authenticator {
    private val lock = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) return null
        return runBlocking {
            lock.withLock {
                // Another thread may have already refreshed
                val current = tokenManager.accessToken
                if (current != null && response.request.header("Authorization")?.contains(current) == false) {
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $current")
                        .build()
                }
                val refresh = tokenManager.refreshToken ?: return@runBlocking null
                try {
                    val result = authApi.get().refreshToken(RefreshTokenRequest(refresh))
                    if (result.status == "success" && result.data != null) {
                        tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${result.data.accessToken}")
                            .build()
                    } else {
                        tokenManager.clear()
                        null
                    }
                } catch (e: Exception) {
                    tokenManager.clear()
                    null
                }
            }
        }
    }
}
```

---

## 5. Error Code Reference

| HTTP | Code | Message (ID) | MVI Action |
|------|------|-------------|------------|
| 400 | `VALIDATION_ERROR` | Permintaan tidak valid | `_state.update { copy(error = ...) }` |
| 400 | `QRIS_INVALID_PAYLOAD` | Data QR code tidak valid | State: error, allow rescan |
| 400 | `REGISTRATION_INVALID_DOCUMENT` | Format dokumen tidak valid | State: error, re-upload |
| 401 | `AUTH_INVALID_PIN` | Kode akses salah | State: error + remaining attempts |
| 401 | `AUTH_TOKEN_EXPIRED` | Sesi berakhir | Authenticator auto-refresh atau SideEffect: NavigateToLogin |
| 401 | `AUTH_TOKEN_INVALID` | Token tidak valid | SideEffect: NavigateToLogin |
| 401 | `AUTH_BIOMETRIC_NOT_REGISTERED` | Biometrik belum terdaftar | State: show register prompt |
| 401 | `VERIFICATION_TOKEN_INVALID` | Token verifikasi tidak valid | SideEffect: ReopenPinSheet |
| 401 | `REGISTRATION_TOKEN_INVALID` | Token registrasi tidak valid | SideEffect: RestartRegistration |
| 403 | `AUTH_DEVICE_NOT_RECOGNIZED` | Perangkat tidak dikenali | State: show device binding |
| 404 | `NOT_FOUND` | Resource tidak ditemukan | State: empty state |
| 404 | `ACCOUNT_NOT_FOUND` | Rekening tidak ditemukan | State: error |
| 404 | `TRANSFER_ACCOUNT_NOT_FOUND` | Rekening tujuan tidak ditemukan | State: error di form |
| 404 | `EWALLET_ACCOUNT_NOT_FOUND` | Akun e-wallet tidak ditemukan | State: error |
| 404 | `REGISTRATION_NOT_FOUND` | Pendaftaran tidak ditemukan | SideEffect: RestartRegistration |
| 409 | `IDEMPOTENCY_CONFLICT` | Transaksi sedang diproses | State: show "please wait" |
| 409 | `REGISTRATION_DUPLICATE` | NIK/phone sudah terdaftar | State: error + login link |
| 422 | `AUTH_OLD_PIN_MISMATCH` | PIN lama tidak sesuai | State: error di form |
| 422 | `INQUIRY_EXPIRED` | Sesi transaksi kedaluwarsa | SideEffect: NavigateBackToInquiry |
| 422 | `INQUIRY_MISMATCH` | Data transaksi tidak sesuai | SideEffect: NavigateBackToInquiry |
| 422 | `TRANSFER_INSUFFICIENT_BALANCE` | Saldo tidak mencukupi | State: error + balance |
| 422 | `TRANSFER_LIMIT_EXCEEDED` | Melebihi limit harian | State: error + limit info |
| 422 | `TRANSFER_SELF_TRANSFER` | Tidak dapat transfer ke sendiri | State: error |
| 422 | `EWALLET_INSUFFICIENT_BALANCE` | Saldo tidak mencukupi | State: error + balance |
| 422 | `QRIS_INSUFFICIENT_BALANCE` | Saldo tidak mencukupi | State: error + balance |
| 422 | `QRIS_LIMIT_EXCEEDED` | Melebihi limit QRIS | State: error + limit |
| 422 | `REGISTRATION_OTP_INVALID` | Kode OTP tidak valid | State: error, allow retry |
| 422 | `REGISTRATION_OTP_EXPIRED` | OTP kedaluwarsa | State: show resend button |
| 423 | `AUTH_ACCOUNT_LOCKED` | Akun terkunci | State: locked screen + timer |
| 429 | `RATE_LIMIT_EXCEEDED` | Terlalu banyak permintaan | State: error + retry countdown |
| 500 | `INTERNAL_ERROR` | Kesalahan sistem | State: generic error |
| 503 | `EWALLET_PROVIDER_DOWN` | Provider tidak tersedia | State: error + retry |
| 503 | `MAINTENANCE_MODE` | Sistem dalam pemeliharaan | SideEffect: NavigateToMaintenance |

---

## 6. Endpoint Reference — Public (No Auth)

### 6.1 Health Check

```http
GET /v1/health
→ 200 { "database": "ok", "redis_session": "ok", "redis_cache": "ok", "config": {...} }

GET /v1/health/live
→ 200 { "database": "ok", "redis_session": "ok", "redis_cache": "ok" }

GET /v1/health/config
→ 200 { "maintenance_mode": false, "min_app_version": "1.0.0", "feature_flags": {...} }
```

### 6.2 Login PIN

```http
POST /v1/auth/login/pin
```

**Request:**
```json
{
  "device_id": "unique-android-device-id",
  "pin_encrypted": "base64-rsa-oaep-sha256-encrypted-pin",
  "device_info": {
    "device_name": "Samsung Galaxy S24",
    "device_model": "SM-S928B",
    "os_version": "Android 15",
    "app_version": "1.0.0"
  }
}
```

**Response 200:**
```json
{
  "access_token": "eyJhbGciOi...",
  "refresh_token": "eyJhbGciOi...",
  "token_type": "Bearer",
  "expires_in": 900,
  "user": {
    "id": "uuid",
    "display_name": "NURHOLIS",
    "full_name": "NURHOLIS MAJID"
  }
}
```

**`device_id`:** Generate UUID sekali saat install, simpan di `EncryptedSharedPreferences`. JANGAN gunakan hardware ID.

### 6.3 Biometric Login (2-step)

**Step 1 — Get Challenge:**
```http
POST /v1/auth/biometric/challenge
{ "device_id": "unique-android-device-id" }

→ 200
{
  "challenge_id": "uuid",
  "challenge": "base64-32-random-bytes",
  "expires_in": 300
}
```

**Step 2 — Submit Signature:**
```http
POST /v1/auth/login/biometric
{
  "device_id": "unique-android-device-id",
  "key_id": "biometric-key-id-from-registration",
  "challenge_id": "uuid-from-step-1",
  "signature": "base64-signed-challenge"
}

→ 200 (same response as PIN login)
```

### 6.4 Refresh Token

```http
POST /v1/auth/token/refresh
{ "refresh_token": "eyJhbGciOi..." }

→ 200 (same response as login)
```

### 6.5 Registration (Buka Rekening)

**Step 1 — Initiate:**
```http
POST /v1/registration/initiate
{ "full_name": "JOHN DOE", "nik": "3201234567890001", "phone_number": "081234567890", "email": "john@example.com" }

→ 201
{ "registration_id": "uuid", "status": "OTP_PENDING", "otp_destination": "0812****7890" }
```

**Step 2 — Verify OTP:**
```http
POST /v1/registration/verify-otp
{ "registration_id": "uuid", "otp_code": "123456" }

→ 200
{ "registration_id": "uuid", "status": "OTP_VERIFIED", "registration_token": "jwt-token" }
```

**Step 3 — Upload Documents:**
```http
POST /v1/registration/upload-document
Authorization: Bearer <registration_token>
Content-Type: multipart/form-data
Form: document_type (KTP|SELFIE|KTP_SELFIE), file (JPEG/PNG, max 5MB)

→ 200 { "message": "Dokumen berhasil diupload.", "document_type": "KTP" }
```

**Step 4 — Complete:**
```http
POST /v1/registration/complete
Authorization: Bearer <registration_token>
{ "pin_encrypted": "base64...", "device_id": "unique-id", "device_model": "Samsung Galaxy S24" }

→ 201
{ "user_id": "uuid", "account_number": "1234567890", "status": "COMPLETED", "message": "Akun berhasil dibuat" }
```

---

## 7. Endpoint Reference — Protected (Butuh Access Token)

Semua endpoint di bawah memerlukan header: `Authorization: Bearer <access_token>`

### 7.1 Auth

```http
POST /v1/auth/logout
{ "all_devices": false }
→ 200 { "message": "Berhasil logout." }

POST /v1/auth/biometric/register
{ "key_id": "alias", "public_key": "PEM", "biometric_type": "FINGERPRINT", "attestation": "" }
→ 201 { "message": "Biometrik berhasil didaftarkan." }

POST /v1/auth/pin/verify
{ "pin_encrypted": "base64...", "purpose": "TRANSFER" }
→ 200 { "verification_token": "jwt", "expires_in": 120 }

POST /v1/auth/pin/change
{ "old_pin_encrypted": "base64...", "new_pin_encrypted": "base64..." }
→ 200 { "message": "Kode akses berhasil diubah." }
```

`purpose`: `TRANSFER` | `EWALLET_TOPUP` | `QRIS_PAYMENT` | `CHANGE_LIMIT` | `CHANGE_PIN`

### 7.2 Account

```http
GET /v1/account/profile → 200 { id, full_name, display_name, phone, email, last_login_at, accounts[] }
GET /v1/account/balance → 200 { accounts[]: { account_id, account_number, account_type, account_label, currency, balance, available_balance, hold_amount, is_primary } }
GET /v1/account/dashboard → 200 { user, primary_account, accounts[], unread_notification_count }
PUT /v1/account/profile { "email": "...", "otp_code": "..." } → 200
PUT /v1/account/settings { "biometric_enabled": true, ... } → 200
PUT /v1/account/transaction-limit { "limits": { "TRANSFER_INTERNAL": { "daily_limit": "100000000" } } } → 200
```

### 7.3 Notifications

```http
GET /v1/notifications?limit=20&cursor=<uuid>
→ 200 { notifications[]: { id, type, title, body, deep_link, is_read, read_at, metadata, created_at } }
// pagination: { cursor, has_more, limit }

PUT /v1/notifications/{id}/read → 200
PUT /v1/notifications/read-all → 200
```

### 7.4 Transactions

```http
GET /v1/transactions/mutations?account_id=<uuid>&limit=20&period=LAST_7_DAYS&cursor=<cursor>
→ 200 { mutations[]: { id, mutation_type, amount, balance_before, balance_after, description, detail, category, reference_number, transaction_date, transaction_time, created_at } }

GET /v1/transactions/history?limit=20&type=<filter>&cursor=<cursor>
→ 200 { transactions[]: { id, type, status, amount, admin_fee, total_amount, currency, reference_number, description, notes, destination_account, destination_name, destination_bank, created_at } }

GET /v1/transfer/recent
→ 200 { recent_transfers[]: { destination_account, destination_name, destination_bank, bank_code, transfer_type, transfer_count, last_transfer_at } }

GET /v1/transactions/{transaction_id}/receipt
→ 200 { transaction_id, type, status, date, time, reference_number, source_account, source_name, destination_account, destination_name, destination_bank, provider_name, amount, admin_fee, total, currency, notes }
```

---

## 8. Transaction Flows (Inquiry → PIN Verify → Execute)

Semua transaksi finansial mengikuti pola 3-langkah:

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│  1. Inquiry  │ →  │ 2. PIN Verify│ →  │  3. Execute  │
│  (5 min TTL) │    │  (120s TTL)  │    │ (idempotent) │
└─────────────┘    └──────────────┘    └─────────────┘
```

### 8.1 Transfer

```http
POST /v1/transfer/inquiry
{ "destination_account": "9876543210", "destination_bank": "BCA", "bank_code": "014", "transfer_type": "INTERNAL", "amount": 2500000, "notes": "Bayar makan siang" }
→ 200 { inquiry_id, destination_account, destination_name, destination_bank, bank_code, transfer_type, admin_fee, expires_in }

POST /v1/auth/pin/verify
{ "pin_encrypted": "base64...", "purpose": "TRANSFER" }
→ 200 { verification_token, expires_in: 120 }

POST /v1/transfer/execute
X-Idempotency-Key: <uuid-v4>
{ "inquiry_id": "uuid", "source_account_id": "uuid", "amount": 2500000, "notes": "...", "verification_token": "jwt" }
→ 201 { transaction_id, reference_number, status, amount, admin_fee, total, source: {account_number, name}, destination: {account_number, name, bank}, notes, created_at }
```

`transfer_type`: `INTERNAL` | `EXTERNAL` | `VIRTUAL_ACCOUNT`

### 8.2 E-Wallet Top-Up

```http
GET /v1/ewallet/providers
→ 200 { providers[]: { id, name, icon_url, is_active, min_amount, max_amount, admin_fee, preset_amounts[] } }

POST /v1/ewallet/inquiry
{ "provider_id": "gopay", "phone_number": "081234567890", "amount": 500000, "source_account_id": "uuid" }
→ 200 { inquiry_id, provider, destination_name, destination_phone, amount, admin_fee, total, source_account, expires_in }

POST /v1/ewallet/topup
X-Idempotency-Key: <uuid-v4>
{ "inquiry_id": "uuid", "verification_token": "jwt" }
→ 201 { transaction_id, reference_number, status, provider, destination_phone, destination_name, amount, admin_fee, total, source_account, source_name, created_at }
```

### 8.3 QRIS Payment

```http
POST /v1/qris/decode
{ "qr_data": "00020101021226...EMVCo..." }
→ 200 { merchant_name, merchant_city, amount, is_amount_fixed, qris_id, expires_at }

POST /v1/qris/pay
X-Idempotency-Key: <uuid-v4>
{ "qris_id": "...", "source_account_id": "uuid", "amount": 150000, "verification_token": "jwt" }
→ 201 { transaction_id, reference_number, status, merchant_name, merchant_city, amount, admin_fee, total, source_account, source_name, created_at }
```

**MVI flow untuk transaksi:**
```
Event.SubmitInquiry → State(loading) → UseCase → State(inquiryResult) → tampilkan konfirmasi
Event.ConfirmTransaction → SideEffect(OpenPinSheet)
Event.PinVerified(token) → State(loading) → UseCase(execute + idempotencyKey) → State(success) | State(error)
```

---

## 9. Idempotency Rules

| Rule | Detail |
|------|--------|
| Header | `X-Idempotency-Key: <uuid-v4>` |
| Required for | `/transfer/execute`, `/ewallet/topup`, `/qris/pay` |
| Generate | `UUID.randomUUID().toString()` per transaksi baru |
| Store | ViewModel state — generate saat inquiry success, clear saat execute success/permanent-failure |
| Retry-safe | Jika network timeout, kirim ulang dengan key yang SAMA |
| Replay indicator | Response header `X-Idempotent-Replayed: true` |
| TTL server | 24 jam |
| Scoped | Per user — key yang sama dari user berbeda tidak conflict |

---

## 10. Pagination (Cursor-based)

```kotlin
// UseCase returns
data class PaginatedResult<T>(
    val items: List<T>,
    val cursor: String?,
    val hasMore: Boolean,
)

// ViewModel state
data class MutasiUiState(
    val mutations: List<MutationItem> = emptyList(),
    val cursor: String? = null,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    // ...
)

// Event
sealed interface MutasiEvent {
    data object LoadInitial : MutasiEvent
    data object LoadMore : MutasiEvent    // triggered by LazyColumn reaching end
    data object Refresh : MutasiEvent
}
```

Saat `hasMore` = false, stop loading. Cursor is opaque — jangan parse isinya.

---

## 11. Seed Data untuk Development

Server menyediakan 3 user test (via `make seed`):

| User | PIN | Device ID | Account Number | Balance |
|------|-----|-----------|----------------|---------|
| NURHOLIS MAJID | `123456` | `device-nurholis-001` | `1234567890` | 50.000.000 |
| BUDI SANTOSO | `654321` | `device-budi-001` | `9876543210` | 25.000.000 |
| SITI RAHAYU | `111111` | `device-siti-001` | `5555666677` | 100.000.000 |

User NURHOLIS sudah memiliki: 6 mutations, 5 notifications, 5 e-wallet providers, 3 promotions aktif.

---

## 12. Screen-to-Endpoint Mapping

| Screen | Primary Endpoint | ViewModel | UseCase |
|--------|-----------------|-----------|---------|
| Splash | `GET /health/config` | SplashViewModel | CheckHealthUseCase |
| Login | `POST /auth/login/pin` | LoginViewModel | LoginUseCase |
| Beranda | `GET /account/dashboard` | HomeViewModel | GetDashboardUseCase |
| Saldo | `GET /account/balance` | BalanceViewModel | GetBalanceUseCase |
| Profil | `GET /account/profile` | ProfileViewModel | GetProfileUseCase |
| Mutasi | `GET /transactions/mutations` | MutasiViewModel | GetMutationsUseCase |
| Riwayat | `GET /transactions/history` | RiwayatViewModel | GetHistoryUseCase |
| Receipt | `GET /transactions/{id}/receipt` | ReceiptViewModel | GetReceiptUseCase |
| Transfer | inquiry → verify → execute | TransferViewModel | TransferInquiryUseCase, VerifyPinUseCase, ExecuteTransferUseCase |
| E-Wallet | providers → inquiry → topup | EWalletViewModel | GetProvidersUseCase, EWalletInquiryUseCase, ExecuteTopUpUseCase |
| QRIS | decode → verify → pay | QRISViewModel | DecodeQRISUseCase, VerifyPinUseCase, ExecuteQRISPayUseCase |
| Notifikasi | `GET /notifications` | NotificationViewModel | GetNotificationsUseCase |
| Registrasi | initiate → otp → upload → complete | RegistrationViewModel | InitiateRegUseCase, VerifyOtpUseCase, UploadDocUseCase, CompleteRegUseCase |
| Ganti PIN | `POST /auth/pin/change` | ChangePinViewModel | ChangePinUseCase |
| Ganti Limit | `PUT /account/transaction-limit` | LimitViewModel | UpdateLimitUseCase |

---

## 13. Security Checklist

- PIN public key di assets, bukan hardcoded string
- Access token di memory (`TokenManager.accessToken`), refresh token di `EncryptedSharedPreferences`
- Biometric keys di `AndroidKeyStore` (hardware-backed)
- Certificate pinning untuk production (OkHttp `CertificatePinner`)
- ProGuard/R8 untuk obfuscation
- Jangan log token, PIN, atau PII
- Network Security Config: cleartext hanya untuk `localhost` / `10.0.2.2`
- `FLAG_SECURE` pada screen login dan PIN
- Auto-logout setelah 15 menit inaktif
- Root detection (Play Integrity API)