import crypto.DataKeyCreator
import crypto.Encryptor
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream

// TODO make this return a sequence of groups of lines whose lengths add up to, at most, maxSize
fun Sequence<ByteArray>.splitBySize(maxSize: Long) = this

fun ByteArray.compress(): ByteArray {
    val output = ByteArrayOutputStream()
    CompressorStreamFactory().createCompressorOutputStream(
            CompressorStreamFactory.BZIP2,
            BufferedOutputStream(output)
    ).use { compressor ->
        compressor.write(this)
    }
    return output.toByteArray()
}

data class EncryptionMetadata(val iv: ByteArray, val cipherText: String, val dataKeyEncryptionKeyId: String)

fun ByteArray.encrypt(dataKeyCreator: DataKeyCreator, encryptor: Encryptor): Pair<ByteArray, EncryptionMetadata> {
    val dataKeyResult = dataKeyCreator.createDataKey()
    val encryptionResult = encryptor.encrypt(dataKeyResult.plaintextDataKey, this)
    return Pair(
            encryptionResult.encrypted,
            EncryptionMetadata(
                    iv = encryptionResult.initialisationVector,
                    cipherText = dataKeyResult.ciphertextDataKey,
                    dataKeyEncryptionKeyId = dataKeyResult.dataKeyEncryptionKeyId
            )
    )
}

