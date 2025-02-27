package app.services.impl

import app.domain.EncryptionResult
import app.services.CipherService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.security.Key
import java.security.SecureRandom
import java.security.Security
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
@Profile("aesCipherService")
class AESCipherService(private val secureRandom: SecureRandom): CipherService {

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
        return EncryptionResult(String(Base64.getEncoder().encode(initialisationVector)),
                String(Base64.getEncoder().encode(encrypted)))
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

    @Value("\${source.cipher.algorithm:AES/CTR/NoPadding}")
    private lateinit var sourceCipherAlgorithm: String


    @Value("\${target.cipher.algorithm:AES/CTR/NoPadding}")
    private lateinit var targetCipherAlgorithm: String

    companion object {
        val logger: Logger = LoggerFactory.getLogger(AESCipherService::class.toString())
    }
}