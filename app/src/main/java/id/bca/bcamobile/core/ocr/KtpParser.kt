package id.bca.bcamobile.core.ocr

import id.bca.bcamobile.domain.onboarding.model.KtpData
import id.bca.bcamobile.domain.onboarding.model.LocalKtpScan
import java.util.Locale

/**
 * Mengubah teks mentah hasil ML Kit menjadi [LocalKtpScan].
 *
 * Bukan library: format e-KTP Indonesia punya tata letak label-nilai yang khas,
 * dan hasil OCR-nya kotor — label sering salah baca satu-dua huruf, titik dua
 * kadang hilang, dan baris alamat terpecah. Parser ini dibuat toleran terhadap
 * itu, bukan menuntut teks bersih.
 */
object KtpParser {

    fun parse(rawText: String): LocalKtpScan {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val nik = findNik(lines)
        val nama = valueOf(lines, LABEL_NAMA)?.cleanName()
        val ttl = valueOf(lines, LABEL_TTL)
        val tempatLahir = ttl?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() }
        val tanggalLahir = ttl?.substringAfter(",", "")?.trim()?.toIsoDate()

        val jenisKelamin = valueOf(lines, LABEL_GENDER)
            ?.substringBefore(LABEL_GOL_DARAH, missingDelimiterValue = "")
            ?.trim()
            ?.normalizeGender()
            ?: nik?.genderFromNik()

        val alamat = valueOf(lines, LABEL_ALAMAT)
        val rtRw = valueOf(lines, LABEL_RT_RW)?.normalizeRtRw()
        val kelurahan = valueOf(lines, LABEL_KELURAHAN)
        val kecamatan = valueOf(lines, LABEL_KECAMATAN)
        val agama = valueOf(lines, LABEL_AGAMA)
        val statusPerkawinan = valueOf(lines, LABEL_STATUS)

        val provinsiIndex = lines.indexOfFirst { it.uppercase(ID).startsWith(HEADER_PROVINSI) }
        val provinsi = lines.getOrNull(provinsiIndex)
            ?.drop(HEADER_PROVINSI.length)?.trim()
        val kota = findKota(lines, provinsiIndex)

        val data = KtpData(
            nik = nik.orEmpty(),
            namaLengkap = nama.orEmpty(),
            tempatLahir = tempatLahir.orEmpty(),
            tanggalLahir = tanggalLahir.orEmpty(),
            jenisKelamin = jenisKelamin.orEmpty(),
            alamat = alamat.orEmpty(),
            rtRw = rtRw.orEmpty(),
            kelurahan = kelurahan.orEmpty(),
            kecamatan = kecamatan.orEmpty(),
            kota = kota.orEmpty(),
            provinsi = provinsi.orEmpty(),
            agama = agama.orEmpty(),
            statusPerkawinan = statusPerkawinan.orEmpty(),
        )

        val missing = REQUIRED_FIELDS.filter { (_, selector) -> selector(data).isBlank() }
            .map { it.first }
        val accuracy = (REQUIRED_FIELDS.size - missing.size) * PERCENT / REQUIRED_FIELDS.size

