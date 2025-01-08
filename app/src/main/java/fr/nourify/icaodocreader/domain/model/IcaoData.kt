package fr.nourify.icaodocreader.domain.model

import android.graphics.Bitmap

data class IcaoData(
    val firstName: String,
    val lastName: String,
    val gender: String,
    val state: String,
    val nationality: String,
    val passiveAuthSuccess: Boolean,
    val chipAuthSucceeded: Boolean,
    val photoBase64: String?,
    val photoBitmap: Bitmap?,
)
