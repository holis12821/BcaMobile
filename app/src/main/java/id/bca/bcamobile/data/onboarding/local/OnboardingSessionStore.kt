package id.bca.bcamobile.data.onboarding.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID
import id.bca.bcamobile.core.device.DeviceIdProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Satu-satunya hal yang dipersistensi dari flow buka rekening: `session_id`,
 * idempotency key submit, dan device id. **Tidak boleh** menyimpan PII
 * (NIK, nama, alamat, foto) — aturan wajib #1 skill `buka-rekening-api`.
 *
 * Idempotency key sengaja ikut disimpan: kalau proses mati saat submit, retry
 * memakai key yang sama sehingga server tidak membuat rekening kedua.
 */
// EncryptedSharedPreferences ditandai deprecated di security-crypto 1.1.0 tanpa
// pengganti resmi dari AndroidX. Tetap dipakai karena isinya hanya identifier
// non-PII dan tetap lebih baik daripada SharedPreferences polos.
@Suppress("DEPRECATION")
@Singleton
class OnboardingSessionStore @Inject constructor(
    private val context: Context,
    private val deviceIdProvider: DeviceIdProvider,
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var sessionId: String?
        get() = prefs.getString(KEY_SESSION_ID, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_SESSION_ID) else putString(KEY_SESSION_ID, value)
        }.apply()

    /** Dibuat sekali per sesi lalu dipakai ulang untuk setiap retry submit. */
    fun idempotencyKey(): String =
        prefs.getString(KEY_IDEMPOTENCY, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_IDEMPOTENCY, it).apply()
        }

    /**
     * Identitas perangkat. Didelegasikan ke [DeviceIdProvider] supaya nilainya sama
     * dengan header `X-Device-ID` — server menolak kalau berbeda (spec 06 §0).
     */
    fun deviceId(): String = deviceIdProvider.deviceId()

    /** Hapus sesi dan idempotency key; device id sengaja dipertahankan. */
    fun clear() {
        prefs.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_IDEMPOTENCY)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "onboarding_session"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_IDEMPOTENCY = "idempotency_key"
    }
}
