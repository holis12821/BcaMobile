package id.bca.bcamobile.core.videocall

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Protokol signaling video call (`06-BUKA-REKENING-API-SPEC.md` §5b).
 *
 * Dua sifat yang paling penting di sini bukan soal pemetaan field, tapi soal apa yang
 * **tidak** boleh terjadi: pesan dengan `type` asing dan pesan cacat harus diabaikan,
 * bukan melempar. Keduanya terjadi di tengah panggilan yang sedang berjalan, dan satu
 * exception di sana memutus verifikasi identitas yang tidak bisa diulang murah.
 */
class SignalingMessageParserTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(text: String) = parseSignalingMessage(json, text)

    @Test
    fun `queue_update membawa posisi dan estimasi`() {
        val event = parse("""{"type":"queue_update","position":1,"estimated_wait_seconds":60}""")

        assertEquals(SignalingEvent.QueueUpdate(position = 1, estimatedWaitSeconds = 60), event)
    }

    @Test
    fun `agent_assigned membawa nama dan id pegawai`() {
        val event = parse(
            """{"type":"agent_assigned","agent":{"name":"Sarah Adisti","employee_id":"CS-1042","photo_url":"https://x"}}""",
        )

        assertEquals(
            SignalingEvent.AgentAssigned(name = "Sarah Adisti", employeeId = "CS-1042"),
            event,
        )
    }

    @Test
    fun `agent_assigned tanpa objek agent diabaikan`() {
        // Tanpa nama, tag petugas akan tampil kosong — lebih baik tidak tampil sama sekali.
        assertNull(parse("""{"type":"agent_assigned"}"""))
    }

    @Test
    fun `answer membawa sdp apa adanya`() {
        val event = parse("""{"type":"answer","sdp":"v=0\r\no=- 123"}""")

        assertEquals(SignalingEvent.Answer("v=0\r\no=- 123"), event)
    }

    @Test
    fun `ice_candidate memetakan ketiga field`() {
        val event = parse(
            """{"type":"ice_candidate","candidate":{"candidate":"candidate:1 1 udp","sdpMid":"0","sdpMLineIndex":0}}""",
        )

        assertEquals(
            SignalingEvent.IceCandidate(
                candidate = "candidate:1 1 udp",
                sdpMid = "0",
                sdpMLineIndex = 0,
            ),
            event,
        )
    }

    @Test
    fun `instruction kosong tidak menimpa instruksi sebelumnya`() {
        // Instruksi kosong yang diteruskan akan menghapus kalimat petugas dari layar.
        assertNull(parse("""{"type":"instruction","text":""}"""))
        assertEquals(
            SignalingEvent.Instruction("Mohon tunjukkan e-KTP asli Anda ke kamera"),
            parse("""{"type":"instruction","text":"Mohon tunjukkan e-KTP asli Anda ke kamera"}"""),
        )
    }

    @Test
    fun `call_ended membawa hasil, nama petugas, dan durasi`() {
        val event = parse(
            """{"type":"call_ended","result":"APPROVED","agent_name":"Sarah Adisti","duration_seconds":195}""",
        )

        assertEquals(
            SignalingEvent.CallEnded(
                result = "APPROVED",
                agentName = "Sarah Adisti",
                durationSeconds = 195,
            ),
            event,
        )
    }

    @Test
    fun `type yang belum dikenal aplikasi diabaikan, bukan melempar`() {
        // Backend bisa menambah jenis pesan tanpa rilis aplikasi. Kalau ini melempar,
        // penambahan satu pesan memutus setiap panggilan yang sedang berjalan.
        assertNull(parse("""{"type":"screen_share_offer","payload":{"a":1}}"""))
        assertNull(parse("""{"type":""}"""))
    }

    @Test
    fun `pesan cacat diabaikan, bukan melempar`() {
        assertNull(parse("bukan json"))
        assertNull(parse("{"))
        assertNull(parse(""))
        // `position` bertipe salah: deserialisasinya gagal, dan itu tidak boleh
        // menjatuhkan panggilan.
        assertNull(parse("""{"type":"queue_update","position":"satu"}"""))
    }

    @Test
    fun `field tambahan dari server tidak memecah pesan yang dikenal`() {
        val event = parse(
            """{"type":"queue_update","position":3,"estimated_wait_seconds":200,"field_baru":"abaikan"}""",
        )

        assertEquals(SignalingEvent.QueueUpdate(position = 3, estimatedWaitSeconds = 200), event)
    }

    @Test
    fun `error dari server jadi kegagalan dengan pesannya`() {
        val event = parse("""{"type":"error","message":"Antrean sudah ditutup."}""")

        assertEquals(SignalingEvent.Failed("Antrean sudah ditutup."), event)
    }
}
