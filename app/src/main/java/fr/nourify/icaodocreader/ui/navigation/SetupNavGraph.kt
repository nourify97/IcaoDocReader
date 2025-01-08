package fr.nourify.icaodocreader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import fr.nourify.icaodocreader.ui.screens.infoacquisition.DataAcquisition
import fr.nourify.icaodocreader.ui.screens.infoacquisition.InfoAcquisitionVm
import fr.nourify.icaodocreader.ui.screens.nfcscan.NfcScan
import fr.nourify.icaodocreader.ui.screens.nfcscan.NfcScanVm
import org.koin.androidx.compose.koinViewModel

@Composable
fun SetupNavGraph(
    startDestination: String,
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(route = Routes.DATA_ACQUISITION_SCREEN) {
            val viewModel: InfoAcquisitionVm = koinViewModel()
            val screenState by viewModel.infoAcquisitionState.collectAsStateWithLifecycle()
            val canNavigate by viewModel.isButtonEnabled.collectAsStateWithLifecycle()

            DataAcquisition(
                modifier = Modifier,
                isBtnEnabled = canNavigate,
                docNumber = screenState.docNumber,
                dob = screenState.dob,
                doe = screenState.doe,
                onDobSelected = { viewModel.onDobSelected(it) },
                onDoeSelected = { viewModel.onDoeSelected(it) },
                onDocNumberChange = { viewModel.onDocNumberChange(it) },
                onNavigateToNfcScan = {
                    viewModel.setFields()
                    navController.navigate(Routes.NFC_SCAN_SCREEN)
                },
            )
        }

        composable(route = Routes.NFC_SCAN_SCREEN) {
            val viewModel: NfcScanVm = koinViewModel()
            val screenState by viewModel.scanState.collectAsStateWithLifecycle()

            NfcScan(
                modifier = Modifier,
                scanState = screenState,
                resetState = viewModel::resetState,
                readTag = { viewModel.readNfcTag(it) },
            )
        }

        composable(route = Routes.RESULT_SCREEN) {
        }
    }
}
