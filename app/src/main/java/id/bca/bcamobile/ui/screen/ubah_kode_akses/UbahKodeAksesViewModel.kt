package id.bca.bcamobile.ui.screen.ubah_kode_akses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.bca.bcamobile.core.network.messageOrNull
import id.bca.bcamobile.domain.auth.AuthRepository
import id.bca.bcamobile.domain.common.DataResult
import id.bca.bcamobile.ui.screen.buka_rekening.common.CREDENTIAL_LENGTH
import id.bca.bcamobile.ui.screen.buka_rekening.common.isAllSameChar
import id.bca.bcamobile.ui.screen.buka_rekening.common.isSequential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ubah kode akses lewat `POST /auth/access-code/change`.
 *
 * Sebelumnya layar ini memanggil `auth/pin/change`, yang memindahkan PIN
 * transaksi — rahasia yang berbeda. Akibatnya kode akses login tidak pernah
 * benar-benar berubah meski layar menyatakan berhasil.
 *
 * Endpoint ini memakai access token, jadi alur ini hanya untuk nasabah yang sudah
 * masuk (Akun → Ubah Kode Akses). Tautan "Ganti Kode Akses" di layar Login butuh
 * verifikasi kartu ATM yang belum punya endpoint, jadi tidak diarahkan ke sini.
 *
 * Kode lama dan baru **tidak** ditulis ke disk maupun log; enkripsi RSA-nya terjadi
 * di dalam repository.
 */
@HiltViewModel
class UbahKodeAksesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UbahKodeAksesUiState())
    val uiState: StateFlow<UbahKodeAksesUiState> = _uiState.asStateFlow()

    fun onKodeLamaChanged(value: String) {
        val trimmed = value.take(CREDENTIAL_LENGTH)
        _uiState.update {
            it.copy(
                kodeLama = trimmed,
                error = null,
                isLanjutAktif = trimmed.length == CREDENTIAL_LENGTH,
            )
        }
    }

    fun onKodeBaruChanged(value: String) {
        _uiState.update { it.copy(kodeBaru = value.take(CREDENTIAL_LENGTH), error = null).revalidate() }
    }

    fun onKonfirmasiChanged(value: String) {
        _uiState.update {
            it.copy(konfirmasiKodeBaru = value.take(CREDENTIAL_LENGTH), error = null).revalidate()
        }
    }

    fun onToggleKodeLamaVisibility() =
        _uiState.update { it.copy(isKodeLamaVisible = !it.isKodeLamaVisible) }

    fun onToggleKodeBaruVisibility() =
        _uiState.update { it.copy(isKodeBaruVisible = !it.isKodeBaruVisible) }

    fun onToggleKonfirmasiVisibility() =
        _uiState.update { it.copy(isKonfirmasiVisible = !it.isKonfirmasiVisible) }

    fun onLanjutClick() {
        val state = _uiState.value
        when (state.langkah) {
            UbahKodeLangkah.KODE_LAMA -> {
                if (state.kodeLama.length != CREDENTIAL_LENGTH) return
                _uiState.update {
                    it.copy(langkah = UbahKodeLangkah.KODE_BARU, error = null).revalidate()
                }
            }

            UbahKodeLangkah.KODE_BARU -> submit()
            UbahKodeLangkah.BERHASIL -> Unit
        }
    }

    /** Back di langkah kedua kembali ke langkah pertama, bukan keluar dari alur. */
    fun onBackRequested(): Boolean {
        val state = _uiState.value
        if (state.langkah != UbahKodeLangkah.KODE_BARU) return false
        _uiState.update {
            it.copy(
                langkah = UbahKodeLangkah.KODE_LAMA,
                error = null,
                isLanjutAktif = it.kodeLama.length == CREDENTIAL_LENGTH,
            )
        }
        return true
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.isNewCodeValid()) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.changeAccessCode(
                oldCode = state.kodeLama,
                newCode = state.kodeBaru,
            )
            when (result) {
                // Kode akses dibuang dari state begitu server menerimanya.
                is DataResult.Success -> _uiState.value = UbahKodeAksesUiState(
                    langkah = UbahKodeLangkah.BERHASIL,
                    isLanjutAktif = true,
                )

                is DataResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = result.error.messageOrNull())
                }
            }
        }
    }
}

// -- Validasi --------------------------------------------------------------

/**
 * Aturan kode akses dipakai bersama dengan flow buka rekening supaya tidak ada dua
 * definisi "kode akses yang sah" di aplikasi ini.
 */
private fun UbahKodeAksesUiState.isNewCodeValid(): Boolean =
    kodeBaru.length == CREDENTIAL_LENGTH &&
        kodeBaru.all { it.isLetterOrDigit() } &&
        !kodeBaru.isSequential() &&
        !kodeBaru.isAllSameChar() &&
        kodeBaru == konfirmasiKodeBaru &&
        kodeBaru != kodeLama

private fun UbahKodeAksesUiState.revalidate(): UbahKodeAksesUiState = copy(
    isPanjangValid = kodeBaru.length == CREDENTIAL_LENGTH && kodeBaru.all { it.isLetterOrDigit() },
    isTidakBerurutan = kodeBaru.isNotEmpty() && !kodeBaru.isSequential(),
    isTidakBerulang = kodeBaru.isNotEmpty() && !kodeBaru.isAllSameChar(),
    isKonfirmasiCocok = kodeBaru.isNotEmpty() && kodeBaru == konfirmasiKodeBaru,
    isLanjutAktif = isNewCodeValid(),
)
