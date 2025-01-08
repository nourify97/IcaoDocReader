package fr.nourify.icaodocreader.ui.screens.infoacquisition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nourify.icaodocreader.domain.usecase.ReadIcaoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class InfoAcquisitionVm(
    private val readIcaoUseCase: ReadIcaoUseCase,
) : ViewModel() {
    private val _infoAcquisitionState = MutableStateFlow(InfoAcquisitionState.toDefault())
    val infoAcquisitionState = _infoAcquisitionState.asStateFlow()

    val isButtonEnabled =
        _infoAcquisitionState
            .map { state ->
                state.isAllFieldsFilled()
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

    fun onDocNumberChange(docNumber: String) {
        _infoAcquisitionState.value =
            _infoAcquisitionState.value.copy(
                docNumber = docNumber,
            )
    }

    fun onDobSelected(date: Long?) {
        _infoAcquisitionState.value =
            _infoAcquisitionState.value.copy(
                dob = date,
            )
    }

    fun onDoeSelected(date: Long?) {
        _infoAcquisitionState.value =
            _infoAcquisitionState.value.copy(
                doe = date,
            )
    }

    fun setFields() {
        readIcaoUseCase.setIcaoData(
            dob = _infoAcquisitionState.value.dob,
            doe = _infoAcquisitionState.value.doe,
            docNumber = _infoAcquisitionState.value.docNumber,
        )
    }
}
