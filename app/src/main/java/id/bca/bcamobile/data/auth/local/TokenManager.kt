package id.bca.bcamobile.data.auth.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Penyimpanan token autentikasi.
 *
 * Access token **hanya di memory** — hilang saat proses mati, dipulihkan lewat
 * refresh token. Refresh token di EncryptedSharedPreferences.
 * Lihat `.claude/skills/bca-mobile-api/SKILL.md` §4.
 */
@Suppress("DEPRECATION")
@Singleton
class TokenManager @Inject constructor(
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

    @Volatile
    var accessToken: String? = null
        private set

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH, null)
        private set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_REFRESH) else putString(KEY_REFRESH, value)
        }.apply()

    fun saveTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    fun clear() {
        accessToken = null
        refreshToken = null
    }

    /** true bila masih ada refresh token; access token boleh kosong dan akan diperbarui. */
    val isLoggedIn: Boolean get() = refreshToken != null

    private companion object {
        const val FILE_NAME = "auth_tokens"
        const val KEY_REFRESH = "refresh_token"
    }
}
