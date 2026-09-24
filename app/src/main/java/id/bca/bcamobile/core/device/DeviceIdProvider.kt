package id.bca.bcamobile.core.device

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Satu-satunya sumber identitas perangkat.
 *
 * Nilai yang sama dipakai di dua tempat dan **harus** cocok: header `X-Device-ID`
 * pada setiap request, dan field `device_id` di body `POST /onboarding/sessions`.
 * Server menolak dengan `ONBOARDING_DEVICE_MISMATCH` kalau keduanya berbeda —
 * lihat `docs/backend/06-BUKA-REKENING-API-SPEC.md` §0.
 *
 * Jangan membuat generator device id kedua di mana pun.
 *
 * Catatan: nilainya berbasis `ANDROID_ID`, yang berubah saat aplikasi dipasang ulang
 * atau perangkat direset. Konsekuensinya draf onboarding lama tidak bisa dilanjutkan —
 * keputusan terbuka nomor 3 di spec 06 §0.
 */
@Suppress("DEPRECATION")
@Singleton
class DeviceIdProvider @Inject constructor(
    private val context: Context,
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

    fun deviceId(): String =
        prefs.getString(KEY_DEVICE_ID, null) ?: resolveAndroidId().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }

    private fun resolveAndroidId(): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
        return if (androidId.isNullOrBlank()) UUID.randomUUID().toString() else androidId
    }

    private companion object {
        const val FILE_NAME = "device_identity"
        const val KEY_DEVICE_ID = "device_id"
    }
}
