package fr.nourify.icaodocreader.domain.usecase

import android.nfc.tech.IsoDep
import android.util.Log
import fr.nourify.icaodocreader.data.icao.IcaoDoc
import fr.nourify.icaodocreader.data.utils.ICAO_FORMAT
import fr.nourify.icaodocreader.data.utils.US_FORMAT
import fr.nourify.icaodocreader.data.utils.toFormattedDate
import fr.nourify.icaodocreader.domain.model.IcaoData
import fr.nourify.icaodocreader.ui.screens.nfcscan.NfcScanVm
import org.jmrtd.BACKey
import org.koin.core.annotation.Single
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

@Single
class ReadIcaoUseCase(
    private val icaoDoc: IcaoDoc,
) {
    private var dob: String? = null
    private var doe: String? = null
    private var docNumber: String = ""

    fun setIcaoData(
        dob: Long?,
        doe: Long?,
        docNumber: String,
    ) {
        this.dob = convertDate(dob?.toFormattedDate())
        this.doe = convertDate(doe?.toFormattedDate())
        this.docNumber = docNumber.uppercase(Locale.getDefault())
    }

    suspend fun readNfcTag(isoDep: IsoDep): Result<IcaoData> {
        if (docNumber.isBlank() || dob.isNullOrBlank() || doe.isNullOrBlank()) {
            return Result.failure(Exception("Missing Icao doc data"))
        }

        return try {
            val bacKey = BACKey(docNumber, dob, doe)
            Result.success(icaoDoc.read(isoDep, bacKey))
        } catch (e: Exception) {
            return Result.failure(Exception(e.message ?: "Unknown error"))
        }
    }

    private fun convertDate(input: String?): String? {
        if (input == null) {
            return null
        }
        return try {
            SimpleDateFormat(ICAO_FORMAT, Locale.US).format(SimpleDateFormat(US_FORMAT, Locale.US).parse(input)!!)
        } catch (e: ParseException) {
            Log.w(NfcScanVm::class.java.simpleName, e)
            null
        }
    }
}
