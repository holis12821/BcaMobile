package id.bca.bcamobile.ui.components

import android.Manifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Penilaian hasil permintaan izin video call.
 *
 * Satu jebakan yang diuji di sini: hasil launcher memuat Bluetooth juga, dan menilainya
 * dengan `values.all { it }` membuat nasabah yang menolak akses Bluetooth kehilangan
 * seluruh verifikasi identitasnya — padahal speaker perangkat tetap bekerja tanpa itu.
 */
class VideoCallPermissionsTest {

    @Test
    fun `kamera dan mikrofon diizinkan sudah cukup`() {
        val hasil = mapOf(
            Manifest.permission.CAMERA to true,
            Manifest.permission.RECORD_AUDIO to true,
        )

        assertTrue(hasil.hasRequiredVideoCallPermissions())
    }

    @Test
    fun `bluetooth ditolak tidak memblokir panggilan`() {
        val hasil = mapOf(
            Manifest.permission.CAMERA to true,
            Manifest.permission.RECORD_AUDIO to true,
            Manifest.permission.BLUETOOTH_CONNECT to false,
        )

        assertTrue("Bluetooth bukan prasyarat", hasil.hasRequiredVideoCallPermissions())
    }

    @Test
    fun `mikrofon ditolak memblokir panggilan`() {
        // Tanpa audio petugas tidak bisa mengajukan pertanyaan verifikasi, jadi panggilan
        // tidak ada gunanya diteruskan.
        val hasil = mapOf(
            Manifest.permission.CAMERA to true,
            Manifest.permission.RECORD_AUDIO to false,
            Manifest.permission.BLUETOOTH_CONNECT to true,
        )

        assertFalse(hasil.hasRequiredVideoCallPermissions())
    }

    @Test
    fun `kamera ditolak memblokir panggilan`() {
        val hasil = mapOf(
            Manifest.permission.CAMERA to false,
            Manifest.permission.RECORD_AUDIO to true,
        )

        assertFalse(hasil.hasRequiredVideoCallPermissions())
    }

    @Test
    fun `izin yang tidak dijawab sama dengan ditolak`() {
        // Dialog yang ditutup tanpa pilihan mengembalikan peta tanpa kuncinya.
        assertFalse(emptyMap<String, Boolean>().hasRequiredVideoCallPermissions())
        assertFalse(
            mapOf(Manifest.permission.CAMERA to true).hasRequiredVideoCallPermissions(),
        )
    }

    @Test
    fun `daftar wajib hanya kamera dan mikrofon`() {
        // Kalau Bluetooth pernah masuk ke daftar ini, seluruh gerbang izin berubah arti.
        assertTrue(
            VIDEO_CALL_PERMISSIONS.toSet() == setOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            ),
        )
    }
}
