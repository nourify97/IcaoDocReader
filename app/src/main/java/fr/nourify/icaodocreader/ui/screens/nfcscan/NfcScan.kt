package fr.nourify.icaodocreader.ui.screens.nfcscan

import android.nfc.tech.IsoDep
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.nourify.icaodocreader.R
import fr.nourify.icaodocreader.data.utils.NfcBroadcastReceiver

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun NfcScan(
    modifier: Modifier = Modifier,
    scanState: NfcScanState,
    resetState: () -> Unit,
    readTag: (IsoDep) -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterVertically,
            ),
    ) {
        when (scanState) {
            NfcScanState.Idle -> {
                Text(stringResource(R.string.place_doc_on_terminal))
            }
            NfcScanState.Loading -> {
                Text(stringResource(R.string.reading_document_data))
                CircularProgressIndicator()
            }
            is NfcScanState.Success -> {
                IcaoDataDisplay(scanState)
            }
            is NfcScanState.Error -> {
                Text(text = scanState.msg)
            }
        }
    }

    NfcBroadcastReceiver { tag ->
        Log.d("NfcScan", tag.id.toHexString())

        if (tag.techList.contains("android.nfc.tech.IsoDep")) {
            resetState()
            readTag(IsoDep.get(tag))
        }
    }
}

@Composable
private fun IcaoDataDisplay(data: NfcScanState.Success) {
    data.photoBitmap?.asImageBitmap()?.let {
        Image(bitmap = it, contentDescription = null)
    }
    Text(stringResource(id = R.string.first_name_label, data.firstName))
    Text(stringResource(id = R.string.last_name_label, data.lastName))
    Text(stringResource(id = R.string.gender_label, data.gender))
    Text(stringResource(id = R.string.state_label, data.state))
    Text(stringResource(id = R.string.nationality_label, data.nationality))
    Text(stringResource(id = R.string.passive_auth_label, if (data.passiveAuth) "Pass" else "Failed"))
    Text(stringResource(id = R.string.chip_auth_label, if (data.chipAuth) "Pass" else "Failed"))
}

@Preview
@Composable
private fun NfcScanPrev() {
    NfcScan(
        readTag = {},
        resetState = {},
        scanState = NfcScanState.Loading,
    )
}
