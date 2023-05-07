package be.bluexin.ghe.io

import be.bluexin.ghe.view.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

data class DataFile(
    val path: String,
    val data: List<DataHolder>
)

data class DataHolder(
    val position: Long,
    val text: String
)

object DataFileHandler {
    private val logger = KotlinLogging.logger { }

    /**
     * Loads data from the file denoted at [pathString]
     *
     * @return the loaded data along with a [Flow] which will complete after emitting when the loaded file was changed on disk
     */
    @OptIn(FlowPreview::class)
    suspend fun loadData(pathString: String): Pair<DataFile, Flow<Unit>>? {
        val file = File(pathString)
        return if (file.isFile) withContext(Dispatchers.IO) {
            DataFile(
                pathString,
                buildList {
                    RandomAccessFile(file, "r").use {
                        var line = it.readLine()
                        while (line != null) {
                            if (line.endsWith("</data>")) {
                                val text = line.substringAfter('>').dropLast(7)
                                val position = it.filePointer - text.length - 8
                                add(DataHolder(position, text).also {
                                    logger.debug { "Found element $it" }
                                })
                            }

                            line = it.readLine()
                        }
                    }
                }
            ) to flow {
                val path = file.toPath()
                path.parent.watchEvents(StandardWatchEventKinds.ENTRY_MODIFY) { event ->
                    val eventPath = event.context() as? Path
                    if (eventPath != null) {
                        logger.debug { "Event: ${event.kind()} x${event.count()} for $eventPath" }
                        if (path.endsWith(eventPath)) emit(Unit)
                    }
                }
            }.flowOn(Dispatchers.IO)
                .debounce(3_000L)
                .take(1)
        } else null
    }

    suspend fun writeData(dataFile: DataFile) {
        withContext(Dispatchers.IO) {
            RandomAccessFile(dataFile.path, "rw").use {
                dataFile.data.forEach { data ->
                    it.readLine()
                    it.seek(data.position)
                    it.write((data.text.replace('0', 'F')).encodeToByteArray())
                }
            }
        }
    }
}

/**
 * Trying stuff out :)
 */
fun main() {
    runBlocking {
        val path = "/home/bluexin/Rebellion/XML.DAT_CLIENT_FILES/datafile.bin.files/datafile_111.xml"
        val (data, updates) = DataFileHandler.loadData(path) ?: error("Something went wrong reading datafile 111")
        val watcher = launch {
            updates.collectLatest {
                logger.info { "Caught update" }
            }
        }
        delay(20_000)
        logger.info { "Replacing all 0's with F's" }
        DataFileHandler.writeData(data)
        watcher.cancelAndJoin()
    }
}