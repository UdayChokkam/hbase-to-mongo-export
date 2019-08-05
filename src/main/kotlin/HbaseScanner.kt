package app.batch

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Connection
import org.apache.hadoop.hbase.client.Result
import org.apache.hadoop.hbase.client.Scan
import org.slf4j.Logger
import org.slf4j.LoggerFactory


data class EncryptionBlock(
        val encryptionKeyId: String,
        val initializationVector: String,
        val encryptedEncryptionKey: String)

data class SourceRecord(
        val hbaseId: String,
        val hbaseTimestamp: Long,
        val encryption: EncryptionBlock,
        var dbObject: String)

fun Result.asSourceRecord(family: ByteArray, topic: ByteArray): SourceRecord {
    val value = getValue(family, topic)
    val dataBlock = Gson().fromJson(String(value), JsonObject::class.java)
    val encryptionInfo = dataBlock.getAsJsonObject("encryption")

    return SourceRecord(
            hbaseId = dataBlock.getAsJsonPrimitive("id").asString,
            hbaseTimestamp = current().timestamp,
            dbObject = dataBlock.getAsJsonPrimitive("dbObject")?.asString ?: "",
            encryption = EncryptionBlock(
                    encryptionKeyId = encryptionInfo.getAsJsonPrimitive("keyEncryptionKeyId").asString,
                    encryptedEncryptionKey = encryptionInfo.getAsJsonPrimitive("encryptedEncryptionKey").asString,
                    initializationVector = encryptionInfo.getAsJsonPrimitive("initialisationVector").asString
            )
    )
}

fun scanTopicTable(connection: Connection, tableName: ByteArray, family: ByteArray, topic: ByteArray) = sequence {
    val logger: Logger = LoggerFactory.getLogger("scanTopicTable")
    logger.info("Getting '$topic' from '$tableName' on '$connection'.")
    val table = connection.getTable(TableName.valueOf(tableName))
    val scan = Scan()
    val scanner = table.getScanner(scan)

    try {
        yieldAll(scanner.map { it.asSourceRecord(family, topic) })
    } finally {
        scanner.close()
        table.close()
    }
}

fun activeTopics(connection: Connection, tableName: ByteArray, family: ByteArray, qualifier: ByteArray) = sequence {
    connection.getTable(TableName.valueOf(tableName)).use { table ->
        val scanner = table.getScanner(family, qualifier)
        yieldAll(scanner.map { it.row })
    }
}