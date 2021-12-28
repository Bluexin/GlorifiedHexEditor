package be.bluexin.ghe.loader

import be.bluexin.ghe.json.Named
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import kotlin.io.path.pathString

object Loader {
    private fun watchService() = FileSystems.getDefault().newWatchService()
    private val objectMapper = jacksonObjectMapper().apply {
        configure(SerializationFeature.INDENT_OUTPUT, true)
    }
    private val logger = KotlinLogging.logger {}

    private fun <T : Named> File.read(typeRef: TypeReference<T>, init: (T) -> Unit): Pair<String, T>? = try {
        val read = objectMapper.readValue(this, typeRef)
        init(read)
        logger.info { "Loaded ${read.name} (${this})" }
        read.name to read
    } catch (e: Exception) {
        logger.warn(e) { "Failed to load ${this.name}" }
        null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T : Named> load(
        root: File,
        directory: String?,
        typeRef: TypeReference<T>,
        init: (T) -> Unit
    ): Flow<Map<String, T>> {
        if (directory == null) return emptyFlow()
        logger.info { "Loading ${typeRef.type.typeName}" }

        val realDir = File(root, directory)
        return if (realDir.isDirectory) withContext(Dispatchers.IO) {
            val initial = realDir.walk().filter { !it.isDirectory }.mapNotNull { it.read(typeRef, init) }.toMap()

            val watchService = watchService()
            val path = realDir.toPath()
            logger.debug { "Watching $path" }
            path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY)

            flow {
                while (true) {
                    val key = watchService.take()
                    logger.debug { "Receiving ${key.watchable()}" }
                    key.pollEvents().forEach { event ->
                        val eventPath = event.context() as Path
                        logger.debug { "Event: ${event.kind()} x${event.count()} for $eventPath" }
                        if (!eventPath.pathString.endsWith('~')) path.resolve(eventPath).toFile().read(typeRef, init)
                            ?.let { this.emit(it) }
                    }
                    key.reset()
                }
            }.flowOn(Dispatchers.IO)
                .runningFold(initial) { acc, it -> acc + it }
        } else emptyFlow()
    }

    suspend inline fun <reified T : Named> load(
        root: File,
        directory: String?,
        noinline init: (T) -> Unit = {}
    ): Flow<Map<String, T>> = load(root, directory, jacksonTypeRef(), init)
}