package fr.nourify.icaodocreader.data.icao

import android.content.Context
import android.graphics.Bitmap
import android.nfc.tech.IsoDep
import android.util.Base64
import android.util.Log
import fr.nourify.icaodocreader.data.model.IcaoFiles
import fr.nourify.icaodocreader.data.model.IcaoResult
import fr.nourify.icaodocreader.data.utils.ImageUtil.decodeImage
import fr.nourify.icaodocreader.domain.model.IcaoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sf.scuba.smartcards.CardService
import org.apache.commons.io.IOUtils
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1Set
import org.bouncycastle.asn1.x509.Certificate
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.icao.DG3File
import org.koin.core.annotation.Single
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertPathValidator
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.util.Arrays

@Single
class IcaoDoc(
    private val context: Context,
) {
    suspend fun read(
        isoDep: IsoDep,
        bacKey: BACKeySpec,
    ): IcaoData =
        withContext(Dispatchers.IO) {
            val state = decode(isoDep, bacKey)
            val mrzInfo = state.files.dg1File.mrzInfo

            return@withContext IcaoData(
                firstName = mrzInfo.secondaryIdentifier.replace("<", " "),
                lastName = mrzInfo.primaryIdentifier.replace("<", " "),
                gender = mrzInfo.gender.toString(),
                state = mrzInfo.issuingState,
                nationality = mrzInfo.nationality,
                passiveAuthSuccess = state.passiveAuthSuccess,
                chipAuthSucceeded = state.chipAuthSucceeded,
                photoBase64 = state.imageBase64,
                photoBitmap = state.bitmap,
            )
        }

    private suspend fun decode(
        isoDep: IsoDep,
        bacKey: BACKeySpec,
    ): IcaoResult {
        try {
            val service = setupIcaoService(isoDep, bacKey)

            // Read all files
            val files = readIcaoFiles(service)

            // Perform authentication
            val chipAuthSucceeded = doChipAuth(service, files)
            val passiveAuthSuccess = doPassiveAuth(files, chipAuthSucceeded)

            // Process face image
            val (faceImageBase64, faceBitmap) = processFaceImage(files.dg2File)

            // Process fingerprints
            // val (fingerImageBase64, fingerBitmap) = processFingerprints(files.dg3File)

            return IcaoResult(
                files = files,
                chipAuthSucceeded = chipAuthSucceeded,
                passiveAuthSuccess = passiveAuthSuccess,
                imageBase64 = faceImageBase64,
                bitmap = faceBitmap,
            )
        } catch (e: Exception) {
            e.message?.let { Log.e(this.javaClass.name, it) }
            throw e
        }
    }

    private fun setupIcaoService(
        isoDep: IsoDep,
        bacKey: BACKeySpec,
    ): PassportService {
        isoDep.timeout = 10000
        val cardService = CardService.getInstance(isoDep)
        cardService.open()

        val service =
            PassportService(
                cardService,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                false,
                false,
            )
        service.open()

        handlePACEAndBAC(service, bacKey)

        return service
    }

    private fun handlePACEAndBAC(
        service: PassportService,
        bacKey: BACKeySpec,
    ) {
        var paceSucceeded = false
        try {
            val cardAccessFile = CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
            for (securityInfo in cardAccessFile.securityInfos) {
                if (securityInfo is PACEInfo) {
                    service.doPACE(
                        bacKey,
                        securityInfo.objectIdentifier,
                        PACEInfo.toParameterSpec(securityInfo.parameterId),
                        null,
                    )
                    paceSucceeded = true
                }
            }
        } catch (e: Exception) {
            Log.w(this.javaClass.name, e)
        }

        service.sendSelectApplet(paceSucceeded)
        if (!paceSucceeded) {
            try {
                service.getInputStream(PassportService.EF_COM).read()
            } catch (e: Exception) {
                service.doBAC(bacKey)
            }
        }
    }

    private fun readIcaoFiles(service: PassportService): IcaoFiles {
        val dg1File = service.getInputStream(PassportService.EF_DG1).use { DG1File(it) }
        val dg2File = service.getInputStream(PassportService.EF_DG2).use { DG2File(it) }
        val sodFile = service.getInputStream(PassportService.EF_SOD).use { SODFile(it) }

        val dg14In = service.getInputStream(PassportService.EF_DG14)
        val dg14Encoded = IOUtils.toByteArray(dg14In)
        val dg14File = ByteArrayInputStream(dg14Encoded).use { DG14File(it) }

        return IcaoFiles(dg1File, dg2File, dg14File, sodFile, dg14Encoded)
    }

    private fun doChipAuth(
        service: PassportService,
        files: IcaoFiles,
    ): Boolean {
        try {
            for (securityInfo in files.dg14File.securityInfos) {
                if (securityInfo is ChipAuthenticationPublicKeyInfo) {
                    service.doEACCA(
                        securityInfo.keyId,
                        ChipAuthenticationPublicKeyInfo.ID_CA_ECDH_AES_CBC_CMAC_256,
                        securityInfo.objectIdentifier,
                        securityInfo.subjectPublicKey,
                    )
                    return true
                }
            }
        } catch (e: Exception) {
            Log.w(this.javaClass.name, e)
        }
        return false
    }

    private suspend fun processFaceImage(dg2File: DG2File): Pair<String?, Bitmap?> {
        var imageBase64: String? = null
        var bitmap: Bitmap? = null

        val allFaceImageInfo = dg2File.faceInfos.flatMap { it.faceImageInfos }

        if (allFaceImageInfo.isNotEmpty()) {
            val faceImageInfo = allFaceImageInfo.first()
            val imageLength = faceImageInfo.imageLength
            val dataInputStream = DataInputStream(faceImageInfo.imageInputStream)
            val buffer = ByteArray(imageLength)

            withContext(Dispatchers.IO) {
                dataInputStream.readFully(buffer, 0, imageLength)
            }

            val inputStream = ByteArrayInputStream(buffer, 0, imageLength)
            bitmap = decodeImage(context, faceImageInfo.mimeType, inputStream)
            imageBase64 = Base64.encodeToString(buffer, Base64.DEFAULT)
        }

        return Pair(imageBase64, bitmap)
    }

    private suspend fun processFingerprints(dg3File: DG3File): Pair<String?, Bitmap?> {
        var imageBase64: String? = null
        var bitmap: Bitmap? = null

        val allFingerImageInfo = dg3File.fingerInfos.flatMap { it.fingerImageInfos }

        if (allFingerImageInfo.isNotEmpty()) {
            val fingerImageInfo = allFingerImageInfo.first()
            val imageLength = fingerImageInfo.imageLength
            val dataInputStream = DataInputStream(fingerImageInfo.imageInputStream)
            val buffer = ByteArray(imageLength)

            withContext(Dispatchers.IO) {
                dataInputStream.readFully(buffer, 0, imageLength)
            }

            val inputStream = ByteArrayInputStream(buffer, 0, imageLength)
            bitmap = decodeImage(context, fingerImageInfo.mimeType, inputStream)
            imageBase64 = Base64.encodeToString(buffer, Base64.DEFAULT)
        } else {
            Log.d(this.javaClass.name, "No fingerprint available")
        }

        return Pair(imageBase64, bitmap)
    }

    private fun doPassiveAuth(
        files: IcaoFiles,
        chipAuthSucceeded: Boolean,
    ): Boolean {
        try {
            val digest = MessageDigest.getInstance(files.sodFile.digestAlgorithm)
            val dataHashes = files.sodFile.dataGroupHashes
            val dg14Hash = if (chipAuthSucceeded) digest.digest(files.dg14Encoded) else ByteArray(0)
            val dg1Hash = digest.digest(files.dg1File.encoded)
            val dg2Hash = digest.digest(files.dg2File.encoded)

            if (!Arrays.equals(dg1Hash, dataHashes[1]) ||
                !Arrays.equals(dg2Hash, dataHashes[2]) ||
                (chipAuthSucceeded && !Arrays.equals(dg14Hash, dataHashes[14]))
            ) {
                return false
            }

            val keystore = validateCertificates(files.sodFile)
            return verifySignature(files.sodFile, keystore)
        } catch (e: Exception) {
            Log.w(this.javaClass.name, e)
            return false
        }
    }

    private fun validateCertificates(sodFile: SODFile): KeyStore {
        val asn1InputStream = ASN1InputStream(context.assets.open("masterList"))
        val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
        keystore.load(null, null)
        val cf = CertificateFactory.getInstance("X.509")

        var p: ASN1Primitive?
        while (asn1InputStream.readObject().also { p = it } != null) {
            val asn1 = ASN1Sequence.getInstance(p)
            requireNotNull(asn1) { "Null sequence passed." }
            require(asn1.size() == 2) { "Incorrect sequence size: ${asn1.size()}" }

            val certSet = ASN1Set.getInstance(asn1.getObjectAt(1))
            for (i in 0 until certSet.size()) {
                val certificate = Certificate.getInstance(certSet.getObjectAt(i))
                val pemCertificate = certificate.encoded
                val javaCertificate = cf.generateCertificate(ByteArrayInputStream(pemCertificate))
                keystore.setCertificateEntry(i.toString(), javaCertificate)
            }
        }

        // Validate document signing certificates
        sodFile.docSigningCertificates.forEach { it.checkValidity() }

        return keystore
    }

    private fun verifySignature(
        sodFile: SODFile,
        keystore: KeyStore,
    ): Boolean {
        val cp =
            CertificateFactory
                .getInstance("X.509")
                .generateCertPath(sodFile.docSigningCertificates)

        val pkixParameters = PKIXParameters(keystore)
        pkixParameters.isRevocationEnabled = false

        val cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType())
        cpv.validate(cp, pkixParameters)

        var sodDigestEncryptionAlgorithm = sodFile.docSigningCertificate.sigAlgName
        val isSSA = sodDigestEncryptionAlgorithm == "SSAwithRSA/PSS"
        if (isSSA) {
            sodDigestEncryptionAlgorithm = "SHA256withRSA/PSS"
        }

        val sign = Signature.getInstance(sodDigestEncryptionAlgorithm)
        if (isSSA) {
            sign.setParameter(PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1))
        }
        sign.initVerify(sodFile.docSigningCertificate)
        sign.update(sodFile.eContent)
        return sign.verify(sodFile.encryptedDigest)
    }
}
