package be.bluexin.ghe.io

import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mu.KotlinLogging
import java.nio.file.*

/*private*/ val logger = KotlinLogging.logger { }

inline fun Path.watch(
    vararg events: WatchEvent.Kind<*>,
    body: (WatchService, WatchKey) -> Unit
) = FileSystems.getDefault().newWatchService().use { watchService ->
    logger.debug { "Watching $this" }
    val watchKey = this.register(watchService, *events)
    try {
        body(watchService, watchKey)
    } finally {
        logger.debug { "Cleaning up !" }
        watchKey.cancel()
    }
}

suspend inline fun Path.watchEvents(
    vararg events: WatchEvent.Kind<*>,
    body: (WatchEvent<*>) -> Unit
) = watch(*events) { watchService, _ ->
    while (currentCoroutineContext().isActive) {
        val key = watchService.poll()
        if (key != null) {
            logger.debug { "Receiving ${key.watchable()}" }
            key.pollEvents().forEach(body)
            if (!key.reset()) {
                key.cancel()
                watchService.close()
                currentCoroutineContext().cancel()
            }
        } else delay(1_000)
    }
}