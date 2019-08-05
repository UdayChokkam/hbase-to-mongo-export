import app.batch.activeTopics
import app.batch.scanTopicTable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import sink.filename
import sink.formatMetadata
import sun.misc.Signal


suspend fun main() {
    configureLogging()

    // Connect to Hbase
    val hbase = Config.Hbase.connect()

    // Get the encryption clients
    val dataKeyHandler = Config.DataKeyService.createDataKeyHandler()
    val encrytionHandler = Config.DataKeyService.createEncryptionHandler()

    // Get the file sink
    val sink = Config.Sink.create()

    // Generate the output from the records in Hbase
    val job = GlobalScope.async {
        activeTopics(
                hbase,
                Config.Hbase.topicTable.toByteArray(),
                Config.Hbase.topicFamily.toByteArray(),
                Config.Hbase.topicQualifier.toByteArray()
        ).forEach { topic ->
            scanTopicTable(
                    hbase,
                    Config.Hbase.dataTable.toByteArray(),
                    Config.Hbase.dataFamily.toByteArray(),
                    topic
            ).map { record ->
                decryptRecord(dataKeyHandler, encrytionHandler, record)
            }.splitBySize(10).map {
                it.compress().encrypt(dataKeyHandler, encrytionHandler)
            }.forEachIndexed { index, (cipherText, encryptionMetadata) ->
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