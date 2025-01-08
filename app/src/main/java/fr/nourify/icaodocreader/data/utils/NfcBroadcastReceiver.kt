package fr.nourify.icaodocreader.data.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

const val INTENT_ACTION_NFC_READ = "fr.nourify.icaodocreader.data.utils.INTENT_ACTION_NFC_READ"

@Composable
fun NfcBroadcastReceiver(onSuccess: (Tag) -> Unit) {
    val context = LocalContext.current

    val currentOnSystemEvent by rememberUpdatedState(onSuccess)

    DisposableEffect(context) {
        val intentFilter = IntentFilter(INTENT_ACTION_NFC_READ)
        val broadcast =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {
                    intent
                        ?.getParcelableCompatibility(
                            NfcAdapter.EXTRA_TAG,
                            Tag::class.java,
                        )?.let { tag ->
                            currentOnSystemEvent(tag)
                        }
                }
            }

        ContextCompat.registerReceiver(
            context,
            broadcast,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }
}
