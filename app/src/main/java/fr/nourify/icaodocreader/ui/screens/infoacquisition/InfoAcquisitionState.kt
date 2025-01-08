package fr.nourify.icaodocreader.ui.screens.infoacquisition

data class InfoAcquisitionState(
    val dob: Long?,
    val doe: Long?,
    val docNumber: String,
) {
    fun isAllFieldsFilled() = (dob != null && doe != null && docNumber.isNotBlank())

    companion object {
        fun toDefault() = InfoAcquisitionState(dob = null, doe = null, docNumber = "")
    }
}
