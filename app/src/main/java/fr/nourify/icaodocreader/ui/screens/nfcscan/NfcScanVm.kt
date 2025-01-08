package fr.nourify.icaodocreader.ui.screens.nfcscan

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nourify.icaodocreader.domain.usecase.ReadIcaoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class NfcScanVm(
    private val readIcaoUseCase: ReadIcaoUseCase,
) : ViewModel() {
    private val _scanState: MutableStateFlow<NfcScanState> = MutableStateFlow(NfcScanState.Idle)
    val scanState: StateFlow<NfcScanState> = _scanState.asStateFlow()

    fun readNfcTag(isoDep: IsoDep) {
        _scanState.value = NfcScanState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            readIcaoUseCase.readNfcTag(isoDep).runCatching {
                onFailure {
                    _scanState.value = NfcScanState.Error(it.localizedMessage ?: "unknown error")
                }
                onSuccess { icaoData ->
                    _scanState.value =
                        NfcScanState.Success(
                            firstName = icaoData.firstName,
                            lastName = icaoData.lastName,
                            gender = icaoData.gender,
                            state = icaoData.state,
                            nationality = icaoData.nationality,
                            passiveAuth = icaoData.passiveAuthSuccess,
                            chipAuth = icaoData.chipAuthSucceeded,
                            photoBase64 = icaoData.photoBase64,
                            photoBitmap = icaoData.photoBitmap,
                        )
                }
            }
        }
    }

    fun resetState() {
        _scanState.value = NfcScanState.Idle
    }
}
