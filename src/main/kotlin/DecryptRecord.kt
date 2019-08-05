import app.batch.SourceRecord
import com.google.gson.Gson
import com.google.gson.JsonObject
import crypto.Decryptor
import crypto.KeyDecryptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun decryptRecord(keyDecryptor: KeyDecryptor, decryptor: Decryptor, item: SourceRecord): ByteArray {
    val logger: Logger = LoggerFactory.getLogger("decryptRecord")
    logger.info("Processing '$item'.")

    val decryptedKey = keyDecryptor.decryptKey(
            item.encryption.encryptionKeyId,
            item.encryption.encryptedEncryptionKey)

    val decrypted = decryptor.decrypt(
            decryptedKey,
            item.encryption.initializationVector,
            item.dbObject)

    val jsonObject = Gson().fromJson(decrypted, JsonObject::class.java)
    jsonObject.addProperty("timestamp", item.hbaseTimestamp)
    return jsonObject.toString().toByteArray()
}
