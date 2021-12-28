package be.bluexin.ghe.actor

import akka.actor.ClassicActorSystemProvider
import akka.stream.javadsl.Source
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import scala.PartialFunction

@Composable
fun <Out, Mat> Source<Out, Mat>.collectAsState(
    initial: Out,
    provider: ClassicActorSystemProvider
): State<Out> = produceState(initial, this) {
    runForeach({
        value = it
    }, provider)
    collect(PartialFunction.fromFunction {
        value = it
    })
}