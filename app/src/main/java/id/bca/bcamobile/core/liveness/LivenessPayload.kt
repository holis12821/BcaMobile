package id.bca.bcamobile.core.liveness

import java.security.MessageDigest

/**
 * Bentuk kanonis payload yang ditandatangani kunci perangkat.
 *
 * Tanda tangan harus menutupi **isi frame**, bukan hanya metadatanya: tanpa sidik
 * digest per frame, payload yang sah bisa dipasangkan ulang dengan frame lain dan
 * tanda tangannya tetap cocok.
 *
 * Formatnya sengaja berupa teks baris-per-baris dan bukan JSON: server harus bisa
 * merekonstruksi string yang **persis sama**, dan urutan kunci JSON tidak dijamin
 * oleh pustaka mana pun. Satu spasi berbeda berarti tanda tangan ditolak, jadi
 * bentuk ini wajib sama di kedua sisi — lihat `livenessSignedPayload` di
 * `bca-mobile-api/internal/domain/onboarding/liveness_challenge_service.go`
 * (fungsi `livenessSignedPayload`).
 *
 * Mengubah formatnya berarti menaikkan [VERSION] dan memperbarui kedua repo dalam
 * commit yang sama.
 */
object LivenessPayload {

    const val VERSION = "v1"

    fun build(
        challengeId: String,
        nonce: String,
        deviceId: String,
        neutralFrame: ByteArray,
        stepFrames: List<LivenessStepFrame>,
    ): String = buildString {
        appendLine(VERSION)
        appendLine("challenge_id=$challengeId")
        appendLine("nonce=$nonce")
        appendLine("device_id=$deviceId")
        appendLine("neutral=${sha256Hex(neutralFrame)}")
        // Diurutkan indeks, bukan urutan pengambilan: keduanya seharusnya sama, dan
        // kalau berbeda yang mengikat tanda tangan adalah urutan yang bisa diprediksi.
        stepFrames.sortedBy { it.index }.forEach { frame ->
            appendLine(
                "step=${frame.index}:${frame.action.name}:" +
                    "${frame.capturedAtMillis}:${sha256Hex(frame.jpeg)}",
            )
        }
    }

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}

/** Satu frame bukti per langkah tantangan, beserta kapan frame itu diambil. */
data class LivenessStepFrame(
    val index: Int,
    val action: LivenessAction,
    val capturedAtMillis: Long,
    val jpeg: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LivenessStepFrame) return false
        return index == other.index &&
            action == other.action &&
            capturedAtMillis == other.capturedAtMillis &&
            jpeg.contentEquals(other.jpeg)
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + action.hashCode()
        result = 31 * result + capturedAtMillis.hashCode()
        result = 31 * result + jpeg.contentHashCode()
        return result
    }
}
