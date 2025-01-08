package fr.nourify.icaodocreader.data.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val US_FORMAT = "yyyy-MM-dd"
const val ICAO_FORMAT = "yyMMdd"

fun Long.toFormattedDate(): String {
    val formatter = SimpleDateFormat(US_FORMAT, Locale.getDefault())
    return formatter.format(Date(this))
}
