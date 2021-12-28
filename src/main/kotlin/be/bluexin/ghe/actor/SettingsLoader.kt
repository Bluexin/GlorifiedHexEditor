package be.bluexin.ghe.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.Behaviors
import be.bluexin.ghe.json.Settings
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import java.io.File

private typealias SLBehavior = Behavior<SettingsLoader>

sealed interface SettingsLoader {
    class AddListener(val listener: ActorRef<Settings>) : SettingsLoader
    object Load : SettingsLoader
    class Update(val settings: Settings) : SettingsLoader

    companion object {
        fun actor(): Behavior<SettingsLoader> = uninitialized()

        private val objectMapper = jacksonObjectMapper().apply {
            configure(SerializationFeature.INDENT_OUTPUT, true)
        }

        private val logger = KotlinLogging.logger {}
        private val settingsFile = File("settings.json")

        private fun uninitialized(listeners: Set<ActorRef<Settings>> = emptySet()): SLBehavior =
            Behaviors.receiveMessage {
                when (it) {
                    is Load -> loadSettings(listeners)
                    is AddListener -> uninitialized(listeners + it.listener)
                    is Update -> update(it.settings, listeners)
                }
            }

        private fun loaded(settings: Settings, listeners: Set<ActorRef<Settings>>): SLBehavior =
            Behaviors.receiveMessage {
                when (it) {
                    is AddListener -> {
                        it.listener.tell(settings)
                        loaded(settings, listeners + it.listener)
                    }
                    is Update -> update(it.settings, listeners)
                    else -> Behaviors.unhandled()
                }
            }

        private fun loadSettings(listeners: Set<ActorRef<Settings>>): SLBehavior {
            val settings = if (settingsFile.isFile) try {
                objectMapper.readValue(settingsFile)
            } catch (e: Exception) {
                logger.warn(e) { "Couldn't load settings" }
                Settings.default
            } else Settings.default
            return update(settings, listeners)
        }

        private fun update(settings: Settings, listeners: Set<ActorRef<Settings>>): SLBehavior {
            listeners.forEach { it.tell(settings) }
            return saveSettings(settings, listeners)
        }

        private fun saveSettings(settings: Settings, listeners: Set<ActorRef<Settings>>): SLBehavior {
            objectMapper.writeValue(settingsFile, settings)
            return loaded(settings, listeners)
        }
    }
}