        return LocalKtpScan(
            data = data,
            accuracyPercent = accuracy,
            missingFields = missing,
        )
    }

    /**
     * NIK e-KTP selalu 16 digit; label bisa salah baca, deretan angkanya tidak.
     *
     * Dicari pada teks asli lebih dulu. Membuang seluruh non-digit tidak aman:
     * label yang salah baca seperti `N1K` akan menempelkan angkanya ke depan NIK
     * dan menggeser 16 digit yang diambil. Penghapusan hanya dilakukan untuk
     * pemisah yang memang biasa disisipkan OCR di tengah angka.
     */
    private fun findNik(lines: List<String>): String? {
        lines.firstNotNullOfOrNull { NIK_PATTERN.find(it)?.value }?.let { return it }
        return lines.firstNotNullOfOrNull { line ->
            NIK_PATTERN.find(line.replace(NUMBER_SEPARATOR, ""))?.value
        }
    }

    /**
     * Kota/kabupaten ada di baris kedua kop e-KTP.
     *
     * Awalan `KOTA` atau `KABUPATEN` dipakai bila ada, tapi DKI Jakarta
     * mencetaknya tanpa awalan — jadi baris tepat setelah `PROVINSI` ikut
     * diterima selama bukan baris berlabel.
     */
    private fun findKota(lines: List<String>, provinsiIndex: Int): String? {
        lines.firstOrNull {
            val upper = it.uppercase(ID)
            upper.startsWith(HEADER_KOTA) || upper.startsWith(HEADER_KABUPATEN)
        }?.let { return it.substringAfter(" ").trim() }

        if (provinsiIndex < 0) return null
        val next = lines.getOrNull(provinsiIndex + 1)?.trim() ?: return null
        return next.takeIf { it.isNotBlank() && !it.contains(":") && !it.any(Char::isDigit) }
    }

    /**
     * Mengambil nilai setelah label.
     *
     * Titik dua sering hilang di hasil OCR, jadi kalau tidak ada pemisah, sisa
     * teks setelah label yang diambil.
     */
    private fun valueOf(lines: List<String>, labels: List<String>): String? {
        for (line in lines) {
            val upper = line.uppercase(ID)
            val label = labels.firstOrNull { upper.contains(it) } ?: continue
            val afterColon = line.substringAfter(":", missingDelimiterValue = "").trim()
            if (afterColon.isNotBlank()) return afterColon
            val afterLabel = line.drop(upper.indexOf(label) + label.length)
                .trimStart(' ', ':', '.', '-')
                .trim()
            if (afterLabel.isNotBlank()) return afterLabel
        }
        return null
    }

    /** `21-04-1995` dan `21/04/1995` menjadi `1995-04-21`. */
    private fun String.toIsoDate(): String? {
        val match = DATE_PATTERN.find(this) ?: return null
        val (day, month, year) = match.destructured
        return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
    }

    /** OCR kerap menyelipkan angka dan tanda baca di baris nama. */
    private fun String.cleanName(): String =
        filter { it.isLetter() || it == ' ' || it == '\'' || it == '.' }
            .replace(MULTI_SPACE, " ")
            .trim()

    private fun String.normalizeGender(): String? = when {
        uppercase(ID).contains(GENDER_LAKI) -> WIRE_LAKI_LAKI
        uppercase(ID).contains(GENDER_PEREMPUAN) -> WIRE_PEREMPUAN
        else -> null
    }

    /**
     * Digit tanggal pada NIK ditambah 40 untuk perempuan.
     * Dipakai hanya saat baris jenis kelamin gagal terbaca.
     */
    private fun String.genderFromNik(): String? {
        if (length != NIK_LENGTH) return null
        val birthDay = substring(NIK_DAY_START, NIK_DAY_END).toIntOrNull() ?: return null
        return if (birthDay > FEMALE_DAY_OFFSET) WIRE_PEREMPUAN else WIRE_LAKI_LAKI
    }

    /** `004002`, `004 / 002`, dan `004/002` disamakan jadi `004/002`. */
    private fun String.normalizeRtRw(): String? {
        val digits = filter { it.isDigit() }
        if (digits.length != RT_RW_DIGITS) return replace(" ", "").takeIf { it.isNotBlank() }
        return "${digits.take(RT_LENGTH)}/${digits.drop(RT_LENGTH)}"
    }

    /** `3174082104950001` menjadi `3174********0001` — satu-satunya bentuk NIK yang boleh di-log. */
    fun maskNik(nik: String): String {
        if (nik.length != NIK_LENGTH) return "*".repeat(nik.length)
        return nik.take(NIK_VISIBLE) + "*".repeat(NIK_LENGTH - 2 * NIK_VISIBLE) +
            nik.takeLast(NIK_VISIBLE)
    }

    /** Field yang dipakai menghitung akurasi. Semua wajib ada di payload data pribadi. */
    private val REQUIRED_FIELDS: List<Pair<String, (KtpData) -> String>> = listOf(
        "nik" to { it.nik },
        "nama_lengkap" to { it.namaLengkap },
        "tempat_lahir" to { it.tempatLahir },
        "tanggal_lahir" to { it.tanggalLahir },
        "jenis_kelamin" to { it.jenisKelamin },
        "alamat" to { it.alamat },
        "rt_rw" to { it.rtRw },
        "kelurahan" to { it.kelurahan },
        "kecamatan" to { it.kecamatan },
        "kota" to { it.kota },
        "provinsi" to { it.provinsi },
        "agama" to { it.agama },
        "status_perkawinan" to { it.statusPerkawinan },
    )

    private val ID = Locale.forLanguageTag("id-ID")
    private val NIK_PATTERN = Regex("""\d{16}""")
    private val DATE_PATTERN = Regex("""(\d{1,2})[-/. ](\d{1,2})[-/. ](\d{4})""")
    private val MULTI_SPACE = Regex("""\s+""")
    private val NUMBER_SEPARATOR = Regex("""[ .\-]""")

    private val LABEL_NAMA = listOf("NAMA")
    private val LABEL_TTL = listOf("TEMPAT/TGL LAHIR", "TEMPAT / TGL LAHIR", "TGL LAHIR", "LAHIR")
    private val LABEL_GENDER = listOf("JENIS KELAMIN", "KELAMIN")
    private val LABEL_ALAMAT = listOf("ALAMAT")
    private val LABEL_RT_RW = listOf("RT/RW", "RT / RW", "RTRW")
    private val LABEL_KELURAHAN = listOf("KEL/DESA", "KEL / DESA", "KELURAHAN", "DESA")
    private val LABEL_KECAMATAN = listOf("KECAMATAN")
    private val LABEL_AGAMA = listOf("AGAMA")
    private val LABEL_STATUS = listOf("STATUS PERKAWINAN", "PERKAWINAN")

    private const val LABEL_GOL_DARAH = "Gol"
    private const val HEADER_PROVINSI = "PROVINSI"
    private const val HEADER_KOTA = "KOTA"
    private const val HEADER_KABUPATEN = "KABUPATEN"
    private const val GENDER_LAKI = "LAKI"
    private const val GENDER_PEREMPUAN = "PEREMPUAN"
    private const val WIRE_LAKI_LAKI = "LAKI_LAKI"
    private const val WIRE_PEREMPUAN = "PEREMPUAN"
    private const val NIK_LENGTH = 16
    private const val NIK_VISIBLE = 4
    private const val NIK_DAY_START = 6
    private const val NIK_DAY_END = 8
    private const val FEMALE_DAY_OFFSET = 40
    private const val RT_RW_DIGITS = 6
    private const val RT_LENGTH = 3
    private const val PERCENT = 100.0
}
