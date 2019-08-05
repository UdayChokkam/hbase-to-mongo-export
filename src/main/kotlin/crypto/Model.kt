package crypto

data class EncryptionResult(
        val initialisationVector: ByteArray,
        val encrypted: ByteArray)

data class DataKeyResult(
        val dataKeyEncryptionKeyId: String,
        val plaintextDataKey: String,
        val ciphertextDataKey: String)

interface KeyDecryptor {
    fun decryptKey(encryptionKeyId: String, encryptedKey: String): String
}

interface DataKeyCreator {
    fun createDataKey(): DataKeyResult
}

interface Decryptor {
    fun decrypt(key: String, initializationVector: String, encrypted: String): String
}

interface Encryptor {
    fun encrypt(key: String, unencrypted: ByteArray): EncryptionResult
}

interface EncryptorDecryptor: Encryptor, Decryptor

interface DataKeyHandler: DataKeyCreator, KeyDecryptor