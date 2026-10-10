package id.bca.bcamobile.core.liveness

/** Satu frame bukti beserta slot yang memintanya. */
data class LivenessFrame(
    val slot: CaptureSlot,
    val jpeg: ByteArray,
    val capturedAtMillis: Long,
) {
    // equals/hashCode bawaan data class membandingkan ByteArray lewat identitas;
    // dibuat eksplisit supaya perbandingan di test tidak menyesatkan.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LivenessFrame) return false
        return slot == other.slot &&
            capturedAtMillis == other.capturedAtMillis &&
            jpeg.contentEquals(other.jpeg)
    }

    override fun hashCode(): Int {
        var result = slot.hashCode()
        result = 31 * result + jpeg.contentHashCode()
        result = 31 * result + capturedAtMillis.hashCode()
        return result
    }
}

/**
 * Penampung frame bukti liveness — **hanya di memori**.
 *
 * Penampung sebelumnya menulis setiap frame jadi JPEG di `cacheDir` lalu berusaha
 * menghapusnya kembali. Begitu frame menyentuh disk, penghapusan menjadi janji,
 * bukan jaminan: proses yang mati di tengah jalan, backup aplikasi, dan pembaca
 * cache pihak ketiga semuanya menang melawan `File.delete()`. Foto wajah tidak
 * boleh ditulis ke disk sama sekali (aturan PII #5 dan §"No face images on disk"
 * di keputusan Phase 2).
 *
 * Bukan thread-safe dengan sendirinya; pemanggilnya (analyzer) sudah menjamin
 * hanya satu frame diproses pada satu waktu.
 */
class LivenessFrameBuffer(private val maxFrames: Int = DEFAULT_MAX_FRAMES) {

    private val frames = mutableListOf<LivenessFrame>()

    val size: Int get() = frames.size

    /**
     * Menyimpan satu frame bukti. Slot yang sama ditimpa, bukan ditumpuk — frame
     * netral terbaik dan satu frame per langkah, tidak lebih.
     */
    fun put(slot: CaptureSlot, jpeg: ByteArray, capturedAtMillis: Long) {
        val replaced = frames.indexOfFirst { it.slot == slot }
        val frame = LivenessFrame(slot, jpeg, capturedAtMillis)
        if (replaced >= 0) {
            wipe(frames[replaced].jpeg)
            frames[replaced] = frame
            return
        }
        if (frames.size >= maxFrames) {
            // Jumlah frame ditentukan jumlah langkah tantangan; kelebihan berarti ada
            // yang salah di pemanggil, dan membuang yang baru lebih aman daripada
            // membuang bukti langkah yang sudah sah.
            wipe(jpeg)
            return
        }
        frames += frame
    }

    /** Frame dalam urutan pengambilan: netral lebih dulu, lalu per langkah. */
    fun snapshot(): List<LivenessFrame> = frames.toList()

    fun stepFrames(): List<LivenessFrame> = frames.filter { it.slot is CaptureSlot.Step }

    fun neutralFrame(): LivenessFrame? = frames.firstOrNull { it.slot is CaptureSlot.Neutral }

    /**
     * Menimpa isi setiap buffer dengan nol lalu melepas acuannya.
     *
     * Menimpa dulu, bukan sekadar `clear()`: array yang hanya dilepas tetap memegang
     * piksel wajah sampai GC memutuskan menyentuhnya, dan sampai saat itu isinya ikut
     * dalam heap dump mana pun.
     */
    fun clear() {
        frames.forEach { wipe(it.jpeg) }
        frames.clear()
    }

    private fun wipe(bytes: ByteArray) = bytes.fill(0)

    private companion object {
        /** Satu frame netral + maksimum lima langkah dari daftar aksi Q5. */
        const val DEFAULT_MAX_FRAMES = 6
    }
}
