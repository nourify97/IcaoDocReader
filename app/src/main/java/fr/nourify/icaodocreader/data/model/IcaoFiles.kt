package fr.nourify.icaodocreader.data.model

import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File

data class IcaoFiles(
    val dg1File: DG1File, // MRZ data
    val dg2File: DG2File, // Facial image
    val dg14File: DG14File, // Security options for BAC/EAC
    val sodFile: SODFile, // Security Object Document
    val dg14Encoded: ByteArray,
)
