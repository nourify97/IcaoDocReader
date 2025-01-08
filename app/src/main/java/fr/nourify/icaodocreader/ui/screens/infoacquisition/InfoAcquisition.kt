package fr.nourify.icaodocreader.ui.screens.infoacquisition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.nourify.icaodocreader.R
import fr.nourify.icaodocreader.ui.components.DatePickerFieldToModal

@Composable
fun DataAcquisition(
    modifier: Modifier = Modifier,
    isBtnEnabled: Boolean,
    docNumber: String,
    dob: Long?,
    doe: Long?,
    onDobSelected: (Long?) -> Unit,
    onDoeSelected: (Long?) -> Unit,
    onDocNumberChange: (String) -> Unit,
    onNavigateToNfcScan: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterVertically,
            ),
    ) {
        OutlinedTextField(
            value = docNumber,
            onValueChange = onDocNumberChange,
            label = { Text(stringResource(R.string.doc_number_label)) },
        )
        DatePickerFieldToModal(
            label = stringResource(R.string.dob_label),
            selectedDate = dob,
            onDateSelected = onDobSelected,
        )
        DatePickerFieldToModal(
            label = stringResource(R.string.doe_label),
            selectedDate = doe,
            onDateSelected = onDoeSelected,
        )
        Button(
            onClick = onNavigateToNfcScan,
            enabled = isBtnEnabled,
        ) {
            Text(stringResource(R.string.continue_button_label))
        }
    }
}

@Preview
@Composable
private fun DataAcquisitionPrev() {
    DataAcquisition(
        docNumber = "",
        isBtnEnabled = true,
        dob = null,
        doe = null,
        onDobSelected = {},
        onDoeSelected = {},
        onDocNumberChange = {},
        onNavigateToNfcScan = {},
    )
}
