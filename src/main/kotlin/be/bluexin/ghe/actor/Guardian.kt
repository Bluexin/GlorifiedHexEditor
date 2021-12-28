package be.bluexin.ghe.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors

sealed interface Guardian {

    class WithSettingsLoader(val body: (ActorRef<SettingsLoader>) -> Unit) : Guardian

    companion object {
        fun system(): ActorSystem<Guardian> =
            ActorSystem.create(Behaviors.setup {
                val sl = it.spawn(SettingsLoader.actor(), "SettingsLoader")

                Behaviors.receiveMessage { message ->
                    when (message) {
                        is WithSettingsLoader -> {
                            message.body(sl)
                            Behaviors.same()
                        }
                    }
                }
            }, "guardian")

    }
}