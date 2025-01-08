package fr.nourify.icaodocreader

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import fr.nourify.icaodocreader.data.utils.INTENT_ACTION_NFC_READ
import fr.nourify.icaodocreader.data.utils.getParcelableCompatibility
import fr.nourify.icaodocreader.ui.navigation.Routes
import fr.nourify.icaodocreader.ui.navigation.SetupNavGraph
import fr.nourify.icaodocreader.ui.theme.IcaoDocReaderTheme

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            IcaoDocReaderTheme {
                SetupNavGraph(
                    startDestination = Routes.DATA_ACQUISITION_SCREEN,
                    navController = navController,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let { nfcIntent ->
            sendBroadcast(
                Intent(INTENT_ACTION_NFC_READ).apply {
                    putExtra(
                        NfcAdapter.EXTRA_TAG,
                        nfcIntent.getParcelableCompatibility(NfcAdapter.EXTRA_TAG, Tag::class.java),
                    )
                    setPackage(packageName)
                },
            )
        }
    }

    private fun enableNfcForegroundDispatch() {
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))

                val pendingIntent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.getActivity(
                            this,
                            0,
                            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            PendingIntent.FLAG_MUTABLE,
                        )
                    } else {
                        PendingIntent.getActivity(
                            this,
                            0,
                            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            PendingIntent.FLAG_UPDATE_CURRENT,
                        )
                    }
                adapter.enableForegroundDispatch(this, pendingIntent, null, filter)
            }
        }
    }

    private fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }
}
