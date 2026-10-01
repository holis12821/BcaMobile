package id.bca.bcamobile.core.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menyediakan kunci publik PIN dengan urutan yang sudah ditetapkan
 * `.claude/skills/frontend-pin-encryption/SKILL.md` §1:
 *
 * ```
 * 1. cache memori (masih segar)  → pakai
 * 2. cache tersimpan (segar)     → pakai
 * 3. GET auth/pin/public-key     → pakai + simpan
 * 4. cache tersimpan (basi)      → pakai, lebih baik daripada gagal
 * 5. assets/pin_public.pem       → pakai, TIDAK disimpan sebagai cache
 * 6. tidak ada                   → null → pemanggil balas CLIENT_PIN_KEY_MISSING
 * ```
 *
 * Asset sengaja tidak pernah ditulis ke cache: ia cadangan tanpa `key_id`, dan
 * menyimpannya akan membuat kunci tanpa versi itu menang atas jawaban server
 * berikutnya.
 *
 * Penyimpanannya `SharedPreferences` biasa, bukan terenkripsi. Isinya kunci
 * **publik** — bukan rahasia — dan mengenkripsinya hanya menambah biaya
 * MasterKey di jalur yang dipakai tepat saat layar masuk dibuka.
 */
@Singleton
class PinKeyProvider @Inject constructor(
    private val context: Context,
    private val remoteSource: PinKeySource,
) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    /** Menjaga satu pengambilan jaringan saja saat beberapa pemanggil bersamaan. */
    private val mutex = Mutex()

    @Volatile
    private var cached: CachedKey? = null

    /**
     * @return kunci siap pakai, atau null bila tidak satu pun sumber menjawab.
     *   Null **wajib** diperlakukan sebagai kegagalan — bukan izin mengirim PIN polos.
     */
    suspend fun current(): PinKey? {
        memoryIfFresh()?.let { return it }

        return mutex.withLock {
            // Pemanggil lain mungkin sudah menyegarkan sementara kita menunggu.
            memoryIfFresh()?.let { return@withLock it }

            storedIfFresh()?.let { stored ->
                cached = stored
                return@withLock stored.key
            }

            remoteSource.fetch()?.let { fresh ->
                store(fresh)
                cached = CachedKey(fresh, System.currentTimeMillis())
                return@withLock fresh
            }

            // Server tidak terjangkau. Cache basi masih lebih baik daripada gagal:
            // kunci hanya salah kalau backend kebetulan baru merotasinya.
            stored()?.let { stale ->
                cached = stale
                return@withLock stale.key
            }

            assetKey()
        }
    }

    /**
     * Membuang kunci yang dipakai terakhir setelah server menolaknya
     * (`AUTH_PIN_KEY_UNKNOWN` / `CRED_DECRYPTION_FAILED`).
     *
     * Cache tersimpan ikut dihapus, bukan hanya yang di memori: tanpa itu
     * [current] akan mengambil kunci basi yang sama dari disk dan percobaan
     * ulangnya gagal dengan alasan yang persis sama.
     */
    fun invalidate() {
        cached = null
        prefs.edit().clear().apply()
    }

    private fun memoryIfFresh(): PinKey? = cached?.takeIf { it.isFresh() }?.key

    private fun storedIfFresh(): CachedKey? = stored()?.takeIf { it.isFresh() }

    private fun stored(): CachedKey? {
        val pem = prefs.getString(KEY_PEM, null)?.takeIf { it.isNotBlank() } ?: return null
        val fetchedAt = prefs.getLong(KEY_FETCHED_AT, 0L)
        if (fetchedAt <= 0L) return null
        return CachedKey(
            key = PinKey(pem = pem, keyId = prefs.getString(KEY_ID, null)),
            fetchedAt = fetchedAt,
        )
    }

    private fun store(key: PinKey) {
        prefs.edit()
            .putString(KEY_PEM, key.pem)
            .putString(KEY_ID, key.keyId)
            .putLong(KEY_FETCHED_AT, System.currentTimeMillis())
            .apply()
    }

    /** Cadangan yang ikut dalam APK. Tanpa `key_id` — versinya tidak diketahui. */
    private fun assetKey(): PinKey? = runCatching {
        context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
    }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?.let { PinKey(pem = it, keyId = null) }

    private data class CachedKey(val key: PinKey, val fetchedAt: Long) {
        /** Mengikuti `Cache-Control: max-age=300` pada endpoint. */
        fun isFresh(): Boolean = System.currentTimeMillis() - fetchedAt < TTL_MILLIS
    }

    private companion object {
        const val FILE_NAME = "pin_key_cache"
        const val KEY_PEM = "public_key_pem"
        const val KEY_ID = "key_id"
        const val KEY_FETCHED_AT = "fetched_at"
        const val ASSET_NAME = "pin_public.pem"
        const val TTL_MILLIS = 5 * 60 * 1000L
    }
}
