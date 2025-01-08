package fr.nourify.icaodocreader.data.model

import android.graphics.Bitmap

data class IcaoResult(
    val files: IcaoFiles,
    val chipAuthSucceeded: Boolean = false,
    val passiveAuthSuccess: Boolean = false,
    val imageBase64: String? = null,
    val bitmap: Bitmap? = null,
)
