package crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.Key
import java.security.SecureRandom
import java.security.Security
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESCipherService(val secureRandom: SecureRandom): EncryptorDecryptor {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    override fun encrypt(key: String, unencrypted: ByteArray): EncryptionResult {
        var initialisationVector = ByteArray(16).apply {
            secureRandom.nextBytes(this)
        }

        val keySpec: Key = SecretKeySpec(key.toByteArray(), "AES")
        val cipher = Cipher.getInstance(targetCipherAlgorithm, "BC").apply {
            init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(initialisationVector))
        }

        val encrypted = cipher.doFinal(unencrypted)
        return EncryptionResult(initialisationVector, encrypted)
    }

    override fun decrypt(key: String, initializationVector: String, encrypted: String): String {
        val keySpec: Key = SecretKeySpec(key.toByteArray(), "AES")

        val cipher = Cipher.getInstance(sourceCipherAlgorithm, "BC").apply {
            init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(Base64.getDecoder().decode(initializationVector)))
        }

        val decodedBytes = Base64.getDecoder().decode(encrypted.toByteArray())
        val original = cipher.doFinal(decodedBytes)
        return String(original)
    }

    private lateinit var sourceCipherAlgorithm: String

    private lateinit var targetCipherAlgorithm: String

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AESCipherService::class.toString())
    }
}