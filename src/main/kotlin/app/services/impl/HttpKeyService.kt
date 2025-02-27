package app.services.impl

import app.domain.DataKeyResult
import app.exceptions.DataKeyDecryptionException
import app.exceptions.DataKeyServiceUnavailableException
import app.services.KeyService
import com.google.gson.Gson
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
@Profile("httpDataKeyService")
class HttpKeyService(private val httpClient: HttpClient): KeyService {

    override fun batchDataKey(): DataKeyResult {
        val response = httpClient.execute(HttpGet("$dataKeyServiceUrl/datakey"))
        return if (response.statusLine.statusCode == 201) {
            val entity = response.entity
            val result = BufferedReader(InputStreamReader(entity.content))
                    .use(BufferedReader::readText).let {
                Gson().fromJson(it, DataKeyResult::class.java)
            }
            EntityUtils.consume(entity)
            result
        }
        else  {
            throw DataKeyServiceUnavailableException(
                    "DataKeyService returned status code '${response.statusLine.statusCode}'.")
        }
    }

    override fun decryptKey(encryptionKeyId: String, encryptedKey: String): String {
        logger.info("Decrypting encryptedKey: '$encryptedKey', encryptionKeyId: '$encryptionKeyId'.")
        val idPart = encryptionKeyId.replace(Regex(".*/"), "")
        val cacheKey = "$encryptedKey/$idPart"
        return if (decryptedKeyCache.containsKey(cacheKey))  {
            decryptedKeyCache.get(cacheKey)!!
        }
        else {
           val httpPost = HttpPost("$dataKeyServiceUrl/datakey/actions/decrypt?keyId=$idPart")
            httpPost.entity = StringEntity(encryptedKey, ContentType.TEXT_PLAIN)
            val response = httpClient.execute(httpPost)
            return when {
                response.statusLine.statusCode == 200 -> {
                    val entity = response.entity
                    val text = BufferedReader(InputStreamReader(response.entity.content)).use(BufferedReader::readText)
                    EntityUtils.consume(entity)
                    val dataKeyResult = Gson().fromJson(text, DataKeyResult::class.java)
                    decryptedKeyCache.put(cacheKey, dataKeyResult.plaintextDataKey)
                    dataKeyResult.plaintextDataKey
                }
                response.statusLine.statusCode == 400 ->
                    throw DataKeyDecryptionException(
                            """Decrypting encryptedKey: '$encryptedKey' with 
                            |encryptionKeyId: '$encryptionKeyId'
                            |data key service returned status code '${response.statusLine.statusCode}'""".trimMargin())
                else ->
                    throw DataKeyServiceUnavailableException(
                            """Decrypting encryptedKey: '$encryptedKey' with 
                            |encryptionKeyId: '$encryptionKeyId'
                            |data key service returned status code '${response.statusLine.statusCode}'""".trimMargin())
            }
        }
    }

    fun clearCache() {
        this.decryptedKeyCache = mutableMapOf()
    }

    private var decryptedKeyCache = mutableMapOf<String, String>()

    @Value("\${data.key.service.url}")
    private var dataKeyServiceUrl: String = "http://localhost:8080"

    companion object {
        val logger: Logger = LoggerFactory.getLogger(HttpKeyService::class.toString())
    }

}
