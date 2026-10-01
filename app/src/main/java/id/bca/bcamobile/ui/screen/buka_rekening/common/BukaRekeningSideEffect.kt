package id.bca.bcamobile.ui.screen.buka_rekening.common

import id.bca.bcamobile.core.network.ErrorText
import id.bca.bcamobile.domain.onboarding.model.OnboardingStep

/**
 * Kejadian sekali pakai: navigasi dan pesan. Tidak boleh disimpan di state.
 *
 * Tetap satu untuk seluruh flow, bukan satu per layar: [AdvanceTo] dipancarkan
 * hampir semua langkah dan tujuannya ditentukan graph, bukan layar asalnya.
 */
sealed interface BukaRekeningSideEffect {

    /** Server sudah memindahkan flow ke [step]; navigasi mengikuti nilai ini, bukan tebakan lokal. */
    data class AdvanceTo(val step: OnboardingStep) : BukaRekeningSideEffect

    /** Sesi habis atau hilang — kembali ke awal flow dan bersihkan state. */
    data object RestartFlow : BukaRekeningSideEffect

    data class ShowMessage(val text: ErrorText) : BukaRekeningSideEffect

    data object ExitFlow : BukaRekeningSideEffect

    /** Foto e-KTP selesai diproses di perangkat; lanjut ke layar pratinjau hasil. */
    data object ShowCaptureResult : BukaRekeningSideEffect
}
