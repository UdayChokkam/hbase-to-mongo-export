package sink

import java.nio.file.Files
import java.nio.file.Paths

class FileSink(val directory: String) : Sink {
    override fun write(filename: String, data: ByteArray) {
        Files.write(Paths.get(directory, filename), data)
    }
}