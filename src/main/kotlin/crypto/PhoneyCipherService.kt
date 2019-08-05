package crypto

class PhoneyCipherService: EncryptorDecryptor {

    override fun encrypt(key: String, unencrypted: ByteArray): EncryptionResult {
        TODO("not implemented")
    }

    override fun decrypt(key: String, initializationVector: String, encrypted: String) =
            """{ "decryptedObject": "${encrypted.substring(10)} ..." }"""
}