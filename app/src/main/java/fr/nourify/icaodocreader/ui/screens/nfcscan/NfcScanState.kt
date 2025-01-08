package fr.nourify.icaodocreader.ui.screens.nfcscan

import android.graphics.Bitmap

sealed class NfcScanState {
    data object Idle : NfcScanState()

    data object Loading : NfcScanState()

    data class Success(
        val firstName: String,
        val lastName: String,
        val gender: String,
        val state: String,
        val nationality: String,
        val passiveAuth: Boolean,
        val chipAuth: Boolean,
        val photoBase64: String?,
        val photoBitmap: Bitmap?,
    ) : NfcScanState()

    data class Error(
        val msg: String,
    ) : NfcScanState()
}
