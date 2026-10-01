package id.bca.bcamobile.core.download

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menulis struk PDF ke folder Unduhan.
 *
 * Memakai MediaStore di API 29 ke atas supaya tidak butuh izin penyimpanan sama
 * sekali; di bawah itu `Environment.DIRECTORY_DOWNLOADS` masih boleh ditulis
 * aplikasi tanpa izin selama berkasnya dibuat oleh aplikasi sendiri.
 *
 * Nama berkas memakai nomor referensi, bukan nomor rekening atau nama nasabah:
 * nama berkas terlihat di aplikasi berkas mana pun dan di notifikasi unduhan.
 */
@Singleton
class ReceiptPdfSaver @Inject constructor(
    private val context: Context,
) {

    /** @return nama berkas yang tersimpan, atau null bila gagal menulis. */
    fun save(bytes: ByteArray, referenceNumber: String): String? {
        val fileName = "$FILE_PREFIX${referenceNumber.safeForFileName()}$FILE_EXTENSION"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(bytes, fileName)
        } else {
            saveToPublicDir(bytes, fileName)
        }
    }

    // Pemeriksaan versinya ada di [save]; anotasi ini yang membuat lint melihatnya,
    // karena ia tidak menelusuri cabang `if` melintasi pemanggilan fungsi.
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(bytes: ByteArray, fileName: String): String? = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_PDF)
            // IS_PENDING menahan berkas dari aplikasi lain sampai isinya utuh,
            // supaya tidak ada yang membuka PDF setengah tertulis.
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null

        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return null

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        fileName
    }.getOrNull()

    @Suppress("DEPRECATION")
    private fun saveToPublicDir(bytes: ByteArray, fileName: String): String? = runCatching {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists() && !dir.mkdirs()) return null
        File(dir, fileName).also { it.writeBytes(bytes) }
        fileName
    }.getOrNull()

    private companion object {
        const val FILE_PREFIX = "BCA-Struk-"
        const val FILE_EXTENSION = ".pdf"
        const val MIME_PDF = "application/pdf"
    }
}

/** Nomor referensi dari server sudah aman, tapi nama berkas tidak boleh menebak. */
private fun String.safeForFileName(): String =
    filter { it.isLetterOrDigit() || it == '-' || it == '_' }.ifBlank { "tanpa-referensi" }
