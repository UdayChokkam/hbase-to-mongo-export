import app.batch.activeTopics
import app.batch.scanTopicTable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import sink.filename
import sink.formatMetadata
import sun.misc.Signal
import java.util.logging.Logger


suspend fun main() {
    configureLogging()
    val logger = Logger.getLogger("main")

    // Connect to Hbase
    val hbase = Config.Hbase.connect()

    // Get the encryption clients
    val dataKeyHandler = Config.DataKeyService.createDataKeyHandler()
    val encrytionHandler = Config.Encryption.createEncryptionHandler()

    // Get the file sink
    val sink = Config.Sink.create()

    // Generate the output from the records in Hbase
    val job = GlobalScope.async {
        val topics = activeTopics(
                hbase,
                Config.Hbase.topicTable.toByteArray(),
                Config.Hbase.topicFamily.toByteArray(),
                Config.Hbase.topicQualifier.toByteArray()
        )


        for (topic in topics) {
            logger.info(String(topic))
            val records = scanTopicTable(
                    hbase,
                    Config.Hbase.dataTable.toByteArray(),
                    Config.Hbase.dataFamily.toByteArray(),
                    topic
            )

            val decryptedRecords = records.map { record ->
                decryptRecord(dataKeyHandler, encrytionHandler, record)
            }

            val encryptedChunks = decryptedRecords.splitBySize(10).map {
                it.compress().encrypt(dataKeyHandler, encrytionHandler)
            }

            encryptedChunks.forEachIndexed { index, (cipherText, encryptionMetadata) ->
                sink.write(
                        filename(String(topic), index, ".enc"),
                        cipherText
                )
                sink.write(
                        filename(String(topic), index, ".metadata"),
                        formatMetadata(encryptionMetadata).toByteArray()
                )
            }
        }
    }

    // Handle signals gracefully and wait for completion
    Signal.handle(Signal("INT")) { job.cancel() }
    Signal.handle(Signal("TERM")) { job.cancel() }
    job.await()

    hbase.close()
}