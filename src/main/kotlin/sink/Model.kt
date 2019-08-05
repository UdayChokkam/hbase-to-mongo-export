package sink

import EncryptionMetadata

interface Sink {
    fun write(filename: String, data: ByteArray)
}

fun filename(prefix: String, fileNum: Int, extension: String) = "%s-%04d.%s".format(prefix, fileNum, extension)

fun formatMetadata(metadata: EncryptionMetadata) = "iv=%s\nciphertext=%s\ndataKeyEncryptionKeyId=%s\n".format(
        metadata.iv,
        metadata.cipherText,
        metadata.dataKeyEncryptionKeyId
)
