package id.bca.bcamobile.core.liveness

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LivenessFrameBufferTest {

    @Test
    fun `frame netral dan frame langkah terpisah`() {
        val buffer = LivenessFrameBuffer()
        buffer.put(CaptureSlot.Neutral, byteArrayOf(1, 2, 3), 10)
        buffer.put(CaptureSlot.Step(0, LivenessAction.BLINK), byteArrayOf(4, 5), 20)

        assertNotNull(buffer.neutralFrame())
        assertEquals(1, buffer.stepFrames().size)
        assertEquals(2, buffer.size)
    }

    @Test
    fun `slot yang sama ditimpa bukan ditumpuk`() {
        val buffer = LivenessFrameBuffer()
        buffer.put(CaptureSlot.Neutral, byteArrayOf(1, 1, 1), 10)
        buffer.put(CaptureSlot.Neutral, byteArrayOf(9, 9, 9), 20)

        assertEquals(1, buffer.size)
        assertArrayEquals(byteArrayOf(9, 9, 9), buffer.neutralFrame()?.jpeg)
    }

    @Test
    fun `isi buffer ditimpa nol saat dibersihkan`() {
        val buffer = LivenessFrameBuffer()
        val jpeg = byteArrayOf(7, 7, 7, 7)
        buffer.put(CaptureSlot.Neutral, jpeg, 10)

        buffer.clear()

        assertEquals(0, buffer.size)
        assertNull(buffer.neutralFrame())
        assertArrayEquals(
            "piksel wajah tidak boleh tertinggal di heap",
            byteArrayOf(0, 0, 0, 0),
            jpeg,
        )
    }

    /**
     * Percobaan kedua memakai aksi yang berbeda, jadi slotnya **tidak sama** —
     * `Step(0, BLINK)` bukan `Step(0, TURN_LEFT)`. Tanpa pembersihan di antara
     * percobaan, frame percobaan lama ikut menumpuk dan terkirim bersama yang baru.
     *
     * Ini yang membuat analyzer wajib membersihkan buffer saat tantangan baru
     * dimulai, bukan mengandalkan slot yang kebetulan tertimpa.
     */
    @Test
    fun `aksi berbeda di indeks sama adalah slot berbeda`() {
        val buffer = LivenessFrameBuffer()
        buffer.put(CaptureSlot.Step(0, LivenessAction.BLINK), byteArrayOf(1), 10)
        buffer.put(CaptureSlot.Step(0, LivenessAction.TURN_LEFT), byteArrayOf(2), 20)

        assertEquals(
            "frame percobaan lama akan ikut terkirim kalau buffer tidak dibersihkan",
            2,
            buffer.stepFrames().size,
        )
    }

    @Test
    fun `frame melebihi batas ditolak dan isinya ditimpa`() {
        val buffer = LivenessFrameBuffer(maxFrames = 1)
        buffer.put(CaptureSlot.Neutral, byteArrayOf(1, 1), 10)
        val rejected = byteArrayOf(2, 2)

        buffer.put(CaptureSlot.Step(0, LivenessAction.BLINK), rejected, 20)

        assertEquals(1, buffer.size)
        assertArrayEquals(byteArrayOf(0, 0), rejected)
    }
}
