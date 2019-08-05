import crypto.*
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.http.impl.client.HttpClientBuilder
import sink.FileSink
import java.security.SecureRandom

fun getEnv(envVar: String): String? {
    val value = System.getenv(envVar)
    return if (value.isNullOrEmpty()) null else value
}

object Config {
    object Hbase {
        val config = Configuration().apply {
            set("hbase.zookeeper.quorum", getEnv("HBASE_ZOOKEEPER_QUORUM") ?: "zookeeper")
            setInt("hbase.zookeeper.port", getEnv("HBASE_ZOOKEEPER_PORT")?.toIntOrNull() ?: 2181)
        }

        val dataTable = getEnv("HBASE_DATA_TABLE") ?: "ucdata"
        val dataFamily = getEnv("HBASE_DATA_FAMILY") ?: "cf"
        val topicTable = getEnv("HBASE_TOPIC_TABLE") ?: "ucdata-topics"
        val topicFamily = getEnv("HBASE_TOPIC_FAMILY") ?: "cf"
        val topicQualifier = getEnv("HBASE_TOPIC_QUALIFIER") ?: "msg"

        fun connect() = ConnectionFactory.createConnection(HBaseConfiguration.create(config))
    }

    object DataKeyService {
        val url = getEnv("DKS_URL") ?: ""
        val fake = getEnv("DKS_FAKE") ?: "" == "true"

        fun createDataKeyHandler(): DataKeyHandler {
            if (fake) {
                return PhoneyKeyService()
            }
            return HttpKeyService(
                    HttpClientBuilder.create().build(),
                    url
            )
        }
    }

    object Encryption {
        var sourceCipherAlgorithm = getEnv("ENC_SOURCE_CIPHER_ALGO") ?: "AES/CTR/NoPadding"
        var targetCipherAlgorithm = getEnv("ENC_TARGET_CIPHER_ALGO") ?: "AES/CTR/NoPadding"

        fun createEncryptionHandler(): EncryptorDecryptor {
            return AESCipherService(SecureRandom(), sourceCipherAlgorithm, targetCipherAlgorithm)
        }
    }

    object Sink {
        var fileDirectory = getEnv("FILE_DIRECTORY") ?: "."

        fun create() = FileSink(fileDirectory)
    }
}